package org.example.datn_sd69.modules.capacity.service;

import org.example.datn_sd69.entity.Capacity;
import org.example.datn_sd69.modules.capacity.dto.request.CapacityRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CapacityService {
    List<Capacity> getAll();
    Capacity getById(Integer id);
    Capacity create(CapacityRequest request);
    Capacity update(Integer id, CapacityRequest request);
    void delete(Integer id);

    Page<Capacity> getAllAdmin(String keyword, Pageable pageable);
    Page<Capacity> getActiveCapacities(int page, int size);
}