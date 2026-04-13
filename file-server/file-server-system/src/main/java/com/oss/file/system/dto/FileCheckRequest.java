package com.oss.file.system.dto;

import lombok.Data;

/**
 * 文件上传前检查请求DTO
 * 用于秒传功能：客户端先计算文件哈希值，发送给服务器判断是否存在
 */
@Data
public class FileCheckRequest {
    /**
     * 文件哈希值（SHA-256）
     */
    private String fileHash;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;
}
