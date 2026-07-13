package org.example.datn_sd69.modules.promotion.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PromotionResponse {

    private Integer id;

    private String name;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    /**
     * 1 = bật chiến dịch
     * 0 = tắt / hết hạn / xóa mềm
     */
    private Integer status;

    private String statusText;

    private Boolean activeNow;

    private Boolean ended;

    private List<PromotionVariantResponse> variants;
}