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
        return concentrationRepository.findByIsDeletedFalse();
    }

    @Override
    public Concentration getById(Integer id) {
        Concentration concentration = concentrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nồng độ có ID: " + id));

        if (Boolean.TRUE.equals(concentration.getIsDeleted())) {
            throw new IllegalArgumentException("Nồng độ này đã bị xóa!");
        }
        return concentration;
    }

    @Override
    public Concentration create(ConcentrationRequest request) {
        String name = request.getName().trim().replaceAll("\\s+", " ");

        Optional<Concentration> existingOpt = concentrationRepository.findByNameIgnoreCase(name);

        if (existingOpt.isPresent()) {
            Concentration existingConcentration = existingOpt.get();

            if (Boolean.TRUE.equals(existingConcentration.getIsDeleted())) {
                existingConcentration.setIsDeleted(false);
                existingConcentration.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                existingConcentration.setName(name);

                return concentrationRepository.save(existingConcentration);
            } else {
                throw new IllegalArgumentException("Nồng độ '" + name + "' đã tồn tại!");
            }
        }

        Concentration concentration = new Concentration();
        concentration.setName(name);
        concentration.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        concentration.setIsDeleted(false);

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
                throw new IllegalArgumentException("Tên nồng độ '" + newName + "' đang nằm trong thùng rác (đã bị xóa)!");
            }
            throw new IllegalArgumentException("Nồng độ '" + newName + "' đã được sử dụng!");
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
        concentration.setIsDeleted(true);
        concentrationRepository.save(concentration);
    }

    @Override
    public Page<Concentration> getAll(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            // Đã bổ sung chuẩn hóa khoảng trắng cho từ khóa tìm kiếm
            return concentrationRepository.searchByName(keyword.trim().replaceAll("\\s+", " "), pageable);
        }
        return concentrationRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public Page<Concentration> getActiveConcentrations(int page, int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return concentrationRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }
}