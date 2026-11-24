package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.system.dto.FileCheckResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.entity.FileShare;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.FileInfoMapper;
import com.oss.file.system.mapper.FileShareMapper;
import com.oss.file.system.mapper.SysUserMapper;
import com.oss.file.system.service.FileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 文件业务实现：秒传、安全落盘、越权校验、分页查询。
 */
@Service
@Slf4j
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    /** 扩展名只允许字母数字，长度 1~10，杜绝路径穿越字符 */
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^\\.[A-Za-z0-9]{1,10}$");

    /** 明确禁止的可执行/脚本类型 */
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".jsp", ".jspx", ".php", ".php5", ".exe", ".bat", ".cmd", ".sh",
            ".js", ".jar", ".war", ".htaccess"
    );

    private static final String SHARE_CACHE_PREFIX = "file:share:";

    @Value("${file.storage.path:D:/file-storage}")
    private String storageBasePath;

    private final FileShareMapper fileShareMapper;
    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate redisTemplate;

    public FileInfoServiceImpl(FileShareMapper fileShareMapper,
                               SysUserMapper sysUserMapper,
                               StringRedisTemplate redisTemplate) {
        this.fileShareMapper = fileShareMapper;
        this.sysUserMapper = sysUserMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 秒传检查：按「当前用户 + 哈希 + 大小」判断，避免跨用户误命中他人文件。
     */
    @Override
    public FileCheckResponse checkFileExists(String fileHash, String fileName, Long fileSize, Long userId) {
        FileCheckResponse response = new FileCheckResponse();
        try {
            FileInfo existing = findExisting(fileHash, fileSize, userId);
            boolean usable = existing != null && physicalFileExists(existing.getFilePath());
            response.setExists(existing != null);
            response.setCanSecondUpload(usable);
            response.setFileId(usable ? existing.getId() : null);
            response.setMessage(usable ? "文件已存在，可以秒传" : "文件不存在或物理文件已丢失，需要正常上传");
            if (usable) {
                log.info("秒传命中: user={}, hash={}", userId, fileHash);
            }
            return response;
        } catch (Exception e) {
            log.error("检查文件是否存在异常", e);
            throw new BusinessException("检查文件异常");
        }
    }

    /**
     * 保存文件：先写临时文件，再写库，最后原子改名；任一步失败都会清理临时文件。
     * 物理文件名带用户前缀，避免不同用户上传相同内容时互相覆盖。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo saveUploadedFile(MultipartFile file, String fileHash, Long uploadUserId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        String extension = safeExtension(file.getOriginalFilename());
        Path root = Paths.get(storageBasePath).toAbsolutePath().normalize();
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path storageDir = root.resolve(dateFolder).normalize();
        Path target = storageDir.resolve(uploadUserId + "_" + fileHash + extension).normalize();

        // 防御性校验：任何情况下都不得写出存储根目录
        if (!target.startsWith(root)) {
            throw new BusinessException("非法的文件存储路径");
        }

        FileInfo existing = findExisting(fileHash, file.getSize(), uploadUserId);
        if (existing != null && physicalFileExists(existing.getFilePath())) {
            log.info("秒传命中（服务端去重）: user={}, hash={}", uploadUserId, fileHash);
            return existing;
        }

        Files.createDirectories(storageDir);
        Path tempFile = storageDir.resolve(fileHash + "-" + UUID.randomUUID().toString().replace("-", "") + ".part");
        try {
            file.transferTo(tempFile.toFile());

            FileInfo fileInfo;
            if (existing != null) {
                // 活跃记录存在但物理文件丢失：更新元数据并重新落盘，不新增行
                fileInfo = existing;
                applyUploadMeta(fileInfo, file, fileHash, target, uploadUserId);
                baseMapper.updateById(fileInfo);
            } else {
                // 同一用户曾删除过该文件：复活原记录，避免唯一键冲突
                FileInfo deleted = baseMapper.selectDeletedByHash(uploadUserId, fileHash);
                if (deleted != null) {
                    fileInfo = deleted;
                    applyUploadMeta(fileInfo, file, fileHash, target, uploadUserId);
                    baseMapper.updateById(fileInfo);
                } else {
                    fileInfo = new FileInfo();
                    applyUploadMeta(fileInfo, file, fileHash, target, uploadUserId);
                    try {
                        baseMapper.insert(fileInfo);
                    } catch (DuplicateKeyException dup) {
                        // 并发上传相同内容：删除本次临时文件，复用已入库记录
                        Files.deleteIfExists(tempFile);
                        FileInfo winner = findExisting(fileHash, file.getSize(), uploadUserId);
                        if (winner != null && physicalFileExists(winner.getFilePath())) {
                            log.info("并发重复上传已幂等处理: hash={}", fileHash);
                            return winner;
                        }
                        throw new BusinessException("文件已存在，请重试");
                    }
                }
            }

            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                // 部分文件系统不支持 ATOMIC_MOVE，退化为普通覆盖
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("文件已保存: {}", target);
            return fileInfo;
        } catch (BusinessException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            log.error("保存文件失败", e);
            throw new BusinessException("保存文件失败");
        }
    }

    /** 把本次上传的元数据写入实体，复活/更新/新增共用 */
    private void applyUploadMeta(FileInfo fileInfo, MultipartFile file, String fileHash,
                                 Path target, Long uploadUserId) {
        fileInfo.setFileHash(fileHash);
        fileInfo.setFileName(sanitizeFileName(file.getOriginalFilename()));
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFilePath(target.toString());
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setUploadUserId(uploadUserId);
        fileInfo.setUploadTime(LocalDateTime.now());
        fileInfo.setIsDeleted(0);
        fileInfo.setDeleteTime(null);
    }

    /**
     * 删除文件：校验归属 → 失效分享并清缓存 → 逻辑删除 → 删除物理文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long fileId, Long userId) {
        FileInfo fileInfo = baseMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        if (!canAccess(fileId, userId)) {
            throw new BusinessException(403, "无权删除此文件");
        }

        LambdaUpdateWrapper<FileShare> shareUpdate = new LambdaUpdateWrapper<>();
        shareUpdate.eq(FileShare::getFileId, fileId)
                   .set(FileShare::getIsActive, 0)
                   .set(FileShare::getUpdateTime, LocalDateTime.now());
        fileShareMapper.update(null, shareUpdate);

        LambdaQueryWrapper<FileShare> shareQuery = new LambdaQueryWrapper<>();
        shareQuery.eq(FileShare::getFileId, fileId);
        for (FileShare share : fileShareMapper.selectList(shareQuery)) {
            try {
                redisTemplate.delete(SHARE_CACHE_PREFIX + share.getShareToken());
            } catch (Exception e) {
                log.warn("清理分享缓存失败: {}", share.getShareToken());
            }
        }

        FileInfo updateFile = new FileInfo();
        updateFile.setId(fileId);
        updateFile.setIsDeleted(1);
        updateFile.setDeleteTime(LocalDateTime.now());
        baseMapper.updateById(updateFile);

        try {
            Path root = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path filePath = Paths.get(fileInfo.getFilePath()).normalize();
            if (filePath.startsWith(root)) {
                Files.deleteIfExists(filePath);
                log.info("物理文件已删除: {}", filePath);
            }
        } catch (IOException e) {
            // DB 已逻辑删除，物理文件残留可在清理任务中处理，不影响本次操作
            log.warn("物理文件删除失败: {}", fileInfo.getFilePath());
        }

        log.info("文件记录已逻辑删除，FileID: {}", fileId);
        return true;
    }

    /**
     * 归属校验：所有者或管理员。
     */
    @Override
    public boolean canAccess(Long fileId, Long userId) {
        FileInfo fileInfo = baseMapper.selectById(fileId);
        if (fileInfo == null || userId == null) {
            return false;
        }
        if (userId.equals(fileInfo.getUploadUserId())) {
            return true;
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @Override
    public FileInfo getFileDetail(Long fileId) {
        return baseMapper.selectById(fileId);
    }

    /**
     * 分页查询用户文件，避免文件量大时全量返回。
     */
    @Override
    public List<FileInfo> getUserFiles(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<FileInfo> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUploadUserId, userId)
               .orderByDesc(FileInfo::getUploadTime);
        return baseMapper.selectPage(pageParam, wrapper).getRecords();
    }

    private FileInfo findExisting(String fileHash, Long fileSize, Long userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileHash, fileHash)
               .eq(FileInfo::getUploadUserId, userId);
        if (fileSize != null) {
            wrapper.eq(FileInfo::getFileSize, fileSize);
        }
        return baseMapper.selectOne(wrapper);
    }

    private boolean physicalFileExists(String path) {
        if (path == null) {
            return false;
        }
        try {
            Path root = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path filePath = Paths.get(path).normalize();
            return filePath.startsWith(root) && Files.isRegularFile(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 扩展名净化：只保留末尾一段且必须是安全的字母数字组合，禁止脚本/可执行文件。
     */
    private String safeExtension(String filename) {
        if (filename == null) {
            return "";
        }
        String base = filename.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot <= 0 || dot == base.length() - 1) {
            return "";
        }
        String ext = base.substring(dot).toLowerCase(Locale.ROOT);
        if (!SAFE_EXTENSION.matcher(ext).matches()) {
            return "";
        }
        if (FORBIDDEN_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不允许上传该类型文件: " + ext);
        }
        return ext;
    }

    /**
     * 文件名净化：去掉路径前缀和控制字符，防止响应头注入。
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String clean = name.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1);
        clean = clean.replaceAll("[\\r\\n\\t\"']", "_");
        return clean.length() > 200 ? clean.substring(0, 200) : clean;
    }
}
