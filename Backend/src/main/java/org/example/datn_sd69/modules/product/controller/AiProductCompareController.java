package org.example.datn_sd69.modules.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.common.config.response.ApiResponse;
import org.example.datn_sd69.modules.product.dto.request.AiProductCompareRequest;
import org.example.datn_sd69.modules.product.dto.response.AiProductCompareResponse;
import org.example.datn_sd69.modules.product.service.AiProductCompareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/compare")
@RequiredArgsConstructor
public class AiProductCompareController {

    private final AiProductCompareService aiProductCompareService;

    @PostMapping("/ai")
    public ResponseEntity<ApiResponse<AiProductCompareResponse>> compareProductsWithAi(
            @Valid @RequestBody AiProductCompareRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        aiProductCompareService.compareProducts(
                                request.getProductIds()
                        ),
                        "Phân tích so sánh sản phẩm bằng AI thành công"
                )
        );
    }
}