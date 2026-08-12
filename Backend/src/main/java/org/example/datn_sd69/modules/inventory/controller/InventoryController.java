package org.example.datn_sd69.modules.inventory.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.InventoryStockStatus;
import org.example.datn_sd69.modules.inventory.dto.InventoryConfigResponse;
import org.example.datn_sd69.modules.inventory.dto.InventoryConfigUpdateRequest;
import org.example.datn_sd69.modules.inventory.dto.InventoryLotStatusResponse;
import org.example.datn_sd69.modules.inventory.dto.InventorySummaryResponse;
import org.example.datn_sd69.modules.inventory.dto.response.InventoryOverviewResponse;
import org.example.datn_sd69.modules.inventory.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;


    /*
     * =========================================================
     * DASHBOARD CARDS
     * OWNER / MANAGER / CASHIER đều được xem
     * =========================================================
     */

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<InventorySummaryResponse> getSummary() {

        return ResponseEntity.ok(
                inventoryService.getSummary()
        );
    }


    /*
     * =========================================================
     * TỔNG QUAN THEO SKU
     * OWNER / MANAGER / CASHIER đều được xem
     * =========================================================
     */

    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<InventoryOverviewResponse>> getOverview(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Boolean nearExpiry,

            @RequestParam(required = false)
            Boolean expired,

            @RequestParam(required = false)
            Boolean locked,

            @RequestParam(defaultValue = "ALL")
            InventoryStockStatus stockStatus,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Trang phải lớn hơn hoặc bằng 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Số bản ghi mỗi trang phải lớn hơn hoặc bằng 1"
            )
            @Max(
                    value = 100,
                    message = "Số bản ghi mỗi trang không được vượt quá 100"
            )
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                inventoryService.getOverview(
                        keyword,
                        nearExpiry,
                        expired,
                        locked,
                        stockStatus,
                        pageable
                )
        );
    }


    /*
     * =========================================================
     * SẮP HẾT HẠN
     * OWNER / MANAGER / CASHIER đều được xem
     * =========================================================
     */

    @GetMapping("/near-expiry")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<InventoryLotStatusResponse>>
    getNearExpiryLots(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Trang phải lớn hơn hoặc bằng 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Số bản ghi mỗi trang phải lớn hơn hoặc bằng 1"
            )
            @Max(
                    value = 100,
                    message = "Số bản ghi mỗi trang không được vượt quá 100"
            )
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                inventoryService.getNearExpiryLots(
                        keyword,
                        pageable
                )
        );
    }


    /*
     * =========================================================
     * HẾT HẠN
     * OWNER / MANAGER / CASHIER đều được xem
     * =========================================================
     */

    @GetMapping("/expired")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<InventoryLotStatusResponse>>
    getExpiredLots(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Trang phải lớn hơn hoặc bằng 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Số bản ghi mỗi trang phải lớn hơn hoặc bằng 1"
            )
            @Max(
                    value = 100,
                    message = "Số bản ghi mỗi trang không được vượt quá 100"
            )
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                inventoryService.getExpiredLots(
                        keyword,
                        pageable
                )
        );
    }


    /*
     * =========================================================
     * LÔ ĐANG KHÓA
     * OWNER / MANAGER / CASHIER đều được xem
     * =========================================================
     */

    @GetMapping("/locked")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<InventoryLotStatusResponse>>
    getLockedLots(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Trang phải lớn hơn hoặc bằng 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "Số bản ghi mỗi trang phải lớn hơn hoặc bằng 1"
            )
            @Max(
                    value = 100,
                    message = "Số bản ghi mỗi trang không được vượt quá 100"
            )
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                inventoryService.getLockedLots(
                        keyword,
                        pageable
                )
        );
    }


    /*
     * =========================================================
     * XEM CẤU HÌNH CẢNH BÁO HSD
     * OWNER / MANAGER / CASHIER đều được xem
     * =========================================================
     */

    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<InventoryConfigResponse> getConfig() {

        return ResponseEntity.ok(
                inventoryService.getConfig()
        );
    }


    /*
     * =========================================================
     * SỬA CẤU HÌNH CẢNH BÁO HSD
     * CHỈ OWNER / MANAGER
     * CASHIER KHÔNG ĐƯỢC SỬA
     * =========================================================
     */

    @PutMapping("/config")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public ResponseEntity<InventoryConfigResponse> updateConfig(

            @Valid
            @RequestBody
            InventoryConfigUpdateRequest request
    ) {

        return ResponseEntity.ok(
                inventoryService.updateConfig(request)
        );
    }
}