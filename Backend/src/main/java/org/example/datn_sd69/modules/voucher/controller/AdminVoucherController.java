package org.example.datn_sd69.modules.voucher.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherService voucherService;

    // API lấy danh sách Voucher
    @GetMapping
    public ResponseEntity<?> getAllVouchers() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    // API thêm mới Voucher
    @PostMapping
    public ResponseEntity<?> createVoucher(@Valid @RequestBody VoucherRequest request) {
        try {
            voucherService.createVoucher(request);
            return ResponseEntity.ok().body("Thêm voucher thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // API Lấy chi tiết 1 Voucher để fill vào form Sửa
    @GetMapping("/{id}")
    public ResponseEntity<?> getVoucher(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(voucherService.getVoucherById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API Cập nhật Voucher
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(@PathVariable Integer id, @Valid @RequestBody VoucherRequest request) {
        try {
            voucherService.updateVoucher(id, request);
            return ResponseEntity.ok().body("Cập nhật voucher thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // API Xóa Voucher (Xóa mềm)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Integer id) {
        try {
            voucherService.deleteVoucher(id);
            return ResponseEntity.ok().body("Xóa voucher thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}