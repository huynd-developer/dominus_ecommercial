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

@Entity
@Table(name = "ProductImage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductImage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "ImageUrl", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "IsPrimary")
    private Boolean isPrimary = false;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
}
