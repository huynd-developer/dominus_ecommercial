package org.example.datn_sd69.modules.fragranceFamily.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.FragranceFamily;
import org.example.datn_sd69.modules.fragranceFamily.service.FragranceFamilyService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fragrance-families")
@RequiredArgsConstructor
public class PublicFragranceFamilyController {
    private final FragranceFamilyService fragranceFamilyService;

    @GetMapping
    public ResponseEntity<Page<FragranceFamily>> getActiveFragranceFamilies(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        return ResponseEntity.ok(fragranceFamilyService.getActiveFragranceFamilies(page, size));
    }
}
