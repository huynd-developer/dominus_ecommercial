package org.example.datn_sd69.modules.cart.controller;

import jakarta.validation.Valid;
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
        return ResponseEntity.ok(cartService.getCartByCustomerId(customerId));
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

        return ResponseEntity.ok("Thêm sản phẩm vào giỏ hàng thành công");
    }

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

        return ResponseEntity.ok("Cập nhật giỏ hàng thành công");
    }

    @DeleteMapping("/remove/{cartItemId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> removeItem(
            Principal principal,
            @PathVariable Integer cartItemId
    ) {
        Integer customerId = getCustomerId(principal);

        cartService.removeCartItem(customerId, cartItemId);

        return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ");
    }

    private Integer getCustomerId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        String email = principal.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));

        return user.getId();
    }
}