package com.oss.file.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oss.file.system.entity.GalleryAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GalleryAssetMapper extends BaseMapper<GalleryAsset> {

    /** 绕过逻辑删除，查询已删除记录用于重传复活 */
    @Select("SELECT * FROM gallery_asset WHERE owner_user_id = #{userId} AND file_hash = #{fileHash} "
            + "AND is_deleted = 1 LIMIT 1")
    GalleryAsset selectDeletedByHash(@Param("userId") Long userId, @Param("fileHash") String fileHash);
}
