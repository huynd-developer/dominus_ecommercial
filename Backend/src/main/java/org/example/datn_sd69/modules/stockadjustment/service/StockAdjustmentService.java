package org.example.datn_sd69.modules.stockadjustment.service;

import org.example.datn_sd69.enums.StockAdjustmentStatus;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentRejectRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentSaveRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentDetailResponse;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface StockAdjustmentService {

    Page<StockAdjustmentListResponse> getList(
            String keyword,
            StockAdjustmentStatus status,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    StockAdjustmentDetailResponse getDetail(Integer id);

    StockAdjustmentDetailResponse create(
            StockAdjustmentSaveRequest request
    );

    StockAdjustmentDetailResponse update(
            Integer id,
            StockAdjustmentSaveRequest request
    );

    StockAdjustmentDetailResponse submit(Integer id);

    StockAdjustmentDetailResponse approve(Integer id);

    StockAdjustmentDetailResponse reject(
            Integer id,
            StockAdjustmentRejectRequest request
    );

    long getPendingCount();
}
