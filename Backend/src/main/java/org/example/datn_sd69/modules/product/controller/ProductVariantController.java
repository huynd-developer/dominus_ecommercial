package org.example.datn_sd69.modules.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.dto.ProductVariantRequest;
import org.example.datn_sd69.modules.product.service.ProductVariantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/product-variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    /**
     * Danh sách tất cả Variant
     */
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Lấy danh sách biến thể thành công",
                "data", productVariantService.getAll(keyword, page, size)
        ));
    }

    /**
     * Danh sách Variant theo Product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProduct(
            @PathVariable Integer productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Lấy danh sách biến thể theo sản phẩm",
                "data", productVariantService.getByProductId(
                        productId,
                        keyword,
                        page,
                        size
                )
        ));
    }

    /**
     * Chi tiết Variant
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Lấy chi tiết biến thể",
                "data", productVariantService.getById(id)
        ));
    }

    /**
     * Thêm Variant
     */
    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ProductVariantRequest request) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Thêm biến thể thành công",
                "data", productVariantService.create(request)
        ));
    }

    /**
     * Cập nhật Variant
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductVariantRequest request) {

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Cập nhật biến thể thành công",
                "data", productVariantService.update(id, request)
        ));
    }

    /**
     * Xóa mềm
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Integer id) {

        productVariantService.delete(id);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Xóa biến thể thành công"
        ));
    }

}