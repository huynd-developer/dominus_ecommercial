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

    // Khóa bí mật dùng để mã hóa và giải mã Token (Phải đủ dài, ít nhất 32 ký tự)
    private static final String SECRET_KEY = "DayLaKhoaBiMatCuaDuAnTotNghiepSD69Dominus";

    // Thời gian sống của Token: 24 giờ (Tính bằng milliseconds)
    private static final long EXPIRATION_TIME = 86400000;

    // Tạo SecretKey từ chuỗi bí mật (Chuẩn hóa theo JJWT 0.12.x)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // 1. Tạo Token cho Khách hàng (Mặc định Role là USER)
    public String generateCustomerToken(String email) {
        return Jwts.builder()
                .subject(email) // 0.12.x dùng subject() thay cho setSubject()
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey()) // Tự động nhận diện thuật toán mã hóa
                .compact();
    }

    // 2. Tạo Token cho Nhân viên (Gắn Role tương ứng với RoleId)
    public String generateEmployeeToken(String email, Integer roleId) {
        String roleName = mapRoleIdToRoleName(roleId);

        return Jwts.builder()
                .subject(email)
                .claim("role", roleName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    // 3. Chuyển đổi RoleId từ Database thành tên Quyền trong Spring Security
    private String mapRoleIdToRoleName(Integer roleId) {
        if (roleId == null) return "ROLE_STAFF";
        switch (roleId) {
            case 1: return "ROLE_OWNER";
            case 2: return "ROLE_MANAGER";
            case 3: return "ROLE_CASHIER";
            default: return "ROLE_STAFF";
        }
    }

    // --- CÁC HÀM TIỆN ÍCH DÙNG CHO PHẦN FILTER ---

    // Lấy Email từ Token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Lấy Role từ Token
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    // Kiểm tra xem Token đã hết hạn chưa
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Lấy thời gian hết hạn của Token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Trích xuất một thông tin cụ thể từ Token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Giải mã toàn bộ Token (Đã cập nhật theo cú pháp mới của bản 0.12.5)
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Thay cho setSigningKey()
                .build()
                .parseSignedClaims(token)    // Thay cho parseClaimsJws()
                .getPayload();               // Thay cho getBody()
    }

    // Xác thực Token xem có hợp lệ với Email truyền vào không
    public boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
}