package org.example.datn_sd69.modules.auth.service;

import org.springframework.transaction.annotation.Transactional;
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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được sử dụng!");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng!");
        }

        Role customerRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Chưa cấu hình quyền Khách hàng!"));

        User newUser = new User();
        newUser.setName(request.getName().trim());
        newUser.setEmail(request.getEmail().trim());
        newUser.setPhone(request.getPhone().trim());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(customerRole);
        newUser.setStatus(1);
        newUser.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);

        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setCustomerRank("Bronze");
        customer.setLoyaltyPoints(0);

        customerRepository.save(customer);

        return "Đăng ký tài khoản thành công!";
    }

    // LUỒNG 1: ĐĂNG NHẬP DÀNH CHO KHÁCH HÀNG
    public AuthResponse loginCustomer(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail().trim())
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

            // Đã xóa check status thủ công vì CustomUserDetailsService đã lo việc đó.

            // Nghiệp vụ Backend: Chặn nếu lấy tài khoản Admin đăng nhập vào cổng Khách hàng
            if (!user.getRole().getName().equalsIgnoreCase("USER")) {
                throw new RuntimeException("Tài khoản này không thể đăng nhập ở cổng Khách hàng!");
            }

            String token = jwtUtils.generateToken(user.getEmail(), user.getRole().getName());
            return new AuthResponse(token, user.getRole().getName(), user.getName());

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Sai email hoặc mật khẩu!");
        } catch (DisabledException e) {
            // Spring tự động ném ra lỗi này nếu enabled = false (tức là status != 1)
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }
    }

    // LUỒNG 2: ĐĂNG NHẬP DÀNH CHO NHÂN VIÊN
    public AuthResponse loginEmployee(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail().trim())
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

            // Đã xóa check status thủ công vì CustomUserDetailsService đã lo việc đó.

            // Nghiệp vụ Backend: Chặn Khách hàng mò vào link đăng nhập ẩn của Nhân viên
            if (user.getRole().getName().equalsIgnoreCase("USER")) {
                throw new RuntimeException("Cảnh báo: Bạn không có quyền truy cập hệ thống quản trị!");
            }

            String token = jwtUtils.generateToken(user.getEmail(), user.getRole().getName());
            return new AuthResponse(token, user.getRole().getName(), user.getName());

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Sai email hoặc mật khẩu!");
        } catch (DisabledException e) {
            // Spring tự động ném ra lỗi này nếu enabled = false (tức là status != 1)
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }
    }
}