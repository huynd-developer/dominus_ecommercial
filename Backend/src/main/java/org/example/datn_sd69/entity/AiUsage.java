package org.example.datn_sd69.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(
        name = "AiUsage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_AiUsage_UserId_UsageDate",
                        columnNames = {"UserId", "UsageDate"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage extends BaseEntity {

    /*
     * ID của tài khoản trong bảng Users.
     *
     * Chỉ lưu scalar UserId, không tạo relation @ManyToOne
     * để module quota AI độc lập và không làm ảnh hưởng
     * tới lifecycle/delete của User hiện tại.
     */
    @Column(name = "UserId", nullable = false)
    private Integer userId;

    /*
     * Ngày áp dụng quota.
     *
     * Mỗi user có một record riêng cho từng ngày.
     */
    @Column(name = "UsageDate", nullable = false)
    private LocalDate usageDate;

    /*
     * Số lượt so sánh bằng AI đã sử dụng trong ngày.
     *
     * Backend giới hạn tối đa 5 lượt/ngày.
     */
    @Column(name = "UsedCount", nullable = false)
    private Integer usedCount = 0;
}