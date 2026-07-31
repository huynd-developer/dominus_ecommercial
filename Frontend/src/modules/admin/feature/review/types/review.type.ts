export interface ReviewMedia {
  id: number;
  mediaUrl: string;
  mediaType: string;
}

export interface Review {
  id: number;
  productName: string;
  productSku: string;
  customerName: string;
  customerEmail: string;
  rating: number;
  comment: string;
  approvalStatus: number; // 0: Chờ duyệt, 1: Đã duyệt, 2: Từ chối, 3: Đã ẩn
  createdAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectedReason: string | null;
  media: ReviewMedia[];
}