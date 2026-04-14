package com.hym.tianyuaibackend;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        // 1. 数据库配置
        String url = "jdbc:mysql://localhost:3306/tianyu_ai_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false";
        String username = "root";
        String password = "12345678";
        // 2. 开始生成
        FastAutoGenerator.create(url, username, password)
                // 3. 全局配置
                .globalConfig(builder -> {
                    builder.author("Tianyu-AI") // 设置作者
                            .outputDir(System.getProperty("user.dir") + "/src/main/java"); // 指定输出目录
                })
                // 4. 包配置
                .packageConfig(builder -> {
                    builder.parent("com.hym.tianyuaibackend").pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper")); // Mapper XML 生成路径
                })
                // 5. 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude(
                                    // 需要生成的表名列表
                                    "sys_user", "sys_news", "community_post", "sys_interaction", "sys_comment", "ai_session", "ai_message", "sys_follow", "sys_admin")
                            // Entity 策略配置
                            .entityBuilder()// 开启 Entity 策略配置
                            .enableLombok()// 使用 Lombok
                            .enableTableFieldAnnotation()// 开启字段注解
                            .logicDeleteColumnName("is_deleted") // 逻辑删除字段名
                            // Controller 策略配置
                            .controllerBuilder()// 开启 Controller 策略配置
                            .enableRestStyle(); // 使用 @RestController 注解
                }).templateEngine(new FreemarkerTemplateEngine())// 使用 Freemarker 引擎模板
                // 6. 执行生成
                .execute();
        System.out.println("============== 代码生成完成！==============");
    }
}
