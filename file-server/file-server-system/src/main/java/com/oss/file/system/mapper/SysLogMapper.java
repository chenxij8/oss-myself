package com.oss.file.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oss.file.system.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统日志Mapper接口
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
