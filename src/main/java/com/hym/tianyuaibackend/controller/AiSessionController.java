package com.hym.tianyuaibackend.controller;

import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.entity.AiSession;
import com.hym.tianyuaibackend.service.IAiSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI会话表 前端控制器
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@RestController
@RequestMapping("/aiSession")
@Tag(name = "AI 会话管理")
public class AiSessionController {

    @Autowired
    private IAiSessionService sessionService;

    @GetMapping("/list")
    @Operation(summary = "获取用户的会话列表", description = "返回当前用户的所有会话，按更新时间降序排列")
    public Map<String, Object> listSessions() {
        Map<String, Object> result = new HashMap<>();
        Long userId = UserContext.getUserId();
        if (userId == null) {
            result.put("code", 401);
            result.put("msg", "用户未登录");
            return result;
        }
        List<AiSession> sessions = sessionService.getUserSessions(userId);
        result.put("code", 200);
        result.put("msg", "获取成功");
        result.put("data", sessions);
        return result;
    }

    @PostMapping("/clear-all")
    @Operation(summary = "清空用户的所有会话", description = "逻辑删除当前用户的所有会话")
    public Map<String, Object> clearAllSessions() {
        Map<String, Object> result = new HashMap<>();
        Long userId = UserContext.getUserId();
        if (userId == null) {
            result.put("code", 401);
            result.put("msg", "用户未登录");
            return result;
        }
        sessionService.deleteUserSessions(userId);
        result.put("code", 200);
        result.put("msg", "已清空所有会话");
        return result;
    }
}
