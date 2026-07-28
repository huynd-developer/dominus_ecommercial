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
  ImageUrl?: string | null;

  productImageUrl?: string | null;
  ProductImageUrl?: string | null;

  variantImageUrl?: string | null;
  VariantImageUrl?: string | null;

  thumbnailUrl?: string | null;
  ThumbnailUrl?: string | null;

  mainImageUrl?: string | null;
  MainImageUrl?: string | null;

  image?: string | null;
  Image?: string | null;

  productImage?: any;
  ProductImage?: any;

  images?: any[];
  Images?: any[];

  productImages?: any[];
  ProductImages?: any[];

  imageList?: any[];
  ImageList?: any[];
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

  fromDate?: string | null;
  toDate?: string | null;

  page?: number;
  size?: number;
}

export interface OrderStatusCountParams {
  keyword?: string | null;
  orderType?: string | null;

  fromDate?: string | null;
  toDate?: string | null;
}

export interface AdminOrderStatusCountResponse {
  total: number;

  pending: number;
  confirmed: number;
  shipping: number;
  completed: number;
  cancelled: number;
  deliveryFailed: number;
  returnRequested: number;
  returnCompleted: number;
}

export interface UpdateOrderStatusResponse {
  message: string;
  orderId: number;
  status: number;
  loyaltyPointsApplied: boolean;
  loyaltyPointsEarned: number;
}