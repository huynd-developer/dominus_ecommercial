package org.example.datn_sd69.common.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF vì dùng cấu trúc RESTful JWT
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Kích hoạt CORS chuẩn
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép tất cả truy cập vào API Đăng nhập, Đăng ký công khai
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. Cho phép truy cập tự do vào cụm API tài khoản khách hàng cũ của bạn
                        .requestMatchers("/api/v1/customer/account/**").permitAll()
                        .requestMatchers("/api/customer/account/**").permitAll()

                        // 3. ĐÃ SỬA: Mở khóa cả đường dẫn gốc và đường dẫn con của Favorites
                        .requestMatchers("/api/user/favorites").permitAll()   // Mở khóa cho lệnh GET (gốc)
                        .requestMatchers("/api/user/favorites/**").permitAll() // Mở khóa cho lệnh POST (/toggle) và nhánh con
                        .requestMatchers("/api/user/profile/**").permitAll()

                        // Tất cả các API quản trị hoặc API khác còn lại bắt buộc phải có Token qua JwtAuthFilter
                        .anyRequest().authenticated()
                );

        // Đính kèm bộ lọc JWT lọc request trước khi xử lý phân quyền
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Cấu hình CORS mở rộng giúp Frontend Vite (chạy cổng 5173) không bị trình duyệt chặn dữ liệu
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}