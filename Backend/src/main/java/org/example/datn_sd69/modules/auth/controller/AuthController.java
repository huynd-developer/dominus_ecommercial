package org.example.datn_sd69.modules.auth.controller;

import jakarta.validation.Valid;
import org.example.datn_sd69.modules.auth.dto.LoginRequest;
import org.example.datn_sd69.modules.auth.dto.RegisterCustomerRequest;
import org.example.datn_sd69.modules.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterCustomerRequest request) {
        // Nếu có lỗi, Spring tự ném ra và GlobalExceptionHandler sẽ bắt
        return ResponseEntity.ok(authService.registerCustomer(request));
    }

    @PostMapping("/login/customer")
    public ResponseEntity<?> loginCustomer(@Valid @RequestBody LoginRequest request) {
        // Tốt nhất nên trả về định dạng JSON kiểu {"accessToken": "chuỗi_token"}
        // Giả sử authService trả về String token, ta bọc nó vào Map để thành JSON
        Object result = authService.loginCustomer(request);
        if (result instanceof String) {
            return ResponseEntity.ok(Map.of("accessToken", result));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login/employee")
    public ResponseEntity<?> loginEmployee(@Valid @RequestBody LoginRequest request) {
        Object result = authService.loginEmployee(request);
        if (result instanceof String) {
            return ResponseEntity.ok(Map.of("accessToken", result));
        }
        return ResponseEntity.ok(result);
    }
}