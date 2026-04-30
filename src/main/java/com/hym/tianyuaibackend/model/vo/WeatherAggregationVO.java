package com.hym.tianyuaibackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 天气预警聚合响应 VO
 */
@Data
public class WeatherAggregationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 城市信息
     */
    private GeoInfo cityInfo;

    /**
     * 实时天气
     */
    private WeatherData weatherNow;

    /**
     * 天气预警列表
     */
    private List<AlertVO> alerts;

    @Data
    public static class GeoInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String adm2;
        private String adm1;
    }

    @Data
    public static class WeatherData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String temp;
        private String icon;
        private String text;
        private String wind360;
        private String windDir;
        private String windScale;
        private String windSpeed;
        private String humidity;
        private String precip;
        private String cloud;
        private String dew;
        private String obsTime;
        private List<String> sources;
        private List<String> license;
    }

    @Data
    public static class AlertVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String eventTypeName;
        private String severity;
        private String headline;
        private String description;
    }
}