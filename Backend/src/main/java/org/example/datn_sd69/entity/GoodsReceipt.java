package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "GoodsReceipt",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_GoodsReceipt_ReceiptNo",
                        columnNames = "ReceiptNo"
                )
        }
)
public class GoodsReceipt extends BaseEntity {

    @Column(name = "ReceiptNo", nullable = false, length = 50)
    private String receiptNo;

    /**
     * 1 = NORMAL_RECEIPT
     * 2 = OPENING_BALANCE
     */
    @Column(name = "ReceiptType", nullable = false)
    private Byte receiptType;

    /**
     * 0 = DRAFT
     * 1 = PENDING_APPROVAL
     * 2 = APPROVED
     * 3 = REJECTED
     * 4 = CANCELLED
     */
    @Column(name = "Status", nullable = false)
    private Byte status;

    @Column(name = "Note", length = 1000)
    private String note;


    // =========================
    // Tạo phiếu
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;


    // =========================
    // Gửi duyệt
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubmittedBy")
    private User submittedBy;

    @Column(name = "SubmittedAt")
    private LocalDateTime submittedAt;


    // =========================
    // Phê duyệt
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private User approvedBy;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;


    // =========================
    // Từ chối
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RejectedBy")
    private User rejectedBy;

    @Column(name = "RejectedAt")
    private LocalDateTime rejectedAt;

    @Column(name = "RejectionReason", length = 500)
    private String rejectionReason;


    // =========================
    // Hủy phiếu
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CancelledBy")
    private User cancelledBy;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;

    @Column(name = "CancellationReason", length = 500)
    private String cancellationReason;
}