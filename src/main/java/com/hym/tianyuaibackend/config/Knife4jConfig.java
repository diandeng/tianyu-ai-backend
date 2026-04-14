package com.hym.tianyuaibackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info().title("田语AI 接口文档") // 文档标题
                .version("1.0") // 版本号
                .description("基于 Spring Boot 3 + 微信小程序的智慧农业 AI 平台") // 描述
                .contact(new Contact().name("田语AI")
                        .email("872785862@qq.com")) // 联系人
        );
    }
}
