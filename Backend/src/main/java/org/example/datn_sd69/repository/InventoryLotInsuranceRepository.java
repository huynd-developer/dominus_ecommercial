package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLotInsurance;
import org.example.datn_sd69.entity.id.InventoryLotInsuranceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLotInsuranceRepository
        extends JpaRepository<
        InventoryLotInsurance,
        InventoryLotInsuranceId
        > {
}