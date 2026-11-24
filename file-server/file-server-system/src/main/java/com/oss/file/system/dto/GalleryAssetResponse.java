package com.oss.file.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图床素材响应：previewUrl 走内联预览，rawUrl 走原始字节（支持 Range）。
 */
@Data
public class GalleryAssetResponse {
    private Long id;
    private String fileName;
    private String fileHash;
    private String mimeType;
    /** IMAGE / VIDEO */
    private String mediaType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Long duration;
    private String previewUrl;
    private String rawUrl;
    private LocalDateTime createTime;
}
