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
<<<<<<< HEAD
  // Bổ sung các trường bóc tách Online / Tại quầy
=======
>>>>>>> 9d167f0ed4c026eca4c2ba188b0c8fa4199145de
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