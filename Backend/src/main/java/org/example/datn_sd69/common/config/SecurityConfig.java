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
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        /*
                         * PUBLIC AUTH / PAYMENT
                         */
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/vnpay/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/payment/vnpay-return").permitAll()
                        /*
                         * PUBLIC SHOP API:
                         * Khách chưa đăng nhập vẫn được xem danh mục, sản phẩm,
                         * chi tiết sản phẩm và bình luận công khai của sản phẩm.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/brands/**",
                                "/api/categories/**",
                                "/api/concentrations/**",
                                "/api/fragrance-families/**",
                                "/api/capacities/**",
                                "/api/products/**",
                                "/api/shop/products/**",
                                "/api/bottle-types/**"
                        ).permitAll()

                        /*
                         * OWNER REPORT
                         */
                        .requestMatchers("/api/owner/reports/**")
                        .hasAuthority("OWNER")

                        /*
                         * EMPLOYEE ADMIN:
                         * Chỉ OWNER được quản lý nhân viên.
                         */
                        .requestMatchers("/api/admin/employees/**")
                        .hasAuthority("OWNER")

                        /*
                         * CUSTOMER ADMIN:
                         * OWNER, MANAGER, CASHIER được xem/tìm kiếm khách hàng.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/admin/customers/**")
                        .hasAnyAuthority("OWNER", "MANAGER", "CASHIER")

                        /*
                         * CUSTOMER STATUS:
                         * OWNER, MANAGER được khóa/mở tài khoản khách hàng.
                         */
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/customers/*/status")
                        .hasAnyAuthority("OWNER", "MANAGER")

                        /*
                         * Không cho thêm/sửa/xóa customer trực tiếp ở admin.
                         */
                        .requestMatchers("/api/admin/customers/**")
                        .denyAll()

                        /*
                         * ADMIN API KHÁC:
                         */
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority("OWNER", "MANAGER", "CASHIER")

                        /*
                         * CUSTOMER API:
                         * Khách đăng nhập mới được dùng giỏ hàng, profile, order, review.
                         */
                        .requestMatchers("/api/customer/**")
                        .hasAuthority("USER")

                        .requestMatchers("/api/v1/customer/**")
                        .hasAuthority("USER")

                        /*
                         * Các request còn lại chỉ cần đăng nhập.
                         * Những API có @PreAuthorize vẫn sẽ check role ở controller/service.
                         */
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "http://localhost:5176",
                "http://localhost:5180"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Cache-Control"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}