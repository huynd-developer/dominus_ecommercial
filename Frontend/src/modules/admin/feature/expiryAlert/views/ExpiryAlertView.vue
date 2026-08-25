<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import Swal from "sweetalert2";

import ExpiryAlertDetailModal from "../components/ExpiryAlertDetailModal.vue";

import { useExpiryAlertStore } from "../stores/expiry-alert.store";

import type {
  ExpiryAlertGroup,
  ExpiryAlertListResponse,
} from "../types/expiry-alert.type";

const store = useExpiryAlertStore();

const detailVisible = ref(false);

const fromDaysInput = ref(
  store.fromDays === null ? "" : String(store.fromDays)
);
const toDaysInput = ref(
  store.toDays === null ? "" : String(store.toDays)
);


const groupOptions: Array<{
  value: ExpiryAlertGroup;
  label: string;
}> = [
  {
    value: "NEAR_EXPIRY",
    label: "Sắp hết hạn",
  },
  {
    value: "EXPIRED",
    label: "Đã hết hạn",
  },
  {
    value: "ALL",
    label: "Tất cả cảnh báo",
  },
];

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

const formatDate = (value?: string | null) => {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`));
};

const getErrorMessage = (error: any) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  store.error ||
  "Đã xảy ra lỗi.";

const expiryLabel = (item: ExpiryAlertListResponse) => {
  if (item.isExpired) {
    const days = Math.abs(item.daysToExpiry);

    return days === 0 ? "Đã hết hạn" : `Hết hạn ${days} ngày`;
  }

  if (item.isNearExpiry) {
    if (item.daysToExpiry === 0) {
      return "Hết hạn hôm nay";
    }

    return `Còn ${item.daysToExpiry} ngày`;
  }

  return `Còn ${item.daysToExpiry} ngày`;
};

const expiryClass = (item: ExpiryAlertListResponse) => {
  if (item.isExpired) {
    return "status-expired";
  }

  if (item.isNearExpiry) {
    return "status-near";
  }

  return "status-normal";
};

const blockInvalidDayKey = (event: KeyboardEvent) => {
  const input = event.target as HTMLInputElement;

  const controlKeys = [
    "Backspace",
    "Delete",
    "Tab",
    "ArrowLeft",
    "ArrowRight",
    "ArrowUp",
    "ArrowDown",
    "Home",
    "End",
    "Enter",
  ];

  if (controlKeys.includes(event.key)) {
    return;
  }

  if (
    (event.ctrlKey || event.metaKey) &&
    ["a", "c", "v", "x"].includes(event.key.toLowerCase())
  ) {
    return;
  }

  if (/^\d$/.test(event.key)) {
    return;
  }

  if (
    event.key === "-" &&
    !input.value.includes("-") &&
    input.selectionStart === 0
  ) {
    return;
  }

  event.preventDefault();
};

const sanitizeDayInput = (rawValue: string) => {
  const value = rawValue.trim();

  if (value === "") {
    return "";
  }

  const negative = value.startsWith("-");
  const digits = value.replace(/\D/g, "");

  if (digits === "") {
    return negative ? "-" : "";
  }

  return negative ? `-${digits}` : digits;
};

const setFromDays = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const value = sanitizeDayInput(input.value);

  input.value = value;
  fromDaysInput.value = value;

  store.fromDays =
    value === "" || value === "-"
      ? null
      : Number(value);
};

const setToDays = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const value = sanitizeDayInput(input.value);

  input.value = value;
  toDaysInput.value = value;

  store.toDays =
    value === "" || value === "-"
      ? null
      : Number(value);
};

const validateFilters = async (): Promise<boolean> => {
  if (store.fromDays !== null && !Number.isInteger(store.fromDays)) {
    await Swal.fire({
      icon: "warning",
      title: "Số ngày không hợp lệ",
      text: "Số ngày bắt đầu phải là số nguyên.",
    });

    return false;
  }

  if (store.toDays !== null && !Number.isInteger(store.toDays)) {
    await Swal.fire({
      icon: "warning",
      title: "Số ngày không hợp lệ",
      text: "Số ngày kết thúc phải là số nguyên.",
    });

    return false;
  }

  if (
    store.fromDays !== null &&
    store.toDays !== null &&
    store.fromDays > store.toDays
  ) {
    await Swal.fire({
      icon: "warning",
      title: "Khoảng ngày không hợp lệ",
      text: "Số ngày bắt đầu không được lớn hơn số ngày kết thúc.",
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
      title: "Không thể tải cảnh báo",
      text: getErrorMessage(error),
    });
  }
};

const loadSummary = async () => {
  try {
    await store.fetchSummary();
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể tải tổng hợp cảnh báo",
      text: getErrorMessage(error),
    });
  }
};

const loadPage = async () => {
  await Promise.all([loadList(), loadSummary()]);
};

const search = async () => {
  store.page = 0;

  await loadList();
};

const changeGroup = async (group: ExpiryAlertGroup) => {
  store.setGroup(group);

  await loadList();
};

const changeGroupFromSelect = async (event: Event) => {
  const group = (event.target as HTMLSelectElement).value as ExpiryAlertGroup;

  await changeGroup(group);
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

const resetFilters = async () => {
  store.resetFilters();
  fromDaysInput.value = "";
  toDaysInput.value = "";

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
      title: "Không thể tải chi tiết lô",
      text: getErrorMessage(error),
    });
  }
};

const closeDetail = () => {
  detailVisible.value = false;

  store.clearDetail();
};


const previewImageUrl = ref("");
const previewProductName = ref("");
const previewSku = ref("");
const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(imageUrl && !failedImageUrls.value.has(imageUrl));

const openImagePreview = (
  imageUrl?: string | null,
  productName?: string | null,
  sku?: string | null
) => {
  if (!hasUsableImage(imageUrl)) {
    return;
  }

  previewImageUrl.value = imageUrl!;
  previewProductName.value = productName || "Sản phẩm";
  previewSku.value = sku || "";
};

const closeImagePreview = () => {
  previewImageUrl.value = "";
  previewProductName.value = "";
  previewSku.value = "";
};

const onImageError = (event: Event) => {
  const image = event.currentTarget as HTMLImageElement;
  const src = image.currentSrc || image.src;

  if (src) {
    const next = new Set(failedImageUrls.value);
    next.add(src);
    failedImageUrls.value = next;
  }
};

const onPreviewImageError = () => {
  if (previewImageUrl.value) {
    const next = new Set(failedImageUrls.value);
    next.add(previewImageUrl.value);
    failedImageUrls.value = next;
  }

  closeImagePreview();
};

onMounted(loadPage);
</script>

<template>
  <div class="expiry-alert-page">
    <!-- PAGE HEADER -->
    <div class="page-header">
      <div>
        <h1>Cảnh báo hạn sử dụng</h1>

        <p>Theo dõi các lô sắp hết hạn và đã hết hạn.</p>
      </div>
    </div>

    <!-- SUMMARY -->
    <div class="summary-grid">
      <button
        type="button"
        class="summary-card near"
        :class="{
          active: store.group === 'NEAR_EXPIRY',
        }"
        @click="changeGroup('NEAR_EXPIRY')"
      >
        <div class="summary-icon">
          <i class="bi bi-clock-history"></i>
        </div>

        <div>
          <span> Sắp hết hạn </span>

          <strong>
            {{ formatNumber(store.summary?.nearExpiryLotCount) }}
            lô
          </strong>

          <small>
            {{ formatNumber(store.summary?.nearExpiryQuantity) }}
            sản phẩm
          </small>
        </div>
      </button>

      <button
        type="button"
        class="summary-card expired"
        :class="{
          active: store.group === 'EXPIRED',
        }"
        @click="changeGroup('EXPIRED')"
      >
        <div class="summary-icon">
          <i class="bi bi-calendar-x"></i>
        </div>

        <div>
          <span> Đã hết hạn </span>

          <strong>
            {{ formatNumber(store.summary?.expiredLotCount) }}
            lô
          </strong>

          <small>
            {{ formatNumber(store.summary?.expiredQuantity) }}
            sản phẩm
          </small>
        </div>
      </button>

      <div class="summary-card config">
        <div class="summary-icon">
          <i class="bi bi-bell"></i>
        </div>

        <div>
          <span> Ngưỡng cảnh báo </span>

          <strong>
            {{ formatNumber(store.summary?.warningDays) }}
            ngày
          </strong>

          <small> Trước hạn sử dụng </small>
        </div>
      </div>
    </div>

    <!-- CONTENT -->
    <div class="content-card">
      <!-- FILTER -->
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

        <select :value="store.group" @change="changeGroupFromSelect">
          <option
            v-for="option in groupOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>

        <div class="days-filter">
          <input
            :value="fromDaysInput"
            type="text"
            pattern="-?[0-9]*"
            autocomplete="off"
            placeholder="Từ ngày"
            title="Có thể nhập số âm để lọc lô đã hết hạn"
            @keydown="blockInvalidDayKey"
            @input="setFromDays"
            @keyup.enter="search"
          />

          <span>đến</span>

          <input
            :value="toDaysInput"
            type="text"
            pattern="-?[0-9]*"
            autocomplete="off"
            placeholder="Đến ngày"
            title="Có thể nhập số âm để lọc lô đã hết hạn"
            @keydown="blockInvalidDayKey"
            @input="setToDays"
            @keyup.enter="search"
          />
        </div>

        <button
          type="button"
          class="filter-btn"
          :disabled="store.loadingList"
          @click="search"
        >
          <i class="bi bi-funnel"></i>

          Lọc
        </button>

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

      <!-- GROUP TITLE -->
      <div class="table-toolbar">
        <div>
          <h3>
            <template v-if="store.group === 'NEAR_EXPIRY'">
              Lô sắp hết hạn
            </template>

            <template v-else-if="store.group === 'EXPIRED'">
              Lô đã hết hạn
            </template>

            <template v-else> Tất cả cảnh báo </template>
          </h3>

          <p>
            <template v-if="store.group === 'NEAR_EXPIRY'">
              Các lô còn tối đa
              <strong>
                {{ store.summary?.warningDays ?? 0 }}
              </strong>
              ngày trước HSD.
            </template>

            <template v-else-if="store.group === 'EXPIRED'">
              Các lô đã quá hạn sử dụng và vẫn còn tồn kho.
            </template>

            <template v-else> Tổng hợp các lô cần chú ý. </template>
          </p>
        </div>

        <div class="size-control">
          <span> Hiển thị </span>

          <select :value="store.size" @change="changePageSize">
            <option :value="10">10</option>

            <option :value="20">20</option>

            <option :value="50">50</option>

            <option :value="100">100</option>
          </select>
        </div>
      </div>

      <!-- TABLE -->
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th class="image-column">Ảnh</th>
              <th>Mã lô</th>
              <th>SKU</th>
              <th>Sản phẩm</th>
              <th>Tồn hiện tại</th>
              <th>Có thể bán</th>
              <th>Hạn sử dụng</th>
              <th>Còn lại</th>
              <th>HSD</th>
              <th>Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="store.loadingList">
              <td colspan="10" class="state-row">
                <span class="spinner-border spinner-border-sm"></span>

                Đang tải dữ liệu...
              </td>
            </tr>

            <tr v-else-if="store.alerts.length === 0">
              <td colspan="10" class="state-row">
                <i class="bi bi-inbox"></i>

                <div>Không có lô phù hợp.</div>
              </td>
            </tr>

            <tr v-for="item in store.alerts" v-else :key="item.id">
              <td class="image-cell">
                <button
                  type="button"
                  class="product-thumb"
                  :class="{ clickable: hasUsableImage(item.imageUrl) }"
                  :disabled="!hasUsableImage(item.imageUrl)"
                  :title="
                    hasUsableImage(item.imageUrl)
                      ? 'Bấm để xem ảnh lớn'
                      : 'Sản phẩm chưa có ảnh'
                  "
                  @click="
                    openImagePreview(
                      item.imageUrl,
                      item.productName,
                      item.sku
                    )
                  "
                >
                  <i class="bi bi-image"></i>

                  <img
                    v-if="hasUsableImage(item.imageUrl)"
                    :src="item.imageUrl || ''"
                    :alt="item.productName"
                    loading="lazy"
                    @error="onImageError"
                  />
                </button>
              </td>

              <td>
                <button
                  type="button"
                  class="lot-link"
                  @click="openDetail(item.id)"
                >
                  {{ item.lotCode }}
                </button>
              </td>

              <td>
                <strong class="sku">
                  {{ item.sku }}
                </strong>
              </td>

              <td>
                <div class="product-cell">
                  <strong>{{ item.productName }}</strong>

                  <span
                    v-if="variantLabel(item)"
                    class="variant-info"
                  >
                    {{ variantLabel(item) }}
                  </span>
                </div>
              </td>

              <td>
                {{ formatNumber(item.quantityOnHand) }}
              </td>

              <td>
                <strong
                  :class="
                    item.sellableQuantity > 0
                      ? 'sellable-positive'
                      : 'sellable-zero'
                  "
                >
                  {{ formatNumber(item.sellableQuantity) }}
                </strong>
              </td>

              <td>
                {{ formatDate(item.expirationDate) }}
              </td>

              <td>
                <strong
                  :class="{
                    'days-expired': item.daysToExpiry < 0,
                    'days-near': item.daysToExpiry >= 0 && item.isNearExpiry,
                  }"
                >
                  {{ item.daysToExpiry }}
                  ngày
                </strong>
              </td>

              <td>
                <span class="status-badge" :class="expiryClass(item)">
                  {{ expiryLabel(item) }}
                </span>
              </td>

              <td>
                <div class="actions">
                  <!-- XEM CHI TIẾT -->
                  <button
                    type="button"
                    title="Xem chi tiết lô"
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

      <!-- PAGINATION -->
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

    <ExpiryAlertDetailModal
      :visible="detailVisible"
      :detail="store.detail"
      :loading="store.loadingDetail"
      @close="closeDetail"
    />

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
          <strong>{{ previewProductName }}</strong>
          <span v-if="previewSku">{{ previewSku }}</span>
        </div>
      </div>
    </div>
    </Teleport>
  </div>
</template>

<style scoped>
.expiry-alert-page {
  min-height: 100%;

  padding: 24px;

  background: #f7f7f8;
}

/* HEADER */

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


/* SUMMARY */

.summary-grid {
  display: grid;

  grid-template-columns: repeat(3, minmax(0, 1fr));

  gap: 14px;

  margin-bottom: 18px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;

  min-height: 108px;

  padding: 17px;

  border: 1px solid #e5e7eb;
  border-radius: 14px;

  background: #fff;

  text-align: left;

  transition: border-color 0.15s ease, box-shadow 0.15s ease,
    transform 0.15s ease;
}

button.summary-card {
  cursor: pointer;
}

button.summary-card:hover {
  transform: translateY(-1px);

  box-shadow: 0 5px 18px rgba(15, 23, 42, 0.06);
}

.summary-card.active {
  border-color: #111827;

  box-shadow: 0 0 0 1px #111827;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;

  flex: 0 0 46px;

  width: 46px;
  height: 46px;

  border-radius: 12px;

  font-size: 20px;
}

.summary-card.near .summary-icon {
  background: #fff7ed;
  color: #c2410c;
}

.summary-card.expired .summary-icon {
  background: #fef2f2;
  color: #b91c1c;
}


.summary-card.config .summary-icon {
  background: #eff6ff;
  color: #1d4ed8;
}

.summary-card span {
  display: block;

  margin-bottom: 5px;

  color: #6b7280;

  font-size: 12px;
}

.summary-card strong {
  display: block;

  color: #111827;

  font-size: 20px;
  font-weight: 750;
}

.summary-card small {
  display: block;

  margin-top: 3px;

  color: #9ca3af;

  font-size: 12px;
}

/* CONTENT */

.content-card {
  padding: 18px;

  border: 1px solid #e5e7eb;
  border-radius: 14px;

  background: #fff;
}

/* FILTERS */

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;

  gap: 9px;

  margin-bottom: 18px;
}

.search-box {
  position: relative;

  display: flex;

  flex: 1;

  min-width: 300px;
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
.filter-btn,
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

.days-filter {
  display: flex;
  align-items: center;
  gap: 7px;
}

.days-filter input {
  width: 110px;
}

.days-filter span {
  color: #9ca3af;

  font-size: 12px;
}

.filter-btn,
.reset-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  padding: 0 14px;

  cursor: pointer;
}

.filter-btn {
  border: 1px solid #222;

  background: #222;

  color: #fff;
}

.reset-btn {
  border: 1px solid #d1d5db;

  background: #fff;

  color: #374151;
}

/* TABLE TOOLBAR */

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

/* TABLE */

.table-wrap {
  overflow-x: auto;

  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

table {
  width: 100%;

  min-width: 1300px;

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

.image-column,
.image-cell {
  width: 80px;
  min-width: 80px;
  text-align: center;
}

.product-thumb {
  position: relative;
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
  color: #9ca3af;
  cursor: default;
}

.product-thumb:disabled {
  opacity: 1;
}

.product-thumb.clickable {
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.product-thumb.clickable:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.1);
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

.sku {
  color: #111827;

  white-space: nowrap;
}

.product-cell {
  display: flex;
  min-width: 160px;
  flex-direction: column;
  gap: 4px;
}

.product-cell > strong {
  color: #111827;
}

.variant-info {
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
}

.lot-link {
  padding: 0;

  border: 0;

  background: transparent;

  color: #111827;

  font-weight: 700;

  cursor: pointer;
}

.lot-link:hover {
  text-decoration: underline;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;

  padding: 4px 9px;

  border-radius: 999px;

  font-size: 12px;
  font-weight: 650;

  white-space: nowrap;
}

.status-expired {
  background: #fef2f2;
  color: #b91c1c;
}

.status-near {
  background: #fff7ed;
  color: #c2410c;
}

.status-normal {
  background: #ecfdf5;
  color: #047857;
}


.days-expired {
  color: #b91c1c;
}

.days-near {
  color: #c2410c;
}

.sellable-zero {
  color: #b91c1c;
}

.sellable-positive {
  color: #047857;
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


.actions button:disabled,
.filters button:disabled {
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

/* PAGINATION */

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
  width: min(760px, calc(100% - 48px));
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
  color: #111827;
  font-size: 14px;
}

.image-preview-info span {
  color: #6b7280;
  font-size: 12px;
}

/* RESPONSIVE */

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .expiry-alert-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .search-box {
    min-width: 100%;
  }

  .days-filter {
    width: 100%;
  }

  .days-filter input {
    flex: 1;
    width: auto;
  }

  .table-toolbar,
  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
