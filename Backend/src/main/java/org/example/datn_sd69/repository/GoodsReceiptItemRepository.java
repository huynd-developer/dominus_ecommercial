package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Integer> {

    List<GoodsReceiptItem> findByGoodsReceipt_IdOrderByIdAsc(Integer goodsReceiptId);

    void deleteAllByGoodsReceipt_Id(Integer goodsReceiptId);

    boolean existsByGoodsReceipt_Id(Integer goodsReceiptId);
}
