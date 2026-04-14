package com.hym.tianyuaibackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 点赞/点踩/收藏请求 DTO
 */
@Data
@Schema(description = "点赞/点踩/收藏请求")
public class NewsLikeRequest {

    @Schema(description = "资讯ID", required = true)
    private Long id;
}