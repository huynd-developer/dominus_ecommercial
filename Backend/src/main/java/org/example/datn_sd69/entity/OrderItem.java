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
@Table(name = "OrderItem")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ProductVariantId",
            nullable = false
    )
    private ProductVariant productVariant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "OriginalPrice", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "DiscountAmount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "FinalPrice", nullable = false)
    private BigDecimal finalPrice;

    @Nationalized
    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "Image", length = 500)
    private String image;
}
