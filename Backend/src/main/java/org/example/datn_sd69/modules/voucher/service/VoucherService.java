package org.example.datn_sd69.modules.voucher.service;

import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.dto.response.VoucherApplyResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    void createVoucher(VoucherRequest request);

    org.springframework.data.domain.Page<Voucher> getVouchers(
            String keyword,
            Integer status,
            int page,
            int size
    );

    List<Voucher> getAllVouchers();

    VoucherApplyResponse applyVoucher(String code, BigDecimal orderTotal);

    Voucher getVoucherById(Integer id);

    void updateVoucher(Integer id, VoucherRequest request);

    /**
     * Giữ method cũ để không làm vỡ caller khác nếu đang dùng.
     */
    void deleteVoucher(Integer id);

    /**
     * Delete stale-safe dành cho Admin FE mới.
     */
    void deleteVoucher(Integer id, String expectedRevision);

    /**
     * Revision chỉ phản ánh dữ liệu Admin có thể chỉnh sửa.
     * Không chứa usedCount để việc khách sử dụng voucher không làm form Admin stale giả.
     */
    String getRevision(Voucher voucher);

    /**
     * Giữ contract cũ nhưng method này KHÔNG còn được @Scheduled tại service.
     * Scheduler hệ thống là nơi duy nhất tự động kết thúc voucher.
     */
    void autoDeactivateExpiredVouchers();
}
