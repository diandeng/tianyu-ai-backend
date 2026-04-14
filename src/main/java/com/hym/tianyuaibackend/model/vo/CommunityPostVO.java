package com.hym.tianyuaibackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子视图对象 (包含用户信息和互动状态)
 */
@Data
@Schema(description = "社区帖子视图对象")
public class CommunityPostVO {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "帖子正文")
    private String content;

    @Schema(description = "图片列表")
    private List<String> images;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "发布用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "发布位置")
    private String location;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞量")
    private Integer likeCount;

    @Schema(description = "收藏量")
    private Integer favoriteCount;

    @Schema(description = "评论量")
    private Integer commentCount;

    @Schema(description = "当前用户是否点赞")
    private Boolean isLiked;

    @Schema(description = "当前用户是否收藏")
    private Boolean isFavorited;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}