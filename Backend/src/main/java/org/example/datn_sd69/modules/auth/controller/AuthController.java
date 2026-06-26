package org.example.datn_sd69.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.auth.dto.request.LoginRequest;
import org.example.datn_sd69.modules.auth.dto.request.RegisterRequest;
import org.example.datn_sd69.modules.auth.dto.request.response.AuthResponse;
import org.example.datn_sd69.modules.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String result = authService.registerCustomer(request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login/customer")
    public ResponseEntity<?> loginCustomer(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.loginCustomer(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Đóng gói thành JSON {"message": "Lỗi gì đó..."}
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Cổng 2: Cho link ẩn bí mật của quản trị viên
    @PostMapping("/login/employee")
    public ResponseEntity<?> loginEmployee(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.loginEmployee(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Đóng gói thành JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}