package org.example.datn_sd69.modules.goodsreceipt.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.*;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.*;
import org.example.datn_sd69.modules.goodsreceipt.service.GoodsReceiptService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/goods-receipts")
@RequiredArgsConstructor
@Validated
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<GoodsReceiptListResponse>> getList(
            @RequestParam(name = "keyword", required = false)
            String keyword,

            @RequestParam(name = "status", required = false)
            GoodsReceiptStatus status,

            @RequestParam(
                    name = "receiptType",
                    defaultValue = "NORMAL_RECEIPT"
            )
            GoodsReceiptType receiptType,

            @RequestParam(name = "createdBy", required = false)
            Integer createdBy,

            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0")
            int page,

            @RequestParam(name = "size", defaultValue = "20")
            @Min(value = 1, message = "Số bản ghi phải từ 1 đến 100")
            @Max(value = 100, message = "Số bản ghi phải từ 1 đến 100")
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                goodsReceiptService.getList(
                        keyword,
                        status,
                        receiptType,
                        createdBy,
                        fromDate,
                        toDate,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<GoodsReceiptDetailResponse> getDetail(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getDetail(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<GoodsReceiptDetailResponse> create(
            @Valid @RequestBody GoodsReceiptSaveRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(goodsReceiptService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<GoodsReceiptDetailResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody GoodsReceiptSaveRequest request
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.update(id, request)
        );
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<GoodsReceiptDetailResponse> submit(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.submit(id)
        );
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public ResponseEntity<GoodsReceiptDetailResponse> approve(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.approve(id)
        );
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public ResponseEntity<GoodsReceiptDetailResponse> reject(
            @PathVariable Integer id,
            @Valid @RequestBody GoodsReceiptRejectRequest request
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.reject(id, request)
        );
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<GoodsReceiptDetailResponse> cancel(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) GoodsReceiptCancelRequest request
    ) {
        if (request == null) {
            request = new GoodsReceiptCancelRequest();
        }

        return ResponseEntity.ok(
                goodsReceiptService.cancel(id, request)
        );
    }

    @GetMapping("/{id}/approval-history")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<GoodsReceiptApprovalHistoryResponse>> getApprovalHistory(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                goodsReceiptService.getApprovalHistory(id)
        );
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<PendingReceiptCountResponse> getPendingCount() {
        return ResponseEntity.ok(
                goodsReceiptService.getPendingCount()
        );
    }
}