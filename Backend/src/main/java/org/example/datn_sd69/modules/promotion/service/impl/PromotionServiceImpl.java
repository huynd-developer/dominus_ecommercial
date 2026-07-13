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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
        Promotion promotion = findActiveRecord(id);

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
        Promotion promotion = findActiveRecord(id);

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
        Promotion promotion = findActiveRecord(id);

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
        LocalDate today = LocalDate.now();

        return promotionVariantRepository
                .findActiveFlashSaleVariants(
                        now,
                        today,
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên chiến dịch khuyến mãi không được để trống"
            );
        }

        if (name.length() < 3 || name.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên chiến dịch khuyến mãi phải từ 3 đến 255 ký tự"
            );
        }

        if (request.getStartDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được để trống"
            );
        }

        if (request.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc không được để trống"
            );
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải lớn hơn ngày bắt đầu"
            );
        }

        if (!request.getEndDate().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải lớn hơn thời gian hiện tại"
            );
        }

        if (request.getVariants() == null || request.getVariants().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phải chọn ít nhất 1 biến thể sản phẩm"
            );
        }

        if (request.getVariants().size() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Một chiến dịch chỉ nên áp dụng tối đa 100 biến thể"
            );
        }

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

            ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy biến thể sản phẩm"
                    ));

            validateVariantCanJoinPromotion(productVariant, request.getStartDate(), request.getEndDate());

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
        }
    }

    private void validateVariantRequest(PromotionVariantRequest request) {
        if (request.getProductVariantId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể sản phẩm không được để trống"
            );
        }

        if (request.getProductVariantId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID biến thể sản phẩm phải lớn hơn 0"
            );
        }

        if (request.getDiscountPercent() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá không được để trống"
            );
        }

        if (request.getDiscountPercent() <= 0 || request.getDiscountPercent() >= 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá phải lớn hơn 0 và nhỏ hơn 100"
            );
        }
    }

    private void validateVariantCanJoinPromotion(
            ProductVariant productVariant,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
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

        if (productVariant.getStockQuantity() == null || productVariant.getStockQuantity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể " + productVariant.getSku() + " đã hết hàng, không thể thêm vào khuyến mãi"
            );
        }

        validateProductVariantDate(productVariant, startDate, endDate);
    }

    private void validateProductVariantDate(
            ProductVariant productVariant,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        if (productVariant.getManufacturingDate() != null) {
            LocalDateTime manufacturingDateTime = productVariant
                    .getManufacturingDate()
                    .atStartOfDay();

            if (startDate.isBefore(manufacturingDateTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chiến dịch không được bắt đầu trước ngày sản xuất của biến thể "
                                + productVariant.getSku()
                );
            }
        }

        if (productVariant.getExpirationDate() != null) {
            LocalDateTime expirationDateTime = productVariant
                    .getExpirationDate()
                    .atTime(23, 59, 59);

            if (LocalDateTime.now().isAfter(expirationDateTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Biến thể " + productVariant.getSku()
                                + " đã hết hạn sử dụng, không thể thêm vào khuyến mãi"
                );
            }

            if (endDate.isAfter(expirationDateTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chiến dịch không được kết thúc sau hạn sử dụng của biến thể "
                                + productVariant.getSku()
                );
            }
        }
    }

    private void validateExistingPromotionBeforeEnable(Promotion promotion) {
        if (promotion.getEndDate() == null || !promotion.getEndDate().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể bật chiến dịch đã hết hạn"
            );
        }

        List<PromotionVariant> variants = promotionVariantRepository.findDetailByPromotionId(promotion.getId());

        if (variants.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chiến dịch chưa có biến thể sản phẩm"
            );
        }

        for (PromotionVariant promotionVariant : variants) {
            ProductVariant productVariant = promotionVariant.getProductVariant();

            validateVariantCanJoinPromotion(
                    productVariant,
                    promotion.getStartDate(),
                    promotion.getEndDate()
            );

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

    private PromotionResponse toPromotionResponse(Promotion promotion) {
        List<PromotionVariantResponse> variants = promotionVariantRepository
                .findDetailByPromotionId(promotion.getId())
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
                .build();
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
                .stockQuantity(variant.getStockQuantity())
                .build();
    }

    private FlashSaleProductResponse toFlashSaleProductResponse(PromotionVariant promotionVariant) {
        ProductVariant variant = promotionVariant.getProductVariant();

        BigDecimal originalPrice = variant.getPrice();
        BigDecimal salePrice = calculateSalePrice(originalPrice, promotionVariant.getDiscountPercent());

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
                .stockQuantity(variant.getStockQuantity())
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

        if (variant.getStatus() == null || variant.getStatus() != STATUS_ENABLED) {
            available = false;
            unavailableReason = "Biến thể đang ngừng bán";
        }

        if (available && (variant.getProduct() == null
                || variant.getProduct().getStatus() == null
                || variant.getProduct().getStatus() != STATUS_ENABLED)) {
            available = false;
            unavailableReason = "Sản phẩm đang ngừng bán";
        }

        if (available && (variant.getStockQuantity() == null || variant.getStockQuantity() <= 0)) {
            available = false;
            unavailableReason = "Biến thể đã hết hàng";
        }

        if (available && variant.getExpirationDate() != null) {
            LocalDateTime expirationDateTime = variant.getExpirationDate().atTime(23, 59, 59);

            if (LocalDateTime.now().isAfter(expirationDateTime)) {
                available = false;
                unavailableReason = "Biến thể đã hết hạn sử dụng";
            }
        }

        if (available && startDate != null && endDate != null) {
            if (!endDate.isAfter(startDate)) {
                available = false;
                unavailableReason = "Khoảng thời gian khuyến mãi không hợp lệ";
            } else {
                if (variant.getManufacturingDate() != null
                        && startDate.isBefore(variant.getManufacturingDate().atStartOfDay())) {
                    available = false;
                    unavailableReason = "Ngày bắt đầu trước ngày sản xuất";
                }

                if (available && variant.getExpirationDate() != null
                        && endDate.isAfter(variant.getExpirationDate().atTime(23, 59, 59))) {
                    available = false;
                    unavailableReason = "Ngày kết thúc sau hạn sử dụng";
                }

                if (available) {
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
        }

        return PromotionProductVariantOptionResponse.builder()
                .productVariantId(variant.getId())
                .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                .sku(variant.getSku())
                .capacity(formatCapacity(variant))
                .bottleType(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .status(variant.getStatus())
                .manufacturingDate(variant.getManufacturingDate())
                .expirationDate(variant.getExpirationDate())
                .availableForPromotion(available)
                .unavailableReason(unavailableReason)
                .build();
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