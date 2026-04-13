package com.oss.file.app.security;

import com.alibaba.fastjson2.JSON;
import com.oss.file.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证入口点
 * 处理未认证用户的请求异常
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 处理认证异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        
        log.error("未认证请求异常: {}", authException.getMessage());

        // 设置响应类型和字符编码
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 构建错误响应
        ApiResponse<Object> apiResponse = new ApiResponse<>(401, "未认证，请先登录");
        
        // 将响应写入Response
        response.getWriter().write(JSON.toJSONString(apiResponse));
    }
}
