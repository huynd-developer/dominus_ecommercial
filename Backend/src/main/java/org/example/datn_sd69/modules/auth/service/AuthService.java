package org.example.datn_sd69.modules.auth.service;


import org.example.datn_sd69.common.config.JwtUtils;
import org.example.datn_sd69.entity.KhachHang;
import org.example.datn_sd69.entity.NhanVien;
import org.example.datn_sd69.modules.auth.dto.LoginRequest;
import org.example.datn_sd69.modules.auth.dto.RegisterCustomerRequest;
import org.example.datn_sd69.repository.KhachHangRepository;
import org.example.datn_sd69.repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    // 1. API Đăng Ký Customer
    public String registerCustomer(RegisterCustomerRequest req) {
        if (khachHangRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        KhachHang newCustomer = new KhachHang();
        newCustomer.setHoTen(req.getName());
        newCustomer.setEmail(req.getEmail());
        newCustomer.setSoDienThoai(req.getPhone());
        newCustomer.setMatKhauMaHoa(passwordEncoder.encode(req.getPassword())); // Mã hóa Password
        newCustomer.setTrangThai(1); // 1 = Active

        khachHangRepository.save(newCustomer);
        return "Đăng ký tài khoản thành công!";
    }

    // 2. API Đăng Nhập Customer
    public String loginCustomer(LoginRequest req) {
        // IN RA ĐỂ KIỂM TRA CHÍNH XÁC CHUỖI NHẬN ĐƯỢC:
        System.out.println("=== EMAIL NHẬN ĐƯỢC: [" + req.getEmail() + "] ===");

        KhachHang kh = khachHangRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (kh.getTrangThai() == 0) throw new RuntimeException("Tài khoản đã bị khóa!");

        if (passwordEncoder.matches(req.getPassword(), kh.getMatKhauMaHoa().trim())) {
            return jwtUtils.generateCustomerToken(kh.getEmail());
        }
        throw new RuntimeException("Sai mật khẩu!");
    }

    // 3. API Đăng Nhập Employee
    public String loginEmployee(LoginRequest req) {
        // --- IN LOG ĐỂ KIỂM TRA DỮ LIỆU ĐẦU VÀO ---
        System.out.println("\n================= DEBUG LOGIN EMPLOYEE =================");
        System.out.println("-> [BE] Email nhan tu Postman: [" + req.getEmail() + "]");
        System.out.println("-> [BE] Password nhan tu Postman: [" + req.getPassword() + "]");

        NhanVien nv = nhanVienRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản nhân viên không tồn tại!"));

        // --- IN LOG KIỂM TRA DỮ LIỆU LẤY TỪ DB ---
        System.out.println("-> [DB] Ho Ten tim thay: " + nv.getHoTen());
        System.out.println("-> [DB] Mat khau ma hoa lay tu DB: [" + nv.getMatKhauMaHoa() + "]");
        if (nv.getMatKhauMaHoa() != null) {
            System.out.println("-> [DB] Do dai chuoi mat khau DB: " + nv.getMatKhauMaHoa().length());
        }
        System.out.println("========================================================\n");
        System.out.println("👉 CHUỖI MÃ HÓA CHUẨN XỊN CỦA BE: " + passwordEncoder.encode("123"));
        if (nv.getTrangThai() == 0) throw new RuntimeException("Tài khoản nhân viên bị khóa!");

        // Trim chuỗi để tránh khoảng trống thừa do kiểu dữ liệu CHAR/NCHAR trong SQL gây ra
        if (passwordEncoder.matches(req.getPassword(), nv.getMatKhauMaHoa().trim())) {
            return jwtUtils.generateEmployeeToken(nv.getEmail(), nv.getVaiTro().getId());
        }
        throw new RuntimeException("Sai mật khẩu!");
    }
}
