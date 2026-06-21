package org.example.datn_sd69.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Lấy token từ header Authorization của Request
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        // Token hợp lệ phải bắt đầu bằng chữ "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Cắt bỏ 7 ký tự "Bearer " để lấy đúng chuỗi mã hóa
            try {
                email = jwtUtils.extractEmail(token);
            } catch (Exception e) {
                System.out.println("Lỗi JWT: Token không hợp lệ hoặc đã hết hạn!");
            }
        }

        // 2. Nếu đọc được email từ Token và luồng hiện tại chưa được xác thực
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Nhờ JwtUtils kiểm tra lại lần cuối xem Token có chuẩn của email này không, còn hạn không
            if (jwtUtils.validateToken(token, email)) {

                // Trích xuất chức danh (Role) từ trong Token
                String role = jwtUtils.extractRole(token);

                // 3. Cấp thẻ thông hành (AuthenticationToken) cho Request này
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Báo cho Spring Security biết: "Ông này hợp lệ, cho qua!"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. Cho phép Request đi tiếp tục đến Controller
        filterChain.doFilter(request, response);
    }
}
