package com.oss.file.app.security;

import com.oss.file.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * 从请求头中提取JWT Token，并验证其有效性
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Authorization 头字段名
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer 令牌前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 过滤器逻辑
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从请求头中提取JWT Token
            String jwt = getJwtFromRequest(request);

            // 验证Token
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                // 从Token中获取用户ID
                String userId = jwtUtil.getUserIdFromToken(jwt);

                if (StringUtils.hasText(userId)) {
                    // 创建认证对象（此处简化处理，实际应该设置权限信息）
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                    
                    // 设置到 Security Context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT认证成功，userId: {}", userId);
                }
            } else if (StringUtils.hasText(jwt)) {
                log.debug("JWT Token 验证失败");
            }
        } catch (Exception e) {
            log.error("JWT 认证异常: {}", e.getMessage());
        }

        // 继续过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT Token
     * 期望格式: Authorization: Bearer <token>
     * @param request HTTP请求
     * @return JWT Token 或 null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}
