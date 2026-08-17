import type {
  InventoryLotDetailResponse,
} from "@/modules/admin/feature/inventoryLot/types/inventory-lot.type";

export type ExpiryAlertGroup =
  | "ALL"
  | "NEAR_EXPIRY"
  | "EXPIRED";

export interface ExpiryAlertListResponse {
  id: number;

  productVariantId: number;

  sku: string;
  productName: string;
  imageUrl: string | null;
  lotCode: string;

  quantityOnHand: number;
  sellableQuantity: number;

  expirationDate: string;
  daysToExpiry: number;

  isNearExpiry: boolean;
  isExpired: boolean;
}

export interface ExpiryAlertSummaryResponse {
  warningDays: number;

  nearExpiryLotCount: number;
  nearExpiryQuantity: number;

  expiredLotCount: number;
  expiredQuantity: number;
}

export interface ExpiryAlertListParams {
  group?: ExpiryAlertGroup;

  keyword?: string;

  /**
   * Có thể âm.
   *
   * Ví dụ:
   * -30 -> đã hết hạn 30 ngày
   * -1  -> đã hết hạn 1 ngày
   * 0   -> hết hạn hôm nay
   * 7   -> còn 7 ngày
   */
  fromDays?: number | null;
  toDays?: number | null;

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
 * Project hiện tại có thể nhận Spring Page theo:
 *
 * {
 *   content: [],
 *   page: {
 *      size,
 *      number,
 *      totalElements,
 *      totalPages
 *   }
 * }
 *
 * Đồng thời vẫn support kiểu Page cũ để FE an toàn.
 */
export interface PageResponse<T> {
  content: T[];

  page?: PageMeta;

  size?: number;
  number?: number;
  totalElements?: number;
  totalPages?: number;
}

export type {
  InventoryLotDetailResponse,
};