package com.hym.tianyuaibackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 帖子评论请求
 */
@Data
@Schema(description = "帖子评论请求")
public class PostCommentRequest {

    @Schema(description = "帖子ID", required = true)
    private Long id;

    @Schema(description = "评论内容", required = true)
    private String content;

    @Schema(description = "父评论ID（回复时使用）")
    private Long parentId;
}