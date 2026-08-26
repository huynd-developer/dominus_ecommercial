package org.example.datn_sd69.modules.inventorylot.service;

import org.example.datn_sd69.modules.inventorylot.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface InventoryLotService {

    /**
     * Danh sách lô dùng chung cho module quản lý lô.
     */
    Page<InventoryLotListResponse> getList(
            String keyword,
            Integer productVariantId,
            Boolean isExpired,
            Boolean isNearExpiry,
            Boolean hasStock,
            LocalDate expirationFrom,
            LocalDate expirationTo,
            Pageable pageable
    );

    /**
     * Danh sách lô được phép đưa vào phiếu kiểm kê mới.
     *
     * Product/SKU đã xóa và không còn tồn vật lý sẽ không xuất hiện.
     * Product/SKU đã xóa nhưng còn QuantityOnHand > 0 vẫn phải
     * xuất hiện để tránh làm thất lạc tồn kho.
     */
    Page<InventoryLotListResponse> getAuditCandidates(
            String keyword,
            Integer productVariantId,
            Boolean isExpired,
            Boolean isNearExpiry,
            Boolean hasStock,
            LocalDate expirationFrom,
            LocalDate expirationTo,
            Pageable pageable
    );

    InventoryLotDetailResponse getDetail(Integer id);

    InventoryLotSourceResponse getSource(Integer id);
}