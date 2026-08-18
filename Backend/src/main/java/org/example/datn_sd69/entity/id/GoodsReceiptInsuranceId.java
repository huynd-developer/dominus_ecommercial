package org.example.datn_sd69.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class GoodsReceiptInsuranceId implements Serializable {

    @Column(name = "GoodsReceiptId")
    private Long goodsReceiptId;

    @Column(name = "InsurancePolicyId")
    private Long insurancePolicyId;
}