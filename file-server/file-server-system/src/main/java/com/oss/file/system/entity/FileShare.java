package com.oss.file.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分享表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("file_share")
public class FileShare implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID，关联到file_info表
     */
    private Long fileId;

    /**
     * 分享令牌，使用UUID算法生成，不可预测的令牌
     */
    private String shareToken;

    /**
     * 分享发起者ID
     */
    private Long shareUserId;

    /**
     * 过期时间，NULL表示永不过期
     */
    private LocalDateTime expireTime;

    /**
     * 最大下载次数，NULL表示无限制
     */
    private Integer maxDownloads;

    /**
     * 当前下载次数
     */
    private Integer currentDownloads;

    /**
     * 是否活跃：1-活跃, 0-已失效
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
