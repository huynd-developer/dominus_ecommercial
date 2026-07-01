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

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){

        return ResponseEntity.ok(
                Map.of(
                        "status","SUCCESS",
                        "message","Danh sách sản phẩm",
                        "data",
                        productService.getActiveProducts(
                                keyword,
                                page,
                                size
                        )
                )
        );

    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Integer id
    ){

        return ResponseEntity.ok(
                Map.of(
                        "status","SUCCESS",
                        "message","Chi tiết sản phẩm",
                        "data",
                        productService.getById(id)
                )
        );

    }

}