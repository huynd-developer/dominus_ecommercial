package org.example.datn_sd69.modules.favorite.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.favorite.dto.request.AddFavoriteRequest;
import org.example.datn_sd69.modules.favorite.dto.response.FavoriteResponse;
import org.example.datn_sd69.modules.favorite.dto.response.FavoriteToggleResponse;
import org.example.datn_sd69.modules.favorite.service.CustomerFavoriteService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.FavoriteRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerFavoriteServiceImpl implements CustomerFavoriteService {

    private static final int ACTIVE_STATUS = 1;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites() {
        Customer customer = getCurrentCustomer();

        return favoriteRepository.findByCustomer_UserIdOrderByCreatedAtDesc(customer.getUserId())
                .stream()
                .map(this::mapToFavoriteResponse)
                .toList();
    }

    @Override
    @Transactional
    public FavoriteResponse addFavorite(AddFavoriteRequest request) {
        Customer customer = getCurrentCustomer();

        Integer productVariantId = request.productVariantId();

        validateProductVariantId(productVariantId);

        ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy biến thể sản phẩm"
                ));

        validateProductVariantCanFavorite(productVariant);

        boolean existed = favoriteRepository.existsByCustomer_UserIdAndProductVariant_Id(
                customer.getUserId(),
                productVariantId
        );

        if (existed) {
            throw badRequest("Sản phẩm này đã có trong danh sách yêu thích");
        }

        Favorite favorite = new Favorite();
        favorite.setCustomer(customer);
        favorite.setProductVariant(productVariant);
        favorite.setCreatedAt(LocalDateTime.now());

        Favorite saved = favoriteRepository.save(favorite);

        return mapToFavoriteResponse(saved);
    }

    @Override
    @Transactional
    public FavoriteToggleResponse toggleFavorite(AddFavoriteRequest request) {
        Customer customer = getCurrentCustomer();

        Integer productVariantId = request.productVariantId();

        validateProductVariantId(productVariantId);

        ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy biến thể sản phẩm"
                ));

        validateProductVariantCanFavorite(productVariant);

        var existingFavorite = favoriteRepository.findByCustomer_UserIdAndProductVariant_Id(
                customer.getUserId(),
                productVariantId
        );

        if (existingFavorite.isPresent()) {
            favoriteRepository.delete(existingFavorite.get());

            return new FavoriteToggleResponse(
                    productVariantId,
                    false,
                    "Đã bỏ yêu thích sản phẩm"
            );
        }

        Favorite favorite = new Favorite();
        favorite.setCustomer(customer);
        favorite.setProductVariant(productVariant);
        favorite.setCreatedAt(LocalDateTime.now());

        favoriteRepository.save(favorite);

        return new FavoriteToggleResponse(
                productVariantId,
                true,
                "Đã thêm sản phẩm vào yêu thích"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkFavorite(Integer productVariantId) {
        Customer customer = getCurrentCustomer();

        validateProductVariantId(productVariantId);

        return favoriteRepository.existsByCustomer_UserIdAndProductVariant_Id(
                customer.getUserId(),
                productVariantId
        );
    }

    @Override
    @Transactional
    public void deleteFavorite(Integer favoriteId) {
        Customer customer = getCurrentCustomer();

        if (favoriteId == null || favoriteId <= 0) {
            throw badRequest("favoriteId không hợp lệ");
        }

        Favorite favorite = favoriteRepository.findByIdAndCustomer_UserId(
                        favoriteId,
                        customer.getUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm yêu thích"
                ));

        favoriteRepository.delete(favorite);
    }

    @Override
    @Transactional
    public void deleteFavoriteByVariant(Integer productVariantId) {
        Customer customer = getCurrentCustomer();

        validateProductVariantId(productVariantId);

        Favorite favorite = favoriteRepository.findByCustomer_UserIdAndProductVariant_Id(
                        customer.getUserId(),
                        productVariantId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sản phẩm chưa có trong danh sách yêu thích"
                ));

        favoriteRepository.delete(favorite);
    }

    private FavoriteResponse mapToFavoriteResponse(Favorite favorite) {
        ProductVariant variant = favorite.getProductVariant();

        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;
        Capacity capacity = variant != null ? variant.getCapacity() : null;
        BottleType bottleType = variant != null ? variant.getBottleType() : null;

        return new FavoriteResponse(
                favorite.getId(),
                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,
                variant != null ? variant.getPrice() : null,
                variant != null ? variant.getStockQuantity() : null,
                capacity != null && capacity.getValue() != null
                        ? capacity.getValue().doubleValue()
                        : null,
                bottleType != null ? bottleType.getName() : null,
                null,
                favorite.getCreatedAt()
        );
    }

    private void validateProductVariantCanFavorite(ProductVariant productVariant) {
        if (productVariant == null) {
            throw badRequest("Biến thể sản phẩm không tồn tại");
        }

        if (productVariant.getStatus() == null || productVariant.getStatus() != ACTIVE_STATUS) {
            throw badRequest("Biến thể sản phẩm đang ngừng bán, không thể yêu thích");
        }

        Product product = productVariant.getProduct();

        if (product == null) {
            throw badRequest("Sản phẩm không tồn tại");
        }

        if (product.getStatus() == null || product.getStatus() != ACTIVE_STATUS) {
            throw badRequest("Sản phẩm đang ngừng bán, không thể yêu thích");
        }
    }

    private void validateProductVariantId(Integer productVariantId) {
        if (productVariantId == null || productVariantId <= 0) {
            throw badRequest("productVariantId phải là số nguyên dương");
        }
    }

    private Customer getCurrentCustomer() {
        User user = getCurrentUser();

        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản hiện tại không phải khách hàng"
                ));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        String email = authentication.getName();

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ"
            );
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}