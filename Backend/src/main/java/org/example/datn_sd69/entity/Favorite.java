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

import java.time.LocalDateTime;

@Entity
@Table(name = "Favorite", uniqueConstraints = {@UniqueConstraint(columnNames = {"CustomerId", "ProductVariantsId"})})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Favorite extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerId", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductVariantsId", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
