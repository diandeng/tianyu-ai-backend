package com.hym.tianyuaibackend.dto.weather;

import lombok.Data;

import java.util.List;

/**
 * 天气聚合响应
 */
@Data
public class WeatherAggregationResponse {
    private CityInfo cityInfo;
    private WeatherNow weatherNow;
    private List<WeatherAlertItem> alerts;
}