import api from "@/common/api";
import type {
  FlashSaleProductResponse,
  FlashSaleSearchParams,
  PageResponse,
  ProductVariantSearchParams,
  PromotionProductVariantOptionResponse,
  PromotionRequest,
  PromotionResponse,
  PromotionSearchParams,
  PromotionStatusRequest,
} from "../types/promotion.types";

const normalizeDateTime = (value?: string | null) => {
  if (!value) return undefined;

  if (value.length === 16) {
    return `${value}:00`;
  }

  return value;
};

export const promotionService = {
  getAll(params: PromotionSearchParams = {}) {
    return api.get<PageResponse<PromotionResponse>>("/admin/promotions", {
      params: {
        keyword: params.keyword || undefined,
        status: params.status ?? undefined,
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    });
  },

  getById(id: number) {
    return api.get<PromotionResponse>(`/admin/promotions/${id}`);
  },

  searchProductVariants(params: ProductVariantSearchParams = {}) {
    return api.get<PageResponse<PromotionProductVariantOptionResponse>>(
      "/admin/promotions/product-variants",
      {
        params: {
          keyword: params.keyword || undefined,
          startDate: normalizeDateTime(params.startDate),
          endDate: normalizeDateTime(params.endDate),
          ignorePromotionId: params.ignorePromotionId ?? undefined,
          page: params.page ?? 0,
          size: params.size ?? 10,
        },
      }
    );
  },

  create(payload: PromotionRequest) {
    return api.post<PromotionResponse>("/admin/promotions", {
      name: payload.name,
      startDate: normalizeDateTime(payload.startDate),
      endDate: normalizeDateTime(payload.endDate),
      variants: payload.variants,
    });
  },

  update(id: number, payload: PromotionRequest) {
    return api.put<PromotionResponse>(`/admin/promotions/${id}`, {
      name: payload.name,
      startDate: normalizeDateTime(payload.startDate),
      endDate: normalizeDateTime(payload.endDate),
      variants: payload.variants,
    });
  },

  changeStatus(id: number, payload: PromotionStatusRequest) {
    return api.patch<PromotionResponse>(
      `/admin/promotions/${id}/status`,
      payload
    );
  },

  remove(id: number) {
    return api.delete(`/admin/promotions/${id}`);
  },

  getFlashSaleProducts(params: FlashSaleSearchParams = {}) {
    return api.get<PageResponse<FlashSaleProductResponse>>(
      "/promotions/flash-sale",
      {
        params: {
          page: params.page ?? 0,
          size: params.size ?? 8,
        },
      }
    );
  },
};