package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.id.GoodsReceiptInsuranceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptInsuranceRepository
        extends JpaRepository<
        GoodsReceiptInsurance,
        GoodsReceiptInsuranceId
        > {
}