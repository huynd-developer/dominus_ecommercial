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

    public String registerCustomer(RegisterCustomerRequest req) {
        if (khachHangRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        KhachHang newCustomer = new KhachHang();
        newCustomer.setHoTen(req.getName());
        newCustomer.setEmail(req.getEmail());
        newCustomer.setSoDienThoai(req.getPhone());
        newCustomer.setMatKhauMaHoa(passwordEncoder.encode(req.getPassword()));
        newCustomer.setTrangThai(1);

        khachHangRepository.save(newCustomer);
        return "Đăng ký tài khoản thành công!";
    }

    public String loginCustomer(LoginRequest req) {
        KhachHang kh = khachHangRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (kh.getTrangThai() == 0) throw new RuntimeException("Tài khoản đã bị khóa!");

        if (passwordEncoder.matches(req.getPassword(), kh.getMatKhauMaHoa())) {
            // ĐÃ SỬA: Truyền thêm kh.getId() vào làm tham số thứ hai
            return jwtUtils.generateCustomerToken(kh.getEmail(), kh.getId().longValue());
        }
        throw new RuntimeException("Sai mật khẩu!");
    }

    public String loginEmployee(LoginRequest req) {
        NhanVien nv = nhanVienRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản nhân viên không tồn tại!"));

        if (nv.getTrangThai() == 0) throw new RuntimeException("Tài khoản nhân viên bị khóa!");

        if (passwordEncoder.matches(req.getPassword(), nv.getMatKhauMaHoa())) {
            // ĐÃ SỬA: Truyền thêm nv.getId() vào làm tham số thứ hai
            return jwtUtils.generateEmployeeToken(nv.getEmail(), nv.getId().longValue(), nv.getVaiTro().getId());
        }
        throw new RuntimeException("Sai mật khẩu!");
    }
}