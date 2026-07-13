export type PromotionStatus = 0 | 1;

export interface PromotionVariantRequest {
  productVariantId: number;
  discountPercent: number;
}

export interface PromotionRequest {
  name: string;
  startDate: string;
  endDate: string;
  variants: PromotionVariantRequest[];
}

export interface PromotionStatusRequest {
  status: PromotionStatus;
}

export interface PromotionVariantResponse {
  productVariantId: number;
  sku: string | null;
  productName: string | null;
  capacity: string | null;
  bottleType: string | null;
  originalPrice: number;
  discountPercent: number;
  salePrice: number;
  stockQuantity: number | null;
}

export interface PromotionResponse {
   id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: PromotionStatus;
  statusText: string;
  activeNow: boolean;
  ended: boolean;
  variants: PromotionVariantResponse[];
}

export interface PromotionProductVariantOptionResponse {
  productVariantId: number;
  productId: number | null;
  productName: string | null;
  sku: string | null;
  capacity: string | null;
  bottleType: string | null;
  price: number;
  stockQuantity: number | null;
  status: number | null;
  manufacturingDate: string | null;
  expirationDate: string | null;
  availableForPromotion: boolean;
  unavailableReason: string | null;
}

export interface FlashSaleProductResponse {
  promotionId: number;
  promotionName: string;
  endDate: string;
  productVariantId: number;
  productId: number | null;
  productName: string | null;
  sku: string | null;
  capacity: string | null;
  bottleType: string | null;
  originalPrice: number;
  discountPercent: number;
  salePrice: number;
  stockQuantity: number | null;
}

export interface PromotionVariantFormItem {
  productVariantId: number;
  discountPercent: number;
  sku?: string | null;
  productName?: string | null;
  capacity?: string | null;
  bottleType?: string | null;
  originalPrice?: number;
  price?: number;
  salePrice?: number;
  stockQuantity?: number | null;
}

export interface PageResponse<T> {
  content: T[];

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

export interface PromotionSearchParams {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}

export interface ProductVariantSearchParams {
  keyword?: string;
  startDate?: string;
  endDate?: string;
  ignorePromotionId?: number | null;
  page?: number;
  size?: number;
}