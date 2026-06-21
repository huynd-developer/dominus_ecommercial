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
        return new BCryptPasswordEncoder(); // Mã hóa chuẩn thực tế
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Tắt CSRF vì dùng JWT

                // 1. SỬA CHÍ MẠNG: Bật cấu hình CORS chuẩn thay vì dùng .disable() để chặn Postman/Vue 3 bắn PUT
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không dùng Session
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả truy cập vào API Auth (Đăng nhập, đăng ký)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. SỬA CHÍ MẠNG: Mở khóa thông suốt cho API tài khoản (Chấp nhận cả có hoặc không có /v1, chấp nhận mọi phương thức PUT/POST)
                        .requestMatchers("/api/v1/customer/account/**").permitAll()
                        .requestMatchers("/api/customer/account/**").permitAll()

                        // Các API khác yêu cầu phải có Token hợp lệ
                        .anyRequest().authenticated()
                );

        // Nhúng cái JwtAuthFilter của bạn vào trước bước kiểm tra tài khoản của Spring
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 3. THÊM ĐOẠN NÀY: Hàm cấu hình CORS cho phép Postman/Browser gửi mọi Method (PUT, POST, GET, OPTIONS) lên Server
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
