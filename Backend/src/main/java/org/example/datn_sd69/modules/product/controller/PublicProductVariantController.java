package org.example.datn_sd69.modules.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.service.ProductVariantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/product-variants")
@RequiredArgsConstructor
public class PublicProductVariantController {

    private final ProductVariantService productVariantService;

    /**
     * Danh sách Variant đang bán theo Product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getVariants(
            @PathVariable Integer productId) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Lấy danh sách biến thể thành công",
                "data",
                productVariantService.getActiveVariantsByProduct(productId)
        ));

    }

}