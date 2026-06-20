package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "DungTichs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DungTich extends BaseEntity {

    @Column(name = "GiaTri", nullable = false)
    private Double giaTri;

    @Column(name = "TrangThai")
    private Integer trangThai;
}