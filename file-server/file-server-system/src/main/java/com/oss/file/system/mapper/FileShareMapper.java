package com.oss.file.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oss.file.system.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分享Mapper接口
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {
}
