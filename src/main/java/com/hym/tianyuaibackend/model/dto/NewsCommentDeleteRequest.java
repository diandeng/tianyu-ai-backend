package com.hym.tianyuaibackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 删除评论请求 DTO
 */
@Data
@Schema(description = "删除评论请求")
public class NewsCommentDeleteRequest {

    @Schema(description = "评论ID", required = true)
    private Long commentId;
}