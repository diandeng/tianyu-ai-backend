package com.hym.tianyuaibackend.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hym.tianyuaibackend.common.AdminContext;
import com.hym.tianyuaibackend.entity.SysAdmin;
import com.hym.tianyuaibackend.model.dto.AdminLoginRequest;
import com.hym.tianyuaibackend.model.dto.AdminLoginUser;
import com.hym.tianyuaibackend.service.ISysAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 管理员表 前端控制器
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-04-09
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@Tag(name = "管理员模块", description = "管理员登录相关接口")
public class SysAdminController {

    @Autowired
    private ISysAdminService sysAdminService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "账号密码登录")
    public Map<String, Object> login(@RequestBody AdminLoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        Map<String, Object> result = new HashMap<>();

        // 1. 查询管理员
        SysAdmin admin = sysAdminService.getOne(
                new LambdaQueryWrapper<SysAdmin>()
                        .eq(SysAdmin::getUsername, username)
                        .eq(SysAdmin::getStatus, 1)
        );

        if (admin == null) {
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
            return result;
        }

        // 2. 验证密码 (SHA256加密比对)
        String sha256Password = sha256(password);
        if (!admin.getPassword().equals(sha256Password)) {
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
            return result;
        }

        // 3. 生成UUID Token并存入Redis
        String token = UUID.randomUUID().toString();
        AdminLoginUser adminLoginUser = new AdminLoginUser();
        adminLoginUser.setAdminId(admin.getId());
        String jsonString = JSON.toJSONString(adminLoginUser);
        redisTemplate.opsForValue().set("login:admin:" + token, jsonString, 7, TimeUnit.DAYS);

        // 4. 更新登录信息
        admin.setLastLoginTime(LocalDateTime.now());
        admin.setLastLoginIp("127.0.0.1");
        sysAdminService.updateById(admin);

        // 5. 返回结果 (密码置空)
        admin.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("admin", admin);

        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", data);

        log.info("管理员 {} 登录成功", username);
        return result;
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取管理员信息")
    public Map<String, Object> getAdminInfo() {
        Map<String, Object> result = new HashMap<>();

        Long adminId = AdminContext.getAdminId();
        if (adminId == null) {
            result.put("code", 401);
            result.put("msg", "未登录或Token已过期");
            return result;
        }

        SysAdmin admin = sysAdminService.getById(adminId);
        if (admin == null) {
            result.put("code", 404);
            result.put("msg", "管理员不存在");
            return result;
        }

        admin.setPassword(null);
        result.put("code", 200);
        result.put("data", admin);
        return result;
    }

    /**
     * 更新管理员信息
     */
    @PostMapping("/update")
    @Operation(summary = "更新管理员信息")
    public Map<String, Object> updateAdminInfo(@RequestBody Map<String, Object> updateData) {
        Map<String, Object> result = new HashMap<>();

        Long adminId = AdminContext.getAdminId();
        if (adminId == null) {
            result.put("code", 401);
            result.put("msg", "未登录或Token已过期");
            return result;
        }

        SysAdmin admin = sysAdminService.getById(adminId);
        if (admin == null) {
            result.put("code", 404);
            result.put("msg", "管理员不存在");
            return result;
        }

        // 更新允许修改的字段
        if (updateData.containsKey("nickname")) {
            admin.setNickname((String) updateData.get("nickname"));
        }
        if (updateData.containsKey("avatar")) {
            admin.setAvatar((String) updateData.get("avatar"));
        }
        if (updateData.containsKey("username")) {
            admin.setUsername((String) updateData.get("username"));
        }

        admin.setUpdateTime(LocalDateTime.now());
        sysAdminService.updateById(admin);

        admin.setPassword(null);
        result.put("code", 200);
        result.put("msg", "更新成功");
        result.put("data", admin);
        return result;
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Map<String, Object> logout(@RequestHeader("Authorization") String token) {
        Map<String, Object> result = new HashMap<>();

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (redisTemplate.delete("login:admin:" + token)) {
            result.put("code", 200);
            result.put("msg", "退出成功");
        } else {
            result.put("code", 500);
            result.put("msg", "Token无效或已过期");
        }
        return result;
    }

    /**
     * 获取权限码
     */
    @GetMapping("/codes")
    @Operation(summary = "获取权限码")
    public Map<String, Object> getAccessCodes() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", java.util.Collections.emptyList());
        return result;
    }

    /**
     * 修改密码
     */
    @PostMapping("/password/update")
    @Operation(summary = "修改密码")
    public Map<String, Object> updatePassword(@RequestBody Map<String, String> passwordData) {
        Map<String, Object> result = new HashMap<>();

        Long adminId = AdminContext.getAdminId();
        if (adminId == null) {
            result.put("code", 401);
            result.put("msg", "未登录或Token已过期");
            return result;
        }

        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "旧密码和新密码不能为空");
            return result;
        }

        SysAdmin admin = sysAdminService.getById(adminId);
        if (admin == null) {
            result.put("code", 404);
            result.put("msg", "管理员不存在");
            return result;
        }

        // 验证旧密码
        String oldSha256 = sha256(oldPassword);
        if (!oldSha256.equals(admin.getPassword())) {
            result.put("code", 400);
            result.put("msg", "旧密码错误");
            return result;
        }

        // 更新新密码
        admin.setPassword(sha256(newPassword));
        admin.setUpdateTime(LocalDateTime.now());
        sysAdminService.updateById(admin);

        result.put("code", 200);
        result.put("msg", "密码修改成功");
        return result;
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Map<String, Object> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String headerToken,
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();

        // 优先从header获取，其次从body获取
        String token = headerToken;
        if (token == null && body != null) {
            token = body.get("token");
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            result.put("code", 401);
            result.put("msg", "Token不能为空");
            return result;
        }

        String redisKey = "login:admin:" + token;
        String jsonString = redisTemplate.opsForValue().get(redisKey);
        if (jsonString == null) {
            result.put("code", 401);
            result.put("msg", "Token无效或已过期");
            return result;
        }

        // 生成新Token并删除旧Token
        String newToken = UUID.randomUUID().toString();
        redisTemplate.delete(redisKey);
        // 新Token存入Redis
        redisTemplate.opsForValue().set("login:admin:" + newToken, jsonString, 7, TimeUnit.DAYS);

        result.put("code", 200);
        result.put("data", newToken);
        return result;
    }

    /**
     * SHA256加密
     */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA256加密失败", e);
        }
    }
}