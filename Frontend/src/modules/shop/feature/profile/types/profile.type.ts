export type GenderValue = 0 | 1 | 2 | null;

export interface CustomerProfileResponse {
  userId: number;
  name: string;
  email: string;
  phone: string;
  address: string;
  avatarUrl: string | null;
  status: number;
  createdAt: string;

  customerRank: string;
  loyaltyPoints: number;
  dateOfBirth: string | null;
  gender: GenderValue;
}

export interface UpdateCustomerProfileRequest {
  name: string;
  phone: string;
  address: string;
  dateOfBirth?: string | null;
  gender?: GenderValue;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface AddFavoriteRequest {
  productVariantId: number;
}

export interface FavoriteResponse {
  favoriteId: number;
  productVariantId: number;
  productId: number;
  productName: string;
  brandName: string;
  sku: string;
  price: number;
  stockQuantity: number;
  capacityValue: number | null;
  bottleTypeName: string | null;
  imageUrl: string | null;
  createdAt: string;
}

export interface CustomerOrderItemResponse {
  orderItemId: number;

  productVariantId: number | null;
  productId: number | null;

  productName: string | null;
  brandName: string | null;
  sku: string | null;

  capacity: string | null;
  bottleType: string | null;

  manufacturingDate: string | null;
  expirationDate: string | null;

  quantity: number;

  /**
   * Giá gốc tại thời điểm đặt hàng.
   */
  originalPrice: number;

  /**
   * Số tiền giảm trên 1 sản phẩm tại thời điểm đặt hàng.
   */
  discountAmount: number;

  /**
   * Giá cuối cùng trên 1 sản phẩm tại thời điểm đặt hàng.
   */
  finalPrice: number;

  /**
   * Thành tiền dòng = finalPrice * quantity.
   */
  lineTotal: number;

  note: string | null;
  image: string | null;
}

export interface CustomerReturnItemResponse {
  orderItemId: number | null;
  productId: number | null;
  productVariantId: number | null;

  productName: string | null;
  brandName: string | null;
  sku: string | null;
  image: string | null;

  capacity: string | null;
  capacityName?: string | null;
  bottleType: string | null;
  bottleTypeName?: string | null;

  orderedQuantity: number;
  returnQuantity: number;

  unitFinalPrice: number;
  itemAmount: number;
  voucherAllocatedAmount: number;
  refundAmount: number;
}

export interface CustomerOrderResponse {
  orderId: number;

  orderType: string | null;

  customerName: string | null;
  customerPhone: string | null;
  shippingAddress: string | null;

  totalAmount: number;
  discountAmount: number;
  finalAmount: number;

  paymentMethod: string | null;

  /**
   * 0 = Chờ xác nhận
   * 1 = Đã xác nhận
   * 2 = Đang giao hàng
   * 3 = Hoàn thành
   * 4 = Đã hủy
   * 5 = Giao hàng thất bại
   * 6 = Yêu cầu hoàn hàng / đổi trả
   * 7 = Hoàn hàng / đổi trả hoàn tất
   */
  status: number;

  statusText: string;
  canCancel: boolean;

  createdAt: string;
  completedAt?: string | null;
  updatedAt?: string | null;

  /**
   * BE có thể trả sẵn, FE vẫn tự fallback theo completedAt + 15 ngày nếu thiếu.
   */
  canReturn?: boolean | null;
  returnDeadline?: string | null;

  cancelReason?: string | null;
  cancelledAt?: string | null;
  canceledAt?: string | null;

  returnType?: ReturnType | string | null;
  returnReason?: string | null;
  returnDescription?: string | null;
  returnEmail?: string | null;
  returnRequestedAt?: string | null;
  returnRefundAmount?: number | null;
  refundAmount?: number | null;
  estimatedRefundAmount?: number | null;
  returnEstimatedRefundAmount?: number | null;
  refundMethod?: RefundMethod | string | null;
  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;
  returnImages?: string[] | null;
  returnVideos?: string[] | null;
  returnMediaUrls?: string[] | null;
  returnItems?: CustomerReturnItemResponse[] | null;

  items: CustomerOrderItemResponse[];
}

export interface CreateReviewRequest {
  orderItemId: number;
  rating: number;
  comment?: string | null;
  mediaFiles?: File[];
  files?: File[];
}

export interface UpdateReviewRequest {
  rating: number;
  comment?: string | null;
  mediaFiles?: File[];
  files?: File[];
}

export interface ReviewMediaResponse {
  url?: string | null;
  mediaUrl?: string | null;
  mediaType?: string | null;
  isVideo?: boolean | null;
}

export interface ReviewResponse {
  reviewId: number;
  orderItemId: number;
  orderId: number;

  productVariantId: number | null;
  productId: number | null;
  productName: string | null;
  brandName: string | null;
  sku: string | null;
  image: string | null;

  rating: number;
  comment: string | null;
  createdAt: string;

  /**
   * Sửa đánh giá tối đa 1 lần trong 30 ngày.
   * Nếu BE chưa trả các field này, FE sẽ tự fallback theo createdAt.
   */
  editedAt?: string | null;
  editCount?: number | null;
  canEdit?: boolean | null;
  editDeadline?: string | null;
  editMessage?: string | null;

  /**
   * 0 = Đang chờ duyệt ảnh/video
   * 1 = Đã hiển thị
   * 2 = Đánh giá không được duyệt
   * 3 = Đánh giá đã bị ẩn
   */
  approvalStatus?: number | string | null;
  approvalStatusText?: string | null;
  approvedAt?: string | null;
  rejectedAt?: string | null;
  rejectedReason?: string | null;

  mediaUrls?: string[] | null;
  mediaFiles?: Array<string | ReviewMediaResponse> | null;
  reviewMediaUrls?: string[] | null;
  reviewMediaFiles?: Array<string | ReviewMediaResponse> | null;
  images?: Array<string | ReviewMediaResponse> | null;
  files?: Array<string | ReviewMediaResponse> | null;
}

export interface ReviewableOrderItemResponse {
  orderItemId: number;
  orderId: number;

  productVariantId: number | null;
  productId: number | null;
  productName: string | null;
  brandName: string | null;
  sku: string | null;
  image: string | null;

  orderStatus: number;
  reviewed: boolean;
  canReview: boolean;
  message: string;
  reviewDeadline?: string | null;
}

export type ReturnType =
  | "RECEIVED_WITH_PROBLEM"
  | "NOT_RECEIVED_OR_MISSING";

export type RefundMethod = "BANK_TRANSFER" | "STORE";

export interface ReturnRequestItemPayload {
  orderItemId: number;
  quantity: number;
}

export interface ReturnRequestSubmitPayload {
  orderId: number;
  returnType: ReturnType;
  reason: string;
  description: string;
  email: string;
  refundMethod: RefundMethod;
  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;
  returnItems: ReturnRequestItemPayload[];
  files: File[];
}