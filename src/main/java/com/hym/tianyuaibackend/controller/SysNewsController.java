package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.entity.SysNews;
import com.hym.tianyuaibackend.service.ISysInteractionService;
import com.hym.tianyuaibackend.service.ISysNewsService;
import com.hym.tianyuaibackend.task.NewsCrawlerTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 农业资讯表 前端控制器
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@RestController
@RequestMapping("/news")
@Tag(name = "农业资讯模块")
public class SysNewsController {
    @Autowired
    private NewsCrawlerTask newsCrawlerTask;

    @Autowired
    private ISysNewsService sysNewsService;

    @Autowired
    private ISysInteractionService sysInteractionService;

    // 目标类型常量: 1-资讯
    private static final Byte TARGET_TYPE_NEWS = 1;

    @PostMapping("/trigger-crawl")
    @Operation(summary = "手动触发抓取", description = "立即调用 Python 爬虫并存入数据库")
    public Map<String, Object> triggerCrawl() {
        Map<String, Object> result = new HashMap<>();

        // 直接调用定时任务里的核心方法
        String executeMsg = newsCrawlerTask.fetchAndSaveNews();

        result.put("code", 200);
        result.put("msg", executeMsg);
        return result;
    }

    /**
     * 分页获取资讯列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取资讯列表", description = "支持分页，每次默认20条")
    public Map<String, Object> getNewsList(@Parameter(description = "当前页码，从1开始") @RequestParam(defaultValue = "1") Integer page, @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 创建分页对象
            Page<SysNews> pageParam = new Page<>(page, size);

            // 2. 构造查询条件
            LambdaQueryWrapper<SysNews> queryWrapper = new LambdaQueryWrapper<>();

            // 【性能优化关键】查列表时，排除 content_md 字段，节省 90% 的带宽
            queryWrapper.select(SysNews.class, info -> !info.getColumn()
                    .equals("content_md"));

            // 过滤掉已删除的
            queryWrapper.eq(SysNews::getIsDeleted, 0);

            // 按 ID 倒序排列 (最新的在最前面)
            queryWrapper.orderByDesc(SysNews::getId);

            // 3. 执行分页查询
            Page<SysNews> newsPage = sysNewsService.page(pageParam, queryWrapper);

            result.put("code", 200);
            result.put("msg", "获取成功");
            result.put("data", newsPage); // 返回包含 records, total, current, pages 的分页对象
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取资讯列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取资讯详情 (包含 Markdown 正文)
     */
    @GetMapping("/detail")
    @Operation(summary = "获取资讯详情", description = "根据ID获取文章完整内容")
    public Map<String, Object> getNewsDetail(@Parameter(description = "文章ID") @RequestParam Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            SysNews news = sysNewsService.getById(id);
            if (news != null && news.getIsDeleted() == 0) {
                // 浏览量 +1 (简单的做法，直接在这里更新)
                news.setViewCount(news.getViewCount() + 1);
                sysNewsService.updateById(news);

                result.put("code", 200);
                result.put("msg", "获取成功");
                result.put("data", news);
            } else {
                result.put("code", 404);
                result.put("msg", "文章不存在或已删除");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取详情失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取我的收藏资讯列表
     */
    @GetMapping("/my-favorites")
    @Operation(summary = "获取我的收藏资讯", description = "获取当前登录用户收藏的资讯列表")
    public Map<String, Object> getMyFavoriteNews(
            @Parameter(description = "当前页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前登录用户
            Long currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                result.put("code", 401);
                result.put("msg", "请先登录");
                return result;
            }

            // 1. 获取用户收藏的资讯ID列表
            List<Long> favoriteNewsIds = sysInteractionService.getFavoriteTargetIds(
                    currentUserId, TARGET_TYPE_NEWS, page, size);

            if (favoriteNewsIds.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("records", List.of());
                data.put("total", 0);
                data.put("current", page);
                data.put("pages", 0);
                data.put("size", size);
                result.put("code", 200);
                result.put("msg", "获取成功");
                result.put("data", data);
                return result;
            }

            // 2. 根据ID列表查询资讯详情
            LambdaQueryWrapper<SysNews> queryWrapper = new LambdaQueryWrapper<>();
            // 查列表时，排除 content_md 字段，节省带宽
            queryWrapper.select(SysNews.class, info -> !info.getColumn().equals("content_md"));
            queryWrapper.eq(SysNews::getIsDeleted, 0);
            queryWrapper.in(SysNews::getId, favoriteNewsIds);
            // 保持收藏的顺序（按ID降序，与收藏时间倒序一致）
            queryWrapper.orderByDesc(SysNews::getId);

            List<SysNews> newsList = sysNewsService.list(queryWrapper);

            // 3. 构建分页结果
            Map<String, Object> data = new HashMap<>();
            data.put("records", newsList);
            data.put("total", (long) newsList.size());
            data.put("current", page);
            data.put("pages", (int) Math.ceil((double) newsList.size() / size));
            data.put("size", size);

            result.put("code", 200);
            result.put("msg", "获取成功");
            result.put("data", data);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取我的收藏资讯失败: " + e.getMessage());
        }

        return result;
    }
}
