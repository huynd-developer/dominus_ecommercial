package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietDonHangs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietDonHang extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "DonHangId", nullable = false)
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "BienTheSanPhamId", nullable = false)
    private BienTheSanPham bienTheSanPham;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "GiaChotDon", nullable = false)
    private BigDecimal giaChotDon;
}
