package com.hym.tianyuaibackend.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.model.dto.LoginUser;
import com.hym.tianyuaibackend.model.dto.UserUpdateDTO;
import com.hym.tianyuaibackend.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 系统用户表 前端控制器
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理模块", description = "系统用户相关接口")
public class SysUserController {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 微信小程序登录
     *
     * @param code 小程序端 wx.login() 获取的 code
     * @return 登录信息 + Token
     */
    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    @Operation(summary = "微信登录", description = "传入code，返回Token和用户信息")
    public Map<String, Object> login(@RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        try {
            String openid;
            String sessionKey;
            // TODO: 测试后门登录
            if ("test".equals(code)) {
                openid = "o_TEST_OPENID_123456"; // 假的 OpenID
                sessionKey = "TEST_SESSION_KEY";  // 假的 SessionKey
                log.warn("注意：当前使用的是测试后门登录！");
            } else {
                // 获取 session 信息
                WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
                openid = session.getOpenid();
                sessionKey = session.getSessionKey();
            }

            // 查询用户是否存在
            SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenid, openid));
            if (user == null) {
                // 新用户注册
                user = new SysUser();
                user.setOpenid(openid);
                user.setNickname("微信用户" + System.currentTimeMillis() % 10000);
                sysUserService.save(user);
                log.info("新用户注册成功，ID: {}", user.getId());
            } else {
                // 更新登录时间
                user.setUpdateTime(LocalDateTime.now());
                sysUserService.updateById(user);
                log.info("用户登录成功，ID: {}", user.getId());
            }
            // Redis 存储登录状态，设置过期时间为 3 天
            String token = UUID.randomUUID().toString();
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(user.getId());
            loginUser.setOpenid(openid);
            loginUser.setSessionKey(sessionKey);
            String jsonString = JSON.toJSONString(loginUser);
            redisTemplate.opsForValue().set("login:token:" + token, jsonString, 3, TimeUnit.DAYS);
            // 返回结果
            result.put("code", 200);
            result.put("msg", "登录成功");
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            result.put("data", data);
        } catch (Exception e) {
            log.error("登录失败", e);
            result.put("code", 500);
            result.put("msg", "登录失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "需要Header带Token")
    public Map<String, Object> getUserInfo() {
        Map<String, Object> result = new HashMap<>();

        Long userId = UserContext.getUserId();
        SysUser user = sysUserService.getById(userId);

        if (user != null) {
            result.put("code", 200);
            result.put("msg", "获取成功");
            result.put("data", user);
        } else {
            result.put("code", 404);
            result.put("msg", "用户不存在");
        }

        return result;
    }

    /**
     * 修改个人信息
     *
     * @return 操作结果，最新用户信息
     */
    @PostMapping("/update")
    @Operation(summary = "修改个人信息", description = "修改昵称、头像、作物标签等")
    public Map<String, Object> updateUserInfo(@RequestBody UserUpdateDTO userUpdateDTO) {
        Map<String, Object> result = new HashMap<>();

        Long userId = UserContext.getUserId();
        SysUser user = new SysUser();
        user.setId(userId);

        BeanUtils.copyProperties(userUpdateDTO, user);
        boolean success = sysUserService.updateById(user);

        if (success) {
            result.put("code", 200);
            result.put("msg", "修改成功");
            result.put("data", sysUserService.getById(userId));
        } else {
            result.put("code", 500);
            result.put("msg", "修改失败");
        }
        return result;
    }

    @PostMapping("/subscribe")
    @Operation(summary = "订阅消息次数+1", description = "用户点击订阅消息后调用，每次订阅次数+1")
    public Map<String, Object> subscribeIncrement() {
        Map<String, Object> result = new HashMap<>();
        Long userId = UserContext.getUserId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "用户未登录");
            return result;
        }
        user.setSubscribeCount(user.getSubscribeCount() + 1);
        sysUserService.updateById(user);
        result.put("code", 200);
        result.put("msg", "订阅成功");
        result.put("data", user.getSubscribeCount());
        return result;
    }

    /**
     * 退出登录
     *
     * @param token 登录时返回的 Token
     * @return 操作结果
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "让当前Token失效")
    public Map<String, Object> logout(@RequestHeader("Authorization") String token) {
        Map<String, Object> result = new HashMap<>();
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (redisTemplate.delete("login:token:" + token)) {
            result.put("code", 200);
            result.put("msg", "退出成功");
        } else {
            result.put("code", 500);
            result.put("msg", "Token无效或已过期");
        }
        return result;
    }
}
