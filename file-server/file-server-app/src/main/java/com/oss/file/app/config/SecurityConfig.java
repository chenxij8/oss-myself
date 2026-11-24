package com.oss.file.app.config;

import com.oss.file.app.security.JwtAuthenticationEntryPoint;
import com.oss.file.app.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 允许跨域的来源，生产环境应通过配置显式指定，避免全放行 */
    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置 - 已适配 Spring Security 6.x Lambda 风格
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF (Lambda 方式代替 .csrf().disable())
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 配置 CORS (默认寻找名为 corsConfigurationSource 的 Bean)
                .cors(Customizer.withDefaults())

                // 3. 设置异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"无权限访问此资源\"}");
                        })
                )

                // 4. 设置会话管理为无状态 (不再使用 .and())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. 配置请求授权
                .authorizeHttpRequests(authz -> authz
                        // 公开接口
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        // 注意：Controller 实际映射为 /download/{token}（单数），必须与此处一致
                        .requestMatchers("/api/shares/download/**", "/api/shares/info/**").permitAll()
                        // 图床内联预览/原始流：外链展示需要匿名可访问（返回文件字节，不含任何用户信息）
                        .requestMatchers("/api/gallery/preview/**", "/api/gallery/raw/**").permitAll()

                        // 管理端接口：仅 ADMIN，方法级 @PreAuthorize 二次校验
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 文件管理接口
                        .requestMatchers(HttpMethod.POST, "/api/files/upload", "/api/files/check").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/files/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/files/**").authenticated()

                        // 分享管理接口
                        .requestMatchers(HttpMethod.POST, "/api/shares").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/shares").authenticated()
                        .requestMatchers("/api/shares/**").authenticated()

                        // 系统监控接口
                        .requestMatchers("/api/monitor/**").authenticated()

                        // 其他所有请求
                        .anyRequest().authenticated()
                )

                // 6. 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 默认仍允许所有来源以保证本地开发可用，生产通过 app.cors.allowed-origins 收紧
        List<String> origins = Arrays.asList(allowedOrigins);
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}