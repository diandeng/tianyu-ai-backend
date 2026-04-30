package com.hym.tianyuaibackend.controller;

import com.hym.tianyuaibackend.model.vo.WeatherAggregationVO;
import com.hym.tianyuaibackend.service.IWeatherAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/weather")
@Tag(name = "天气预警聚合服务")
public class WeatherController {

    @Autowired
    private IWeatherAggregationService weatherAggregationService;

    @GetMapping("/aggregation")
    @Operation(summary = "获取天气聚合信息", description = "根据经纬度获取城市名称、实时天气、预警信息")
    public Map<String, Object> getAggregation(
            @Parameter(description = "经度") @RequestParam double lon,
            @Parameter(description = "纬度") @RequestParam double lat) {
        Map<String, Object> result = new HashMap<>();
        try {
            WeatherAggregationVO vo = weatherAggregationService.getWeatherAggregation(lon, lat);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", vo);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取天气信息失败: " + e.getMessage());
        }
        return result;
    }
}