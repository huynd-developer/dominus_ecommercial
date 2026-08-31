import { defineStore } from "pinia";
import { ref } from "vue";

import { productService } from "../services/productService";
import { useAppStore } from "@/common/store/app.store";

import type { ProductResponse } from "../types/product.type";

export const useProductStore = defineStore("productStore", () => {
  const appStore = useAppStore();

  const products = ref<ProductResponse[]>([]);
  const deletedProducts = ref<ProductResponse[]>([]);
  const brandList = ref<any[]>([]);
  const categoryList = ref<any[]>([]);
  const concentrationList = ref<any[]>([]);
  const fragranceFamilyList = ref<any[]>([]);
  const capacityList = ref<any[]>([]);
  const bottleTypeList = ref<any[]>([]);

  const isLoading = ref(false);

  // =========================
  // PRODUCT
  // =========================

  const fetchProducts = async () => {
    isLoading.value = true;

    try {
      /**
       * Service đã chịu trách nhiệm unwrap response.
       *
       * Kết quả mong đợi:
       *
       * {
       *   totalItems,
       *   totalPages,
       *   currentPage,
       *   content: [...]
       * }
       */
      const pageData = await productService.getProducts(0, 999);

      /**
       * Chỉ gán khi content thực sự là Array.
       *
       * Không:
       * - filter status;
       * - filter stock;
       * - filter variant;
       * - filter Product;
       *
       * Admin phải hiển thị toàn bộ Product
       * mà BE trả về.
       */
      products.value = Array.isArray(pageData?.content)
        ? pageData.content
        : Array.isArray(pageData)
        ? pageData
        : [];
    } catch (e: any) {
      appStore.notifyError(
        e?.response?.data?.message ?? "Không thể tải danh sách sản phẩm"
      );
    } finally {
      isLoading.value = false;
    }
  };

  const fetchDeletedProducts = async () => {
    isLoading.value = true;

    try {
      const pageData = await productService.getDeletedProducts(0, 999);

      deletedProducts.value = Array.isArray(pageData?.content)
        ? pageData.content
        : Array.isArray(pageData)
        ? pageData
        : [];
    } catch (e: any) {
      deletedProducts.value = [];

      appStore.notifyError(
        e?.response?.data?.message ?? "Không thể tải danh sách sản phẩm đã xóa"
      );
    } finally {
      isLoading.value = false;
    }
  };

  // =========================
  // DROPDOWN DATA
  // =========================

  const fetchDropdowns = async () => {
    try {
      const [
        brands,
        categories,
        concentrations,
        fragranceFamilies,
        capacities,
        bottleTypes,
      ] = await Promise.all([
        productService.getBrands(),
        productService.getCategories(),
        productService.getConcentrations(),
        productService.getFragranceFamilies(),
        productService.getCapacities(),
        productService.getBottleTypes(),
      ]);

      brandList.value =
        brands?.content ??
        brands?.data?.content ??
        brands?.data ??
        brands ??
        [];

      categoryList.value =
        categories?.content ??
        categories?.data?.content ??
        categories?.data ??
        categories ??
        [];

      concentrationList.value =
        concentrations?.content ??
        concentrations?.data?.content ??
        concentrations?.data ??
        concentrations ??
        [];

      fragranceFamilyList.value =
        fragranceFamilies?.content ??
        fragranceFamilies?.data?.content ??
        fragranceFamilies?.data ??
        fragranceFamilies ??
        [];

      capacityList.value =
        capacities?.content ??
        capacities?.data?.content ??
        capacities?.data ??
        capacities ??
        [];

      bottleTypeList.value =
        bottleTypes?.content ??
        bottleTypes?.data?.content ??
        bottleTypes?.data ??
        bottleTypes ??
        [];

      console.log("brandList", brandList.value);

      console.log("categoryList", categoryList.value);

      console.log("concentrationList", concentrationList.value);
    } catch (err) {
      console.error("Lỗi tải dữ liệu dropdown", err);

      appStore.notifyError("Không thể tải dữ liệu dropdown");
    }
  };

  return {
    products,
    deletedProducts,

    brandList,
    categoryList,
    concentrationList,
    fragranceFamilyList,
    capacityList,
    bottleTypeList,

    isLoading,

    fetchProducts,
    fetchDeletedProducts,
    fetchDropdowns,
  };
});
