package org.example.datn_sd69.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "OrderDeliveryEvidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryEvidence extends BaseEntity {

    /**
     * 1 = Giao hàng thành công
     * 2 = Giao hàng thất bại
     */
    @Column(name = "EvidenceType", nullable = false)
    private Integer evidenceType;

    @Nationalized
    @Column(name = "ImageUrl", length = 1000, nullable = false)
    private String imageUrl;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}