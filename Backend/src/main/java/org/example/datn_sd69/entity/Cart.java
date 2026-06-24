package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "Cart")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Cart extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerId", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
