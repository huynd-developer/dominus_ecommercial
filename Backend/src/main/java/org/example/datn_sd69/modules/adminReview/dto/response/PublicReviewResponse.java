package org.example.datn_sd69.modules.adminReview.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PublicReviewResponse {
    private Integer id;

    // Tên khách hàng (Thường ẩn bớt ở Service như: "Nguyễn V***")
    private String customerName;

    private String customerAvatar;

    private Integer rating; // Số sao (1 - 5)

    private String comment; // Nội dung đánh giá

    private LocalDateTime createdAt;

    // Tên phân loại sản phẩm khách đã mua (vd: "100ml / Chai thủy tinh")
    private String variantName;

    // Danh sách ảnh/video đi kèm đánh giá
    private List<ReviewMediaDTO> mediaList;

    @Data
    public static class ReviewMediaDTO {
        private Integer id;
        private String mediaUrl;
        private String mediaType; // "IMAGE" hoặc "VIDEO"
    }
}
