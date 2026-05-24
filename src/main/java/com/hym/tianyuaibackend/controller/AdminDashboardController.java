package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hym.tianyuaibackend.entity.*;
import com.hym.tianyuaibackend.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员数据看板控制器
 */
@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "管理员数据看板")
public class AdminDashboardController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysNewsMapper newsMapper;

    @Autowired
    private CommunityPostMapper postMapper;

    @Autowired
    private AiSessionMapper sessionMapper;

    @Autowired
    private SysCommentMapper commentMapper;

    @GetMapping("")
    @Operation(summary = "获取数据看板统计", description = "返回平台运营概况和趋势数据")
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        // 总计数
        long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        long totalNews = newsMapper.selectCount(new QueryWrapper<>());
        long totalPosts = postMapper.selectCount(new QueryWrapper<>());
        long totalSessions = sessionMapper.selectCount(new QueryWrapper<>());
        long totalComments = commentMapper.selectCount(new QueryWrapper<>());

        // 今日新增
        String today = LocalDate.now().toString();
        long todayUsers = userMapper.selectCount(new QueryWrapper<SysUser>().apply("DATE(create_time) = {0}", today));
        long todayPosts = postMapper.selectCount(new QueryWrapper<CommunityPost>().apply("DATE(create_time) = {0}", today));

        // 概览卡片
        List<Map<String, Object>> overview = new ArrayList<>();
        overview.add(card("carbon:user-avatar", "用户量", "总用户量", totalUsers, todayUsers));
        overview.add(card("carbon:document", "资讯数", "总资讯数", totalNews, 0));
        overview.add(card("carbon:chat", "帖子数", "总帖子数", totalPosts, todayPosts));
        overview.add(card("carbon:ai-status", "AI会话", "总会话数", totalSessions, 0));
        data.put("overview", overview);

        // 趋势数据（近7天）
        Map<String, Object> trends = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> userTrend = new ArrayList<>();
        List<Long> postTrend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).toString();
            dates.add(LocalDate.now().minusDays(i).format(fmt));

            Long uc = userMapper.selectCount(new QueryWrapper<SysUser>().apply("DATE(create_time) = {0}", day));
            userTrend.add(uc);

            Long pc = postMapper.selectCount(new QueryWrapper<CommunityPost>().apply("DATE(create_time) = {0}", day));
            postTrend.add(pc);
        }
        trends.put("dates", dates);
        trends.put("newUsers", userTrend);
        trends.put("newPosts", postTrend);
        data.put("trends", trends);

        // 评论总数也加入趋势
        data.put("totalComments", totalComments);

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> card(String icon, String title, String totalTitle, long totalValue, long value) {
        Map<String, Object> item = new HashMap<>();
        item.put("icon", icon);
        item.put("title", title);
        item.put("totalTitle", totalTitle);
        item.put("totalValue", totalValue);
        item.put("value", value);
        return item;
    }
}
