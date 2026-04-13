package com.oss.file.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分享参数DTO
 * 用于创建/更新分享链接
 */
@Data
public class FileShareRequest {
    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 过期时间（可选）
     */
    private LocalDateTime expireTime;

    /**
     * 最大下载次数（可选，NULL表示无限）
     */
    private Integer maxDownloads;
}
