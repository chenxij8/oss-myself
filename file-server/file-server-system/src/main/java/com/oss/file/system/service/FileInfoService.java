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
     * 检查文件是否存在（通过哈希值）
     * @param fileHash 文件哈希值
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return 检查结果
     */
    FileCheckResponse checkFileExists(String fileHash, String fileName, Long fileSize);

    /**
     * 保存上传的文件
     * @param file 多部分文件
     * @param fileHash 文件哈希值
     * @param uploadUserId 上传用户ID
     * @return 文件信息
     */
    FileInfo saveUploadedFile(MultipartFile file, String fileHash, Long uploadUserId) throws IOException;

    /**
     * 删除文件及其分享记录
     * @param fileId 文件ID
     * @param userId 操作用户ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId, Long userId);

    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileInfo getFileDetail(Long fileId);

    /**
     * 获取用户上传的文件列表
     * @param userId 用户ID
     * @return 文件列表
     */
    List<FileInfo> getUserFiles(Long userId);
}
