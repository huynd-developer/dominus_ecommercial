package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "StockAdjustment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_StockAdjustment_AdjustmentNo",
                        columnNames = "AdjustmentNo"
                )
        }
)
public class StockAdjustment extends BaseEntity {

    @Column(name = "AdjustmentNo", nullable = false, length = 50)
    private String adjustmentNo;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubmittedBy")
    private User submittedBy;

    @Column(name = "SubmittedAt")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private User approvedBy;

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RejectedBy")
    private User rejectedBy;

    @Column(name = "RejectedAt")
    private LocalDateTime rejectedAt;

    @Column(name = "RejectionReason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CancelledBy")
    private User cancelledBy;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;

    @Column(name = "CancellationReason", length = 500)
    private String cancellationReason;

    @OneToMany(
            mappedBy = "stockAdjustment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<StockAdjustmentItem> items = new ArrayList<>();

    public void addItem(StockAdjustmentItem item) {
        items.add(item);
        item.setStockAdjustment(this);
    }

    public void clearItems() {
        items.clear();
    }
}