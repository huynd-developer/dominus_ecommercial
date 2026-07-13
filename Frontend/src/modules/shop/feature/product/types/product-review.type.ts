export interface PageResponse<T> {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;

  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface PublicProductReviewResponse {
  reviewId: number;
  productId: number | null;
  productVariantId: number | null;
  productName: string;
  brandName: string | null;
  sku: string | null;

  customerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ProductReviewSummaryResponse {
  productId: number;
  averageRating: number;
  reviewCount: number;
  fiveStarCount: number;
  fourStarCount: number;
  threeStarCount: number;
  twoStarCount: number;
  oneStarCount: number;
}

export interface ReviewSearchParams {
  page?: number;
  size?: number;
}