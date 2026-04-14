package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.entity.CommunityPost;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.model.dto.PostPublishRequest;
import com.hym.tianyuaibackend.model.vo.CommunityPostVO;
import com.hym.tianyuaibackend.service.ICommunityPostService;
import com.hym.tianyuaibackend.service.ISysInteractionService;
import com.hym.tianyuaibackend.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 社区帖子表 前端控制器
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Slf4j
@RestController
@RequestMapping("/post")
@Tag(name = "社区帖子模块")
public class CommunityPostController {

    @Autowired
    private ICommunityPostService communityPostService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysInteractionService sysInteractionService;

    private static final Byte TARGET_TYPE_NEWS = 1;
    private static final Byte TARGET_TYPE_POST = 2;

    /**
     * 获取帖子列表 (Feed流，类似小红书)
     */
    @GetMapping("/list")
    @Operation(summary = "获取帖子列表", description = "支持分页，获取社区帖子Feed流，类似于小红书")
    public Map<String, Object> getPostList(
            @Parameter(description = "当前页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户ID，用于获取该用户的帖子") @RequestParam(required = false) Long userId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 创建分页对象
            Page<CommunityPost> pageParam = new Page<>(page, size);

            // 2. 构造查询条件
            LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();

            // 【性能优化】查列表时，排除 content 字段，节省带宽
            queryWrapper.select(CommunityPost.class, info -> !info.getColumn().equals("content"));

            // 过滤正常状态的帖子
            queryWrapper.eq(CommunityPost::getStatus, 1);
            queryWrapper.eq(CommunityPost::getIsDeleted, 0);

            // 如果指定了用户ID，只查询该用户的帖子
            if (userId != null) {
                queryWrapper.eq(CommunityPost::getUserId, userId);
            }

            // 按创建时间倒序排列 (最新的在最前面)
            queryWrapper.orderByDesc(CommunityPost::getCreateTime);

            // 3. 执行分页查询
            Page<CommunityPost> postPage = communityPostService.page(pageParam, queryWrapper);

            // 4. 转换为VO并补充用户信息
            List<CommunityPostVO> voList = postPage.getRecords().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            // 5. 构建返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("records", voList);
            data.put("total", postPage.getTotal());
            data.put("current", postPage.getCurrent());
            data.put("pages", postPage.getPages());
            data.put("size", postPage.getSize());

            result.put("code", 200);
            result.put("msg", "获取成功");
            result.put("data", data);

        } catch (Exception e) {
            log.error("获取帖子列表失败", e);
            result.put("code", 500);
            result.put("msg", "获取帖子列表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取帖子详情", description = "根据ID获取帖子完整内容，包含正文")
    public Map<String, Object> getPostDetail(
            @Parameter(description = "帖子ID") @PathVariable Long id) {

        Map<String, Object> result = new HashMap<>();

        try {
            CommunityPost post = communityPostService.getById(id);
            if (post == null || post.getIsDeleted() == 1 || post.getStatus() == 0) {
                result.put("code", 404);
                result.put("msg", "帖子不存在或已被删除");
                return result;
            }

            // 浏览量 +1
            post.setViewCount(post.getViewCount() + 1);
            communityPostService.updateById(post);

            // 转换为VO
            CommunityPostVO vo = convertToVO(post);
            // 列表查询时没有content，这里重新赋值
            vo.setContent(post.getContent());

            result.put("code", 200);
            result.put("msg", "获取成功");
            result.put("data", vo);

        } catch (Exception e) {
            log.error("获取帖子详情失败", e);
            result.put("code", 500);
            result.put("msg", "获取帖子详情失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 发布帖子
     */
    @PostMapping("/publish")
    @Operation(summary = "发布帖子", description = "发布新的社区帖子")
    public Map<String, Object> publishPost(@RequestBody PostPublishRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前登录用户
            Long currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                result.put("code", 401);
                result.put("msg", "请先登录");
                return result;
            }

            // 参数校验
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                result.put("code", 400);
                result.put("msg", "标题不能为空");
                return result;
            }

            // 创建帖子
            CommunityPost post = new CommunityPost();
            post.setTitle(request.getTitle().trim());
            post.setContent(request.getContent());
            post.setImages(request.getImages());
            post.setTags(request.getTags());
            post.setUserId(currentUserId);
            post.setLocation(request.getLocation());

            // 初始化统计字段
            post.setViewCount(0);
            post.setLikeCount(0);
            post.setFavoriteCount(0);
            post.setCommentCount(0);
            post.setStatus(1); // 正常状态
            post.setIsDeleted(0);
            post.setCreateTime(LocalDateTime.now());
            post.setUpdateTime(LocalDateTime.now());

            communityPostService.save(post);

            result.put("code", 200);
            result.put("msg", "发布成功");
            result.put("data", post.getId());

        } catch (Exception e) {
            log.error("发布帖子失败", e);
            result.put("code", 500);
            result.put("msg", "发布帖子失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新帖子
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "更新帖子", description = "更新指定ID的帖子")
    public Map<String, Object> updatePost(
            @Parameter(description = "帖子ID") @PathVariable Long id,
            @RequestBody PostPublishRequest request) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前登录用户
            Long currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                result.put("code", 401);
                result.put("msg", "请先登录");
                return result;
            }

            // 查询帖子
            CommunityPost post = communityPostService.getById(id);
            if (post == null || post.getIsDeleted() == 1) {
                result.put("code", 404);
                result.put("msg", "帖子不存在");
                return result;
            }

            // 只能修改自己的帖子
            if (!post.getUserId().equals(currentUserId)) {
                result.put("code", 403);
                result.put("msg", "只能修改自己发布的帖子");
                return result;
            }

            // 更新字段
            if (request.getTitle() != null) {
                post.setTitle(request.getTitle().trim());
            }
            if (request.getContent() != null) {
                post.setContent(request.getContent());
            }
            if (request.getImages() != null) {
                post.setImages(request.getImages());
            }
            if (request.getTags() != null) {
                post.setTags(request.getTags());
            }
            if (request.getLocation() != null) {
                post.setLocation(request.getLocation());
            }

            post.setUpdateTime(LocalDateTime.now());
            communityPostService.updateById(post);

            result.put("code", 200);
            result.put("msg", "更新成功");

        } catch (Exception e) {
            log.error("更新帖子失败", e);
            result.put("code", 500);
            result.put("msg", "更新帖子失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除帖子", description = "删除指定ID的帖子（逻辑删除）")
    public Map<String, Object> deletePost(
            @Parameter(description = "帖子ID") @PathVariable Long id) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前登录用户
            Long currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                result.put("code", 401);
                result.put("msg", "请先登录");
                return result;
            }

            // 查询帖子
            CommunityPost post = communityPostService.getById(id);
            if (post == null || post.getIsDeleted() == 1) {
                result.put("code", 404);
                result.put("msg", "帖子不存在");
                return result;
            }

            // 只能删除自己的帖子
            if (!post.getUserId().equals(currentUserId)) {
                result.put("code", 403);
                result.put("msg", "只能删除自己发布的帖子");
                return result;
            }

            // 逻辑删除
            post.setIsDeleted(1);
            post.setUpdateTime(LocalDateTime.now());
            communityPostService.updateById(post);

            result.put("code", 200);
            result.put("msg", "删除成功");

        } catch (Exception e) {
            log.error("删除帖子失败", e);
            result.put("code", 500);
            result.put("msg", "删除帖子失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取我的帖子列表
     */
    @GetMapping("/my-posts")
    @Operation(summary = "获取我的帖子", description = "获取当前登录用户发布的帖子列表")
    public Map<String, Object> getMyPosts(
            @Parameter(description = "当前页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前登录用户
            Long currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                result.put("code", 401);
                result.put("msg", "请先登录");
                return result;
            }

            // 复用list接口，传入当前用户ID
            return getPostList(page, size, currentUserId);

        } catch (Exception e) {
            log.error("获取我的帖子失败", e);
            result.put("code", 500);
            result.put("msg", "获取我的帖子失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取我的收藏帖子列表
     */
    @GetMapping("/my-favorites")
    @Operation(summary = "获取我的收藏帖子", description = "获取当前登录用户收藏的帖子列表")
    public Map<String, Object> getMyFavoritePosts(
            @Parameter(description = "当前页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前登录用户
            Long currentUserId = UserContext.getUserId();
            if (currentUserId == null) {
                result.put("code", 401);
                result.put("msg", "请先登录");
                return result;
            }

            // 1. 获取用户收藏的帖子ID列表
            List<Long> favoritePostIds = sysInteractionService.getFavoriteTargetIds(
                    currentUserId, TARGET_TYPE_POST, page, size);

            if (favoritePostIds.isEmpty()) {
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

            // 2. 根据ID列表查询帖子详情
            LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(CommunityPost.class, info -> !info.getColumn().equals("content"));
            queryWrapper.eq(CommunityPost::getStatus, 1);
            queryWrapper.eq(CommunityPost::getIsDeleted, 0);
            queryWrapper.in(CommunityPost::getId, favoritePostIds);
            // 保持收藏的顺序
            queryWrapper.orderByDesc(CommunityPost::getCreateTime);

            List<CommunityPost> posts = communityPostService.list(queryWrapper);

            // 3. 转换为VO并补充用户信息
            List<CommunityPostVO> voList = posts.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            // 4. 构建分页结果（这里简化处理，直接用返回的列表大小）
            Map<String, Object> data = new HashMap<>();
            data.put("records", voList);
            data.put("total", (long) voList.size());
            data.put("current", page);
            data.put("pages", (int) Math.ceil((double) voList.size() / size));
            data.put("size", size);

            result.put("code", 200);
            result.put("msg", "获取成功");
            result.put("data", data);

        } catch (Exception e) {
            log.error("获取我的收藏帖子失败", e);
            result.put("code", 500);
            result.put("msg", "获取我的收藏帖子失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 将CommunityPost转换为CommunityPostVO
     */
    private CommunityPostVO convertToVO(CommunityPost post) {
        CommunityPostVO vo = new CommunityPostVO();
        BeanUtils.copyProperties(post, vo);

        // 查询发布者信息
        SysUser user = sysUserService.getById(post.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }

        // 获取当前登录用户
        Long currentUserId = UserContext.getUserId();

        // 查询互动状态
        Map<String, Boolean> interactionStatus = sysInteractionService.getInteractionStatus(
                currentUserId, TARGET_TYPE_POST, post.getId());
        vo.setIsLiked(interactionStatus.getOrDefault("liked", false));
        vo.setIsFavorited(interactionStatus.getOrDefault("favorited", false));

        return vo;
    }
}