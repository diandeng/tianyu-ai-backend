package com.hym.tianyuaibackend.controller;

import com.hym.tianyuaibackend.common.UserContext;
import com.hym.tianyuaibackend.model.dto.PostCommentRequest;
import com.hym.tianyuaibackend.model.dto.PostLikeRequest;
import com.hym.tianyuaibackend.service.IPostInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 帖子互动控制器
 */
@RestController
@RequestMapping("/post")
@Tag(name = "帖子互动模块")
public class PostInteractionController {

    @Autowired
    private IPostInteractionService postInteractionService;

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like")
    @Operation(summary = "点赞/取消点赞")
    public Map<String, Object> toggleLike(@RequestBody PostLikeRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return unauthorizedResult();
        }
        return postInteractionService.toggleLike(userId, request.getId());
    }

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/favorite")
    @Operation(summary = "收藏/取消收藏")
    public Map<String, Object> toggleFavorite(@RequestBody PostLikeRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return unauthorizedResult();
        }
        return postInteractionService.toggleFavorite(userId, request.getId());
    }

    /**
     * 获取互动状态
     */
    @GetMapping("/interaction-status")
    @Operation(summary = "获取互动状态", description = "获取点赞/收藏状态")
    public Map<String, Object> getInteractionStatus(
            @Parameter(description = "帖子ID") @RequestParam Long id) {
        Long userId = UserContext.getUserId();
        return postInteractionService.getInteractionStatus(userId, id);
    }

    /**
     * 发表评论
     */
    @PostMapping("/comment")
    @Operation(summary = "发表评论")
    public Map<String, Object> addComment(@RequestBody PostCommentRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return unauthorizedResult();
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "评论内容不能为空");
            return result;
        }
        return postInteractionService.addComment(userId, request.getId(), request.getContent().trim(), request.getParentId());
    }

    /**
     * 获取评论列表
     */
    @GetMapping("/comments")
    @Operation(summary = "获取评论列表")
    public Map<String, Object> getComments(
            @Parameter(description = "帖子ID") @RequestParam Long id,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        return postInteractionService.getComments(id, page, size);
    }

    /**
     * 删除评论
     */
    @PostMapping("/comment/delete")
    @Operation(summary = "删除评论")
    public Map<String, Object> deleteComment(@RequestBody Map<String, Long> request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return unauthorizedResult();
        }
        Long commentId = request.get("commentId");
        if (commentId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("msg", "评论ID不能为空");
            return result;
        }
        return postInteractionService.deleteComment(userId, commentId);
    }

    private Map<String, Object> unauthorizedResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("msg", "请先登录");
        return result;
    }
}