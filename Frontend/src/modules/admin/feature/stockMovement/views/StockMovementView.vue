<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";

import { useRoute } from "vue-router";

import Swal from "sweetalert2";

import StockMovementDetailModal from "../components/StockMovementDetailModal.vue";

import { useStockMovementStore } from "../stores/stock-movement.store";

import {
  REFERENCE_TYPE_OPTIONS,
  STOCK_MOVEMENT_TYPE_OPTIONS,
} from "../types/stock-movement.type";

import type {
  StockMovementListResponse,
  StockMovementType,
} from "../types/stock-movement.type";

const store = useStockMovementStore();
const route = useRoute();

const detailVisible = ref(false);

let searchTimer: ReturnType<typeof setTimeout> | null = null;

const autoFilterEnabled = ref(false);

/* =========================
   IMAGE PREVIEW
   ========================= */

const previewImageUrl = ref("");
const previewProductName = ref("");
const previewSku = ref("");

const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(imageUrl && !failedImageUrls.value.has(imageUrl));

const openImagePreview = (item: StockMovementListResponse) => {
  if (!hasUsableImage(item.imageUrl)) {
    return;
  }

  previewImageUrl.value = item.imageUrl!;
  previewProductName.value = item.productName || "Sản phẩm";
  previewSku.value = item.sku || "";
};

const closeImagePreview = () => {
  previewImageUrl.value = "";
  previewProductName.value = "";
  previewSku.value = "";
};

const markImageFailed = (imageUrl?: string | null) => {
  if (!imageUrl) {
    return;
  }

  const next = new Set(failedImageUrls.value);
  next.add(imageUrl);
  failedImageUrls.value = next;
};

const onImageError = (event: Event, imageUrl?: string | null) => {
  const image = event.currentTarget as HTMLImageElement;

  markImageFailed(imageUrl || image.currentSrc || image.src);
};

const onPreviewImageError = () => {
  markImageFailed(previewImageUrl.value);
  closeImagePreview();
};

/* ========================= */

const totalPages = computed(() => {
  const value = Number(store.totalPages);

  return Number.isFinite(value) && value > 0 ? value : 0;
});

const currentPage = computed(() => {
  const value = Number(store.page);

  return Number.isFinite(value) && value >= 0 ? value : 0;
});

const pageNumbers = computed(() => {
  const total = totalPages.value;

  if (total <= 0) {
    return [];
  }

  const maxVisible = 5;

  let start = Math.max(0, currentPage.value - Math.floor(maxVisible / 2));

  let end = Math.min(total - 1, start + maxVisible - 1);

  start = Math.max(0, end - maxVisible + 1);

  return Array.from(
    {
      length: end - start + 1,
    },
    (_, index) => start + index
  );
});

const pageStart = computed(() =>
  store.totalElements <= 0 ? 0 : currentPage.value * store.size + 1
);

const pageEnd = computed(() =>
  store.totalElements <= 0
    ? 0
    : Math.min((currentPage.value + 1) * store.size, store.totalElements)
);

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const formatCapacity = (value?: number | null) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return "";
  }

  return `${new Intl.NumberFormat("vi-VN", {
    maximumFractionDigits: 2,
  }).format(Number(value))} ml`;
};

const variantLabel = (item: {
  capacityValue?: number | null;
  bottleTypeName?: string | null;
}) => {
  const parts = [
    formatCapacity(item.capacityValue),
    String(item.bottleTypeName ?? "").trim(),
  ].filter(Boolean);

  return parts.join(" · ");
};

const formatSignedNumber = (value?: number | null) => {
  const numberValue = Number(value ?? 0);

  const formatted = formatNumber(Math.abs(numberValue));

  if (numberValue > 0) {
    return `+${formatted}`;
  }

  if (numberValue < 0) {
    return `-${formatted}`;
  }

  return "0";
};

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
};

const getErrorMessage = (error: any) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  store.error ||
  "Đã xảy ra lỗi.";

const movementTypeLabel = (item: StockMovementListResponse) => {
  if (item.movementTypeLabel) {
    return item.movementTypeLabel;
  }

  const option = STOCK_MOVEMENT_TYPE_OPTIONS.find(
    (movement) => movement.value === item.movementType
  );

  return option?.label || item.movementType || "Không xác định";
};

const movementClass = (item: StockMovementListResponse) => {
  if (item.quantityChange > 0) {
    return "movement-in";
  }

  if (item.quantityChange < 0) {
    return "movement-out";
  }

  return "movement-neutral";
};

const referenceTypeLabel = (value?: string | null) => {
  if (!value) {
    return "—";
  }

  const option = REFERENCE_TYPE_OPTIONS.find((item) => item.value === value);

  return option?.label || value;
};

const validatePositiveOptionalId = async (
  value: number | null,
  label: string
): Promise<boolean> => {
  if (value === null) {
    return true;
  }

  if (!Number.isInteger(value) || value <= 0) {
    await Swal.fire({
      icon: "warning",
      title: `${label} không hợp lệ`,
      text: `${label} phải là số nguyên lớn hơn 0.`,
    });

    return false;
  }

  return true;
};

const validateFilters = async (): Promise<boolean> => {
  if (!(await validatePositiveOptionalId(store.inventoryLotId, "ID lô"))) {
    return false;
  }

  if (
    !(await validatePositiveOptionalId(store.createdBy, "ID người thao tác"))
  ) {
    return false;
  }

  if (!(await validatePositiveOptionalId(store.referenceId, "ID chứng từ"))) {
    return false;
  }

  if (store.fromDate && store.toDate && store.fromDate > store.toDate) {
    await Swal.fire({
      icon: "warning",
      title: "Khoảng thời gian không hợp lệ",
      text: "Từ ngày không được lớn hơn đến ngày.",
    });

    return false;
  }

  return true;
};

const loadList = async () => {
  if (!(await validateFilters())) {
    return;
  }

  try {
    await store.fetchList();
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể tải lịch sử kho",
      text: getErrorMessage(error),
    });
  }
};

const search = async () => {
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }

  store.page = 0;
  await loadList();
};

const resetFilters = async () => {
  autoFilterEnabled.value = false;

  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }

  store.resetFilters();

  await nextTick();
  await loadList();

  autoFilterEnabled.value = true;
};

const changePageSize = async (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value);

  if (![10, 20, 50, 100].includes(value)) {
    return;
  }

  store.size = value;
  store.page = 0;

  await loadList();
};

const goToPage = async (page: number) => {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) {
    return;
  }

  store.page = page;
  await loadList();
};

const previousPage = async () => {
  if (currentPage.value <= 0) {
    return;
  }

  store.page--;
  await loadList();
};

const nextPage = async () => {
  if (currentPage.value + 1 >= totalPages.value) {
    return;
  }

  store.page++;
  await loadList();
};

const openDetail = async (id: number) => {
  detailVisible.value = true;

  try {
    await store.fetchDetail(id);
  } catch (error) {
    detailVisible.value = false;

    await Swal.fire({
      icon: "error",
      title: "Không thể tải chi tiết biến động",
      text: getErrorMessage(error),
    });
  }
};

const closeDetail = () => {
  detailVisible.value = false;
  store.clearDetail();
};

const applyRouteFilters = () => {
  const rawLotId = Array.isArray(route.query.inventoryLotId)
    ? route.query.inventoryLotId[0]
    : route.query.inventoryLotId;

  if (rawLotId) {
    const parsed = Number(rawLotId);

    if (Number.isInteger(parsed) && parsed > 0) {
      store.inventoryLotId = parsed;
    }
  }

  const rawMovementType = Array.isArray(route.query.movementType)
    ? route.query.movementType[0]
    : route.query.movementType;

  if (
    rawMovementType &&
    STOCK_MOVEMENT_TYPE_OPTIONS.some(
      (option) => option.value === rawMovementType
    )
  ) {
    store.movementType = rawMovementType as StockMovementType;
  }

  const rawReferenceType = Array.isArray(route.query.referenceType)
    ? route.query.referenceType[0]
    : route.query.referenceType;

  if (rawReferenceType) {
    store.referenceType = String(rawReferenceType).trim();
  }

  const rawReferenceId = Array.isArray(route.query.referenceId)
    ? route.query.referenceId[0]
    : route.query.referenceId;

  if (rawReferenceId) {
    const parsed = Number(rawReferenceId);

    if (Number.isInteger(parsed) && parsed > 0) {
      store.referenceId = parsed;
    }
  }
};

watch(
  () => store.keyword,
  () => {
    if (!autoFilterEnabled.value) {
      return;
    }

    if (searchTimer) {
      clearTimeout(searchTimer);
    }

    searchTimer = setTimeout(async () => {
      store.page = 0;
      await loadList();
      searchTimer = null;
    }, 400);
  }
);

watch(
  [
    () => store.movementType,
    () => store.referenceType,
    () => store.fromDate,
    () => store.toDate,
  ],
  async () => {
    if (!autoFilterEnabled.value) {
      return;
    }

    if (store.fromDate && store.toDate && store.fromDate > store.toDate) {
      return;
    }

    store.page = 0;
    await loadList();
  }
);

onMounted(async () => {
  applyRouteFilters();

  await loadList();

  autoFilterEnabled.value = true;
});

onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
});
</script>

<template>
  <div class="stock-movement-page">
    <div class="page-header">
      <div>
        <h1>Lịch sử kho</h1>

        <p>
          Theo dõi toàn bộ biến động tăng/giảm tồn theo SKU, lô, chứng từ và
          người thao tác.
        </p>
      </div>
    </div>

    <div class="content-card">
      <div class="filters">
        <div class="search-box">
          <i class="bi bi-search"></i>

          <input
            v-model="store.keyword"
            type="text"
            maxlength="150"
            placeholder="Tìm SKU, tên sản phẩm hoặc mã lô..."
            @keyup.enter="search"
          />
        </div>

        <select v-model="store.movementType">
          <option value="">Tất cả biến động</option>

          <option
            v-for="option in STOCK_MOVEMENT_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>

        <div class="date-filter">
          <label>
            <span>Từ ngày</span>

            <input v-model="store.fromDate" type="date" />
          </label>

          <label>
            <span>Đến ngày</span>

            <input v-model="store.toDate" type="date" />
          </label>
        </div>

        <button
          type="button"
          class="reset-btn"
          :disabled="store.loadingList"
          @click="resetFilters"
        >
          <i class="bi bi-arrow-counterclockwise"></i>

          Đặt lại
        </button>
      </div>

      <div class="table-toolbar">
        <div>
          <h3>Biến động tồn kho</h3>

          <p>
            Mỗi dòng là một lần tăng hoặc giảm tồn đã được hệ thống ghi nhận.
          </p>
        </div>

        <div class="size-control">
          <span>Hiển thị</span>

          <select :value="store.size" @change="changePageSize">
            <option :value="10">10</option>

            <option :value="20">20</option>

            <option :value="50">50</option>

            <option :value="100">100</option>
          </select>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th class="image-column">Ảnh</th>

              <th>Mã lô</th>

              <th>SKU / Sản phẩm</th>

              <th>Loại biến động</th>

              <th>Thay đổi</th>

              <th>Tồn trước</th>

              <th>Tồn sau</th>

              <th>Chứng từ nguồn</th>

              <th>Lý do</th>

              <th>Người thao tác</th>

              <th>Thời gian biến động</th>

              <th>Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="store.loadingList">
              <td colspan="12" class="state-row">
                <span class="spinner-border spinner-border-sm"></span>

                Đang tải lịch sử kho...
              </td>
            </tr>

            <tr v-else-if="store.movements.length === 0">
              <td colspan="12" class="state-row">
                <i class="bi bi-inbox"></i>

                <div>Không có biến động phù hợp.</div>
              </td>
            </tr>

            <tr v-for="item in store.movements" v-else :key="item.id">
              <!-- IMAGE -->
              <td class="image-cell">
                <button
                  type="button"
                  class="product-thumb"
                  :class="{
                    clickable: hasUsableImage(item.imageUrl),
                  }"
                  :disabled="!hasUsableImage(item.imageUrl)"
                  :title="
                    hasUsableImage(item.imageUrl)
                      ? 'Bấm để xem ảnh lớn'
                      : 'Sản phẩm chưa có ảnh'
                  "
                  @click="openImagePreview(item)"
                >
                  <i class="bi bi-image"></i>

                  <img
                    v-if="hasUsableImage(item.imageUrl)"
                    :src="item.imageUrl || ''"
                    :alt="item.productName"
                    loading="lazy"
                    @error="onImageError($event, item.imageUrl)"
                  />
                </button>
              </td>

              <!-- LOT -->
              <td>
                <strong>
                  {{ item.lotCode }}
                </strong>
              </td>

              <!-- SKU / PRODUCT -->
              <td>
                <strong class="sku">
                  {{ item.sku }}
                </strong>

                <span class="product-name" :title="item.productName">
                  {{ item.productName }}
                </span>

                <span
                  v-if="variantLabel(item)"
                  class="variant-info"
                >
                  {{ variantLabel(item) }}
                </span>
              </td>

              <!-- MOVEMENT -->
              <td>
                <span class="movement-badge" :class="movementClass(item)">
                  {{ movementTypeLabel(item) }}
                </span>
              </td>

              <!-- CHANGE -->
              <td>
                <strong class="quantity-change" :class="movementClass(item)">
                  {{ formatSignedNumber(item.quantityChange) }}
                </strong>
              </td>

              <!-- BEFORE -->
              <td>
                {{ formatNumber(item.quantityBefore) }}
              </td>

              <!-- AFTER -->
              <td>
                <strong>
                  {{ formatNumber(item.quantityAfter) }}
                </strong>
              </td>

              <!-- REFERENCE -->
              <td>
                <div class="reference-cell">
                  <span>
                    {{ referenceTypeLabel(item.referenceType) }}
                  </span>
                </div>
              </td>

              <!-- REASON -->
              <td class="reason-cell" :title="item.reason || ''">
                {{ item.reason || "—" }}
              </td>

              <!-- USER -->
              <td>
                <strong>
                  {{ item.createdByName || "—" }}
                </strong>
              </td>

              <!-- TIME -->
              <td class="time-cell">
                {{ formatDateTime(item.createdAt) }}
              </td>

              <!-- ACTION -->
              <td>
                <div class="actions">
                  <button
                    type="button"
                    title="Xem chi tiết biến động"
                    @click="openDetail(item.id)"
                  >
                    <i class="bi bi-eye"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <div class="pagination-info">
          Hiển thị

          <strong> {{ pageStart }}-{{ pageEnd }} </strong>

          /

          <strong>
            {{ store.totalElements }}
          </strong>

          bản ghi

          <span>
            Trang

            <strong>
              {{ totalPages > 0 ? currentPage + 1 : 0 }}
            </strong>

            /

            <strong>
              {{ totalPages }}
            </strong>
          </span>
        </div>

        <div class="page-controls">
          <button
            type="button"
            :disabled="currentPage <= 0 || store.loadingList"
            @click="previousPage"
          >
            Trước
          </button>

          <button
            v-for="page in pageNumbers"
            :key="page"
            type="button"
            class="page-number"
            :class="{
              active: page === currentPage,
            }"
            :disabled="page === currentPage"
            @click="goToPage(page)"
          >
            {{ page + 1 }}
          </button>

          <button
            type="button"
            :disabled="
              totalPages === 0 ||
              currentPage + 1 >= totalPages ||
              store.loadingList
            "
            @click="nextPage"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <StockMovementDetailModal
      :visible="detailVisible"
      :detail="store.detail"
      :loading="store.loadingDetail"
      @close="closeDetail"
    />
  </div>

  <!-- IMAGE PREVIEW -->
  <Teleport to="body">
    <div
      v-if="previewImageUrl"
      class="image-preview-backdrop"
      @click.self="closeImagePreview"
    >
      <div class="image-preview-dialog">
        <button
          type="button"
          class="image-preview-close"
          aria-label="Đóng ảnh"
          @click="closeImagePreview"
        >
          <i class="bi bi-x-lg"></i>
        </button>

        <img
          :src="previewImageUrl"
          :alt="previewProductName"
          class="image-preview-img"
          @error="onPreviewImageError"
        />

        <div class="image-preview-info">
          <strong>
            {{ previewProductName }}
          </strong>

          <span v-if="previewSku">
            {{ previewSku }}
          </span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.stock-movement-page {
  min-height: 100%;
  padding: 24px;
  background: #f7f7f8;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 6px;
  color: #222;
  font-size: 26px;
  font-weight: 700;
}

.page-header p {
  margin: 0;
  color: #777;
  font-size: 14px;
}

.readonly-note {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border: 1px solid #bfdbfe;
  border-radius: 9px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 13px;
  white-space: nowrap;
}

.content-card {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.filters {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 9px;
  margin-bottom: 18px;
}

.search-box {
  position: relative;
  display: flex;
  flex: 1 1 330px;
  min-width: 280px;
}

.search-box i {
  position: absolute;
  top: 50%;
  left: 12px;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}

.search-box input {
  width: 100%;
  padding-left: 36px !important;
}

.filters input,
.filters select,
.reset-btn {
  box-sizing: border-box;
  height: 40px;
  border-radius: 8px;
  font-size: 13px;
}

.filters input,
.filters select {
  padding: 0 11px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  outline: none;
}

.filters input:focus,
.filters select:focus {
  border-color: #6b7280;
}

.filters > select {
  min-width: 165px;
}

.reference-type-filter input {
  width: 170px;
}

.date-filter {
  display: flex;
  align-items: flex-end;
  gap: 7px;
}

.date-filter label {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.date-filter label > span {
  color: #6b7280;
  font-size: 11px;
}

.date-filter input {
  width: 145px;
}

.reset-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  cursor: pointer;
}

.table-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 12px;
}

.table-toolbar h3 {
  margin: 0 0 4px;
  color: #111827;
  font-size: 17px;
  font-weight: 700;
}

.table-toolbar p {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
}

.size-control {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #6b7280;
  font-size: 12px;
}

.size-control select {
  height: 35px;
  padding: 0 9px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  background: #fff;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

table {
  width: 100%;
  min-width: 1580px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
  vertical-align: middle;
  font-size: 13px;
}

th {
  background: #f9fafb;
  color: #4b5563;
  font-weight: 650;
  white-space: nowrap;
}

tbody tr:hover {
  background: #fafafa;
}

tbody tr:last-child td {
  border-bottom: 0;
}

/* =========================
   PRODUCT IMAGE
   ========================= */

.image-column {
  width: 76px;
  min-width: 76px;
  text-align: center;
}

.image-cell {
  width: 76px;
  min-width: 76px;
  text-align: center;
}

.product-thumb {
  position: relative;
  width: 52px;
  height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 9px;
  background: #f8f9fa;
  color: #9ca3af;
  cursor: default;
}

.product-thumb:disabled {
  opacity: 1;
}

.product-thumb.clickable {
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease,
    transform 0.15s ease;
}

.product-thumb.clickable:hover {
  border-color: #b6b6b6;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.12);
  transform: scale(1.04);
}

.product-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}

.product-thumb i {
  font-size: 18px;
}

/* ========================= */

.time-cell {
  white-space: nowrap;
}

.sku {
  display: block;
  color: #111827;
  white-space: nowrap;
}

.product-name {
  display: block;
  margin-top: 3px;
  max-width: 220px;
  overflow: hidden;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.variant-info {
  display: block;
  margin-top: 3px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
}


td small {
  display: block;
  margin-top: 3px;
  color: #9ca3af;
  font-size: 11px;
}

.movement-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
}

.movement-in {
  color: #047857 !important;
}

.movement-badge.movement-in {
  background: #ecfdf5;
}

.movement-out {
  color: #b91c1c !important;
}

.movement-badge.movement-out {
  background: #fef2f2;
}

.movement-neutral {
  color: #4b5563 !important;
}

.movement-badge.movement-neutral {
  background: #f3f4f6;
}

.quantity-change {
  font-size: 14px;
}

.reference-cell span {
  display: block;
  white-space: nowrap;
}

.reason-cell {
  max-width: 240px;
  overflow: hidden;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions {
  display: flex;
  align-items: center;
  gap: 5px;
}

.actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #374151;
  cursor: pointer;
}

.actions button:hover {
  background: #f9fafb;
}

.filters button:disabled,
.actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.state-row {
  padding: 32px;
  color: #6b7280;
  text-align: center;
}

.state-row i {
  display: block;
  margin-bottom: 6px;
  font-size: 22px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  color: #666;
  font-size: 13px;
}

.pagination-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
}

.pagination-info > span {
  margin-left: 10px;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 5px;
}

.page-controls button {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #374151;
  cursor: pointer;
}

.page-controls button:hover:not(:disabled) {
  background: #f3f4f6;
}

.page-controls .page-number.active {
  border-color: #222;
  background: #222;
  color: #fff;
}

.page-controls button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* =========================
   IMAGE PREVIEW
   ========================= */

.image-preview-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100001;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.72);
}

.image-preview-dialog {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(760px, 100%);
  max-height: calc(100vh - 48px);
  padding: 18px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.28);
}

.image-preview-close {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #333;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.image-preview-close:hover {
  background: #f3f4f6;
}

.image-preview-img {
  display: block;
  max-width: 100%;
  max-height: calc(100vh - 170px);
  object-fit: contain;
  border-radius: 10px;
}

.image-preview-info {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  text-align: center;
}

.image-preview-info strong {
  color: #333;
  font-size: 14px;
}

.image-preview-info span {
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 900px) {
  .page-header,
  .table-toolbar,
  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .date-filter {
    width: 100%;
  }

  .date-filter label {
    flex: 1;
  }

  .date-filter input {
    width: 100%;
  }
}

@media (max-width: 700px) {
  .stock-movement-page {
    padding: 14px;
  }

  .search-box {
    min-width: 100%;
  }

  .filters > select,
  .reference-type-filter,
  .reference-type-filter input {
    width: 100%;
  }

  .date-filter {
    flex-direction: column;
    align-items: stretch;
  }

  .image-preview-backdrop {
    padding: 12px;
  }

  .image-preview-dialog {
    padding: 12px;
  }

  .image-preview-img {
    max-height: calc(100vh - 140px);
  }
}
</style>
