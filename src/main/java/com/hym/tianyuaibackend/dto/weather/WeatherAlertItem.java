package com.hym.tianyuaibackend.dto.weather;

import lombok.Data;

/**
 * 单条天气预警
 */
@Data
public class WeatherAlertItem {
    private String id;
    private String eventTypeName;
    private String severity;
    private String headline;
    private String description;
}