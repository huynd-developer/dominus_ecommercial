package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "SanPhams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SanPham extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "ThuongHieuId", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne
    @JoinColumn(name = "NongDoId", nullable = false)
    private NongDo nongDo;

    @ManyToOne
    @JoinColumn(name = "DanhMucId", nullable = false)
    private DanhMuc danhMuc;

    @Column(name = "TenSanPham", nullable = false)
    private String tenSanPham;

    @Column(name = "GioiTinh")
    private Integer gioiTinh; // 0: Nam, 1: Nu, 2: Unisex

    @Column(name = "MoTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "LaNiche")
    private Boolean laNiche;

    @Column(name = "TrangThai")
    private Integer trangThai;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao; // Giữ nguyên trường của SQL gốc

    // Mapping bảng trung gian SanPham_NhomHuong tự động
    @ManyToMany
    @JoinTable(
            name = "SanPham_NhomHuong",
            joinColumns = @JoinColumn(name = "SanPhamId"),
            inverseJoinColumns = @JoinColumn(name = "NhomHuongId")
    )
    private Set<NhomHuong> nhomHuongs;
}