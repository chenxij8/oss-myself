package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.system.dto.FileShareResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.entity.FileShare;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.FileShareMapper;
import com.oss.file.system.mapper.SysUserMapper;
import com.oss.file.system.service.FileInfoService;
import com.oss.file.system.service.FileShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件分享业务实现。
 * 1. 创建/删除分享均校验文件归属；
 * 2. 下载次数校验与扣减合并为一条条件 UPDATE，具备原子性，避免并发突破限制；
 * 3. Redis 缓存真正参与读取（短 TTL），且 Redis 异常时自动降级到数据库。
 */
@Service
@Slf4j
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements FileShareService {

    private static final String SHARE_CACHE_PREFIX = "file:share:";
    private static final long CACHE_TTL_SECONDS = 30L;

    private final FileInfoService fileInfoService;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    public FileShareServiceImpl(FileInfoService fileInfoService,
                                StringRedisTemplate redisTemplate,
                                SysUserMapper sysUserMapper,
                                ObjectMapper objectMapper) {
        this.fileInfoService = fileInfoService;
        this.redisTemplate = redisTemplate;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileShareResponse createShare(Long fileId, Long userId, Long expireTime, Integer maxDownloads) {
        if (!fileInfoService.canAccess(fileId, userId)) {
            throw new BusinessException(403, "无权分享此文件");
        }
        FileInfo fileInfo = fileInfoService.getFileDetail(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        String shareToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        FileShare fileShare = new FileShare();
        fileShare.setFileId(fileId);
        fileShare.setShareToken(shareToken);
        fileShare.setShareUserId(userId);
        fileShare.setMaxDownloads(maxDownloads);
        fileShare.setCurrentDownloads(0);
        fileShare.setIsActive(1);
        fileShare.setCreateTime(now);
        fileShare.setUpdateTime(now);
        if (expireTime != null && expireTime > 0) {
            fileShare.setExpireTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(expireTime), ZoneId.systemDefault()));
        }

        baseMapper.insert(fileShare);
        log.info("分享链接已创建，Token: {}, FileId: {}", shareToken, fileId);

        FileShareResponse response = buildShareResponse(fileShare, fileInfo);
        cachePut(shareToken, response, expireTime);
        return response;
    }

    @Override
    public FileShareResponse getShareInfo(String shareToken) {
        FileShareResponse cached = cacheGet(shareToken);
        if (cached != null && !isExpired(cached.getExpireTime())) {
            return cached;
        }
        if (cached != null) {
            cacheDelete(shareToken);
        }

        FileShare fileShare = findByToken(shareToken);
        if (fileShare == null) {
            throw new BusinessException("分享链接不存在");
        }
        if (!isActive(fileShare)) {
            throw new BusinessException("分享链接已过期或已失效");
        }
        FileInfo fileInfo = fileInfoService.getFileDetail(fileShare.getFileId());
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        FileShareResponse response = buildShareResponse(fileShare, fileInfo);
        cachePut(shareToken, response, toEpochMillis(fileShare.getExpireTime()));
        return response;
    }

    @Override
    public boolean isShareValid(String shareToken) {
        FileShareResponse cached = cacheGet(shareToken);
        if (cached != null) {
            return !isExpired(cached.getExpireTime())
                    && (cached.getMaxDownloads() == null || cached.getCurrentDownloads() < cached.getMaxDownloads());
        }
        FileShare fileShare = findByToken(shareToken);
        if (fileShare == null) {
            return false;
        }
        if (!isActive(fileShare)) {
            markInactive(fileShare);
            cacheDelete(shareToken);
            return false;
        }
        FileInfo fileInfo = fileInfoService.getFileDetail(fileShare.getFileId());
        if (fileInfo == null) {
            return false;
        }
        cachePut(shareToken, buildShareResponse(fileShare, fileInfo), toEpochMillis(fileShare.getExpireTime()));
        return true;
    }

    /** 兼容旧调用：原子扣减下载次数 */
    @Override
    public boolean incrementDownloadCount(String shareToken) {
        return consumeShare(shareToken) != null;
    }

    /**
     * 原子校验 + 扣减：单条 UPDATE 同时判断活跃、过期、下载次数并自增。
     * 影响行数为 1 表示抢到一次下载额度。
     */
    @Override
    public FileShareResponse consumeShare(String shareToken) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<FileShare> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileShare::getShareToken, shareToken)
                     .eq(FileShare::getIsActive, 1)
                     .and(w -> w.isNull(FileShare::getExpireTime).or().gt(FileShare::getExpireTime, now))
                     .and(w -> w.isNull(FileShare::getMaxDownloads)
                             .or().apply("current_downloads < max_downloads"))
                     .setSql("current_downloads = current_downloads + 1")
                     .set(FileShare::getUpdateTime, now);

        int rows = baseMapper.update(null, updateWrapper);
        if (rows != 1) {
            FileShare share = findByToken(shareToken);
            if (share != null && !isActive(share)) {
                markInactive(share);
            }
            cacheDelete(shareToken);
            return null;
        }

        FileShare fileShare = findByToken(shareToken);
        if (fileShare == null) {
            return null;
        }
        FileInfo fileInfo = fileInfoService.getFileDetail(fileShare.getFileId());
        if (fileInfo == null) {
            return null;
        }
        cacheDelete(shareToken); // 计数已变化，删除旧缓存
        return buildShareResponse(fileShare, fileInfo);
    }

    @Override
    public List<FileShareResponse> getUserShares(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<FileShare> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileShare::getShareUserId, userId)
               .orderByDesc(FileShare::getCreateTime);
        List<FileShare> shares = baseMapper.selectPage(pageParam, wrapper).getRecords();
        if (shares.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询文件信息，避免 N+1
        List<Long> fileIds = shares.stream().map(FileShare::getFileId).distinct().collect(Collectors.toList());
        Map<Long, FileInfo> fileMap = fileInfoService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileInfo::getId, Function.identity(), (a, b) -> a));

        return shares.stream()
                .filter(share -> fileMap.containsKey(share.getFileId()))
                .map(share -> buildShareResponse(share, fileMap.get(share.getFileId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteShare(Long shareId, Long userId) {
        FileShare fileShare = baseMapper.selectById(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享链接不存在");
        }
        if (!userId.equals(fileShare.getShareUserId()) && !isAdmin(userId)) {
            throw new BusinessException(403, "无权删除此分享链接");
        }

        FileShare update = new FileShare();
        update.setId(shareId);
        update.setIsActive(0);
        update.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(update);
        cacheDelete(fileShare.getShareToken());
        log.info("分享链接已删除，ShareId: {}", shareId);
        return true;
    }

    private FileShare findByToken(String shareToken) {
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileShare::getShareToken, shareToken);
        return baseMapper.selectOne(wrapper);
    }

    private boolean isActive(FileShare share) {
        if (!Integer.valueOf(1).equals(share.getIsActive())) {
            return false;
        }
        if (isExpired(share.getExpireTime())) {
            return false;
        }
        return share.getMaxDownloads() == null || share.getCurrentDownloads() < share.getMaxDownloads();
    }

    private boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    private void markInactive(FileShare share) {
        FileShare update = new FileShare();
        update.setId(share.getId());
        update.setIsActive(0);
        update.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(update);
    }

    private boolean isAdmin(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private Long toEpochMillis(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private FileShareResponse buildShareResponse(FileShare fileShare, FileInfo fileInfo) {
        FileShareResponse response = new FileShareResponse();
        response.setId(fileShare.getId());
        response.setFileId(fileShare.getFileId());
        response.setShareToken(fileShare.getShareToken());
        response.setFileName(fileInfo.getFileName());
        response.setFileSize(fileInfo.getFileSize());
        response.setExpireTime(fileShare.getExpireTime());
        response.setMaxDownloads(fileShare.getMaxDownloads());
        response.setCurrentDownloads(fileShare.getCurrentDownloads());
        response.setCreateTime(fileShare.getCreateTime());
        response.setShareUrl("/api/shares/download/" + fileShare.getShareToken());
        return response;
    }

    /* ---------------- Redis 缓存：所有异常都降级，绝不影响主流程 ---------------- */

    private void cachePut(String token, FileShareResponse response, Long expireTimeMillis) {
        try {
            long ttl = CACHE_TTL_SECONDS;
            if (expireTimeMillis != null && expireTimeMillis > 0) {
                long remain = (expireTimeMillis - System.currentTimeMillis()) / 1000;
                if (remain <= 0) {
                    return;
                }
                ttl = Math.min(ttl, remain);
            }
            redisTemplate.opsForValue().set(SHARE_CACHE_PREFIX + token,
                    objectMapper.writeValueAsString(response), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("分享缓存写入失败，已降级: {}", token);
        }
    }

    private FileShareResponse cacheGet(String token) {
        try {
            String json = redisTemplate.opsForValue().get(SHARE_CACHE_PREFIX + token);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, FileShareResponse.class);
        } catch (Exception e) {
            log.debug("分享缓存读取失败，已降级: {}", token);
            return null;
        }
    }

    private void cacheDelete(String token) {
        try {
            redisTemplate.delete(SHARE_CACHE_PREFIX + token);
        } catch (Exception e) {
            log.debug("分享缓存删除失败，已降级: {}", token);
        }
    }
}
