//package org.example.datn_sd69.modules.orderAdmin.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderResponse;
//import org.example.datn_sd69.modules.orderAdmin.service.OrderService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/orders")
//@RequiredArgsConstructor
//public class OrderControllerTrung {
//
//    private final OrderService orderService;
//
//    /**
//     * Danh sách đơn hàng
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
//        Pageable pageable = PageRequest.of(page, size);
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
//    @GetMapping("/{id}")
//    public ResponseEntity<OrderResponse.Detail> getOrderDetail(
//            @PathVariable Integer id
//    ) {
//
//        return ResponseEntity.ok(
//                orderService.getOrderDetail(id)
//        );
//    }
//
//    /**
//     * Chuyển trạng thái
//     * 0 -> 1 -> 2 -> 3
//     */
//    @PutMapping("/{id}/next-status")
//    public ResponseEntity<OrderResponse.UpdateStatus> nextStatus(
//            @PathVariable Integer id
//    ) {
//
//        return ResponseEntity.ok(
//                orderService.nextStatus(id)
//        );
//    }
//
//    /**
//     * Hủy đơn hàng
//     */
//    @PutMapping("/{id}/cancel")
//    public ResponseEntity<OrderResponse.UpdateStatus> cancelOrder(
//            @PathVariable Integer id
//    ) {
//
//        return ResponseEntity.ok(
//                orderService.cancelOrder(id)
//        );
//    }
//}