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
        String message = authService.registerCustomer(request);
        // Trả về chuẩn JSON thay vì plain text
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/login/customer")
    public ResponseEntity<?> loginCustomer(@Valid @RequestBody LoginRequest request) {
        // Nhận trực tiếp String và bọc vào JSON
        String token = authService.loginCustomer(request);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    @PostMapping("/login/employee")
    public ResponseEntity<?> loginEmployee(@Valid @RequestBody LoginRequest request) {
        String token = authService.loginEmployee(request);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}