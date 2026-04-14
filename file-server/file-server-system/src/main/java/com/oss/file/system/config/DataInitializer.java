package com.oss.file.system.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器 - 启动时检查并创建默认管理员账号
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 检查 admin 用户是否已存在
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", "admin");
        SysUser existingUser = sysUserMapper.selectOne(wrapper);

        if (existingUser == null) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());

            int rows = sysUserMapper.insert(admin);
            if (rows > 0) {
                log.info("✅ 默认管理员账号创建成功: admin / admin123");
            } else {
                log.error("❌ 默认管理员账号创建失败");
            }
        } else {
            // 开发环境：始终确保 admin 密码为 admin123
            SysUser updateUser = new SysUser();
            updateUser.setId(existingUser.getId());
            updateUser.setPassword(passwordEncoder.encode("admin123"));
            updateUser.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(updateUser);
            log.info("✅ admin 用户已存在，密码已重置为 admin123");
        }
    }
}
