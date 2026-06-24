package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "CartItem", uniqueConstraints = {@UniqueConstraint(columnNames = {"CartId", "ProductVariantId"})})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CartItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CartId", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductVariantId", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Nationalized
    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "ThumbnailUrl", length = 500)
    private String thumbnailUrl;
}
