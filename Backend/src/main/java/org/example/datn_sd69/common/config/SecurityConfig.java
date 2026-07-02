package org.example.datn_sd69.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Cú pháp mới cho CORS
                .cors(Customizer.withDefaults())

                // 2. Cú pháp mới để tắt CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Cú pháp mới cấu hình Session (Không dùng session, chỉ dùng Token)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Phân quyền API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        // Cho phép tất cả truy cập API đăng nhập, đăng ký
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/vnpay/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands/**", "/api/categories/**","/api/concentrations/**", "/api/fragrance-families/**","/api/capacities/**", "/api/products/**", "/api/bottle-types/**" ).permitAll()
                        // Nhóm Nhân sự: owner, manager, cashier mới được vào các API bắt đầu bằng /api/admin/
                        .requestMatchers("/api/admin/**").hasAnyAuthority("OWNER", "MANAGER", "CASHIER")

                        // Nhóm Khách hàng: chỉ user mới được vào các API của khách
                        .requestMatchers("/api/customer/**").hasAuthority("USER")

                        // Các API còn lại bắt buộc phải có token mới được gọi
                        .anyRequest().authenticated()
                )
                // 5. Thêm Filter kiểm tra JWT trước khi request đi vào Controller
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173")); // URL của FE
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}