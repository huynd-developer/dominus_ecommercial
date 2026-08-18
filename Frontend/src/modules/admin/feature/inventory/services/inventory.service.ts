import api from "@/common/api";

import type {
  InventoryConfig,
  InventoryConfigUpdateRequest,
  InventoryLotParams,
  InventoryLotStatus,
  InventoryOverview,
  InventoryOverviewParams,
  InventorySummary,
  PageResponse,
} from "../types/inventory.type";

const BASE_URL = "/admin/inventory";

const cleanParams = (params: Record<string, unknown>) => {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) =>
        value !== undefined &&
        value !== null &&
        value !== ""
    )
  );
};

export const inventoryService = {
  async getSummary(): Promise<InventorySummary> {
    const response = await api.get<InventorySummary>(
      `${BASE_URL}/summary`
    );

    return response.data;
  },

  async getOverview(
    params: InventoryOverviewParams
  ): Promise<PageResponse<InventoryOverview>> {
    const response = await api.get<
      PageResponse<InventoryOverview>
    >(`${BASE_URL}/overview`, {
      params: cleanParams({
        keyword: params.keyword?.trim(),
        nearExpiry: params.nearExpiry,
        expired: params.expired,
        stockStatus: params.stockStatus ?? "ALL",
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  async getNearExpiry(
    params: InventoryLotParams
  ): Promise<PageResponse<InventoryLotStatus>> {
    const response = await api.get<
      PageResponse<InventoryLotStatus>
    >(`${BASE_URL}/near-expiry`, {
      params: cleanParams({
        keyword: params.keyword?.trim(),
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  async getExpired(
    params: InventoryLotParams
  ): Promise<PageResponse<InventoryLotStatus>> {
    const response = await api.get<
      PageResponse<InventoryLotStatus>
    >(`${BASE_URL}/expired`, {
      params: cleanParams({
        keyword: params.keyword?.trim(),
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  async getConfig(): Promise<InventoryConfig> {
    const response = await api.get<InventoryConfig>(
      `${BASE_URL}/config`
    );

    return response.data;
  },

  async updateConfig(
    request: InventoryConfigUpdateRequest
  ): Promise<InventoryConfig> {
    const response = await api.put<InventoryConfig>(
      `${BASE_URL}/config`,
      request
    );

    return response.data;
  },
};

export default inventoryService;