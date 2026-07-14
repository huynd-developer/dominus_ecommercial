import { defineStore } from "pinia";
import { ownerReportService } from "../services/ownerReport.service";
import type {
  BestSellingProductResponse,
  ReportFilterParams,
  ReportFilterType,
  ReportSummaryResponse,
  RevenueChartResponse,
} from "../types/report.type";

const FILTER_TYPES: ReportFilterType[] = [
  "DAY",
  "WEEK",
  "MONTH",
  "YEAR",
  "CUSTOM",
];

const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
const POSITIVE_INTEGER_REGEX = /^[1-9]\d*$/;

const MAX_LIMIT = 50;
const MAX_CUSTOM_DAYS = 366;

function hasWhitespace(value: string): boolean {
  return /\s/.test(value);
}

function parseLocalDate(value: string): Date | null {
  if (!value) {
    return null;
  }

  if (hasWhitespace(value)) {
    return null;
  }

  if (!DATE_REGEX.test(value)) {
    return null;
  }

  const parts = value.split("-");

  if (parts.length !== 3) {
    return null;
  }

  const [rawYear, rawMonth, rawDay] = parts;

  const year = Number(rawYear);
  const month = Number(rawMonth);
  const day = Number(rawDay);

  if (
    !Number.isInteger(year) ||
    !Number.isInteger(month) ||
    !Number.isInteger(day)
  ) {
    return null;
  }

  if (year < 2000 || year > 2100) {
    return null;
  }

  if (month < 1 || month > 12) {
    return null;
  }

  if (day < 1 || day > 31) {
    return null;
  }

  const date = new Date(year, month - 1, day);

  const isValid =
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day;

  return isValid ? date : null;
}

function getTodayDateOnly(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

function normalizeLimit(value: string): string {
  const cleanValue = String(value || "").replace(/[^\d]/g, "").slice(0, 2);

  return cleanValue || "10";
}

function toNumber(value: unknown): number {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function getErrorMessage(error: any): string {
  const responseData = error?.response?.data;

  if (typeof responseData === "string") {
    return responseData;
  }

  if (responseData?.message) {
    return responseData.message;
  }

  if (responseData?.errors) {
    const firstError = Object.values(responseData.errors)[0];

    if (typeof firstError === "string") {
      return firstError;
    }

    if (Array.isArray(firstError) && typeof firstError[0] === "string") {
      return firstError[0];
    }
  }

  if (error?.message) {
    return error.message;
  }

  return "Có lỗi xảy ra khi tải báo cáo";
}

function normalizeSummary(data: any): ReportSummaryResponse | null {
  if (!data) {
    return null;
  }

  return {
    filterType: data.filterType || "MONTH",
    fromDate: data.fromDate || "",
    toDate: data.toDate || "",
    totalRevenue: toNumber(data.totalRevenue),
    totalOrders: toNumber(data.totalOrders),
    totalProductsSold: toNumber(data.totalProductsSold),
  };
}

function normalizeChartData(data: any): RevenueChartResponse[] {
  if (!Array.isArray(data)) {
    return [];
  }

  return data.map((item) => ({
    label: String(item?.label || ""),
    revenue: toNumber(item?.revenue),
    totalOrders: toNumber(item?.totalOrders),
  }));
}

function normalizeBestSellingProducts(data: any): BestSellingProductResponse[] {
  if (!Array.isArray(data)) {
    return [];
  }

  return data.map((item) => ({
    productId: toNumber(item?.productId),
    productName: String(item?.productName || "Sản phẩm"),
    brandName: String(item?.brandName || "Không rõ thương hiệu"),
    totalSold: toNumber(item?.totalSold),
    revenue: toNumber(item?.revenue),
    imageUrl: item?.imageUrl || null,
  }));
}

export const useOwnerReportStore = defineStore("ownerReport", {
  state: () => ({
    filter: {
      filterType: "MONTH" as ReportFilterType,
      fromDate: "",
      toDate: "",
      limit: "10",
    },

    summary: null as ReportSummaryResponse | null,
    chartData: [] as RevenueChartResponse[],
    bestSellingProducts: [] as BestSellingProductResponse[],

    loading: false,
    error: "",
  }),

  actions: {
    validateFilter(): string {
      const filterType = this.filter.filterType;
      const fromDate = this.filter.fromDate;
      const toDate = this.filter.toDate;
      const limit = normalizeLimit(this.filter.limit);

      this.filter.limit = limit;

      if (!filterType) {
        return "Vui lòng chọn mốc thời gian";
      }

      if (hasWhitespace(filterType)) {
        return "Mốc thời gian không được chứa khoảng trắng";
      }

      if (!FILTER_TYPES.includes(filterType)) {
        return "Mốc thời gian không hợp lệ";
      }

      if (!limit) {
        return "Limit không được để trống";
      }

      if (hasWhitespace(limit)) {
        return "Limit không được chứa khoảng trắng";
      }

      if (!POSITIVE_INTEGER_REGEX.test(limit)) {
        return "Limit chỉ được nhập số nguyên dương";
      }

      const limitNumber = Number(limit);

      if (!Number.isInteger(limitNumber)) {
        return "Limit chỉ được nhập số nguyên dương";
      }

      if (limitNumber < 1 || limitNumber > MAX_LIMIT) {
        return `Limit phải nằm trong khoảng từ 1 đến ${MAX_LIMIT}`;
      }

      if (filterType !== "CUSTOM") {
        return "";
      }

      if (!fromDate) {
        return "Vui lòng chọn ngày bắt đầu";
      }

      if (!toDate) {
        return "Vui lòng chọn ngày kết thúc";
      }

      if (hasWhitespace(fromDate)) {
        return "Ngày bắt đầu không được chứa khoảng trắng";
      }

      if (hasWhitespace(toDate)) {
        return "Ngày kết thúc không được chứa khoảng trắng";
      }

      if (!DATE_REGEX.test(fromDate)) {
        return "Ngày bắt đầu phải đúng định dạng yyyy-MM-dd";
      }

      if (!DATE_REGEX.test(toDate)) {
        return "Ngày kết thúc phải đúng định dạng yyyy-MM-dd";
      }

      const startDate = parseLocalDate(fromDate);
      const endDate = parseLocalDate(toDate);

      if (!startDate) {
        return "Ngày bắt đầu không hợp lệ";
      }

      if (!endDate) {
        return "Ngày kết thúc không hợp lệ";
      }

      if (startDate > endDate) {
        return "Ngày bắt đầu không được lớn hơn ngày kết thúc";
      }

      const today = getTodayDateOnly();

      if (endDate > today) {
        return "Ngày kết thúc không được lớn hơn ngày hiện tại";
      }

      const inclusiveDays =
        (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24) + 1;

      if (inclusiveDays > MAX_CUSTOM_DAYS) {
        return `Khoảng thời gian tùy chỉnh tối đa là ${MAX_CUSTOM_DAYS} ngày`;
      }

      return "";
    },

    buildParams(): ReportFilterParams {
      const params: ReportFilterParams = {
        filterType: this.filter.filterType || "MONTH",
        limit: normalizeLimit(this.filter.limit),
      };

      if (this.filter.filterType === "CUSTOM") {
        params.fromDate = this.filter.fromDate;
        params.toDate = this.filter.toDate;
      }

      return params;
    },

    resetCustomDateIfNeeded(): void {
      if (this.filter.filterType !== "CUSTOM") {
        this.filter.fromDate = "";
        this.filter.toDate = "";
      }
    },

    async fetchAll(): Promise<void> {
      this.resetCustomDateIfNeeded();

      const validateMessage = this.validateFilter();

      if (validateMessage) {
        this.error = validateMessage;
        this.loading = false;
        return;
      }

      this.loading = true;
      this.error = "";

      try {
        const params = this.buildParams();

        const [summaryRes, chartRes, bestSellingRes] = await Promise.all([
          ownerReportService.getSummary(params),
          ownerReportService.getRevenueChart(params),
          ownerReportService.getBestSellingProducts(params),
        ]);

        this.summary = normalizeSummary(summaryRes.data);
        this.chartData = normalizeChartData(chartRes.data);
        this.bestSellingProducts = normalizeBestSellingProducts(
          bestSellingRes.data
        );
      } catch (error: any) {
        this.error = getErrorMessage(error);
        this.summary = null;
        this.chartData = [];
        this.bestSellingProducts = [];
      } finally {
        this.loading = false;
      }
    },
  },
});