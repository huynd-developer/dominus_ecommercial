import { defineStore } from "pinia";
import Swal from "sweetalert2";
import { promotionService } from "../services/promotion.service";
import type {
  FlashSaleProductResponse,
  FlashSaleSearchParams,
  PageResponse,
  PromotionProductVariantOptionResponse,
  PromotionRequest,
  PromotionResponse,
  PromotionSearchParams,
  PromotionStatus,
  ProductVariantSearchParams,
} from "../types/promotion.types";

const getErrorMessage = (error: any): string => {
  const data = error?.response?.data;

  if (data?.errors) {
    return Object.values(data.errors).map(String).join("<br/>");
  }

  if (data?.message) {
    return data.message;
  }

  if (typeof data === "string") {
    return data;
  }

  return "Có lỗi xảy ra, vui lòng thử lại";
};

const getPageContent = <T>(page: PageResponse<T> | T[] | any): T[] => {
  if (Array.isArray(page)) return page;
  if (Array.isArray(page?.content)) return page.content;
  if (Array.isArray(page?.data?.content)) return page.data.content;
  if (Array.isArray(page?.data)) return page.data;
  return [];
};

const getPageNumber = <T>(page: PageResponse<T>) => {
  return page.page?.number ?? page.number ?? 0;
};

const getTotalPages = <T>(page: PageResponse<T>) => {
  return page.page?.totalPages ?? page.totalPages ?? 0;
};

const getTotalElements = <T>(page: PageResponse<T>) => {
  return page.page?.totalElements ?? page.totalElements ?? 0;
};

export const usePromotionStore = defineStore("promotionStore", {
  state: () => ({
    promotions: [] as PromotionResponse[],
    variantOptions: [] as PromotionProductVariantOptionResponse[],
    flashSaleProducts: [] as FlashSaleProductResponse[],

    pageNumber: 0,
    pageSize: 10,
    totalPages: 0,
    totalElements: 0,

    optionPageNumber: 0,
    optionPageSize: 10,
    optionTotalPages: 0,
    optionTotalElements: 0,

    flashSalePageNumber: 0,
    flashSalePageSize: 8,
    flashSaleTotalPages: 0,
    flashSaleTotalElements: 0,

    loading: false,
    optionLoading: false,
    flashSaleLoading: false,
    saving: false,
  }),

  actions: {
    async fetchPromotions(params: PromotionSearchParams = {}) {
      this.loading = true;

      try {
        const res = await promotionService.getAll({
          keyword: params.keyword,
          status: params.status,
          page: params.page ?? this.pageNumber,
          size: params.size ?? this.pageSize,
        });

        this.promotions = getPageContent<PromotionResponse>(res.data);
        this.pageNumber = getPageNumber(res.data);
        this.totalPages = getTotalPages(res.data);
        this.totalElements = getTotalElements(res.data);

        if (params.size) {
          this.pageSize = params.size;
        }
      } catch (error: any) {
        await Swal.fire({
          icon: "error",
          title: "Không tải được danh sách chiến dịch",
          html: getErrorMessage(error),
          confirmButtonColor: "#bd9a5f",
        });
      } finally {
        this.loading = false;
      }
    },

    async fetchDetail(id: number) {
      const res = await promotionService.getById(id);
      return res.data;
    },

    async searchProductVariants(params: ProductVariantSearchParams = {}) {
      this.optionLoading = true;

      try {
        const res = await promotionService.searchProductVariants({
          keyword: params.keyword,
          startDate: params.startDate,
          endDate: params.endDate,
          ignorePromotionId: params.ignorePromotionId,
          page: params.page ?? this.optionPageNumber,
          size: params.size ?? this.optionPageSize,
        });

        this.variantOptions =
          getPageContent<PromotionProductVariantOptionResponse>(res.data);
        this.optionPageNumber = getPageNumber(res.data);
        this.optionTotalPages = getTotalPages(res.data);
        this.optionTotalElements = getTotalElements(res.data);

        if (params.size) {
          this.optionPageSize = params.size;
        }
      } catch (error: any) {
        await Swal.fire({
          icon: "error",
          title: "Không tải được danh sách biến thể",
          html: getErrorMessage(error),
          confirmButtonColor: "#bd9a5f",
        });
      } finally {
        this.optionLoading = false;
      }
    },

    async createPromotion(payload: PromotionRequest) {
      this.saving = true;

      try {
        const res = await promotionService.create(payload);

        await Swal.fire({
          icon: "success",
          title: "Tạo chiến dịch thành công",
          timer: 1200,
          showConfirmButton: false,
        });

        return res.data;
      } catch (error: any) {
        await Swal.fire({
          icon: "error",
          title: "Tạo chiến dịch thất bại",
          html: getErrorMessage(error),
          confirmButtonColor: "#bd9a5f",
        });

        throw error;
      } finally {
        this.saving = false;
      }
    },

    async updatePromotion(id: number, payload: PromotionRequest) {
      this.saving = true;

      try {
        const res = await promotionService.update(id, payload);

        await Swal.fire({
          icon: "success",
          title: "Cập nhật chiến dịch thành công",
          timer: 1200,
          showConfirmButton: false,
        });

        return res.data;
      } catch (error: any) {
        await Swal.fire({
          icon: "error",
          title: "Cập nhật chiến dịch thất bại",
          html: getErrorMessage(error),
          confirmButtonColor: "#bd9a5f",
        });

        throw error;
      } finally {
        this.saving = false;
      }
    },

    async changeStatus(id: number, status: PromotionStatus) {
      try {
        const res = await promotionService.changeStatus(id, { status });

        await Swal.fire({
          icon: "success",
          title: status === 1 ? "Đã bật chiến dịch" : "Đã tắt chiến dịch",
          timer: 1000,
          showConfirmButton: false,
        });

        return res.data;
      } catch (error: any) {
        await Swal.fire({
          icon: "error",
          title: "Đổi trạng thái thất bại",
          html: getErrorMessage(error),
          confirmButtonColor: "#bd9a5f",
        });

        throw error;
      }
    },

    async removePromotion(id: number) {
      try {
        await promotionService.remove(id);

        await Swal.fire({
          icon: "success",
          title: "Đã xóa chiến dịch",
          timer: 1000,
          showConfirmButton: false,
        });
      } catch (error: any) {
        await Swal.fire({
          icon: "error",
          title: "Xóa chiến dịch thất bại",
          html: getErrorMessage(error),
          confirmButtonColor: "#bd9a5f",
        });

        throw error;
      }
    },

    async fetchFlashSaleProducts(params: FlashSaleSearchParams = {}) {
      this.flashSaleLoading = true;

      try {
        const res = await promotionService.getFlashSaleProducts({
          page: params.page ?? this.flashSalePageNumber,
          size: params.size ?? this.flashSalePageSize,
        });

        this.flashSaleProducts = getPageContent<FlashSaleProductResponse>(
          res.data
        );
        this.flashSalePageNumber = getPageNumber(res.data);
        this.flashSaleTotalPages = getTotalPages(res.data);
        this.flashSaleTotalElements = getTotalElements(res.data);

        if (params.size) {
          this.flashSalePageSize = params.size;
        }
      } finally {
        this.flashSaleLoading = false;
      }
    },
  },
});