package org.example.datn_sd69.modules.adminReview.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectReviewRequest {
    @NotBlank(message = "Lý do từ chối không được để trống!")
    private String rejectedReason;
}
