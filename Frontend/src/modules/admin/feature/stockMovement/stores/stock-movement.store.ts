import { defineStore } from "pinia";

import stockMovementService
  from "../services/stock-movement.service";

import type {
  StockMovementDetailResponse,
  StockMovementListResponse,
  StockMovementType,
} from "../types/stock-movement.type";

interface StockMovementState {
  movements: StockMovementListResponse[];
  detail: StockMovementDetailResponse | null;

  keyword: string;
  inventoryLotId: number | null;
  movementType: StockMovementType | "";
  createdBy: number | null;
  referenceType: string;
  referenceId: number | null;
  fromDate: string;
  toDate: string;

  page: number;
  size: number;
  totalElements: number;
  totalPages: number;

  loadingList: boolean;
  loadingDetail: boolean;

  error: string | null;
}

const safeNumber = (
  value: unknown,
  fallback = 0
): number => {
  const parsed = Number(value);

  return Number.isFinite(parsed)
    ? parsed
    : fallback;
};

const errorMessage = (
  error: any
): string =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  error?.message ||
  "Có lỗi xảy ra.";

export const useStockMovementStore =
  defineStore("stockMovement", {
    state: (): StockMovementState => ({
      movements: [],
      detail: null,

      keyword: "",
      inventoryLotId: null,
      movementType: "",
      createdBy: null,
      referenceType: "",
      referenceId: null,
      fromDate: "",
      toDate: "",

      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,

      loadingList: false,
      loadingDetail: false,

      error: null,
    }),

    actions: {
      async fetchList() {
        this.loadingList = true;
        this.error = null;

        try {
          const data =
            await stockMovementService.getList({
              keyword: this.keyword,
              inventoryLotId: this.inventoryLotId,
              movementType: this.movementType,
              createdBy: this.createdBy,
              referenceType: this.referenceType,
              referenceId: this.referenceId,
              fromDate: this.fromDate,
              toDate: this.toDate,
              page: this.page,
              size: this.size,
            });

          this.movements =
            Array.isArray(data?.content)
              ? data.content
              : [];

          this.page =
            safeNumber(
              data?.page?.number ??
                data?.number,
              0
            );

          this.size =
            safeNumber(
              data?.page?.size ??
                data?.size,
              this.size
            );

          this.totalElements =
            safeNumber(
              data?.page?.totalElements ??
                data?.totalElements,
              0
            );

          this.totalPages =
            safeNumber(
              data?.page?.totalPages ??
                data?.totalPages,
              0
            );
        } catch (error) {
          this.movements = [];
          this.totalElements = 0;
          this.totalPages = 0;
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
          this.detail =
            await stockMovementService
              .getDetail(id);

          return this.detail;
        } catch (error) {
          this.detail = null;
          this.error = errorMessage(error);
          throw error;
        } finally {
          this.loadingDetail = false;
        }
      },

      resetFilters() {
        this.keyword = "";
        this.inventoryLotId = null;
        this.movementType = "";
        this.createdBy = null;
        this.referenceType = "";
        this.referenceId = null;
        this.fromDate = "";
        this.toDate = "";
        this.page = 0;
        this.size = 20;
      },

      clearDetail() {
        this.detail = null;
      },
    },
  });
