package com.hym.tianyuaibackend.dto.weather;

import lombok.Data;

/**
 * 实时天气
 */
@Data
public class WeatherNow {
    private String temp;
    private String icon;
    private String text;
    private String humidity;
    private String obsTime;
}