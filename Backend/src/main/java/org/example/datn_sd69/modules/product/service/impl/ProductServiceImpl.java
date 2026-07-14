package org.example.datn_sd69.modules.product.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.product.dto.request.ProductRequest;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.example.datn_sd69.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

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
    private final FragranceFamilyRepository fragranceFamilyRepository;

    private final Cloudinary cloudinary;
    @Override
    public ProductResponse createProduct(ProductRequest request) {

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Brand"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category"));

        Concentration concentration = concentrationRepository.findById(request.getConcentrationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Concentration"));

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(brand);
        product.setCategory(category);
        product.setConcentration(concentration);
        product.setGender(request.getGender());
        product.setIsNiche(request.getIsNiche());
        product.setStatus(request.getStatus());
        product.setIsDeleted(false);

        if (request.getFragranceFamilyIds() != null
                && !request.getFragranceFamilyIds().isEmpty()) {

            Set<FragranceFamily> families =
                    new HashSet<>(
                            fragranceFamilyRepository.findAllById(
                                    request.getFragranceFamilyIds()
                            )
                    );

            product.setFragranceFamilies(families);
        }

        Product savedProduct = productRepository.save(product);

        List<ProductVariant> variants = new ArrayList<>();

        for (ProductRequest.VariantRequestDTO dto : request.getVariants()) {

            Capacity capacity =
                    capacityRepository.findById(dto.getCapacityId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy Capacity"));

            BottleType bottleType =
                    bottleTypeRepository.findById(dto.getBottleTypeId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy BottleType"));

            ProductVariant variant = new ProductVariant();

            variant.setProduct(savedProduct);
            variant.setCapacity(capacity);
            variant.setBottleType(bottleType);
            variant.setSku(dto.getSku());
            variant.setPrice(dto.getPrice());
            variant.setStockQuantity(dto.getStockQuantity());

            if (dto.getExpirationDate()
                    .isBefore(dto.getManufacturingDate())) {
                throw new RuntimeException(
                        "Hạn sử dụng phải sau ngày sản xuất"
                );
            }

            variant.setManufacturingDate(
                    dto.getManufacturingDate()
            );

            variant.setExpirationDate(
                    dto.getExpirationDate()
            );

            variant.setStatus(dto.getStatus());
            variant.setIsDeleted(false);

            variants.add(variant);
        }

        productVariantRepository.saveAll(variants);

        return getProductById(savedProduct.getId());
    }
    @Override
    public ProductResponse updateProduct(
            Integer id,
            ProductRequest request) {

        Product product =
                productRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy sản phẩm"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Brand"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category"));

        Concentration concentration =
                concentrationRepository.findById(request.getConcentrationId())
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy Concentration"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(brand);
        product.setCategory(category);
        product.setConcentration(concentration);
        product.setGender(request.getGender());
        product.setIsNiche(request.getIsNiche());
        product.setStatus(request.getStatus());

        Set<FragranceFamily> families =
                new HashSet<>(
                        fragranceFamilyRepository.findAllById(
                                request.getFragranceFamilyIds()
                        )
                );

        product.setFragranceFamilies(families);

        productRepository.save(product);

        productVariantRepository.deleteByProduct_Id(id);

        productVariantRepository.flush();

        List<ProductVariant> variants = new ArrayList<>();

        for (ProductRequest.VariantRequestDTO dto : request.getVariants()) {

            Capacity capacity =
                    capacityRepository.findById(dto.getCapacityId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy Capacity"));

            BottleType bottleType =
                    bottleTypeRepository.findById(dto.getBottleTypeId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy BottleType"));

            ProductVariant variant = new ProductVariant();

            variant.setProduct(product);
            variant.setCapacity(capacity);
            variant.setBottleType(bottleType);
            variant.setSku(dto.getSku());
            variant.setPrice(dto.getPrice());
            variant.setStockQuantity(dto.getStockQuantity());

            if (dto.getExpirationDate()
                    .isBefore(dto.getManufacturingDate())) {
                throw new RuntimeException(
                        "Hạn sử dụng phải sau ngày sản xuất"
                );
            }

            variant.setManufacturingDate(
                    dto.getManufacturingDate()
            );

            variant.setExpirationDate(
                    dto.getExpirationDate()
            );

            variant.setStatus(dto.getStatus());
            variant.setIsDeleted(false);

            variants.add(variant);
        }

        productVariantRepository.saveAll(variants);

        return getProductById(id);
    }
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllProducts(
            int page,
            int size) {

        Page<Product> productPage =
                productRepository.findByStatusAndIsDeletedFalse(
                        1,
                        PageRequest.of(page, size)
                );

        List<ProductResponse> items =
                productPage.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        Map<String, Object> result = new HashMap<>();

        result.put("content", items);
        result.put("currentPage", productPage.getNumber());
        result.put("totalItems", productPage.getTotalElements());
        result.put("totalPages", productPage.getTotalPages());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllProductsAdmin(
            int page,
            int size) {

        Page<Product> productPage =
                productRepository.findByIsDeletedFalse(
                        PageRequest.of(page, size)
                );

        List<ProductResponse> items =
                productPage.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        Map<String, Object> result = new HashMap<>();

        result.put("content", items);
        result.put("currentPage", productPage.getNumber());
        result.put("totalItems", productPage.getTotalElements());
        result.put("totalPages", productPage.getTotalPages());

        return result;
    }
    @Override
    public void deleteProduct(Integer id) {

        Product product =
                productRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy sản phẩm"));

        product.setIsDeleted(true);

        productRepository.save(product);
    }
    @Override
    public String uploadImage(
            Integer productId,
            MultipartFile file) throws Exception {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy sản phẩm"));

        Map uploadResult =
                cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.emptyMap()
                );

        String imageUrl =
                uploadResult.get("secure_url").toString();

        ProductImage image = new ProductImage();

        image.setProduct(product);
        image.setImageUrl(imageUrl);

        List<ProductImage> images =
                productImageRepository.findByProduct_Id(productId);

        image.setIsPrimary(images.isEmpty());

        productImageRepository.save(image);

        return imageUrl;
    }
    @Override
    public void deleteProductImage(Integer imageId) {

        ProductImage image =
                productImageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy ảnh"));

        productImageRepository.delete(image);
    }
    @Override
    public void setPrimaryImage(
            Integer productId,
            Integer imageId) {

        ProductImage selectedImage =
                productImageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy ảnh"));

        List<ProductImage> images =
                productImageRepository.findByProduct_Id(productId);

        for (ProductImage image : images) {

            image.setIsPrimary(false);

            productImageRepository.save(image);
        }

        selectedImage.setIsPrimary(true);

        productImageRepository.save(selectedImage);
    }
    private ProductResponse mapToResponse(
            Product product) {

        ProductResponse response =
                new ProductResponse();

        response.setId(product.getId());

        response.setName(product.getName());

        response.setDescription(
                product.getDescription()
        );

        response.setBrandId(
                product.getBrand().getId()
        );

        response.setBrandName(
                product.getBrand().getName()
        );

        response.setCategoryId(
                product.getCategory().getId()
        );

        response.setCategoryName(
                product.getCategory().getName()
        );

        response.setConcentrationId(
                product.getConcentration().getId()
        );

        response.setConcentrationName(
                product.getConcentration().getName()
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
        Set<ProductResponse.FragranceFamilyDTO> fragranceDTOs =
                product.getFragranceFamilies()
                        .stream()
                        .map(f -> {

                            ProductResponse.FragranceFamilyDTO dto =
                                    new ProductResponse.FragranceFamilyDTO();

                            dto.setId(f.getId());
                            dto.setName(f.getName());

                            return dto;

                        })
                        .collect(Collectors.toSet());

        response.setFragranceFamilies(fragranceDTOs);
        List<ProductImage> images =
                productImageRepository.findByProduct_Id(
                        product.getId()
                );

        images.stream()
                .filter(img ->
                        Boolean.TRUE.equals(
                                img.getIsPrimary()
                        ))
                .findFirst()
                .ifPresent(img ->
                        response.setPrimaryImageUrl(
                                img.getImageUrl()
                        ));
        List<ProductVariant> variants =
                productVariantRepository.findByProduct_Id(
                        product.getId()
                );

        List<ProductResponse.VariantDTO> variantDTOs =
                variants.stream()
                        .map(v -> {

                            ProductResponse.VariantDTO dto =
                                    new ProductResponse.VariantDTO();

                            dto.setId(v.getId());

                            dto.setCapacityId(
                                    v.getCapacity().getId()
                            );

                            dto.setCapacityName(
                                    String.valueOf(
                                            v.getCapacity().getValue()
                                    )
                            );

                            dto.setBottleTypeId(
                                    v.getBottleType().getId()
                            );

                            dto.setBottleTypeName(
                                    v.getBottleType().getName()
                            );

                            dto.setSku(
                                    v.getSku()
                            );

                            dto.setPrice(
                                    v.getPrice()
                            );

                            dto.setStockQuantity(
                                    v.getStockQuantity()
                            );

                            dto.setManufacturingDate(
                                    v.getManufacturingDate()
                            );

                            dto.setExpirationDate(
                                    v.getExpirationDate()
                            );

                            dto.setStatus(
                                    v.getStatus()
                            );

                            return dto;
                        })
                        .toList();

        response.setVariants(variantDTOs);

        return response;
    }
}