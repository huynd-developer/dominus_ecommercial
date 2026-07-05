package org.example.datn_sd69.modules.bottleType.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.BottleType;
import org.example.datn_sd69.modules.bottleType.dto.request.BottleTypeRequest;
import org.example.datn_sd69.modules.bottleType.service.BottleTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bottle-types")
@RequiredArgsConstructor
public class AdminBottleTypeController {
    private final BottleTypeService bottleTypeService;

    @GetMapping
    public ResponseEntity<Page<BottleType>> getAllBottleTypes(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bottleTypeService.getAll(keyword, pageable));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<BottleType> createBottleType(@Valid @RequestBody BottleTypeRequest request) {
        return ResponseEntity.ok(bottleTypeService.create(request));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<BottleType> updateBottleType(
            @PathVariable Integer id,
            @Valid @RequestBody BottleTypeRequest request) {
        return ResponseEntity.ok(bottleTypeService.update(id, request));
    }

    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBottleType(@PathVariable Integer id) {
        bottleTypeService.delete(id);
        return ResponseEntity.ok("Xóa loại chai thành công!");
    }
}
