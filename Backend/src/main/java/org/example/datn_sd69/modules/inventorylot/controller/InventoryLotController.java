package org.example.datn_sd69.modules.inventorylot.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.inventorylot.dto.response.*;
import org.example.datn_sd69.modules.inventorylot.service.InventoryLotService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/inventory-lots")
@RequiredArgsConstructor
@Validated
public class InventoryLotController {

    private final InventoryLotService inventoryLotService;

    // =========================================================
    // GENERIC LIST
    // =========================================================

    /**
     * Danh sách lô dùng chung.
     *
     * GIỮ NGUYÊN endpoint và hành vi hiện tại để không
     * ảnh hưởng các module đang sử dụng API này.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<InventoryLotListResponse>> getList(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Integer productVariantId,

            @RequestParam(required = false)
            Boolean isExpired,

            @RequestParam(required = false)
            Boolean isNearExpiry,

            @RequestParam(required = false)
            Boolean hasStock,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate expirationFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate expirationTo,

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
                        size
                );

        return ResponseEntity.ok(
                inventoryLotService.getList(
                        keyword,
                        productVariantId,
                        isExpired,
                        isNearExpiry,
                        hasStock,
                        expirationFrom,
                        expirationTo,
                        pageable
                )
        );
    }

    // =========================================================
    // STOCK ADJUSTMENT / INVENTORY AUDIT CANDIDATES
    // =========================================================

    /**
     * API dùng RIÊNG cho popup chọn lô khi tạo/sửa phiếu kiểm kê.
     *
     * Việc xác định lô nào được phép xuất hiện thuộc về
     * InventoryLotService/InventoryLotRepository.
     *
     * Không thay đổi API GET /api/admin/inventory-lots hiện tại,
     * vì API generic vẫn phải phục vụ tra cứu lô/lịch sử.
     */
    @GetMapping("/audit-candidates")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<InventoryLotListResponse>> getAuditCandidates(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Integer productVariantId,

            @RequestParam(required = false)
            Boolean isExpired,

            @RequestParam(required = false)
            Boolean isNearExpiry,

            @RequestParam(required = false)
            Boolean hasStock,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate expirationFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate expirationTo,

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
                        size
                );

        return ResponseEntity.ok(
                inventoryLotService.getAuditCandidates(
                        keyword,
                        productVariantId,
                        isExpired,
                        isNearExpiry,
                        hasStock,
                        expirationFrom,
                        expirationTo,
                        pageable
                )
        );
    }

    // =========================================================
    // DETAIL
    // =========================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<InventoryLotDetailResponse> getDetail(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                inventoryLotService.getDetail(id)
        );
    }

    // =========================================================
    // SOURCE
    // =========================================================

    @GetMapping("/{id}/source")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<InventoryLotSourceResponse> getSource(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                inventoryLotService.getSource(id)
        );
    }
}
