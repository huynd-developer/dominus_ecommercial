package org.example.datn_sd69.modules.auth.service;

import org.example.datn_sd69.common.config.JwtUtils;
import org.example.datn_sd69.entity.KhachHang;
import org.example.datn_sd69.entity.NhanVien;
import org.example.datn_sd69.entity.VaiTro;
import org.example.datn_sd69.modules.auth.dto.LoginRequest;
import org.example.datn_sd69.modules.auth.dto.RegisterCustomerRequest;
import org.example.datn_sd69.repository.KhachHangRepository;
import org.example.datn_sd69.repository.NhanVienRepository;
import org.example.datn_sd69.repository.VaiTroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private VaiTroRepository vaiTroRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    // 1. API Đăng Ký Customer
    public String registerCustomer(RegisterCustomerRequest req) {
        String cleanEmail = req.getEmail().trim();
        String cleanPhone = req.getPhone().trim();
        String cleanName = req.getName().trim();

        // 👉 ĐÃ SỬA: Ném IllegalArgumentException thay vì RuntimeException để GlobalExceptionHandler bắt thành lỗi 400
        if (khachHangRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        if (khachHangRepository.existsBySoDienThoai(cleanPhone)) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký bởi tài khoản khác!");
        }

        VaiTro userRole = vaiTroRepository.findByTenVaiTro("user")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy role 'user'!"));

        KhachHang newCustomer = new KhachHang();
        newCustomer.setHoTen(cleanName);
        newCustomer.setEmail(cleanEmail);
        newCustomer.setSoDienThoai(cleanPhone);
        newCustomer.setMatKhauMaHoa(passwordEncoder.encode(req.getPassword()));
        newCustomer.setTrangThai(1);
        newCustomer.setVaiTro(userRole);

        khachHangRepository.save(newCustomer);
        return "Đăng ký tài khoản thành công!";
    }

    // 2. API Đăng Nhập Customer
    public String loginCustomer(LoginRequest req) {
        // 👉 ĐÃ SỬA: Tránh tiết lộ tài khoản có tồn tại hay không cho hacker
        KhachHang kh = khachHangRepository.findByEmail(req.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác!"));

        if (kh.getTrangThai() == 0) throw new LockedException("Tài khoản đã bị khóa!");

        if (passwordEncoder.matches(req.getPassword(), kh.getMatKhauMaHoa().trim())) {
            return jwtUtils.generateCustomerToken(kh.getEmail());
        }
        throw new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác!");
    }

    // 3. API Đăng Nhập Employee
    public String loginEmployee(LoginRequest req) {
        NhanVien nv = nhanVienRepository.findByEmail(req.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác!"));

        if (nv.getTrangThai() == 0) throw new LockedException("Tài khoản nhân viên bị khóa!");

        if (passwordEncoder.matches(req.getPassword(), nv.getMatKhauMaHoa().trim())) {
            return jwtUtils.generateEmployeeToken(nv.getEmail(), nv.getVaiTro().getTenVaiTro());
        }
        throw new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác!");
    }
}