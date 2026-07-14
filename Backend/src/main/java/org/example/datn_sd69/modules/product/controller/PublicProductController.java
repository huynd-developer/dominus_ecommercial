package org.example.datn_sd69.modules.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    /**
     * Danh sách sản phẩm đang hoạt động.
     *
     * GET /api/products?page=0&size=12
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return ResponseEntity.ok(
                productService.getAllProducts(safePage, safeSize)
        );
    }

    /**
     * Chi tiết sản phẩm.
     *
     * GET /api/products/2
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductDetail(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    /**
     * Danh sách biến thể của sản phẩm.
     *
     * GET /api/products/2/variants
     */
    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductResponse.VariantDTO>> getProductVariants(
            @PathVariable Integer id
    ) {
        ProductResponse product =
                productService.getProductById(id);

        List<ProductResponse.VariantDTO> variants =
                product.getVariants() == null
                        ? Collections.emptyList()
                        : product.getVariants();

        return ResponseEntity.ok(variants);
    }
}