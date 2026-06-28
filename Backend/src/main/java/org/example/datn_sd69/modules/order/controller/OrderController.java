package org.example.datn_sd69.modules.order.controller;

import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.modules.order.service.OrderService;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserRepository userRepo;

    private Integer getCustomerId(Principal principal) {
        String email = principal.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return user.getId();
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkout(Principal principal, @RequestBody OrderRequest request) {
        try {
            Integer customerId = getCustomerId(principal);
            // Hứng cái Map từ Service và trả về dưới dạng JSON
            Map<String, Object> result = orderService.placeOrder(customerId, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            // Trả về lỗi 400 nếu như bị bắt lỗi (Vd: Hết hàng, Giỏ trống...)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}