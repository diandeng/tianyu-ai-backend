package com.hym.tianyuaibackend.model.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private Long sessionId;  // 会话 ID
    private String message;  // 用户输入的消息
    private String imageUrl; // 可选的图片 URL
}
