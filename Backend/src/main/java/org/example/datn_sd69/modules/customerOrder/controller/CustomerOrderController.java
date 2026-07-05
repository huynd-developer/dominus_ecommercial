package org.example.datn_sd69.modules.customerOrder.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.customerOrder.service.CustomerOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
@Validated
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    @GetMapping
    public ResponseEntity<?> getMyOrders() {
        return ResponseEntity.ok(customerOrderService.getMyOrders());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId
    ) {
        return ResponseEntity.ok(customerOrderService.getOrderDetail(orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId
    ) {
        customerOrderService.cancelOrder(orderId);

        return ResponseEntity.ok(Map.of(
                "message", "Hủy đơn hàng thành công"
        ));
    }
}