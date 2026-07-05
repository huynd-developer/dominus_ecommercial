import api from "@/common/api";
import type {
  BestSellingProductResponse,
  ReportFilterParams,
  ReportSummaryResponse,
  RevenueChartResponse,
} from "../types/report.type";

const buildParams = (params: ReportFilterParams) => {
  const query: Record<string, string> = {
    filterType: params.filterType,
  };

  if (params.filterType === "CUSTOM") {
    query.fromDate = params.fromDate || "";
    query.toDate = params.toDate || "";
  }

  return query;
};

export const ownerReportService = {
  getSummary(params: ReportFilterParams) {
    return api.get<ReportSummaryResponse>("/owner/reports/summary", {
      params: buildParams(params),
    });
  },

  getRevenueChart(params: ReportFilterParams) {
    return api.get<RevenueChartResponse[]>("/owner/reports/revenue-chart", {
      params: buildParams(params),
    });
  },

  getBestSellingProducts(params: ReportFilterParams) {
    return api.get<BestSellingProductResponse[]>(
      "/owner/reports/best-selling-products",
      {
        params: {
          ...buildParams(params),
          limit: params.limit || "10",
        },
      }
    );
  },
};