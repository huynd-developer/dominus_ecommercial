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
  /**
   * Giữ nguyên contract FE hiện tại để không lan TypeScript sang caller khác.
   * Component đã tự fallback an toàn nếu runtime nhận null từ dữ liệu legacy.
   */
  productId: number;

  productName: string;
  brandName: string;

  /**
   * Gross sold quantity trong kỳ bán.
   */
  totalSold: number;

  /**
   * Doanh thu bán của sản phẩm sau giảm giá trên dòng và phần voucher
   * toàn đơn được phân bổ theo tỷ lệ. Không trừ refund phát sinh sau bán.
   */
  revenue: number;

  imageUrl?: string | null;
}
