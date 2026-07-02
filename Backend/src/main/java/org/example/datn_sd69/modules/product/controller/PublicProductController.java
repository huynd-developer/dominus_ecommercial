package org.example.datn_sd69.modules.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    /* ================= PRODUCT ================= */

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Danh sách sản phẩm",
                "data", productService.getActiveProducts(keyword, page, size)
        ));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(
            @PathVariable Integer productId
    ) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Chi tiết sản phẩm",
                "data", productService.getById(productId)
        ));
    }

    /* ================= PRODUCT VARIANTS ================= */

    @GetMapping("/{productId}/variants")
    public ResponseEntity<?> getVariants(
            @PathVariable Integer productId
    ) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Danh sách biến thể",
                "data", productService.getActiveVariantsByProduct(productId)
        ));
    }

    @GetMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<?> getVariant(
            @PathVariable Integer productId,
            @PathVariable Integer variantId
    ) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Chi tiết biến thể",
                "data", productService.getVariantById(variantId)
        ));
    }

    @GetMapping("/variants/sku/{sku}")
    public ResponseEntity<?> getVariantBySku(
            @PathVariable String sku
    ) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Thông tin biến thể",
                "data", productService.getVariantBySku(sku)
        ));
    }
}