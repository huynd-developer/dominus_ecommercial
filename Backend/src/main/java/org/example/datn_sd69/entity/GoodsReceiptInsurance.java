package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.id.GoodsReceiptInsuranceId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "GoodsReceiptInsurance")
public class GoodsReceiptInsurance {

    @EmbeddedId
    private GoodsReceiptInsuranceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("goodsReceiptId")
    @JoinColumn(name = "GoodsReceiptId", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("insurancePolicyId")
    @JoinColumn(name = "InsurancePolicyId", nullable = false)
    private InsurancePolicy insurancePolicy;
}