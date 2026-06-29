package org.example.datn_sd69.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép gửi kèm cookie/token
        config.setAllowCredentials(true);

        // Cho phép Frontend ở cổng 5173 và 5174 gọi API (Thêm dòng nếu m chạy cổng khác)
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:5174");

        // Cho phép mọi loại Header và Method (GET, POST, PUT, DELETE, OPTIONS,...)
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // Áp dụng cấu hình này cho toàn bộ API (/**)
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
