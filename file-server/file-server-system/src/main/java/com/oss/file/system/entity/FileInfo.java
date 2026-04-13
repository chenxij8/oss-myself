package com.oss.file.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("file_info")
public class FileInfo implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件SHA-256哈希值，唯一标识一个文件
     */
    private String fileHash;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 本地存储路径，物理隔离的绝对路径
     */
    private String filePath;

    /**
     * MIME类型（如 application/pdf, image/png等）
     */
    private String mimeType;

    /**
     * 上传用户ID
     */
    private Long uploadUserId;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 逻辑删除标志：0-未删除, 1-已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;
}
