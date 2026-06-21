package org.example.datn_sd69.modules.auth.service;


import org.example.datn_sd69.entity.KhachHang;
import org.example.datn_sd69.modules.auth.dto.AccountUpdateRequest;
import org.example.datn_sd69.modules.auth.dto.CustomerProfileResponse;
import org.example.datn_sd69.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class KhachHangServices {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Dùng chung Bean với SecurityConfig

    @Transactional
    public CustomerProfileResponse updateProfileAndPass(AccountUpdateRequest request) {
        // 1. Lấy khách hàng ID = 1 (Phan Văn Khách VIP) theo data của bạn
        KhachHang khachHang = khachHangRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản khách hàng."));

        // 2. Kiểm tra trùng Email
        if (!khachHang.getEmail().equals(request.getEmail())) {
            khachHangRepository.findByEmail(request.getEmail()).ifPresent(k -> {
                throw new RuntimeException("Email này đã được sử dụng bởi tài khoản khác.");
            });
            khachHang.setEmail(request.getEmail());
        }

        // 3. Kiểm tra trùng Số điện thoại
        if (!khachHang.getSoDienThoai().equals(request.getSoDienThoai())) {
            khachHangRepository.findBySoDienThoai(request.getSoDienThoai()).ifPresent(k -> {
                throw new RuntimeException("Số điện thoại này đã được sử dụng.");
            });
            khachHang.setSoDienThoai(request.getSoDienThoai());
        }

        // 4. Cập nhật các thông tin cơ bản
        khachHang.setHoTen(request.getHoTen());
        khachHang.setDiaChiMacDinh(request.getDiaChiMacDinh());

        // 5. Xử lý đổi mật khẩu (Chỉ chạy khi người dùng chủ động điền mật khẩu mới)
        if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {

            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại để xác nhận đổi mật khẩu.");
            }

            // Hỗ trợ cả cơ chế so khớp chuỗi thô (dữ liệu mẫu) lẫn mật khẩu đã mã hóa BCrypt
            boolean isPasswordMatch = passwordEncoder.matches(request.getCurrentPassword(), khachHang.getMatKhauMaHoa())
                    || khachHang.getMatKhauMaHoa().equals(request.getCurrentPassword());

            if (!isPasswordMatch) {
                throw new RuntimeException("Mật khẩu hiện tại không chính xác.");
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không trùng khớp.");
            }

            // Mã hóa và lưu mật khẩu mới chuẩn BCrypt vào Database
            khachHang.setMatKhauMaHoa(passwordEncoder.encode(request.getNewPassword()));
        }

        // 6. Lưu xuống Database
        KhachHang savedKhachHang = khachHangRepository.save(khachHang);

        // 7. Trả về Response DTO sạch đẹp cho Front-end
        return CustomerProfileResponse.builder()
                .id(savedKhachHang.getId())
                .hoTen(savedKhachHang.getHoTen())
                .soDienThoai(savedKhachHang.getSoDienThoai())
                .email(savedKhachHang.getEmail())
                .diaChiMacDinh(savedKhachHang.getDiaChiMacDinh())
                .trangThai(savedKhachHang.getTrangThai())
                .membershipTier("VIP Gold Member")
                .amountToNextTier(new BigDecimal("0"))
                .progressPercentage(100.0)
                .build();
    }
}
