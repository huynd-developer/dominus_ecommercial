package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.io.Serializable;

@Embeddable
@Getter @Setter @EqualsAndHashCode
@NoArgsConstructor @AllArgsConstructor
public class PromotionVariantId implements Serializable {
    private Integer promotionId;
    private Integer productVariantId;
}
