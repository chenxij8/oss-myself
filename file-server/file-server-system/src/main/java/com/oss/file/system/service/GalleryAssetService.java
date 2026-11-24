package com.oss.file.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oss.file.system.dto.GalleryAssetResponse;
import com.oss.file.system.entity.GalleryAsset;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface GalleryAssetService extends IService<GalleryAsset> {

    /** 上传图床素材（图片/视频） */
    GalleryAssetResponse upload(MultipartFile file, Long userId) throws IOException;

    /** 分页列出当前用户素材，mediaType 可为 IMAGE/VIDEO/null */
    List<GalleryAssetResponse> listAssets(Long userId, int page, int size, String mediaType);

    /** 按 ID 获取素材（不做归属校验，公开预览用） */
    GalleryAsset getAsset(Long id);

    /** 统计当前用户素材数量与占用空间 */
    java.util.Map<String, Object> stats(Long userId);

    /** 删除（校验归属） */
    boolean deleteAsset(Long id, Long userId);

    /** 实体转响应 DTO */
    GalleryAssetResponse toResponse(GalleryAsset asset);
}
