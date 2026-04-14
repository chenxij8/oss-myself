package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.util.FileHashUtil;
import com.oss.file.system.dto.FileCheckResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.mapper.FileInfoMapper;
import com.oss.file.system.service.FileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文件业务实现类
 * 负责文件的上传、检查、删除等操作
 */
@Service
@Slf4j
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    /**
     * 文件存储根目录路径（从配置文件读取）
     */
    @Value("${file.storage.path:D:/file-storage}")
    private String storageBasePath;

    /**
     * 检查文件是否存在（通过哈希值实现秒传）
     */
    @Override
    public FileCheckResponse checkFileExists(String fileHash, String fileName, Long fileSize) {
        try {
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getFileHash, fileHash);
            FileInfo existingFile = baseMapper.selectOne(queryWrapper);

            FileCheckResponse response = new FileCheckResponse();
            if (existingFile != null) {
                response.setExists(true);
                response.setFileId(existingFile.getId());
                response.setCanSecondUpload(true);
                response.setMessage("文件已存在，可以秒传");
                log.info("检测到重复文件：{}, Hash: {}", fileName, fileHash);
            } else {
                response.setExists(false);
                response.setCanSecondUpload(false);
                response.setMessage("文件不存在，需要正常上传");
            }
            return response;
        } catch (Exception e) {
            log.error("检查文件是否存在异常", e);
            throw new BusinessException("检查文件异常：" + e.getMessage());
        }
    }

    /**
     * 保存上传的文件
     * 文件被存储在物理隔离的目录中，不映射为静态资源
     */
    @Override
    public FileInfo saveUploadedFile(MultipartFile file, String fileHash, Long uploadUserId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        try {
            // 生成存储目录：按日期分类
            String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String storagePath = storageBasePath + File.separator + dateFolder;
            Path storageDir = Paths.get(storagePath);

            // 创建目录
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            // 生成存储文件名（使用哈希值作为文件名保证唯一性）
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String storedFileName = fileHash + fileExtension;
            Path filePath = storageDir.resolve(storedFileName);

            // 检查该文件是否已存在
            FileInfo existingFile = null;
            if (Files.exists(filePath)) {
                LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FileInfo::getFileHash, fileHash);
                existingFile = baseMapper.selectOne(queryWrapper);
            }

            // 如果文件已存在，直接返回（秒传）
            if (existingFile != null) {
                log.info("文件已存在，执行秒传: {}", fileHash);
                return existingFile;
            }

            // 保存文件到磁盘
            file.transferTo(filePath.toFile());
            log.info("文件已保存到: {}", filePath.toAbsolutePath());

            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileHash(fileHash);
            fileInfo.setFileName(file.getOriginalFilename());
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFilePath(filePath.toAbsolutePath().toString());
            fileInfo.setMimeType(file.getContentType());
            fileInfo.setUploadUserId(uploadUserId);
            fileInfo.setUploadTime(LocalDateTime.now());
            fileInfo.setIsDeleted(0);

            baseMapper.insert(fileInfo);
            log.info("文件信息已保存，FileID: {}, Hash: {}", fileInfo.getId(), fileHash);

            return fileInfo;
        } catch (IOException e) {
            log.error("保存文件异常: {}", e.getMessage(), e);
            throw new BusinessException("保存文件失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件及其分享记录
     */
    @Override
    public boolean deleteFile(Long fileId, Long userId) {
        try {
            FileInfo fileInfo = baseMapper.selectById(fileId);
            if (fileInfo == null) {
                throw new BusinessException("文件不存在");
            }

            // 验证删除权限（仅文件上传者或管理员可删除）
            // 这里简单处理，实际应该在Controller通过权限验证

            // 删除物理文件
            Path filePath = Paths.get(fileInfo.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("物理文件已删除: {}", filePath);
            }

            // 逻辑删除数据库记录
            FileInfo updateFile = new FileInfo();
            updateFile.setId(fileId);
            updateFile.setIsDeleted(1);
            updateFile.setDeleteTime(LocalDateTime.now());
            baseMapper.updateById(updateFile);

            log.info("文件记录已逻辑删除，FileID: {}", fileId);
            return true;
        } catch (IOException e) {
            log.error("删除文件异常", e);
            throw new BusinessException("删除文件失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件详情
     */
    @Override
    public FileInfo getFileDetail(Long fileId) {
        return baseMapper.selectById(fileId);
    }

    /**
     * 获取用户上传的文件列表
     */
    @Override
    public List<FileInfo> getUserFiles(Long userId) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUploadUserId, userId)
                    .eq(FileInfo::getIsDeleted, 0)
                    .orderByDesc(FileInfo::getUploadTime);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}
