package com.oss.file.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oss.file.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
