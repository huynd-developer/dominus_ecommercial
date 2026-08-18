import api from "@/common/api";

import type {
  ExpiryAlertListParams,
  ExpiryAlertListResponse,
  ExpiryAlertSummaryResponse,
  InventoryLotDetailResponse,
  PageResponse,
} from "../types/expiry-alert.type";

const cleanParams = (
  params: Record<string, unknown>
): Record<string, unknown> => {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null && value !== ""
    )
  );
};

const expiryAlertService = {
  /**
   * GET /api/admin/expiry-alerts
   */
  async getList(
    params: ExpiryAlertListParams
  ): Promise<PageResponse<ExpiryAlertListResponse>> {
    const response = await api.get("/admin/expiry-alerts", {
      params: cleanParams({
        group: params.group ?? "NEAR_EXPIRY",

        keyword: params.keyword?.trim() || undefined,

        fromDays: params.fromDays ?? undefined,

        toDays: params.toDays ?? undefined,

        page: params.page ?? 0,

        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  /**
   * GET /api/admin/expiry-alerts/summary
   */
  async getSummary(): Promise<ExpiryAlertSummaryResponse> {
    const response = await api.get("/admin/expiry-alerts/summary");

    return response.data;
  },

  /**
   * GET /api/admin/expiry-alerts/{id}
   */
  async getDetail(id: number): Promise<InventoryLotDetailResponse> {
    const response = await api.get(`/admin/expiry-alerts/${id}`);

    return response.data;
  },
};

export default expiryAlertService;