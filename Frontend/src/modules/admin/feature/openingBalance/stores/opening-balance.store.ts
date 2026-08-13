import { defineStore } from "pinia";
import openingBalanceService from "../services/opening-balance.service";

import type {
  OpeningBalanceApprovalHistoryResponse,
  OpeningBalanceDetailResponse,
  OpeningBalanceListResponse,
  OpeningBalanceRejectRequest,
  OpeningBalanceSaveRequest,
  OpeningBalanceStatus,
} from "../types/opening-balance.type";

interface OpeningBalanceState {
  receipts: OpeningBalanceListResponse[];
  detail: OpeningBalanceDetailResponse | null;
  history: OpeningBalanceApprovalHistoryResponse[];

  keyword: string;
  status: OpeningBalanceStatus | "";
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
  loadingHistory: boolean;
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

export const useOpeningBalanceStore = defineStore("openingBalance", {
  state: (): OpeningBalanceState => ({
    receipts: [],
    detail: null,
    history: [],

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
    loadingHistory: false,
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
        const data = await openingBalanceService.getList({
          keyword: this.keyword,
          status: this.status,
          createdBy: this.createdBy,
          fromDate: this.fromDate,
          toDate: this.toDate,
          page: this.page,
          size: this.size,
        });

        this.receipts = Array.isArray(data?.content) ? data.content : [];
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
        this.detail = await openingBalanceService.getDetail(id);
        return this.detail;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingDetail = false;
      }
    },

    async fetchHistory(id: number) {
      this.loadingHistory = true;
      this.error = null;

      try {
        this.history = await openingBalanceService.getApprovalHistory(id);
        return this.history;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingHistory = false;
      }
    },

    async fetchPendingCount() {
      this.loadingPendingCount = true;

      try {
        this.pendingCount = await openingBalanceService.getPendingCount();
        return this.pendingCount;
      } catch (error) {
        // Không làm hỏng cả màn hình chỉ vì badge lỗi.
        this.pendingCount = 0;
        return 0;
      } finally {
        this.loadingPendingCount = false;
      }
    },

    async create(request: OpeningBalanceSaveRequest) {
      this.saving = true;
      this.error = null;

      try {
        const result = await openingBalanceService.create(request);
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

    async update(id: number, request: OpeningBalanceSaveRequest) {
      this.saving = true;
      this.error = null;

      try {
        const result = await openingBalanceService.update(id, request);
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
        const result = await openingBalanceService.submit(id);
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
        const result = await openingBalanceService.approve(id);
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

    async reject(id: number, request: OpeningBalanceRejectRequest) {
      this.processing = true;
      this.error = null;

      try {
        const result = await openingBalanceService.reject(id, request);
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

    clearDetailContext() {
      this.detail = null;
      this.history = [];
    },
  },
});
