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
@Table(name = "ReturnRequestItem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReturnRequestId", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderItemId", nullable = false)
    private OrderItem orderItem;

    @Column(name = "ReturnQuantity", nullable = false)
    private Integer returnQuantity;

    @Column(name = "RefundAmount", nullable = false)
    private BigDecimal refundAmount;

    /**
     * 0 = Chờ xử lý
     * 1 = Chấp nhận hoàn
     * 2 = Từ chối hoàn
     * 3 = Đã hoàn tất
     * 4 = Khách hủy yêu cầu
     */
    @Column(name = "Status", nullable = false)
    private Integer status = 0;

    @Nationalized
    @Column(name = "RejectReason", length = 500)
    private String rejectReason;

    @Column(name = "AcceptedAt")
    private LocalDateTime acceptedAt;

    @Column(name = "RejectedAt")
    private LocalDateTime rejectedAt;

    @Column(name = "RefundedAt")
    private LocalDateTime refundedAt;
}
