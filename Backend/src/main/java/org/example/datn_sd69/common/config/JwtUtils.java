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

    private static final String SECRET_KEY = "DayLaKhoaBiMatCuaDuAnTotNghiepSD69Dominus";
    private static final long EXPIRATION_TIME = 86400000;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // ĐÃ SỬA: Thêm tham số Long id và claim userId
    public String generateCustomerToken(String email, Long id) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", id)
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    // ĐÃ SỬA: Thêm tham số Long id và claim userId
    public String generateEmployeeToken(String email, Long id, Integer roleId) {
        String roleName = mapRoleIdToRoleName(roleId);

        return Jwts.builder()
                .subject(email)
                .claim("userId", id)
                .claim("role", roleName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    private String mapRoleIdToRoleName(Integer roleId) {
        if (roleId == null) return "ROLE_STAFF";
        switch (roleId) {
            case 1: return "ROLE_OWNER";
            case 2: return "ROLE_MANAGER";
            case 3: return "ROLE_CASHIER";
            default: return "ROLE_STAFF";
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    // ĐÃ THÊM: Hàm lấy ID ra từ Token
    public Object extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId");
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