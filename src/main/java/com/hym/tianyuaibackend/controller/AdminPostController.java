package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.CommunityPost;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.mapper.SysUserMapper;
import com.hym.tianyuaibackend.service.ICommunityPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员帖子管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/post")
@Tag(name = "管理员帖子管理模块")
public class AdminPostController {

    @Autowired
    private ICommunityPostService communityPostService;

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 获取帖子列表（分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取帖子列表")
    public Map<String, Object> getPostList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> result = new HashMap<>();

        Page<CommunityPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(CommunityPost::getTitle, keyword);
        }
        if (status != null) {
            queryWrapper.eq(CommunityPost::getStatus, status);
        }

        queryWrapper.eq(CommunityPost::getIsDeleted, 0);
        queryWrapper.orderByDesc(CommunityPost::getCreateTime);

        Page<CommunityPost> resultPage = communityPostService.page(page, queryWrapper);

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
     * 获取帖子详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取帖子详情")
    public Map<String, Object> getPostDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        CommunityPost post = communityPostService.getById(id);
        if (post == null || post.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("msg", "帖子不存在或已删除");
            return result;
        }

        // 获取发布者信息
        SysUser user = userMapper.selectById(post.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("user", user);

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    /**
     * 删除帖子（逻辑删除）
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除帖子")
    public Map<String, Object> deletePost(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        CommunityPost post = communityPostService.getById(id);
        if (post == null || post.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("msg", "帖子不存在或已删除");
            return result;
        }

        post.setIsDeleted(1);
        communityPostService.updateById(post);

        result.put("code", 200);
        result.put("data", true);
        result.put("msg", "删除成功");
        return result;
    }

    /**
     * 设置帖子状态（违规/正常）
     */
    @PostMapping("/status/{id}")
    @Operation(summary = "设置帖子状态")
    public Map<String, Object> setPostStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        Map<String, Object> result = new HashMap<>();

        CommunityPost post = communityPostService.getById(id);
        if (post == null || post.getIsDeleted() == 1) {
            result.put("code", 404);
            result.put("msg", "帖子不存在或已删除");
            return result;
        }

        post.setStatus(status);
        communityPostService.updateById(post);

        result.put("code", 200);
        result.put("data", true);
        result.put("msg", status == 1 ? "已恢复正常" : "已标记为违规");
        return result;
    }
}