package com.oss.file.app.security;

import com.oss.file.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器
 * 从 Authorization 头提取 Token，一次解析即可完成校验与身份/角色装载。
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt)) {
                // 只解析一次：签名、过期时间、用户 ID、角色全部从同一份 Claims 获取
                Claims claims = jwtUtil.parseToken(jwt);
                if (claims == null) {
                    log.debug("JWT Token 无效或已过期");
                } else {
                    String userId = claims.getSubject();
                    if (StringUtils.hasText(userId)) {
                        Object roleObj = claims.get("role");
                        List<SimpleGrantedAuthority> authorities = roleObj == null
                                ? Collections.emptyList()
                                : Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + roleObj.toString().toUpperCase()));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT 认证成功，userId: {}", userId);
                    }
                }
            }
        } catch (Exception e) {
            // 认证失败不能阻断请求，由 Spring Security 的 EntryPoint 统一返回 401
            log.warn("JWT 认证异常: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }
}
