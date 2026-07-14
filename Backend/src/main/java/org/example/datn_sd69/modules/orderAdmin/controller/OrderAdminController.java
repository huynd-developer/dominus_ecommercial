//package org.example.datn_sd69.modules.orderAdmin.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderResponse;
//import org.example.datn_sd69.modules.orderAdmin.service.OrderService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/admin/orders")
//@RequiredArgsConstructor
//public class OrderAdminController {
//
//    private final OrderService orderService;
//
//    /**
//     * Danh sách đơn hàng ONLINE + IN_STORE
//     */
//    @GetMapping
//    public ResponseEntity<Page<OrderResponse.ListItem>> getOrders(
//
//            @RequestParam(required = false) String keyword,
//
//            @RequestParam(required = false) Integer status,
//
//            @RequestParam(required = false) String orderType,
//
//            @RequestParam(defaultValue = "0") Integer page,
//
//            @RequestParam(defaultValue = "10") Integer size
//    ) {
//
//        Pageable pageable = PageRequest.of(
//                page,
//                size,
//                Sort.by("createdAt").descending()
//        );
//
//        return ResponseEntity.ok(
//                orderService.getOrders(
//                        keyword,
//                        status,
//                        orderType,
//                        pageable
//                )
//        );
//    }
//
//    /**
//     * Chi tiết đơn hàng
//     */
//    @GetMapping("/{orderId}")
//    public ResponseEntity<OrderResponse.Detail> getOrderDetail(
//            @PathVariable Integer orderId
//    ) {
//
//        return ResponseEntity.ok(
//                orderService.getOrderDetail(orderId)
//        );
//    }
//
//    /**
//     * Chuyển trạng thái:
//     * 0 -> 1 -> 2 -> 3
//     */
//    @PutMapping("/{orderId}/next-status")
//    public ResponseEntity<OrderResponse.UpdateStatus> nextStatus(
//            @PathVariable Integer orderId
//    ) {
//
//        return ResponseEntity.ok(
//                orderService.nextStatus(orderId)
//        );
//    }
//
//    /**
//     * Hủy đơn hàng
//     * status = 4
//     * hoàn lại tồn kho
//     */
//    @PutMapping("/{orderId}/cancel")
//    public ResponseEntity<OrderResponse.UpdateStatus> cancelOrder(
//            @PathVariable Integer orderId
//    ) {
//
//        return ResponseEntity.ok(
//                orderService.cancelOrder(orderId)
//        );
//    }
//}