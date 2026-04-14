package com.hym.tianyuaibackend.controller;

import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.service.IAiMessageService;
import com.hym.tianyuaibackend.service.IAiSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/ai")
@Tag(name = "AI 智能模块")
public class AiController {

    @Autowired
    private IAiSessionService sessionService;

    @Autowired
    private IAiMessageService messageService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "支持纯文本或图文诊断，返回 EventStream。如果传入 imageUrl，会自动触发病虫害检测逻辑。")
    public SseEmitter chatStream(
            @Parameter(description = "会话ID，如果是新对话则不传或传0") @RequestParam(required = false) Long sessionId,
            @Parameter(description = "用户输入的内容") @RequestParam String content,
            @Parameter(description = "图片URL（可选），传入则开启图文诊断") @RequestParam(required = false) String imageUrl) {

        Long userId = UserContext.getUserId();
        // 如果 userId 为 null，Service 层会处理并返回一个带错误的 emitter
        if (userId == null) {
            // 即使未登录，也调用服务，让服务内部统一处理未登录逻辑
            return messageService.handleStreamChat(null, content, imageUrl, true);
        }

        // 1. 获取或创建会话ID
        Long finalSessionId = sessionService.createOrGetSession(userId, sessionId);

        // 2. 如果是新创建的会话，通过 SSE 事件把新 ID 发给前端
        boolean isNewSession = (sessionId == null || sessionId == 0L);
        SseEmitter emitter = messageService.handleStreamChat(finalSessionId, content, imageUrl, isNewSession);

        return emitter;
    }
}
