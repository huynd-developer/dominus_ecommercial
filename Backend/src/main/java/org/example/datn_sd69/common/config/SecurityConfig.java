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

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/vnpay/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/payment/vnpay-return").permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/brands/**",
                                "/api/categories/**",
                                "/api/concentrations/**",
                                "/api/fragrance-families/**",
                                "/api/capacities/**",
                                "/api/products/**",
                                "/api/shop/products/**",
                                "/api/bottle-types/**",
                                "/api/promotions/flash-sale",
                                "/api/admin/orders/**"
                        ).permitAll()

                        .requestMatchers("/api/owner/reports/**")
                        .hasAuthority("OWNER")

                        .requestMatchers("/api/admin/employees/**")
                        .hasAuthority("OWNER")

                        .requestMatchers("/api/admin/promotions/**")
                        .hasAnyAuthority("OWNER", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/admin/customers/**")
                        .hasAnyAuthority("OWNER", "MANAGER", "CASHIER")

                        .requestMatchers(HttpMethod.PATCH, "/api/admin/customers/*/status")
                        .hasAnyAuthority("OWNER", "MANAGER")

                        .requestMatchers("/api/admin/customers/**")
                        .denyAll()

                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority("OWNER", "MANAGER", "CASHIER")

                        .requestMatchers("/api/customer/**")
                        .hasAuthority("USER")

                        .requestMatchers("/api/v1/customer/**")
                        .hasAuthority("USER")

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