package org.example.datn_sd69.modules.promotion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.promotion.dto.request.PromotionRequest;
import org.example.datn_sd69.modules.promotion.dto.request.PromotionStatusRequest;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/promotions")
@PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    /**
     * Danh sách chiến dịch khuyến mãi.
     */
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(promotionService.getAll(keyword, status, pageable));
    }

    /**
     * Chi tiết chiến dịch + danh sách biến thể đã chọn.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.getById(id));
    }

    /**
     * API cho UI chọn sản phẩm khuyến mãi.
     * Admin không nhập ID thủ công.
     * FE search theo tên sản phẩm / SKU / barcode rồi tick chọn biến thể.
     */
    @GetMapping("/product-variants")
    public ResponseEntity<?> searchProductVariantsForPromotion(
            @RequestParam(required = false) String keyword,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,

            @RequestParam(required = false) Integer ignorePromotionId,

            Pageable pageable
    ) {
        return ResponseEntity.ok(
                promotionService.searchProductVariantsForPromotion(
                        keyword,
                        startDate,
                        endDate,
                        ignorePromotionId,
                        pageable
                )
        );
    }

    /**
     * Tạo chiến dịch.
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.create(request));
    }

    /**
     * Cập nhật chiến dịch.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @Valid @RequestBody PromotionRequest request
    ) {
        return ResponseEntity.ok(promotionService.update(id, request));
    }

    /**
     * Bật / tắt chiến dịch.
     * status: 1 = bật, 0 = tắt.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @Valid @RequestBody PromotionStatusRequest request
    ) {
        return ResponseEntity.ok(promotionService.changeStatus(id, request.getStatus()));
    }

    /**
     * Xóa mềm chiến dịch.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        promotionService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa chiến dịch khuyến mãi thành công"));
    }
}