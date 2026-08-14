import api from "@/common/api";
import goodsReceiptService from "@/modules/admin/feature/goodsReceipt/services/goods-receipt.service";

import type {
  GoodsReceiptDetailResponse,
} from "@/modules/admin/feature/goodsReceipt/types/goods-receipt.type";

import type {
  InventorySkuOption,
  OpeningBalanceApprovalHistoryResponse,
  OpeningBalanceCancelRequest,
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
      "BE trả về dữ liệu không phải phiếu khởi tạo tồn đầu kỳ (OPENING_BALANCE)."
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

/**
 * Dedicated OpeningBalanceController nhận đúng OpeningBalanceSaveRequest.
 * Không gửi receiptType, unitCost và lotCode từ FE.
 * receiptType = OPENING_BALANCE do server quyết định.
 */
const toOpeningBalancePayload = (
  request: OpeningBalanceSaveRequest
): OpeningBalanceSaveRequest => ({
  note: request.note?.trim() || null,
  items: request.items.map((item) => ({
    productVariantId: Number(item.productVariantId),
    quantity: Number(item.quantity),
    manufacturedDate: item.manufacturedDate || null,
    receivedDate: item.receivedDate,
    expirationDate: item.expirationDate,
    note: item.note?.trim() || null,
  })),
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
    const response = await api.get("/admin/opening-balances", {
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

    return assertOpeningBalancePage(response.data);
  },

  async getDetail(id: number): Promise<OpeningBalanceDetailResponse> {
    const response = await api.get(`/admin/opening-balances/${id}`);
    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async create(
    request: OpeningBalanceSaveRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(
      "/admin/opening-balances",
      toOpeningBalancePayload(request)
    );

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async update(
    id: number,
    request: OpeningBalanceSaveRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.put(
      `/admin/opening-balances/${id}`,
      toOpeningBalancePayload(request)
    );

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async submit(id: number): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/opening-balances/${id}/submit`);
    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async cancel(
    id: number,
    request: OpeningBalanceCancelRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/opening-balances/${id}/cancel`, {
      reason: request.reason.trim(),
    });

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async approve(id: number): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/opening-balances/${id}/approve`);
    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async reject(
    id: number,
    request: OpeningBalanceRejectRequest
  ): Promise<OpeningBalanceDetailResponse> {
    const response = await api.post(`/admin/opening-balances/${id}/reject`, {
      reason: request.reason.trim(),
    });

    return assertOpeningBalance(response.data as GoodsReceiptDetailResponse);
  },

  async getApprovalHistory(
    id: number
  ): Promise<OpeningBalanceApprovalHistoryResponse[]> {
    const response = await api.get(
      `/admin/opening-balances/${id}/approval-history`
    );

    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * OpeningBalanceController hiện không có /pending-count.
   * Đếm bằng chính list dedicated + PENDING_APPROVAL.
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
   * Tái sử dụng API tìm SKU của Module 2.
   * Chỉ giới hạn số SKU hiển thị trong picker ở tối đa 100.
   * Khi có keyword, goodsReceiptService vẫn gọi BE để tìm trên toàn bộ dữ liệu.
   */
  async searchSku(keyword: string): Promise<InventorySkuOption[]> {
    const options = await goodsReceiptService.searchSku(keyword);
    return options.slice(0, 100);
  },
};

export default openingBalanceService;