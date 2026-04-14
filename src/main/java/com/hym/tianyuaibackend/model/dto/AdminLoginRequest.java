package com.hym.tianyuaibackend.model.dto;

import lombok.Data;

/**
 * 管理员登录请求
 */
@Data
public class AdminLoginRequest {
    private String username;
    private String password;
}