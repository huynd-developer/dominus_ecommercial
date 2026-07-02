package org.example.datn_sd69.modules.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.product.dto.*;
import org.example.datn_sd69.modules.product.service.ProductCloudinaryServiceImpl;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.example.datn_sd69.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ConcentrationRepository concentrationRepository;

    private final CapacityRepository capacityRepository;
    private final BottleTypeRepository bottleTypeRepository;

    private final ProductCloudinaryServiceImpl cloudinary;

    // =====================================================
    // CREATE PRODUCT
    // =====================================================

    @Override
    public ProductResponse create(
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images
    ) {

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thương hiệu"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        Concentration concentration = concentrationRepository
                .findById(request.getConcentrationId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy nồng độ"));

        Product product = new Product();

        product.setName(request.getName());
        product.setBrand(brand);
        product.setCategory(category);
        product.setConcentration(concentration);
        product.setDescription(request.getDescription());
        product.setGender(request.getGender());
        product.setIsNiche(request.getIsNiche());
        product.setStatus(request.getStatus());
        product.setCreatedAt(LocalDateTime.now());
        product.setIsDeleted(false);

        product = productRepository.save(product);

        List<ProductImageResponse> imageResponses =
                saveImages(product, primaryImage, images);

        ProductResponse response = mapToResponse(product);

        response.setImages(imageResponses);

        return response;
    }
    // =====================================================
// UPDATE PRODUCT
// =====================================================

    @Override
    public ProductResponse update(
            Integer id,
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images
    ) {

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thương hiệu"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        Concentration concentration = concentrationRepository
                .findById(request.getConcentrationId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy nồng độ"));

        product.setName(request.getName());
        product.setBrand(brand);
        product.setCategory(category);
        product.setConcentration(concentration);
        product.setDescription(request.getDescription());
        product.setGender(request.getGender());
        product.setIsNiche(request.getIsNiche());
        product.setStatus(request.getStatus());

        product = productRepository.save(product);

        /*
         * Upload thêm ảnh nếu có.
         * Nếu muốn chức năng thay thế toàn bộ ảnh cũ thì
         * sẽ xử lý ở phần saveImages()/deleteImages().
         */
        if (primaryImage != null || (images != null && !images.isEmpty())) {
            saveImages(product, primaryImage, images);
        }

        return mapToResponse(product);
    }

    // =====================================================
// DELETE PRODUCT
// =====================================================

    @Override
    public void delete(Integer id) {

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        // Soft delete Product
        product.setIsDeleted(true);
        productRepository.save(product);

        // Soft delete tất cả Variant
        List<ProductVariant> variants =
                productVariantRepository.findByProductIdAndIsDeletedFalse(id);

        for (ProductVariant variant : variants) {
            variant.setIsDeleted(true);
        }

        productVariantRepository.saveAll(variants);

        // Soft delete tất cả Image
        List<ProductImage> images =
                productImageRepository.findByProductIdAndIsDeletedFalse(id);

        for (ProductImage image : images) {
            image.setIsDeleted(true);
        }

        productImageRepository.saveAll(images);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(
            String keyword,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Product> products;

        if (keyword == null || keyword.isBlank()) {
            products = productRepository.findByIsDeletedFalse(pageable);
        } else {
            products = productRepository
                    .findByNameContainingIgnoreCaseAndIsDeletedFalse(
                            keyword,
                            pageable
                    );
        }

        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(
            String keyword,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Product> products;

        if (keyword == null || keyword.isBlank()) {

            products = productRepository
                    .findByStatusAndIsDeletedFalse(
                            1,
                            pageable
                    );

        } else {

            products = productRepository
                    .findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(
                            keyword,
                            1,
                            pageable
                    );

        }

        return products.map(this::mapToResponse);

    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {

        Product product = productRepository
                .findDetailWithVariants(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        return mapToResponse(product);

    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProduct(
            Integer productId
    ) {

        return productVariantRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .stream()
                .map(this::mapVariantToResponse)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getActiveVariantsByProduct(
            Integer productId
    ) {

        return productVariantRepository
                .findByProductIdAndStatusAndIsDeletedFalse(
                        productId,
                        1
                )
                .stream()
                .map(this::mapVariantToResponse)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(
            Integer variantId
    ) {

        ProductVariant variant = productVariantRepository
                .findByIdAndIsDeletedFalse(variantId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy biến thể"));

        return mapVariantToResponse(variant);

    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantBySku(
            String sku
    ) {

        ProductVariant variant = productVariantRepository
                .findBySkuIgnoreCaseAndIsDeletedFalse(sku)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy SKU"));

        return mapVariantToResponse(variant);

    }

    @Override
    public ProductVariantResponse createVariant(
            Integer productId,
            ProductVariantRequest request
    ) {

        Product product = productRepository
                .findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        Capacity capacity = capacityRepository
                .findById(request.getCapacityId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy dung tích"));

        BottleType bottleType = bottleTypeRepository
                .findById(request.getBottleTypeId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy loại chai"));

        // Không cho trùng Capacity + BottleType
        List<ProductVariant> variants =
                productVariantRepository.findByProductIdAndIsDeletedFalse(productId);

        for (ProductVariant item : variants) {

            if (item.getCapacity().getId().equals(capacity.getId())
                    && item.getBottleType().getId().equals(bottleType.getId())) {

                throw new RuntimeException(
                        "Biến thể đã tồn tại"
                );

            }

        }

        ProductVariant variant = new ProductVariant();

        variant.setProduct(product);
        variant.setCapacity(capacity);
        variant.setBottleType(bottleType);

        variant.setPrice(request.getPrice());

        variant.setStockQuantity(request.getStockQuantity());

        variant.setStatus(request.getStatus());

        variant.setIsDeleted(false);

        String sku = generateSku(
                product,
                capacity,
                bottleType
        );

        variant.setSku(sku);

        variant = productVariantRepository.save(variant);

        return mapVariantToResponse(variant);

    }

    @Override
    public ProductVariantResponse updateVariant(
            Integer variantId,
            ProductVariantRequest request
    ) {

        ProductVariant variant =
                productVariantRepository
                        .findByIdAndIsDeletedFalse(variantId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy biến thể"));

        Capacity capacity =
                capacityRepository
                        .findById(request.getCapacityId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy dung tích"));

        BottleType bottleType =
                bottleTypeRepository
                        .findById(request.getBottleTypeId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy loại chai"));

        List<ProductVariant> variants =
                productVariantRepository
                        .findByProductIdAndIsDeletedFalse(
                                variant.getProduct().getId());

        for (ProductVariant item : variants) {

            if (!item.getId().equals(variantId)
                    && item.getCapacity().getId().equals(capacity.getId())
                    && item.getBottleType().getId().equals(bottleType.getId())) {

                throw new RuntimeException(
                        "Biến thể đã tồn tại"
                );

            }

        }

        variant.setCapacity(capacity);

        variant.setBottleType(bottleType);

        variant.setPrice(request.getPrice());

        variant.setStockQuantity(request.getStockQuantity());

        variant.setStatus(request.getStatus());

        variant.setSku(generateSku(
                variant.getProduct(),
                capacity,
                bottleType
        ));

        variant =
                productVariantRepository.save(variant);

        return mapVariantToResponse(variant);

    }

    @Override
    public void deleteVariant(Integer variantId) {

        ProductVariant variant =
                productVariantRepository
                        .findByIdAndIsDeletedFalse(variantId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy biến thể"));

        variant.setIsDeleted(true);

        productVariantRepository.save(variant);

    }

    private String generateSku(
            Product product,
            Capacity capacity,
            BottleType bottleType
    ) {

        String productCode =
                product.getId().toString();

        String capacityCode =
                capacity.getId().toString();

        String bottleCode =
                bottleType.getId().toString();

        return "SP"
                + productCode
                + "-"
                + capacityCode
                + "-"
                + bottleCode;

    }

    private ProductResponse mapToResponse(Product product) {

        ProductResponse res = new ProductResponse();

        res.setId(product.getId());

        res.setName(product.getName());

        res.setBrandId(product.getBrand().getId());
        res.setBrandName(product.getBrand().getName());

        res.setCategoryId(product.getCategory().getId());
        res.setCategoryName(product.getCategory().getName());

        res.setConcentrationId(product.getConcentration().getId());
        res.setConcentrationName(product.getConcentration().getName());

        res.setGender(product.getGender());

        res.setDescription(product.getDescription());

        res.setIsNiche(product.getIsNiche());

        res.setStatus(product.getStatus());

        // ================= IMAGES =================

        List<ProductImageResponse> imageResponses =
                productImageRepository
                        .findByProductIdAndIsDeletedFalse(product.getId())
                        .stream()
                        .map(image -> {

                            ProductImageResponse dto =
                                    new ProductImageResponse();

                            dto.setId(image.getId());

                            dto.setImageUrl(image.getImageUrl());

                            dto.setPrimary(image.getIsPrimary());

                            return dto;

                        })
                        .toList();

        res.setImages(imageResponses);

        // ================= VARIANTS =================

        List<ProductVariantResponse> variants =
                productVariantRepository
                        .findByProductIdAndIsDeletedFalse(product.getId())
                        .stream()
                        .map(this::mapVariantToResponse)
                        .toList();

        res.setVariants(variants);

        return res;

    }

    private ProductVariantResponse mapVariantToResponse(
            ProductVariant variant
    ) {

        ProductVariantResponse dto =
                new ProductVariantResponse();

        dto.setId(variant.getId());

        dto.setProductId(
                variant.getProduct().getId()
        );

        dto.setProductName(
                variant.getProduct().getName()
        );

        dto.setCapacityId(
                variant.getCapacity().getId()
        );

        dto.setCapacityValue(
                variant.getCapacity().getValue()
        );

        dto.setBottleTypeId(
                variant.getBottleType().getId()
        );

        dto.setBottleTypeName(
                variant.getBottleType().getName()
        );

        dto.setSku(
                variant.getSku()
        );

        dto.setPrice(
                variant.getPrice()
        );

        dto.setStockQuantity(
                variant.getStockQuantity()
        );

        dto.setStatus(
                variant.getStatus()
        );

        return dto;

    }

    private List<ProductImageResponse> saveImages(

            Product product,

            MultipartFile primaryImage,

            List<MultipartFile> images

    ) {

        List<ProductImageResponse> result =
                new ArrayList<>();

        // ================= PRIMARY =================

        if (primaryImage != null && !primaryImage.isEmpty()) {

            String imageUrl =
                    cloudinary.uploadImage(primaryImage);

            ProductImage image =
                    new ProductImage();

            image.setProduct(product);

            image.setImageUrl(imageUrl);

            image.setIsPrimary(true);

            image.setIsDeleted(false);

            image =
                    productImageRepository.save(image);

            result.add(

                    new ProductImageResponse(

                            image.getId(),

                            image.getImageUrl(),

                            true

                    )

            );

        }

        // ================= SUB IMAGES =================

        if (images != null) {

            for (MultipartFile file : images) {

                if (file == null || file.isEmpty()) {

                    continue;

                }

                String imageUrl =
                        cloudinary.uploadImage(file);

                ProductImage image =
                        new ProductImage();

                image.setProduct(product);

                image.setImageUrl(imageUrl);

                image.setIsPrimary(false);

                image.setIsDeleted(false);

                image =
                        productImageRepository.save(image);

                result.add(

                        new ProductImageResponse(

                                image.getId(),

                                image.getImageUrl(),

                                false

                        )

                );

            }

        }

        return result;

    }

    private void deleteImages(Product product) {

        List<ProductImage> images =
                productImageRepository
                        .findByProductIdAndIsDeletedFalse(
                                product.getId()
                        );

        for (ProductImage image : images) {

            image.setIsDeleted(true);

        }

        productImageRepository.saveAll(images);

    }
}
