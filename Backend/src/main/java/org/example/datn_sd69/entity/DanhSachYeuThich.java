package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "DanhSachYeuThichs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DanhSachYeuThich extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "KhachHangId", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "SanPhamId", nullable = false)
    private SanPham sanPham;

    @Column(name = "NgayThem")
    private LocalDateTime ngayThem; // Giữ nguyên trường của SQL gốc
}
