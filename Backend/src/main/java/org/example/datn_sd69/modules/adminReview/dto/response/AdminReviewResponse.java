package org.example.datn_sd69.modules.adminReview.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AdminReviewResponse {
    private Integer id;
    private String productName;
    private String productSku;
    private String customerName;
    private String customerEmail;
    private Integer rating;
    private String comment;
    private Integer approvalStatus;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectedReason;
    private List<AdminReviewMediaResponse> media;
}
