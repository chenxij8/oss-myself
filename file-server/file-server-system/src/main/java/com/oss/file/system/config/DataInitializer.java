package com.oss.file.system.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器：仅在 admin 账号不存在时创建一次。
 * 不再每次启动重置密码，避免生产环境被弱口令后门覆盖。
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /** 生产环境请通过环境变量 APP_ADMIN_INITIAL_PASSWORD 覆盖 */
    @Value("${app.admin.initial-password:admin123}")
    private String initialPassword;

    public DataInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", "admin");
        SysUser existingUser = sysUserMapper.selectOne(wrapper);
        if (existingUser != null) {
            log.info("管理员账号已存在，跳过初始化（密码不会被重置）");
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        if (sysUserMapper.insert(admin) > 0) {
            log.info("默认管理员账号创建成功: admin（初始密码来自配置 app.admin.initial-password，请尽快修改）");
        } else {
            log.error("默认管理员账号创建失败");
        }
    }
}
