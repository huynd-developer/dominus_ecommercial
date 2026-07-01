package org.example.datn_sd69.modules.bottleType.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.BottleType;
import org.example.datn_sd69.modules.bottleType.service.BottleTypeService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bottle-types")
@RequiredArgsConstructor
public class PublicBottleTypeController {
    private final BottleTypeService bottleTypeService;

    @GetMapping
    public ResponseEntity<Page<BottleType>> getActiveBottleTypes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        return ResponseEntity.ok(bottleTypeService.getActiveBottleTypes(page, size));
    }
}
