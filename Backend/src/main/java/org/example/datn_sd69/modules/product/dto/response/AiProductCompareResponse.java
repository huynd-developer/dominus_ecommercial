package org.example.datn_sd69.modules.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiProductCompareResponse {

    private List<Integer> productIds;

    /*
     * Giữ lại để tương thích với caller cũ nếu còn sử dụng.
     * FE compare mới sẽ đọc dữ liệu có cấu trúc từ insights.
     */
    private String analysis;

    private List<ProductInsight> insights;

    @Getter
    @Builder
    public static class ProductInsight {

        private Integer productId;

        private String longevity;

        private String style;

        private String occasion;
    }
}