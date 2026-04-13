package com.oss.file.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件分享响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileShareResponse {
    /**
     * 分享ID
     */
    private Long id;

    /**
     * 分享令牌（直链URL中使用）
     */
    private String shareToken;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 最大下载次数
     */
    private Integer maxDownloads;

    /**
     * 当前下载次数
     */
    private Integer currentDownloads;

    /**
     * 分享链接（直链）
     */
    private String shareUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
