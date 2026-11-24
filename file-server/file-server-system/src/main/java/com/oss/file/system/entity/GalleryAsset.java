package com.oss.file.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图床素材：图片/视频独立存储，与 file_info 完全分离。
 */
@Data
@TableName("gallery_asset")
public class GalleryAsset implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 素材所属用户 */
    private Long ownerUserId;

    /** SHA-256，用于秒传去重 */
    private String fileHash;

    /** 原始文件名（净化后） */
    private String fileName;

    /** 物理存储绝对路径 */
    private String storedPath;

    /** 服务端嗅探得到的真实 MIME */
    private String mimeType;

    /** IMAGE / VIDEO */
    private String mediaType;

    private Long fileSize;

    /** 图片宽高，视频为 null */
    private Integer width;
    private Integer height;

    /** 视频时长（秒），未知为 null */
    private Long duration;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deleteTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
