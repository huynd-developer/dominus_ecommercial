package org.example.datn_sd69.modules.auth.controller;

// NHỚ IMPORT CÁI NÀY VÀO
import jakarta.validation.Valid;
import org.example.datn_sd69.modules.auth.dto.LoginRequest;
import org.example.datn_sd69.modules.auth.dto.RegisterCustomerRequest;
import org.example.datn_sd69.modules.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    // Thêm @Valid vào đây
    public ResponseEntity<?> register(@Valid @RequestBody RegisterCustomerRequest request) {
        try {
            return ResponseEntity.ok(authService.registerCustomer(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login/customer")
    // Thêm @Valid vào đây
    public ResponseEntity<?> loginCustomer(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.loginCustomer(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/login/employee")
    // Thêm @Valid vào đây
    public ResponseEntity<?> loginEmployee(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.loginEmployee(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}