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

@Entity
@Table(name = "ProductVariant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductVariant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CapacityId", nullable = false)
    private Capacity capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BottleTypeId", nullable = false)
    private BottleType bottleType;

    @Column(name = "Sku", length = 100, nullable = false, unique = true)
    private String sku;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "StockQuantity")
    private Integer stockQuantity = 0;

    @Column(name = "Status")
    private Integer status = 1;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
}
