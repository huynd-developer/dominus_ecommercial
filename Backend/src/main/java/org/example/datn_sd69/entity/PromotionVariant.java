package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "PromotionVariant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PromotionVariant {
    @EmbeddedId
    private PromotionVariantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("promotionId")
    @JoinColumn(name = "PromotionId")
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productVariantId")
    @JoinColumn(name = "ProductVariantId")
    private ProductVariant productVariant;

    @Column(name = "DiscountPercent", nullable = false)
    private Double discountPercent;
}
