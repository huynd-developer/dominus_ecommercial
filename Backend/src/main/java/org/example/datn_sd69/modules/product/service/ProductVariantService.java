package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.modules.product.dto.ProductVariantRequest;
import org.example.datn_sd69.modules.product.dto.ProductVariantResponse;
import org.springframework.data.domain.Page;

public interface ProductVariantService {

    /**
     * Danh sách tất cả Variant
     */
    Page<ProductVariantResponse> getAll(
            String keyword,
            int page,
            int size
    );

    /**
     * Danh sách Variant theo Product
     */
    Page<ProductVariantResponse> getByProductId(
            Integer productId,
            String keyword,
            int page,
            int size
    );

    /**
     * Chi tiết Variant
     */
    ProductVariantResponse getById(Integer id);

    /**
     * Thêm Variant
     */
    ProductVariantResponse create(
            ProductVariantRequest request
    );

    /**
     * Cập nhật Variant
     */
    ProductVariantResponse update(
            Integer id,
            ProductVariantRequest request
    );

    /**
     * Xóa mềm
     */
    void delete(Integer id);

    /**
     * Sinh SKU tự động
     */
    String generateSku(
            Integer productId,
            Integer capacityId,
            Integer bottleTypeId
    );

}