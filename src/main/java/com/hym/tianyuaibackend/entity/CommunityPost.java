package com.hym.tianyuaibackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 社区帖子表
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Getter
@Setter
@ToString
@TableName(value = "community_post", autoResultMap = true)
public class CommunityPost implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 帖子正文
     */
    @TableField("content")
    private String content;

    /**
     * 图片列表 (JSON数组)
     */
    @TableField(value = "images", typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 标签 (JSON数组)
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 发布用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 发布位置
     */
    @TableField("location")
    private String location;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞量
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 收藏量
     */
    @TableField("favorite_count")
    private Integer favoriteCount;

    /**
     * 评论量
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * 状态: 0-违规, 1-正常
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
