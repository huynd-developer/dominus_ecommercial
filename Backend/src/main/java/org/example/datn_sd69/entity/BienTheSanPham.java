package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "BienTheSanPhams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BienTheSanPham extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "SanPhamId", nullable = false)
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "DungTichId", nullable = false)
    private DungTich dungTich;

    @ManyToOne
    @JoinColumn(name = "LoaiVoChaiId", nullable = false)
    private LoaiVoChai loaiVoChai;

    @Column(name = "MaSku", nullable = false, unique = true, length = 100)
    private String maSku;

    @Column(name = "GiaBan", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "SoLuongTonKho")
    private Integer soLuongTonKho;

    @Column(name = "TrangThai")
    private Integer trangThai;
}
