package com.hym.tianyuaibackend.interceptor;

import com.alibaba.fastjson2.JSON;
import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.model.dto.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;

    public LoginInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 获取请求头中的 token
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            // 未登录或格式错误，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        // 提取 Bearer Token
        String token = authHeader.substring(7);
        
        // 从 Redis 中获取用户信息
        String redisKey = "login:token:" + token;
        String jsonString = redisTemplate.opsForValue().get(redisKey);
        if (jsonString == null) {
            // token 无效，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        try {
            // 将 json 字符串转换为 LoginUser 对象
            LoginUser loginUser = JSON.parseObject(jsonString, LoginUser.class);
            Long userId = loginUser.getUserId();
            // 存入 ThreadLocal
            UserContext.setUserId(userId);

            return true;
        } catch (Exception e) {
            log.error("解析用户信息失败, token: {}", token, e);
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束，清理 ThreadLocal，防止内存泄露
        UserContext.remove();
    }
}