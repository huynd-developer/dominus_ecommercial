import { defineStore } from "pinia";
import Swal from "sweetalert2";
import { promotionService } from "../services/promotion.service";
import type {
  FlashSaleProductResponse,
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

  return "Có lỗi xảy ra, vui lòng thử lại";
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

    loading: false,
    optionLoading: false,
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

        this.promotions = res.data.content || [];
        this.pageNumber = getPageNumber(res.data);
        this.totalPages = getTotalPages(res.data);
        this.totalElements = getTotalElements(res.data);
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

        this.variantOptions = res.data.content || [];
        this.optionPageNumber = getPageNumber(res.data);
        this.optionTotalPages = getTotalPages(res.data);
        this.optionTotalElements = getTotalElements(res.data);
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

    async fetchFlashSaleProducts() {
      const res = await promotionService.getFlashSaleProducts();
      this.flashSaleProducts = res.data || [];
    },
  },
});