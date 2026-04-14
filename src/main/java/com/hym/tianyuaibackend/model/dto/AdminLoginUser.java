package com.hym.tianyuaibackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员登录用户信息
 */
@Data
public class AdminLoginUser implements Serializable {
    private Long adminId;
}