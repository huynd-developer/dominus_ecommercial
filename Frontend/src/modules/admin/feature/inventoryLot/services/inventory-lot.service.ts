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
  /**
   * Danh sách lô dùng chung cho màn Quản lý lô hàng.
   *
   * GIỮ NGUYÊN endpoint cũ để không ảnh hưởng các màn/module
   * đang sử dụng danh sách InventoryLot hiện tại.
   */
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

  /**
   * Danh sách lô dùng RIÊNG cho popup chọn lô khi tạo phiếu kiểm kê.
   *
   * Khớp với BE:
   * GET /api/admin/inventory-lots/audit-candidates
   *
   * Không thay thế getList() để tránh ảnh hưởng màn Quản lý lô hàng.
   */
  async getAuditCandidates(
    params: InventoryLotListParams
  ): Promise<PageResponse<InventoryLotListResponse>> {
    const response = await api.get(
      "/admin/inventory-lots/audit-candidates",
      {
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
      }
    );

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