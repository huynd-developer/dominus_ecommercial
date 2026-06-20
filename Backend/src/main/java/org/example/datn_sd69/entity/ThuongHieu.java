package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "ThuongHieus")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThuongHieu extends BaseEntity {

    @Column(name = "TenThuongHieu", nullable = false)
    private String tenThuongHieu;

    @Column(name = "MoTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "TrangThai")
    private Integer trangThai;
}
