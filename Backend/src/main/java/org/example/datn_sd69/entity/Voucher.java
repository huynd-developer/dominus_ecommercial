package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Voucher")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Voucher extends BaseEntity {
    @Column(name = "Code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "DiscountType", length = 50, nullable = false)
    private String discountType;

    @Column(name = "DiscountValue", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "MinOrderValue")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "MaxDiscount")
    private BigDecimal maxDiscount;

    @Column(name = "UsageLimit", nullable = false)
    private Integer usageLimit;

    @Column(name = "UsedCount")
    private Integer usedCount = 0;

    @Column(name = "StartDate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "EndDate", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "Status")
    private Integer status = 1;
}
