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
@Table(name = "StockMovement")
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InventoryLotId", nullable = false)
    private InventoryLot inventoryLot;

    /**
     * 1 = RECEIPT_IN
     * 2 = OPENING_IN
     * 3 = SALE_OUT
     * 4 = RETURN_IN
     * 5 = ADJUST_IN
     * 6 = ADJUST_OUT
     * 7 = DISPOSAL_OUT
     */
    @Column(name = "MovementType", nullable = false)
    private Byte movementType;

    @Column(name = "QuantityChange", nullable = false)
    private Integer quantityChange;

    @Column(name = "QuantityBefore", nullable = false)
    private Integer quantityBefore;

    @Column(name = "QuantityAfter", nullable = false)
    private Integer quantityAfter;

    @Column(name = "ReferenceType", length = 50)
    private String referenceType;

    /**
     * BIGINT vì có thể tham chiếu cả
     * GoodsReceipt.Id BIGINT
     * hoặc các chứng từ khác.
     */
    @Column(name = "ReferenceId")
    private Long referenceId;

    @Column(name = "ReferenceLineId")
    private Long referenceLineId;

    @Column(name = "Reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
}