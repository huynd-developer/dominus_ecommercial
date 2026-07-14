package org.example.datn_sd69.modules.promotion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.promotion.dto.request.PromotionRequest;
import org.example.datn_sd69.modules.promotion.dto.request.PromotionStatusRequest;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(promotionService.getAll(keyword, status, pageable));
    }

    /**
     * Đặt /product-variants TRƯỚC /{id}
     * để tránh trường hợp Spring hiểu nhầm "product-variants" là id.
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

            @PageableDefault(size = 10) Pageable pageable
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @Valid @RequestBody PromotionRequest request
    ) {
        return ResponseEntity.ok(promotionService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @Valid @RequestBody PromotionStatusRequest request
    ) {
        return ResponseEntity.ok(promotionService.changeStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        promotionService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa chiến dịch khuyến mãi thành công"));
    }
}