export type GoodsReceiptStatus =
  | "DRAFT"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";

export type GoodsReceiptType = "NORMAL_RECEIPT" | "OPENING_BALANCE";

export interface PageMeta {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Hỗ trợ cả 2 kiểu JSON Page mà Spring có thể trả:
 * - { content, page: { size, number, totalElements, totalPages } }
 * - { content, size, number, totalElements, totalPages }
 */
export interface PageResponse<T> {
  content: T[];
  page?: PageMeta;
  size?: number;
  number?: number;
  totalElements?: number;
  totalPages?: number;
}

export interface InventoryLotListResponse {
  id: number;
  productVariantId: number;
  sku: string;
  productName: string;
  lotCode: string;
  receivedDate: string;
  expirationDate: string;
  daysToExpiry: number;
  initialQuantity: number;
  quantityOnHand: number;
  sellableQuantity: number;
  isNearExpiry: boolean;
  isExpired: boolean;
  goodsReceiptId: number | null;
  receiptNo: string | null;
}

export interface InventoryLotDetailResponse {
  id: number;

  productVariantId: number;
  sku: string;
  productName: string;
  lotCode: string;

  manufacturedDate: string | null;
  receivedDate: string;
  expirationDate: string;
  daysToExpiry: number;

  initialQuantity: number;
  quantityOnHand: number;
  sellableQuantity: number;
  unitCost: number | null;

  isNearExpiry: boolean;
  isExpired: boolean;

  createdById: number;
  createdByName: string;
  createdAt: string;

  goodsReceiptItemId: number | null;
  goodsReceiptId: number | null;
  receiptNo: string | null;
  receiptType: GoodsReceiptType | null;
  receiptTypeLabel: string | null;
  receiptStatus: GoodsReceiptStatus | null;
  receiptStatusLabel: string | null;
}

export interface InventoryLotSourceResponse {
  inventoryLotId: number;
  goodsReceiptItemId: number | null;
  goodsReceiptId: number | null;
  receiptNo: string | null;
  receiptType: GoodsReceiptType | null;
  receiptTypeLabel: string | null;
  receiptStatus: GoodsReceiptStatus | null;
  receiptStatusLabel: string | null;
}

export interface InventoryLotListParams {
  keyword?: string;
  productVariantId?: number | null;
  isExpired?: boolean;
  isNearExpiry?: boolean;
  hasStock?: boolean;
  expirationFrom?: string;
  expirationTo?: string;
  page?: number;
  size?: number;
}

export type InventoryLotExpiryFilter =
  | ""
  | "NEAR_EXPIRY"
  | "EXPIRED"
  | "NOT_EXPIRED";

export type InventoryLotStockFilter = "" | "IN_STOCK" | "OUT_OF_STOCK";