package com.oss.file.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oss.file.system.dto.FileShareResponse;
import com.oss.file.system.entity.FileShare;

import java.util.List;

/**
 * 文件分享业务接口
 */
public interface FileShareService extends IService<FileShare> {

    /**
     * 创建分享链接
     */
    FileShareResponse createShare(Long fileId, Long userId, Long expireTime, Integer maxDownloads);

    /**
     * 获取分享信息
     */
    FileShareResponse getShareInfo(String shareToken);

    /**
     * 检查分享链接是否有效
     */
    boolean isShareValid(String shareToken);

    /**
     * 增加分享的下载计数（原子操作）
     */
    boolean incrementDownloadCount(String shareToken);

    /**
     * 原子校验并扣减下载次数：成功返回含 fileId 的分享信息，失败返回 null。
     * 校验与扣减合并为一条 UPDATE，避免并发突破下载次数限制。
     */
    FileShareResponse consumeShare(String shareToken);

    /**
     * 分页获取用户的分享列表
     */
    List<FileShareResponse> getUserShares(Long userId, int page, int size);

    /**
     * 删除分享链接（所有者或管理员）
     */
    boolean deleteShare(Long shareId, Long userId);
}
