package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietFlashSales")
@IdClass(ChiTietFlashSaleId.class) // Đánh dấu đây là bảng dùng khóa chính kép
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietFlashSale { // <-- BỎ EXTENDS BASE ENTITY ĐI NHÉ!

    @Id
    @ManyToOne
    @JoinColumn(name = "ChuongTrinhFlashSaleId", nullable = false)
    private ChuongTrinhFlashSale chuongTrinhFlashSale;

    @Id // Đánh dấu ID 2
    @ManyToOne
    @JoinColumn(name = "BienTheSanPhamId", nullable = false)
    private BienTheSanPham bienTheSanPham;

    // Đã update theo DB (DECIMAL(5,2))
    @Column(name = "PhanTramGiamGia", nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramGiamGia;
}