package org.example.datn_sd69.modules.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.product.dto.ProductVariantRequest;
import org.example.datn_sd69.modules.product.dto.ProductVariantResponse;
import org.example.datn_sd69.modules.product.service.ProductVariantService;
import org.example.datn_sd69.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;

    private final ProductRepository productRepository;

    private final CapacityRepository capacityRepository;

    private final BottleTypeRepository bottleTypeRepository;

    @Override
    public ProductVariantResponse create(ProductVariantRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        Capacity capacity = capacityRepository.findById(request.getCapacityId())
                .orElseThrow(() ->
                        new RuntimeException("Dung tích không tồn tại"));

        BottleType bottleType = bottleTypeRepository.findById(request.getBottleTypeId())
                .orElseThrow(() ->
                        new RuntimeException("Loại chai không tồn tại"));

        String sku = request.getSku();

        if (sku == null || sku.isBlank()) {

            sku = generateSku(
                    request.getProductId(),
                    request.getCapacityId(),
                    request.getBottleTypeId()
            );

        }

        if (productVariantRepository.existsBySkuIgnoreCase(sku)) {
            throw new RuntimeException("SKU đã tồn tại.");
        }

        ProductVariant variant = new ProductVariant();

        variant.setProduct(product);

        variant.setCapacity(capacity);

        variant.setBottleType(bottleType);

        variant.setSku(sku);

        variant.setPrice(request.getPrice());

        variant.setStockQuantity(request.getStockQuantity());

        variant.setStatus(request.getStatus());

        variant.setIsDeleted(false);

        variant = productVariantRepository.save(variant);

        return mapToResponse(variant);

    }

    @Override
    public ProductVariantResponse update(
            Integer id,
            ProductVariantRequest request) {

        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Biến thể không tồn tại"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        Capacity capacity = capacityRepository.findById(request.getCapacityId())
                .orElseThrow(() ->
                        new RuntimeException("Dung tích không tồn tại"));

        BottleType bottleType = bottleTypeRepository.findById(request.getBottleTypeId())
                .orElseThrow(() ->
                        new RuntimeException("Loại chai không tồn tại"));

        String sku = request.getSku();

        if (sku == null || sku.isBlank()) {

            sku = generateSku(
                    request.getProductId(),
                    request.getCapacityId(),
                    request.getBottleTypeId()
            );

        }

        if (productVariantRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new RuntimeException("SKU đã tồn tại.");
        }

        variant.setProduct(product);

        variant.setCapacity(capacity);

        variant.setBottleType(bottleType);

        variant.setSku(sku);

        variant.setPrice(request.getPrice());

        variant.setStockQuantity(request.getStockQuantity());

        variant.setStatus(request.getStatus());

        variant = productVariantRepository.save(variant);

        return mapToResponse(variant);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductVariantResponse> getByProductId(
            Integer productId,
            String keyword,
            int page,
            int size) {

        Page<ProductVariant> variantPage;

        if (keyword == null || keyword.isBlank()) {

            variantPage = productVariantRepository
                    .findByProductIdAndIsDeletedFalse(
                            productId,
                            PageRequest.of(page, size)
                    );

        } else {

            variantPage = productVariantRepository
                    .findByProductIdAndSkuContainingIgnoreCaseAndIsDeletedFalse(
                            productId,
                            keyword,
                            PageRequest.of(page, size)
                    );

        }

        return variantPage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductVariantResponse> getAll(
            String keyword,
            int page,
            int size) {

        Page<ProductVariant> variantPage;

        if (keyword == null || keyword.trim().isEmpty()) {

            variantPage = productVariantRepository.findByIsDeletedFalse(
                    PageRequest.of(page, size)
            );

        } else {

            variantPage = productVariantRepository
                    .findBySkuContainingIgnoreCaseAndIsDeletedFalse(
                            keyword,
                            PageRequest.of(page, size)
                    );

        }

        return variantPage.map(this::mapToResponse);

    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getById(Integer id) {

        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Biến thể không tồn tại"));

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            throw new RuntimeException("Biến thể đã bị xóa");
        }

        return mapToResponse(variant);

    }

    @Override
    public void delete(Integer id) {

        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Biến thể không tồn tại"));

        variant.setIsDeleted(true);

        productVariantRepository.save(variant);

    }

    @Override
    public String generateSku(
            Integer productId,
            Integer capacityId,
            Integer bottleTypeId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        Capacity capacity = capacityRepository.findById(capacityId)
                .orElseThrow(() ->
                        new RuntimeException("Dung tích không tồn tại"));

        BottleType bottleType = bottleTypeRepository.findById(bottleTypeId)
                .orElseThrow(() ->
                        new RuntimeException("Loại chai không tồn tại"));

        // VD: Dior Sauvage -> DS
        String productCode = Arrays.stream(product.getName().trim().split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());

        // VD: 100.0 -> 100
        String capacityCode =
                String.valueOf(capacity.getValue().intValue());

        // VD: Chai gốc Fullbox -> FULL
        String bottleName = bottleType.getName().toUpperCase();

        String bottleCode;

        if (bottleName.contains("FULL")) {
            bottleCode = "FULL";
        } else if (bottleName.contains("TEST")) {
            bottleCode = "TEST";
        } else if (bottleName.contains("THỦY")) {
            bottleCode = "GLASS";
        } else if (bottleName.contains("NHỰA")) {
            bottleCode = "PET";
        } else if (bottleName.contains("LIMIT")) {
            bottleCode = "LIMIT";
        } else {
            bottleCode = "BT";
        }

        return productCode + capacityCode + bottleCode;
    }

    private ProductVariantResponse mapToResponse(ProductVariant variant) {

        ProductVariantResponse response = new ProductVariantResponse();

        response.setId(variant.getId());

        response.setProductId(
                variant.getProduct().getId()
        );

        response.setProductName(
                variant.getProduct().getName()
        );

        response.setCapacityId(
                variant.getCapacity().getId()
        );

        response.setCapacityValue(
                variant.getCapacity().getValue()
        );

        response.setBottleTypeId(
                variant.getBottleType().getId()
        );

        response.setBottleTypeName(
                variant.getBottleType().getName()
        );

        response.setSku(
                variant.getSku()
        );

        response.setPrice(
                variant.getPrice()
        );

        response.setStockQuantity(
                variant.getStockQuantity()
        );

        response.setStatus(
                variant.getStatus()
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getActiveVariantsByProduct(
            Integer productId) {

        return productVariantRepository
                .findByProductIdAndStatusAndIsDeletedFalse(productId, 1)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}