package org.example.datn_sd69.modules.stockadjustment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.StockAdjustmentStatus;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentCancelRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentRejectRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentSaveRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentDetailResponse;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentListResponse;
import org.example.datn_sd69.modules.stockadjustment.service.StockAdjustmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/stock-adjustments")
@RequiredArgsConstructor
@Validated
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    // =========================================================
    // LIST
    // =========================================================

    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<Page<StockAdjustmentListResponse>> getList(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            StockAdjustmentStatus status,

            @RequestParam(required = false)
            Integer createdBy,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Trang phải lớn hơn hoặc bằng 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Số bản ghi phải từ 1 đến 100"
            )
            @Max(
                    value = 100,
                    message = "Số bản ghi phải từ 1 đến 100"
            )
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                );

        return ResponseEntity.ok(
                stockAdjustmentService.getList(
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
    // PENDING COUNT
    // =========================================================

    @GetMapping("/pending-count")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<Long> getPendingCount() {

        return ResponseEntity.ok(
                stockAdjustmentService.getPendingCount()
        );
    }

    // =========================================================
    // DETAIL
    // =========================================================

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> getDetail(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                stockAdjustmentService.getDetail(id)
        );
    }

    // =========================================================
    // CREATE DRAFT
    // CASHIER / MANAGER / OWNER
    // =========================================================

    @PostMapping
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> create(
            @Valid
            @RequestBody
            StockAdjustmentSaveRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        stockAdjustmentService.create(
                                request
                        )
                );
    }

    // =========================================================
    // UPDATE DRAFT
    // Người tạo hoặc OWNER
    // =========================================================

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> update(
            @PathVariable Integer id,
            @Valid
            @RequestBody
            StockAdjustmentSaveRequest request
    ) {

        return ResponseEntity.ok(
                stockAdjustmentService.update(
                        id,
                        request
                )
        );
    }

    // =========================================================
    // SUBMIT
    // Người tạo hoặc OWNER
    // =========================================================

    @PostMapping("/{id}/submit")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> submit(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                stockAdjustmentService.submit(id)
        );
    }

    // =========================================================
    // CANCEL DRAFT
    // Người tạo hoặc OWNER
    // =========================================================

    @PostMapping("/{id}/cancel")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> cancel(
            @PathVariable Integer id,
            @Valid
            @RequestBody
            StockAdjustmentCancelRequest request
    ) {

        return ResponseEntity.ok(
                stockAdjustmentService.cancel(
                        id,
                        request
                )
        );
    }

    // =========================================================
    // APPROVE
    // OWNER toàn quyền
    // MANAGER không self-review
    // =========================================================

    @PostMapping("/{id}/approve")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> approve(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                stockAdjustmentService.approve(id)
        );
    }

    // =========================================================
    // REJECT
    // OWNER toàn quyền
    // MANAGER không self-review
    // =========================================================

    @PostMapping("/{id}/reject")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER')"
    )
    public ResponseEntity<StockAdjustmentDetailResponse> reject(
            @PathVariable Integer id,
            @Valid
            @RequestBody
            StockAdjustmentRejectRequest request
    ) {

        return ResponseEntity.ok(
                stockAdjustmentService.reject(
                        id,
                        request
                )
        );
    }
}