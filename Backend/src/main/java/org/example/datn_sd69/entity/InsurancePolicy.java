package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "InsurancePolicy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_InsurancePolicy_PolicyNumber",
                        columnNames = "PolicyNumber"
                )
        }
)
public class InsurancePolicy extends BaseEntity {

    @Column(name = "PolicyNumber", nullable = false, length = 100)
    private String policyNumber;

    @Column(name = "ProviderName", nullable = false, length = 255)
    private String providerName;

    @Column(
            name = "CoverageAmount",
            precision = 18,
            scale = 2
    )
    private BigDecimal coverageAmount;

    @Column(name = "CoverageStartDate", nullable = false)
    private LocalDate coverageStartDate;

    @Column(name = "CoverageEndDate", nullable = false)
    private LocalDate coverageEndDate;

    @Column(name = "DocumentUrl", length = 1000)
    private String documentUrl;

    @Column(name = "Note", length = 1000)
    private String note;

    @Column(name = "IsCancelled", nullable = false)
    private Boolean isCancelled;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;

    @Column(name = "CancelReason", length = 500)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
}