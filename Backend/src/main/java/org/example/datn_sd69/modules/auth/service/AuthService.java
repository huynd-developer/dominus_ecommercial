package org.example.datn_sd69.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.common.config.JwtUtils;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Role;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.auth.dto.request.LoginRequest;
import org.example.datn_sd69.modules.auth.dto.request.RegisterRequest;
import org.example.datn_sd69.modules.auth.dto.response.AuthResponse;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.RoleRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional(rollbackFor = Exception.class)
    public String registerCustomer(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalize(request.getPhone());

        if (userRepository.existsEmail(email, null)) {
            throw new IllegalArgumentException("Email này đã được sử dụng!");
        }

        if (userRepository.existsPhone(phone, null)) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng!");
        }

        Role customerRole = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new RuntimeException(
                        "Lỗi hệ thống: Chưa cấu hình quyền Khách hàng!"
                ));

        User newUser = new User();
        newUser.setName(request.getName().trim());
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        newUser.setRole(customerRole);
        newUser.setStatus(1);
        newUser.setIsDeleted(false);
        newUser.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);

        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setCustomerRank("Bronze");
        customer.setLoyaltyPoints(0);

        customerRepository.save(customer);

        return "Đăng ký tài khoản thành công!";
    }

    public AuthResponse loginCustomer(LoginRequest request) {
        try {
            String email = normalizeEmail(request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            User user = userRepository.findWithRoleByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

            validateActiveAccount(user);

            if (!user.getRole().getName().equalsIgnoreCase("USER")) {
                throw new RuntimeException("Tài khoản này không thể đăng nhập ở cổng Khách hàng!");
            }

            String roleName = user.getRole().getName().trim().toUpperCase();
            String token = jwtUtils.generateToken(user.getEmail(), roleName);

            return new AuthResponse(token, roleName, user.getName());

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Sai email hoặc mật khẩu!");
        } catch (DisabledException e) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa hoặc đã bị xóa!");
        }
    }

    public AuthResponse loginEmployee(LoginRequest request) {
        try {
            String email = normalizeEmail(request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            User user = userRepository.findWithRoleByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

            validateActiveAccount(user);

            if (user.getRole().getName().equalsIgnoreCase("USER")) {
                throw new RuntimeException("Cảnh báo: Bạn không có quyền truy cập hệ thống quản trị!");
            }

            String roleName = user.getRole().getName().trim().toUpperCase();
            String token = jwtUtils.generateToken(user.getEmail(), roleName);

            return new AuthResponse(token, roleName, user.getName());

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Sai email hoặc mật khẩu!");
        } catch (DisabledException e) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa hoặc đã bị xóa!");
        }
    }

    private void validateActiveAccount(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new RuntimeException("Tài khoản không tồn tại hoặc đã bị xóa!");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}