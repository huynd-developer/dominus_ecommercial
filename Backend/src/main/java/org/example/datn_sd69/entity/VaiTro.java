package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "VaiTros")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VaiTro extends BaseEntity {
    @Column(name = "TenVaiTro", nullable = false, length = 255)
    private String tenVaiTro;

    @Column(name = "MoTa", length = 500)
    private String moTa;
}