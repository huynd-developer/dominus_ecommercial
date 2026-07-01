package org.example.datn_sd69.modules.capacity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Capacity;

// Nhớ check lại đường dẫn import file Request của bạn nhé
import org.example.datn_sd69.modules.capacity.dto.request.CapacityRequest;
import org.example.datn_sd69.modules.capacity.service.CapacityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/capacities") // ĐƯỜNG DẪN ADMIN
@RequiredArgsConstructor
public class AdminCapacityController {

    private final CapacityService capacityService;

    @GetMapping
    public ResponseEntity<Page<Capacity>> getAllCapacities(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        // Đã đổi thành gọi hàm getAll()
        return ResponseEntity.ok(capacityService.getAll(pageable));
    }

    // 2. Thêm mới dung tích
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<Capacity> createCapacity(@Valid @RequestBody CapacityRequest capacityRequest) {
        // Đã đổi thành gọi hàm create()
        return ResponseEntity.ok(capacityService.create(capacityRequest));
    }

    // 3. Cập nhật dung tích
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<Capacity> updateCapacity(
            @PathVariable Integer id,
            @Valid @RequestBody CapacityRequest capacityRequest) {
        // Đã đổi thành gọi hàm update()
        return ResponseEntity.ok(capacityService.update(id, capacityRequest));
    }

    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCapacity(@PathVariable Integer id) {
        // Đã đổi thành gọi hàm delete()
        capacityService.delete(id);
        return ResponseEntity.ok("Xóa dung tích thành công!");
    }
}