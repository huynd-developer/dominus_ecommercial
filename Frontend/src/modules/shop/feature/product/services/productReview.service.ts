import api from "@/common/api";
import type {
  PageResponse,
  ProductReviewSummaryResponse,
  PublicProductReviewResponse,
  ReviewSearchParams,
} from "../types/product-review.type";

export const productReviewService = {
  getSummary(productId: number) {
    return api.get<ProductReviewSummaryResponse>(
      `/shop/products/${productId}/reviews/summary`
    );
  },

  getReviews(productId: number, params: ReviewSearchParams = {}) {
    return api.get<
      PageResponse<PublicProductReviewResponse> | PublicProductReviewResponse[]
    >(`/shop/products/${productId}/reviews`, {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 5,
      },
    });
  },
};