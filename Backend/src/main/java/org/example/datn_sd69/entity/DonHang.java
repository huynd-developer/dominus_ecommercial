package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DonHangs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonHang extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "KhachHangId")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "NhanVienId")
    private NhanVien nhanVien;

    @ManyToOne
    @JoinColumn(name = "MaGiamGiaId")
    private MaGiamGia maGiamGia;

    @Column(name = "LoaiDonHang", nullable = false, length = 20)
    private String loaiDonHang; // 'Online' hoac 'POS'

    @Column(name = "TenNguoiNhan")
    private String tenNguoiNhan;

    @Column(name = "SdtNguoiNhan", length = 15)
    private String sdtNguoiNhan;

    @Column(name = "DiaChiGiaoHang", length = 500)
    private String diaChiGiaoHang;

    @Column(name = "HinhThucThanhToan", nullable = false, length = 20)
    private String hinhThucThanhToan; // 'COD', 'TienMat', 'VNPay'

    @Column(name = "TongTienHang", nullable = false)
    private BigDecimal tongTienHang;

    @Column(name = "SoTienGiam")
    private BigDecimal soTienGiam;

    @Column(name = "TongThanhToan", nullable = false)
    private BigDecimal tongThanhToan;

    @Column(name = "TrangThaiThanhToan")
    private Integer trangThaiThanhToan;

    @Column(name = "TrangThaiDonHang")
    private Integer trangThaiDonHang; // 0: Cho duyet, 1: Dang giao, 2: Hoan thanh, 3: Huy

    @Column(name = "MaGiaoDichVnPay", length = 100)
    private String maGiaoDichVnPay;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao; // Giữ nguyên trường của SQL gốc
}
