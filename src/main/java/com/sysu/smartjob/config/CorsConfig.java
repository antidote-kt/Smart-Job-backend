package com.sysu.smartjob.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 跨域设置
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源
        config.addAllowedOriginPattern("*");
        // 允许所有头信息
        config.addAllowedHeader("*");
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许携带认证信息（cookies）
        config.setAllowCredentials(true);
        // 暴露响应头
        config.addExposedHeader("satoken");
        // 设置跨域请求的有效期，单位秒
        config.setMaxAge(3600L);

        // 添加映射路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}