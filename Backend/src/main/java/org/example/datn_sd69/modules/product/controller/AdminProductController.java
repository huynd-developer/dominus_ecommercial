package org.example.datn_sd69.modules.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.dto.request.ProductRequest;
import org.example.datn_sd69.common.config.response.ApiResponse;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * Chi tiết sản phẩm
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        productService.getProductById(id),
                        "Lấy chi tiết sản phẩm thành công"
                )
        );
    }

    /**
     * Tạo sản phẩm
     */
    @PostMapping("/admin")
    @PreAuthorize("hasAnyAuthority('MANAGER','OWNER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        productService.createProduct(request),
                        "Tạo sản phẩm thành công"
                )
        );
    }

    /**
     * Danh sách sản phẩm public
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        productService.getAllProducts(page, size),
                        "Lấy danh sách sản phẩm thành công"
                )
        );
    }

    /**
     * Danh sách sản phẩm admin
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('MANAGER','OWNER','CASHIER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllProductsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        productService.getAllProductsAdmin(page, size),
                        "Lấy danh sách sản phẩm Admin thành công"
                )
        );
    }

    /**
     * Cập nhật sản phẩm
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER','OWNER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        productService.updateProduct(id, request),
                        "Cập nhật sản phẩm thành công"
                )
        );
    }

    /**
     * Xóa mềm sản phẩm
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('OWNER')")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Integer id) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Ẩn sản phẩm thành công"
                )
        );
    }

    /**
     * Upload ảnh sản phẩm
     */
    @PostMapping("/admin/{id}/images")
    @PreAuthorize("hasAnyAuthority('MANAGER','OWNER')")
    public ResponseEntity<ApiResponse<String>> uploadProductImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws Exception {

        String imageUrl =
                productService.uploadImage(id, file);

        return ResponseEntity.ok(
                ApiResponse.success(
                        imageUrl,
                        "Upload ảnh thành công"
                )
        );
    }

    /**
     * Chọn ảnh đại diện
     */
    @PutMapping("/admin/{productId}/images/{imageId}/primary")
    @PreAuthorize("hasAnyAuthority('MANAGER','OWNER')")
    public ResponseEntity<ApiResponse<String>> setPrimaryImage(
            @PathVariable Integer productId,
            @PathVariable Integer imageId) {

        productService.setPrimaryImage(
                productId,
                imageId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Đặt ảnh chính thành công"
                )
        );
    }

    /**
     * Xóa ảnh
     */
    @DeleteMapping("/admin/images/{imageId}")
    @PreAuthorize("hasAnyAuthority('MANAGER','OWNER')")
    public ResponseEntity<ApiResponse<String>> deleteImage(
            @PathVariable Integer imageId) {

        productService.deleteProductImage(imageId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Xóa ảnh thành công"
                )
        );
    }
}