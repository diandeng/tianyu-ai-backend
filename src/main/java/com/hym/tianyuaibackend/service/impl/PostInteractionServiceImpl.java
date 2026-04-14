package com.hym.tianyuaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.CommunityPost;
import com.hym.tianyuaibackend.entity.SysComment;
import com.hym.tianyuaibackend.entity.SysInteraction;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.mapper.CommunityPostMapper;
import com.hym.tianyuaibackend.mapper.SysCommentMapper;
import com.hym.tianyuaibackend.mapper.SysInteractionMapper;
import com.hym.tianyuaibackend.mapper.SysUserMapper;
import com.hym.tianyuaibackend.service.IPostInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子互动服务实现类
 */
@Service
public class PostInteractionServiceImpl implements IPostInteractionService {

    // 目标类型常量
    private static final Byte TARGET_TYPE_POST = 2;
    // 操作类型常量
    private static final Byte ACTION_TYPE_LIKE = 1;
    private static final Byte ACTION_TYPE_FAVORITE = 3;

    @Autowired
    private SysInteractionMapper interactionMapper;

    @Autowired
    private SysCommentMapper commentMapper;

    @Autowired
    private CommunityPostMapper postMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long postId) {
        return toggleInteraction(userId, postId, ACTION_TYPE_LIKE, "like_count");
    }

    @Override
    @Transactional
    public Map<String, Object> toggleFavorite(Long userId, Long postId) {
        return toggleInteraction(userId, postId, ACTION_TYPE_FAVORITE, "favorite_count");
    }

    /**
     * 通用点赞/收藏切换逻辑
     */
    private Map<String, Object> toggleInteraction(Long userId, Long postId, Byte actionType, String column) {
        Map<String, Object> result = new HashMap<>();

        // 1. 先查当前活跃（is_deleted=0）的记录
        LambdaQueryWrapper<SysInteraction> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetType, TARGET_TYPE_POST)
                .eq(SysInteraction::getTargetId, postId)
                .eq(SysInteraction::getActionType, actionType);

        SysInteraction existing = interactionMapper.selectOne(wrapper);

        if (existing != null) {
            // 已存在活跃记录 -> 执行逻辑删除（取消）
            interactionMapper.deleteById(existing.getId());
            updatePostCount(postId, column, -1);
            result.put("action", "cancel");
            result.put("msg", "已取消");
        } else {
            int revived = interactionMapper.reviveInteraction(userId, TARGET_TYPE_POST, postId, actionType);

            if (revived == 0) {
                // 数据库里连已删除的都没有 -> 彻底的新增
                SysInteraction interaction = new SysInteraction();
                interaction.setUserId(userId);
                interaction.setTargetType(TARGET_TYPE_POST);
                interaction.setTargetId(postId);
                interaction.setActionType(actionType);
                interaction.setCreateTime(LocalDateTime.now());
                interaction.setIsDeleted(0);
                interactionMapper.insert(interaction);
            }
            updatePostCount(postId, column, 1);
            result.put("action", "add");
            result.put("msg", "成功");
        }

        result.put("code", 200);
        result.put("success", true);
        return result;
    }

    /**
     * 原子更新帖子统计数
     */
    private void updatePostCount(Long postId, String column, int delta) {
        String sql = delta > 0 ?
                String.format("%s = %s + 1", column, column) :
                String.format("%s = GREATEST(0, %s - 1)", column, column);

        postMapper.update(null, Wrappers.<CommunityPost>lambdaUpdate()
                .setSql(sql)
                .eq(CommunityPost::getId, postId));
    }

    @Override
    public Map<String, Object> getInteractionStatus(Long userId, Long postId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        data.put("liked", false);
        data.put("favorited", false);

        if (userId != null) {
            // MP 默认会自动加 is_deleted = 0
            List<SysInteraction> interactions = interactionMapper.selectList(
                    Wrappers.<SysInteraction>lambdaQuery()
                            .eq(SysInteraction::getUserId, userId)
                            .eq(SysInteraction::getTargetType, TARGET_TYPE_POST)
                            .eq(SysInteraction::getTargetId, postId)
            );
            for (SysInteraction interaction : interactions) {
                if (ACTION_TYPE_LIKE.equals(interaction.getActionType())) data.put("liked", true);
                if (ACTION_TYPE_FAVORITE.equals(interaction.getActionType())) data.put("favorited", true);
            }
        }

        result.put("code", 200);
        result.put("msg", "获取成功");
        result.put("data", data);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> addComment(Long userId, Long postId, String content, Long parentId) {
        Map<String, Object> result = new HashMap<>();

        SysComment comment = new SysComment();
        comment.setUserId(userId);
        comment.setTargetType(TARGET_TYPE_POST);
        comment.setTargetId(postId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        // 使用SQL原子操作增加评论计数
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .setSql("comment_count = comment_count + 1")
                .eq(CommunityPost::getId, postId));

        result.put("code", 200);
        result.put("msg", "评论成功");
        result.put("success", true);
        return result;
    }

    @Override
    public Map<String, Object> getComments(Long postId, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();

        Page<SysComment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysComment::getTargetType, TARGET_TYPE_POST)
                .eq(SysComment::getTargetId, postId)
                .orderByDesc(SysComment::getCreateTime);

        Page<SysComment> commentPage = commentMapper.selectPage(pageParam, wrapper);

        // 填充用户信息
        List<Map<String, Object>> records = commentPage.getRecords()
                .stream()
                .map(comment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", comment.getId());
                    map.put("content", comment.getContent());
                    map.put("createTime", comment.getCreateTime());
                    map.put("parentId", comment.getParentId());

                    SysUser user = userMapper.selectById(comment.getUserId());
                    if (user != null) {
                        map.put("userId", user.getId());
                        map.put("nickname", user.getNickname());
                        map.put("avatar", user.getAvatar());
                    }

                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", commentPage.getTotal());
        data.put("current", commentPage.getCurrent());
        data.put("pages", commentPage.getPages());

        result.put("code", 200);
        result.put("msg", "获取成功");
        result.put("data", data);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> deleteComment(Long userId, Long commentId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysComment::getId, commentId)
                .eq(SysComment::getUserId, userId)
                .last("LIMIT 1");
        SysComment comment = commentMapper.selectOne(wrapper);

        if (comment == null) {
            result.put("code", 404);
            result.put("msg", "评论不存在");
            return result;
        }

        // 使用逻辑删除（@TableLogic 自动处理）
        commentMapper.deleteById(commentId);

        // 使用SQL原子操作减少评论计数
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .setSql("comment_count = GREATEST(0, comment_count - 1)")
                .eq(CommunityPost::getId, comment.getTargetId()));

        result.put("code", 200);
        result.put("msg", "删除成功");
        result.put("success", true);
        return result;
    }
}