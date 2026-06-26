package org.example.datn_sd69.modules.brand.controller;

import org.example.datn_sd69.modules.brand.request.BrandRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thuong-hieu")
@CrossOrigin("*")
public class BrandController {

    // 1. Xem danh sách (Phân trang + Tìm kiếm)
    // TẤT CẢ các role đều được xem (Bao phủ mọi case chữ Hoa/Thường từ DB)
    @PreAuthorize("hasAnyAuthority(" +
            "'OWNER', 'Owner', 'owner', " +
            "'MANAGER', 'Manager', 'manager', " +
            "'CASHIER', 'Cashier', 'cashier', " +
            "'USER', 'User', 'user')")
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<Map<String, Object>> mockBrands = List.of(
                Map.of("id", 1, "name", "Gucci", "description", "Thương hiệu thời trang Ý", "status", 1),
                Map.of("id", 2, "name", "Dior", "description", "Nước hoa xa xỉ Pháp", "status", 1),
                Map.of("id", 3, "name", "Chanel", "description", "Thời trang đẳng cấp", "status", 1)
        );

        return ResponseEntity.ok(Map.of(
                "content", mockBrands,
                "totalElements", mockBrands.size(),
                "totalPages", 1,
                "size", size,
                "number", page,
                "message", "Test Mock Data: Lấy danh sách thành công!"
        ));
    }

    // 2. Xem chi tiết 1 Brand theo ID
    // TẤT CẢ các role đều được xem
    @PreAuthorize("hasAnyAuthority(" +
            "'OWNER', 'Owner', 'owner', " +
            "'MANAGER', 'Manager', 'manager', " +
            "'CASHIER', 'Cashier', 'cashier', " +
            "'USER', 'User', 'user')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", "Thương hiệu Test " + id,
                "description", "Mô tả giả lập cho ID " + id,
                "status", 1
        ));
    }

    // 3. Thêm mới Brand
    // Chỉ OWNER, MANAGER, CASHIER được phép làm (Cấm USER)
    @PreAuthorize("hasAnyAuthority(" +
            "'OWNER', 'Owner', 'owner', " +
            "'MANAGER', 'Manager', 'manager', " +
            "'CASHIER', 'Cashier', 'cashier')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody BrandRequest request) {
        return ResponseEntity.ok(Map.of(
                "id", 99,
                "name", request.getName(),
                "description", request.getDescription(),
                "status", request.getStatus(),
                "message", "Test Mock Data: Thêm mới thành công!"
        ));
    }

    // 4. Cập nhật Brand
    // Chỉ OWNER, MANAGER, CASHIER được phép làm
    @PreAuthorize("hasAnyAuthority(" +
            "'OWNER', 'Owner', 'owner', " +
            "'MANAGER', 'Manager', 'manager', " +
            "'CASHIER', 'Cashier', 'cashier')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody BrandRequest request) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", request.getName(),
                "description", request.getDescription(),
                "status", request.getStatus(),
                "message", "Test Mock Data: Cập nhật thành công!"
        ));
    }

    // 5. Xóa mềm Brand (Chuyển Status về 0)
    // Chỉ OWNER, MANAGER, CASHIER được phép làm
    @PreAuthorize("hasAnyAuthority(" +
            "'OWNER', 'Owner', 'owner', " +
            "'MANAGER', 'Manager', 'manager', " +
            "'CASHIER', 'Cashier', 'cashier')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", 0,
                "message", "Test Mock Data: Xóa thành công ID " + id
        ));
    }
}