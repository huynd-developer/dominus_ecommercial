package org.example.datn_sd69.modules.orderStatus.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.orderStatus.dto.UpdateOrderStatusRequest;
import org.example.datn_sd69.modules.orderStatus.service.AdminOrderStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyAuthority('OWNER','MANAGER','CASHIER')")
public class AdminOrderStatusController {

    private final AdminOrderStatusService adminOrderStatusService;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable @Positive(message = "orderId phải là số nguyên dương") Integer orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        Order order = adminOrderStatusService.updateStatus(orderId, request.status());

        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật trạng thái đơn hàng thành công",
                "orderId", order.getId(),
                "status", order.getStatus(),
                "loyaltyPointsApplied", Boolean.TRUE.equals(order.getLoyaltyPointsApplied()),
                "loyaltyPointsEarned", order.getLoyaltyPointsEarned() == null ? 0 : order.getLoyaltyPointsEarned()
        ));
    }
}
