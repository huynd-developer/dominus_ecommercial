package org.example.datn_sd69.modules.voucher.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.dto.response.VoucherApplyResponse;
import org.example.datn_sd69.modules.voucher.service.VoucherService;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;


    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findByIsDeletedFalseOrderByIdDesc();
    }

    @Override
    public void createVoucher(VoucherRequest request) {
        // Validate Ngày bắt đầu - Ngày kết thúc
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc!");
        }

        // Validate Trùng mã
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã Voucher này đã tồn tại!");
        }

        Voucher voucher = new Voucher();
        voucher.setCode(request.getCode());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : java.math.BigDecimal.ZERO);
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        voucherRepository.save(voucher);
    }

    @Override
    public VoucherApplyResponse applyVoucher(String code, java.math.BigDecimal orderTotal) {
        // 1. Gọi hàm Query của nhóm m để check Voucher còn sống không (Status=1, trong hạn, còn lượt)
        Voucher voucher = voucherRepository.findValidByCode(code, java.time.LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại, đã hết hạn hoặc hết lượt sử dụng!"));

        // 2. Check điều kiện hóa đơn tối thiểu
        if (orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderValue() + "đ để áp dụng mã này!");
        }

        // 3. Tính toán số tiền được giảm
        java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;

        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            // Nếu giảm theo %
            java.math.BigDecimal percent = voucher.getDiscountValue().divide(java.math.BigDecimal.valueOf(100));
            discountAmount = orderTotal.multiply(percent);
        } else {
            // Nếu giảm số tiền cố định
            discountAmount = voucher.getDiscountValue();
        }

        // 4. Giới hạn mức giảm tối đa (nếu có)
        if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                discountAmount = voucher.getMaxDiscount();
            }
        }

        // Trả về số tiền được giảm để Frontend trừ đi
        return new VoucherApplyResponse(voucher.getCode(), discountAmount, "Áp dụng mã thành công!");
    }
}