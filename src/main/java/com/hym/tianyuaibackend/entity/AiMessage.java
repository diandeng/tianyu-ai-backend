package com.hym.tianyuaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * AI消息表
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Getter
@Setter
@ToString
@TableName(value = "ai_message", autoResultMap = true)
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 角色: user-用户, assistant-助手, system-系统
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容(Markdown格式)
     */
    @TableField("content")
    private String content;

    /**
     * 图片URL
     */
    @TableField("image")
    private String image;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
