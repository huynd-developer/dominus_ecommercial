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
}

export interface InventoryOverview {
  productVariantId: number;
  sku: string;
  productName: string;
  imageUrl: string | null;

  capacityValue: number | null;
  bottleTypeName: string | null;

  totalQuantity: number;
  sellableQuantity: number;
  nearExpiryQuantity: number;
  expiredQuantity: number;
}

export interface InventoryLotStatus {
  inventoryLotId: number;
  productVariantId: number;

  sku: string;
  productName: string;
  imageUrl: string | null;
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
}

export interface InventoryConfig {
  id: number;
  expiryWarningDays: number;
}

export interface InventoryConfigUpdateRequest {
  expiryWarningDays: number;

  /**
   * Snapshot cấu hình FE đang nhìn thấy trước khi bấm lưu.
   * Khớp BE InventoryConfigUpdateRequest.expectedExpiryWarningDays.
   *
   * Optional để không làm vỡ các caller cũ nếu còn tồn tại.
   */
  expectedExpiryWarningDays?: number;
}

export interface InventoryOverviewParams {
  keyword?: string;
  nearExpiry?: boolean;
  expired?: boolean;
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