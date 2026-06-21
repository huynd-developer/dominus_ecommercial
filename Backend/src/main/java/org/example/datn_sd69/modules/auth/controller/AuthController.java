package org.example.datn_sd69.modules.auth.controller;

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
    public ResponseEntity<?> register(@RequestBody RegisterCustomerRequest request) {
        try {
            return ResponseEntity.ok(authService.registerCustomer(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login/customer")
    public ResponseEntity<?> loginCustomer(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.loginCustomer(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/login/employee")
    public ResponseEntity<?> loginEmployee(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.loginEmployee(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
