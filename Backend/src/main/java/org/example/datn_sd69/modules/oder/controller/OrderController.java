package org.example.datn_sd69.modules.oder.controller;

import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.oder.dto.request.OrderRequest;
import org.example.datn_sd69.modules.oder.dto.service.OrderService;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserRepository userRepo;

    private Integer getCustomerId(Principal principal) {
        String email = principal.getName(); // Lấy email từ token
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return user.getId();
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkout(Principal principal, @RequestBody OrderRequest request) {
        // Lấy customerId từ principal (giống cách làm CartController)
        Integer customerId = getCustomerId(principal);
        return ResponseEntity.ok(orderService.placeOrder(customerId, request));
    }
}