import api from "@/common/api";

import type {
  GoodsReceiptApprovalHistoryResponse,
  GoodsReceiptCancelRequest,
  GoodsReceiptDetailResponse,
  GoodsReceiptListParams,
  GoodsReceiptListResponse,
  GoodsReceiptRejectRequest,
  GoodsReceiptSaveRequest,
  InventorySkuOption,
  PageResponse,
  PendingReceiptCountResponse,
} from "../types/goods-receipt.type";

const cleanParams = (params: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(params).filter(([, value]) =>
      value !== undefined && value !== null && value !== ""
    )
  );

const goodsReceiptService = {
  async getList(
    params: GoodsReceiptListParams
  ): Promise<PageResponse<GoodsReceiptListResponse>> {
    const response = await api.get("/admin/goods-receipts", {
      params: cleanParams({
        keyword: params.keyword?.trim() || undefined,
        status: params.status || undefined,
        receiptType: params.receiptType || "NORMAL_RECEIPT",
        createdBy: params.createdBy || undefined,
        fromDate: params.fromDate || undefined,
        toDate: params.toDate || undefined,
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return response.data;
  },

  async getDetail(id: number): Promise<GoodsReceiptDetailResponse> {
    const response = await api.get(`/admin/goods-receipts/${id}`);
    return response.data;
  },

  async create(
    request: GoodsReceiptSaveRequest
  ): Promise<GoodsReceiptDetailResponse> {
    const response = await api.post("/admin/goods-receipts", request);
    return response.data;
  },

  async update(
    id: number,
    request: GoodsReceiptSaveRequest
  ): Promise<GoodsReceiptDetailResponse> {
    const response = await api.put(
      `/admin/goods-receipts/${id}`,
      request
    );

    return response.data;
  },

  async submit(id: number): Promise<GoodsReceiptDetailResponse> {
    const response = await api.post(
      `/admin/goods-receipts/${id}/submit`
    );

    return response.data;
  },

  async approve(id: number): Promise<GoodsReceiptDetailResponse> {
    const response = await api.post(
      `/admin/goods-receipts/${id}/approve`
    );

    return response.data;
  },

  async reject(
    id: number,
    request: GoodsReceiptRejectRequest
  ): Promise<GoodsReceiptDetailResponse> {
    const response = await api.post(
      `/admin/goods-receipts/${id}/reject`,
      request
    );

    return response.data;
  },

  async cancel(
    id: number,
    request: GoodsReceiptCancelRequest
  ): Promise<GoodsReceiptDetailResponse> {
    const response = await api.post(
      `/admin/goods-receipts/${id}/cancel`,
      request
    );

    return response.data;
  },

  async getApprovalHistory(
    id: number
  ): Promise<GoodsReceiptApprovalHistoryResponse[]> {
    const response = await api.get(
      `/admin/goods-receipts/${id}/approval-history`
    );

    return response.data;
  },

  async getPendingCount(): Promise<PendingReceiptCountResponse> {
    const response = await api.get(
      "/admin/goods-receipts/pending-count"
    );

    return response.data;
  },

  // Tận dụng API Module 1 để search SKU, không tạo API giả ở FE.
  async searchSku(keyword = ""): Promise<InventorySkuOption[]> {
    const response = await api.get("/admin/inventory/overview", {
      params: {
        keyword: keyword.trim() || undefined,
        stockStatus: "ALL",
        page: 0,
        size: 20,
      },
    });

    const data = response.data;

    return Array.isArray(data?.content)
      ? data.content.map((item: any) => ({
          productVariantId: Number(item.productVariantId),
          sku: String(item.sku ?? ""),
          productName: String(item.productName ?? ""),

          // Hai field mới BE đã bổ sung cho việc nhận diện biến thể.
          capacityValue:
            item.capacityValue == null
              ? null
              : Number(item.capacityValue),

          bottleTypeName:
            item.bottleTypeName == null
              ? null
              : String(item.bottleTypeName),
        }))
      : [];
  },
};

export default goodsReceiptService;