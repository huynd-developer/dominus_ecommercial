import api from "@/common/api";

import type {
  InventoryLotDetailResponse,
  InventoryLotListParams,
  InventoryLotListResponse,
  InventoryLotSourceResponse,
  PageResponse,
} from "../types/inventory-lot.type";

const cleanParams = (params: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null && value !== ""
    )
  );

const inventoryLotService = {
  async getList(
    params: InventoryLotListParams
  ): Promise<PageResponse<InventoryLotListResponse>> {
    const response = await api.get("/admin/inventory-lots", {
      params: cleanParams({
        keyword: params.keyword?.trim() || undefined,
        productVariantId: params.productVariantId ?? undefined,
        isExpired: params.isExpired,
        isNearExpiry: params.isNearExpiry,
        hasStock: params.hasStock,
        expirationFrom: params.expirationFrom || undefined,
        expirationTo: params.expirationTo || undefined,
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  async getDetail(id: number): Promise<InventoryLotDetailResponse> {
    const response = await api.get(`/admin/inventory-lots/${id}`);
    return response.data;
  },

  async getSource(id: number): Promise<InventoryLotSourceResponse> {
    const response = await api.get(`/admin/inventory-lots/${id}/source`);
    return response.data;
  },
};

export default inventoryLotService;