package com.hym.tianyuaibackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Data
public class LoginUser implements Serializable {
    private Long userId;
    private String openid;
    private String sessionKey;
}
