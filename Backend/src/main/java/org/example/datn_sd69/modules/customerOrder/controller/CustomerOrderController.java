package org.example.datn_sd69.modules.customerOrder.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.customerOrder.service.CustomerOrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * FE gửi multipart/form-data:
     * - returnType
     * - reason
     * - description
     * - email
     * - refundMethod
     * - bankName
     * - bankAccountNumber
     * - bankAccountHolder
     * - returnItems hoặc items: JSON string [{"orderItemId":1,"quantity":1}]
     * - mediaFiles hoặc files
     */
    @PutMapping(
            value = "/{orderId}/request-return",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> requestReturnOrder(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId,

            @RequestParam String returnType,
            @RequestParam String reason,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String email,
            @RequestParam String refundMethod,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String bankAccountNumber,
            @RequestParam(required = false) String bankAccountHolder,

            @RequestParam(required = false) String returnItems,
            @RequestParam(required = false) String items,

            @RequestParam(required = false, name = "mediaFiles")
            List<MultipartFile> mediaFiles,

            @RequestParam(required = false, name = "files")
            List<MultipartFile> files
    ) {
        customerOrderService.requestReturnOrder(
                orderId,
                returnType,
                reason,
                description,
                email,
                refundMethod,
                bankName,
                bankAccountNumber,
                bankAccountHolder,
                chooseReturnItemsPayload(returnItems, items),
                mergeFiles(mediaFiles, files)
        );

        return ResponseEntity.ok(Map.of(
                "message", "Gửi yêu cầu hoàn hàng thành công"
        ));
    }

    /**
     * Giữ lại route JSON cũ, nhưng vẫn bắt buộc có returnItems/items.
     * Không tự mặc định hoàn toàn bộ đơn.
     */
    @PutMapping(
            value = "/{orderId}/request-return",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> requestReturnOrderJson(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId,

            @RequestBody Map<String, String> payload
    ) {
        String returnItemsPayload = payload == null
                ? null
                : chooseReturnItemsPayload(payload.get("returnItems"), payload.get("items"));

        customerOrderService.requestReturnOrder(
                orderId,
                payload == null ? null : payload.getOrDefault("returnType", "RECEIVED_WITH_PROBLEM"),
                payload == null ? null : payload.get("reason"),
                payload == null ? null : payload.get("description"),
                payload == null ? null : payload.get("email"),
                payload == null ? null : payload.getOrDefault("refundMethod", "STORE"),
                payload == null ? null : payload.get("bankName"),
                payload == null ? null : payload.get("bankAccountNumber"),
                payload == null ? null : payload.get("bankAccountHolder"),
                returnItemsPayload,
                null
        );

        return ResponseEntity.ok(Map.of(
                "message", "Gửi yêu cầu hoàn hàng thành công"
        ));
    }

    @PutMapping("/{orderId}/cancel-return")
    public ResponseEntity<?> cancelReturnRequest(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId
    ) {
        customerOrderService.cancelReturnRequest(orderId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã hủy yêu cầu hoàn hàng thành công"
        ));
    }

    private String chooseReturnItemsPayload(String returnItems, String items) {
        if (returnItems != null && !returnItems.trim().isEmpty()) {
            return returnItems;
        }

        if (items != null && !items.trim().isEmpty()) {
            return items;
        }

        return null;
    }

    private List<MultipartFile> mergeFiles(
            List<MultipartFile> mediaFiles,
            List<MultipartFile> files
    ) {
        List<MultipartFile> result = new ArrayList<>();

        if (mediaFiles != null) {
            result.addAll(mediaFiles);
        }

        if (files != null) {
            result.addAll(files);
        }

        return result.isEmpty() ? null : result;
    }
}
