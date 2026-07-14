package org.example.datn_sd69.modules.cart.service;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.PromotionVariant;
import org.example.datn_sd69.modules.cart.dto.response.CartItemResponse;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.PromotionVariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int STATUS_ACTIVE = 1;

    private final PromotionVariantRepository promotionVariantRepository;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductVariantRepository variantRepo;
    private final CustomerRepository customerRepo;

    @Transactional
    public void addVariantToCart(
            Integer customerId,
            Integer variantId,
            Integer quantity,
            String note,
            String thumbnailUrl
    ) {
        validateCustomerId(customerId);
        validateVariantId(variantId);
        validateAddQuantity(quantity);

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thông tin khách hàng"
                ));

        Cart cart = cartRepo.findByCustomerUserId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(customer);
                    return cartRepo.save(newCart);
                });

        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Biến thể sản phẩm không tồn tại"
                ));

        validateVariantCanAddToCart(variant, quantity);

        CartItem item = cartItemRepo.findByCartIdAndProductVariantId(cart.getId(), variantId)
                .orElse(null);

        if (item != null) {
            int currentQuantity = item.getQuantity() == null ? 0 : item.getQuantity();
            int newQuantity = currentQuantity + quantity;

            validateVariantCanAddToCart(variant, newQuantity);

            item.setQuantity(newQuantity);

            String cleanNote = normalizeOptionalText(note, 255, "Ghi chú");
            if (cleanNote != null) {
                item.setNote(cleanNote);
            }

            String cleanThumbnail = normalizeOptionalText(thumbnailUrl, 500, "Ảnh sản phẩm");
            if (cleanThumbnail != null) {
                item.setThumbnailUrl(cleanThumbnail);
            }

            cartItemRepo.save(item);
            return;
        }

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setProductVariant(variant);
        newItem.setQuantity(quantity);
        newItem.setNote(normalizeOptionalText(note, 255, "Ghi chú"));
        newItem.setThumbnailUrl(normalizeOptionalText(thumbnailUrl, 500, "Ảnh sản phẩm"));

        cartItemRepo.save(newItem);
    }

    @Transactional
    public void updateCartItemQuantity(
            Integer customerId,
            Integer cartItemId,
            Integer newQuantity
    ) {
        validateCustomerId(customerId);
        validateCartItemId(cartItemId);

        if (newQuantity == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng không được để trống"
            );
        }

        if (newQuantity < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng không được âm"
            );
        }

        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm trong giỏ hàng"
                ));

        validateCartItemOwner(item, customerId);

        if (newQuantity == 0) {
            cartItemRepo.delete(item);
            return;
        }

        ProductVariant variant = item.getProductVariant();

        if (variant == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm trong giỏ hàng không tồn tại"
            );
        }

        validateVariantCanAddToCart(variant, newQuantity);

        item.setQuantity(newQuantity);
        cartItemRepo.save(item);
    }

    @Transactional
    public void removeCartItem(Integer customerId, Integer cartItemId) {
        validateCustomerId(customerId);
        validateCartItemId(cartItemId);

        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm trong giỏ hàng"
                ));

        validateCartItemOwner(item, customerId);

        cartItemRepo.delete(item);
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCartByCustomerId(Integer customerId) {
        validateCustomerId(customerId);

        Cart cart = cartRepo.findByCustomerUserId(customerId).orElse(null);

        if (cart == null) {
            return new ArrayList<>();
        }

        return cartItemRepo.findByCartId(cart.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CartItemResponse mapToResponse(CartItem item) {
        CartItemResponse res = new CartItemResponse();

        res.setCartItemId(item.getId());
        res.setQuantity(item.getQuantity());
        res.setNote(item.getNote());
        res.setThumbnailUrl(item.getThumbnailUrl());
        res.setImageUrl(item.getThumbnailUrl());

        ProductVariant variant = item.getProductVariant();

        if (variant == null) {
            res.setAvailable(false);
            res.setSellable(false);
            res.setExpired(false);
            res.setHasPromotion(false);
            res.setUnavailableReason("Sản phẩm không tồn tại");
            return res;
        }

        res.setProductVariantId(variant.getId());
        res.setSku(variant.getSku());
        res.setStockQuantity(variant.getStockQuantity());
        res.setManufacturingDate(variant.getManufacturingDate());
        res.setExpirationDate(variant.getExpirationDate());
        res.setVariantStatus(variant.getStatus());
        res.setExpired(isVariantExpired(variant));

        applyCurrentPrice(res, variant);

        if (variant.getProduct() != null) {
            res.setProductName(variant.getProduct().getName());
        }

        if (variant.getCapacity() != null && variant.getCapacity().getValue() != null) {
            res.setCapacity(formatCapacity(variant.getCapacity().getValue()));
        }

        if (variant.getBottleType() != null) {
            res.setBottleType(variant.getBottleType().getName());
        }

        String unavailableReason = getUnavailableReason(variant, item.getQuantity());

        boolean sellable = unavailableReason == null;

        res.setAvailable(sellable);
        res.setSellable(sellable);
        res.setUnavailableReason(unavailableReason);

        return res;
    }

    /**
     * Set giá cho cart.
     *
     * Có Flash Sale active:
     * - originalPrice = ProductVariant.Price
     * - salePrice = giá sau giảm
     * - price = salePrice
     *
     * Không có Flash Sale:
     * - originalPrice = ProductVariant.Price
     * - price = originalPrice
     */
    private void applyCurrentPrice(CartItemResponse res, ProductVariant variant) {
        BigDecimal originalPrice = variant.getPrice() == null
                ? BigDecimal.ZERO
                : variant.getPrice();

        res.setOriginalPrice(originalPrice);

        PromotionVariant activePromotion = findActivePromotion(variant);

        if (activePromotion == null) {
            res.setPrice(originalPrice);
            res.setSalePrice(null);
            res.setDiscountPercent(null);
            res.setHasPromotion(false);
            res.setPromotionId(null);
            res.setPromotionName(null);
            res.setPromotionEndDate(null);
            return;
        }

        Number discountNumber = activePromotion.getDiscountPercent();
        Double discountPercent = discountNumber == null
                ? null
                : discountNumber.doubleValue();

        BigDecimal salePrice = calculateSalePrice(originalPrice, discountNumber);

        res.setPrice(salePrice);
        res.setSalePrice(salePrice);
        res.setDiscountPercent(discountPercent);
        res.setHasPromotion(true);

        if (activePromotion.getPromotion() != null) {
            res.setPromotionId(activePromotion.getPromotion().getId());
            res.setPromotionName(activePromotion.getPromotion().getName());
            res.setPromotionEndDate(activePromotion.getPromotion().getEndDate());
        }
    }

    private PromotionVariant findActivePromotion(ProductVariant variant) {
        if (variant == null || variant.getId() == null) {
            return null;
        }

        List<PromotionVariant> activePromotions =
                promotionVariantRepository.findActivePromotionByProductVariantId(
                        variant.getId(),
                        LocalDateTime.now(),
                        LocalDate.now()
                );

        if (activePromotions == null || activePromotions.isEmpty()) {
            return null;
        }

        return activePromotions.get(0);
    }

    private BigDecimal calculateSalePrice(BigDecimal originalPrice, Number discountPercent) {
        if (originalPrice == null) {
            return BigDecimal.ZERO;
        }

        if (discountPercent == null) {
            return originalPrice;
        }

        BigDecimal discount = BigDecimal.valueOf(discountPercent.doubleValue());

        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return originalPrice;
        }

        BigDecimal discountAmount = originalPrice
                .multiply(discount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return originalPrice.subtract(discountAmount).max(BigDecimal.ZERO);
    }

    private String getUnavailableReason(ProductVariant variant, Integer quantity) {
        if (variant == null) {
            return "Sản phẩm không tồn tại";
        }

        String sku = variant.getSku() != null ? variant.getSku() : "N/A";

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            return "Sản phẩm [" + sku + "] đã bị xóa, không thể mua";
        }

        if (variant.getStatus() == null || variant.getStatus() != STATUS_ACTIVE) {
            return "Sản phẩm [" + sku + "] hiện không còn kinh doanh";
        }

        if (variant.getProduct() == null) {
            return "Sản phẩm [" + sku + "] không tồn tại";
        }

        if (variant.getProduct().getStatus() == null
                || variant.getProduct().getStatus() != STATUS_ACTIVE) {
            return "Sản phẩm [" + variant.getProduct().getName() + "] hiện không còn kinh doanh";
        }

        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "Giá sản phẩm [" + sku + "] không hợp lệ";
        }

        if (variant.getStockQuantity() == null || variant.getStockQuantity() <= 0) {
            return "Sản phẩm [" + sku + "] đã hết hàng";
        }

        if (quantity != null && quantity <= 0) {
            return "Số lượng sản phẩm [" + sku + "] không hợp lệ";
        }

        if (quantity != null && quantity > variant.getStockQuantity()) {
            return "Số lượng trong giỏ vượt quá tồn kho hiện tại. Sản phẩm ["
                    + sku
                    + "] chỉ còn "
                    + variant.getStockQuantity()
                    + " trong kho";
        }

        LocalDate today = LocalDate.now();

        if (variant.getManufacturingDate() == null) {
            return "Sản phẩm [" + sku + "] chưa có ngày sản xuất";
        }

        if (variant.getExpirationDate() == null) {
            return "Sản phẩm [" + sku + "] chưa có hạn sử dụng";
        }

        if (variant.getManufacturingDate().isAfter(today)) {
            return "Sản phẩm [" + sku + "] chưa tới ngày được bán";
        }

        if (variant.getExpirationDate().isBefore(today)) {
            return "Sản phẩm [" + sku + "] đã hết hạn sử dụng";
        }

        if (!variant.getExpirationDate().isAfter(variant.getManufacturingDate())) {
            return "Sản phẩm [" + sku + "] có hạn sử dụng không hợp lệ";
        }

        return null;
    }

    private void validateVariantCanAddToCart(ProductVariant variant, Integer quantity) {
        String reason = getUnavailableReason(variant, quantity);

        if (reason != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    reason
            );
        }
    }

    private boolean isVariantExpired(ProductVariant variant) {
        return variant != null
                && variant.getExpirationDate() != null
                && variant.getExpirationDate().isBefore(LocalDate.now());
    }

    private void validateCartItemOwner(CartItem item, Integer customerId) {
        if (item.getCart() == null
                || item.getCart().getCustomer() == null
                || item.getCart().getCustomer().getUserId() == null
                || !item.getCart().getCustomer().getUserId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Không có quyền thao tác trên giỏ hàng này"
            );
        }
    }

    private void validateCustomerId(Integer customerId) {
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản khách hàng không hợp lệ"
            );
        }
    }

    private void validateVariantId(Integer variantId) {
        if (variantId == null || variantId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Biến thể sản phẩm không hợp lệ"
            );
        }
    }

    private void validateCartItemId(Integer cartItemId) {
        if (cartItemId == null || cartItemId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã sản phẩm trong giỏ không hợp lệ"
            );
        }
    }

    private void validateAddQuantity(Integer quantity) {
        if (quantity == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng không được để trống"
            );
        }

        if (quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng phải lớn hơn 0"
            );
        }
    }

    private String normalizeOptionalText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " tối đa " + maxLength + " ký tự"
            );
        }

        return trimmed;
    }

    private String formatCapacity(Double value) {
        if (value == null) {
            return "-";
        }

        if (value % 1 == 0) {
            return value.intValue() + "ml";
        }

        return value + "ml";
    }
}