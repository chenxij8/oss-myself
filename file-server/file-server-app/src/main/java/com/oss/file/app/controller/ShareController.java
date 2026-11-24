package com.oss.file.app.controller;

import com.oss.file.app.security.SecurityUtils;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.system.dto.FileShareRequest;
import com.oss.file.system.dto.FileShareResponse;
import com.oss.file.system.entity.FileInfo;
import com.oss.file.system.service.FileInfoService;
import com.oss.file.system.service.FileShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;

/**
 * 分享控制器：创建、查询、公开下载、列表、删除。
 */
@RestController
@RequestMapping("/api/shares")
@Slf4j
public class ShareController {

    private final FileShareService fileShareService;
    private final FileInfoService fileInfoService;

    public ShareController(FileShareService fileShareService, FileInfoService fileInfoService) {
        this.fileShareService = fileShareService;
        this.fileInfoService = fileInfoService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileShareResponse>> createShare(
            @RequestBody FileShareRequest shareRequest,
            Authentication authentication) {
        try {
            if (shareRequest == null || shareRequest.getFileId() == null) {
                throw new BusinessException("文件ID不能为空");
            }
            Long userId = SecurityUtils.getUserId(authentication);
            Long expireTime = shareRequest.getExpireTime() == null ? null
                    : shareRequest.getExpireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            FileShareResponse response = fileShareService.createShare(
                    shareRequest.getFileId(), userId, expireTime, shareRequest.getMaxDownloads());
            log.info("分享链接已创建，ShareId: {}, Token: {}", response.getId(), response.getShareToken());
            return ResponseEntity.ok(ApiResponse.success("分享链接创建成功", response));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("创建分享链接异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 分享信息（公开） */
    @GetMapping("/info/{shareToken}")
    public ResponseEntity<ApiResponse<FileShareResponse>> getShareInfo(@PathVariable String shareToken) {
        try {
            FileShareResponse response = fileShareService.getShareInfo(shareToken);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("获取分享信息异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 公开下载：一次原子操作完成「校验 + 扣减」，并用返回的 fileId 查询文件。
     * 修复了原来误把「分享ID」当「文件ID」导致下载错文件/404 的问题。
     */
    @GetMapping("/download/{shareToken}")
    public ResponseEntity<InputStreamResource> downloadFileByShare(
            @PathVariable String shareToken,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        try {
            FileShareResponse shareInfo = fileShareService.consumeShare(shareToken);
            if (shareInfo == null) {
                log.warn("分享链接无效/已过期/次数用尽，Token: {}", shareToken);
                return ResponseEntity.status(HttpStatus.GONE).build();
            }

            FileInfo fileInfo = fileInfoService.getFileDetail(shareInfo.getFileId());
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get(fileInfo.getFilePath()).normalize();
            if (!Files.isRegularFile(filePath)) {
                log.error("物理文件不存在，FilePath: {}", fileInfo.getFilePath());
                return ResponseEntity.notFound().build();
            }

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));
            log.info("分享文件下载，Token: {}, FileName: {}, ClientIP: {}",
                    shareToken, fileInfo.getFileName(), clientIp);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, FileController.contentDisposition(fileInfo.getFileName()))
                    .contentType(FileController.contentType(fileInfo.getMimeType()))
                    .contentLength(fileInfo.getFileSize() == null ? Files.size(filePath) : fileInfo.getFileSize())
                    .body(resource);
        } catch (IOException e) {
            log.error("流式传输文件异常", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("下载分享文件异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 分页获取用户分享列表（原来返回 null 的 TODO） */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileShareResponse>>> getUserShares(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            List<FileShareResponse> shares = fileShareService.getUserShares(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success("获取分享列表成功", shares));
        } catch (Exception e) {
            log.error("获取分享列表异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<ApiResponse<Object>> deleteShare(
            @PathVariable Long shareId,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            fileShareService.deleteShare(shareId, userId);
            log.info("分享链接已删除，ShareId: {}", shareId);
            return ResponseEntity.ok(ApiResponse.success("分享链接已删除"));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("删除分享链接异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }
}
