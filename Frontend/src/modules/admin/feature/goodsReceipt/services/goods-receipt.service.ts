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

const mapInventorySkuOption = (item: any): InventorySkuOption => ({
  productVariantId: Number(item.productVariantId),
  sku: String(item.sku ?? ""),
  productName: String(item.productName ?? ""),
  imageUrl:
    item.imageUrl == null
      ? null
      : String(item.imageUrl),

  // Metadata nhận diện biến thể.
  capacityValue:
    item.capacityValue == null
      ? null
      : Number(item.capacityValue),

  bottleTypeName:
    item.bottleTypeName == null
      ? null
      : String(item.bottleTypeName),

  // Dùng riêng cho việc ưu tiên SKU cần nhập thêm.
  totalQuantity: Number(item.totalQuantity ?? 0),
  sellableQuantity: Number(item.sellableQuantity ?? 0),
});

const compareLowStockFirst = (
  a: InventorySkuOption,
  b: InventorySkuOption
) => {
  const sellableA = Number(a.sellableQuantity ?? 0);
  const sellableB = Number(b.sellableQuantity ?? 0);

  if (sellableA !== sellableB) {
    return sellableA - sellableB;
  }

  const totalA = Number(a.totalQuantity ?? 0);
  const totalB = Number(b.totalQuantity ?? 0);

  if (totalA !== totalB) {
    return totalA - totalB;
  }

  const productCompare = a.productName.localeCompare(
    b.productName,
    "vi"
  );

  if (productCompare !== 0) {
    return productCompare;
  }

  return a.sku.localeCompare(b.sku, "vi");
};

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
  // Chỉ riêng màn chọn SKU của phiếu nhập:
  // - lấy đủ các trang kết quả phù hợp
  // - ưu tiên SKU có số lượng có thể bán thấp hơn lên trước
  // Không thay đổi thứ tự của màn Tổng quan kho.
  async searchSku(keyword = ""): Promise<InventorySkuOption[]> {
    const normalizedKeyword = keyword.trim();
    const pageSize = 100;

    const getPage = async (page: number) => {
      const response = await api.get("/admin/inventory/overview", {
        params: {
          keyword: normalizedKeyword || undefined,
          stockStatus: "ALL",
          page,
          size: pageSize,
        },
      });

      return response.data;
    };

    const firstPage = await getPage(0);

    const firstContent = Array.isArray(firstPage?.content)
      ? firstPage.content
      : [];

    const totalPagesRaw =
      firstPage?.page?.totalPages ??
      firstPage?.totalPages ??
      (firstContent.length > 0 ? 1 : 0);

    const totalPages = Math.max(
      0,
      Number.isFinite(Number(totalPagesRaw))
        ? Number(totalPagesRaw)
        : 0
    );

    const allItems: any[] = [...firstContent];

    // API inventory đang giới hạn page size, nên lấy tiếp các trang còn lại
    // để việc ưu tiên "sắp hết hàng" đúng trên toàn bộ kết quả tìm kiếm,
    // không chỉ đúng trong 20/100 SKU đầu tiên.
    for (let page = 1; page < totalPages; page++) {
      const data = await getPage(page);

      if (Array.isArray(data?.content)) {
        allItems.push(...data.content);
      }
    }

    const uniqueOptions = new Map<number, InventorySkuOption>();

    allItems.forEach((item) => {
      const option = mapInventorySkuOption(item);

      if (
        Number.isInteger(option.productVariantId) &&
        option.productVariantId > 0
      ) {
        uniqueOptions.set(option.productVariantId, option);
      }
    });

    return Array.from(uniqueOptions.values()).sort(
      compareLowStockFirst
    );
  },
};

export default goodsReceiptService;