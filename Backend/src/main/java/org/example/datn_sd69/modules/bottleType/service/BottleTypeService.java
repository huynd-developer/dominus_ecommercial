package org.example.datn_sd69.modules.bottleType.service;

import org.example.datn_sd69.entity.BottleType;
import org.example.datn_sd69.modules.bottleType.dto.request.BottleTypeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BottleTypeService {
    List<BottleType> getAll();
    BottleType getById(Integer id);
    BottleType create(BottleTypeRequest request);
    BottleType update(Integer id, BottleTypeRequest request);
    void delete(Integer id);

    Page<BottleType> getAll(Pageable pageable);

    Page<BottleType> getActiveBottleTypes(int page, int size);
}
