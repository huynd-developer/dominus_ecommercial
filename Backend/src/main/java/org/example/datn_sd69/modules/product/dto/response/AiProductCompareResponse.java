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
     * FE compare có thể dùng field này để hiển thị
     * tóm tắt khác biệt giữa các sản phẩm.
     */
    private String analysis;

    /*
     * Gợi ý lựa chọn từ AI.
     *
     * Đây là field bổ sung, không thay thế analysis/insights
     * để tránh làm ảnh hưởng caller cũ.
     */
    private String recommendation;

    /*
     * Dữ liệu AI có cấu trúc theo từng sản phẩm.
     * FE có thể dùng để bổ sung trực tiếp vào bảng so sánh.
     */
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