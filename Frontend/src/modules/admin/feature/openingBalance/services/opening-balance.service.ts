import api from "@/common/api";
import goodsReceiptService from "@/modules/admin/feature/goodsReceipt/services/goods-receipt.service";

import type {
  GoodsReceiptDetailResponse,
  GoodsReceiptItemRequest,
  GoodsReceiptSaveRequest,
} from "@/modules/admin/feature/goodsReceipt/types/goods-receipt.type";

import type {
  InventorySkuOption,
  OpeningBalanceApprovalHistoryResponse,
  OpeningBalanceDetailResponse,
  OpeningBalanceListParams,
  OpeningBalanceListResponse,
  OpeningBalancePageResponse,
  OpeningBalanceRejectRequest,
  OpeningBalanceSaveRequest,
} from "../types/opening-balance.type";

const OPENING_BALANCE = "OPENING_BALANCE" as const;

const cleanParams = (params: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null && value !== ""
    )
  );

const contractError = (message: string) => {
  const error = new Error(message) as Error & {
    response?: { data: { message: string } };
  };

  error.response = {
    data: { message },
  };

  return error;
};

const assertOpeningBalance = <T extends { receiptType?: string | null }>(
  data: T
): T => {
  if (!data || data.receiptType !== OPENING_BALANCE) {
    throw contractError(
      "BE trả về dữ liệu không phải phiếu kiểm kho ban đầu (OPENING_BALANCE)."
    );
  }

  return data;
};

const assertOpeningBalancePage = (
  data: OpeningBalancePageResponse<OpeningBalanceListResponse>
) => {
  const content = Array.isArray(data?.content) ? data.content : [];

  const invalid = content.find(
    (item) => item?.receiptType !== OPENING_BALANCE
  );

  if (invalid) {
    throw contractError(
      `BE trả lẫn phiếu ${invalid.receiptNo || invalid.id} không phải OPENING_BALANCE.`
    );
  }

  return data;
};

const toGoodsReceiptPayload = (
  request: OpeningBalanceSaveRequest
): GoodsReceiptSaveRequest => ({
  receiptType: OPENING_BALANCE,
  note: request.note?.trim() || null,
  items: request.items.map(
    (item): GoodsReceiptItemRequest => ({
      productVariantId: Number(item.productVariantId),
      lotCode: item.lotCode.trim(),
      quantity: Number(item.quantity),
      // Module 4 không có nghiệp vụ đơn giá. BE dùng chung GoodsReceiptItem,
      // vì vậy luôn gửi null để contract khớp DTO hiện tại.
      unitCost: null,
      manufacturedDate: item.manufacturedDate || null,
      receivedDate: item.receivedDate,
      expirationDate: item.expirationDate,
      note: item.note?.trim() || null,
    })
  ),
});

const totalElementsOf = (
  data: OpeningBalancePageResponse<OpeningBalanceListResponse>
) => {
  const raw = data?.page?.totalElements ?? data?.totalElements;
  const parsed = Number(raw);

  return Number.isFinite(parsed)
    ? parsed
    : Array.isArray(data?.content)
      ? data.content.length
      : 0;
};

const openingBalanceService = {
  async getList(
    params: OpeningBalanceListParams
  ): Promise<OpeningBalancePageResponse<OpeningBalanceListResponse>> {
    const response = await api.get("/admin/goods-receipts", {
      params: cleanParams({
        keyword: params.keyword?.trim() || undefined,
        status: params.status || undefined,
        receiptType: OPENING_BALANCE,
        createdBy: params.createdBy ?? undefined,
        fromDate: params.fromDate || undefined,
        toDate: params.toDate || undefined,
        page: params.page ?? 0,
        size: params.size ?? 20,
      }),
    });

    return assertOpeningBalancePage(response.data);
  },

  async getDetail(id: number): Promise<OpeningBalanceDetailResponse> {
    const response = await api.get(`/admin/goods-receipts/${id}`);
    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async create(
    request: OpeningBalanceSaveRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(
      "/admin/goods-receipts",
      toGoodsReceiptPayload(request)
    );

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async update(
    id: number,
    request: OpeningBalanceSaveRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.put(
      `/admin/goods-receipts/${id}`,
      toGoodsReceiptPayload(request)
    );

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async submit(id: number): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/goods-receipts/${id}/submit`);
    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async approve(id: number): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/goods-receipts/${id}/approve`);
    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async reject(
    id: number,
    request: OpeningBalanceRejectRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/goods-receipts/${id}/reject`, {
      reason: request.reason.trim(),
    });

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async getApprovalHistory(
    id: number
  ): Promise<OpeningBalanceApprovalHistoryResponse[]> {
    const response = await api.get(
      `/admin/goods-receipts/${id}/approval-history`
    );

    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * KHÔNG gọi /admin/goods-receipts/pending-count vì BE hiện tại
   * đang đếm riêng NORMAL_RECEIPT.
   * Đếm Module 4 bằng chính list filter OPENING_BALANCE + PENDING_APPROVAL.
   */
  async getPendingCount(): Promise<number> {
    const data = await this.getList({
      status: "PENDING_APPROVAL",
      page: 0,
      size: 1,
    });

    return totalElementsOf(data);
  },

  /**
   * Tái sử dụng đúng API tìm SKU của Module 2 đang chạy ổn.
   */
  async searchSku(keyword: string): Promise<InventorySkuOption[]> {
    return goodsReceiptService.searchSku(keyword);
  },
};

export default openingBalanceService;
