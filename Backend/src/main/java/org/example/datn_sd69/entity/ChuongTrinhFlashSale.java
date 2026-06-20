package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChuongTrinhFlashSales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChuongTrinhFlashSale extends BaseEntity {

    @Column(name = "TenChuongTrinh", nullable = false)
    private String tenChuongTrinh;

    @Column(name = "NgayBatDau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "TrangThai")
    private Integer trangThai;
}