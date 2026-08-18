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
public class InventoryLotInsuranceId implements Serializable {

    @Column(name = "InventoryLotId")
    private Long inventoryLotId;

    @Column(name = "InsurancePolicyId")
    private Long insurancePolicyId;
}