package org.example.datn_sd69.modules.goodsreceipt.service;

import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.*;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface GoodsReceiptService {

    Page<GoodsReceiptListResponse> getList(
            String keyword,
            GoodsReceiptStatus status,
            GoodsReceiptType receiptType,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    GoodsReceiptDetailResponse getDetail(Integer id);

    GoodsReceiptDetailResponse create(GoodsReceiptSaveRequest request);

    GoodsReceiptDetailResponse update(Integer id, GoodsReceiptSaveRequest request);

    GoodsReceiptDetailResponse submit(Integer id);

    GoodsReceiptDetailResponse approve(Integer id);

    GoodsReceiptDetailResponse reject(Integer id, GoodsReceiptRejectRequest request);

    GoodsReceiptDetailResponse cancel(Integer id, GoodsReceiptCancelRequest request);

    List<GoodsReceiptApprovalHistoryResponse> getApprovalHistory(Integer id);

    PendingReceiptCountResponse getPendingCount();
}
