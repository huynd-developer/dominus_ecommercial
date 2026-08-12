package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipt, Long> {
}