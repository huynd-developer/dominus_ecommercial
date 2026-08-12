package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLotLockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLotLockHistoryRepository
        extends JpaRepository<InventoryLotLockHistory, Long> {

    List<InventoryLotLockHistory>
    findByInventoryLotIdOrderByActionAtDesc(Long inventoryLotId);
}