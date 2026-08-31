<script setup lang="ts">
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
  onActivated,
  watch,
} from "vue";

import { useRouter, useRoute } from "vue-router";

import Swal from "sweetalert2";

import { useProductStore } from "../stores/productStore";
import { productService } from "../services/productService";

import type { Product, ProductVariant } from "../types/product.type";

import ProductList from "../components/ProductList.vue";
import ProductModal from "../components/ProductModal.vue";

const store = useProductStore();

const router = useRouter();
const route = useRoute();

const searchQuery = ref(String(route.query.q || ""));

/**
 * Chỉ lọc trạng thái ở FE.
 *
 * Không thay đổi status Product/Variant.
 * Tab Đã xóa bỏ qua filter này.
 */
type ProductStatusFilter =
  | "ALL"
  | "ACTIVE"
  | "OUT_OF_STOCK"
  | "INACTIVE";

const statusFilter = ref<ProductStatusFilter>("ALL");

const showModal = ref(false);

const selectedProduct = ref<Product | null>(null);

const isCloneMode = ref(false);

/**
 * false = danh sách Product chưa xóa
 * true  = danh sách Product đã soft-delete
 */
const deletedMode = ref(false);

/**
 * BE deleted/restore hiện dành cho OWNER.
 *
 * Không thay route hoặc permission hiện tại.
 * Chỉ ẩn chức năng ở FE với role khác.
 */
const currentRole = computed(() =>
  String(localStorage.getItem("role") || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim()
);

const canManageDeletedProducts = computed(() => currentRole.value === "OWNER");

const currentPage = ref(Number(route.query.page) || 1);

const pageSize = ref(10);

const loading = ref(false);

const refreshingOnFocus = ref(false);

let hasActivatedOnce = false;

/**
 * Chỉ quyết định danh sách nào cần refresh.
 *
 * Không thay đổi dữ liệu Product.
 */
const fetchCurrentProductList = async () => {
  if (deletedMode.value) {
    await store.fetchDeletedProducts();
    return;
  }

  await store.fetchProducts();
};

const refreshProductsSilently = async () => {
  if (refreshingOnFocus.value) {
    return;
  }

  refreshingOnFocus.value = true;

  try {
    await Promise.all([store.fetchDropdowns(), fetchCurrentProductList()]);
  } finally {
    refreshingOnFocus.value = false;
  }
};

const handleWindowFocus = () => {
  void refreshProductsSilently();
};

const handleVisibilityChange = () => {
  if (document.visibilityState === "visible") {
    void refreshProductsSilently();
  }
};

onMounted(async () => {
  loading.value = true;

  try {
    /**
     * Trang mặc định luôn mở danh sách Product chưa xóa.
     */
    await Promise.all([store.fetchDropdowns(), store.fetchProducts()]);
  } finally {
    loading.value = false;
  }

  window.addEventListener("focus", handleWindowFocus);

  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onActivated(() => {
  if (!hasActivatedOnce) {
    hasActivatedOnce = true;
    return;
  }

  void refreshProductsSilently();
});

onBeforeUnmount(() => {
  window.removeEventListener("focus", handleWindowFocus);

  document.removeEventListener("visibilitychange", handleVisibilityChange);
});

/**
 * Kiểm tra master data có hoạt động hay không.
 *
 * Giữ nguyên logic cũ.
 */
const isItemActive = (item: any) => {
  if (!item) {
    return false;
  }

  if (
    item.isDeleted === true ||
    item.deleted === true ||
    item.is_deleted === true
  ) {
    return false;
  }

  if (item.status === 0 || item.status === "0" || item.status === false) {
    return false;
  }

  if (item.isActive === false || item.is_active === false) {
    return false;
  }

  return true;
};

const activeBrands = computed(() => store.brandList.filter(isItemActive));

const activeCategories = computed(() =>
  store.categoryList.filter(isItemActive)
);

const activeConcentrations = computed(() =>
  store.concentrationList.filter(isItemActive)
);

const activeCapacities = computed(() =>
  store.capacityList.filter(isItemActive)
);

const activeBottleTypes = computed(() =>
  store.bottleTypeList.filter(isItemActive)
);

const activeFragranceFamilies = computed(() =>
  store.fragranceFamilyList.filter(isItemActive)
);

/**
 * Chỉ đổi nguồn danh sách.
 *
 * Không filter status/stock/variant.
 */
const sourceProducts = computed(() =>
  deletedMode.value ? store.deletedProducts : store.products
);

const filteredData = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase();

  return sourceProducts.value.filter((item) => {
    /**
     * Giữ nguyên tìm kiếm hiện tại cho cả 2 tab.
     */
    if (keyword) {
      const text = [
        item.name,
        item.brandName,
        item.categoryName,
        item.concentrationName,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      if (!text.includes(keyword)) {
        return false;
      }
    }

    /**
     * Trạng thái chỉ lọc ở tab Sản phẩm.
     *
     * Tab Đã xóa chỉ mang ý nghĩa isDeleted = true,
     * nên không dùng Đang bán/Hết hàng/Ngừng bán để lọc.
     */
    if (!deletedMode.value && statusFilter.value !== "ALL") {
      const sellableQuantity = (item.variants ?? []).reduce(
        (sum, variant) =>
          sum + Number(variant.sellableQuantity ?? 0),
        0
      );

      const productStatus = Number(item.status);

      if (statusFilter.value === "ACTIVE") {
        return productStatus === 1 && sellableQuantity > 0;
      }

      if (statusFilter.value === "OUT_OF_STOCK") {
        return productStatus === 1 && sellableQuantity <= 0;
      }

      if (statusFilter.value === "INACTIVE") {
        return productStatus !== 1;
      }
    }

    return true;
  });
});

const totalPages = computed(
  () => Math.ceil(filteredData.value.length / pageSize.value) || 1
);

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;

  return filteredData.value.slice(start, start + pageSize.value);
});

watch(
  () => route.query.page,
  (page) => {
    currentPage.value = Number(page) || 1;
  }
);

watch(
  () => route.query.q,
  (q) => {
    searchQuery.value = String(q || "");
  }
);

watch(currentPage, () => {
  router.replace({
    query: {
      ...route.query,
      page: currentPage.value,
      q: searchQuery.value || undefined,
    },
  });
});

watch(searchQuery, () => {
  currentPage.value = 1;

  router.replace({
    query: {
      ...route.query,
      page: 1,
      q: searchQuery.value || undefined,
    },
  });
});

/**
 * Đổi filter chỉ reset phân trang.
 * Không gọi API và không thay đổi dữ liệu Product.
 */
watch(statusFilter, () => {
  currentPage.value = 1;
});

const refreshProducts = async () => {
  if (loading.value) {
    return;
  }

  loading.value = true;

  try {
    await Promise.all([store.fetchDropdowns(), fetchCurrentProductList()]);
  } finally {
    loading.value = false;
  }
};

/**
 * Chuyển giữa danh sách bình thường / đã xóa.
 */
const switchProductMode = async (deleted: boolean) => {
  if (deletedMode.value === deleted) {
    return;
  }

  if (deleted && !canManageDeletedProducts.value) {
    return;
  }

  deletedMode.value = deleted;
  currentPage.value = 1;

  loading.value = true;

  try {
    await fetchCurrentProductList();
  } finally {
    loading.value = false;
  }
};

const openAddModal = async () => {
  try {
    await store.fetchDropdowns();

    selectedProduct.value = null;
    isCloneMode.value = false;
    showModal.value = true;
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể mở form sản phẩm",
      text:
        error?.response?.data?.message ||
        "Không thể tải dữ liệu danh mục mới nhất",
    });
  }
};

const openEditModal = async (item: Product) => {
  try {
    const latest = await productService.getProductById(item.id);

    selectedProduct.value = latest;

    isCloneMode.value = false;

    showModal.value = true;
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể mở sản phẩm",
      text:
        error?.response?.data?.message ||
        "Không thể tải dữ liệu sản phẩm mới nhất",
    });

    await store.fetchProducts();
  }
};

const openCloneModal = (item: Product) => {
  selectedProduct.value = item;

  isCloneMode.value = true;

  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  isCloneMode.value = false;
};

const handleProductConflict = async (error: any) => {
  if (Number(error?.response?.status) !== 409) {
    return false;
  }

  await store.fetchProducts();

  await Swal.fire({
    icon: "warning",
    title: "Sản phẩm đã thay đổi",
    text:
      error?.response?.data?.message ||
      "Sản phẩm đã được thay đổi ở nơi khác. Danh sách mới nhất đã được tải lại, vui lòng kiểm tra và xác nhận lại.",
    confirmButtonText: "Đã hiểu",
    confirmButtonColor: "#2563eb",
  });

  return true;
};

/**
 * GIỮ NGUYÊN LOGIC UPDATE STATUS PRODUCT.
 */
const toggleProductStatus = async (product: Product, newStatus: number) => {
  const actionName = newStatus === 1 ? "Mở bán" : "Ngừng bán";

  const confirmColor = newStatus === 1 ? "#10b981" : "#f59e0b";

  const result = await Swal.fire({
    title: `Xác nhận ${actionName.toLowerCase()}?`,

    text: `Bạn có chắc chắn muốn chuyển trạng thái sản phẩm "${product.name}" thành "${actionName}" không?`,

    icon: "warning",

    showCancelButton: true,

    confirmButtonColor: confirmColor,

    confirmButtonText: actionName,

    cancelButtonText: "Hủy",
  });

  if (!result.isConfirmed) {
    return;
  }

  try {
    Swal.fire({
      title: "Đang xử lý...",
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading(),
    });

    const payload = {
      name: product.name,

      description: product.description,

      brandId: product.brandId || (product as any).brand?.id,

      categoryId: product.categoryId || (product as any).category?.id,

      concentrationId:
        product.concentrationId || (product as any).concentration?.id,

      gender: product.gender,

      isNiche: product.isNiche,

      status: newStatus,

      expectedRevision: product.revision ?? undefined,

      fragranceFamilyIds:
        product.fragranceFamilies?.map((f: any) => f.id) || [],

      variants:
        product.variants?.map((v: any) => ({
          id: v.id,

          capacityId: v.capacityId || v.capacity?.id || 0,

          bottleTypeId: v.bottleTypeId || v.bottleType?.id || 0,

          price: v.price,

          status: v.status,

          sku: v.sku ? String(v.sku).trim() : undefined,
        })) || [],
    };

    await productService.updateProduct(product.id, payload);

    Swal.fire({
      icon: "success",
      title: "Thành công",
      text: "Đã cập nhật trạng thái sản phẩm",
      timer: 1200,
      showConfirmButton: false,
    });

    await store.fetchProducts();
  } catch (error: any) {
    if (await handleProductConflict(error)) {
      return;
    }

    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: error?.response?.data?.message || "Không thể thực hiện thao tác",
    });
  }
};

/**
 * GIỮ NGUYÊN LOGIC UPDATE STATUS VARIANT.
 */
const handleToggleVariantStatus = async (
  product: Product,
  variant: ProductVariant
) => {
  const newStatus = variant.status === 1 ? 0 : 1;

  const actionName = newStatus === 1 ? "Mở bán" : "Ngừng bán";

  const confirmColor = newStatus === 1 ? "#10b981" : "#f59e0b";

  const result = await Swal.fire({
    title: `Xác nhận ${actionName.toLowerCase()} biến thể?`,

    text: `Bạn có muốn đổi trạng thái biến thể (${variant.capacityName}ml - ${variant.bottleTypeName}) thành "${actionName}" không?`,

    icon: "warning",
    showCancelButton: true,

    confirmButtonColor: confirmColor,

    confirmButtonText: actionName,

    cancelButtonText: "Hủy",
  });

  if (!result.isConfirmed) {
    return;
  }

  try {
    Swal.fire({
      title: "Đang xử lý...",
      allowOutsideClick: false,

      didOpen: () => Swal.showLoading(),
    });

    const payload = {
      name: product.name,

      description: product.description,

      brandId: product.brandId || (product as any).brand?.id,

      categoryId: product.categoryId || (product as any).category?.id,

      concentrationId:
        product.concentrationId || (product as any).concentration?.id,

      gender: product.gender,

      isNiche: product.isNiche,

      status: product.status,

      expectedRevision: product.revision ?? undefined,

      fragranceFamilyIds:
        product.fragranceFamilies?.map((f: any) => f.id) || [],

      variants:
        product.variants?.map((v: any) => ({
          id: v.id,

          capacityId: v.capacityId || v.capacity?.id || 0,

          bottleTypeId: v.bottleTypeId || v.bottleType?.id || 0,

          price: v.price,

          status: v.id === variant.id ? newStatus : v.status,

          sku: v.sku ? String(v.sku).trim() : undefined,
        })) || [],
    };

    await productService.updateProduct(product.id, payload);

    Swal.fire({
      icon: "success",
      title: "Thành công",
      text: "Đã cập nhật trạng thái biến thể",
      timer: 1200,
      showConfirmButton: false,
    });

    await store.fetchProducts();
  } catch (error: any) {
    if (await handleProductConflict(error)) {
      return;
    }

    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: error?.response?.data?.message || "Không thể cập nhật biến thể",
    });
  }
};

const handleStopSelling = (id: number) => {
  const product = store.products.find((p) => p.id === id);

  if (product) {
    toggleProductStatus(product, 0);
  }
};

const handleStartSelling = (id: number) => {
  const product = store.products.find((p) => p.id === id);

  if (product) {
    toggleProductStatus(product, 1);
  }
};

/**
 * Soft delete.
 */
const handleDelete = async (id: number) => {
  try {
    Swal.fire({
      title: "Đang xóa...",
      allowOutsideClick: false,

      didOpen: () => Swal.showLoading(),
    });

    await productService.deleteProduct(id);

    Swal.fire({
      icon: "success",
      title: "Đã xóa",
      text: "Sản phẩm đã được chuyển vào danh sách Đã xóa",
      timer: 1200,
      showConfirmButton: false,
    });

    await store.fetchProducts();
  } catch (error: any) {
    if (await handleProductConflict(error)) {
      return;
    }

    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: error?.response?.data?.message || "Không thể xóa sản phẩm",
    });
  }
};

/**
 * Restore Product.
 *
 * Không:
 * - set status = 1
 * - update Variant
 * - update kho
 */
const handleRestore = async (id: number) => {
  const product = store.deletedProducts.find((item) => item.id === id);

  if (!product) {
    return;
  }

  const result = await Swal.fire({
    title: "Khôi phục sản phẩm?",

    text: `Bạn có chắc chắn muốn khôi phục "${product.name}" không?`,

    icon: "question",

    showCancelButton: true,

    confirmButtonColor: "#059669",

    cancelButtonColor: "#64748b",

    confirmButtonText: "Khôi phục",

    cancelButtonText: "Hủy",
  });

  if (!result.isConfirmed) {
    return;
  }

  try {
    Swal.fire({
      title: "Đang khôi phục...",
      allowOutsideClick: false,

      didOpen: () => Swal.showLoading(),
    });

    await productService.restoreProduct(id);

    Swal.fire({
      icon: "success",
      title: "Đã khôi phục",
      text: "Sản phẩm đã được khôi phục thành công",
      timer: 1200,
      showConfirmButton: false,
    });

    await store.fetchDeletedProducts();
  } catch (error: any) {
    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: error?.response?.data?.message || "Không thể khôi phục sản phẩm",
    });
  }
};

const calculateTotalStock = (variants?: ProductVariant[]) =>
  variants?.reduce((sum, item) => sum + Number(item.totalQuantity ?? 0), 0) ??
  0;

const calculateTotalSellableStock = (variants?: ProductVariant[]) =>
  variants?.reduce(
    (sum, item) => sum + Number(item.sellableQuantity ?? 0),
    0
  ) ?? 0;

const getStockClass = (stock: number) => {
  if (stock === 0) {
    return "danger";
  }

  if (stock < 10) {
    return "warning";
  }

  return "success";
};

/**
 * Chỉ enrich dữ liệu hiển thị.
 *
 * Không thay status Product/Variant.
 */
const rows = computed(() => {
  return paginatedData.value.map((product) => {
    const variants = product.variants || [];

    const stock = calculateTotalStock(variants);

    const sellableStock = calculateTotalSellableStock(variants);

    return {
      ...product,

      variants,

      stock,

      sellableStock,

      stockClass: getStockClass(stock),

      sellableStockClass: getStockClass(sellableStock),
    };
  });
});
</script>

<template>
  <div class="product-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-box-seam me-2"></i>

          Quản lý nước hoa
        </h3>
      </div>

      <button class="btn btn-primary px-4" @click="openAddModal">
        <i class="bi bi-plus-circle me-2"></i>

        Thêm sản phẩm
      </button>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <!-- Chỉ OWNER có endpoint deleted/restore -->
        <div v-if="canManageDeletedProducts" class="view-tabs">
          <button
            type="button"
            class="view-tab"
            :class="{
              active: !deletedMode,
            }"
            @click="switchProductMode(false)"
          >
            Sản phẩm
          </button>

          <button
            type="button"
            class="view-tab"
            :class="{
              active: deletedMode,
            }"
            @click="switchProductMode(true)"
          >
            Đã xóa
          </button>
        </div>

        <div class="search-box">
          <i class="bi bi-search"></i>

          <input
            v-model="searchQuery"
            placeholder="Tìm theo tên, thương hiệu..."
          />
        </div>
      </div>

      <div class="toolbar-right">
        <select
          v-if="!deletedMode"
          v-model="statusFilter"
          class="form-select"
          style="width: 155px"
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang bán</option>
          <option value="OUT_OF_STOCK">Hết hàng</option>
          <option value="INACTIVE">Ngừng bán</option>
        </select>

        <select v-model="pageSize" class="form-select" style="width: 100px">
          <option :value="10">10</option>

          <option :value="20">20</option>

          <option :value="50">50</option>
        </select>

        <button
          class="btn btn-light"
          :disabled="loading"
          @click="refreshProducts"
        >
          <i class="bi bi-arrow-clockwise"></i>
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">Đang tải dữ liệu...</div>

    <div v-else-if="filteredData.length === 0" class="empty-state">
      {{ deletedMode ? "Không có sản phẩm đã xóa" : "Không tìm thấy sản phẩm" }}
    </div>

    <div v-else class="table-wrapper">
      <ProductList
        :paginated-data="rows"
        :deleted-mode="deletedMode"
        @edit="openEditModal"
        @clone="openCloneModal"
        @stop-selling="handleStopSelling"
        @start-selling="handleStartSelling"
        @toggle-variant-status="handleToggleVariantStatus"
        @delete="handleDelete"
        @restore="handleRestore"
      />
    </div>

    <div v-if="!loading" class="footer">
      <div class="text-muted">
        Hiển thị
        <b>
          {{ paginatedData.length }}
        </b>
        /
        <b>
          {{ filteredData.length }}
        </b>
      </div>

      <div class="pagination">
        <button
          class="btn btn-light"
          :disabled="currentPage === 1"
          @click="currentPage--"
        >
          ←
        </button>

        <span>
          {{ currentPage }}
          /
          {{ totalPages }}
        </span>

        <button
          class="btn btn-light"
          :disabled="currentPage === totalPages"
          @click="currentPage++"
        >
          →
        </button>
      </div>
    </div>

    <Teleport to="body">
      <ProductModal
        v-if="showModal"
        :product-selected="selectedProduct"
        :is-clone="isCloneMode"
        :brand-list="activeBrands"
        :category-list="activeCategories"
        :concentration-list="activeConcentrations"
        :fragrance-family-list="activeFragranceFamilies"
        :capacity-list="activeCapacities"
        :bottle-type-list="activeBottleTypes"
        @close="closeModal"
        @refresh="store.fetchProducts"
      />
    </Teleport>
  </div>
</template>

<style scoped>
.loading-state,
.empty-state {
  padding: 60px;
  text-align: center;
  color: #64748b;
  font-size: 15px;
}

.product-page {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 30px;
  border-bottom: 1px solid #eef2f7;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 30px;
  background: #fafafa;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.view-tabs {
  display: flex;
  gap: 6px;
}

.view-tab {
  border: 1px solid #e2e8f0;
  background: white;
  color: #64748b;
  padding: 9px 15px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.view-tab.active {
  background: #2563eb;
  border-color: #2563eb;
  color: white;
}

.search-box {
  width: 420px;
  position: relative;
}

.search-box i {
  position: absolute;
  top: 50%;
  left: 16px;
  transform: translateY(-50%);
  color: #94a3b8;
}

.search-box input {
  width: 100%;
  padding: 12px 18px 12px 45px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  transition: 0.25s;
}

.search-box input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.table-wrapper {
  padding: 20px 24px;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 28px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn {
  border-radius: 12px;
}

.btn-primary {
  border: none;
  background: #2563eb;
}

.btn-primary:hover {
  background: #1d4ed8;
}
</style>
