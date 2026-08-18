package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "StockAdjustmentItem",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_StockAdjustmentItem_Adjustment_Lot",
                        columnNames = {
                                "StockAdjustmentId",
                                "InventoryLotId"
                        }
                )
        }
)
public class StockAdjustmentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StockAdjustmentId", nullable = false)
    private StockAdjustment stockAdjustment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InventoryLotId", nullable = false)
    private InventoryLot inventoryLot;

    /**
     * Snapshot QuantityOnHand tại thời điểm người dùng kiểm kê.
     */
    @Column(name = "SystemQuantity", nullable = false)
    private Integer systemQuantity;

    /**
     * Số lượng thực tế người dùng đếm được.
     */
    @Column(name = "ActualQuantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "Reason", length = 500)
    private String reason;
}
