package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "OrderRefund")
@Data
public class OrderRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @Column(name = "RefundType", nullable = false)
    private String refundType;

    @Column(name = "Amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "BankName", nullable = false)
    private String bankName;

    @Column(name = "BankAccountNumber", nullable = false)
    private String bankAccountNumber;

    @Column(name = "BankAccountHolder", nullable = false)
    private String bankAccountHolder;

    @Column(name = "Status")
    private Integer status = 0;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "RefundedAt")
    private LocalDateTime refundedAt;

    @Column(name = "RefundedBy")
    private Integer refundedBy;
}