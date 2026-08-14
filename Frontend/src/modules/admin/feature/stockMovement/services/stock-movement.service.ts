import api from "@/common/api";

import type {
  PageResponse,
  StockMovementDetailResponse,
  StockMovementListParams,
  StockMovementListResponse,
} from "../types/stock-movement.type";

const cleanParams = (
  params: Record<string, unknown>
): Record<string, unknown> =>
  Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) =>
        value !== undefined &&
        value !== null &&
        value !== ""
    )
  );

const stockMovementService = {
  async getList(
    params: StockMovementListParams
  ): Promise<PageResponse<StockMovementListResponse>> {
    const response = await api.get(
      "/admin/stock-movements",
      {
        params: cleanParams({
          keyword: params.keyword?.trim() || undefined,
          inventoryLotId: params.inventoryLotId ?? undefined,
          movementType: params.movementType || undefined,
          createdBy: params.createdBy ?? undefined,
          referenceType: params.referenceType?.trim() || undefined,
          referenceId: params.referenceId ?? undefined,
          fromDate: params.fromDate || undefined,
          toDate: params.toDate || undefined,
          page: params.page ?? 0,
          size: params.size ?? 20,
        }),
      }
    );

    return response.data;
  },

  async getDetail(
    id: number
  ): Promise<StockMovementDetailResponse> {
    const response = await api.get(
      `/admin/stock-movements/${id}`
    );

    return response.data;
  },
};

export default stockMovementService;
