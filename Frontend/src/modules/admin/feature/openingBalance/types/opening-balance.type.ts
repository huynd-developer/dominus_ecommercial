import type {
  GoodsReceiptApprovalHistoryResponse,
  GoodsReceiptDetailResponse,
  GoodsReceiptListResponse,
  GoodsReceiptStatus,
  InventorySkuOption,
} from "@/modules/admin/feature/goodsReceipt/types/goods-receipt.type";

/**
 * Opening Balance dùng dedicated API /admin/opening-balances nhưng response
 * vẫn là GoodsReceipt response của BE với receiptType = OPENING_BALANCE.
 */
export type OpeningBalanceStatus = GoodsReceiptStatus;
export type OpeningBalanceListResponse = GoodsReceiptListResponse;
export type OpeningBalanceDetailResponse = GoodsReceiptDetailResponse;
export type OpeningBalanceApprovalHistoryResponse =
  GoodsReceiptApprovalHistoryResponse;
export type { InventorySkuOption };

export interface OpeningBalanceItemRequest {
  productVariantId: number;
  lotCode: string;
  quantity: number;
  manufacturedDate: string | null;
  receivedDate: string;
  expirationDate: string;
  note: string | null;
}

/**
 * Khớp OpeningBalanceSaveRequest của BE.
 * FE không gửi receiptType và unitCost.
 */
export interface OpeningBalanceSaveRequest {
  note: string | null;
  items: OpeningBalanceItemRequest[];
}

export interface OpeningBalanceRejectRequest {
  reason: string;
}

/**
 * Lý do hủy được FE bắt buộc chọn trước khi gọi BE.
 * BE GoodsReceiptCancelRequest vẫn nhận reason dạng chuỗi.
 */
export interface OpeningBalanceCancelRequest {
  reason: string;
}

export interface OpeningBalanceListParams {
  keyword?: string;
  status?: OpeningBalanceStatus | "";
  createdBy?: number | null;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface PageMeta {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Hỗ trợ cả hai format Page:
 * - { content, page: { size, number, totalElements, totalPages } }
 * - { content, size, number, totalElements, totalPages }
 */
export interface OpeningBalancePageResponse<T> {
  content: T[];
  page?: PageMeta;
  size?: number;
  number?: number;
  totalElements?: number;
  totalPages?: number;
}
