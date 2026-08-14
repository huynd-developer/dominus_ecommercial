import { defineStore } from "pinia";
import stockAdjustmentService from "../services/stock-adjustment.service";

import type {
  StockAdjustmentDetailResponse,
  StockAdjustmentListResponse,
  StockAdjustmentRejectRequest,
  StockAdjustmentSaveRequest,
  StockAdjustmentStatus,
} from "../types/stock-adjustment.type";

interface StockAdjustmentState {
  adjustments: StockAdjustmentListResponse[];
  detail: StockAdjustmentDetailResponse | null;

  keyword: string;
  status: StockAdjustmentStatus | "";
  createdBy: number | null;
  fromDate: string;
  toDate: string;

  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  pendingCount: number;

  loadingList: boolean;
  loadingDetail: boolean;
  loadingPendingCount: boolean;
  saving: boolean;
  processing: boolean;
  error: string | null;
}

const errorMessage = (error: any): string =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  error?.message ||
  "Có lỗi xảy ra";

const safeNumber = (value: unknown, fallback = 0) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

export const useStockAdjustmentStore = defineStore("stockAdjustment", {
  state: (): StockAdjustmentState => ({
    adjustments: [],
    detail: null,

    keyword: "",
    status: "",
    createdBy: null,
    fromDate: "",
    toDate: "",

    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    pendingCount: 0,

    loadingList: false,
    loadingDetail: false,
    loadingPendingCount: false,
    saving: false,
    processing: false,
    error: null,
  }),

  actions: {
    async fetchList() {
      this.loadingList = true;
      this.error = null;

      try {
        const data = await stockAdjustmentService.getList({
          keyword: this.keyword,
          status: this.status,
          createdBy: this.createdBy,
          fromDate: this.fromDate,
          toDate: this.toDate,
          page: this.page,
          size: this.size,
        });

        this.adjustments = Array.isArray(data?.content) ? data.content : [];
        this.page = safeNumber(data?.page?.number ?? data?.number, 0);
        this.size = safeNumber(data?.page?.size ?? data?.size, this.size);
        this.totalElements = safeNumber(
          data?.page?.totalElements ?? data?.totalElements,
          0
        );
        this.totalPages = safeNumber(
          data?.page?.totalPages ?? data?.totalPages,
          0
        );

        return data;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingList = false;
      }
    },

    async fetchDetail(id: number) {
      this.loadingDetail = true;
      this.error = null;

      try {
        this.detail = await stockAdjustmentService.getDetail(id);
        return this.detail;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingDetail = false;
      }
    },

    async fetchPendingCount() {
      this.loadingPendingCount = true;

      try {
        this.pendingCount = await stockAdjustmentService.getPendingCount();
        return this.pendingCount;
      } catch {
        this.pendingCount = 0;
        return 0;
      } finally {
        this.loadingPendingCount = false;
      }
    },

    async create(request: StockAdjustmentSaveRequest) {
      this.saving = true;
      this.error = null;

      try {
        const result = await stockAdjustmentService.create(request);
        this.detail = result;
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.saving = false;
      }
    },

    async update(id: number, request: StockAdjustmentSaveRequest) {
      this.saving = true;
      this.error = null;

      try {
        const result = await stockAdjustmentService.update(id, request);
        if (this.detail?.id === id) this.detail = result;
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.saving = false;
      }
    },

    async submit(id: number) {
      this.processing = true;
      this.error = null;

      try {
        const result = await stockAdjustmentService.submit(id);
        if (this.detail?.id === id) this.detail = result;
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.processing = false;
      }
    },

    async approve(id: number) {
      this.processing = true;
      this.error = null;

      try {
        const result = await stockAdjustmentService.approve(id);
        if (this.detail?.id === id) this.detail = result;
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.processing = false;
      }
    },

    async reject(id: number, request: StockAdjustmentRejectRequest) {
      this.processing = true;
      this.error = null;

      try {
        const result = await stockAdjustmentService.reject(id, request);
        if (this.detail?.id === id) this.detail = result;
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.processing = false;
      }
    },

    resetFilters() {
      this.keyword = "";
      this.status = "";
      this.createdBy = null;
      this.fromDate = "";
      this.toDate = "";
      this.page = 0;
    },

    clearDetail() {
      this.detail = null;
    },
  },
});
