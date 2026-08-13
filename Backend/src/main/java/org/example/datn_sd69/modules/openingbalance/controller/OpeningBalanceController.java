package org.example.datn_sd69.modules.openingbalance.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptRejectRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptApprovalHistoryResponse;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptDetailResponse;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptListResponse;
import org.example.datn_sd69.modules.openingbalance.dto.request.OpeningBalanceSaveRequest;
import org.example.datn_sd69.modules.openingbalance.service.OpeningBalanceService;
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
@RequestMapping("/api/admin/opening-balances")
@RequiredArgsConstructor
@Validated
public class OpeningBalanceController {

    private final OpeningBalanceService openingBalanceService;

    // =========================================================
    // LIST
    // =========================================================

    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<Page<GoodsReceiptListResponse>> getList(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            GoodsReceiptStatus status,

            @RequestParam(required = false)
            Integer createdBy,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Trang phải lớn hơn hoặc bằng 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Số bản ghi phải từ 1 đến 100")
            @Max(value = 100, message = "Số bản ghi phải từ 1 đến 100")
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                openingBalanceService.getList(
                        keyword,
                        status,
                        createdBy,
                        fromDate,
                        toDate,
                        pageable
                )
        );
    }

    // =========================================================
    // DETAIL
    // =========================================================

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<GoodsReceiptDetailResponse> getDetail(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                openingBalanceService.getDetail(id)
        );
    }

    // =========================================================
    // CREATE DRAFT
    // =========================================================

    @PostMapping
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<GoodsReceiptDetailResponse> create(
            @Valid @RequestBody OpeningBalanceSaveRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        openingBalanceService.create(request)
                );
    }

    // =========================================================
    // UPDATE DRAFT
    // =========================================================

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<GoodsReceiptDetailResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody OpeningBalanceSaveRequest request
    ) {

        return ResponseEntity.ok(
                openingBalanceService.update(id, request)
        );
    }

    // =========================================================
    // SUBMIT
    // =========================================================

    @PostMapping("/{id}/submit")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<GoodsReceiptDetailResponse> submit(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                openingBalanceService.submit(id)
        );
    }

    // =========================================================
    // APPROVE
    // =========================================================

    @PostMapping("/{id}/approve")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<GoodsReceiptDetailResponse> approve(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                openingBalanceService.approve(id)
        );
    }

    // =========================================================
    // REJECT
    // =========================================================

    @PostMapping("/{id}/reject")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<GoodsReceiptDetailResponse> reject(
            @PathVariable Integer id,
            @Valid @RequestBody GoodsReceiptRejectRequest request
    ) {

        return ResponseEntity.ok(
                openingBalanceService.reject(id, request)
        );
    }

    // =========================================================
    // APPROVAL HISTORY
    // =========================================================

    @GetMapping("/{id}/approval-history")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<List<GoodsReceiptApprovalHistoryResponse>>
    getApprovalHistory(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                openingBalanceService.getApprovalHistory(id)
        );
    }
}