package org.example.datn_sd69.modules.bottleType.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.BottleType;
import org.example.datn_sd69.modules.bottleType.dto.request.BottleTypeRequest;
import org.example.datn_sd69.modules.bottleType.service.BottleTypeService;
import org.example.datn_sd69.repository.BottleTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BottleTypeServiceImpl implements BottleTypeService {
    private final BottleTypeRepository bottleTypeRepository;

    @Override
    public List<BottleType> getAll() {
        return bottleTypeRepository.findByStatusNot(0);
    }

    @Override
    public BottleType getById(Integer id) {
        return bottleTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại chai có ID: " + id));
    }

    @Override
    public BottleType create(BottleTypeRequest request) {
        String name = request.getName().trim();

        Optional<BottleType> existingOpt = bottleTypeRepository.findByNameIgnoreCase(name);

        if (existingOpt.isPresent()) {
            BottleType existingType = existingOpt.get();
            if (existingType.getStatus() == 0) {
                // Khôi phục nếu đã xóa mềm
                existingType.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                existingType.setName(name);
                return bottleTypeRepository.save(existingType);
            } else {
                throw new RuntimeException("Loại chai '" + name + "' đã tồn tại!");
            }
        }

        BottleType bottleType = new BottleType();
        bottleType.setName(name);
        bottleType.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        return bottleTypeRepository.save(bottleType);
    }

    @Override
    public BottleType update(Integer id, BottleTypeRequest request) {
        BottleType existingType = getById(id);
        String newName = request.getName().trim();

        Optional<BottleType> checkDuplicateOpt = bottleTypeRepository.findByNameIgnoreCase(newName);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            throw new RuntimeException("Loại chai '" + newName + "' đã được sử dụng ở một bản ghi khác!");
        }

        existingType.setName(newName);
        if (request.getStatus() != null) {
            existingType.setStatus(request.getStatus());
        }

        return bottleTypeRepository.save(existingType);
    }

    @Override
    public void delete(Integer id) {
        BottleType bottleType = getById(id);
        bottleType.setStatus(0); // Xóa mềm
        bottleTypeRepository.save(bottleType);
    }

    @Override
    public Page<BottleType> getAll(Pageable pageable) {
        return bottleTypeRepository.findByStatusNot(0, pageable);
    }

    @Override
    public Page<BottleType> getActiveBottleTypes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bottleTypeRepository.findByStatus(1, pageable);
    }
}
