package com.hym.tianyuaibackend.interceptor;

import com.alibaba.fastjson2.JSON;
import com.hym.tianyuaibackend.common.AdminContext;
import com.hym.tianyuaibackend.model.dto.AdminLoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员登录拦截器
 */
@Slf4j
@Component
public class AdminLoginInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;

    public AdminLoginInterceptor(StringRedisTemplate redisTemplate) {
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

        // 从 Redis 中获取管理员信息
        String redisKey = "login:admin:" + token;
        String jsonString = redisTemplate.opsForValue().get(redisKey);
        if (jsonString == null) {
            // token 无效，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        try {
            // 将 json 字符串转换为 AdminLoginUser 对象
            AdminLoginUser adminLoginUser = JSON.parseObject(jsonString, AdminLoginUser.class);
            Long adminId = adminLoginUser.getAdminId();
            // 存入 ThreadLocal
            AdminContext.setAdminId(adminId);

            return true;
        } catch (Exception e) {
            log.error("解析管理员信息失败, token: {}", token, e);
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束，清理 ThreadLocal，防止内存泄露
        AdminContext.remove();
    }
}