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
import org.example.datn_sd69.modules.product.service.CloudinaryService;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.example.datn_sd69.repository.BrandRepository;
import org.example.datn_sd69.repository.CategoryRepository;
import org.example.datn_sd69.repository.ConcentrationRepository;
import org.example.datn_sd69.repository.ProductImageRepository;
import org.example.datn_sd69.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    private final CloudinaryService cloudinaryService;

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

        Concentration concentration =
                concentrationRepository.findById(request.getConcentrationId())
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

        product.setStatus(request.getStatus());

        product.setCreatedAt(LocalDateTime.now());

        product = productRepository.save(product);

        List<ProductImageResponse> imageResponses =
                saveImages(product, primaryImage, images);

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());

        response.setName(product.getName());

        response.setBrandId(brand.getId());

        response.setBrandName(brand.getName());

        response.setCategoryId(category.getId());

        response.setCategoryName(category.getName());

        response.setConcentrationId(concentration.getId());

        response.setConcentrationName(concentration.getName());

        response.setDescription(product.getDescription());

        response.setGender(product.getGender());

        response.setIsNiche(product.getIsNiche());

        response.setStatus(product.getStatus());

        response.setImages(imageResponses);

        return response;

    }

    @Override
    public ProductResponse update(
            Integer id,
            ProductRequest request,
            MultipartFile primaryImage,
            List<MultipartFile> images) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() ->
                        new RuntimeException("Brand không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category không tồn tại"));

        Concentration concentration =
                concentrationRepository.findById(request.getConcentrationId())
                        .orElseThrow(() ->
                                new RuntimeException("Concentration không tồn tại"));

    /*
        Update thông tin sản phẩm
     */

        product.setName(request.getName());

        product.setBrand(brand);

        product.setCategory(category);

        product.setConcentration(concentration);

        product.setDescription(request.getDescription());

        product.setGender(request.getGender());

        product.setIsNiche(request.getIsNiche());

        product.setStatus(request.getStatus());

        product = productRepository.save(product);

        deleteImages(product);

        List<ProductImageResponse> imageResponses =
                saveImages(product, primaryImage, images);

        ProductResponse response =
                new ProductResponse();

        response.setId(product.getId());

        response.setName(product.getName());

        response.setBrandId(
                brand.getId());

        response.setBrandName(
                brand.getName());

        response.setCategoryId(
                category.getId());

        response.setCategoryName(
                category.getName());

        response.setConcentrationId(
                concentration.getId());

        response.setConcentrationName(
                concentration.getName());

        response.setDescription(
                product.getDescription());

        response.setGender(
                product.getGender());

        response.setIsNiche(
                product.getIsNiche());

        response.setStatus(
                product.getStatus());

        response.setImages(
                imageResponses);

        return response;

    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        return mapToResponse(product);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(
            String keyword,
            int page,
            int size) {

        Page<Product> productPage;

        if (keyword == null || keyword.trim().isEmpty()) {

            productPage = productRepository.findAll(
                    PageRequest.of(page, size)
            );

        } else {

            productPage = productRepository
                    .findByNameContainingIgnoreCase(
                            keyword,
                            PageRequest.of(page, size)
                    );

        }

        List<ProductResponse> responses = productPage
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageImpl<>(
                responses,
                productPage.getPageable(),
                productPage.getTotalElements()
        );

    }

    @Override
    public void delete(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sản phẩm không tồn tại"));

        deleteImages(product);

        productRepository.delete(product);

    }

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

    private List<ProductImageResponse> saveImages(

            Product product,

            MultipartFile primaryImage,

            List<MultipartFile> images

    ) {

        List<ProductImageResponse> responses =
                new ArrayList<>();

        if (primaryImage != null &&
                !primaryImage.isEmpty()) {

            ProductImage image = new ProductImage();

            image.setProduct(product);

            image.setImageUrl(
                    cloudinaryService.uploadImage(primaryImage)
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

        if (images != null) {

            for (MultipartFile file : images) {

                if (file == null || file.isEmpty()) {

                    continue;

                }

                ProductImage image = new ProductImage();

                image.setProduct(product);

                image.setImageUrl(
                        cloudinaryService.uploadImage(file)
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

    private void deleteImages(Product product) {

        List<ProductImage> images =
                productImageRepository.findByProductId(
                        product.getId()
                );

        for (ProductImage image : images) {

            cloudinaryService.deleteImage(
                    image.getImageUrl()
            );

        }

        productImageRepository.deleteAll(images);

    }



}