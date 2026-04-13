package com.oss.file.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件检查响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCheckResponse {
    /**
     * 文件是否存在
     */
    private Boolean exists;

    /**
     * 文件ID（如果存在）
     */
    private Long fileId;

    /**
     * 是否可以秒传
     */
    private Boolean canSecondUpload;

    /**
     * 消息
     */
    private String message;
}
