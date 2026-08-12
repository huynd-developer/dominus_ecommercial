package org.example.datn_sd69.modules.inventory.service;

import org.example.datn_sd69.enums.InventoryStockStatus;
import org.example.datn_sd69.modules.inventory.dto.InventoryConfigResponse;
import org.example.datn_sd69.modules.inventory.dto.InventoryConfigUpdateRequest;
import org.example.datn_sd69.modules.inventory.dto.InventoryLotStatusResponse;
import org.example.datn_sd69.modules.inventory.dto.InventorySummaryResponse;
import org.example.datn_sd69.modules.inventory.dto.response.InventoryOverviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventorySummaryResponse getSummary();


    Page<InventoryOverviewResponse> getOverview(
            String keyword,
            Boolean nearExpiry,
            Boolean expired,
            Boolean locked,
            InventoryStockStatus stockStatus,
            Pageable pageable
    );


    Page<InventoryLotStatusResponse> getNearExpiryLots(
            String keyword,
            Pageable pageable
    );


    Page<InventoryLotStatusResponse> getExpiredLots(
            String keyword,
            Pageable pageable
    );


    Page<InventoryLotStatusResponse> getLockedLots(
            String keyword,
            Pageable pageable
    );


    InventoryConfigResponse getConfig();


    InventoryConfigResponse updateConfig(
            InventoryConfigUpdateRequest request
    );
}