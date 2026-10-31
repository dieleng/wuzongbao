package com.example.portfolio.config;

import com.example.portfolio.common.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 登录后写入 session 的标记 */
    public static final String SESSION_KEY = "LOGIN_ADMIN";

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/admin/**")
                // 登录页和登录接口本身不需要鉴权
                .excludePathPatterns("/admin/login", "/admin/api/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把本地上传目录映射为 /uploads/** 静态资源
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);
    }
}
