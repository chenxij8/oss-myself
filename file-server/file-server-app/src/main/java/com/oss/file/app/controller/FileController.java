package com.oss.file.app.controller;

import com.oss.file.app.security.SecurityUtils;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.common.util.FileHashUtil;
import com.oss.file.system.dto.FileCheckRequest;
import com.oss.file.system.dto.FileCheckResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.service.FileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文件控制器：上传、下载、秒传校验、分页列表。
 */
@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {

    private final FileInfoService fileInfoService;

    public FileController(FileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    /** 秒传校验（带上当前用户，避免跨用户误命中） */
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<FileCheckResponse>> checkFile(
            @RequestBody FileCheckRequest checkRequest,
            Authentication authentication) {
        try {
            if (checkRequest == null || checkRequest.getFileHash() == null) {
                throw new BusinessException("文件哈希值不能为空");
            }
            Long userId = SecurityUtils.getUserId(authentication);
            FileCheckResponse response = fileInfoService.checkFileExists(
                    checkRequest.getFileHash(), checkRequest.getFileName(), checkRequest.getFileSize(), userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("检查文件异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 上传文件：流式计算哈希，避免大文件一次性加载进内存 */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileInfo>> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("上传文件不能为空");
            }
            Long userId = SecurityUtils.getUserId(authentication);
            String fileHash;
            try (InputStream inputStream = file.getInputStream()) {
                fileHash = FileHashUtil.calculateSHA256(inputStream);
            }

            log.info("文件上传开始，文件名: {}, 大小: {}, Hash: {}",
                    file.getOriginalFilename(), file.getSize(), fileHash);
            FileInfo fileInfo = fileInfoService.saveUploadedFile(file, fileHash, userId);
            log.info("文件上传成功，FileID: {}", fileInfo.getId());
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", fileInfo));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 获取文件详情（仅所有者或管理员） */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileInfo>> getFileInfo(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            if (!fileInfoService.canAccess(fileId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.forbidden());
            }
            FileInfo fileInfo = fileInfoService.getFileDetail(fileId);
            if (fileInfo == null) {
                throw new BusinessException("文件不存在");
            }
            return ResponseEntity.ok(ApiResponse.success(fileInfo));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("获取文件详情异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 下载文件（仅所有者或管理员），流式返回 */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            FileInfo fileInfo = fileInfoService.getFileDetail(fileId);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            if (!fileInfoService.canAccess(fileId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path filePath = Paths.get(fileInfo.getFilePath()).normalize();
            if (!Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileInfo.getFileName()))
                    .contentType(contentType(fileInfo.getMimeType()))
                    .contentLength(fileInfo.getFileSize() == null ? Files.size(filePath) : fileInfo.getFileSize())
                    .body(resource);
        } catch (IOException e) {
            log.error("下载文件异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 删除文件（仅所有者或管理员） */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Object>> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            fileInfoService.deleteFile(fileId, userId);
            log.info("文件已删除，FileID: {}", fileId);
            return ResponseEntity.ok(ApiResponse.success("文件已删除"));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("删除文件异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 分页获取用户文件列表 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileInfo>>> getFileList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            List<FileInfo> fileList = fileInfoService.getUserFiles(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(fileList));
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 对文件名做 URL 编码，防止响应头注入并支持中文名 */
    static String contentDisposition(String fileName) {
        String safe = (fileName == null || fileName.isBlank()) ? "download" : fileName.replaceAll("[\\r\\n\"]", "_");
        return "attachment; filename*=UTF-8''" + URLEncoder.encode(safe, StandardCharsets.UTF_8);
    }

    static MediaType contentType(String mimeType) {
        try {
            return mimeType == null || mimeType.isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
