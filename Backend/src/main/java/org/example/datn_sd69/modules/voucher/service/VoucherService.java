package org.example.datn_sd69.modules.voucher.service;

import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.dto.response.VoucherApplyResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    // Khai báo hàm tạo mới Voucher
    void createVoucher(VoucherRequest request);

    List<Voucher> getAllVouchers();

    VoucherApplyResponse applyVoucher(String code, BigDecimal orderTotal);
}