package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.AiMessage;
import com.hym.tianyuaibackend.entity.AiSession;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.mapper.AiMessageMapper;
import com.hym.tianyuaibackend.mapper.AiSessionMapper;
import com.hym.tianyuaibackend.mapper.SysUserMapper;
import com.hym.tianyuaibackend.model.dto.LlmConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员 AI 管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai")
@Tag(name = "管理员AI管理模块")
public class AdminAiController {

    private static final String REDIS_KEY_LLM_CONFIG = "llm:config";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AiSessionMapper sessionMapper;

    @Autowired
    private AiMessageMapper messageMapper;

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 获取 LLM 配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取LLM配置")
    public Map<String, Object> getLlmConfig() {
        Map<String, Object> result = new HashMap<>();

        LlmConfigDTO config = new LlmConfigDTO();
        config.setTextModel(getOrDefault("textModel", "glm-4-flash"));
        config.setVisionModel(getOrDefault("visionModel", "glm-4.6v"));
        config.setSystemPrompt(getOrDefault("systemPrompt", "你是田语AI，由田语公司开发的AI专家助手。核心能力是纯文本模型、视觉识别，上下文长度为1M token。重要特性是免费服务、农业相关、多语言支持。行为准则是提供热情细腻的帮助、保持坦诚开放的态度、不知道的不会编造。"));
        config.setTemperature(getOrDefaultDouble("temperature", 0.7));

        result.put("code", 200);
        result.put("data", config);
        return result;
    }

    /**
     * 更新 LLM 配置（热更新）
     */
    @PostMapping("/config")
    @Operation(summary = "更新LLM配置")
    public Map<String, Object> updateLlmConfig(@RequestBody LlmConfigDTO config) {
        Map<String, Object> result = new HashMap<>();

        if (config.getTextModel() != null) {
            redisTemplate.opsForValue().set(REDIS_KEY_LLM_CONFIG + ":textModel", config.getTextModel());
        }
        if (config.getVisionModel() != null) {
            redisTemplate.opsForValue().set(REDIS_KEY_LLM_CONFIG + ":visionModel", config.getVisionModel());
        }
        if (config.getSystemPrompt() != null) {
            redisTemplate.opsForValue().set(REDIS_KEY_LLM_CONFIG + ":systemPrompt", config.getSystemPrompt());
        }
        if (config.getTemperature() != null) {
            redisTemplate.opsForValue().set(REDIS_KEY_LLM_CONFIG + ":temperature", String.valueOf(config.getTemperature()));
        }

        result.put("code", 200);
        result.put("msg", "配置更新成功");
        return result;
    }

    /**
     * 获取会话列表（分页）
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表")
    public Map<String, Object> getSessionList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();

        Page<AiSession> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiSession> queryWrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            queryWrapper.eq(AiSession::getUserId, userId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(AiSession::getTitle, keyword);
        }

        queryWrapper.orderByDesc(AiSession::getUpdateTime);
        Page<AiSession> resultPage = sessionMapper.selectPage(page, queryWrapper);

        // 填充用户信息
        for (AiSession session : resultPage.getRecords()) {
            SysUser user = userMapper.selectById(session.getUserId());
            if (user != null) {
                session.setTitle(user.getNickname() + " - " + session.getTitle());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        data.put("pageNum", resultPage.getCurrent());
        data.put("pageSize", resultPage.getSize());
        data.put("pages", resultPage.getPages());

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    /**
     * 获取会话消息详情
     */
    @GetMapping("/messages/{sessionId}")
    @Operation(summary = "获取会话消息详情")
    public Map<String, Object> getMessages(@PathVariable Long sessionId) {
        Map<String, Object> result = new HashMap<>();

        // 获取会话信息
        AiSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            result.put("code", 404);
            result.put("msg", "会话不存在");
            return result;
        }

        // 获取用户信息
        SysUser user = userMapper.selectById(session.getUserId());

        // 获取消息列表
        LambdaQueryWrapper<AiMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiMessage::getSessionId, sessionId);
        queryWrapper.orderByAsc(AiMessage::getCreateTime);
        List<AiMessage> messages = messageMapper.selectList(queryWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("session", session);
        data.put("user", user);
        data.put("messages", messages);

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    private String getOrDefault(String key, String defaultValue) {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_LLM_CONFIG + ":" + key);
        return value != null ? value : defaultValue;
    }

    private Double getOrDefaultDouble(String key, Double defaultValue) {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_LLM_CONFIG + ":" + key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}