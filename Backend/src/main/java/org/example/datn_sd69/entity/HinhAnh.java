package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "HinhAnhs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HinhAnh extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "SanPhamId", nullable = false)
    private SanPham sanPham;

    @Column(name = "DuongDanAnh", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String duongDanAnh;

    @Column(name = "LaAnhChinh")
    private Boolean laAnhChinh;
}
