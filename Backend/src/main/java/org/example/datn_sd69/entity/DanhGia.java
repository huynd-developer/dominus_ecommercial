package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;
import java.time.LocalDateTime;

@Entity
@Table(name = "DanhGias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DanhGia extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "ChiTietDonHangId", nullable = false)
    private ChiTietDonHang chiTietDonHang;

    @Column(name = "DiemSoSao", nullable = false)
    private Integer diemSoSao;

    @Column(name = "BinhLuan", columnDefinition = "NVARCHAR(MAX)")
    private String binhLuan;

    @Column(name = "NgayDanhGia")
    private LocalDateTime ngayDanhGia;
}