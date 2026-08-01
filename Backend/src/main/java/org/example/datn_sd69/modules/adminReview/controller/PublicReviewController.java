package org.example.datn_sd69.modules.adminReview.controller;

import jakarta.validation.Valid;
import org.example.datn_sd69.modules.adminReview.dto.response.PublicReviewResponse;
import org.example.datn_sd69.modules.adminReview.service.PublicReviewService;
import org.example.datn_sd69.modules.adminReview.dto.request.CreateReviewRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class PublicReviewController {
    @Autowired
    private PublicReviewService publicReviewService; // Khai báo Service tương ứng

    /**
     * API 1: Khách hàng xem danh sách đánh giá của 1 sản phẩm.
     * - KHÔNG có @PreAuthorize: Bất kỳ ai (chưa đăng nhập) cũng có thể xem.
     * - Logic ở Service: Chỉ lấy các bản ghi có trạng thái = 1 (Đã duyệt).
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<PublicReviewResponse>> getApprovedReviews(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Page<PublicReviewResponse> result = publicReviewService.getApprovedReviewsByProduct(productId, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * API 2: Khách hàng gửi đánh giá mới sau khi mua hàng.
     * - CÓ @PreAuthorize: Bắt buộc phải đăng nhập với quyền USER (hoặc CUSTOMER).
     * - Logic ở Service: Lưu dữ liệu vào DB với trạng thái mặc định = 0 (Chờ duyệt).
     */
    @PreAuthorize("hasAuthority('USER')") // Thay 'USER' bằng tên Role khách hàng trong hệ thống của bạn nếu khác
    @PostMapping
    public ResponseEntity<String> createReview(@Valid @RequestBody CreateReviewRequest request) {

        publicReviewService.createReview(request);
        return ResponseEntity.ok("Gửi đánh giá thành công. Đánh giá của bạn đang chờ kiểm duyệt!");
    }
}
