package org.example.datn_sd69.modules.concentration.service;

import org.example.datn_sd69.entity.Concentration;
import org.example.datn_sd69.modules.concentration.dto.request.ConcentrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConcentrationService {
    List<Concentration> getAll();
    Concentration getById(Integer id);
    Concentration create(ConcentrationRequest request);
    Concentration update(Integer id, ConcentrationRequest request);
    void delete(Integer id);

    Page<Concentration> getAll(String keyword, Pageable pageable);

    Page<Concentration> getActiveConcentrations(int page, int size);
}
