package com.hym.tianyuaibackend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hym.tianyuaibackend.model.vo.WeatherAggregationVO;
import com.hym.tianyuaibackend.service.IWeatherAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class WeatherAggregationServiceImpl implements IWeatherAggregationService {

    private static final Logger log = LoggerFactory.getLogger(WeatherAggregationServiceImpl.class);

    private static final String API_HOST = "https://m56k5qd29w.re.qweatherapi.com";
    private static final String API_KEY = "cc7c368dba84425bb9a2b78949fe26d5";

    // 缓存 Key 前缀
    private static final String GEO_CACHE_PREFIX = "geo:info:";
    private static final String WEATHER_CACHE_PREFIX = "weather:data:";

    // 缓存 TTL
    private static final Duration GEO_CACHE_TTL = Duration.ofDays(30);
    private static final Duration WEATHER_CACHE_TTL = Duration.ofMinutes(15);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public WeatherAggregationVO getWeatherAggregation(double lon, double lat) {
        // 1. 坐标网格化：截断至小数点后 2 位
        BigDecimal latBd = BigDecimal.valueOf(lat).setScale(2, RoundingMode.DOWN);
        BigDecimal lonBd = BigDecimal.valueOf(lon).setScale(2, RoundingMode.DOWN);
        String latStr = latBd.toPlainString();
        String lonStr = lonBd.toPlainString();

        String locationParam = lonStr + "," + latStr;

        // 2. 构建缓存 Key
        String geoCacheKey = GEO_CACHE_PREFIX + latStr + ":" + lonStr;
        String weatherCacheKey = WEATHER_CACHE_PREFIX + latStr + ":" + lonStr;

        // 3. 尝试从缓存获取
        WeatherAggregationVO cachedResult = getCachedResult(weatherCacheKey);
        if (cachedResult != null) {
            log.debug("从缓存获取天气数据: lat={}, lon={}", latStr, lonStr);
            return cachedResult;
        }

        // 4. 并发调用三个 API
        CompletableFuture<WeatherAggregationVO.GeoInfo> geoFuture = CompletableFuture.supplyAsync(() ->
            fetchGeoInfo(locationParam, geoCacheKey)
        );

        CompletableFuture<WeatherAggregationVO.WeatherData> weatherFuture = CompletableFuture.supplyAsync(() ->
            fetchWeatherData(locationParam)
        );

        CompletableFuture<List<WeatherAggregationVO.AlertVO>> alertFuture = CompletableFuture.supplyAsync(() ->
            fetchAlertList(latStr, lonStr)
        );

        // 5. 等待所有任务完成
        CompletableFuture.allOf(geoFuture, weatherFuture, alertFuture).join();

        try {
            WeatherAggregationVO.GeoInfo geoInfo = geoFuture.get();
            WeatherAggregationVO.WeatherData weatherData = weatherFuture.get();
            List<WeatherAggregationVO.AlertVO> alerts = alertFuture.get();

            // 6. 构建响应
            WeatherAggregationVO result = new WeatherAggregationVO();
            result.setCityInfo(geoInfo);
            result.setWeatherNow(weatherData);
            result.setAlerts(alerts != null ? alerts : new ArrayList<>());

            // 7. 写入缓存（即使预警为空也要缓存，防止穿透）
            cacheResult(weatherCacheKey, result);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取天气聚合信息被中断", e);
            throw new RuntimeException("获取天气聚合信息被中断", e);
        } catch (ExecutionException e) {
            log.error("获取天气聚合信息执行异常", e);
            throw new RuntimeException("获取天气聚合信息执行异常", e);
        }
    }

    /**
     * 从缓存获取结果
     */
    private WeatherAggregationVO getCachedResult(String cacheKey) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseObject(cached, WeatherAggregationVO.class);
            }
        } catch (Exception e) {
            log.warn("从缓存获取数据失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 缓存结果
     */
    private void cacheResult(String cacheKey, WeatherAggregationVO result) {
        try {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), WEATHER_CACHE_TTL);
        } catch (Exception e) {
            log.warn("缓存数据失败: {}", e.getMessage());
        }
    }

    /**
     * 获取城市信息
     */
    private WeatherAggregationVO.GeoInfo fetchGeoInfo(String locationParam, String cacheKey) {
        // 先查缓存
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseObject(cached, WeatherAggregationVO.GeoInfo.class);
            }
        } catch (Exception e) {
            log.warn("获取Geo缓存失败: {}", e.getMessage());
        }

        // 调用 API
        String url = API_HOST + "/geo/v2/city/lookup?location=" + locationParam;
        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            JSONArray locationArr = json.getJSONArray("location");
            if (locationArr != null && !locationArr.isEmpty()) {
                JSONObject location = locationArr.getJSONObject(0);
                WeatherAggregationVO.GeoInfo geoInfo = new WeatherAggregationVO.GeoInfo();
                geoInfo.setName(location.getString("name"));
                geoInfo.setAdm2(location.getString("adm2"));
                geoInfo.setAdm1(location.getString("adm1"));

                // 写入缓存
                try {
                    redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(geoInfo), GEO_CACHE_TTL);
                } catch (Exception e) {
                    log.warn("写入Geo缓存失败: {}", e.getMessage());
                }

                return geoInfo;
            }
        } catch (Exception e) {
            log.error("获取城市信息失败: location={}, error={}", locationParam, e.getMessage());
        }
        return null;
    }

    /**
     * 获取实时天气
     */
    private WeatherAggregationVO.WeatherData fetchWeatherData(String locationParam) {
        String url = API_HOST + "/v7/weather/now?location=" + locationParam;
        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            JSONObject now = json.getJSONObject("now");
            if (now != null) {
                WeatherAggregationVO.WeatherData weatherData = new WeatherAggregationVO.WeatherData();
                weatherData.setTemp(now.getString("temp"));
                weatherData.setIcon(now.getString("icon"));
                weatherData.setText(now.getString("text"));
                weatherData.setWind360(now.getString("wind360"));
                weatherData.setWindDir(now.getString("windDir"));
                weatherData.setWindScale(now.getString("windScale"));
                weatherData.setWindSpeed(now.getString("windSpeed"));
                weatherData.setHumidity(now.getString("humidity"));
                weatherData.setPrecip(now.getString("precip"));
                weatherData.setCloud(now.getString("cloud"));
                weatherData.setDew(now.getString("dew"));
                weatherData.setObsTime(now.getString("obsTime"));

                JSONObject refer = json.getJSONObject("refer");
                if (refer != null) {
                    weatherData.setSources(refer.getJSONArray("sources").toJavaList(String.class));
                    weatherData.setLicense(refer.getJSONArray("license").toJavaList(String.class));
                }
                return weatherData;
            }
        } catch (Exception e) {
            log.error("获取实时天气失败: location={}, error={}", locationParam, e.getMessage());
        }
        return null;
    }

    /**
     * 获取天气预警列表
     */
    private List<WeatherAggregationVO.AlertVO> fetchAlertList(String lat, String lon) {
        // 注意：预警接口是 Path 参数，顺序是 lat/lon
        String url = API_HOST + "/weatheralert/v1/current/" + lat + "/" + lon;
        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            // 检查 metadata.zeroResult
            JSONObject metadata = json.getJSONObject("metadata");
            if (metadata != null && metadata.getBoolean("zeroResult") != null && metadata.getBoolean("zeroResult")) {
                return new ArrayList<>();
            }

            JSONArray alertsArr = json.getJSONArray("alerts");
            if (alertsArr == null || alertsArr.isEmpty()) {
                return new ArrayList<>();
            }

            List<WeatherAggregationVO.AlertVO> alerts = new ArrayList<>();
            for (int i = 0; i < alertsArr.size(); i++) {
                JSONObject alert = alertsArr.getJSONObject(i);
                WeatherAggregationVO.AlertVO alertVO = new WeatherAggregationVO.AlertVO();
                alertVO.setId(alert.getString("id"));

                // eventType 是对象
                JSONObject eventType = alert.getJSONObject("eventType");
                if (eventType != null) {
                    alertVO.setEventTypeName(eventType.getString("name"));
                }

                alertVO.setSeverity(alert.getString("severity"));
                alertVO.setHeadline(alert.getString("headline"));
                alertVO.setDescription(alert.getString("description"));
                alerts.add(alertVO);
            }
            return alerts;

        } catch (Exception e) {
            log.error("获取天气预警失败: lat={}, lon={}, error={}", lat, lon, e.getMessage());
            return new ArrayList<>();
        }
    }
}
