package com.oss.file.app.controller;

import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.common.util.JwtUtil;
import com.oss.file.system.dto.LoginRequest;
import com.oss.file.system.dto.LoginResponse;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户登录、注册等认证相关的请求
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(SysUserService sysUserService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应（包含Token）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 参数校验
            if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                throw new BusinessException("用户名和密码不能为空");
            }

            // 根据用户名查询用户
            SysUser user = sysUserService.getUserByUsername(loginRequest.getUsername());
            if (user == null) {
                log.warn("登录失败：用户不存在，用户名: {}", loginRequest.getUsername());
                throw new BusinessException("用户名或密码错误");
            }

            // 检查用户状态
            if (user.getStatus() == 0) {
                log.warn("登录失败：用户已被禁用，用户名: {}", loginRequest.getUsername());
                throw new BusinessException("用户已被禁用");
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("登录失败：密码错误，用户名: {}", loginRequest.getUsername());
                throw new BusinessException("用户名或密码错误");
            }

            // 更新最后登录时间
            sysUserService.updateLastLoginTime(user.getId());

            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getId().toString());

            // 构建响应
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setAccessToken(token);
            loginResponse.setTokenType("Bearer");
            loginResponse.setExpiresIn(86400L); // 24小时
            loginResponse.setUserId(user.getId());
            loginResponse.setUsername(user.getUsername());
            loginResponse.setRole(user.getRole());

            log.info("用户登录成功，用户名: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("登录异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 用户注册（需要管理员权限）
     * @param sysUser 用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SysUser>> register(@RequestBody SysUser sysUser) {
        try {
            // 参数校验
            if (sysUser == null || sysUser.getUsername() == null || sysUser.getPassword() == null) {
                throw new BusinessException("用户名和密码不能为空");
            }

            // 检查用户名是否已存在
            SysUser existingUser = sysUserService.getUserByUsername(sysUser.getUsername());
            if (existingUser != null) {
                throw new BusinessException("用户名已存在");
            }

            // 加密密码
            sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
            
            // 设置默认值
            if (sysUser.getRole() == null) {
                sysUser.setRole("USER");
            }
            if (sysUser.getStatus() == null) {
                sysUser.setStatus(1);
            }

            // 保存用户
            sysUserService.save(sysUser);

            log.info("用户注册成功，用户名: {}", sysUser.getUsername());
            return ResponseEntity.ok(ApiResponse.success("注册成功", sysUser));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("注册异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }
}
