package org.example.datn_sd69.modules.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CustomerProfileResponse {
    private Object id; // Tự động tương thích với kiểu ID của BaseEntity
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChiMacDinh;
    private Integer trangThai;

    // Các thông tin bổ trợ hiển thị trên UI Dominus
    private String membershipTier;       // Ví dụ: "Gold Member"
    private BigDecimal amountToNextTier;  // Số tiền còn thiếu để lên hạng
    private double progressPercentage;    // Phần trăm thanh tiến trình
}