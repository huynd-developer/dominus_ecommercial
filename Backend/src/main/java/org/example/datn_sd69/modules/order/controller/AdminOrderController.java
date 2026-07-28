package org.example.datn_sd69.modules.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping({"", "/"})
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        return ResponseEntity.ok(
                adminOrderService.getOrders(keyword, status, orderType, pageable)
        );
    }

    @GetMapping({"/{orderId}", "/{orderId}/"})
    public ResponseEntity<?> getOrderDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(
                adminOrderService.getOrderDetail(orderId)
        );
    }
}