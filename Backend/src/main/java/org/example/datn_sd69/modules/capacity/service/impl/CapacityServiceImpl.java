package org.example.datn_sd69.modules.capacity.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Capacity;
import org.example.datn_sd69.modules.capacity.dto.request.CapacityRequest;
import org.example.datn_sd69.modules.capacity.service.CapacityService;
import org.example.datn_sd69.repository.CapacityRepository;
// ĐÃ THÊM IMPORT: Để kiểm tra biến thể
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapacityServiceImpl implements CapacityService {

    private final CapacityRepository capacityRepository;
    // ĐÃ THÊM: Inject Repository để quét sản phẩm
    private final ProductVariantRepository productVariantRepository;

    @Override
    public List<Capacity> getAll() {
        return capacityRepository.findByIsDeletedFalse();
    }

    @Override
    public Capacity getById(Integer id) {
        Capacity capacity = capacityRepository.findById(id)
                .orElseThrow(() -> badRequest("Không tìm thấy dung tích có ID: " + id));

        if (Boolean.TRUE.equals(capacity.getIsDeleted())) {
            throw badRequest("Dung tích này đã bị xóa!");
        }

        return capacity;
    }

    @Override
    public Capacity create(CapacityRequest request) {
        Double value = normalizeValue(request.getValue());
        Optional<Capacity> existingOpt = capacityRepository.findByValue(value);

        if (existingOpt.isPresent()) {
            Capacity existingCapacity = existingOpt.get();

            if (Boolean.TRUE.equals(existingCapacity.getIsDeleted())) {
                existingCapacity.setIsDeleted(false);
                existingCapacity.setStatus(request.getStatus());
                return capacityRepository.save(existingCapacity);
            }

            throw badRequest("Dung tích '" + formatMl(value) + " ml' đã tồn tại và đang hoạt động!");
        }

        Capacity capacity = new Capacity();
        capacity.setValue(value);
        capacity.setStatus(request.getStatus());
        capacity.setIsDeleted(false);

        return capacityRepository.save(capacity);
    }

    @Override
    public Capacity update(Integer id, CapacityRequest request) {
        Capacity existingCapacity = getById(id);
        Double newValue = normalizeValue(request.getValue());

        Optional<Capacity> checkDuplicateOpt = capacityRepository.findByValue(newValue);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            Capacity duplicateCapacity = checkDuplicateOpt.get();

            if (Boolean.TRUE.equals(duplicateCapacity.getIsDeleted())) {
                throw badRequest("Dung tích '" + formatMl(newValue) + " ml' đang nằm trong thùng rác!");
            }

            throw badRequest("Dung tích '" + formatMl(newValue) + " ml' đã được sử dụng ở một bản ghi khác!");
        }

        existingCapacity.setValue(newValue);
        existingCapacity.setStatus(request.getStatus());

        return capacityRepository.save(existingCapacity);
    }

    @Override
    public void delete(Integer id) {
        Capacity capacity = getById(id);

        // ĐÃ THÊM: Kiểm tra xem dung tích này có đang được sử dụng ở biến thể sản phẩm nào không
        // Nếu có thì chặn ngay lập tức, quăng Exception ra cho Frontend bắt
        boolean isUsed = productVariantRepository.existsByCapacity_IdAndIsDeletedFalse(id);
        if (isUsed) {
            throw new IllegalStateException("Không thể ném vào thùng rác! Đang có sản phẩm thuộc dung tích này.");
        }

        capacity.setIsDeleted(true);
        capacity.setStatus(0); // Ẩn luôn cho an toàn
        capacityRepository.save(capacity);
    }

    @Override
    public Page<Capacity> getActiveCapacities(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return capacityRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }

    @Override
    public Page<Capacity> getAllAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return capacityRepository.findByIsDeletedFalse(pageable);
        }

        try {
            Double searchVal = Double.parseDouble(keyword.trim());
            return capacityRepository.findByValueAndIsDeletedFalse(searchVal, pageable);
        } catch (NumberFormatException e) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }

    private Double normalizeValue(Double value) {
        if (value == null) {
            throw badRequest("Dung tích không được để trống");
        }
        if (value.isNaN() || value.isInfinite()) {
            throw badRequest("Dung tích không hợp lệ");
        }
        return value;
    }

    private String formatMl(Double value) {
        return new DecimalFormat("#.##").format(value);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}