package org.example.datn_sd69.modules.expiryalert.service;

import org.example.datn_sd69.enums.ExpiryAlertGroup;
import org.example.datn_sd69.modules.expiryalert.dto.response.ExpiryAlertListResponse;
import org.example.datn_sd69.modules.expiryalert.dto.response.ExpiryAlertSummaryResponse;
import org.example.datn_sd69.modules.inventorylot.dto.response.InventoryLotDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpiryAlertService {

    Page<ExpiryAlertListResponse> getList(
            ExpiryAlertGroup group,
            String keyword,
            Integer fromDays,
            Integer toDays,
            Pageable pageable
    );

    ExpiryAlertSummaryResponse getSummary();

    InventoryLotDetailResponse getDetail(Integer id);
}