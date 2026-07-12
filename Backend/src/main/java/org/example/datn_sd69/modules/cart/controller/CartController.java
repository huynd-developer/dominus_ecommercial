package org.example.datn_sd69.modules.cart.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.cart.dto.request.CartAddRequest;
import org.example.datn_sd69.modules.cart.dto.request.CartUpdateRequest;
import org.example.datn_sd69.modules.cart.service.CartService;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepo;

    @GetMapping("/my-cart")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> getMyCart(Principal principal) {
        Integer customerId = getCustomerId(principal);

        return ResponseEntity.ok(
                cartService.getCartByCustomerId(customerId)
        );
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> addToCart(
            Principal principal,
            @Valid @RequestBody CartAddRequest request
    ) {
        Integer customerId = getCustomerId(principal);

        cartService.addVariantToCart(
                customerId,
                request.getProductVariantId(),
                request.getQuantity(),
                request.getNote(),
                request.getThumbnailUrl()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Thêm sản phẩm vào giỏ hàng thành công"
        ));
    }

    /**
     * Endpoint cũ - giữ nguyên để không ảnh hưởng các màn hình khác.
     *
     * FE nào đang gọi:
     * PUT /api/v1/customer/cart/update/{cartItemId}
     * body: { "quantity": 2 }
     *
     * vẫn chạy bình thường.
     */
    @PutMapping("/update/{cartItemId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> updateQuantity(
            Principal principal,
            @PathVariable Integer cartItemId,
            @Valid @RequestBody CartUpdateRequest request
    ) {
        Integer customerId = getCustomerId(principal);

        cartService.updateCartItemQuantity(
                customerId,
                cartItemId,
                request.getQuantity()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật giỏ hàng thành công"
        ));
    }

    /**
     * Endpoint mới - thêm để khớp với CheckoutView hiện tại.
     *
     * FE checkout đang gọi:
     * PUT /api/v1/customer/cart/update
     * body:
     * {
     *   "cartItemId": 1,
     *   "productVariantId": 5,
     *   "quantity": 2
     * }
     *
     * productVariantId nhận vào để FE gửi không lỗi, nhưng BE không cần dùng.
     * BE update theo cartItemId và tự check chủ giỏ hàng trong service.
     */
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> updateQuantityFromBody(
            Principal principal,
            @Valid @RequestBody CartUpdateBodyRequest request
    ) {
        Integer customerId = getCustomerId(principal);

        cartService.updateCartItemQuantity(
                customerId,
                request.getCartItemId(),
                request.getQuantity()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật giỏ hàng thành công"
        ));
    }

    @DeleteMapping("/remove/{cartItemId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> removeItem(
            Principal principal,
            @PathVariable Integer cartItemId
    ) {
        Integer customerId = getCustomerId(principal);

        cartService.removeCartItem(customerId, cartItemId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã xóa sản phẩm khỏi giỏ"
        ));
    }

    private Integer getCustomerId(Principal principal) {
        if (principal == null
                || principal.getName() == null
                || principal.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        String email = principal.getName().trim();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));

        if (user.getId() == null || user.getId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản đăng nhập không hợp lệ"
            );
        }

        return user.getId();
    }

    @Data
    public static class CartUpdateBodyRequest {

        @NotNull(message = "Mã sản phẩm trong giỏ không được để trống")
        private Integer cartItemId;

        /**
         * Không bắt buộc dùng ở BE vì cartItemId đã đủ để xác định dòng giỏ hàng.
         * Giữ field này để FE gửi lên không bị lỗi mapping.
         */
        private Integer productVariantId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}