package com.oss.file.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oss.file.system.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息Mapper接口
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}
