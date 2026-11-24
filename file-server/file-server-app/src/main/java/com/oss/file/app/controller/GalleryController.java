package com.oss.file.app.controller;

import com.oss.file.app.security.SecurityUtils;
import com.oss.file.app.util.RangeSupport;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.system.dto.GalleryAssetResponse;
import com.oss.file.system.entity.GalleryAsset;
import com.oss.file.system.service.GalleryAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 图床控制器：上传、列表、统计、在线预览（含 Range）、原始流、删除。
 */
@RestController
@RequestMapping("/api/gallery")
@Slf4j
public class GalleryController {

    private final GalleryAssetService galleryService;

    public GalleryController(GalleryAssetService galleryService) {
        this.galleryService = galleryService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<GalleryAssetResponse>> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            GalleryAssetResponse response = galleryService.upload(file, userId);
            return ResponseEntity.ok(ApiResponse.success("上传成功", response));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (IOException e) {
            log.error("图床上传 IO 异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        } catch (Exception e) {
            log.error("图床上传异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryAssetResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String mediaType,
            Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            return ResponseEntity.ok(ApiResponse.success(galleryService.listAssets(userId, page, size, mediaType)));
        } catch (Exception e) {
            log.error("获取图床列表异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            return ResponseEntity.ok(ApiResponse.success(galleryService.stats(userId)));
        } catch (Exception e) {
            log.error("获取图床统计异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /** 内联预览：图片/视频在浏览器直接打开，支持 Range 拖动 */
    @GetMapping({"/preview/{id}", "/raw/{id}"})
    public ResponseEntity<?> preview(@PathVariable Long id,
                                     @RequestHeader(value = "Range", required = false) String range,
                                     @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        try {
            GalleryAsset asset = galleryService.getAsset(id);
            if (asset == null) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get(asset.getStoredPath()).normalize();
            MediaType mediaType = mediaType(asset);
            return RangeSupport.respond(filePath, range, mediaType, true, asset.getFileName());
        } catch (IOException e) {
            log.error("图床预览 IO 异常, id={}", id, e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("图床预览异常, id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = SecurityUtils.getUserId(authentication);
            galleryService.deleteAsset(id, userId);
            return ResponseEntity.ok(ApiResponse.success("素材已删除"));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("删除图床素材异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    private MediaType mediaType(GalleryAsset asset) {
        try {
            if (asset.getMimeType() != null && !asset.getMimeType().isBlank()) {
                return MediaType.parseMediaType(asset.getMimeType());
            }
        } catch (Exception ignored) {
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
