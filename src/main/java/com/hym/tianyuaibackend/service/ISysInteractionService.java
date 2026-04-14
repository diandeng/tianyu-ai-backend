package com.hym.tianyuaibackend.service;

import com.hym.tianyuaibackend.entity.SysInteraction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 通用互动记录表 服务类
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
public interface ISysInteractionService extends IService<SysInteraction> {

    /**
     * 执行互动操作（点赞/点踩/收藏）
     *
     * @param userId      用户ID
     * @param targetType  目标类型: 1-资讯, 2-帖子
     * @param targetId    目标ID
     * @param actionType  操作类型: 1-点赞, 2-点踩, 3-收藏
     * @return 操作结果，包含当前状态
     */
    Map<String, Object> doInteraction(Long userId, Byte targetType, Long targetId, Byte actionType);

    /**
     * 取消互动操作
     *
     * @param userId      用户ID
     * @param targetType  目标类型
     * @param targetId    目标ID
     * @param actionType  操作类型
     * @return 操作结果
     */
    Map<String, Object> cancelInteraction(Long userId, Byte targetType, Long targetId, Byte actionType);

    /**
     * 获取用户对目标的互动状态
     *
     * @param userId     用户ID
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 互动状态Map
     */
    Map<String, Boolean> getInteractionStatus(Long userId, Byte targetType, Long targetId);

    /**
     * 获取目标的互动统计数
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 统计数Map
     */
    Map<String, Integer> getInteractionCounts(Byte targetType, Long targetId);

    /**
     * 获取用户收藏的目标ID列表
     *
     * @param userId 用户ID
     * @param targetType 目标类型: 1-资讯, 2-帖子
     * @param page 页码
     * @param size 每页数量
     * @return 目标ID列表
     */
    List<Long> getFavoriteTargetIds(Long userId, Byte targetType, Integer page, Integer size);
}
