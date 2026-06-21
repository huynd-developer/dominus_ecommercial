package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;
import java.time.LocalDateTime;

@Entity
@Table(name = "NhanViens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhanVien extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "VaiTroId", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "HoTen", nullable = false)
    private String hoTen;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "MatKhauMaHoa", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String matKhauMaHoa;

    @Column(name = "SoDienThoai", length = 15)
    private String soDienThoai;

    @Column(name = "DiaChi", length = 500)
    private String diaChi;

    @Column(name = "TrangThai")
    private Integer trangThai;

    @Column(name = "NgayTao")
    private LocalDateTime ngayTao;
}