package com.oss.file.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oss.file.system.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件信息Mapper接口
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 查询同一用户同一哈希「已逻辑删除」的记录，用于重传时复活原记录。
     * 直接使用原生 SQL 以绕过 MyBatis-Plus 自动追加的 is_deleted = 0 条件。
     */
    @Select("SELECT * FROM file_info WHERE upload_user_id = #{userId} AND file_hash = #{fileHash} "
            + "AND is_deleted = 1 LIMIT 1")
    FileInfo selectDeletedByHash(@Param("userId") Long userId, @Param("fileHash") String fileHash);
}
