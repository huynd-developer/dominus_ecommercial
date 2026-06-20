package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "ChiTietFlashSales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietFlashSale extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "ChuongTrinhFlashSaleId", nullable = false)
    private ChuongTrinhFlashSale chuongTrinhFlashSale;

    @ManyToOne
    @JoinColumn(name = "BienTheSanPhamId", nullable = false)
    private BienTheSanPham bienTheSanPham;

    @Column(name = "PhanTramGiamGia", nullable = false)
    private Integer phanTramGiamGia;
}
