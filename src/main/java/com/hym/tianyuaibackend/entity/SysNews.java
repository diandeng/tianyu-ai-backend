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
 * 农业资讯表
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Getter
@Setter
@ToString
@TableName(value = "sys_news", autoResultMap = true)
public class SysNews implements Serializable {

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
     * 封面图片URL
     */
    @TableField("cover_img")
    private String coverImg;

    /**
     * Markdown正文
     */
    @TableField("content_md")
    private String contentMd;

    /**
     * 文章摘要
     */
    @TableField("description")
    private String description;

    /**
     * 标签 (JSON数组)
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 发布者
     */
    @TableField("author")
    private String author;

    /**
     * 来源唯一ID
     */
    @TableField("source_id")
    private String sourceId;

    /**
     * 文章来源URL
     */
    @TableField("source_url")
    private String sourceUrl;

    /**
     * 是否爬取: 0-否, 1-是
     */
    @TableField("is_crawled")
    private Integer isCrawled;

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
     * 点踩量
     */
    @TableField("dislike_count")
    private Integer dislikeCount;

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
