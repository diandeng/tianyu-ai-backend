package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.SysNews;
import com.hym.tianyuaibackend.service.ISysNewsService;
import com.hym.tianyuaibackend.task.NewsCrawlerTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员资讯管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/news")
@Tag(name = "管理员资讯管理模块")
public class AdminNewsController {

    @Autowired
    private ISysNewsService sysNewsService;

    @Autowired
    private NewsCrawlerTask newsCrawlerTask;

    /**
     * 获取资讯列表（分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取资讯列表")
    public Map<String, Object> getNewsList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();

        Page<SysNews> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysNews> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(SysNews::getTitle, keyword);
        }

        queryWrapper.eq(SysNews::getIsDeleted, 0);
        queryWrapper.orderByDesc(SysNews::getId);

        Page<SysNews> resultPage = sysNewsService.page(page, queryWrapper);

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
     * 获取资讯详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取资讯详情")
    public Map<String, Object> getNewsDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        SysNews news = sysNewsService.getById(id);
        if (news == null || news.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("msg", "资讯不存在或已删除");
            return result;
        }

        result.put("code", 200);
        result.put("data", news);
        return result;
    }

    /**
     * 删除资讯（逻辑删除）
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除资讯")
    public Map<String, Object> deleteNews(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        boolean success = sysNewsService.removeById(id);
        result.put("code", 200);
        result.put("data", success);
        result.put("msg", success ? "删除成功" : "删除失败");
        return result;
    }

    /**
     * 手动触发爬取资讯
     */
    @PostMapping("/crawl")
    @Operation(summary = "手动触发爬取")
    public Map<String, Object> triggerCrawl() {
        Map<String, Object> result = new HashMap<>();
        String executeMsg = newsCrawlerTask.fetchAndSaveNews();
        result.put("code", 200);
        result.put("data", executeMsg);
        result.put("msg", "爬取完成");
        return result;
    }
}