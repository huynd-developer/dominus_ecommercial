package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "NongDos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NongDo extends BaseEntity {

    @Column(name = "TenNongDo", nullable = false, length = 100)
    private String tenNongDo;

    @Column(name = "TrangThai")
    private Integer trangThai;
}