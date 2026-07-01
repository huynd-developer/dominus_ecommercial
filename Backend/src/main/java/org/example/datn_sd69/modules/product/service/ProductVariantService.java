package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.modules.product.dto.ProductVariantRequest;
import org.example.datn_sd69.modules.product.dto.ProductVariantResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductVariantService {

    Page<ProductVariantResponse> getAll(
            String keyword,
            int page,
            int size
    );

    Page<ProductVariantResponse> getByProductId(
            Integer productId,
            String keyword,
            int page,
            int size
    );

    ProductVariantResponse getById(Integer id);

    ProductVariantResponse create(
            ProductVariantRequest request
    );

    ProductVariantResponse update(
            Integer id,
            ProductVariantRequest request
    );

    void delete(Integer id);

    String generateSku(
            Integer productId,
            Integer capacityId,
            Integer bottleTypeId
    );

    /**
     * API Public
     */
    List<ProductVariantResponse> getActiveVariantsByProduct(
            Integer productId
    );

}