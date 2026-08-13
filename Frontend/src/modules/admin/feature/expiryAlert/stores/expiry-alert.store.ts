import { defineStore } from "pinia";

import expiryAlertService from "../services/expiry-alert.service";

import type {
  ExpiryAlertGroup,
  ExpiryAlertListResponse,
  ExpiryAlertSummaryResponse,
  InventoryLotDetailResponse,
  InventoryLotLockRequest,
  InventoryLotUnlockRequest,
} from "../types/expiry-alert.type";

interface ExpiryAlertState {
  alerts: ExpiryAlertListResponse[];

  summary: ExpiryAlertSummaryResponse | null;

  detail: InventoryLotDetailResponse | null;

  group: ExpiryAlertGroup;

  keyword: string;

  fromDays: number | null;
  toDays: number | null;

  page: number;
  size: number;

  totalElements: number;
  totalPages: number;

  loadingList: boolean;
  loadingSummary: boolean;
  loadingDetail: boolean;

  processing: boolean;

  error: string | null;
}

const safeNumber = (value: unknown, fallback = 0): number => {
  const parsed = Number(value);

  return Number.isFinite(parsed) ? parsed : fallback;
};

const getErrorMessage = (error: any): string => {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.detail ||
    error?.message ||
    "Có lỗi xảy ra."
  );
};

export const useExpiryAlertStore = defineStore("expiryAlert", {
  state: (): ExpiryAlertState => ({
    alerts: [],

    summary: null,

    detail: null,

    group: "NEAR_EXPIRY",

    keyword: "",

    fromDays: null,
    toDays: null,

    page: 0,
    size: 20,

    totalElements: 0,
    totalPages: 0,

    loadingList: false,
    loadingSummary: false,
    loadingDetail: false,

    processing: false,

    error: null,
  }),

  actions: {
    async fetchList() {
      this.loadingList = true;
      this.error = null;

      try {
        const data = await expiryAlertService.getList({
          group: this.group,

          keyword: this.keyword,

          fromDays: this.fromDays,
          toDays: this.toDays,

          page: this.page,
          size: this.size,
        });

        this.alerts = Array.isArray(data?.content) ? data.content : [];

        /**
         * BE hiện tại trả:
         *
         * page: {
         *   size,
         *   number,
         *   totalElements,
         *   totalPages
         * }
         *
         * Nhưng vẫn hỗ trợ format Page cũ.
         */
        const meta = data?.page ?? data;

        this.page = safeNumber(meta?.number, this.page);

        this.size = safeNumber(meta?.size, this.size);

        this.totalElements = safeNumber(meta?.totalElements, 0);

        this.totalPages = safeNumber(meta?.totalPages, 0);
      } catch (error) {
        this.alerts = [];
        this.totalElements = 0;
        this.totalPages = 0;

        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingList = false;
      }
    },

    async fetchSummary() {
      this.loadingSummary = true;
      this.error = null;

      try {
        const data = await expiryAlertService.getSummary();

        this.summary = {
          warningDays: safeNumber(data?.warningDays),

          nearExpiryLotCount: safeNumber(data?.nearExpiryLotCount),

          nearExpiryQuantity: safeNumber(data?.nearExpiryQuantity),

          expiredLotCount: safeNumber(data?.expiredLotCount),

          expiredQuantity: safeNumber(data?.expiredQuantity),

          lockedLotCount: safeNumber(data?.lockedLotCount),

          lockedQuantity: safeNumber(data?.lockedQuantity),
        };
      } catch (error) {
        this.summary = null;

        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingSummary = false;
      }
    },

    async fetchDetail(id: number) {
      this.loadingDetail = true;
      this.error = null;

      try {
        this.detail = await expiryAlertService.getDetail(id);

        return this.detail;
      } catch (error) {
        this.detail = null;

        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.loadingDetail = false;
      }
    },

    async lockLot(id: number, request: InventoryLotLockRequest) {
      this.processing = true;
      this.error = null;

      try {
        const detail = await expiryAlertService.lock(id, request);

        this.detail = detail;

        /**
         * Sau khi khóa:
         * - IsLocked thay đổi
         * - summary LOCKED thay đổi
         * - danh sách hiện tại có thể thay đổi
         */
        await Promise.all([this.fetchList(), this.fetchSummary()]);

        return detail;
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.processing = false;
      }
    },
    async unlockLot(id: number, request?: InventoryLotUnlockRequest) {
      this.processing = true;
      this.error = null;

      try {
        const detail = await expiryAlertService.unlock(id, request);

        this.detail = detail;

        /*
         * Sau khi mở khóa:
         *
         * - IsLocked đổi false
         * - locked summary thay đổi
         * - nếu đang ở tab LOCKED thì lô có thể biến mất
         */
        await Promise.all([this.fetchList(), this.fetchSummary()]);

        return detail;
      } catch (error) {
        this.error = getErrorMessage(error);

        throw error;
      } finally {
        this.processing = false;
      }
    },

    setGroup(group: ExpiryAlertGroup) {
      this.group = group;
      this.page = 0;
    },

    resetFilters() {
      this.group = "NEAR_EXPIRY";

      this.keyword = "";

      this.fromDays = null;
      this.toDays = null;

      this.page = 0;
      this.size = 20;
    },

    clearDetail() {
      this.detail = null;
    },
  },
});
