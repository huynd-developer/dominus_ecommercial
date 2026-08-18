import { defineStore } from "pinia";
import goodsReceiptService from "../services/goods-receipt.service";
import type {
  GoodsReceiptApprovalHistoryResponse,
  GoodsReceiptCancelRequest,
  GoodsReceiptDetailResponse,
  GoodsReceiptListResponse,
  GoodsReceiptRejectRequest,
  GoodsReceiptSaveRequest,
  GoodsReceiptStatus,
} from "../types/goods-receipt.type";

interface GoodsReceiptState {
  receipts: GoodsReceiptListResponse[];
  detail: GoodsReceiptDetailResponse | null;
  history: GoodsReceiptApprovalHistoryResponse[];
  pendingCount: number;
  keyword: string;
  status: GoodsReceiptStatus | "";
  createdBy: number | null;
  fromDate: string;
  toDate: string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  loadingList: boolean;
  loadingDetail: boolean;
  loadingHistory: boolean;
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

export const useGoodsReceiptStore = defineStore("goodsReceipt", {
  state: (): GoodsReceiptState => ({
    receipts: [],
    detail: null,
    history: [],
    pendingCount: 0,
    keyword: "",
    status: "",
    createdBy: null,
    fromDate: "",
    toDate: "",
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    loadingList: false,
    loadingDetail: false,
    loadingHistory: false,
    saving: false,
    processing: false,
    error: null,
  }),

  actions: {
    async fetchList() {
      this.loadingList = true;
      this.error = null;
      try {
        const data = await goodsReceiptService.getList({
          keyword: this.keyword,
          status: this.status,
          receiptType: "NORMAL_RECEIPT",
          createdBy: this.createdBy,
          fromDate: this.fromDate,
          toDate: this.toDate,
          page: this.page,
          size: this.size,
        });

        this.receipts = Array.isArray(data?.content) ? data.content : [];
        this.page = safeNumber(data?.page?.number, 0);
        this.size = safeNumber(data?.page?.size, this.size);
        this.totalElements = safeNumber(data?.page?.totalElements, 0);
        this.totalPages = safeNumber(data?.page?.totalPages, 0);
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
        this.detail = await goodsReceiptService.getDetail(id);
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
        this.history = await goodsReceiptService.getApprovalHistory(id);
        return this.history;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingHistory = false;
      }
    },

    async fetchPendingCount() {
      try {
        const result = await goodsReceiptService.getPendingCount();
        this.pendingCount = safeNumber(result?.pendingCount, 0);
      } catch {
        this.pendingCount = 0;
      }
    },

    async create(request: GoodsReceiptSaveRequest) {
      this.saving = true;
      this.error = null;
      try {
        const result = await goodsReceiptService.create(request);
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.saving = false;
      }
    },

    async update(id: number, request: GoodsReceiptSaveRequest) {
      this.saving = true;
      this.error = null;
      try {
        const result = await goodsReceiptService.update(id, request);
        await this.fetchList();
        if (this.detail?.id === id) this.detail = result;
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.saving = false;
      }
    },

    async runWorkflow(action: () => Promise<GoodsReceiptDetailResponse>) {
      this.processing = true;
      this.error = null;
      try {
        const result = await action();
        if (this.detail?.id === result.id) this.detail = result;
        await Promise.all([this.fetchList(), this.fetchPendingCount()]);
        if (this.detail?.id === result.id) await this.fetchHistory(result.id);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.processing = false;
      }
    },

    async submit(id: number) {
      return this.runWorkflow(() => goodsReceiptService.submit(id));
    },

    async approve(id: number) {
      return this.runWorkflow(() => goodsReceiptService.approve(id));
    },

    async reject(id: number, request: GoodsReceiptRejectRequest) {
      return this.runWorkflow(() => goodsReceiptService.reject(id, request));
    },

    async cancel(id: number, request: GoodsReceiptCancelRequest) {
      return this.runWorkflow(() => goodsReceiptService.cancel(id, request));
    },

    resetFilters() {
      this.keyword = "";
      this.status = "";
      this.createdBy = null;
      this.fromDate = "";
      this.toDate = "";
      this.page = 0;
    },
  },
});
