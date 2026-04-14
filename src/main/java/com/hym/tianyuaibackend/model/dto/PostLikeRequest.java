package com.hym.tianyuaibackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 帖子点赞/收藏请求
 */
@Data
@Schema(description = "帖子点赞/收藏请求")
public class PostLikeRequest {

    @Schema(description = "帖子ID", required = true)
    private Long id;
}