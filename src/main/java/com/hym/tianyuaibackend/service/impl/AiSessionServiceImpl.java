package com.hym.tianyuaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hym.tianyuaibackend.entity.AiSession;
import com.hym.tianyuaibackend.mapper.AiSessionMapper;
import com.hym.tianyuaibackend.service.IAiSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * AI会话表 服务实现类
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
@Service
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSession> implements IAiSessionService {

    @Override
    public Long createOrGetSession(Long userId, Long sessionId) {
        if (sessionId != null && sessionId > 0) {
            AiSession session = this.getById(sessionId);
            // 确保会话属于当前用户
            if (session != null && session.getUserId().equals(userId)) {
                return session.getId();
            }
        }
        AiSession newSession = new AiSession();
        newSession.setUserId(userId);
        newSession.setTitle("新对话");
        this.save(newSession);
        return newSession.getId();
    }

    @Override
    public void updateTitle(Long sessionId, String title) {
        AiSession session = new AiSession();
        session.setId(sessionId);
        session.setTitle(title);
        this.updateById(session);
    }

    @Override
    public List<AiSession> getUserSessions(Long userId) {
        QueryWrapper<AiSession> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByDesc("update_time");
        return this.list(query);
    }

    @Override
    public void deleteUserSessions(Long userId) {
        QueryWrapper<AiSession> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        // 逻辑删除（@TableLogic 自动处理 is_deleted = 1）
        this.remove(query);
    }
}
