package com.oss.file.app.controller;

import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.common.util.FileHashUtil;
import com.oss.file.system.dto.FileCheckRequest;
import com.oss.file.system.dto.FileCheckResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.service.FileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

/**
 * 文件控制器
 * 处理文件上传、下载、检查等操作
 */
@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {

    private final FileInfoService fileInfoService;

    public FileController(FileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    /**
     * 检查文件是否存在（秒传功能）
     * @param checkRequest 检查请求，包含文件哈希值
     * @param authentication 认证信息
     * @return 检查结果
     */
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<FileCheckResponse>> checkFile(
            @RequestBody FileCheckRequest checkRequest,
            Authentication authentication) {
        try {
            if (checkRequest == null || checkRequest.getFileHash() == null) {
                throw new BusinessException("文件哈希值不能为空");
            }

            FileCheckResponse response = fileInfoService.checkFileExists(
                    checkRequest.getFileHash(),
                    checkRequest.getFileName(),
                    checkRequest.getFileSize()
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("检查文件异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 上传文件
     * @param file 上传的多部分文件
     * @param authentication 认证信息
     * @return 文件信息
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileInfo>> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("上传文件不能为空");
            }

            // 获取当前用户ID
            String userId = (String) authentication.getPrincipal();

            // 计算文件哈希值
            String fileHash;
            try {
                fileHash = FileHashUtil.calculateSHA256(file.getBytes());
            } catch (NoSuchAlgorithmException e) {
                throw new BusinessException("计算文件哈希值异常");
            }

            log.info("文件上传开始，文件名: {}, 大小: {}, Hash: {}", 
                    file.getOriginalFilename(), file.getSize(), fileHash);

            // 保存文件
            FileInfo fileInfo = fileInfoService.saveUploadedFile(file, fileHash, Long.parseLong(userId));

            log.info("文件上传成功，FileID: {}", fileInfo.getId());
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", fileInfo));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @param authentication 认证信息
     * @return 文件信息
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileInfo>> getFileInfo(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            FileInfo fileInfo = fileInfoService.getFileDetail(fileId);
            if (fileInfo == null) {
                throw new BusinessException("文件不存在");
            }

            return ResponseEntity.ok(ApiResponse.success(fileInfo));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("获取文件详情异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 下载文件（仅限文件上传者）
     * @param fileId 文件ID
     * @param authentication 认证信息
     * @return 文件流
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            FileInfo fileInfo = fileInfoService.getFileDetail(fileId);

            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }

            // 验证权限（仅文件上传者可下载）
            if (!fileInfo.getUploadUserId().equals(Long.parseLong(userId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            File file = new File(fileInfo.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fileInputStream);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileInfo.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, fileInfo.getMimeType())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()))
                    .body(resource);
        } catch (IOException e) {
            log.error("下载文件异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除文件
     * @param fileId 文件ID
     * @param authentication 认证信息
     * @return 删除结果
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Object>> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            fileInfoService.deleteFile(fileId, Long.parseLong(userId));

            log.info("文件已删除，FileID: {}", fileId);
            return ResponseEntity.ok(ApiResponse.success("文件已删除"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("删除文件异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取用户文件列表
     * @param authentication 认证信息
     * @return 文件列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileInfo>>> getFileList(
            Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            List<FileInfo> fileList = fileInfoService.getUserFiles(Long.parseLong(userId));

            log.info("获取用户文件列表，用户ID: {}, 文件数量: {}", userId, fileList.size());
            return ResponseEntity.ok(ApiResponse.success(fileList));
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }
}
