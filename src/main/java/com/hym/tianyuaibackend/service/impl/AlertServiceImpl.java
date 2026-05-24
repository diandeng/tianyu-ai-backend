package com.hym.tianyuaibackend.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.SysAlertRecord;
import com.hym.tianyuaibackend.entity.SysUser;
import com.hym.tianyuaibackend.mapper.SysAlertRecordMapper;
import com.hym.tianyuaibackend.mapper.SysUserMapper;
import com.hym.tianyuaibackend.service.IAlertService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AlertServiceImpl implements IAlertService {
    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysAlertRecordMapper sysAlertRecordMapper;

    @Override
    public void sendAlertMessage(String toUser, String alertLevel,
                                 String alertTime, String alertContent, String alertType) {
        // 检查用户订阅次数
        SysUser user = sysUserMapper.selectByOpenid(toUser);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getSubscribeCount() == null || user.getSubscribeCount() <= 0) {
            throw new RuntimeException("订阅次数不足");
        }

        // 格式化时间为微信要求的格式：2019年10月1日 15:01
        String formattedTime = formatAlertTime(alertTime);

        WxMaSubscribeMessage subscribeMessage = WxMaSubscribeMessage.builder()
                .toUser(toUser)
                .templateId("KVob9YpC5O8qMFMi29FveLPmBJLAJL7W1sgua8wEQCw")
                .data(Arrays.asList(
                        new WxMaSubscribeMessage.MsgData("short_thing14", alertLevel),
                        new WxMaSubscribeMessage.MsgData("time2", formattedTime),
                        new WxMaSubscribeMessage.MsgData("thing9", alertContent),
                        new WxMaSubscribeMessage.MsgData("thing8", alertType)
                ))
                .build();
        try {
            wxMaService.getMsgService().sendSubscribeMsg(subscribeMessage);
            log.info("订阅消息发送成功：{}", toUser);
        } catch (WxErrorException e) {
            log.error("微信消息发送失败：{}", e.getMessage());
            throw new RuntimeException("发送失败: " + e.getError().getErrorMsg());
        }

        // 扣减订阅次数
        user.setSubscribeCount(user.getSubscribeCount() - 1);
        sysUserMapper.updateById(user);
    }

    @Override
    public SysAlertRecord publishAlert(String alertLevel, String alertType,
                                       String alertTime, String alertContent, List<String> openids) {
        // 保存预警记录
        SysAlertRecord record = new SysAlertRecord();
        record.setAlertLevel(alertLevel);
        record.setAlertType(alertType);

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(alertTime);
            record.setAlertTime(zdt.withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
        } catch (Exception e) {
            // 解析失败则使用当前时间
            record.setAlertTime(LocalDateTime.now());
            log.warn("预警时间解析失败，使用当前时间: {}", alertTime);
        }

        record.setAlertContent(alertContent);
        record.setRecipientCount(openids.size());
        sysAlertRecordMapper.insert(record);

        // 逐个发送订阅消息（忽略单个发送失败）
        int successCount = 0;
        int failCount = 0;
        for (String openid : openids) {
            try {
                sendAlertMessage(openid, alertLevel, alertTime, alertContent, alertType);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("发送给用户 {} 失败: {}", openid, e.getMessage());
            }
        }
        log.info("预警发布完成：成功 {} 人，失败 {} 人，共 {} 人", successCount, failCount, openids.size());

        return record;
    }

    @Override
    public Page<SysAlertRecord> getPublishedAlerts(int pageNum, int pageSize) {
        Page<SysAlertRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAlertRecord> queryWrapper = new LambdaQueryWrapper<SysAlertRecord>()
                .orderByDesc(SysAlertRecord::getCreateTime);
        return sysAlertRecordMapper.selectPage(page, queryWrapper);
    }

    /**
     * 将 ISO 时间格式化为微信订阅消息要求的格式
     */
    private String formatAlertTime(String alertTime) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(alertTime);
            LocalDateTime localTime = zdt.withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
            return localTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"));
        } catch (Exception e) {
            log.warn("时间格式解析失败，使用原始时间: {}", alertTime);
            return alertTime;
        }
    }
}