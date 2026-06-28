package org.example.datn_sd69.modules.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.dto.ProductRequest;
import org.example.datn_sd69.modules.product.dto.ProductResponse;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminProductController {

    private final ProductService productService;

    /**
     * Danh sách sản phẩm
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.getAll(keyword, page, size));
    }

    /**
     * Chi tiết sản phẩm
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Thêm sản phẩm
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> create(

            @Valid
            @ModelAttribute ProductRequest request,

            @RequestParam(value = "primaryImage", required = false)
            MultipartFile primaryImage,

            @RequestParam(value = "images", required = false)
            List<MultipartFile> images

    ) {

        return ResponseEntity.ok(
                productService.create(
                        request,
                        primaryImage,
                        images
                )
        );
    }

    /**
     * Sửa sản phẩm
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> update(

            @PathVariable Integer id,

            @Valid
            @ModelAttribute ProductRequest request,

            @RequestParam(value = "primaryImage", required = false)
            MultipartFile primaryImage,

            @RequestParam(value = "images", required = false)
            List<MultipartFile> images

    ) {

        return ResponseEntity.ok(
                productService.update(
                        id,
                        request,
                        primaryImage,
                        images
                )
        );
    }

    /**
     * Xóa sản phẩm
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Integer id
    ) {

        productService.delete(id);

        return ResponseEntity.ok("Xóa sản phẩm thành công.");
    }

}