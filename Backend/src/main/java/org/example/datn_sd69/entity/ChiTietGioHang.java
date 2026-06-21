package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "ChiTietGioHangs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietGioHang extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "GioHangId", nullable = false)
    private GioHang gioHang;

    @ManyToOne
    @JoinColumn(name = "BienTheSanPhamId", nullable = false)
    private BienTheSanPham bienTheSanPham;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "GhiChu", length = 255)
    private String ghiChu;

    @Column(name = "DuongDanAnhThuNho", columnDefinition = "VARCHAR(MAX)")
    private String duongDanAnhThuNho;
}