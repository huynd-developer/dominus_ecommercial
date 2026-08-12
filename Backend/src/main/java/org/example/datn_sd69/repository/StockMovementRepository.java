package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByInventoryLotIdOrderByCreatedAtDesc(
            Long inventoryLotId
    );
}