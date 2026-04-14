package com.hym.tianyuaibackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.hym.tianyuaibackend.mapper")
@EnableScheduling
public class TianyuAiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(TianyuAiBackendApplication.class, args);
    }
}
