package org.example.datn_sd69.modules.voucher.service;

import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.dto.response.VoucherApplyResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    // Khai báo hàm tạo mới Voucher
    void createVoucher(VoucherRequest request);

    org.springframework.data.domain.Page<Voucher> getVouchers(String keyword, Integer status, int page, int size);

    // Dùng cho Customer lấy danh sách xổ xuống
    List<org.example.datn_sd69.entity.Voucher> getAllVouchers();
    VoucherApplyResponse applyVoucher(String code, BigDecimal orderTotal);
    Voucher getVoucherById(Integer id);
    void updateVoucher(Integer id, VoucherRequest request);
    void deleteVoucher(Integer id);

    // Hàm chạy ngầm tự động khóa voucher
    void autoDeactivateExpiredVouchers();
}