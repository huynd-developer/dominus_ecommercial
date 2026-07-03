package org.example.datn_sd69.modules.customer.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.customer.dto.response.CustomerResponse;
import org.example.datn_sd69.modules.customer.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerAdminController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(
                customerService.getCustomers(keyword, status, pageable)
        );
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(
                customerService.getCustomerById(userId)
        );
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public ResponseEntity<CustomerResponse> updateStatus(
            @PathVariable Integer userId,
            @RequestParam Integer status
    ) {
        return ResponseEntity.ok(
                customerService.updateStatus(userId, status)
        );
    }
}