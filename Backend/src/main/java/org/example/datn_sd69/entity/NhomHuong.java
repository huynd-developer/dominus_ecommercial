package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "NhomHuongs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhomHuong extends BaseEntity {

    @Column(name = "TenNhomHuong", nullable = false)
    private String tenNhomHuong;

    @Column(name = "TrangThai")
    private Integer trangThai;
}
