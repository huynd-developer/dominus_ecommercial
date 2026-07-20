package org.example.datn_sd69.modules.capacity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Capacity;
import org.example.datn_sd69.modules.capacity.dto.request.CapacityRequest;
import org.example.datn_sd69.modules.capacity.service.CapacityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/capacities")
@RequiredArgsConstructor
public class AdminCapacityController {

    private final CapacityService capacityService;

    @GetMapping
    public ResponseEntity<Page<Capacity>> getAllCapacities(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "99") int size) {

        // CẤU HÌNH SORT: Sắp xếp giảm dần theo cột "id" để bản ghi mới thêm lên đầu trang 1
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // TRUYỀN PAGEABLE VÀO SERVICE (Chuẩn Spring Boot)
        return ResponseEntity.ok(capacityService.getAllAdmin(keyword, pageable));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<Capacity> createCapacity(@Valid @RequestBody CapacityRequest capacityRequest) {
        return ResponseEntity.ok(capacityService.create(capacityRequest));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<Capacity> updateCapacity(
            @PathVariable Integer id,
            @Valid @RequestBody CapacityRequest capacityRequest) {
        return ResponseEntity.ok(capacityService.update(id, capacityRequest));
    }

    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCapacity(@PathVariable Integer id) {
        capacityService.delete(id);
        return ResponseEntity.ok("Xóa dung tích thành công!");
    }
}