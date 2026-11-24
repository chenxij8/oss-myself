package com.oss.file.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oss.file.system.entity.SysUser;

import java.util.List;

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

    /** 分页查询用户（管理端） */
    List<SysUser> listUsers(int page, int size, String keyword);

    /** 统计用户数 */
    long countUsers();

    /** 统计启用状态的管理员数量 */
    long countActiveAdmins();

    /** 修改用户状态（0禁用/1启用） */
    boolean updateUserStatus(Long userId, Integer status);

    /** 修改用户角色（ADMIN/USER） */
    boolean updateUserRole(Long userId, String role);

    /** 重置用户密码 */
    boolean resetPassword(Long userId, String rawPassword);

    /** 脱敏：清除密码字段 */
    void maskPassword(SysUser user);
}
