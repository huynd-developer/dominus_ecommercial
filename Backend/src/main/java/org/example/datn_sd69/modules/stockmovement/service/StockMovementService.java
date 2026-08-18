package org.example.datn_sd69.modules.stockmovement.service;

import org.example.datn_sd69.enums.StockMovementType;

import org.example.datn_sd69.modules.stockmovement.dto.response.StockMovementDetailResponse;
import org.example.datn_sd69.modules.stockmovement.dto.response.StockMovementListResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface StockMovementService {

    Page<StockMovementListResponse> getList(

            String keyword,

            Integer inventoryLotId,

            StockMovementType movementType,

            Integer createdBy,

            String referenceType,

            Integer referenceId,

            LocalDate fromDate,

            LocalDate toDate,

            Pageable pageable
    );


    StockMovementDetailResponse getDetail(
            Integer id
    );
}