package com.hym.tianyuaibackend.controller;

import com.hym.tianyuaibackend.service.IAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alert")
@Tag(name = "预警订阅消息")
@Slf4j
public class AlertController {

    @Autowired
    private IAlertService alertService;

    @PostMapping("/send")
    @Operation(summary = "发送预警订阅消息", description = "向用户发送微信订阅消息，需要用户已订阅模板")
    public Map<String, Object> sendAlertMessage(@RequestBody AlertSendDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            alertService.sendAlertMessage(dto.getToUser(), dto.getAlertLevel(),
                    dto.getAlertTime(), dto.getAlertContent(), dto.getAlertType());
            Map<String, Object> data = new HashMap<>();
            data.put("recipientCount", 1);
            result.put("code", 200);
            result.put("data", data);
        } catch (Exception e) {
            log.error("发送预警消息失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Data
    public static class AlertSendDTO {
        private String toUser;
        private String alertLevel;
        private String alertTime;
        private String alertContent;
        private String alertType;
    }
}