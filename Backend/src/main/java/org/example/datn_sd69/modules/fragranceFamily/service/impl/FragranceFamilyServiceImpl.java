package org.example.datn_sd69.modules.fragranceFamily.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.FragranceFamily;
import org.example.datn_sd69.modules.fragranceFamily.dto.request.FragranceFamilyRequest;
import org.example.datn_sd69.modules.fragranceFamily.service.FragranceFamilyService;
import org.example.datn_sd69.repository.FragranceFamilyRepository;
// ĐÃ THÊM: Import ProductRepository để kiểm tra ràng buộc
import org.example.datn_sd69.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FragranceFamilyServiceImpl implements FragranceFamilyService {

    private final FragranceFamilyRepository fragranceFamilyRepository;
    // ĐÃ THÊM: Inject ProductRepository
    private final ProductRepository productRepository;

    @Override
    public List<FragranceFamily> getAll() {
        return fragranceFamilyRepository.findByIsDeletedFalse();
    }

    @Override
    public FragranceFamily getById(Integer id) {
        FragranceFamily family = fragranceFamilyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm hương có ID: " + id));

        if (Boolean.TRUE.equals(family.getIsDeleted())) {
            throw new IllegalArgumentException("Nhóm hương này đã bị xóa khỏi hệ thống!");
        }
        return family;
    }

    @Override
    public FragranceFamily create(FragranceFamilyRequest request) {
        String name = request.getName().trim().replaceAll("\\s+", " ");
        Optional<FragranceFamily> existingOpt = fragranceFamilyRepository.findByName(name);

        if (existingOpt.isPresent()) {
            FragranceFamily existingFamily = existingOpt.get();

            if (Boolean.TRUE.equals(existingFamily.getIsDeleted())) {
                existingFamily.setIsDeleted(false);
                existingFamily.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                existingFamily.setName(name);
                return fragranceFamilyRepository.save(existingFamily);
            } else {
                throw new IllegalArgumentException("Nhóm hương '" + name + "' đã tồn tại!");
            }
        }

        FragranceFamily fragranceFamily = new FragranceFamily();
        fragranceFamily.setName(name);
        fragranceFamily.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        fragranceFamily.setIsDeleted(false);

        return fragranceFamilyRepository.save(fragranceFamily);
    }

    @Override
    public FragranceFamily update(Integer id, FragranceFamilyRequest request) {
        FragranceFamily existingFamily = getById(id);
        String newName = request.getName().trim().replaceAll("\\s+", " ");

        Optional<FragranceFamily> checkDuplicateOpt = fragranceFamilyRepository.findByName(newName);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            FragranceFamily duplicateFamily = checkDuplicateOpt.get();
            if (Boolean.TRUE.equals(duplicateFamily.getIsDeleted())) {
                throw new IllegalArgumentException("Tên nhóm hương '" + newName + "' đang nằm trong thùng rác!");
            }
            throw new IllegalArgumentException("Nhóm hương '" + newName + "' đã được sử dụng ở một bản ghi khác!");
        }

        existingFamily.setName(newName);
        if (request.getStatus() != null) {
            existingFamily.setStatus(request.getStatus());
        }

        return fragranceFamilyRepository.save(existingFamily);
    }

    @Override
    public void delete(Integer id) {
        FragranceFamily fragranceFamily = getById(id);

        // ĐÃ THÊM: Kiểm tra ràng buộc trước khi xóa
        boolean isUsed = productRepository.existsByFragranceFamilies_IdAndIsDeletedFalse(id);
        if (isUsed) {
            throw new IllegalStateException("Không thể ném vào thùng rác! Đang có sản phẩm thuộc nhóm hương này.");
        }

        fragranceFamily.setIsDeleted(true);
        fragranceFamily.setStatus(0); // Ẩn luôn cho an toàn
        fragranceFamilyRepository.save(fragranceFamily);
    }

    @Override
    public Page<FragranceFamily> getAll(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return fragranceFamilyRepository.searchByName(keyword.trim().replaceAll("\\s+", " "), pageable);
        }
        return fragranceFamilyRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public Page<FragranceFamily> getActiveFragranceFamilies(int page, int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return fragranceFamilyRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }
}