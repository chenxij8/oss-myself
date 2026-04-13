package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.system.dto.FileShareResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.entity.FileShare;
import com.oss.file.system.mapper.FileShareMapper;
import com.oss.file.system.service.FileInfoService;
import com.oss.file.system.service.FileShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件分享业务实现类
 * 负责分享链接的生成、验证、删除等操作
 */
@Service
@Slf4j
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements FileShareService {

    private final FileInfoService fileInfoService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis中分享链接缓存的前缀
     */
    private static final String SHARE_CACHE_PREFIX = "file:share:";

    /**
     * Redis中分享限流的前缀
     */
    private static final String SHARE_RATE_LIMIT_PREFIX = "file:share:rate:";

    public FileShareServiceImpl(FileInfoService fileInfoService, StringRedisTemplate redisTemplate) {
        this.fileInfoService = fileInfoService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 创建分享链接
     */
    @Override
    public FileShareResponse createShare(Long fileId, Long userId, Long expireTime, Integer maxDownloads) {
        try {
            // 验证文件是否存在
            FileInfo fileInfo = fileInfoService.getFileDetail(fileId);
            if (fileInfo == null) {
                throw new BusinessException("文件不存在");
            }

            // 生成唯一的分享令牌（UUID）
            String shareToken = UUID.randomUUID().toString().replace("-", "");

            // 创建分享记录
            FileShare fileShare = new FileShare();
            fileShare.setFileId(fileId);
            fileShare.setShareToken(shareToken);
            fileShare.setShareUserId(userId);
            fileShare.setMaxDownloads(maxDownloads);
            fileShare.setCurrentDownloads(0);
            fileShare.setIsActive(1);
            fileShare.setCreateTime(LocalDateTime.now());
            fileShare.setUpdateTime(LocalDateTime.now());

            // 设置过期时间
            if (expireTime != null && expireTime > 0) {
                LocalDateTime expireDateTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(expireTime),
                        ZoneId.systemDefault()
                );
                fileShare.setExpireTime(expireDateTime);
            }

            baseMapper.insert(fileShare);
            log.info("分享链接已创建，Token: {}, FileId: {}", shareToken, fileId);

            // 缓存到Redis（以过期时间为TTL）
            String cacheKey = SHARE_CACHE_PREFIX + shareToken;
            redisTemplate.opsForValue().set(cacheKey, fileId.toString());
            if (expireTime != null && expireTime > 0) {
                long ttl = (expireTime - System.currentTimeMillis()) / 1000;
                if (ttl > 0) {
                    redisTemplate.expire(cacheKey, ttl, TimeUnit.SECONDS);
                }
            }

            // 构建分享响应
            return buildShareResponse(fileShare, fileInfo);
        } catch (Exception e) {
            log.error("创建分享链接异常", e);
            throw new BusinessException("创建分享链接失败：" + e.getMessage());
        }
    }

    /**
     * 获取分享信息
     */
    @Override
    public FileShareResponse getShareInfo(String shareToken) {
        try {
            LambdaQueryWrapper<FileShare> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileShare::getShareToken, shareToken);
            FileShare fileShare = baseMapper.selectOne(queryWrapper);

            if (fileShare == null) {
                throw new BusinessException("分享链接不存在");
            }

            // 检查分享链接是否有效
            if (!isShareValid(shareToken)) {
                throw new BusinessException("分享链接已过期或已失效");
            }

            FileInfo fileInfo = fileInfoService.getFileDetail(fileShare.getFileId());
            if (fileInfo == null) {
                throw new BusinessException("文件不存在");
            }

            return buildShareResponse(fileShare, fileInfo);
        } catch (Exception e) {
            log.error("获取分享信息异常", e);
            throw new BusinessException("获取分享信息失败：" + e.getMessage());
        }
    }

    /**
     * 检查分享链接是否有效
     */
    @Override
    public boolean isShareValid(String shareToken) {
        try {
            LambdaQueryWrapper<FileShare> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileShare::getShareToken, shareToken);
            queryWrapper.eq(FileShare::getIsActive, 1);
            FileShare fileShare = baseMapper.selectOne(queryWrapper);

            if (fileShare == null) {
                return false;
            }

            // 检查是否过期
            if (fileShare.getExpireTime() != null && fileShare.getExpireTime().isBefore(LocalDateTime.now())) {
                // 标记为失效
                FileShare update = new FileShare();
                update.setId(fileShare.getId());
                update.setIsActive(0);
                baseMapper.updateById(update);
                return false;
            }

            // 检查是否超过下载次数限制
            if (fileShare.getMaxDownloads() != null && fileShare.getCurrentDownloads() >= fileShare.getMaxDownloads()) {
                // 标记为失效
                FileShare update = new FileShare();
                update.setId(fileShare.getId());
                update.setIsActive(0);
                baseMapper.updateById(update);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("检查分享有效性异常", e);
            return false;
        }
    }

    /**
     * 增加分享的下载计数
     */
    @Override
    public boolean incrementDownloadCount(String shareToken) {
        try {
            LambdaQueryWrapper<FileShare> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileShare::getShareToken, shareToken);
            FileShare fileShare = baseMapper.selectOne(queryWrapper);

            if (fileShare == null) {
                return false;
            }

            // 检查是否超过最大下载次数
            if (fileShare.getMaxDownloads() != null && fileShare.getCurrentDownloads() >= fileShare.getMaxDownloads()) {
                return false;
            }

            // 增加下载计数
            FileShare update = new FileShare();
            update.setId(fileShare.getId());
            update.setCurrentDownloads(fileShare.getCurrentDownloads() + 1);
            update.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(update);

            return true;
        } catch (Exception e) {
            log.error("增加下载计数异常", e);
            return false;
        }
    }

    /**
     * 删除分享链接
     */
    @Override
    public boolean deleteShare(Long shareId, Long userId) {
        try {
            FileShare fileShare = baseMapper.selectById(shareId);
            if (fileShare == null) {
                throw new BusinessException("分享链接不存在");
            }

            // 验证权限（仅分享所有者可删除）
            if (!fileShare.getShareUserId().equals(userId)) {
                throw new BusinessException("无权删除此分享链接");
            }

            // 标记为失效
            FileShare update = new FileShare();
            update.setId(shareId);
            update.setIsActive(0);
            baseMapper.updateById(update);

            // 清除Redis缓存
            String cacheKey = SHARE_CACHE_PREFIX + fileShare.getShareToken();
            redisTemplate.delete(cacheKey);

            log.info("分享链接已删除，ShareId: {}", shareId);
            return true;
        } catch (Exception e) {
            log.error("删除分享链接异常", e);
            throw new BusinessException("删除分享链接失败：" + e.getMessage());
        }
    }

    /**
     * 构建分享信息响应
     */
    private FileShareResponse buildShareResponse(FileShare fileShare, FileInfo fileInfo) {
        FileShareResponse response = new FileShareResponse();
        response.setId(fileShare.getId());
        response.setShareToken(fileShare.getShareToken());
        response.setFileName(fileInfo.getFileName());
        response.setFileSize(fileInfo.getFileSize());
        response.setExpireTime(fileShare.getExpireTime());
        response.setMaxDownloads(fileShare.getMaxDownloads());
        response.setCurrentDownloads(fileShare.getCurrentDownloads());
        response.setCreateTime(fileShare.getCreateTime());
        
        // 生成分享URL（假设前端访问地址）
        response.setShareUrl("/api/share/download/" + fileShare.getShareToken());
        
        return response;
    }
}
