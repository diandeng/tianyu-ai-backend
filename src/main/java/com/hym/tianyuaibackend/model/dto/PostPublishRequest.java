package com.hym.tianyuaibackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发布帖子请求DTO
 */
@Data
@Schema(description = "发布帖子请求")
public class PostPublishRequest {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "帖子正文")
    private String content;

    @Schema(description = "图片列表 (JSON数组)")
    private List<String> images;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "发布位置")
    private String location;
}