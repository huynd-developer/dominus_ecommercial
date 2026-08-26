import api from "@/common/api";
import inventoryLotService from "@/modules/admin/feature/inventoryLot/services/inventory-lot.service";

import type { InventoryLotListResponse } from "@/modules/admin/feature/inventoryLot/types/inventory-lot.type";

import type {
  PageResponse,
  StockAdjustmentCancelRequest,
  StockAdjustmentDetailResponse,
  StockAdjustmentListParams,
  StockAdjustmentListResponse,
  StockAdjustmentRejectRequest,
  StockAdjustmentSaveRequest,
} from "../types/stock-adjustment.type";

const cleanParams = (params: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null && value !== ""
    )
  );

const stockAdjustmentService = {
  async getList(
    params: StockAdjustmentListParams
  ): Promise<PageResponse<StockAdjustmentListResponse>> {
    const response = await api.get("/admin/stock-adjustments", {
      params: cleanParams({
        keyword: params.keyword?.trim() || undefined,
        status: params.status || undefined,
        createdBy: params.createdBy ?? undefined,
        fromDate: params.fromDate || undefined,
        toDate: params.toDate || undefined,
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  async getDetail(id: number): Promise<StockAdjustmentDetailResponse> {
    const response = await api.get(`/admin/stock-adjustments/${id}`);
    return response.data;
  },

  async getPendingCount(): Promise<number> {
    const response = await api.get("/admin/stock-adjustments/pending-count");

    const value = Number(response.data);

    return Number.isFinite(value) ? value : 0;
  },

  async create(
    request: StockAdjustmentSaveRequest
  ): Promise<StockAdjustmentDetailResponse> {
    const response = await api.post("/admin/stock-adjustments", request);

    return response.data;
  },

  async update(
    id: number,
    request: StockAdjustmentSaveRequest
  ): Promise<StockAdjustmentDetailResponse> {
    const response = await api.put(`/admin/stock-adjustments/${id}`, request);

    return response.data;
  },

  async submit(id: number): Promise<StockAdjustmentDetailResponse> {
    const response = await api.post(`/admin/stock-adjustments/${id}/submit`);

    return response.data;
  },

  async cancel(
    id: number,
    request: StockAdjustmentCancelRequest
  ): Promise<StockAdjustmentDetailResponse> {
    const response = await api.post(`/admin/stock-adjustments/${id}/cancel`, {
      reason: request.reason.trim(),
    });

    return response.data;
  },

  async approve(id: number): Promise<StockAdjustmentDetailResponse> {
    const response = await api.post(`/admin/stock-adjustments/${id}/approve`);

    return response.data;
  },

  async reject(
    id: number,
    request: StockAdjustmentRejectRequest
  ): Promise<StockAdjustmentDetailResponse> {
    const response = await api.post(`/admin/stock-adjustments/${id}/reject`, {
      reason: request.reason.trim(),
    });

    return response.data;
  },

  /**
   * Danh sách lô dùng riêng cho popup tạo/sửa phiếu kiểm kê.
   *
   * Không truyền hasStock để vẫn có thể kiểm kê:
   * - lô đang còn tồn;
   * - lô hệ thống = 0 nhưng Product/SKU vẫn còn quản lý,
   *   nhằm phát hiện trường hợp thực tế có hàng thừa.
   *
   * BE sẽ tự xử lý:
   * - Product/SKU chưa xóa -> hiển thị;
   * - Product/SKU đã xóa nhưng QuantityOnHand > 0 -> vẫn hiển thị;
   * - Product/SKU đã xóa và QuantityOnHand = 0 -> không hiển thị.
   */
  async searchLots(keyword: string): Promise<InventoryLotListResponse[]> {
    const data = await inventoryLotService.getAuditCandidates({
      keyword: keyword.trim(),
      page: 0,
      size: 100,
    });

    return Array.isArray(data?.content) ? data.content : [];
  },
};

export default stockAdjustmentService;
