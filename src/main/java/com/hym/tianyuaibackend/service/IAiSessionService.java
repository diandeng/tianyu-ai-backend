package com.hym.tianyuaibackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hym.tianyuaibackend.entity.AiSession;

import java.util.List;

/**
 * <p>
 * AI会话表 服务类
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
public interface IAiSessionService extends IService<AiSession> {

    /**
     * 创建或获取一个会话。
     * 如果传入的 sessionId 有效，则直接返回该ID。
     * 否则，为指定用户创建一个新的会话。
     *
     * @param userId    用户ID
     * @param sessionId 可能是 null 或 0 的会话ID
     * @return 现有或新创建的会话ID
     */
    Long createOrGetSession(Long userId, Long sessionId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title     新标题
     */
    void updateTitle(Long sessionId, String title);

    /**
     * 获取用户的所有会话列表，按更新时间降序排列
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<AiSession> getUserSessions(Long userId);

    /**
     * 删除用户的所有会话（逻辑删除）
     *
     * @param userId 用户ID
     */
    void deleteUserSessions(Long userId);
}
