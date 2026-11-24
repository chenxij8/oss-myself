package com.oss.file.app.security;

import com.oss.file.common.exception.BusinessException;
import org.springframework.security.core.Authentication;

/**
 * 从 Spring Security 主体中安全提取用户 ID，避免各 Controller 强转 String 导致的 ClassCastException。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(401, "未认证，请先登录");
        }
        Object principal = authentication.getPrincipal();
        try {
            if (principal instanceof Number) {
                return ((Number) principal).longValue();
            }
            return Long.valueOf(principal.toString().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "认证信息无效");
        }
    }
}
