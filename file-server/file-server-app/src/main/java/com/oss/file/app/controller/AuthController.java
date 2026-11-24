package com.oss.file.app.controller;

import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.common.util.JwtUtil;
import com.oss.file.system.dto.LoginRequest;
import com.oss.file.system.dto.LoginResponse;
import com.oss.file.system.dto.RegisterRequest;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器：登录、注册。
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
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        try {
            if (loginRequest == null || !hasText(loginRequest.getUsername()) || !hasText(loginRequest.getPassword())) {
                throw new BusinessException("用户名和密码不能为空");
            }

            SysUser user = sysUserService.getUserByUsername(loginRequest.getUsername());
            if (user == null) {
                log.warn("登录失败：用户不存在，用户名: {}", loginRequest.getUsername());
                throw new BusinessException("用户名或密码错误");
            }

            // Integer 可能为 null，避免拆箱 NPE
            if (Integer.valueOf(0).equals(user.getStatus())) {
                log.warn("登录失败：用户已被禁用，用户名: {}", loginRequest.getUsername());
                throw new BusinessException("用户已被禁用");
            }

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("登录失败：密码错误，用户名: {}", loginRequest.getUsername());
                throw new BusinessException("用户名或密码错误");
            }

            sysUserService.updateLastLoginTime(user.getId());

            // Token 中携带角色，过滤器据此装载权限，避免每请求都查库
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId().toString());
            claims.put("role", user.getRole());
            String token = jwtUtil.generateToken(claims, user.getId().toString());

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setAccessToken(token);
            loginResponse.setTokenType("Bearer");
            loginResponse.setExpiresIn(86400L);
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
     * 用户注册（公开接口，但角色固定为 USER，不接受客户端传入的 role/status/id）
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest request) {
        try {
            if (request == null || !hasText(request.getUsername()) || !hasText(request.getPassword())) {
                throw new BusinessException("用户名和密码不能为空");
            }
            if (request.getPassword().length() < 6) {
                throw new BusinessException("密码长度不能少于 6 位");
            }

            if (sysUserService.getUserByUsername(request.getUsername()) != null) {
                throw new BusinessException("用户名已存在");
            }

            SysUser user = new SysUser();
            user.setUsername(request.getUsername().trim());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole("USER");   // 服务端强制，杜绝提权
            user.setStatus(1);
            user.setCreateTime(java.time.LocalDateTime.now());
            user.setUpdateTime(java.time.LocalDateTime.now());
            sysUserService.save(user);

            log.info("用户注册成功，用户名: {}", user.getUsername());

            // 响应脱敏：绝不回显密码哈希
            Map<String, Object> safeUser = new HashMap<>();
            safeUser.put("id", user.getId());
            safeUser.put("username", user.getUsername());
            safeUser.put("role", user.getRole());
            return ResponseEntity.ok(ApiResponse.success("注册成功", safeUser));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("注册异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
