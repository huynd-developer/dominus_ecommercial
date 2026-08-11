export type ReportFilterType =
  | "DAY"
  | "WEEK"
  | "MONTH"
  | "QUARTER"
  | "YEAR"
  | "CUSTOM";

export interface ReportFilterParams {
  filterType: ReportFilterType;
  fromDate?: string;
  toDate?: string;
  limit?: string;
  orderType?: string; // Bổ sung lọc theo loại đơn
}

export interface ReportSummaryResponse {
  filterType: ReportFilterType;
  fromDate: string;
  toDate: string;
  totalRevenue: number;
  totalOrders: number;
  totalProductsSold: number;
  onlineRevenue: number;
  offlineRevenue: number;
  onlineOrders: number;
  offlineOrders: number;
}

export interface RevenueChartResponse {
  label: string;
  revenue: number;
  totalOrders: number;
}

export interface BestSellingProductResponse {
  productId: number;
  productName: string;
  brandName: string;
  totalSold: number;
  revenue: number;
  imageUrl?: string | null;
}