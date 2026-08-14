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

export interface PageResponse<T> {
  content: T[];
  page: PageMeta;
}

export interface GoodsReceiptItemRequest {
  productVariantId: number;
  quantity: number;
  unitCost?: number | null;
  manufacturedDate?: string | null;
  expirationDate: string;
  note?: string | null;
}

export interface GoodsReceiptSaveRequest {
  receiptType: GoodsReceiptType;
  note?: string | null;
  items: GoodsReceiptItemRequest[];
}

export interface GoodsReceiptRejectRequest {
  reason: string;
}

export interface GoodsReceiptCancelRequest {
  reason?: string | null;
}

export interface GoodsReceiptItemResponse {
  id: number;
  productVariantId: number;
  sku: string;
  productName: string;

  // Metadata nhận diện biến thể.
  capacityValue: number | null;
  bottleTypeName: string | null;

  lotCode: string;
  quantity: number;
  unitCost: number | null;
  manufacturedDate: string | null;
  receivedDate: string;
  expirationDate: string;

  // Giữ để tương thích API/dữ liệu cũ, màn chi tiết không hiển thị.
  note: string | null;
}

export interface GoodsReceiptListResponse {
  id: number;
  receiptNo: string;
  receiptType: GoodsReceiptType;
  receiptTypeLabel: string;
  status: GoodsReceiptStatus;
  statusLabel: string;
  note: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
  submittedAt: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  cancelledAt: string | null;
  totalSku: number;
  totalQuantity: number;
}

export interface GoodsReceiptDetailResponse {
  id: number;
  receiptNo: string;
  receiptType: GoodsReceiptType;
  receiptTypeLabel: string;
  status: GoodsReceiptStatus;
  statusLabel: string;
  note: string | null;
  createdById: number;
  createdByName: string;
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
  cancelledById: number | null;
  cancelledByName: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  totalSku: number;
  totalQuantity: number;
  items: GoodsReceiptItemResponse[];
}

export interface GoodsReceiptApprovalHistoryResponse {
  id: number;
  fromStatus: GoodsReceiptStatus | null;
  fromStatusLabel: string | null;
  toStatus: GoodsReceiptStatus;
  toStatusLabel: string;
  actionById: number;
  actionByName: string;
  reason: string | null;
  actionAt: string;
}

export interface PendingReceiptCountResponse {
  pendingCount: number;
}

export interface GoodsReceiptListParams {
  keyword?: string;
  status?: GoodsReceiptStatus | "";
  receiptType?: GoodsReceiptType;
  createdBy?: number | null;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface InventorySkuOption {
  productVariantId: number;
  sku: string;
  productName: string;

  // Metadata chỉ dùng để nhận diện biến thể khi chọn SKU.
  // Để optional nhằm không ảnh hưởng các chỗ khác nếu có dùng interface này.
  capacityValue?: number | null;
  bottleTypeName?: string | null;
}