package org.example.datn_sd69.modules.fragranceFamily.service;

import org.example.datn_sd69.entity.FragranceFamily;
import org.example.datn_sd69.modules.fragranceFamily.dto.request.FragranceFamilyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FragranceFamilyService {
    List<FragranceFamily> getAll();
    FragranceFamily getById(Integer id);
    FragranceFamily create(FragranceFamilyRequest request);
    FragranceFamily update(Integer id, FragranceFamilyRequest request);
    void delete(Integer id);

    Page<FragranceFamily> getAll(String keyword,Pageable pageable);

    Page<FragranceFamily> getActiveFragranceFamilies(int page, int size);
}
