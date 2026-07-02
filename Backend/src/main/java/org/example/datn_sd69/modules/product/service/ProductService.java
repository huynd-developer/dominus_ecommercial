package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.modules.product.dto.ProductRequest;
import org.example.datn_sd69.modules.product.dto.ProductResponse;
import org.example.datn_sd69.modules.product.dto.ProductVariantRequest;
import org.example.datn_sd69.modules.product.dto.ProductVariantResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    /* ================= PRODUCT ================= */

    Page<ProductResponse> getAll(
            String keyword,
            int page,
            int size
    );

    Page<ProductResponse> getActiveProducts(
            String keyword,
            int page,
            int size
    );

    ProductResponse getById(Integer id);

    ProductResponse create(
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images
    );

    ProductResponse update(
            Integer id,
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images
    );

    void delete(Integer id);

    /* ================= VARIANT ================= */

    ProductVariantResponse getVariantById(Integer variantId);

    List<ProductVariantResponse> getVariantsByProduct(Integer productId);

    List<ProductVariantResponse> getActiveVariantsByProduct(Integer productId);

    ProductVariantResponse createVariant(
            Integer productId,
            ProductVariantRequest request
    );

    ProductVariantResponse updateVariant(
            Integer variantId,
            ProductVariantRequest request
    );

    void deleteVariant(Integer variantId);

    ProductVariantResponse getVariantBySku(String sku);

}