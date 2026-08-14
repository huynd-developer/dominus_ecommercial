export type StockMovementType =
  | "RECEIPT_IN"
  | "OPENING_IN"
  | "SALE_OUT"
  | "RETURN_IN"
  | "ADJUST_IN"
  | "ADJUST_OUT"
  | "DISPOSAL_OUT";

export interface StockMovementListResponse {
  id: number;
  createdAt: string;

  inventoryLotId: number;
  productVariantId: number;

  sku: string;
  productName: string;
  lotCode: string;

  movementType: StockMovementType | null;
  movementTypeLabel: string | null;

  quantityChange: number;
  quantityBefore: number;
  quantityAfter: number;

  referenceType: string | null;
  referenceId: number | null;
  referenceLineId: number | null;

  reason: string | null;

  createdById: number;
  createdByName: string;
}

export interface StockMovementDetailResponse
  extends StockMovementListResponse {}

export interface StockMovementListParams {
  keyword?: string;
  inventoryLotId?: number | null;
  movementType?: StockMovementType | "";
  createdBy?: number | null;
  referenceType?: string;
  referenceId?: number | null;
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

export interface PageResponse<T> {
  content: T[];
  page?: PageMeta;
  size?: number;
  number?: number;
  totalElements?: number;
  totalPages?: number;
}

export const STOCK_MOVEMENT_TYPE_OPTIONS: Array<{
  value: StockMovementType;
  label: string;
}> = [
  { value: "RECEIPT_IN", label: "Nhập kho" },
  { value: "OPENING_IN", label: "Tồn đầu kỳ" },
  { value: "SALE_OUT", label: "Xuất bán" },
  { value: "RETURN_IN", label: "Nhập trả hàng" },
  { value: "ADJUST_IN", label: "Điều chỉnh tăng" },
  { value: "ADJUST_OUT", label: "Điều chỉnh giảm" },
  { value: "DISPOSAL_OUT", label: "Xuất hủy" },
];

export const REFERENCE_TYPE_OPTIONS = [
  { value: "GOODS_RECEIPT", label: "Phiếu nhập kho" },
  { value: "ORDER", label: "Đơn hàng" },
  { value: "RETURN", label: "Phiếu trả hàng" },
  { value: "STOCK_ADJUSTMENT", label: "Điều chỉnh kho" },
];
