package org.example.datn_sd69.modules.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.entity.Concentration;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductImage;
import org.example.datn_sd69.modules.product.dto.ProductImageResponse;
import org.example.datn_sd69.modules.product.dto.ProductRequest;
import org.example.datn_sd69.modules.product.dto.ProductResponse;
import org.example.datn_sd69.modules.product.service.ProductCloudinaryServiceImpl;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.example.datn_sd69.repository.BrandRepository;
import org.example.datn_sd69.repository.CategoryRepository;
import org.example.datn_sd69.repository.ConcentrationRepository;
import org.example.datn_sd69.repository.ProductImageRepository;
import org.example.datn_sd69.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ProductImageRepository productImageRepository;

    private final BrandRepository brandRepository;

    private final CategoryRepository categoryRepository;

    private final ConcentrationRepository concentrationRepository;

    private final ProductCloudinaryServiceImpl productCloudinaryServiceImpl;

    // ===========================
    // CREATE
    // ===========================

    @Override
    public ProductResponse create(
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images
    ) {

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() ->
                        new RuntimeException("Brand không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category không tồn tại"));

        Concentration concentration = concentrationRepository
                .findById(request.getConcentrationId())
                .orElseThrow(() ->
                        new RuntimeException("Concentration không tồn tại"));

        Product product = new Product();

        product.setName(request.getName());

        product.setBrand(brand);

        product.setCategory(category);

        product.setConcentration(concentration);

        product.setDescription(request.getDescription());

        product.setGender(request.getGender());

        product.setIsNiche(request.getIsNiche());

        product.setStatus(
                request.getStatus() == null
                        ? 1
                        : request.getStatus()
        );

        product.setIsDeleted(false);

        product.setCreatedAt(LocalDateTime.now());

        product = productRepository.save(product);

        List<ProductImageResponse> imageResponses =
                saveImages(product, primaryImage, images);

        ProductResponse response = mapToResponse(product);

        response.setImages(imageResponses);

        return response;
    }

    // ===========================
    // UPDATE
    // ===========================

    @Override
    public ProductResponse update(
            Integer id,
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() ->
                        new RuntimeException("Brand không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category không tồn tại"));

        Concentration concentration = concentrationRepository
                .findById(request.getConcentrationId())
                .orElseThrow(() ->
                        new RuntimeException("Concentration không tồn tại"));

        product.setName(request.getName());

        product.setBrand(brand);

        product.setCategory(category);

        product.setConcentration(concentration);

        product.setDescription(request.getDescription());

        product.setGender(request.getGender());

        product.setIsNiche(request.getIsNiche());

        product.setStatus(request.getStatus());

        product = productRepository.save(product);

        // Nếu có upload ảnh mới thì thay toàn bộ
        if ((primaryImage != null && !primaryImage.isEmpty())
                || (images != null && !images.isEmpty())) {

            deleteImages(product);

            saveImages(product, primaryImage, images);
        }

        return mapToResponse(product);
    }
    // ===========================
    // GET BY ID
    // ===========================

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new RuntimeException("Sản phẩm đã bị xóa");
        }

        return mapToResponse(product);
    }

    // ===========================
    // ADMIN - GET ALL
    // ===========================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(
            String keyword,
            int page,
            int size
    ) {

        Page<Product> productPage;

        if (keyword == null || keyword.trim().isEmpty()) {

            productPage = productRepository.findByIsDeletedFalse(
                    PageRequest.of(page, size)
            );

        } else {

            productPage = productRepository
                    .findByNameContainingIgnoreCaseAndIsDeletedFalse(
                            keyword,
                            PageRequest.of(page, size)
                    );
        }

        return productPage.map(this::mapToResponse);

    }

    // ===========================
    // PUBLIC - GET ACTIVE
    // ===========================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(
            String keyword,
            int page,
            int size
    ) {

        Page<Product> productPage;

        if (keyword == null || keyword.trim().isEmpty()) {

            productPage =
                    productRepository.findByStatusAndIsDeletedFalse(
                            1,
                            PageRequest.of(page, size)
                    );

        } else {

            productPage =
                    productRepository
                            .findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(
                                    keyword,
                                    1,
                                    PageRequest.of(page, size)
                            );

        }

        return productPage.map(this::mapToResponse);

    }

    // ===========================
    // DELETE (SOFT DELETE)
    // ===========================

    @Override
    public void delete(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        deleteImages(product);

        product.setIsDeleted(true);

        productRepository.save(product);

    }
    // ===========================
    // MAP ENTITY -> RESPONSE
    // ===========================

    private ProductResponse mapToResponse(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());

        response.setName(product.getName());

        response.setBrandId(product.getBrand().getId());

        response.setBrandName(product.getBrand().getName());

        response.setCategoryId(product.getCategory().getId());

        response.setCategoryName(product.getCategory().getName());

        response.setConcentrationId(
                product.getConcentration().getId()
        );

        response.setConcentrationName(
                product.getConcentration().getName()
        );

        response.setDescription(
                product.getDescription()
        );

        response.setGender(
                product.getGender()
        );

        response.setIsNiche(
                product.getIsNiche()
        );

        response.setStatus(
                product.getStatus()
        );

        List<ProductImageResponse> images =
                productImageRepository
                        .findByProductId(product.getId())
                        .stream()
                        .map(image -> new ProductImageResponse(

                                image.getId(),

                                image.getImageUrl(),

                                image.getIsPrimary()

                        ))
                        .toList();

        response.setImages(images);

        return response;
    }

    // ===========================
    // SAVE IMAGES
    // ===========================

    private List<ProductImageResponse> saveImages(

            Product product,

            MultipartFile primaryImage,

            List<MultipartFile> images

    ) {

        List<ProductImageResponse> responses =
                new ArrayList<>();

        // ---------- Ảnh đại diện ----------
        if (primaryImage != null &&
                !primaryImage.isEmpty()) {

            ProductImage image = new ProductImage();

            image.setProduct(product);

            image.setImageUrl(
                    productCloudinaryServiceImpl
                            .uploadImage(primaryImage)
            );

            image.setIsPrimary(true);

            image = productImageRepository.save(image);

            responses.add(

                    new ProductImageResponse(

                            image.getId(),

                            image.getImageUrl(),

                            true

                    )

            );

        }

        // ---------- Ảnh phụ ----------
        if (images != null) {

            for (MultipartFile file : images) {

                if (file == null || file.isEmpty()) {

                    continue;

                }

                ProductImage image = new ProductImage();

                image.setProduct(product);

                image.setImageUrl(
                        productCloudinaryServiceImpl
                                .uploadImage(file)
                );

                image.setIsPrimary(false);

                image = productImageRepository.save(image);

                responses.add(

                        new ProductImageResponse(

                                image.getId(),

                                image.getImageUrl(),

                                false

                        )

                );

            }

        }

        return responses;

    }
    // ===========================
    // DELETE IMAGES
    // ===========================

    private void deleteImages(Product product) {

        List<ProductImage> images =
                productImageRepository.findByProductId(
                        product.getId()
                );

        if (images.isEmpty()) {
            return;
        }

        for (ProductImage image : images) {

            try {

                if (image.getImageUrl() != null &&
                        !image.getImageUrl().isBlank()) {

                    productCloudinaryServiceImpl.deleteImage(
                            image.getImageUrl()
                    );

                }

            } catch (Exception ex) {

                // Không làm dừng transaction nếu Cloudinary lỗi
                ex.printStackTrace();

            }

        }

        productImageRepository.deleteAll(images);

    }

}