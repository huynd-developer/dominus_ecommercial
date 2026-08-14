export type StockAdjustmentStatus =
  | "DRAFT"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED";

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
}

export interface StockAdjustmentRejectRequest {
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
}

export interface StockAdjustmentItemResponse {
  id: number;
  inventoryLotId: number;
  productVariantId: number | null;
  sku: string | null;
  productName: string | null;
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

  items: StockAdjustmentItemResponse[];
}
