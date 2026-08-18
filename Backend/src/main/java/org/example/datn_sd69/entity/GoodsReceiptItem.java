package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "GoodsReceiptItem",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_GoodsReceiptItem_Receipt_SKU_Lot",
                        columnNames = {
                                "GoodsReceiptId",
                                "ProductVariantId",
                                "LotCode"
                        }
                )
        }
)
public class GoodsReceiptItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GoodsReceiptId", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductVariantId", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "LotCode", nullable = false, length = 100)
    private String lotCode;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UnitCost", precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "ManufacturedDate")
    private LocalDate manufacturedDate;

    @Column(name = "ReceivedDate", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "ExpirationDate", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "Note", length = 500)
    private String note;
}