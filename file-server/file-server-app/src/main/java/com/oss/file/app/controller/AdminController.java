package com.oss.file.app.controller;

import com.oss.file.app.security.SecurityUtils;
import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import com.oss.file.system.entity.SysUser;
import com.oss.file.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端接口：用户管理。所有方法要求 ADMIN 角色（SecurityConfig + 方法级双重校验）。
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final SysUserService sysUserService;

    public AdminController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<SysUser>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(sysUserService.listUsers(page, size, keyword)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("userCount", sysUserService.countUsers());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 禁用/启用用户：不允许禁用自己 */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long userId,
            @RequestParam Integer status,
            Authentication authentication) {
        try {
            Long currentUserId = SecurityUtils.getUserId(authentication);
            if (currentUserId.equals(userId) && Integer.valueOf(0).equals(status)) {
                throw new BusinessException("不能禁用当前登录账号");
            }
            sysUserService.updateUserStatus(userId, status);
            return ResponseEntity.ok(ApiResponse.success("状态已更新"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        }
    }

    /** 修改角色：不允许降级自己，且至少保留一名启用状态的 ADMIN */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Object>> updateRole(
            @PathVariable Long userId,
            @RequestParam String role,
            Authentication authentication) {
        try {
            Long currentUserId = SecurityUtils.getUserId(authentication);
            String normalized = role == null ? "" : role.trim().toUpperCase();
            if (currentUserId.equals(userId) && "USER".equals(normalized)) {
                throw new BusinessException("不能降级当前登录账号");
            }
            SysUser target = sysUserService.getById(userId);
            if (target == null) {
                throw new BusinessException("用户不存在");
            }
            if ("ADMIN".equalsIgnoreCase(target.getRole()) && "USER".equals(normalized)
                    && !Integer.valueOf(0).equals(target.getStatus())) {
                if (sysUserService.countActiveAdmins() <= 1) {
                    throw new BusinessException("系统必须至少保留一名启用状态的管理员");
                }
            }
            sysUserService.updateUserRole(userId, normalized);
            return ResponseEntity.ok(ApiResponse.success("角色已更新"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @PathVariable Long userId,
            @RequestParam String password) {
        try {
            sysUserService.resetPassword(userId, password);
            return ResponseEntity.ok(ApiResponse.success("密码已重置"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage()));
        }
    }
}
