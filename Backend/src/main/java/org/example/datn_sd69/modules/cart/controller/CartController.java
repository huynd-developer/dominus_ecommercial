package org.example.datn_sd69.modules.cart.controller;

import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.cart.dto.request.CartAddRequest;
import org.example.datn_sd69.modules.cart.dto.request.CartUpdateRequest;
import org.example.datn_sd69.modules.cart.service.CartService;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
// Đã sửa đường dẫn để gom vào nhóm API của Customer
@RequestMapping("/api/v1/customer/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepo;

    // Hàm phụ để lấy ID khách hàng từ Principal
    private Integer getCustomerId(Principal principal) {
        String email = principal.getName(); // Lấy email từ token
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return user.getId();
    }

    // 1. Lấy giỏ hàng của user đang đăng nhập
    @GetMapping("/my-cart")
    @PreAuthorize("isAuthenticated()") // Tạm hạ quyền xuống để test
    public ResponseEntity<?> getMyCart(Principal principal) {
        Integer customerId = getCustomerId(principal);
        return ResponseEntity.ok(cartService.getCartByCustomerId(customerId));
    }

    // 2. Thêm biến thể vào giỏ
    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addToCart(Principal principal, @RequestBody CartAddRequest request) {
        Integer customerId = getCustomerId(principal);
        cartService.addVariantToCart(customerId, request.getProductVariantId(), request.getQuantity());
        return ResponseEntity.ok("Thêm sản phẩm vào giỏ hàng thành công");
    }

    // 3. Cập nhật số lượng
    @PutMapping("/update/{cartItemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateQuantity(Principal principal, @PathVariable Integer cartItemId, @RequestBody CartUpdateRequest request) {
        Integer customerId = getCustomerId(principal);
        cartService.updateCartItemQuantity(customerId, cartItemId, request.getQuantity());
        return ResponseEntity.ok("Cập nhật số lượng thành công");
    }

    // 4. Xóa item khỏi giỏ
    @DeleteMapping("/remove/{cartItemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeItem(Principal principal, @PathVariable Integer cartItemId) {
        Integer customerId = getCustomerId(principal);
        cartService.removeCartItem(customerId, cartItemId);
        return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ");
    }
}