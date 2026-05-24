package com.hym.tianyuaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hym.tianyuaibackend.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 系统用户表 Mapper 接口
 * </p>
 *
 * @author Tianyu-AI
 * @since 2026-01-28
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE openid = #{openid} LIMIT 1")
    SysUser selectByOpenid(@Param("openid") String openid);
}
