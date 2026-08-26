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

  /**
   * Chỉ dùng cho API top sản phẩm bán chạy.
   */
  limit?: string;

  /**
   * Giữ để không làm ảnh hưởng store/state cũ.
   * Owner Report API hiện tại không gửi orderType lên BE.
   */
  orderType?: string;
}

export interface ReportSummaryResponse {
  filterType: ReportFilterType;
  fromDate: string;
  toDate: string;

  /**
   * Doanh thu thuần trong kỳ:
   * doanh thu bán - tiền hoàn sản phẩm đã hoàn tất.
   */
  totalRevenue: number;

  /**
   * Tổng số giao dịch bán đã hoàn thành trong kỳ.
   * Return sau đó không làm giảm chỉ số này.
   */
  totalOrders: number;

  /**
   * Gross sold quantity của các giao dịch bán hoàn thành trong kỳ.
   */
  totalProductsSold: number;

  /**
   * Doanh thu thuần ONLINE.
   */
  onlineRevenue: number;

  /**
   * Doanh thu thuần POS / IN_STORE.
   */
  offlineRevenue: number;

  onlineOrders: number;
  offlineOrders: number;
}

export interface RevenueChartResponse {
  label: string;

  /**
   * Doanh thu thuần tại bucket thời gian tương ứng.
   */
  revenue: number;

  /**
   * Số giao dịch bán hoàn thành tại bucket.
   * Refund không làm giảm số đơn.
   */
  totalOrders: number;
}

export interface BestSellingProductResponse {
  productId: number;

  productName: string;
  brandName: string;

  capacityName?: string;
  bottleTypeName?: string; // THÊM

  totalSold: number;
  revenue: number;

  imageUrl?: string | null;
}