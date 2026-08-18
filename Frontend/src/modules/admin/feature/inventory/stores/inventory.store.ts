import { defineStore } from "pinia";

import inventoryService from "../services/inventory.service";

import type {
  InventoryConfig,
  InventoryConfigUpdateRequest,
  InventoryLotStatus,
  InventoryOverview,
  InventoryOverviewParams,
  InventorySummary,
  InventoryStockStatus,
} from "../types/inventory.type";

interface InventoryState {
  summary: InventorySummary | null;

  overview: InventoryOverview[];
  nearExpiryLots: InventoryLotStatus[];
  expiredLots: InventoryLotStatus[];

  config: InventoryConfig | null;

  loadingSummary: boolean;
  loadingOverview: boolean;
  loadingLots: boolean;
  loadingConfig: boolean;
  savingConfig: boolean;

  keyword: string;

  stockStatus: InventoryStockStatus;
  nearExpiryFilter?: boolean;
  expiredFilter?: boolean;

  overviewPage: number;
  overviewSize: number;
  overviewTotalElements: number;
  overviewTotalPages: number;

  lotPage: number;
  lotSize: number;
  lotTotalElements: number;
  lotTotalPages: number;

  error: string | null;
}

const getErrorMessage = (error: any): string => {
  return error?.response?.data?.message || error?.message || "Có lỗi xảy ra";
};

/**
 * Chuyển giá trị pagination về number an toàn.
 * Nếu API không trả hoặc giá trị không hợp lệ thì dùng fallback.
 */
const safeNumber = (value: unknown, fallback: number = 0): number => {
  const numberValue = Number(value);

  return Number.isFinite(numberValue) ? numberValue : fallback;
};

/**
 * Hỗ trợ cả 2 trường hợp:
 *
 * 1. Service đã return response.data
 * 2. Service vô tình return nguyên AxiosResponse
 *
 * Không ảnh hưởng nếu service hiện tại đã đúng.
 */
const unwrapResponse = <T>(response: T): any => {
  const value = response as any;

  if (
    value &&
    typeof value === "object" &&
    value.data &&
    typeof value.data === "object" &&
    !Array.isArray(value.data) &&
    value.content === undefined
  ) {
    return value.data;
  }

  return value;
};

export const useInventoryStore = defineStore("inventory", {
  state: (): InventoryState => ({
    summary: null,

    overview: [],
    nearExpiryLots: [],
    expiredLots: [],

    config: null,

    loadingSummary: false,
    loadingOverview: false,
    loadingLots: false,
    loadingConfig: false,
    savingConfig: false,

    keyword: "",

    stockStatus: "ALL",

    nearExpiryFilter: undefined,
    expiredFilter: undefined,

    overviewPage: 0,
    overviewSize: 20,
    overviewTotalElements: 0,
    overviewTotalPages: 0,

    lotPage: 0,
    lotSize: 20,
    lotTotalElements: 0,
    lotTotalPages: 0,

    error: null,
  }),

  actions: {
    async fetchSummary() {
      this.loadingSummary = true;
      this.error = null;

      try {
        this.summary = await inventoryService.getSummary();
      } catch (error) {
        this.error = getErrorMessage(error);
        throw error;
      } finally {
        this.loadingSummary = false;
      }
    },

    async fetchOverview(override: Partial<InventoryOverviewParams> = {}) {
      this.loadingOverview = true;
      this.error = null;

      try {
        const response = await inventoryService.getOverview({
          keyword: override.keyword ?? this.keyword,

          stockStatus: override.stockStatus ?? this.stockStatus,

          nearExpiry:
            override.nearExpiry !== undefined
              ? override.nearExpiry
              : this.nearExpiryFilter,

          expired:
            override.expired !== undefined
              ? override.expired
              : this.expiredFilter,

          page: override.page ?? this.overviewPage,

          size: override.size ?? this.overviewSize,
        });

        const data = unwrapResponse(response);

        this.overview = Array.isArray(data?.content) ? data.content : [];

        this.overviewPage = safeNumber(data?.page?.number, 0);

        this.overviewSize = safeNumber(data?.page?.size, this.overviewSize);

        this.overviewTotalElements = safeNumber(data?.page?.totalElements, 0);

        this.overviewTotalPages = safeNumber(data?.page?.totalPages, 0);
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingOverview = false;
      }
    },

    async fetchNearExpiry() {
      this.loadingLots = true;
      this.error = null;

      try {
        const response = await inventoryService.getNearExpiry({
          keyword: this.keyword,
          page: this.lotPage,
          size: this.lotSize,
        });

        const data = unwrapResponse(response);

        this.nearExpiryLots = Array.isArray(data?.content) ? data.content : [];

        this.lotPage = safeNumber(data?.number, 0);

        this.lotSize = safeNumber(data?.size, this.lotSize);

        this.lotTotalElements = safeNumber(data?.totalElements, 0);

        this.lotTotalPages = safeNumber(data?.totalPages, 0);
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingLots = false;
      }
    },

    async fetchExpired() {
      this.loadingLots = true;
      this.error = null;

      try {
        const response = await inventoryService.getExpired({
          keyword: this.keyword,
          page: this.lotPage,
          size: this.lotSize,
        });

        const data = unwrapResponse(response);

        this.expiredLots = Array.isArray(data?.content) ? data.content : [];

        this.lotPage = safeNumber(data?.number, 0);

        this.lotSize = safeNumber(data?.size, this.lotSize);

        this.lotTotalElements = safeNumber(data?.totalElements, 0);

        this.lotTotalPages = safeNumber(data?.totalPages, 0);
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingLots = false;
      }
    },

    async fetchConfig() {
      this.loadingConfig = true;
      this.error = null;

      try {
        this.config = await inventoryService.getConfig();
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingConfig = false;
      }
    },

    async updateConfig(request: InventoryConfigUpdateRequest) {
      this.savingConfig = true;
      this.error = null;

      try {
        const updated = await inventoryService.updateConfig(request);

        this.config = updated;

        await Promise.all([this.fetchSummary(), this.fetchOverview()]);

        return updated;
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.savingConfig = false;
      }
    },

    resetOverviewFilters() {
      this.keyword = "";
      this.stockStatus = "ALL";

      this.nearExpiryFilter = undefined;

      this.expiredFilter = undefined;

      this.overviewPage = 0;
    },

    resetLotPage() {
      this.lotPage = 0;
    },
  },
});