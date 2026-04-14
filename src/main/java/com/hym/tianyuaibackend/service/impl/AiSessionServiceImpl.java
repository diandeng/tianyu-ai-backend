package com.hym.tianyuaibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hym.tianyuaibackend.entity.AiSession;
import com.hym.tianyuaibackend.mapper.AiSessionMapper;
import com.hym.tianyuaibackend.service.IAiSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        newSession.setTitle("新的对话 " + LocalDateTime.now().toLocalTime());
        this.save(newSession);
        return newSession.getId();
    }
}
