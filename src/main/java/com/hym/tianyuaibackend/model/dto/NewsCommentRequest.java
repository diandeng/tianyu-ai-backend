package com.hym.tianyuaibackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 评论请求 DTO
 */
@Data
@Schema(description = "评论请求")
public class NewsCommentRequest {

    @Schema(description = "资讯ID", required = true)
    private Long id;

    @Schema(description = "评论内容", required = true)
    private String content;
}