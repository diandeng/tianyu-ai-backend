package com.hym.tianyuaibackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hym.tianyuaibackend.entity.SysAlertRecord;
import com.hym.tianyuaibackend.service.IAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员预警管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/alert")
@Tag(name = "管理员预警管理")
public class AdminAlertController {

    @Autowired
    private IAlertService alertService;

    /**
     * 发布预警
     */
@PostMapping("/publish")
@Operation(summary = "发布预警")
public Map<String, Object> publishAlert(@RequestBody AlertPublishDTO dto) {
    Map<String, Object> result = new HashMap<>();
    try {
        if (dto.getOpenids() == null || dto.getOpenids().isEmpty()) {
            result.put("code", 400);
            result.put("msg", "接收用户列表不能为空");
            return result;
        }

        SysAlertRecord record = alertService.publishAlert(
                dto.getAlertLevel(), dto.getAlertType(),
                dto.getAlertTime(), dto.getAlertContent(), dto.getOpenids()
        );

        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("recipientCount", record.getRecipientCount());

        result.put("code", 200);
        result.put("data", data);
    } catch (Exception e) {
        log.error("发布预警失败", e);
        result.put("code", 500);
        result.put("msg", e.getMessage());
    }
    return result;
}

    /**
     * 获取已发布预警列表（分页）
     */
    @GetMapping("/published")
    @Operation(summary = "获取已发布预警列表")
    public Map<String, Object> getPublishedAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = new HashMap<>();

        Page<SysAlertRecord> page = alertService.getPublishedAlerts(pageNum, pageSize);

        Map<String, Object> data = new HashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        data.put("pageNum", page.getCurrent());
        data.put("pageSize", page.getSize());
        data.put("pages", page.getPages());

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @Data
    public static class AlertPublishDTO {
        private String alertLevel;
        private String alertType;
        private String alertTime;
        private String alertContent;
        private List<String> openids;
    }
}