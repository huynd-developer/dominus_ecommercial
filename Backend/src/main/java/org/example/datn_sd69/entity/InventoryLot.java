package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "InventoryLot")
public class InventoryLot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductVariantId", nullable = false)
    private ProductVariant productVariant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GoodsReceiptItemId")
    private GoodsReceiptItem goodsReceiptItem;

    @Column(name = "LotCode", nullable = false, length = 100)
    private String lotCode;

    @Column(name = "ManufacturedDate")
    private LocalDate manufacturedDate;

    @Column(name = "ReceivedDate", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "ExpirationDate", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "InitialQuantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "QuantityOnHand", nullable = false)
    private Integer quantityOnHand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
}