package org.example.datn_sd69.modules.capacity.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Capacity;
import org.example.datn_sd69.modules.capacity.service.CapacityService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/capacities")
@RequiredArgsConstructor
public class PublicCapacityController {

    private final CapacityService capacityService;

    @GetMapping
    public ResponseEntity<Page<Capacity>> getActiveCapacities(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        return ResponseEntity.ok(capacityService.getActiveCapacities(page, size));
    }
}