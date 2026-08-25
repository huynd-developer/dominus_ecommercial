package org.example.datn_sd69.modules.voucher.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.dto.response.VoucherApplyResponse;
import org.example.datn_sd69.modules.voucher.service.VoucherService;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Voucher> getVouchers(
            String keyword,
            Integer status,
            int page,
            int size
    ) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        return voucherRepository.searchVouchers(
                normalizeNullableKeyword(keyword),
                status,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findByIsDeletedFalseOrderByIdDesc();
    }

    @Override
    @Transactional
    public void createVoucher(VoucherRequest request) {
        validateVoucherRequest(request, true);

        String code = normalizeCode(request.getCode());

        if (code.isEmpty()) {
            code = generateUniqueCode();
        } else if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã Voucher này đã tồn tại!");
        }

        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setDiscountType(normalizeDiscountType(request.getDiscountType()));
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(defaultZero(request.getMinOrderValue()));
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setUsedCount(0);
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        voucher.setIsDeleted(false);

        voucherRepository.save(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherApplyResponse applyVoucher(String code, BigDecimal orderTotal) {
        String cleanCode = normalizeCode(code);

        if (cleanCode.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã giảm giá!");
        }

        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị đơn hàng phải lớn hơn 0!");
        }

        Voucher voucher = voucherRepository
                .findValidByCode(cleanCode, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã giảm giá không tồn tại, đã hết hạn hoặc hết lượt sử dụng!"
                ));

        BigDecimal minOrderValue = defaultZero(voucher.getMinOrderValue());

        if (orderTotal.compareTo(minOrderValue) < 0) {
            throw new IllegalArgumentException(
                    "Đơn hàng chưa đạt giá trị tối thiểu "
                            + minOrderValue
                            + "đ để áp dụng mã này!"
            );
        }

        BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);

        return new VoucherApplyResponse(
                voucher.getCode(),
                discountAmount,
                "Áp dụng mã thành công!"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher getVoucherById(Integer id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại!"));
    }

    @Override
    @Transactional
    public void updateVoucher(Integer id, VoucherRequest request) {
        validateVoucherRequest(request, false);

        Voucher voucher = voucherRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại!"));

        ensureNotDeleted(voucher);
        validateExpectedRevision(voucher, request.getExpectedRevision());

        String newCode = normalizeCode(request.getCode());

        if (newCode.isEmpty()) {
            throw new IllegalArgumentException("Mã Voucher không được để trống khi cập nhật!");
        }

        if (voucherRepository.existsByCodeIgnoreCaseAndIdNot(newCode, id)) {
            throw new IllegalArgumentException("Mã Voucher này đã tồn tại!");
        }

        voucher.setCode(newCode);
        voucher.setDiscountType(normalizeDiscountType(request.getDiscountType()));
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(defaultZero(request.getMinOrderValue()));
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(Integer id) {
        deleteVoucher(id, null);
    }

    @Override
    @Transactional
    public void deleteVoucher(Integer id, String expectedRevision) {
        Voucher voucher = voucherRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại!"));

        if (Boolean.TRUE.equals(voucher.getIsDeleted())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Voucher đã bị xóa trước đó. Vui lòng tải lại danh sách."
            );
        }

        validateExpectedRevision(voucher, expectedRevision);

        voucher.setIsDeleted(true);
        voucherRepository.save(voucher);
    }

    @Override
    public String getRevision(Voucher voucher) {
        if (voucher == null) {
            return null;
        }

        String source = String.join("|",
                valueOf(voucher.getId()),
                valueOf(voucher.getCode()),
                valueOf(voucher.getDiscountType()),
                decimalOf(voucher.getDiscountValue()),
                decimalOf(voucher.getMinOrderValue()),
                decimalOf(voucher.getMaxDiscount()),
                valueOf(voucher.getUsageLimit()),
                timeOf(voucher.getStartDate()),
                timeOf(voucher.getEndDate()),
                valueOf(voucher.getStatus()),
                valueOf(voucher.getIsDeleted())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không thể tạo revision Voucher.", e);
        }
    }

    /**
     * Giữ method để tương thích service cũ, nhưng cố ý KHÔNG có @Scheduled.
     * SystemJobScheduler là nơi duy nhất chạy tự động kết thúc voucher.
     */
    @Override
    @Transactional
    public void autoDeactivateExpiredVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> expiredVouchers = voucherRepository.findToEnd(1, now);

        if (expiredVouchers.isEmpty()) {
            return;
        }

        for (Voucher voucher : expiredVouchers) {
            voucher.setStatus(0);
        }

        voucherRepository.saveAll(expiredVouchers);
    }

    private void validateVoucherRequest(VoucherRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu Voucher không được để trống!");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống!");
        }

        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu!");
        }

        if (request.getDiscountValue() == null
                || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức giảm phải lớn hơn 0!");
        }

        String discountType = normalizeDiscountType(request.getDiscountType());

        if (isPercentType(discountType)
                && request.getDiscountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Mức giảm phần trăm không được vượt quá 100%!");
        }

        if (request.getUsageLimit() == null || request.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Giới hạn lượt dùng phải lớn hơn 0!");
        }

        if (request.getStatus() != null
                && request.getStatus() != 0
                && request.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái Voucher không hợp lệ!");
        }

        if (!creating && normalizeCode(request.getCode()).isEmpty()) {
            throw new IllegalArgumentException("Mã Voucher không được để trống khi cập nhật!");
        }
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal orderTotal) {
        String discountType = normalizeDiscountType(voucher.getDiscountType());
        BigDecimal discountValue = defaultZero(voucher.getDiscountValue());
        BigDecimal discountAmount;

        if (isPercentType(discountType)) {
            discountAmount = orderTotal
                    .multiply(discountValue)
                    .divide(ONE_HUNDRED);
        } else if (isFixedType(discountType)) {
            discountAmount = discountValue;
        } else {
            throw new IllegalArgumentException("Kiểu giảm giá không hợp lệ!");
        }

        BigDecimal maxDiscount = voucher.getMaxDiscount();

        if (maxDiscount != null
                && maxDiscount.compareTo(BigDecimal.ZERO) > 0
                && discountAmount.compareTo(maxDiscount) > 0) {
            discountAmount = maxDiscount;
        }

        if (discountAmount.compareTo(orderTotal) > 0) {
            discountAmount = orderTotal;
        }

        return discountAmount.max(BigDecimal.ZERO);
    }

    private void validateExpectedRevision(Voucher voucher, String expectedRevision) {
        if (expectedRevision == null || expectedRevision.trim().isEmpty()) {
            return;
        }

        String currentRevision = getRevision(voucher);

        if (!currentRevision.equals(expectedRevision.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Voucher đã được thay đổi bởi thao tác khác. Vui lòng tải lại dữ liệu mới nhất trước khi lưu."
            );
        }
    }

    private void ensureNotDeleted(Voucher voucher) {
        if (Boolean.TRUE.equals(voucher.getIsDeleted())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Voucher đã bị xóa. Vui lòng tải lại danh sách."
            );
        }
    }

    private String generateUniqueCode() {
        String code;

        do {
            code = "SALE" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase(Locale.ROOT);
        } while (voucherRepository.existsByCodeIgnoreCase(code));

        return code;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }

    private String normalizeNullableKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String value = keyword.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeDiscountType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);

        if (!isPercentType(value) && !isFixedType(value)) {
            throw new IllegalArgumentException("Kiểu giảm giá không hợp lệ!");
        }

        return value;
    }

    private boolean isPercentType(String type) {
        return "PERCENT".equals(type)
                || "PERCENTAGE".equals(type)
                || "%".equals(type);
    }

    private boolean isFixedType(String type) {
        return "FIXED".equals(type)
                || "AMOUNT".equals(type)
                || "MONEY".equals(type);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String decimalOf(BigDecimal value) {
        return value == null ? "<null>" : value.stripTrailingZeros().toPlainString();
    }

    private String timeOf(LocalDateTime value) {
        return value == null ? "<null>" : value.toString();
    }

    private String valueOf(Object value) {
        return value == null ? "<null>" : String.valueOf(value);
    }
}
