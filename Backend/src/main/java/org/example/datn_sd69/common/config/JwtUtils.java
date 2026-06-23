package org.example.datn_sd69.common.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    // Khóa bí mật dùng để mã hóa và giải mã Token
    private static final String SECRET_KEY = "DayLaKhoaBiMatCuaDuAnTotNghiepSD69Dominus";

    // Thời gian sống của Token: 24 giờ
    private static final long EXPIRATION_TIME = 86400000;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // 1. Tạo Token cho Khách hàng (Chỉ để "USER")
    public String generateCustomerToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    // 2. Tạo Token cho Nhân viên (Lấy nguyên tên từ DB và viết hoa)
    public String generateEmployeeToken(String email, String roleName) {
        // 👉 ĐÃ SỬA: Lấy tên quyền từ DB (chỉ viết hoa lên cho chuẩn, ko thêm ROLE_)
        String formattedRole = roleName.toUpperCase();

        return Jwts.builder()
                .subject(email)
                .claim("role", formattedRole)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    // --- CÁC HÀM TIỆN ÍCH DÙNG CHO PHẦN FILTER ---

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
}