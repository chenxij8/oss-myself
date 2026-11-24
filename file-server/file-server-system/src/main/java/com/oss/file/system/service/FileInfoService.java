package com.oss.file.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oss.file.system.dto.FileCheckResponse;
import com.oss.file.system.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件业务接口
 */
public interface FileInfoService extends IService<FileInfo> {

    /**
     * 检查当前用户是否已上传过相同文件（秒传）
     */
    FileCheckResponse checkFileExists(String fileHash, String fileName, Long fileSize, Long userId);

    /**
     * 保存上传的文件
     */
    FileInfo saveUploadedFile(MultipartFile file, String fileHash, Long uploadUserId) throws IOException;

    /**
     * 删除文件（仅文件所有者或管理员）
     */
    boolean deleteFile(Long fileId, Long userId);

    /**
     * 判断用户是否有权操作该文件（所有者或管理员）
     */
    boolean canAccess(Long fileId, Long userId);

    /**
     * 获取文件详情
     */
    FileInfo getFileDetail(Long fileId);

    /**
     * 分页获取用户上传的文件列表
     */
    List<FileInfo> getUserFiles(Long userId, int page, int size);
}
