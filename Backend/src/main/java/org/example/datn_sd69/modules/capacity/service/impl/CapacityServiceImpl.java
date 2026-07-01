package org.example.datn_sd69.modules.capacity.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Capacity;
import org.example.datn_sd69.modules.capacity.dto.request.CapacityRequest;
import org.example.datn_sd69.modules.capacity.service.CapacityService;
import org.example.datn_sd69.repository.CapacityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapacityServiceImpl implements CapacityService {

    private final CapacityRepository capacityRepository;

    @Override
    public List<Capacity> getAll() {
        return capacityRepository.findByStatusNot(0);
    }

    @Override
    public Capacity getById(Integer id) {
        return capacityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dung tích có ID: " + id));
    }

    @Override
    public Capacity create(CapacityRequest request) {
        Double value = request.getValue();

        // 1. Kiểm tra xem dung tích này đã từng tồn tại dưới DB chưa (Bao gồm cả thằng đã bị xóa)
        Optional<Capacity> existingOpt = capacityRepository.findByValue(value);

        if (existingOpt.isPresent()) {
            Capacity existingCapacity = existingOpt.get();
            if (existingCapacity.getStatus() == 0) {
                // Nếu đã bị xóa mềm -> Khôi phục lại trạng thái hoạt động thay vì tạo mới
                existingCapacity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                return capacityRepository.save(existingCapacity);
            } else {
                // Nếu đang hoạt động -> Chặn luôn vì trùng lặp
                throw new RuntimeException("Dung tích '" + value + "' đã tồn tại!");
            }
        }

        // 2. Nếu chưa từng tồn tại -> Tạo mới tinh
        Capacity capacity = new Capacity();
        capacity.setValue(value);
        capacity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        return capacityRepository.save(capacity);
    }

    @Override
    public Capacity update(Integer id, CapacityRequest request) {
        Capacity existingCapacity = getById(id);
        Double newValue = request.getValue();

        // Kiểm tra xem số mới cập nhật có bị trùng với một số khác đã có trong DB không
        Optional<Capacity> checkDuplicateOpt = capacityRepository.findByValue(newValue);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            throw new RuntimeException("Dung tích '" + newValue + "' đã được sử dụng ở một bản ghi khác!");
        }

        existingCapacity.setValue(newValue);
        if (request.getStatus() != null) {
            existingCapacity.setStatus(request.getStatus());
        }

        return capacityRepository.save(existingCapacity);
    }

    @Override
    public void delete(Integer id) {
        Capacity capacity = getById(id);
        capacity.setStatus(0); // Xóa mềm
        capacityRepository.save(capacity);
    }

    @Override
    public Page<Capacity> getAll(Pageable pageable) {
        return capacityRepository.findByStatusNot(0, pageable);
    }

    @Override
    public Page<Capacity> getActiveCapacities(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return capacityRepository.findByStatus(1, pageable);
    }
}