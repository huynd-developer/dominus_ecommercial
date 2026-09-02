package org.example.datn_sd69.modules.product.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.product.dto.request.ProductRequest;
import org.example.datn_sd69.modules.product.dto.response.ProductImageResponse;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.example.datn_sd69.repository.*;
import org.example.datn_sd69.repository.projection.ProductVariantInventoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final ReviewRepository reviewRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ConcentrationRepository concentrationRepository;
    private final CapacityRepository capacityRepository;
    private final BottleTypeRepository bottleTypeRepository;
    private final FragranceFamilyRepository fragranceFamilyRepository;

    private final Cloudinary cloudinary;

    @Override
    public ProductResponse createProduct(ProductRequest request) {

        validateActiveProductNameUnique(
                request.getName(),
                null
        );

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

        product.setFragranceFamilies(
                resolveFragranceFamilies(
                        request.getFragranceFamilyIds()
                )
        );

        Product savedProduct = productRepository.save(product);

        List<ProductVariant> variants = new ArrayList<>();

        Set<String> variantPairSet = new HashSet<>();

        for (ProductRequest.VariantRequestDTO dto : request.getVariants()) {

            Capacity capacity =
                    capacityRepository.findById(dto.getCapacityId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy Capacity"));

            BottleType bottleType =
                    bottleTypeRepository.findById(dto.getBottleTypeId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy BottleType"));

            String pairKey = capacity.getId() + "-" + bottleType.getId();
            if (!variantPairSet.add(pairKey)) {
                throw new RuntimeException("Không được phép có 2 biến thể trùng cả Dung tích và Loại chai giống nhau!");
            }

            ProductVariant variant = new ProductVariant();

            variant.setProduct(savedProduct);
            variant.setCapacity(capacity);
            variant.setBottleType(bottleType);

            String sku = (dto.getSku() != null && !dto.getSku().trim().isEmpty())
                    ? dto.getSku()
                    : generateSku(savedProduct, capacity, bottleType);
            variant.setSku(sku);

            variant.setPrice(dto.getPrice());

            /* ProductVariant chỉ mô tả SKU; tồn + NSX/HSD thật thuộc InventoryLot. */
            variant.setStockQuantity(0);
            variant.setManufacturingDate(null);
            variant.setExpirationDate(null);

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
                findActiveProductForUpdateOrThrow(id);

        /*
         * Revision chỉ đại diện dữ liệu Product/SKU mà form quản lý sản phẩm được phép sửa.
         * Không đưa tồn kho, NSX/HSD theo lot, ảnh hay rating vào revision để tránh conflict giả
         * khi các module kho/ảnh/đánh giá thay đổi độc lập.
         */
        List<ProductVariant> existingVariants =
                productVariantRepository.findByProduct_IdAndIsDeletedFalse(id);

        validateExpectedRevision(
                request.getExpectedRevision(),
                buildProductRevision(product, existingVariants)
        );

        /*
         * Chỉ kiểm tra tên sau khi request đã vượt qua stale protection.
         * Bỏ qua chính Product hiện tại khi kiểm tra trùng.
         */
        validateActiveProductNameUnique(
                request.getName(),
                id
        );

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

        product.setFragranceFamilies(
                resolveFragranceFamilies(
                        request.getFragranceFamilyIds()
                )
        );

        productRepository.save(product);

        /* Chỉ sửa variant chưa xóa mềm; variant cũ vẫn giữ để bảo toàn FK/lịch sử kho. */
        Map<Integer, ProductVariant> existingVariantMap = existingVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        List<ProductVariant> variantsToSave = new ArrayList<>();
        Set<String> variantPairSet = new HashSet<>();

        for (ProductRequest.VariantRequestDTO dto : request.getVariants()) {

            Capacity capacity =
                    capacityRepository.findById(dto.getCapacityId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy Capacity"));

            BottleType bottleType =
                    bottleTypeRepository.findById(dto.getBottleTypeId())
                            .orElseThrow(() ->
                                    new RuntimeException("Không tìm thấy BottleType"));

            String pairKey = capacity.getId() + "-" + bottleType.getId();
            if (!variantPairSet.add(pairKey)) {
                throw new RuntimeException("Không được phép có 2 biến thể trùng cả Dung tích và Loại chai giống nhau!");
            }

            ProductVariant variant;
            boolean isNewVariant;

            if (dto.getId() != null && existingVariantMap.containsKey(dto.getId())) {
                variant = existingVariantMap.get(dto.getId());
                existingVariantMap.remove(dto.getId());
                isNewVariant = false;
            } else {
                variant = new ProductVariant();
                variant.setProduct(product);
                isNewVariant = true;
            }

            variant.setCapacity(capacity);
            variant.setBottleType(bottleType);

            String sku = (dto.getSku() != null && !dto.getSku().trim().isEmpty())
                    ? dto.getSku()
                    : generateSku(product, capacity, bottleType);
            variant.setSku(sku);

            variant.setPrice(dto.getPrice());

            /* Không cập nhật tồn/NSX/HSD từ Product. Variant cũ giữ legacy; variant mới khởi tạo rỗng. */
            if (isNewVariant) {
                variant.setStockQuantity(0);
                variant.setManufacturingDate(null);
                variant.setExpirationDate(null);
            }

            variant.setStatus(dto.getStatus());
            variant.setIsDeleted(false);

            variantsToSave.add(variant);
        }

        /* Không hard delete vì ProductVariant có thể đã được tham chiếu bởi dữ liệu kho/khuyến mãi. */
        if (!existingVariantMap.isEmpty()) {
            existingVariantMap.values().forEach(variant -> {
                variant.setIsDeleted(true);
                variant.setStatus(0);
            });
            productVariantRepository.saveAll(existingVariantMap.values());
        }

        productVariantRepository.saveAll(variantsToSave);

        return getProductById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sản phẩm"
            );
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllProducts(int page, int size) {

        Page<Product> productPage =
                productRepository.findByStatusAndIsDeletedFalse(
                        1,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
                );

        List<ProductResponse> items =
                productPage.getContent().stream().map(this::mapToResponse).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", items);
        result.put("currentPage", productPage.getNumber());
        result.put("totalItems", productPage.getTotalElements());
        result.put("totalPages", productPage.getTotalPages());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllProductsAdmin(int page, int size) {

        Page<Product> productPage =
                productRepository.findByIsDeletedFalse(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
                );

        List<ProductResponse> items =
                productPage.getContent().stream().map(this::mapToResponse).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", items);
        result.put("currentPage", productPage.getNumber());
        result.put("totalItems", productPage.getTotalElements());
        result.put("totalPages", productPage.getTotalPages());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDeletedProductsAdmin(int page, int size) {

        Page<Product> productPage =
                productRepository.findByIsDeletedTrue(
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "id")
                        )
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
                findActiveProductForUpdateOrThrow(id);

        product.setIsDeleted(true);

        productRepository.save(product);
    }
    @Override
    public void restoreProduct(Integer id) {

        Product product =
                findProductForUpdateOrThrow(id);

        if (!Boolean.TRUE.equals(product.getIsDeleted())) {
            throw conflict("Sản phẩm chưa bị xóa.");
        }

        /*
         * Trước khi khôi phục phải kiểm tra tên Product này
         * có đang được Product chưa xóa khác sử dụng hay không.
         *
         * Không đổi status và không tác động variant/tồn kho/ảnh.
         */
        validateActiveProductNameUnique(
                product.getName(),
                product.getId()
        );

        product.setIsDeleted(false);

        productRepository.save(product);
    }

    @Override
    @Transactional
    public String uploadImage(
            Integer productId,
            MultipartFile file,
            Boolean isPrimary
    ) throws Exception {

        /*
         * Kiểm tra sớm để không upload Cloudinary cho product không tồn tại/đã ẩn.
         * Không giữ DB lock trong lúc gọi dịch vụ ngoài.
         */
        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy sản phẩm"
                                ));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw conflict("Sản phẩm đã được ẩn hoặc đã thay đổi.");
        }

        Map uploadResult =
                cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.emptyMap()
                );

        String imageUrl =
                uploadResult.get("secure_url")
                        .toString();

        /*
         * Khóa đúng Product trước khi đổi trạng thái ảnh primary.
         * Mọi mutation ảnh của cùng product đi qua cùng row lock nên không thể
         * kết thúc với nhiều ảnh primary do race.
         */
        Product lockedProduct =
                findActiveProductForUpdateOrThrow(productId);

        if (Boolean.TRUE.equals(isPrimary)) {

            List<ProductImage> oldImages =
                    productImageRepository
                            .findByProduct_Id(productId);

            for (ProductImage image : oldImages) {

                image.setIsPrimary(false);

            }

            productImageRepository.saveAll(oldImages);
        }

        ProductImage image =
                new ProductImage();

        image.setProduct(lockedProduct);

        image.setImageUrl(imageUrl);

        image.setIsPrimary(
                Boolean.TRUE.equals(isPrimary)
        );

        productImageRepository.save(image);

        return imageUrl;
    }

    @Override
    @Transactional
    public void deleteProductImage(
            Integer imageId
    ) {

        ProductImage initialImage =
                productImageRepository.findById(
                                imageId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy ảnh"
                                ));

        Integer productId =
                initialImage.getProduct().getId();

        /*
         * Khóa theo Product để serialize delete / set-primary / upload-primary
         * của cùng một sản phẩm.
         */
        findProductForUpdateOrThrow(productId);

        ProductImage image =
                productImageRepository
                        .findByIdAndProduct_Id(imageId, productId)
                        .orElseThrow(() ->
                                conflict("Ảnh đã thay đổi hoặc đã bị xóa.")
                        );

        boolean wasPrimary =
                Boolean.TRUE.equals(
                        image.getIsPrimary()
                );

        productImageRepository.delete(image);
        productImageRepository.flush();

        if (wasPrimary) {

            List<ProductImage> remainImages =
                    productImageRepository
                            .findByProduct_Id(productId);

            if (!remainImages.isEmpty()) {

                ProductImage first =
                        remainImages.get(0);

                first.setIsPrimary(true);

                productImageRepository.save(first);
            }
        }
    }

    @Override
    @Transactional
    public void setPrimaryImage(
            Integer productId,
            Integer imageId
    ) {

        findActiveProductForUpdateOrThrow(productId);

        productImageRepository
                .findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Ảnh không thuộc sản phẩm."
                        )
                );

        List<ProductImage> images =
                productImageRepository
                        .findByProduct_Id(productId);

        for (ProductImage image : images) {

            image.setIsPrimary(
                    image.getId().equals(imageId)
            );
        }

        productImageRepository.saveAll(images);
    }


    private Set<FragranceFamily> resolveFragranceFamilies(
            Collection<Integer> ids
    ) {

        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }

        Set<Integer> requestedIds =
                ids.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        if (requestedIds.isEmpty()) {
            return new HashSet<>();
        }

        List<FragranceFamily> found =
                fragranceFamilyRepository.findAllById(requestedIds);

        if (found.size() != requestedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Có nhóm hương không tồn tại. Vui lòng tải lại danh sách nhóm hương."
            );
        }

        return new HashSet<>(found);
    }

    /**
     * Đảm bảo tại một thời điểm không tồn tại 2 Product chưa xóa mềm
     * có cùng tên.
     *
     * excludeProductId:
     * - null: CREATE
     * - có giá trị: UPDATE / RESTORE, bỏ qua chính Product đang thao tác
     */
    private void validateActiveProductNameUnique(
            String name,
            Integer excludeProductId
    ) {

        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên sản phẩm không được để trống."
            );
        }

        boolean duplicated;

        if (excludeProductId == null) {

            duplicated =
                    productRepository.existsActiveByName(name);

        } else {

            duplicated =
                    productRepository.existsOtherActiveByName(
                            name,
                            excludeProductId
                    );
        }

        if (duplicated) {
            throw conflict(
                    "Tên sản phẩm \"" + name.trim()
                            + "\" đã được sử dụng bởi một sản phẩm chưa xóa."
            );
        }
    }
    private Product findProductForUpdateOrThrow(Integer id) {

        return productRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));
    }

    private Product findActiveProductForUpdateOrThrow(Integer id) {

        Product product =
                findProductForUpdateOrThrow(id);

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw conflict("Sản phẩm đã được ẩn hoặc đã thay đổi.");
        }

        return product;
    }

    private void validateExpectedRevision(
            String expectedRevision,
            String currentRevision
    ) {

        if (expectedRevision == null
                || expectedRevision.trim().isEmpty()) {
            /*
             * Giữ compatibility cho client cũ.
             * FE quản lý sản phẩm mới phải gửi revision để bật stale protection.
             */
            return;
        }

        if (!expectedRevision.trim().equalsIgnoreCase(currentRevision)) {
            throw conflict(
                    "Sản phẩm đã thay đổi. Vui lòng tải lại dữ liệu mới nhất trước khi lưu."
            );
        }
    }

    private String buildProductRevision(
            Product product,
            List<ProductVariant> activeVariants
    ) {

        StringBuilder value =
                new StringBuilder();

        appendRevisionPart(value, product.getId());
        appendRevisionPart(value, product.getName());
        appendRevisionPart(value, product.getDescription());
        appendRevisionPart(
                value,
                product.getBrand() == null
                        ? null
                        : product.getBrand().getId()
        );
        appendRevisionPart(
                value,
                product.getCategory() == null
                        ? null
                        : product.getCategory().getId()
        );
        appendRevisionPart(
                value,
                product.getConcentration() == null
                        ? null
                        : product.getConcentration().getId()
        );
        appendRevisionPart(value, product.getGender());
        appendRevisionPart(value, product.getIsNiche());
        appendRevisionPart(value, product.getStatus());
        appendRevisionPart(value, product.getIsDeleted());

        List<Integer> fragranceFamilyIds =
                product.getFragranceFamilies() == null
                        ? List.of()
                        : product.getFragranceFamilies()
                        .stream()
                        .map(FragranceFamily::getId)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();

        for (Integer fragranceFamilyId : fragranceFamilyIds) {
            appendRevisionPart(
                    value,
                    "F:" + fragranceFamilyId
            );
        }

        List<ProductVariant> variants =
                activeVariants == null
                        ? List.of()
                        : activeVariants.stream()
                        .sorted(
                                Comparator.comparing(
                                        ProductVariant::getId,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                        )
                        .toList();

        for (ProductVariant variant : variants) {

            appendRevisionPart(value, "V");
            appendRevisionPart(value, variant.getId());
            appendRevisionPart(
                    value,
                    variant.getCapacity() == null
                            ? null
                            : variant.getCapacity().getId()
            );
            appendRevisionPart(
                    value,
                    variant.getBottleType() == null
                            ? null
                            : variant.getBottleType().getId()
            );
            appendRevisionPart(value, variant.getSku());
            appendRevisionPart(
                    value,
                    variant.getPrice() == null
                            ? null
                            : variant.getPrice()
                            .stripTrailingZeros()
                            .toPlainString()
            );
            appendRevisionPart(value, variant.getStatus());
            appendRevisionPart(value, variant.getIsDeleted());

            /*
             * Cố ý KHÔNG hash:
             * - stockQuantity legacy
             * - manufacturingDate / expirationDate legacy
             * - InventoryLot
             * vì chúng không thuộc form Product/SKU và không được gây stale giả.
             */
        }

        return sha256(value.toString());
    }

    private void appendRevisionPart(
            StringBuilder builder,
            Object value
    ) {

        String text =
                value == null
                        ? "<null>"
                        : String.valueOf(value);

        builder.append(text.length())
                .append(':')
                .append(text)
                .append('|');
    }

    private String sha256(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "Không thể tạo revision sản phẩm.",
                    ex
            );
        }
    }

    private ResponseStatusException conflict(
            String message
    ) {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    private String generateSku(Product product,
                               Capacity capacity,
                               BottleType bottleType) {

        String productCode = product.getName()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();

        if (productCode.length() > 5) {
            productCode = productCode.substring(0, 5);
        }

        String capacityCode = String.valueOf(capacity.getValue());

        String bottleCode = bottleType.getName()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();

        if (bottleCode.length() > 3) {
            bottleCode = bottleCode.substring(0, 3);
        }

        String sku;

        do {
            int random = new Random().nextInt(9000) + 1000;

            sku = productCode
                    + "-"
                    + capacityCode
                    + "-"
                    + bottleCode
                    + "-"
                    + random;

        } while (productVariantRepository.existsBySku(sku));

        return sku;
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

        response.setIsDeleted(
                product.getIsDeleted()
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

        response.setImages(

                images.stream()

                        .map(img -> {

                            ProductImageResponse dto =
                                    new ProductImageResponse();

                            dto.setId(
                                    img.getId()
                            );

                            dto.setImageUrl(
                                    img.getImageUrl()
                            );

                            dto.setIsPrimary(
                                    img.getIsPrimary()
                            );

                            return dto;
                        })

                        .toList()
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
                productVariantRepository.findByProduct_IdAndIsDeletedFalse(
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

                            /*
                             * Compatibility ngày: không đọc ProductVariant legacy.
                             * Nếu SKU còn bán được, trả ngày của lot FEFO tiếp theo.
                             */
                            InventoryLot nextSellableLot =
                                    inventoryLotRepository.findNextSellableLot(v.getId())
                                            .orElse(null);

                            dto.setManufacturingDate(
                                    nextSellableLot != null
                                            ? nextSellableLot.getManufacturedDate()
                                            : null
                            );

                            dto.setExpirationDate(
                                    nextSellableLot != null
                                            ? nextSellableLot.getExpirationDate()
                                            : null
                            );

                            dto.setStatus(
                                    v.getStatus()
                            );

                            ProductVariantInventoryProjection inventory =
                                    productVariantRepository.findInventoryByVariantId(v.getId());

                            dto.setTotalQuantity(
                                    inventory != null && inventory.getTotalQuantity() != null
                                            ? inventory.getTotalQuantity()
                                            : 0L
                            );

                            long sellableQuantity =
                                    inventory != null && inventory.getSellableQuantity() != null
                                            ? inventory.getSellableQuantity()
                                            : 0L;

                            dto.setSellableQuantity(sellableQuantity);

                            /*
                             * Compatibility: field cũ vẫn giữ để không phá API/client,
                             * nhưng dữ liệu tồn phải lấy từ InventoryLot sellable.
                             */
                            dto.setStockQuantity(
                                    sellableQuantity > Integer.MAX_VALUE
                                            ? Integer.MAX_VALUE
                                            : (int) sellableQuantity
                            );

                            return dto;
                        })
                        .toList();

        response.setVariants(variantDTOs);
        response.setRevision(buildProductRevision(product, variants));

        // Điểm trung bình + số lượt đánh giá
        Double avgRating =
                reviewRepository.findAverageRatingByProductId(
                        product.getId()
                );

        Long reviewCount =
                reviewRepository.countReviewsByProductId(
                        product.getId()
                );

        response.setRating(
                avgRating != null
                        ? Math.round(avgRating * 10) / 10.0
                        : 0.0
        );

        response.setReviewCount(
                reviewCount != null ? reviewCount : 0L
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(
            Integer productId
    ) {

        return productImageRepository
                .findByProduct_Id(productId)
                .stream()
                .map(img -> {

                    ProductImageResponse dto =
                            new ProductImageResponse();

                    dto.setId(img.getId());

                    dto.setImageUrl(img.getImageUrl());

                    dto.setIsPrimary(
                            img.getIsPrimary()
                    );

                    return dto;

                })
                .toList();
    }
}