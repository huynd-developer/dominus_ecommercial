package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.GoodsReceiptApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptApprovalHistoryRepository
        extends JpaRepository<GoodsReceiptApprovalHistory, Long> {

    List<GoodsReceiptApprovalHistory>
    findByGoodsReceiptIdOrderByActionAtAsc(Long goodsReceiptId);
}