package org.example.datn_sd69.modules.adminReview.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

@Data
public class CreateReviewRequest {
    @NotNull(message = "ID sản phẩm không được để trống")
    private Integer orderItemId;

    private Integer productVariantId; // Có thể null

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
    private Integer rating;

    @NotBlank(message = "Nội dung đánh giá không được để trống")
    private String comment;

    // KHÔNG BẮT BUỘC: Khách gửi null hoặc danh sách rỗng [] đều hợp lệ
    // Có thêm @Valid để NẾU khách có gửi ảnh thì mới Validate các field bên trong ReviewMediaRequest
    @Valid
    private List<ReviewMediaRequest> mediaList;

    @Data
    public static class ReviewMediaRequest {
        @NotBlank(message = "URL media không được để trống")
        private String mediaUrl;

        @NotBlank(message = "Loại media không được để trống")
        private String mediaType; // "IMAGE" hoặc "VIDEO"
    }
}
