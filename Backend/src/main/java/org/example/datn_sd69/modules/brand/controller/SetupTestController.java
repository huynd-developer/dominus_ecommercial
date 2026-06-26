package org.example.datn_sd69.modules.brand.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Role;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.repository.RoleRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class SetupTestController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/api/setup-test-data")
    public String setupData() {
        // 1. Tạo 3 Role chuẩn IN HOA
        Role ownerRole = createRoleIfNotFound("OWNER", "Chủ cửa hàng");
        Role managerRole = createRoleIfNotFound("MANAGER", "Quản lý");
        Role cashierRole = createRoleIfNotFound("CASHIER", "Thu ngân");

        // 2. Tạo 3 Tài khoản, cấp thêm số điện thoại giả không trùng nhau để không bị lỗi UNIQUE
        createUserIfNotFound("owner@shop.com", "Ngô Boss", "0999000111", ownerRole);
        createUserIfNotFound("manager@shop.com", "Nguyễn Quản Lý", "0999000222", managerRole);
        createUserIfNotFound("cashier@shop.com", "Trần Thu Ngân", "0999000333", cashierRole);

        return "🎉 Khởi tạo dữ liệu Test thành công! Bạn có thể dùng các email (owner@shop.com, manager@shop.com, cashier@shop.com) với mật khẩu '123456' để đăng nhập.";
    }

    // Hàm hỗ trợ tạo Role
    private Role createRoleIfNotFound(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void createUserIfNotFound(String email, String name, String phone, Role role) {
        // Tìm user theo email, nếu có thì lấy ra, chưa có thì tạo mới
        User user = userRepository.findByEmail(email).orElse(new User());

        user.setEmail(email);
        user.setName(name);
        user.setPhone(phone);
        // ÉP BUỘC GHI ĐỀ MẬT KHẨU MỚI BẰNG BCRYPT BẤT KỂ CŨ HAY MỚI
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus(1);

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        userRepository.save(user);
    }

}