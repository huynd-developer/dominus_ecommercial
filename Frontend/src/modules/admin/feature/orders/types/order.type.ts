export type OrderType = "ONLINE" | "IN_STORE";

export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

export interface VoucherInfo {
  voucherId: number;
  voucherCode: string;
  voucherName: string;
}

export interface AdminOrderItemResponse {
  orderItemId: number;
  productVariantId: number;
  sku: string;
  productName: string;
  capacity: string;
  bottleType: string;
  quantity: number;
  originalPrice: number;
  discountAmount: number;
  finalPrice: number;
  lineTotal: number;
  note?: string | null;
  imageUrl?: string | null;
}

export interface AdminOrderResponse {
  orderId: number;
  orderCode: string;
  orderType: OrderType | string;

  customerId?: number | null;
  customerName?: string | null;
  customerPhone?: string | null;
  shippingAddress?: string | null;

  cashierId?: number | null;
  voucher?: VoucherInfo | null;

  totalAmount: number;
  discountAmount: number;
  finalAmount: number;

  paymentMethod: string;
  status: OrderStatus;
  statusText: string;

  createdAt?: string | null;
  completedAt?: string | null;

  totalQuantity?: number | null;
  items: AdminOrderItemResponse[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

export interface OrderSearchParams {
  keyword?: string;
  status?: number | null;
  orderType?: string | null;
  page?: number;
  size?: number;
}

export interface UpdateOrderStatusResponse {
  message: string;
  orderId: number;
  status: number;
  loyaltyPointsApplied: boolean;
  loyaltyPointsEarned: number;
}