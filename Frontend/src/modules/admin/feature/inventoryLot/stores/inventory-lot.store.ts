import { defineStore } from "pinia";
import inventoryLotService from "../services/inventory-lot.service";

import type {
  InventoryLotDetailResponse,
  InventoryLotExpiryFilter,
  InventoryLotListResponse,
  InventoryLotSourceResponse,
  InventoryLotStockFilter,
} from "../types/inventory-lot.type";

interface InventoryLotState {
  lots: InventoryLotListResponse[];
  detail: InventoryLotDetailResponse | null;
  source: InventoryLotSourceResponse | null;

  keyword: string;
  productVariantId: number | null;
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

    keyword: "",
    productVariantId: null,
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
    error: null,
  }),

  actions: {
    buildApiFilters() {
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

      return { isExpired, isNearExpiry, hasStock };
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

    async fetchDetailContext(id: number) {
      this.detail = null;
      this.source = null;

      await Promise.all([
        this.fetchDetail(id),
        this.fetchSource(id),
      ]);

      return this.detail;
    },

    resetFilters() {
      this.keyword = "";
      this.productVariantId = null;
      this.expiryFilter = "";
      this.stockFilter = "";
      this.expirationFrom = "";
      this.expirationTo = "";
      this.page = 0;
    },

    clearDetailContext() {
      this.detail = null;
      this.source = null;
    },
  },
});