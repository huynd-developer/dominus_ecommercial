package org.example.datn_sd69.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.auth.service.CustomUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                String email = jwtUtils.getEmailFromToken(jwt);

                UserDetails userDetails;

                try {
                    userDetails = userDetailsService.loadUserByUsername(email);
                } catch (UsernameNotFoundException e) {

                    /*
                     * Token vẫn tồn tại nhưng tài khoản không còn tồn tại trong DB.
                     * Có thể xảy ra khi admin xóa cứng tài khoản trong lúc
                     * người dùng vẫn đang đăng nhập.
                     */
                    SecurityContextHolder.clearContext();

                    writeAuthError(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "ACCOUNT_REMOVED",
                            "Tài khoản của bạn không còn tồn tại hoặc đã bị xóa."
                    );

                    return;
                }

                /*
                 * CustomUserDetailsService hiện tại quy định:
                 *
                 * enabled = status == 1 && isDeleted != true
                 *
                 * Vì vậy:
                 * - Admin khóa tài khoản -> enabled = false
                 * - Tài khoản bị soft delete -> enabled = false
                 *
                 * Không cho request tiếp tục sử dụng JWT cũ.
                 */
                if (!userDetails.isEnabled()) {

                    SecurityContextHolder.clearContext();

                    writeAuthError(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "ACCOUNT_DISABLED",
                            "Tài khoản của bạn đã bị khóa hoặc không còn hoạt động. Vui lòng liên hệ quản trị viên."
                    );

                    return;
                }

                /*
                 * Tài khoản vẫn hoạt động bình thường:
                 * giữ nguyên logic authentication cũ.
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {

            /*
             * Giữ nguyên hành vi cũ đối với:
             * - JWT lỗi
             * - JWT hết hạn
             * - token không hợp lệ
             * - các lỗi authentication khác
             *
             * Request tiếp tục xuống Spring Security để xử lý 401
             * như trước đây.
             */
            logger.error("Không thể set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trả response riêng cho trường hợp account không còn được phép
     * sử dụng token hiện tại.
     */
    private void writeAuthError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}"
        );

        response.getWriter().flush();
    }

    private String parseJwt(HttpServletRequest request) {

        String headerAuth = request.getHeader("Authorization");

        if (
                StringUtils.hasText(headerAuth)
                        && headerAuth.startsWith("Bearer ")
        ) {
            return headerAuth.substring(7);
        }

        return null;
    }
}