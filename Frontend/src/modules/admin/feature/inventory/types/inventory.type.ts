export type InventoryStockStatus =
  | "ALL"
  | "IN_STOCK"
  | "OUT_OF_STOCK";

export interface InventorySummary {
  totalSku: number;
  inStockSku: number;
  outOfStockSku: number;
  totalQuantity: number;
  sellableQuantity: number;
  nearExpiryQuantity: number;
  expiredQuantity: number;
  lockedQuantity: number;
}

export interface InventoryOverview {
  productVariantId: number;
  sku: string;
  productName: string;
  totalQuantity: number;
  sellableQuantity: number;
  nearExpiryQuantity: number;
  expiredQuantity: number;
  lockedQuantity: number;
}

export interface InventoryLotStatus {
  inventoryLotId: number;
  productVariantId: number;

  sku: string;
  productName: string;
  lotCode: string;

  manufacturedDate: string | null;
  receivedDate: string;
  expirationDate: string;

  initialQuantity: number;
  quantityOnHand: number;
  sellableQuantity: number;

  daysToExpiry: number;

  nearExpiry: boolean;
  expired: boolean;
  locked: boolean;

  lockReason: string | null;
}

export interface InventoryConfig {
  id: number;
  expiryWarningDays: number;
}

export interface InventoryConfigUpdateRequest {
  expiryWarningDays: number;
}

export interface InventoryOverviewParams {
  keyword?: string;
  nearExpiry?: boolean;
  expired?: boolean;
  locked?: boolean;
  stockStatus?: InventoryStockStatus;
  page?: number;
  size?: number;
}

export interface InventoryLotParams {
  keyword?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];

  totalElements: number;
  totalPages: number;

  size: number;
  number: number;

  first: boolean;
  last: boolean;
  empty: boolean;

  numberOfElements: number;
}