package com.hym.tianyuaibackend.model.dto;

import lombok.Data;

/**
 * LLM 配置 DTO
 */
@Data
public class LlmConfigDTO {
    /**
     * 文本模型
     */
    private String textModel;

    /**
     * 视觉模型
     */
    private String visionModel;

    /**
     * System Prompt 提示词
     */
    private String systemPrompt;

    /**
     * Temperature 参数
     */
    private Double temperature;
}