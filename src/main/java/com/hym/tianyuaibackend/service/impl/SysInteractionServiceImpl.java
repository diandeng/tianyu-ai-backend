package com.hym.tianyuaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.CommunityPost;
import com.hym.tianyuaibackend.entity.SysInteraction;
import com.hym.tianyuaibackend.entity.SysNews;
import com.hym.tianyuaibackend.mapper.CommunityPostMapper;
import com.hym.tianyuaibackend.mapper.SysInteractionMapper;
import com.hym.tianyuaibackend.mapper.SysNewsMapper;
import com.hym.tianyuaibackend.service.ISysInteractionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 通用互动记录表 服务实现类
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Slf4j
@Service
public class SysInteractionServiceImpl extends ServiceImpl<SysInteractionMapper, SysInteraction> implements ISysInteractionService {

    // 目标类型常量
    private static final Byte TARGET_TYPE_NEWS = 1;
    private static final Byte TARGET_TYPE_POST = 2;

    // 操作类型常量
    private static final Byte ACTION_TYPE_LIKE = 1;
    private static final Byte ACTION_TYPE_DISLIKE = 2;
    private static final Byte ACTION_TYPE_FAVORITE = 3;

    @Autowired
    private SysNewsMapper sysNewsMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Override
    @Transactional
    public Map<String, Object> doInteraction(Long userId, Byte targetType, Long targetId, Byte actionType) {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查是否已存在此互动
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getTargetId, targetId)
                .eq(SysInteraction::getActionType, actionType);

        SysInteraction existing = getOne(wrapper);

        if (existing != null) {
            // 已存在，可能是已删除的，恢复它
            if (existing.getIsDeleted() == 1) {
                existing.setIsDeleted(0);
                existing.setCreateTime(LocalDateTime.now());
                updateById(existing);
                updateTargetCount(targetType, targetId, actionType, 1);
                result.put("msg", "已恢复" + getActionName(actionType));
            } else {
                result.put("msg", "已" + getActionName(actionType));
            }
        } else {
            // 新建互动记录
            SysInteraction interaction = new SysInteraction();
            interaction.setUserId(userId);
            interaction.setTargetType(targetType);
            interaction.setTargetId(targetId);
            interaction.setActionType(actionType);
            interaction.setCreateTime(LocalDateTime.now());
            save(interaction);

            // 更新目标表的统计数
            updateTargetCount(targetType, targetId, actionType, 1);
            result.put("msg", getActionName(actionType) + "成功");
        }

        // 检查互斥操作
        handleMutexActions(userId, targetType, targetId, actionType);

        result.put("success", true);
        result.put("code", 200);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> cancelInteraction(Long userId, Byte targetType, Long targetId, Byte actionType) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getTargetId, targetId)
                .eq(SysInteraction::getActionType, actionType);

        SysInteraction existing = getOne(wrapper);

        if (existing != null && existing.getIsDeleted() == 0) {
            existing.setIsDeleted(1);
            updateById(existing);
            updateTargetCount(targetType, targetId, actionType, -1);
            result.put("success", true);
            result.put("code", 200);
            result.put("msg", "已取消" + getActionName(actionType));
        } else {
            result.put("success", false);
            result.put("code", 400);
            result.put("msg", "未" + getActionName(actionType));
        }

        return result;
    }

    @Override
    public Map<String, Boolean> getInteractionStatus(Long userId, Byte targetType, Long targetId) {
        Map<String, Boolean> status = new HashMap<>();
        status.put("liked", false);
        status.put("disliked", false);
        status.put("favorited", false);

        if (userId == null) {
            return status;
        }

        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getTargetId, targetId)
                .eq(SysInteraction::getIsDeleted, 0);

        for (SysInteraction interaction : list(wrapper)) {
            if (ACTION_TYPE_LIKE.equals(interaction.getActionType())) {
                status.put("liked", true);
            } else if (ACTION_TYPE_DISLIKE.equals(interaction.getActionType())) {
                status.put("disliked", true);
            } else if (ACTION_TYPE_FAVORITE.equals(interaction.getActionType())) {
                status.put("favorited", true);
            }
        }

        return status;
    }

    @Override
    public Map<String, Integer> getInteractionCounts(Byte targetType, Long targetId) {
        Map<String, Integer> counts = new HashMap<>();

        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getTargetId, targetId)
                .eq(SysInteraction::getIsDeleted, 0);

        int likeCount = 0, dislikeCount = 0, favoriteCount = 0;

        for (SysInteraction interaction : list(wrapper)) {
            if (ACTION_TYPE_LIKE.equals(interaction.getActionType())) {
                likeCount++;
            } else if (ACTION_TYPE_DISLIKE.equals(interaction.getActionType())) {
                dislikeCount++;
            } else if (ACTION_TYPE_FAVORITE.equals(interaction.getActionType())) {
                favoriteCount++;
            }
        }

        counts.put("likeCount", likeCount);
        counts.put("dislikeCount", dislikeCount);
        counts.put("favoriteCount", favoriteCount);

        return counts;
    }

    /**
     * 更新目标表的统计字段
     */
    private void updateTargetCount(Byte targetType, Long targetId, Byte actionType, int delta) {
        if (TARGET_TYPE_NEWS.equals(targetType)) {
            SysNews news = sysNewsMapper.selectById(targetId);
            if (news != null) {
                switch (actionType) {
                    case 1:
                        news.setLikeCount(Math.max(0, news.getLikeCount() + delta));
                        break;
                    case 2:
                        news.setDislikeCount(Math.max(0, news.getDislikeCount() + delta));
                        break;
                    case 3:
                        news.setFavoriteCount(Math.max(0, news.getFavoriteCount() + delta));
                        break;
                }
                sysNewsMapper.updateById(news);
            }
        } else if (TARGET_TYPE_POST.equals(targetType)) {
            CommunityPost post = communityPostMapper.selectById(targetId);
            if (post != null) {
                switch (actionType) {
                    case 1:
                        post.setLikeCount(Math.max(0, post.getLikeCount() + delta));
                        break;
                    case 3:
                        post.setFavoriteCount(Math.max(0, post.getFavoriteCount() + delta));
                        break;
                }
                communityPostMapper.updateById(post);
            }
        }
    }

    /**
     * 处理互斥操作：点赞和点踩互斥
     */
    private void handleMutexActions(Long userId, Byte targetType, Long targetId, Byte actionType) {
        // 如果点了赞，取消点踩
        if (ACTION_TYPE_LIKE.equals(actionType)) {
            LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysInteraction::getUserId, userId)
                    .eq(SysInteraction::getTargetType, targetType)
                    .eq(SysInteraction::getTargetId, targetId)
                    .eq(SysInteraction::getActionType, ACTION_TYPE_DISLIKE)
                    .eq(SysInteraction::getIsDeleted, 0);

            SysInteraction dislike = getOne(wrapper);
            if (dislike != null) {
                dislike.setIsDeleted(1);
                updateById(dislike);
                updateTargetCount(targetType, targetId, ACTION_TYPE_DISLIKE, -1);
            }
        }
        // 如果点了踩，取消点赞
        else if (ACTION_TYPE_DISLIKE.equals(actionType)) {
            LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysInteraction::getUserId, userId)
                    .eq(SysInteraction::getTargetType, targetType)
                    .eq(SysInteraction::getTargetId, targetId)
                    .eq(SysInteraction::getActionType, ACTION_TYPE_LIKE)
                    .eq(SysInteraction::getIsDeleted, 0);

            SysInteraction like = getOne(wrapper);
            if (like != null) {
                like.setIsDeleted(1);
                updateById(like);
                updateTargetCount(targetType, targetId, ACTION_TYPE_LIKE, -1);
            }
        }
    }

    private String getActionName(Byte actionType) {
        return switch (actionType) {
            case 1 -> "点赞";
            case 2 -> "点踩";
            case 3 -> "收藏";
            default -> "操作";
        };
    }

    @Override
    public List<Long> getFavoriteTargetIds(Long userId, Byte targetType, Integer page, Integer size) {
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getActionType, ACTION_TYPE_FAVORITE)
                .eq(SysInteraction::getIsDeleted, 0)
                .orderByDesc(SysInteraction::getCreateTime);

        // 使用分页查询
        Page<SysInteraction> pageParam = new Page<>(page, size);
        Page<SysInteraction> resultPage = page(pageParam, wrapper);

        return resultPage.getRecords().stream()
                .map(SysInteraction::getTargetId)
                .collect(Collectors.toList());
    }
}