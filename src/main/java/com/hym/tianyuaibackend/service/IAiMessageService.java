package com.hym.tianyuaibackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hym.tianyuaibackend.entity.AiMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <p>
 * AI消息表 服务类
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
public interface IAiMessageService extends IService<AiMessage> {

    /**
     * 【核心入口】处理流式对话（支持文本和图片）
     *
     * @param sessionId 会话ID
     * @param content   用户输入的文本
     * @param imageUrl  用户上传的图片URL (可选)
     * @param isNewSession 是否是新创建的会话
     * @return SseEmitter 用于流式输出
     */
    SseEmitter handleStreamChat(Long sessionId, String content, String imageUrl, boolean isNewSession);
}
