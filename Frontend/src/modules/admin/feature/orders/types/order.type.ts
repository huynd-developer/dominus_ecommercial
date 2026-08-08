export type OrderType = "ONLINE" | "IN_STORE";

// BỔ SUNG SỐ 8 VÀO ORDER STATUS
export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8; 

export type ReturnProcessStatus = 0 | 1 | 2 | 3;

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

export interface AdminReturnItemResponse {
  returnRequestItemId?: number | null;
  orderItemId?: number | null;
  productVariantId?: number | null;
  productId?: number | null;

  productName?: string | null;
  brandName?: string | null;
  sku?: string | null;
  capacity?: string | null;
  bottleType?: string | null;
  imageUrl?: string | null;

  orderedQuantity?: number | null;
  returnQuantity?: number | null;

  /** Giá gốc trên 1 sản phẩm tại thời điểm đặt hàng. */
  unitOriginalPrice?: number | null;

  /** Số tiền giảm trên 1 sản phẩm tại thời điểm đặt hàng. */
  unitDiscountAmount?: number | null;

  /** Giá cuối trên 1 sản phẩm tại thời điểm đặt hàng. */
  unitFinalPrice?: number | null;

  /** Tổng giá gốc của số lượng hoàn. */
  itemOriginalAmount?: number | null;

  /** Tổng giảm giá sản phẩm của số lượng hoàn. */
  itemDiscountAmount?: number | null;

  /** Tổng tiền hàng sau giảm sản phẩm, trước phân bổ voucher. */
  itemAmount?: number | null;

  /** Phần voucher được phân bổ vào sản phẩm hoàn. */
  voucherAllocatedAmount?: number | null;

  /** Số tiền thực tế cần hoàn cho dòng này. */
  refundAmount?: number | null;

  /**
   * 0 = Chờ xử lý
   * 1 = Đã chấp nhận
   * 2 = Từ chối
   * 3 = Đã hoàn tiền
   */
  status?: ReturnProcessStatus | number | null;
  statusText?: string | null;

  /** Lý do admin từ chối sản phẩm hoàn. */
  rejectReason?: string | null;
  rejectedReason?: string | null;
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

  /** Phí vận chuyển của đơn hàng. */
  shippingFee?: number | null;

  /** Phí vận chuyển được tính vào tiền hoàn, nếu có. */
  returnShippingFee?: number | null;

  paymentMethod: string;
  status: OrderStatus;
  statusText: string;

  createdAt?: string | null;
  completedAt?: string | null;

  /** Người xác nhận giao hàng thành công trong hệ thống. */
  deliveryCompletedByName?: string | null;

  /** Thông tin giao hàng thất bại. */
  deliveryFailedReason?: string | null;
  deliveryFailedDescription?: string | null;
  deliveryFailedAt?: string | null;
  deliveryFailedByName?: string | null;

  /**
   * Thông tin hoàn tiền riêng cho đơn giao hàng thất bại đã thanh toán trước.
   * Không có deliveryRefundStatus; FE suy ra từ amount/bank/refundedAt.
   */
  deliveryRefundAmount?: number | null;
  deliveryRefundBankName?: string | null;
  deliveryRefundBankAccountNumber?: string | null;
  deliveryRefundBankAccountHolder?: string | null;
  deliveryRefundedAt?: string | null;
  deliveryRefundedByName?: string | null;
  deliveryRefundRequired?: boolean | null;
  deliveryRefundBankInfoProvided?: boolean | null;
  deliveryRefundCompleted?: boolean | null;
  canMarkDeliveryRefunded?: boolean | null;

  /** Ảnh minh chứng giao hàng. */
  deliverySuccessMediaUrls?: string[] | null;
  deliveryFailedMediaUrls?: string[] | null;

  /** Lý do hủy đơn. Có thể do khách chọn hoặc admin nhập/chọn khi hủy. */
  cancelReason?: string | null;
  cancellationReason?: string | null;
  cancelNote?: string | null;
  cancelDescription?: string | null;

  /** Thời điểm hủy đơn. BE hiện dùng cancelledAt, FE hỗ trợ thêm canceledAt để tương thích. */
  cancelledAt?: string | null;
  canceledAt?: string | null;

  totalQuantity?: number | null;
  isPaymentReported?: boolean | null;

  returnType?: string | number | null;
  returnReason?: string | null;
  returnDescription?: string | null;
  returnEmail?: string | null;
  returnRequestedAt?: string | null;

  refundMethod?: string | number | null;
  returnRefundAmount?: number | null;
  refundAmount?: number | null;

  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;

  /** Trạng thái tổng của yêu cầu hoàn: 0 chờ xử lý, 1 đã chấp nhận, 2 từ chối, 3 đã hoàn tiền. */
  returnProcessStatus?: ReturnProcessStatus | number | string | null;
  returnProcessStatusText?: string | null;

  /** Lý do từ chối yêu cầu hoàn, dùng để admin/khách nhìn được vì sao bị từ chối. */
  returnRejectReason?: string | null;
  rejectReason?: string | null;
  rejectedReason?: string | null;

  canAcceptReturn?: boolean | null;
  canRejectReturn?: boolean | null;
  canMarkReturnRefunded?: boolean | null;

  returnImages?: string[] | null;
  returnVideos?: string[] | null;
  returnMediaUrls?: string[] | null;
  returnItems?: AdminReturnItemResponse[] | null;

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
  loyaltyPointsApplied?: boolean;
  loyaltyPointsEarned?: number;
}

export interface AdminCancelOrderRequest {
  reason: string;
  description?: string | null;
}

export interface MarkDeliveryCompletedRequest {
  files: File[];
}

export interface MarkDeliveryFailedRequest {
  reason: string;
  description?: string | null;
  files?: File[];
}

export interface RejectReturnRequest {
  reason: string;
}