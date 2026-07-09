import api from "@/common/api";
import type {
  ProductReviewSummaryResponse,
  PublicProductReviewResponse,
} from "../types/product-review.type";

export const productReviewService = {
  getSummary(productId: number) {
    return api.get<ProductReviewSummaryResponse>(
      `/shop/products/${productId}/reviews/summary`
    );
  },

  getReviews(productId: number) {
    return api.get<PublicProductReviewResponse[]>(
      `/shop/products/${productId}/reviews`
    );
  },
};