package org.example.datn_sd69.modules.capacity.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Capacity;
import org.example.datn_sd69.modules.capacity.dto.request.CapacityRequest; // Import chuẩn ở đây
import org.example.datn_sd69.modules.capacity.service.CapacityService;
import org.example.datn_sd69.repository.CapacityRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapacityServiceImpl implements CapacityService {

    private final CapacityRepository capacityRepository;

    @Override
    public List<Capacity> getAll() {
        // Đã sửa từ findByStatusNot thành findByIsDeletedFalse
        return capacityRepository.findByIsDeletedFalse();
    }

    @Override
    public Capacity getById(Integer id) {
        Capacity capacity = capacityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dung tích có ID: " + id));
        if (capacity.getIsDeleted()) {
            throw new RuntimeException("Dung tích này đã bị xóa!");
        }
        return capacity;
    }

    @Override
    public Capacity create(CapacityRequest request) {
        Double value = request.getValue();
        Optional<Capacity> existingOpt = capacityRepository.findByValue(value);

        if (existingOpt.isPresent()) {
            Capacity existingCapacity = existingOpt.get();
            if (existingCapacity.getIsDeleted()) {
                existingCapacity.setIsDeleted(false);
                existingCapacity.setStatus(request.getStatus());
                return capacityRepository.save(existingCapacity);
            } else {
                throw new RuntimeException("Dung tích '" + value + " ml' đã tồn tại và đang hoạt động!");
            }
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
        Double newValue = request.getValue();

        Optional<Capacity> checkDuplicateOpt = capacityRepository.findByValue(newValue);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            Capacity duplicateCapacity = checkDuplicateOpt.get();
            if (duplicateCapacity.getIsDeleted()) {
                throw new RuntimeException("Dung tích '" + newValue + " ml' đang nằm trong thùng rác!");
            } else {
                throw new RuntimeException("Dung tích '" + newValue + " ml' đã được sử dụng ở một bản ghi khác!");
            }
        }

        existingCapacity.setValue(newValue);
        existingCapacity.setStatus(request.getStatus());
        return capacityRepository.save(existingCapacity);
    }

    @Override
    public void delete(Integer id) {
        Capacity capacity = getById(id);
        capacity.setIsDeleted(true);
        capacity.setStatus(0);
        capacityRepository.save(capacity);
    }

//    @Override
//    public Page<Capacity> getAll(Pageable pageable) {
//        return capacityRepository.findByIsDeletedFalse(pageable);
//    }

    @Override
    public Page<Capacity> getActiveCapacities(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return capacityRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }

    @Override
    public Page<Capacity> getAllAdmin(String keyword, Pageable pageable) {

        // Nếu không có từ khóa tìm kiếm -> lấy tất cả với pageable (đã có sẵn Sort từ Controller)
        if (keyword == null || keyword.trim().isEmpty()) {
            return capacityRepository.findByIsDeletedFalse(pageable);
        }

        // Nếu có từ khóa, cố gắng ép kiểu sang Double để tìm kiếm
        try {
            Double searchVal = Double.parseDouble(keyword.trim());
            return capacityRepository.findByValueAndIsDeletedFalse(searchVal, pageable);
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập chữ (VD: "abc") -> Trả về danh sách rỗng
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
}