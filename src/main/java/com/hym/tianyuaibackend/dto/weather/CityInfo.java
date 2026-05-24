package com.hym.tianyuaibackend.dto.weather;

import lombok.Data;

/**
 * 城市信息
 */
@Data
public class CityInfo {
    private String name;
    private String adm2;
    private String adm1;
}