package com.hym.tianyuaibackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Getter
@Setter
@ToString
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 微信OpenID
     */
    @TableField("openid")
    private String openid;

    /**
     * 微信UnionID
     */
    @TableField("unionid")
    private String unionid;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 性别：0-未知，1-男，2-女
     */
    @TableField("gender")
    private Integer gender;

    /**
     * 身份: 1-普通农户, 2-农技专家, 3-收购商
     */
    @TableField("role_type")
    private Integer roleType;

    /**
     * 农场位置
     */
    @TableField("farm_location")
    private String farmLocation;

    /**
     * 种植年限
     */
    @TableField("planting_years")
    private Integer plantingYears;

    /**
     * 种植面积(亩)
     */
    @TableField("farm_size")
    private Double farmSize;

    /**
     * 主要作物 (JSON数组)
     */
    @TableField(value = "main_crops", typeHandler = JacksonTypeHandler.class)
    private List<String> mainCrops;

    /**
     * 个性签名
     */
    @TableField("signature")
    private String signature;

    /**
     * 关注数
     */
    @TableField("follow_count")
    private Integer followCount;

    /**
     * 粉丝数
     */
    @TableField("fans_count")
    private Integer fansCount;

    /**
     * 订阅消息剩余次数
     */
    @TableField("subscribe_count")
    private Integer subscribeCount;

    /**
     * 用户设置 (JSON格式)
     */
    @TableField(value = "settings", typeHandler = JacksonTypeHandler.class)
    private Object settings;

    /**
     * 状态: 0-封禁, 1-正常
     */
    @TableField("status")
    private Integer status;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
