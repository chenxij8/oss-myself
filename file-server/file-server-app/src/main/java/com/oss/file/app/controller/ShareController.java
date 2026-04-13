package com.oss.file.app.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 文件分享控制器
 * 处理分享链接的生成、查询、下载等操作
 */
@RestController
@RequestMapping("/api/shares")
@Slf4j
public class ShareController {

    private final FileShareService fileShareService;
    private final FileInfoService fileInfoService;

    /**
     * 限流配置：每个IP每分钟最多请求次数
     */
    private static final int RATE_LIMIT_PER_MINUTE = 60;

    public ShareController(FileShareService fileShareService, FileInfoService fileInfoService) {
        this.fileShareService = fileShareService;
        this.fileInfoService = fileInfoService;
    }

    /**
     * 创建分享链接
     * @param shareRequest 分享请求（包含文件ID、过期时间、下载次数限制）
     * @param authentication 认证信息
     * @return 分享信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FileShareResponse>> createShare(
            @RequestBody FileShareRequest shareRequest,
            Authentication authentication) {
        try {
            if (shareRequest == null || shareRequest.getFileId() == null) {
                throw new BusinessException("文件ID不能为空");
            }

            String userId = (String) authentication.getPrincipal();

            // 转换过期时间为毫秒时间戳
            Long expireTime = null;
            if (shareRequest.getExpireTime() != null) {
                expireTime = shareRequest.getExpireTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }

            // 创建分享链接
            FileShareResponse response = fileShareService.createShare(
                    shareRequest.getFileId(),
                    Long.parseLong(userId),
                    expireTime,
                    shareRequest.getMaxDownloads()
            );

            log.info("分享链接已创建，ShareId: {}, Token: {}", response.getId(), response.getShareToken());
            return ResponseEntity.ok(ApiResponse.success("分享链接创建成功", response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("创建分享链接异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取分享信息（公开接口，无需认证）
     * @param shareToken 分享令牌
     * @return 分享信息
     */
    @GetMapping("/info/{shareToken}")
    public ResponseEntity<ApiResponse<FileShareResponse>> getShareInfo(
            @PathVariable String shareToken) {
        try {
            FileShareResponse response = fileShareService.getShareInfo(shareToken);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("获取分享信息异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 通过分享链接下载文件（公开接口，无需认证）
     * 采用流式传输，不映射为静态资源
     * @param shareToken 分享令牌
     * @return 文件流
     */
    @GetMapping("/download/{shareToken}")
    public ResponseEntity<InputStreamResource> downloadFileByShare(
            @PathVariable String shareToken,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        try {
            // 检查分享链接是否有效
            if (!fileShareService.isShareValid(shareToken)) {
                log.warn("分享链接无效或已过期，Token: {}", shareToken);
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(null);
            }

            // 获取分享信息
            FileShareResponse shareInfo = fileShareService.getShareInfo(shareToken);
            if (shareInfo == null) {
                return ResponseEntity.notFound().build();
            }

            // 获取文件信息
            FileInfo fileInfo = fileInfoService.getFileDetail(shareInfo.getId());
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查文件是否存在
            File file = new File(fileInfo.getFilePath());
            if (!file.exists() || !file.isFile()) {
                log.error("物理文件不存在，FilePath: {}", fileInfo.getFilePath());
                return ResponseEntity.notFound().build();
            }

            // 增加下载计数
            fileShareService.incrementDownloadCount(shareToken);

            // 打开文件流
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fileInputStream);

            log.info("分享文件下载，Token: {}, FileName: {}, ClientIP: {}", 
                    shareToken, fileInfo.getFileName(), clientIp);

            // 返回文件流
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileInfo.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, fileInfo.getMimeType())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()))
                    .body(resource);
        } catch (IOException e) {
            log.error("流式传输文件异常", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("下载分享文件异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户的分享列表
     * @param authentication 认证信息
     * @return 分享列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getUserShares(Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            // TODO: 实现获取用户分享列表的逻辑
            return ResponseEntity.ok(ApiResponse.success("获取分享列表成功", null));
        } catch (Exception e) {
            log.error("获取分享列表异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 删除分享链接
     * @param shareId 分享ID
     * @param authentication 认证信息
     * @return 删除结果
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<ApiResponse<Object>> deleteShare(
            @PathVariable Long shareId,
            Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            fileShareService.deleteShare(shareId, Long.parseLong(userId));

            log.info("分享链接已删除，ShareId: {}", shareId);
            return ResponseEntity.ok(ApiResponse.success("分享链接已删除"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("删除分享链接异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }
}
