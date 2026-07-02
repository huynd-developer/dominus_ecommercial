package org.example.datn_sd69.modules.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.dto.ProductRequest;
import org.example.datn_sd69.modules.product.dto.ProductVariantRequest;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /* ================= PRODUCT ================= */

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                productService.getAll(keyword, page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Integer id) {

        return ResponseEntity.ok(
                productService.getById(id)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(

            @ModelAttribute ProductRequest request,

            @RequestPart(required = false) MultipartFile primaryImage,

            @RequestPart(required = false) List<MultipartFile> images) {

        return ResponseEntity.ok(
                productService.create(
                        request,
                        primaryImage,
                        images
                )
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(

            @PathVariable Integer id,

            @ModelAttribute ProductRequest request,

            @RequestPart(required = false) MultipartFile primaryImage,

            @RequestPart(required = false) List<MultipartFile> images) {

        return ResponseEntity.ok(
                productService.update(
                        id,
                        request,
                        primaryImage,
                        images
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {

        productService.delete(id);

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", "Xóa thành công"
                )
        );
    }

    /* ================= VARIANT ================= */

    @GetMapping("/{productId}/variants")
    public ResponseEntity<?> getVariants(
            @PathVariable Integer productId) {

        return ResponseEntity.ok(
                productService.getVariantsByProduct(productId)
        );
    }

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<?> getVariant(
            @PathVariable Integer variantId) {

        return ResponseEntity.ok(
                productService.getVariantById(variantId)
        );
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<?> createVariant(

            @PathVariable Integer productId,

            @RequestBody ProductVariantRequest request) {

        return ResponseEntity.ok(
                productService.createVariant(
                        productId,
                        request
                )
        );
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<?> updateVariant(

            @PathVariable Integer variantId,

            @RequestBody ProductVariantRequest request) {

        return ResponseEntity.ok(
                productService.updateVariant(
                        variantId,
                        request
                )
        );
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<?> deleteVariant(
            @PathVariable Integer variantId) {

        productService.deleteVariant(variantId);

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", "Xóa biến thể thành công"
                )
        );
    }
}