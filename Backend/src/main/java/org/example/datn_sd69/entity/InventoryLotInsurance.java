package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.id.InventoryLotInsuranceId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "InventoryLotInsurance")
public class InventoryLotInsurance {

    @EmbeddedId
    private InventoryLotInsuranceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("inventoryLotId")
    @JoinColumn(name = "InventoryLotId", nullable = false)
    private InventoryLot inventoryLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("insurancePolicyId")
    @JoinColumn(name = "InsurancePolicyId", nullable = false)
    private InsurancePolicy insurancePolicy;
}