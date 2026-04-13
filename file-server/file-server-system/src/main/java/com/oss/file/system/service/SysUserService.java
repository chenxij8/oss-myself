package com.oss.file.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oss.file.system.entity.SysUser;

/**
 * 用户业务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 通过用户名获取用户
     * @param username 用户名
     * @return 用户对象
     */
    SysUser getUserByUsername(String username);

    /**
     * 验证用户密码
     * @param userId 用户ID
     * @param rawPassword 原始密码
     * @return 验证结果
     */
    boolean validatePassword(Long userId, String rawPassword);

    /**
     * 更新最后登录时间
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);
}
