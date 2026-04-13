package com.oss.file.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统日志表
 * 记录所有的操作日志，包括上传、下载、分享、删除等
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_log")
public class SysLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型：UPLOAD/DOWNLOAD/SHARE/DELETE/LOGIN等
     */
    private String operation;

    /**
     * 资源类型：FILE/SHARE等
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 操作状态：SUCCESS/FAIL
     */
    private String status;

    /**
     * 错误信息（操作失败时记录）
     */
    private String errorMsg;

    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
