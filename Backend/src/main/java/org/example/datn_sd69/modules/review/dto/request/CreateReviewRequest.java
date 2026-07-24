package org.example.datn_sd69.modules.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public record CreateReviewRequest(

        @NotNull(message = "orderItemId không được để trống")
        @Positive(message = "orderItemId phải là số nguyên dương")
        Integer orderItemId,

        @NotNull(message = "Số sao đánh giá không được để trống")
        @Min(value = 1, message = "Số sao đánh giá tối thiểu là 1")
        @Max(value = 5, message = "Số sao đánh giá tối đa là 5")
        Integer rating,

        @Size(max = 1000, message = "Bình luận tối đa 1000 ký tự")
        String comment,

        // ĐÃ THÊM: Hứng danh sách file từ Frontend gửi lên
        List<MultipartFile> mediaFiles
) {
}