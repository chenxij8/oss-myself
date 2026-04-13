package com.oss.file.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oss.file.system.dto.FileShareResponse;
import com.oss.file.system.entity.FileShare;

/**
 * 文件分享业务接口
 */
public interface FileShareService extends IService<FileShare> {

    /**
     * 创建分享链接
     * @param fileId 文件ID
     * @param userId 分享发起者ID
     * @param expireTime 过期时间（毫秒时间戳，可为null表示不过期）
     * @param maxDownloads 最大下载次数（可为null表示无限）
     * @return 分享信息
     */
    FileShareResponse createShare(Long fileId, Long userId, Long expireTime, Integer maxDownloads);

    /**
     * 获取分享信息
     * @param shareToken 分享令牌
     * @return 分享信息
     */
    FileShareResponse getShareInfo(String shareToken);

    /**
     * 检查分享链接是否有效
     * @param shareToken 分享令牌
     * @return 是否有效
     */
    boolean isShareValid(String shareToken);

    /**
     * 增加分享的下载计数
     * @param shareToken 分享令牌
     * @return 是否成功
     */
    boolean incrementDownloadCount(String shareToken);

    /**
     * 删除分享链接
     * @param shareId 分享ID
     * @param userId 操作用户ID
     * @return 是否删除成功
     */
    boolean deleteShare(Long shareId, Long userId);
}
