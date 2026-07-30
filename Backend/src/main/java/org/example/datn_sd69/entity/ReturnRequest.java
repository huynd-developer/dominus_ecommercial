package org.example.datn_sd69.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "ReturnRequest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    /**
     * 1 = Đã nhận hàng nhưng hàng có vấn đề
     * 2 = Chưa nhận hàng / nhận thiếu hàng
     */
    @Column(name = "ReturnType", nullable = false)
    private Integer returnType;

    @Nationalized
    @Column(name = "Reason", nullable = false, length = 255)
    private String reason;

    @Nationalized
    @Column(name = "Description", length = 2000)
    private String description;

    /**
     * 1 = Chuyển khoản ngân hàng
     * 2 = Nhận hoàn tại cửa hàng
     */
    @Column(name = "RefundMethod", nullable = false)
    private Integer refundMethod;

    @Nationalized
    @Column(name = "BankName", length = 100)
    private String bankName;

    @Column(name = "BankAccountNumber", length = 30)
    private String bankAccountNumber;

    @Nationalized
    @Column(name = "BankAccountHolder", length = 100)
    private String bankAccountHolder;

    /**
     * Tổng tiền hoàn của yêu cầu.
     * BE tự tính từ các dòng ReturnRequestItem.
     */
    @Column(name = "RefundAmount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
