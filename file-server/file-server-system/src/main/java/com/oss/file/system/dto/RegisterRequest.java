package com.oss.file.system.dto;

import lombok.Data;

/**
 * 注册请求 DTO。
 * 刻意不包含 role / status / id 等字段，防止匿名注册时自助提权为 ADMIN。
 */
@Data
public class RegisterRequest {

    private String username;

    private String password;

    private String email;
}
