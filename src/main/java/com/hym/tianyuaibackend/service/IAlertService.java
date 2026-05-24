package com.hym.tianyuaibackend.service;

import com.hym.tianyuaibackend.entity.SysAlertRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface IAlertService {

    /**
     * 发送预警订阅消息
     *
     * @param toUser      接收消息的用户OpenID
     * @param alertLevel  预警等级
     * @param alertTime   预警时间
     * @param alertContent 预警内容
     * @param alertType   预警类型
     */
    void sendAlertMessage(String toUser, String alertLevel,
                          String alertTime, String alertContent, String alertType);

    /**
     * 发布预警并向多位用户发送订阅消息
     *
     * @param alertLevel   预警等级
     * @param alertType    预警类型
     * @param alertTime    预警时间
     * @param alertContent 预警内容
     * @param openids      接收消息的用户OpenID列表
     * @return 保存的预警记录
     */
    SysAlertRecord publishAlert(String alertLevel, String alertType,
                                String alertTime, String alertContent, List<String> openids);

    /**
     * 分页查询已发布预警
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    Page<SysAlertRecord> getPublishedAlerts(int pageNum, int pageSize);
}