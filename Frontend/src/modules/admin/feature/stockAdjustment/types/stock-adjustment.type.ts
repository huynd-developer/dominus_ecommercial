export type StockAdjustmentStatus =
  | "DRAFT"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";

export interface PageMeta {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface PageResponse<T> {
  content: T[];
  page?: PageMeta;
  size?: number;
  number?: number;
  totalElements?: number;
  totalPages?: number;
}

export interface StockAdjustmentItemRequest {
  inventoryLotId: number;
  actualQuantity: number;
  reason: string | null;
}

export interface StockAdjustmentSaveRequest {
  note: string | null;
  items: StockAdjustmentItemRequest[];

  /**
   * Chỉ gửi khi UPDATE DRAFT.
   * CREATE không cần field này.
   */
  expectedRevision?: string | null;
}

export interface StockAdjustmentRejectRequest {
  reason: string;
}

export interface StockAdjustmentCancelRequest {
  reason: string;
}

export interface StockAdjustmentListParams {
  keyword?: string;
  status?: StockAdjustmentStatus | "";
  createdBy?: number | null;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface StockAdjustmentSummary {
  totalLots: number;
  matchedLots: number;
  mismatchLots: number;
  increasedLots: number;
  decreasedLots: number;
  totalIncrease: number;
  totalDecrease: number;
}

export interface StockAdjustmentListResponse extends StockAdjustmentSummary {
  id: number;
  adjustmentNo: string;
  status: StockAdjustmentStatus;
  statusLabel: string;
  createdById: number;
  createdByName: string | null;
  createdAt: string;
  submittedAt: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  cancelledAt: string | null;
}

export interface StockAdjustmentItemResponse {
  id: number;
  inventoryLotId: number;
  productVariantId: number | null;
  sku: string | null;
  productName: string | null;
  imageUrl: string | null;
  capacityValue: number | null;
  bottleTypeName: string | null;
  lotCode: string | null;
  systemQuantity: number;
  actualQuantity: number;
  quantityDifference: number;
  currentQuantity: number | null;
  resultLabel: string;
  reason: string | null;
}

export interface StockAdjustmentDetailResponse extends StockAdjustmentSummary {
  id: number;
  adjustmentNo: string;
  status: StockAdjustmentStatus;
  statusLabel: string;

  /**
   * Revision do BE trả về để chống stale/lost-update khi sửa DRAFT.
   */
  revision: string;

  note: string | null;

  createdById: number;
  createdByName: string | null;
  createdAt: string;

  submittedById: number | null;
  submittedByName: string | null;
  submittedAt: string | null;

  approvedById: number | null;
  approvedByName: string | null;
  approvedAt: string | null;

  rejectedById: number | null;
  rejectedByName: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;

  cancelledById: number | null;
  cancelledByName: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;

  items: StockAdjustmentItemResponse[];
}