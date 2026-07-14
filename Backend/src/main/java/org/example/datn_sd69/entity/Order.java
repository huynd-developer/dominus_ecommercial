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
@Table(name = "Orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerId")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CashierId")
    private Employee cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VoucherId")
    private Voucher voucher;

    @Column(name = "OrderType", length = 50, nullable = false)
    private String orderType;

    @Nationalized
    @Column(name = "CustomerName", length = 255)
    private String customerName;

    @Column(name = "CustomerPhone", length = 15)
    private String customerPhone;

    @Nationalized
    @Column(name = "ShippingAddress", length = 500)
    private String shippingAddress;

    @Column(name = "TotalAmount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "DiscountAmount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "FinalAmount", nullable = false)
    private BigDecimal finalAmount;

    @Nationalized
    @Column(name = "PaymentMethod", length = 50, nullable = false)
    private String paymentMethod;

    /**
     0 = Chờ xác nhận
     1 = Đã xác nhận / Đang chuẩn bị hàng
     2 = Đang giao hàng
     3 = Hoàn thành
     4 = Đã hủy
     5 = Giao hàng thất bại
     6 = Yêu cầu hoàn hàng / đổi trả
     7 = Hoàn hàng / đổi trả hoàn tất
     */
    @Column(name = "Status")
    private Integer status = 0;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Chống cộng điểm nhiều lần khi đơn đã hoàn thành.
     */
    @Column(name = "LoyaltyPointsApplied", nullable = false)
    private Boolean loyaltyPointsApplied = false;

    /**
     * Lưu số điểm đơn hàng này đã cộng cho khách.
     */
    @Column(name = "LoyaltyPointsEarned", nullable = false)
    private Integer loyaltyPointsEarned = 0;

    /**
     * Thời điểm đơn chuyển sang hoàn thành.
     */
    @Column(name = "CompletedAt")
    private LocalDateTime completedAt;
}