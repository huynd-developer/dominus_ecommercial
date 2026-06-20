package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MaGiamGias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaGiamGia extends BaseEntity {

    @Column(name = "MaCode", nullable = false, unique = true, length = 50)
    private String maCode;

    @Column(name = "LoaiGiamGia", nullable = false, length = 20)
    private String loaiGiamGia; // 'PhanTram' hoac 'TienMat'

    @Column(name = "GiaTriGiam", nullable = false)
    private BigDecimal giaTriGiam;

    @Column(name = "GiaTriDonToiThieu")
    private BigDecimal giaTriDonToiThieu;

    @Column(name = "GiamToiDa")
    private BigDecimal giamToiDa;

    @Column(name = "GioiHanSuDung", nullable = false)
    private Integer gioiHanSuDung;

    @Column(name = "SoLanDaDung")
    private Integer soLanDaDung;

    @Column(name = "NgayBatDau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "TrangThai")
    private Integer trangThai;
}