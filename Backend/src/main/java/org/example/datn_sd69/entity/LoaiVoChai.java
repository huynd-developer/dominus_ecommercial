package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "LoaiVoChais")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoaiVoChai extends BaseEntity {

    @Column(name = "TenLoaiVo", nullable = false)
    private String tenLoaiVo;

    @Column(name = "TrangThai")
    private Integer trangThai;
}
