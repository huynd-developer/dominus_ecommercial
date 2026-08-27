package org.example.datn_sd69.modules.promotion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Promotion;
import org.example.datn_sd69.entity.PromotionVariant;
import org.example.datn_sd69.entity.PromotionVariantId;
import org.example.datn_sd69.modules.promotion.dto.request.PromotionRequest;
import org.example.datn_sd69.modules.promotion.dto.request.PromotionVariantRequest;
import org.example.datn_sd69.modules.promotion.dto.response.FlashSaleProductResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionProductVariantOptionResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionVariantResponse;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.example.datn_sd69.repository.ProductImageRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.PromotionRepository;
import org.example.datn_sd69.repository.PromotionVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final PromotionRepository promotionRepository;
    private final PromotionVariantRepository promotionVariantRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getAll(String keyword, Integer status, Pageable pageable) {
        validateStatusFilter(status);

        return promotionRepository
                .search(normalizeKeyword(keyword), status, normalizePageable(pageable, 10, 100))
                .map(this::toPromotionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getById(Integer id) {
        return toPromotionResponse(findActiveRecord(id));
    }

    @Override
    public PromotionResponse create(PromotionRequest request) {
        /*
         * validateRequest() khóa các ProductVariant đã chọn trước khi check overlap.
         * Vì vậy hai request tạo campaign cùng SKU sẽ được serialize theo SKU.
         */
        validateRequest(request, null);

        Promotion promotion = new Promotion();
        promotion.setName(request.getName().trim());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setStatus(STATUS_ENABLED);
        promotion.setIsDeleted(false);

        Promotion savedPromotion = promotionRepository.save(promotion);
        savePromotionVariants(savedPromotion, request.getVariants());

        return toPromotionResponse(savedPromotion);
    }

    @Override
    public PromotionResponse update(Integer id, PromotionRequest request) {
        Promotion promotion = findActiveRecordForUpdate(id);

        validateExpectedRevision(promotion, request.getExpectedRevision());

        if (isEnded(promotion)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không được sửa chiến dịch khuyến mãi đã kết thúc"
            );
        }

        validateRequest(request, id);

        promotion.setName(request.getName().trim());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());

        promotionVariantRepository.deleteByPromotion_Id(id);
        savePromotionVariants(promotion, request.getVariants());

        return toPromotionResponse(promotion);
    }

    @Override
    public PromotionResponse changeStatus(Integer id, Integer status) {
        // Giữ caller cũ hoạt động; Admin FE mới dùng overload có expectedRevision.
        return changeStatus(id, status, null);
    }

    @Override
    public PromotionResponse changeStatus(Integer id, Integer status, String expectedRevision) {
        Promotion promotion = findActiveRecordForUpdate(id);

        validateExpectedRevision(promotion, expectedRevision);

        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái khuyến mãi không hợp lệ. Chỉ nhận 0 hoặc 1."
            );
        }

        if (status == STATUS_ENABLED) {
            if (isEnded(promotion)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không thể bật lại chiến dịch đã hết hạn"
                );
            }

            validateExistingPromotionBeforeEnable(promotion);
        }

        promotion.setStatus(status);

        return toPromotionResponse(promotion);
    }

    @Override
    public void softDelete(Integer id) {
        // Giữ caller cũ hoạt động; Admin FE mới dùng overload có expectedRevision.
        softDelete(id, null);
    }

    @Override
    public void softDelete(Integer id, String expectedRevision) {
        Promotion promotion = findActiveRecordForUpdate(id);

        validateExpectedRevision(promotion, expectedRevision);

        if (isRunningNow(promotion)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không nên xóa chiến dịch đang diễn ra. Hãy tắt chiến dịch trước."
            );
        }

        promotion.setIsDeleted(true);
        promotion.setStatus(STATUS_DISABLED);
    }

    @Override
    public int syncExpiredPromotions() {
        return promotionRepository.disableExpiredPromotions(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlashSaleProductResponse> getActiveFlashSaleProducts(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        return promotionVariantRepository
                .findActiveFlashSaleVariantsByPromotionTime(
                        now,
                        normalizePageable(pageable, 8, 24)
                )
                .map(this::toFlashSaleProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionProductVariantOptionResponse> searchProductVariantsForPromotion(
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer ignorePromotionId,
            Pageable pageable
    ) {
        return productVariantRepository
                .searchVariantsForPromotion(
                        normalizeKeyword(keyword),
                        normalizePageable(pageable, 10, 50)
                )
                .map(variant -> toPromotionProductVariantOptionResponse(
                        variant,
                        startDate,
                        endDate,
                        ignorePromotionId
                ));
    }

    private void validateRequest(PromotionRequest request, Integer ignorePromotionId) {
        String name = request.getName() == null ? "" : request.getName().trim();

        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chiến dịch khuyến mãi không được để trống");
        }

        if (name.length() < 3 || name.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chiến dịch khuyến mãi phải từ 3 đến 255 ký tự");
        }

        if (request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được để trống");
        }

        if (request.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc không được để trống");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải lớn hơn ngày bắt đầu");
        }

        if (!request.getEndDate().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải lớn hơn thời gian hiện tại");
        }

        if (request.getVariants() == null || request.getVariants().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải chọn ít nhất 1 biến thể sản phẩm");
        }

        if (request.getVariants().size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một chiến dịch chỉ nên áp dụng tối đa 100 biến thể");
        }

        /*
         * Bước 1: giữ nguyên validate DTO/duplicate cũ, chỉ gom ID.
         */
        Set<Integer> selectedVariantIds = new HashSet<>();

        for (PromotionVariantRequest variantRequest : request.getVariants()) {
            validateVariantRequest(variantRequest);

            Integer productVariantId = variantRequest.getProductVariantId();

            if (!selectedVariantIds.add(productVariantId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không được chọn trùng một biến thể trong cùng chiến dịch"
                );
            }
        }

        /*
         * Bước 2: khóa SKU theo ID tăng dần trước khi check overlap.
         * Đây là phần chống race create/update/enable giữa hai campaign.
         */
        List<Integer> orderedVariantIds = selectedVariantIds.stream()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants =
                productVariantRepository.findAllByIdInForPromotionUpdate(orderedVariantIds);

        Map<Integer, ProductVariant> variantsById = new HashMap<>();
        for (ProductVariant variant : lockedVariants) {
            variantsById.put(variant.getId(), variant);
        }

        for (Integer variantId : orderedVariantIds) {
            if (!variantsById.containsKey(variantId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy biến thể sản phẩm"
                );
            }
        }

        /*
         * Khi UPDATE, SKU đã thuộc chính campaign hiện tại được phép giữ lại dù
         * tạm thời hết hàng. Điều này tránh việc một campaign đang/chờ chạy bị
         * buộc phải loại SKU chỉ vì tồn có thể bán tại thời điểm chỉnh sửa = 0.
         * Chỉ SKU mới thêm vào campaign mới phải có tồn có thể bán > 0.
         */
        Set<Integer> existingVariantIds = new HashSet<>();
        if (ignorePromotionId != null) {
            promotionVariantRepository.findDetailByPromotionId(ignorePromotionId)
                    .stream()
                    .map(PromotionVariant::getProductVariant)
                    .filter(variant -> variant != null && variant.getId() != null)
                    .map(ProductVariant::getId)
                    .forEach(existingVariantIds::add);
        }

        /*
         * Bước 3: giữ nguyên business rule eligibility/overlap hiện tại,
         * nhưng chạy khi lock SKU vẫn còn giữ trong transaction.
         */
        for (PromotionVariantRequest variantRequest : request.getVariants()) {
            Integer productVariantId = variantRequest.getProductVariantId();
            ProductVariant productVariant = variantsById.get(productVariantId);

            validateVariantCanJoinPromotion(productVariant);

            long overlapCount = promotionVariantRepository.countOverlapPromotion(
                    productVariantId,
                    request.getStartDate(),
                    request.getEndDate(),
                    ignorePromotionId
            );

            if (overlapCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Biến thể " + productVariant.getSku()
                                + " đã thuộc một chiến dịch khác trong cùng khoảng thời gian"
                );
            }

            if (!existingVariantIds.contains(productVariantId)) {
                validateVariantHasSellableStock(productVariant);
            }
        }
    }

    private void validateVariantRequest(PromotionVariantRequest request) {
        if (request.getProductVariantId() == null || request.getProductVariantId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID biến thể sản phẩm phải lớn hơn 0");
        }

        if (request.getDiscountPercent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm giảm giá không được để trống");
        }

        if (request.getDiscountPercent() <= 0 || request.getDiscountPercent() >= 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm giảm giá phải lớn hơn 0 và nhỏ hơn 100");
        }
    }

    private void validateVariantCanJoinPromotion(ProductVariant productVariant) {
        if (productVariant == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể sản phẩm không tồn tại"
            );
        }

        if (Boolean.TRUE.equals(productVariant.getIsDeleted())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể " + productVariant.getSku() + " đã bị xóa"
            );
        }

        if (productVariant.getStatus() == null || productVariant.getStatus() != STATUS_ENABLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể " + productVariant.getSku() + " đang ngừng bán, không thể thêm vào khuyến mãi"
            );
        }

        if (productVariant.getProduct() == null
                || productVariant.getProduct().getStatus() == null
                || productVariant.getProduct().getStatus() != STATUS_ENABLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm của biến thể " + productVariant.getSku() + " đang ngừng bán"
            );
        }

        if (productVariant.getPrice() == null
                || productVariant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể " + productVariant.getSku() + " chưa có giá bán hợp lệ"
            );
        }

        /*
         * Promotion chỉ quản lý:
         * - SKU áp dụng
         * - phần trăm / giá khuyến mãi
         * - khoảng thời gian chiến dịch
         *
         * Không dùng ProductVariant.stockQuantity, manufacturingDate,
         * expirationDate để quyết định SKU có được tham gia Promotion hay không.
         * Tồn vật lý và HSD thực thuộc InventoryLot.
         */
    }

    /**
     * Chỉ dùng khi thêm SKU mới vào campaign.
     * Tồn được lấy từ InventoryLot (sellableQuantity), tuyệt đối không dùng
     * ProductVariant.stockQuantity legacy.
     */
    private void validateVariantHasSellableStock(ProductVariant productVariant) {
        if (getSellableQuantity(productVariant) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể " + productVariant.getSku()
                            + " đã hết hàng, không thể thêm vào Flash Sale"
            );
        }
    }

    private void validateExistingPromotionBeforeEnable(Promotion promotion) {
        if (promotion.getEndDate() == null || !promotion.getEndDate().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể bật chiến dịch đã hết hạn");
        }

        List<PromotionVariant> variants = promotionVariantRepository.findDetailByPromotionId(promotion.getId());

        if (variants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiến dịch chưa có biến thể sản phẩm");
        }

        List<Integer> orderedVariantIds = variants.stream()
                .map(PromotionVariant::getProductVariant)
                .filter(v -> v != null && v.getId() != null)
                .map(ProductVariant::getId)
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants =
                productVariantRepository.findAllByIdInForPromotionUpdate(orderedVariantIds);

        Map<Integer, ProductVariant> variantsById = new HashMap<>();
        for (ProductVariant variant : lockedVariants) {
            variantsById.put(variant.getId(), variant);
        }

        for (Integer variantId : orderedVariantIds) {
            ProductVariant productVariant = variantsById.get(variantId);

            if (productVariant == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy biến thể sản phẩm"
                );
            }

            validateVariantCanJoinPromotion(productVariant);

            long overlapCount = promotionVariantRepository.countOverlapPromotion(
                    productVariant.getId(),
                    promotion.getStartDate(),
                    promotion.getEndDate(),
                    promotion.getId()
            );

            if (overlapCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Biến thể " + productVariant.getSku()
                                + " đang bị trùng thời gian với chiến dịch khác"
                );
            }
        }
    }

    private void savePromotionVariants(Promotion promotion, List<PromotionVariantRequest> variantRequests) {
        List<PromotionVariant> promotionVariants = variantRequests.stream()
                .map(request -> {
                    ProductVariant productVariant = productVariantRepository.findById(request.getProductVariantId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Không tìm thấy biến thể sản phẩm"
                            ));

                    PromotionVariantId id = new PromotionVariantId(
                            promotion.getId(),
                            productVariant.getId()
                    );

                    PromotionVariant promotionVariant = new PromotionVariant();
                    promotionVariant.setId(id);
                    promotionVariant.setPromotion(promotion);
                    promotionVariant.setProductVariant(productVariant);
                    promotionVariant.setDiscountPercent(request.getDiscountPercent());

                    return promotionVariant;
                })
                .toList();

        promotionVariantRepository.saveAll(promotionVariants);
    }

    private Promotion findActiveRecord(Integer id) {
        return promotionRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy chiến dịch khuyến mãi"
                ));
    }

    /**
     * Mutation-only lookup. Không dùng cho GET/list để tránh lock không cần thiết.
     */
    private Promotion findActiveRecordForUpdate(Integer id) {
        return promotionRepository.findByIdForUpdate(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy chiến dịch khuyến mãi"
                ));
    }

    private void validateExpectedRevision(Promotion promotion, String expectedRevision) {
        if (expectedRevision == null || expectedRevision.trim().isBlank()) {
            // Compatibility cho caller cũ. FE Admin mới sẽ luôn gửi revision khi mutation.
            return;
        }

        List<PromotionVariant> currentVariants = promotionVariantRepository
                .findDetailByPromotionId(promotion.getId());

        String currentRevision = calculateRevision(promotion, currentVariants);

        if (!currentRevision.equalsIgnoreCase(expectedRevision.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chiến dịch khuyến mãi đã thay đổi. Vui lòng tải lại dữ liệu mới nhất."
            );
        }
    }

    private PromotionResponse toPromotionResponse(Promotion promotion) {
        List<PromotionVariant> promotionVariants = promotionVariantRepository
                .findDetailByPromotionId(promotion.getId());

        List<PromotionVariantResponse> variants = promotionVariants
                .stream()
                .map(this::toPromotionVariantResponse)
                .toList();

        return PromotionResponse.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .status(promotion.getStatus())
                .statusText(resolveStatusText(promotion))
                .activeNow(isRunningNow(promotion))
                .ended(isEnded(promotion))
                .variants(variants)
                .revision(calculateRevision(promotion, promotionVariants))
                .build();
    }

    /**
     * Revision chỉ phản ánh dữ liệu thuộc quyền sở hữu Promotion:
     * name/start/end/status/isDeleted + SKU áp dụng + discountPercent.
     *
     * Cố ý KHÔNG đưa ProductVariant.price, tồn kho, NSX/HSD, ảnh vào hash
     * để thay đổi ở module Product/Inventory không tạo false stale cho Promotion.
     */
    private String calculateRevision(
            Promotion promotion,
            List<PromotionVariant> promotionVariants
    ) {
        StringBuilder snapshot = new StringBuilder();

        appendRevisionPart(snapshot, promotion.getName());
        appendRevisionPart(snapshot, promotion.getStartDate());
        appendRevisionPart(snapshot, promotion.getEndDate());
        appendRevisionPart(snapshot, promotion.getStatus());
        appendRevisionPart(snapshot, promotion.getIsDeleted());

        if (promotionVariants != null) {
            promotionVariants.stream()
                    .sorted(Comparator.comparing(
                            pv -> pv.getProductVariant() == null
                                    ? null
                                    : pv.getProductVariant().getId(),
                            Comparator.nullsLast(Integer::compareTo)
                    ))
                    .forEach(pv -> {
                        Integer variantId = pv.getProductVariant() == null
                                ? null
                                : pv.getProductVariant().getId();

                        appendRevisionPart(snapshot, variantId);

                        Double discountPercent = pv.getDiscountPercent();
                        String normalizedDiscount = discountPercent == null
                                ? null
                                : BigDecimal.valueOf(discountPercent)
                                .stripTrailingZeros()
                                .toPlainString();

                        appendRevisionPart(snapshot, normalizedDiscount);
                    });
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    snapshot.toString().getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    private void appendRevisionPart(StringBuilder snapshot, Object value) {
        String text = value == null ? "<null>" : String.valueOf(value);
        snapshot.append(text.length())
                .append(':')
                .append(text)
                .append('|');
    }

    private PromotionVariantResponse toPromotionVariantResponse(PromotionVariant promotionVariant) {
        ProductVariant variant = promotionVariant.getProductVariant();

        BigDecimal originalPrice = variant.getPrice();
        BigDecimal salePrice = calculateSalePrice(originalPrice, promotionVariant.getDiscountPercent());

        return PromotionVariantResponse.builder()
                .productVariantId(variant.getId())
                .sku(variant.getSku())
                .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                .capacity(formatCapacity(variant))
                .bottleType(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .originalPrice(originalPrice)
                .discountPercent(promotionVariant.getDiscountPercent())
                .salePrice(salePrice)
                .stockQuantity(getSellableQuantity(variant))
                .build();
    }

    private FlashSaleProductResponse toFlashSaleProductResponse(PromotionVariant promotionVariant) {
        ProductVariant variant = promotionVariant.getProductVariant();

        BigDecimal originalPrice = variant.getPrice();
        BigDecimal salePrice = calculateSalePrice(originalPrice, promotionVariant.getDiscountPercent());

        // LẤY ẢNH CHÍNH CỦA SẢN PHẨM
        String imageUrl = null;
        if (variant.getProduct() != null && variant.getProduct().getId() != null) {
            // Hoặc nếu m có repository image, có thể query ảnh chính.
            // Cách an toàn nhất là lấy từ danh sách ảnh của sản phẩm thông qua productRepository hoặc productImageRepository:
            imageUrl = productImageRepository.findByProduct_Id(variant.getProduct().getId())
                    .stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(img -> img.getImageUrl())
                    .findFirst()
                    .orElseGet(() -> {
                        // Nếu không có ảnh chính thì lấy tạm ảnh đầu tiên
                        var imgs = productImageRepository.findByProduct_Id(variant.getProduct().getId());
                        return imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
                    });
        }

        return FlashSaleProductResponse.builder()
                .promotionId(promotionVariant.getPromotion().getId())
                .promotionName(promotionVariant.getPromotion().getName())
                .endDate(promotionVariant.getPromotion().getEndDate())
                .productVariantId(variant.getId())
                .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                .sku(variant.getSku())
                .capacity(formatCapacity(variant))
                .bottleType(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .originalPrice(originalPrice)
                .discountPercent(promotionVariant.getDiscountPercent())
                .salePrice(salePrice)
                .stockQuantity(getSellableQuantity(variant))
                .imageUrl(imageUrl) // <-- GÁN ẢNH VÀO ĐÂY LÀ XONG
                .build();
    }

    private PromotionProductVariantOptionResponse toPromotionProductVariantOptionResponse(
            ProductVariant variant,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer ignorePromotionId
    ) {
        boolean available = true;
        String unavailableReason = null;

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            available = false;
            unavailableReason = "Biến thể đã bị xóa";
        }

        if (available && (variant.getStatus() == null || variant.getStatus() != STATUS_ENABLED)) {
            available = false;
            unavailableReason = "Biến thể đang ngừng bán";
        }

        if (available && (variant.getProduct() == null
                || variant.getProduct().getStatus() == null
                || variant.getProduct().getStatus() != STATUS_ENABLED)) {
            available = false;
            unavailableReason = "Sản phẩm đang ngừng bán";
        }

        if (available && (variant.getPrice() == null
                || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            available = false;
            unavailableReason = "Biến thể chưa có giá bán hợp lệ";
        }

        /*
         * Không dùng ProductVariant.stockQuantity / NSX / HSD legacy để quyết định.
         * Tồn hiển thị và điều kiện "Hết hàng" đều lấy từ sellableQuantity thật
         * của InventoryLot.
         */
        int sellableQuantity = getSellableQuantity(variant);

        if (available && startDate != null && endDate != null) {
            if (!endDate.isAfter(startDate)) {
                available = false;
                unavailableReason = "Khoảng thời gian khuyến mãi không hợp lệ";
            } else {
                long overlapCount = promotionVariantRepository.countOverlapPromotion(
                        variant.getId(),
                        startDate,
                        endDate,
                        ignorePromotionId
                );

                if (overlapCount > 0) {
                    available = false;
                    unavailableReason = "Biến thể đã thuộc chiến dịch khác trong cùng thời gian";
                }
            }
        }

        if (available && sellableQuantity <= 0) {
            available = false;
            unavailableReason = "Hết hàng";
        }

        return PromotionProductVariantOptionResponse.builder()
                .productVariantId(variant.getId())
                .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                .sku(variant.getSku())
                .capacity(formatCapacity(variant))
                .bottleType(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                .stockQuantity(sellableQuantity)
                .status(variant.getStatus())
                /*
                 * Giữ nguyên 2 field DTO để không phá contract FE hiện tại.
                 * Đây chỉ là compatibility/display legacy, KHÔNG dùng cho business rule Promotion.
                 */
                .manufacturingDate(variant.getManufacturingDate())
                .expirationDate(variant.getExpirationDate())
                .availableForPromotion(available)
                .unavailableReason(unavailableReason)
                .build();
    }

    /**
     * Tồn có thể bán thật của SKU từ InventoryLot thông qua
     * vw_ProductVariantInventory.
     *
     * Không đọc và không đồng bộ ProductVariant.stockQuantity.
     */
    private int getSellableQuantity(ProductVariant variant) {
        if (variant == null || variant.getId() == null) {
            return 0;
        }

        var inventory = productVariantRepository.findInventoryByVariantId(variant.getId());

        if (inventory == null || inventory.getSellableQuantity() == null) {
            return 0;
        }

        long sellableQuantity = Math.max(0L, inventory.getSellableQuantity());

        if (sellableQuantity > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) sellableQuantity;
    }

    private BigDecimal calculateSalePrice(BigDecimal originalPrice, Double discountPercent) {
        if (originalPrice == null || discountPercent == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount = originalPrice
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return originalPrice.subtract(discountAmount).max(BigDecimal.ZERO);
    }

    private String resolveStatusText(Promotion promotion) {
        if (promotion.getStatus() == null || promotion.getStatus() == STATUS_DISABLED) {
            if (isEnded(promotion)) {
                return "Đã kết thúc";
            }

            return "Đã tắt";
        }

        if (isEnded(promotion)) {
            return "Đã kết thúc";
        }

        if (isRunningNow(promotion)) {
            return "Đang diễn ra";
        }

        if (promotion.getStartDate() != null && LocalDateTime.now().isBefore(promotion.getStartDate())) {
            return "Sắp diễn ra";
        }

        return "Đang bật";
    }

    private boolean isRunningNow(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();

        return promotion.getStatus() != null
                && promotion.getStatus() == STATUS_ENABLED
                && !Boolean.TRUE.equals(promotion.getIsDeleted())
                && promotion.getStartDate() != null
                && promotion.getEndDate() != null
                && !now.isBefore(promotion.getStartDate())
                && now.isBefore(promotion.getEndDate());
    }

    private boolean isEnded(Promotion promotion) {
        return promotion.getEndDate() != null
                && !LocalDateTime.now().isBefore(promotion.getEndDate());
    }

    private String formatCapacity(ProductVariant variant) {
        if (variant.getCapacity() == null || variant.getCapacity().getValue() == null) {
            return null;
        }

        return variant.getCapacity().getValue() + "ml";
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private void validateStatusFilter(Integer status) {
        if (status == null) {
            return;
        }

        if (status != STATUS_DISABLED && status != STATUS_ENABLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái khuyến mãi chỉ được là 0 hoặc 1"
            );
        }
    }

    private Pageable normalizePageable(Pageable pageable, int defaultSize, int maxSize) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, defaultSize);
        }

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? defaultSize : pageable.getPageSize();
        size = Math.min(size, maxSize);

        return PageRequest.of(page, size, pageable.getSort());
    }
}
