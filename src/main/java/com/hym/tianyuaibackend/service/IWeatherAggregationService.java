package com.hym.tianyuaibackend.service;

/**
 * 天气预警聚合服务接口
 */
public interface IWeatherAggregationService {

    /**
     * 获取聚合天气预警信息
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 聚合后的天气预警信息
     */
    com.hym.tianyuaibackend.model.vo.WeatherAggregationVO getWeatherAggregation(double lon, double lat);
}
