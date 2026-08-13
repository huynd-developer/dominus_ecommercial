import type {
  GoodsReceiptApprovalHistoryResponse,
  GoodsReceiptDetailResponse,
  GoodsReceiptListResponse,
  GoodsReceiptStatus,
  InventorySkuOption,
} from "@/modules/admin/feature/goodsReceipt/types/goods-receipt.type";

/**
 * Module 4 hiện tại dùng chung GoodsReceipt của BE với:
 * receiptType = OPENING_BALANCE.
 *
 * Response dùng nguyên shape GoodsReceipt để không tạo contract FE giả
 * khác với BE đang chạy.
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
 * Không expose receiptType cho component.
 * Service sẽ luôn ép OPENING_BALANCE và unitCost = null trước khi gọi BE.
 */
export interface OpeningBalanceSaveRequest {
  note: string | null;
  items: OpeningBalanceItemRequest[];
}

export interface OpeningBalanceRejectRequest {
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
 * Hỗ trợ cả hai format Page mà project đã từng nhận:
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
