package org.example.datn_sd69.modules.inventorylot.service;

import org.example.datn_sd69.modules.inventorylot.dto.request.InventoryLotLockRequest;
import org.example.datn_sd69.modules.inventorylot.dto.request.InventoryLotUnlockRequest;
import org.example.datn_sd69.modules.inventorylot.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface InventoryLotService {

    Page<InventoryLotListResponse> getList(
            String keyword,
            Integer productVariantId,
            Boolean isLocked,
            Boolean isExpired,
            Boolean isNearExpiry,
            Boolean hasStock,
            LocalDate expirationFrom,
            LocalDate expirationTo,
            Pageable pageable
    );

    InventoryLotDetailResponse getDetail(Integer id);

    InventoryLotSourceResponse getSource(Integer id);

    List<InventoryLotLockHistoryResponse> getLockHistory(Integer id);

    InventoryLotDetailResponse lock(Integer id, InventoryLotLockRequest request);

    InventoryLotDetailResponse unlock(Integer id, InventoryLotUnlockRequest request);
}
