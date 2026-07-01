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
        return concentrationRepository.findByStatusNot(0);
    }

    @Override
    public Concentration getById(Integer id) {
        return concentrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nồng độ có ID: " + id));
    }

    @Override
    public Concentration create(ConcentrationRequest request) {
        String name = request.getName().trim();

        // 1. Kiểm tra xem tên này đã từng tồn tại dưới DB chưa
        Optional<Concentration> existingOpt = concentrationRepository.findByNameIgnoreCase(name);

        if (existingOpt.isPresent()) {
            Concentration existingConcentration = existingOpt.get();
            if (existingConcentration.getStatus() == 0) {
                // Nếu đã bị xóa mềm -> Khôi phục lại
                existingConcentration.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                // Cập nhật lại tên cho chuẩn format (lỡ người dùng gõ chữ hoa chữ thường khác nhau)
                existingConcentration.setName(name);
                return concentrationRepository.save(existingConcentration);
            } else {
                throw new RuntimeException("Nồng độ '" + name + "' đã tồn tại!");
            }
        }

        // 2. Chưa từng tồn tại -> Tạo mới tinh
        Concentration concentration = new Concentration();
        concentration.setName(name);
        concentration.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        return concentrationRepository.save(concentration);
    }

    @Override
    public Concentration update(Integer id, ConcentrationRequest request) {
        Concentration existingConcentration = getById(id);
        String newName = request.getName().trim();

        Optional<Concentration> checkDuplicateOpt = concentrationRepository.findByNameIgnoreCase(newName);

        if (checkDuplicateOpt.isPresent() && !checkDuplicateOpt.get().getId().equals(id)) {
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
        Concentration concentration = getById(id);
        concentration.setStatus(0);
        concentrationRepository.save(concentration);
    }

    @Override
    public Page<Concentration> getAll(Pageable pageable) {
        return concentrationRepository.findByStatusNot(0, pageable);
    }

    @Override
    public Page<Concentration> getActiveConcentrations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return concentrationRepository.findByStatus(1, pageable);
    }
}
