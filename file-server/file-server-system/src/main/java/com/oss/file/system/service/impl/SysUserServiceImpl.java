package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.SysUserMapper;
import com.oss.file.system.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户业务实现类
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 通过用户名获取用户
     */
    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 验证用户密码
     */
    @Override
    public boolean validatePassword(Long userId, String rawPassword) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * 更新最后登录时间
     */
    @Override
    public void updateLastLoginTime(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastLogin(LocalDateTime.now());
        baseMapper.updateById(user);
    }

    @Override
    public List<SysUser> listUsers(int page, int size, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<SysUser> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getEmail, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        List<SysUser> users = baseMapper.selectPage(pageParam, wrapper).getRecords();
        users.forEach(this::maskPassword);
        return users;
    }

    @Override
    public long countUsers() {
        return baseMapper.selectCount(null);
    }

    @Override
    public long countActiveAdmins() {
        return baseMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "ADMIN")
                .eq(SysUser::getStatus, 1));
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能是 0（禁用）或 1（启用）");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(user) > 0;
    }

    @Override
    public boolean updateUserRole(Long userId, String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!"ADMIN".equals(normalized) && !"USER".equals(normalized)) {
            throw new BusinessException("角色只能是 ADMIN 或 USER");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRole(normalized);
        user.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(user) > 0;
    }

    @Override
    public boolean resetPassword(Long userId, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于 6 位");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(user) > 0;
    }

    @Override
    public void maskPassword(SysUser user) {
        if (user != null) {
            user.setPassword(null);
        }
    }
}
