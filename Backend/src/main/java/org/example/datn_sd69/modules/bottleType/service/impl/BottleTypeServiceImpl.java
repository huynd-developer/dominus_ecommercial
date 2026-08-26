package org.example.datn_sd69.modules.bottleType.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.BottleType;
import org.example.datn_sd69.modules.bottleType.dto.request.BottleTypeRequest;
import org.example.datn_sd69.modules.bottleType.service.BottleTypeService;
import org.example.datn_sd69.repository.BottleTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại chai có ID: " + id));
    }

    @Override
    public BottleType create(BottleTypeRequest request) {
        // Chuẩn hóa khoảng trắng
        String name = request.getName().trim().replaceAll("\\s+", " ");

        Optional<BottleType> existingOpt = bottleTypeRepository.findByNameIgnoreCase(name);

        if (existingOpt.isPresent()) {
            BottleType existingType = existingOpt.get();
            // ĐÃ SỬA: Phải check cả isDeleted và status để khôi phục toàn diện
            if (existingType.getStatus() == 0 || Boolean.TRUE.equals(existingType.getIsDeleted())) {
                existingType.setIsDeleted(false); // Đánh dấu không còn bị xóa
                existingType.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                existingType.setName(name);
                return bottleTypeRepository.save(existingType);
            } else {
                // Trả về chuẩn 400 Bad Request
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại chai '" + name + "' đã tồn tại trong hệ thống!");
            }
        }

        BottleType bottleType = new BottleType();
        bottleType.setName(name);
        bottleType.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        bottleType.setIsDeleted(false);
        return bottleTypeRepository.save(bottleType);
    }

    @Override
    public BottleType update(Integer id, BottleTypeRequest request) {
        BottleType existingType = getById(id);
        // Chuẩn hóa khoảng trắng
        String newName = request.getName().trim().replaceAll("\\s+", " ");

        // Lấy tất cả lên để quét, loại trừ chính nó và loại trừ những thằng đã bị xóa mềm
        List<BottleType> allTypes = bottleTypeRepository.findAll();
        for (BottleType type : allTypes) {
            if (!type.getId().equals(id)
                    && type.getName().equalsIgnoreCase(newName)
                    && !Boolean.TRUE.equals(type.getIsDeleted())) {

                // Ném lỗi với message chuẩn
                throw new IllegalArgumentException("Loại chai '" + newName + "' đã được sử dụng ở một bản ghi khác!");
            }
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
        bottleType.setIsDeleted(true); // Xóa mềm
        bottleType.setStatus(0); // Ẩn luôn khỏi giao diện bán hàng
        bottleTypeRepository.save(bottleType);
    }

    @Override
    public Page<BottleType> getAll(String keyword, Pageable pageable) {
        // Bổ sung chuẩn hóa khoảng trắng cho từ khóa tìm kiếm
        String searchKeyword = (keyword == null) ? "" : keyword.trim().replaceAll("\\s+", " ");
        return bottleTypeRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(searchKeyword, pageable);
    }

    @Override
    public Page<BottleType> getActiveBottleTypes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bottleTypeRepository.findByStatus(1, pageable);
    }
}