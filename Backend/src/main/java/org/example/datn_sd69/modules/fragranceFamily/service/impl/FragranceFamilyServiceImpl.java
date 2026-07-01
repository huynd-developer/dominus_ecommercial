package org.example.datn_sd69.modules.fragranceFamily.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.FragranceFamily;
import org.example.datn_sd69.modules.fragranceFamily.dto.request.FragranceFamilyRequest;
import org.example.datn_sd69.modules.fragranceFamily.service.FragranceFamilyService;
import org.example.datn_sd69.repository.FragranceFamilyRepository;
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

    @Override
    public List<FragranceFamily> getAll() {
        return fragranceFamilyRepository.findByStatusNot(0);
    }

    @Override
    public FragranceFamily getById(Integer id) {
        return fragranceFamilyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm hương có ID: " + id));
    }

    @Override
    public FragranceFamily create(FragranceFamilyRequest request) {
        String name = request.getName().trim();

        // 1. Check xem tên này đã có trong DB chưa (kể cả bản ghi cũ đã xóa mềm)
        Optional<FragranceFamily> existingOpt = fragranceFamilyRepository.findByNameIgnoreCase(name);

        if (existingOpt.isPresent()) {
            FragranceFamily existingFamily = existingOpt.get();
            if (existingFamily.getStatus() == 0) {
                // Nếu đã xóa mềm trước đó -> Tự động khôi phục (Reactivate)
                existingFamily.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                existingFamily.setName(name); // Đồng bộ lại chữ hoa/thường chuẩn nhất
                return fragranceFamilyRepository.save(existingFamily);
            } else {
                // Nếu đang hoạt động mà định thêm trùng -> Chặn luôn!
                throw new RuntimeException("Nhóm hương '" + name + "' đã tồn tại!");
            }
        }

        // 2. Nếu chưa từng tồn tại -> Tạo mới hoàn toàn
        FragranceFamily fragranceFamily = new FragranceFamily();
        fragranceFamily.setName(name);
        fragranceFamily.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        return fragranceFamilyRepository.save(fragranceFamily);
    }

    @Override
    public FragranceFamily update(Integer id, FragranceFamilyRequest request) {
        FragranceFamily existingFamily = getById(id);
        String newName = request.getName().trim();

        // Check xem tên mới sửa có trùng với ID khác trong DB không
        Optional<FragranceFamily> checkDuplicateOpt = fragranceFamilyRepository.findByNameIgnoreCase(newName);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            throw new RuntimeException("Nhóm hương '" + newName + "' đã được sử dụng ở một bản ghi khác!");
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
        fragranceFamily.setStatus(0); // Xóa mềm
        fragranceFamilyRepository.save(fragranceFamily);
    }

    @Override
    public Page<FragranceFamily> getAll(Pageable pageable) {
        return fragranceFamilyRepository.findByStatusNot(0, pageable);
    }

    @Override
    public Page<FragranceFamily> getActiveFragranceFamilies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fragranceFamilyRepository.findByStatus(1, pageable);
    }
}
