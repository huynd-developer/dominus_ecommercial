import { defineStore } from "pinia";
import inventoryLotService from "../services/inventory-lot.service";

import type {
  InventoryLotDetailResponse,
  InventoryLotExpiryFilter,
  InventoryLotListResponse,
  InventoryLotLockFilter,
  InventoryLotLockHistoryResponse,
  InventoryLotLockRequest,
  InventoryLotSourceResponse,
  InventoryLotStockFilter,
  InventoryLotUnlockRequest,
} from "../types/inventory-lot.type";

interface InventoryLotState {
  lots: InventoryLotListResponse[];
  detail: InventoryLotDetailResponse | null;
  source: InventoryLotSourceResponse | null;
  history: InventoryLotLockHistoryResponse[];

  keyword: string;
  productVariantId: number | null;
  lockFilter: InventoryLotLockFilter;
  expiryFilter: InventoryLotExpiryFilter;
  stockFilter: InventoryLotStockFilter;
  expirationFrom: string;
  expirationTo: string;

  page: number;
  size: number;
  totalElements: number;
  totalPages: number;

  loadingList: boolean;
  loadingDetail: boolean;
  loadingSource: boolean;
  loadingHistory: boolean;
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

export const useInventoryLotStore = defineStore("inventoryLot", {
  state: (): InventoryLotState => ({
    lots: [],
    detail: null,
    source: null,
    history: [],

    keyword: "",
    productVariantId: null,
    lockFilter: "",
    expiryFilter: "",
    stockFilter: "",
    expirationFrom: "",
    expirationTo: "",

    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,

    loadingList: false,
    loadingDetail: false,
    loadingSource: false,
    loadingHistory: false,
    processing: false,
    error: null,
  }),

  actions: {
    buildApiFilters() {
      const isLocked =
        this.lockFilter === "LOCKED"
          ? true
          : this.lockFilter === "UNLOCKED"
            ? false
            : undefined;

      let isExpired: boolean | undefined;
      let isNearExpiry: boolean | undefined;

      if (this.expiryFilter === "EXPIRED") {
        isExpired = true;
      } else if (this.expiryFilter === "NEAR_EXPIRY") {
        isExpired = false;
        isNearExpiry = true;
      } else if (this.expiryFilter === "NOT_EXPIRED") {
        isExpired = false;
      }

      const hasStock =
        this.stockFilter === "IN_STOCK"
          ? true
          : this.stockFilter === "OUT_OF_STOCK"
            ? false
            : undefined;

      return { isLocked, isExpired, isNearExpiry, hasStock };
    },

    async fetchList() {
      this.loadingList = true;
      this.error = null;

      try {
        const filters = this.buildApiFilters();

        const data = await inventoryLotService.getList({
          keyword: this.keyword,
          productVariantId: this.productVariantId,
          ...filters,
          expirationFrom: this.expirationFrom,
          expirationTo: this.expirationTo,
          page: this.page,
          size: this.size,
        });

        this.lots = Array.isArray(data?.content) ? data.content : [];

        // Tương thích cả Spring Page kiểu mới (page object)
        // và kiểu cũ (metadata top-level).
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
        this.detail = await inventoryLotService.getDetail(id);
        return this.detail;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingDetail = false;
      }
    },

    async fetchSource(id: number) {
      this.loadingSource = true;
      this.error = null;

      try {
        this.source = await inventoryLotService.getSource(id);
        return this.source;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingSource = false;
      }
    },

    async fetchHistory(id: number) {
      this.loadingHistory = true;
      this.error = null;

      try {
        this.history = await inventoryLotService.getLockHistory(id);
        return this.history;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.loadingHistory = false;
      }
    },

    async fetchDetailContext(id: number) {
      this.detail = null;
      this.source = null;
      this.history = [];

      await Promise.all([
        this.fetchDetail(id),
        this.fetchSource(id),
        this.fetchHistory(id),
      ]);

      return this.detail;
    },

    async lock(id: number, request: InventoryLotLockRequest) {
      this.processing = true;
      this.error = null;

      try {
        const result = await inventoryLotService.lock(id, request);
        if (this.detail?.id === id) this.detail = result;

        await Promise.all([this.fetchList(), this.fetchHistory(id)]);
        return result;
      } catch (error) {
        this.error = errorMessage(error);
        throw error;
      } finally {
        this.processing = false;
      }
    },

    async unlock(id: number, request?: InventoryLotUnlockRequest) {
      this.processing = true;
      this.error = null;

      try {
        const result = await inventoryLotService.unlock(id, request);
        if (this.detail?.id === id) this.detail = result;

        await Promise.all([this.fetchList(), this.fetchHistory(id)]);
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
      this.productVariantId = null;
      this.lockFilter = "";
      this.expiryFilter = "";
      this.stockFilter = "";
      this.expirationFrom = "";
      this.expirationTo = "";
      this.page = 0;
    },

    clearDetailContext() {
      this.detail = null;
      this.source = null;
      this.history = [];
    },
  },
});
