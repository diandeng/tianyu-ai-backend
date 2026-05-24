package com.hym.tianyuaibackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 预警记录表
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-05-12
 */
@Getter
@Setter
@ToString
@TableName(value = "sys_alert_record")
public class SysAlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预警等级: 蓝色/黄色/橙色/红色
     */
    @TableField("alert_level")
    private String alertLevel;

    /**
     * 预警类型: 大风/暴雨/高温/霜冻/雷电
     */
    @TableField("alert_type")
    private String alertType;

    /**
     * 预警时间
     */
    @TableField("alert_time")
    private LocalDateTime alertTime;

    /**
     * 预警内容
     */
    @TableField("alert_content")
    private String alertContent;

    /**
     * 接收人数
     */
    @TableField("recipient_count")
    private Integer recipientCount;

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
}