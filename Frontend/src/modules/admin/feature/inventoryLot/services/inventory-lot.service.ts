import api from "@/common/api";

import type {
  InventoryLotDetailResponse,
  InventoryLotListParams,
  InventoryLotListResponse,
  InventoryLotLockHistoryResponse,
  InventoryLotLockRequest,
  InventoryLotSourceResponse,
  InventoryLotUnlockRequest,
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
        isLocked: params.isLocked,
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

  async getLockHistory(id: number): Promise<InventoryLotLockHistoryResponse[]> {
    const response = await api.get(`/admin/inventory-lots/${id}/lock-history`);
    return response.data;
  },

  async lock(
    id: number,
    request: InventoryLotLockRequest
  ): Promise<InventoryLotDetailResponse> {
    const response = await api.post(`/admin/inventory-lots/${id}/lock`, request);
    return response.data;
  },

  async unlock(
    id: number,
    request?: InventoryLotUnlockRequest
  ): Promise<InventoryLotDetailResponse> {
    const reason = request?.reason?.trim();

    const response = reason
      ? await api.post(`/admin/inventory-lots/${id}/unlock`, { reason })
      : await api.post(`/admin/inventory-lots/${id}/unlock`);

    return response.data;
  },
};

export default inventoryLotService;
