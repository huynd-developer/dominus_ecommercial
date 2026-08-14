package org.example.datn_sd69.modules.openingbalance.service;

import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptCancelRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptRejectRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptApprovalHistoryResponse;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptDetailResponse;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptListResponse;
import org.example.datn_sd69.modules.openingbalance.dto.request.OpeningBalanceSaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OpeningBalanceService {

    Page<GoodsReceiptListResponse> getList(
            String keyword,
            GoodsReceiptStatus status,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    GoodsReceiptDetailResponse getDetail(Integer id);

    GoodsReceiptDetailResponse create(OpeningBalanceSaveRequest request);

    GoodsReceiptDetailResponse update(
            Integer id,
            OpeningBalanceSaveRequest request
    );

    GoodsReceiptDetailResponse submit(Integer id);
    GoodsReceiptDetailResponse cancel(
            Integer id,
            GoodsReceiptCancelRequest request
    );
    GoodsReceiptDetailResponse approve(Integer id);

    GoodsReceiptDetailResponse reject(
            Integer id,
            GoodsReceiptRejectRequest request
    );

    List<GoodsReceiptApprovalHistoryResponse> getApprovalHistory(Integer id);
}