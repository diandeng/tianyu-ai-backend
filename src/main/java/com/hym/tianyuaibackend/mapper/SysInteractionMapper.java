package com.hym.tianyuaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hym.tianyuaibackend.entity.SysInteraction;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 通用互动记录表 Mapper 接口
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
public interface SysInteractionMapper extends BaseMapper<SysInteraction> {

    /**
     * 强行复活被删除的记录
     * 这里的 SQL 会无视 MyBatis-Plus 的逻辑删除插件
     */
    @Update("UPDATE sys_interaction SET is_deleted = 0, create_time = NOW() " +
            "WHERE user_id = #{userId} AND target_type = #{targetType} " +
            "AND target_id = #{targetId} AND action_type = #{actionType} " +
            "AND is_deleted = 1")
    int reviveInteraction(@Param("userId") Long userId,
                          @Param("targetType") Byte targetType,
                          @Param("targetId") Long targetId,
                          @Param("actionType") Byte actionType);
}
