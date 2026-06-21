package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;
import java.time.LocalDateTime;

@Entity
@Table(name = "GioHangs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GioHang extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "KhachHangId", nullable = false)
    private KhachHang khachHang;

    @Column(name = "NgayTao", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime ngayTao;
}