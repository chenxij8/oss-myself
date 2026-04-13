package com.oss.file.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.SysUserMapper;
import com.oss.file.system.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}
