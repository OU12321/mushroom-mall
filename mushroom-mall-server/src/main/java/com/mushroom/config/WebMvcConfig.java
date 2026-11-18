package com.mushroom.config;

import com.mushroom.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 (拦截器 + 静态资源映射)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Value("${mushroom.upload-path}")
    private String uploadPath;

    /**
     * 注册登录拦截器
     *
     * 注意：/common/upload 不再放行 —— 上传属于写入操作，必须登录后才能调用，
     * 否则任何人都能往服务器塞文件。前端 el-upload 已通过 :headers 携带 JWT。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/product/list",
                        "/product/detail/**",
                        "/category/list",
                        "/banner/list",
                        "/video/list",
                        "/static/**",
                        "/ws/**",
                        "/error"
                );
    }

    /**
     * 图片静态资源映射 (映射到文件系统上传目录)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
