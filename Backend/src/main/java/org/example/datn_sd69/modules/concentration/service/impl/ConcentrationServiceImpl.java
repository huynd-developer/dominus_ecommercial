package org.example.datn_sd69.modules.concentration.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Concentration;
import org.example.datn_sd69.modules.concentration.dto.request.ConcentrationRequest;
import org.example.datn_sd69.modules.concentration.service.ConcentrationService;
import org.example.datn_sd69.repository.ConcentrationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConcentrationServiceImpl implements ConcentrationService {

    private final ConcentrationRepository concentrationRepository;

    @Override
    public List<Concentration> getAll() {
        // Chỉ lấy những bản ghi chưa bị xóa
        return concentrationRepository.findByIsDeletedFalse();
    }

    @Override
    public Concentration getById(Integer id) {
        Concentration concentration = concentrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nồng độ có ID: " + id));

        // Chặn người dùng thao tác với bản ghi đã bị xóa mềm (trừ khi cố tình bypass qua DB)
        if (Boolean.TRUE.equals(concentration.getIsDeleted())) {
            throw new RuntimeException("Nồng độ này đã bị xóa!");
        }
        return concentration;
    }

    @Override
    public Concentration create(ConcentrationRequest request) {
        // Dọn dẹp khoảng trắng thừa
        String name = request.getName().trim().replaceAll("\\s+", " ");

        // 1. Kiểm tra xem tên này đã từng tồn tại dưới DB chưa
        Optional<Concentration> existingOpt = concentrationRepository.findByNameIgnoreCase(name);

        if (existingOpt.isPresent()) {
            Concentration existingConcentration = existingOpt.get();

            if (Boolean.TRUE.equals(existingConcentration.getIsDeleted())) {
                // Nếu đã bị xóa mềm -> Khôi phục lại
                existingConcentration.setIsDeleted(false);
                existingConcentration.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                existingConcentration.setName(name); // Format lại đúng chữ hoa/thường

                return concentrationRepository.save(existingConcentration);
            } else {
                throw new RuntimeException("Nồng độ '" + name + "' đã tồn tại!");
            }
        }

        // 2. Chưa từng tồn tại -> Tạo mới
        Concentration concentration = new Concentration();
        concentration.setName(name);
        concentration.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        concentration.setIsDeleted(false); // Đảm bảo tạo mới luôn là false

        return concentrationRepository.save(concentration);
    }

    @Override
    public Concentration update(Integer id, ConcentrationRequest request) {
        Concentration existingConcentration = getById(id);
        String newName = request.getName().trim().replaceAll("\\s+", " ");

        Optional<Concentration> checkDuplicateOpt = concentrationRepository.findByNameIgnoreCase(newName);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
            Concentration duplicateConfig = checkDuplicateOpt.get();
            if (Boolean.TRUE.equals(duplicateConfig.getIsDeleted())) {
                throw new RuntimeException("Tên nồng độ '" + newName + "' đang nằm trong thùng rác (đã bị xóa)!");
            }
            throw new RuntimeException("Nồng độ '" + newName + "' đã được sử dụng!");
        }

        existingConcentration.setName(newName);
        if (request.getStatus() != null) {
            existingConcentration.setStatus(request.getStatus());
        }

        return concentrationRepository.save(existingConcentration);
    }

    @Override
    public void delete(Integer id) {
        // Hàm getById đã check sẵn validate tồn tại và chưa bị xóa rồi
        Concentration concentration = getById(id);

        // Chuyển cờ isDeleted thành true (Xóa mềm)
        concentration.setIsDeleted(true);

        concentrationRepository.save(concentration);
    }

    @Override
    public Page<Concentration> getAll(String keyword, Pageable pageable) {
        // Nếu có nhập chữ vào ô tìm kiếm
        if (keyword != null && !keyword.trim().isEmpty()) {
            return concentrationRepository.searchByName(keyword.trim(), pageable);
        }
        // Nếu ô tìm kiếm để trống thì trả về toàn bộ danh sách chưa xóa
        return concentrationRepository.findByIsDeletedFalse(pageable);
    }
    @Override
    public Page<Concentration> getActiveConcentrations(int page, int size) {
        // Giới hạn size tránh fetch quá nhiều dữ liệu
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);

        // Trạng thái = 1 (Active) VÀ chưa bị xóa (isDeleted = false)
        return concentrationRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }
}