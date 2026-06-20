package org.example.datn_sd69.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "NhanViens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhanVien extends BaseEntity {

    @Column(name = "HoTen", nullable = false)
    private String hoTen;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "MatKhauMaHoa", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String matKhauMaHoa;

    @Column(name = "VaiTroId", nullable = false)
    private Integer vaiTroId; // 1: Owner, 2: Manager, 3: Cashier

    @Column(name = "TrangThai")
    private Integer trangThai;
}
