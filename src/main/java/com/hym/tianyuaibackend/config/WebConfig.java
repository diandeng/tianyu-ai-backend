package com.hym.tianyuaibackend.config;

import com.hym.tianyuaibackend.interceptor.AdminLoginInterceptor;
import com.hym.tianyuaibackend.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AdminLoginInterceptor adminLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(   // 需要拦截的路径
                        "/user/info",
                        "/user/update",
                        "/user/logout",
                        "/ai/chat/stream",
                        "/news/my-favorites",
                        "/news/like",
                        "/news/favorite",
                        "/news/comment",
                        "/news/comment/delete",
                        "/news/interaction-status",
                        "/post/publish",
                        "/post/update",
                        "/post/delete",
                        "/post/my-posts",
                        "/post/my-favorites",
                        "/post/like",
                        "/post/favorite",
                        "/post/comment",
                        "/post/comment/delete"
                );

        // 管理员登录拦截器
        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns(
                        "/admin/info",
                        "/admin/update",
                        "/admin/password/update",
                        "/admin/logout",
                        "/admin/refresh",
                        "/admin/ai/**",
                        "/admin/news/**",
                        "/admin/post/**"
                );
    }
}