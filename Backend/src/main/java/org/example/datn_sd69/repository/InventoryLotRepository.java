package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLotRepository
        extends JpaRepository<InventoryLot, Long> {

    List<InventoryLot> findByProductVariantId(Integer productVariantId);
}