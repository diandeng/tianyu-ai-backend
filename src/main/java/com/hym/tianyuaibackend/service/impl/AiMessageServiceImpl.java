package com.hym.tianyuaibackend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.entity.AiMessage;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.mapper.AiMessageMapper;
import com.hym.tianyuaibackend.mapper.SysUserMapper;
import com.hym.tianyuaibackend.model.dto.PythonAiResponse;
import com.hym.tianyuaibackend.service.IAiMessageService;
import com.hym.tianyuaibackend.service.IAiSessionService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements IAiMessageService {

    // Zhipu API
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String DEFAULT_MODEL_TEXT = "glm-4-flash";
    private static final String DEFAULT_MODEL_VISION = "glm-4.6v";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是田语AI，由田语公司开发的AI专家助手。核心能力是纯文本模型、视觉识别，上下文长度为1M token。重要特性是免费服务、农业相关、多语言支持。行为准则是提供热情细腻的帮助、保持坦诚开放的态度、不知道的不会编造。";
    private static final Double DEFAULT_TEMPERATURE = 0.7;

    // Redis Keys
    private static final String REDIS_KEY_LLM_CONFIG = "llm:config";

    // Python Service
    private static final String PYTHON_API_URL = "http://127.0.0.1:8000/predict";

    // Roles
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";

    // FLAG
    private static final boolean FLAG = true;

    @Value("${ai.zhipu.apiKey}")
    private String apiKey;

    private OkHttpClient okHttpClient;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IAiSessionService sessionService;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;


    @PostConstruct
    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public SseEmitter handleStreamChat(Long sessionId, String content, String imageUrl, boolean isNewSession, String sessionTitle) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        final Long userId = UserContext.getUserId();

        if (userId == null) {
            emitter.completeWithError(new IllegalArgumentException("用户未登录"));
            return emitter;
        }

        taskExecutor.execute(() -> {
            try {
                // 如果是新会话，发送 session-created 事件（包含 ID 和标题）
                if (isNewSession) {
                    JSONObject sessionEvent = new JSONObject();
                    sessionEvent.put("id", sessionId);
                    sessionEvent.put("title", sessionTitle != null ? sessionTitle : "新对话");
                    sendSseEvent(emitter, "session-created", sessionEvent.toJSONString());
                }

                saveMessage(sessionId, ROLE_USER, content, imageUrl);

                String finalPrompt = content;
                String model = getModelText();

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    sendSseEvent(emitter, "正在进行智能检测...\n\n");
                    CascadeResult result = runCascadeDiagnosis(imageUrl);
                    finalPrompt += "\n" + result.prompt;
                    model = result.model;
                }

                List<JSONObject> messages = buildMessageHistory(sessionId, userId, finalPrompt, imageUrl, model);
                executeStreamRequest(sessionId, model, messages, emitter, isNewSession, content);

            } catch (Exception e) {
                log.error("流式对话异常", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void executeStreamRequest(Long sessionId, String model, List<JSONObject> messages, SseEmitter emitter, boolean isNewSession, String userContent) {
        StringBuilder fullResponse = new StringBuilder();
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("model", model);
            bodyJson.put("stream", true);
            bodyJson.put("messages", messages);
            bodyJson.put("temperature", getTemperature());

            String token = generateToken(apiKey);
            Request request = new Request.Builder()
                    .url(ZHIPU_API_URL)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(bodyJson.toJSONString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown Error";
                    log.error("智谱API请求失败: {}", errorBody);
                    sendSseEvent(emitter, "AI服务响应异常: " + response.code());
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String dataStr = line.substring(5).trim();
                            if ("[DONE]".equals(dataStr)) {
                                break;
                            }
                            try {
                                JSONObject json = JSON.parseObject(dataStr);
                                String delta = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta").getString("content");
                                log.info("[DEBUG SSE] 收到delta: 【{}】, 长度: {}, 包含换行符: {}", delta, delta != null ? delta.length() : 0, delta != null ? delta.contains("\n") : false);
                                if (delta != null) {
                                    sendSseEvent(emitter, delta);
                                    fullResponse.append(delta);
                                }
                            } catch (Exception e) {
                                log.warn("[DEBUG SSE] 解析delta失败: {}, 原始数据: {}", e.getMessage(), dataStr);
                            }
                        }
                    }
                }
            }
            String aiContent = fullResponse.toString();
            if (!aiContent.isEmpty()) {
                saveMessage(sessionId, ROLE_ASSISTANT, aiContent, null);
                log.info("AI回复已保存，长度: {}", aiContent.length());
            }
            // 新会话：AI 回复完成后，用 AI 生成标题
            if (isNewSession && !aiContent.isEmpty()) {
                generateAndSendTitle(emitter, sessionId, userContent);
            }
        } catch (Exception e) {
            log.error("HTTP请求中断", e);
            emitter.completeWithError(e);
        } finally {
            emitter.complete();
        }
    }

    /**
     * 调用 AI 为会话生成简洁标题（15 字以内），更新数据库并通过 SSE 发送给前端
     */
    private void generateAndSendTitle(SseEmitter emitter, Long sessionId, String userContent) {
        try {
            // 用用户第一句话让 AI 总结标题
            JSONObject titleBody = new JSONObject();
            titleBody.put("model", DEFAULT_MODEL_TEXT);
            titleBody.put("stream", false);
            titleBody.put("messages", List.of(
                    new JSONObject() {{
                        put("role", "user");
                        put("content", "用不超过15个字概括以下问题的主旨（只返回概括结果，不要解释，不要标点）：\n" + userContent);
                    }}
            ));
            titleBody.put("temperature", 0.3);
            titleBody.put("max_tokens", 50);

            String token = generateToken(apiKey);
            Request request = new Request.Builder()
                    .url(ZHIPU_API_URL)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(titleBody.toJSONString(), MediaType.parse("application/json")))
                    .build();

            String aiTitle = null;
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = JSON.parseObject(response.body().string());
                    String title = json.getJSONArray("choices")
                            .getJSONObject(0).getJSONObject("message").getString("content");
                    if (title != null) {
                        aiTitle = title.trim();
                        if (aiTitle.length() > 20) aiTitle = aiTitle.substring(0, 20) + "...";
                    }
                }
            }

            if (aiTitle != null) {
                sessionService.updateTitle(sessionId, aiTitle);
                log.info("准备发送session-title事件: title={}, sessionId={}", aiTitle, sessionId);
                sendSseEvent(emitter, "session-title", aiTitle);
                log.info("session-title事件已发送");
            } else {
                // AI 调用失败时的降级：截取用户消息
                String fallbackTitle = userContent.replaceAll("https?://\\S+", "").trim();
                if (fallbackTitle.length() > 15) fallbackTitle = fallbackTitle.substring(0, 15) + "...";
                if (fallbackTitle.isEmpty()) fallbackTitle = "新对话";
                sessionService.updateTitle(sessionId, fallbackTitle);
                sendSseEvent(emitter, "session-title", fallbackTitle);
                log.info("标题降级(截取): {}", fallbackTitle);
            }
        } catch (Exception e) {
            log.warn("AI生成标题失败", e);
            // 外层 catch 也确保发送降级标题
            try {
                String fallbackTitle = userContent.replaceAll("https?://\\S+", "").trim();
                if (fallbackTitle.length() > 15) fallbackTitle = fallbackTitle.substring(0, 15) + "...";
                if (fallbackTitle.isEmpty()) fallbackTitle = "新对话";
                sessionService.updateTitle(sessionId, fallbackTitle);
                sendSseEvent(emitter, "session-title", fallbackTitle);
            } catch (Exception ignored) {}
        }
    }

    private List<JSONObject> buildMessageHistory(Long sessionId, Long userId, String currentPrompt, String imageUrl, String model) {
        List<JSONObject> messageList = new ArrayList<>();
        SysUser user = userMapper.selectById(userId);
        String userProfile = "";
        if (user != null) {
            String crops = user.getMainCrops() != null ? JSON.toJSONString(user.getMainCrops()) : "未知作物";
            String loc = user.getFarmLocation() != null ? user.getFarmLocation() : "未知地区";
            userProfile = String.format("当前用户是种植 %s 的农户，位于 %s。", crops, loc);
        }

        // 从Redis获取system prompt，支持热更新
        String systemPromptTemplate = getSystemPrompt();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", ROLE_SYSTEM);
        systemMsg.put("content", systemPromptTemplate + userProfile);
        messageList.add(systemMsg);

        if (sessionId != null && sessionId > 0) {
            QueryWrapper<AiMessage> query = new QueryWrapper<>();
            query.eq("session_id", sessionId);
            query.orderByAsc("create_time");
            query.last("LIMIT 20");
            List<AiMessage> historyDB = this.list(query);

            for (AiMessage msg : historyDB) {
                JSONObject historyMsg = new JSONObject();
                historyMsg.put("role", msg.getRole());
                String content = msg.getContent();
                if (msg.getImage() != null && !msg.getImage().isEmpty()) {
                    content += " [用户上传了一张图片]";
                }
                historyMsg.put("content", content);
                messageList.add(historyMsg);
            }
        }

        JSONObject currentUserMsg = new JSONObject();
        currentUserMsg.put("role", ROLE_USER);

        if (getModelVision().equals(model) && imageUrl != null) {
            JSONArray contentArr = new JSONArray();
            JSONObject textObj = new JSONObject();
            textObj.put("type", "text");
            textObj.put("text", currentPrompt);
            contentArr.add(textObj);

            JSONObject imgObj = new JSONObject();
            imgObj.put("type", "image_url");
            JSONObject urlObj = new JSONObject();
            urlObj.put("url", imageUrl);
            imgObj.put("image_url", urlObj);
            contentArr.add(imgObj);
            currentUserMsg.put("content", contentArr);
        } else {
            currentUserMsg.put("content", currentPrompt);
        }
        messageList.add(currentUserMsg);
        return messageList;
    }

    private String getModelText() {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_LLM_CONFIG + ":textModel");
        return value != null ? value : DEFAULT_MODEL_TEXT;
    }

    private String getModelVision() {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_LLM_CONFIG + ":visionModel");
        return value != null ? value : DEFAULT_MODEL_VISION;
    }

    private String getSystemPrompt() {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_LLM_CONFIG + ":systemPrompt");
        return value != null ? value : DEFAULT_SYSTEM_PROMPT;
    }

    private Double getTemperature() {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_LLM_CONFIG + ":temperature");
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return DEFAULT_TEMPERATURE;
            }
        }
        return DEFAULT_TEMPERATURE;
    }

    private String generateToken(String apiKey) {
        try {
            String[] parts = apiKey.split("\\.");
            String id = parts[0];
            String secret = parts[1];

            Map<String, Object> payload = new HashMap<>();
            payload.put("api_key", id);
            payload.put("exp", System.currentTimeMillis() + 3600 * 1000);
            payload.put("timestamp", System.currentTimeMillis());

            Algorithm algorithm = Algorithm.HMAC256(secret.getBytes());
            return JWT.create()
                    .withHeader(Map.of("alg", "HS256", "sign_type", "SIGN"))
                    .withPayload(payload)
                    .sign(algorithm);
        } catch (Exception e) {
            log.error("Token生成失败", e);
            throw new RuntimeException("鉴权失败");
        }
    }

    private void saveMessage(Long sessionId, String role, String content, String imageUrl) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        if (imageUrl != null) msg.setImage(imageUrl);
        this.save(msg);
    }

    private void sendSseEvent(SseEmitter emitter, String data) {
        try {
            // SSE占位符替换：空格 -> &#32;, 换行 -> &#92n
            String encodedData = data.replace(" ", "&#32;").replace("\n", "&#92;n");
            log.info("[DEBUG SSE] 发送SSE事件: data={}, 长度={}", encodedData, data.length());
            emitter.send(SseEmitter.event().data(encodedData));
        } catch (IOException e) {
            log.debug("SSE发送失败: {}", e.getMessage());
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("SSE发送失败: {}", e.getMessage());
        }
    }

    private CascadeResult runCascadeDiagnosis(String imageUrl) {
        CascadeResult result = new CascadeResult();
        result.prompt = "";
        result.model = getModelVision();
        if (FLAG) return result;
        try {
            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("url", imageUrl);
            PythonAiResponse pyResp = restTemplate.postForObject(PYTHON_API_URL, requestMap, PythonAiResponse.class);

            if (pyResp != null && pyResp.getCode() == 200 && pyResp.getData() != null) {
                PythonAiResponse.DataDTO data = pyResp.getData();
                if (data.getIsConfident()) {
                    result.prompt = String.format("我的自动化设备检测到作物患有【%s】，置信度为 %.2f。请你作为植物病理学专家，做两件事：\n1. 将该病害名称准确翻译为中文。\n2. 给出详细的防治方案（包括物理防治和化学药剂推荐）。", data.getClassName(), data.getConfidence());
                    result.model = getModelText();
                } else {
                    result.prompt = "请仔细观察这张图片。我的初步检测模型不太确定，可能是健康或者某种病害。请你识别图中的作物状态，如果生病了，请给出治疗方案。";
                    result.model = getModelVision();
                }
            } else {
                result.model = getModelVision();
            }
        } catch (Exception e) {
            log.error("Python模型调用失败", e);
            result.model = getModelVision();
        }
        return result;
    }

    private static class CascadeResult {
        String prompt;
        String model;
    }
}