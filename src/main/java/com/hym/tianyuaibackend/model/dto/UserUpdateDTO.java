package com.hym.tianyuaibackend.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UserUpdateDTO {
    private String nickname;    // 昵称
    private String avatar;  // 头像URL
    private String signature;   // 个性签名
    private Integer roleType;   // 身份: 1-普通农户, 2-农技专家, 3-收购商
    private String farmLocation;    // 农场位置
    private BigDecimal farmSize;    // 种植面积(亩)
    private Integer plantingYears;  // 种植年限
    private List<String> mainCrops;    // 主要作物
}
