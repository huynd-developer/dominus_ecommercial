<template>
  <div class="container-fluid py-3">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h4 class="fw-bold mb-1">Quản lý đơn hàng</h4>
        <small class="text-muted">
          Theo dõi đơn online, đơn tại quầy và cập nhật trạng thái đơn hàng.
        </small>
      </div>

      <button
        class="btn btn-outline-primary"
        :disabled="loading"
        @click="loadOrders"
      >
        Làm mới
      </button>
    </div>

    <OrderFilter
      :keyword="keyword"
      :status="status"
      :order-type="orderType"
      :from-date="fromDate"
      :to-date="toDate"
      @search="handleSearch"
    />

    <div class="order-filter-tabs mb-3">
      <div class="order-status-tabs">
        <button
          v-for="item in statusTabs"
          :key="String(item.value)"
          type="button"
          class="underline-tab"
          :class="{ active: status === item.value }"
          :disabled="loading"
          @click="changeStatusTab(item.value)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="order-type-tabs">
        <button
          v-for="item in orderTypeTabs"
          :key="item.value"
          type="button"
          class="underline-tab type-tab"
          :class="{ active: orderType === item.value }"
          :disabled="loading"
          @click="changeOrderTypeTab(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <OrderTable
      :orders="orders"
      :loading="loading"
      @view-detail="openDetail"
      @change-status="confirmChangeStatus"
    />

    <div v-if="totalElements > 0" class="order-pagination mt-3">
      <div class="pagination-info">
        <span class="pagination-total">
          Tổng <strong>{{ totalElements }}</strong> đơn hàng
        </span>

        <span v-if="orders.length > 0" class="pagination-range">
          Hiển thị {{ showingFrom }} - {{ showingTo }}
        </span>
      </div>

      <div class="pagination-actions">
        <button
          type="button"
          class="pagination-btn"
          :disabled="page <= 0 || loading || safeTotalPages <= 1"
          @click="goToPage(page - 1)"
        >
          <i class="bi bi-chevron-left"></i>
        </button>

        <button
          v-for="pageNumber in pageNumbers"
          :key="pageNumber"
          type="button"
          class="pagination-number"
          :class="{ active: pageNumber === page + 1 }"
          :disabled="loading"
          @click="goToPage(pageNumber - 1)"
        >
          {{ pageNumber }}
        </button>

        <button
          type="button"
          class="pagination-btn"
          :disabled="
            page >= safeTotalPages - 1 || loading || safeTotalPages <= 1
          "
          @click="goToPage(page + 1)"
        >
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>
    </div>

    <OrderDetailModal
      :show="showDetailModal"
      :order="selectedOrder"
      @close="closeDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Swal from "sweetalert2";
import OrderFilter from "../components/OrderFilter.vue";
import OrderTable from "../components/OrderTable.vue";
import OrderDetailModal from "../components/OrderDetailModal.vue";
import { orderService } from "../services/order.service";
import type { AdminOrderResponse } from "../types/order.type";

const orders = ref<AdminOrderResponse[]>([]);
const selectedOrder = ref<AdminOrderResponse | null>(null);

const loading = ref(false);
const detailLoading = ref(false);

const showDetailModal = ref(false);

const keyword = ref("");
const status = ref<number | null>(null);
const orderType = ref("");

const fromDate = ref("");
const toDate = ref("");

const page = ref(0);
const size = ref(10);
const totalElements = ref(0);
const totalPages = ref(0);

const statusTabs = [
  { label: "Tất cả", value: null },
  { label: "Chờ xác nhận", value: 0 },
  { label: "Đã xác nhận", value: 1 },
  { label: "Đang giao", value: 2 },
  { label: "Hoàn thành", value: 3 },
  { label: "Đã hủy", value: 4 },
  { label: "Giao thất bại", value: 5 },
  { label: "Yêu cầu hoàn", value: 6 },
  { label: "Hoàn hàng hoàn tất", value: 7 },
];

const orderTypeTabs = [
  { label: "Tất cả đơn", value: "" },
  { label: "Online", value: "ONLINE" },
  { label: "Tại quầy", value: "IN_STORE" },
];

const safeTotalPages = computed(() => {
  if (totalElements.value <= 0) return 1;
  return Math.max(totalPages.value, 1);
});

const showingFrom = computed(() => {
  if (totalElements.value <= 0 || orders.value.length === 0) return 0;
  return page.value * size.value + 1;
});

const showingTo = computed(() => {
  if (totalElements.value <= 0 || orders.value.length === 0) return 0;
  return Math.min((page.value + 1) * size.value, totalElements.value);
});

const pageNumbers = computed(() => {
  const total = safeTotalPages.value;
  const current = page.value + 1;

  if (total <= 5) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }

  let start = Math.max(1, current - 2);
  let end = Math.min(total, start + 4);

  if (end - start < 4) {
    start = Math.max(1, end - 4);
  }

  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});

onMounted(() => {
  loadOrders();
});

async function loadOrders() {
  loading.value = true;
  try {
    const rawData = await orderService.getOrders({
      keyword: keyword.value,
      status: status.value,
      orderType: orderType.value,
      fromDate: fromDate.value,
      toDate: toDate.value,
      page: page.value,
      size: size.value,
    });

    const pageData = resolvePageData(rawData);

    orders.value = pageData.content;
    totalElements.value = pageData.totalElements;
    totalPages.value = pageData.totalPages;

    if (pageData.currentPage !== null) {
      page.value = pageData.currentPage;
    }

    if (totalPages.value <= 0 && totalElements.value > 0) {
      totalPages.value = Math.ceil(totalElements.value / size.value);
    }
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không tải được danh sách đơn hàng",
      text: error?.response?.data?.message || "Vui lòng kiểm tra lại kết nối.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

function resolvePageData(rawData: any) {
  const data = rawData?.data?.data ?? rawData?.data ?? rawData;
  const content = Array.isArray(data?.content) ? data.content : [];
  const totalElementsValue = Number(data?.totalElements ?? content.length ?? 0);
  let totalPagesValue = Number(data?.totalPages ?? 0);

  if (!Number.isFinite(totalPagesValue) || totalPagesValue <= 0) {
    totalPagesValue = totalElementsValue > 0 ? Math.ceil(totalElementsValue / size.value) : 0;
  }

  const responsePage = Number(data?.number ?? data?.page?.number);

  return {
    content,
    totalElements: Number.isFinite(totalElementsValue) ? totalElementsValue : 0,
    totalPages: Number.isFinite(totalPagesValue) ? totalPagesValue : 0,
    currentPage: Number.isFinite(responsePage) ? responsePage : null,
  };
}

function handleSearch(payload: {
  keyword: string;
  status: number | null;
  orderType: string;
  fromDate: string;
  toDate: string;
}) {
  keyword.value = payload.keyword;
  status.value = payload.status;
  orderType.value = payload.orderType;
  fromDate.value = payload.fromDate;
  toDate.value = payload.toDate;

  page.value = 0;
  loadOrders();
}

function changeStatusTab(value: number | null) {
  if (status.value === value) return;

  status.value = value;
  page.value = 0;
  loadOrders();
}

function changeOrderTypeTab(value: string) {
  if (orderType.value === value) return;

  orderType.value = value;
  page.value = 0;
  loadOrders();
}

function goToPage(targetPage: number) {
  if (loading.value) return;
  if (targetPage < 0 || targetPage >= safeTotalPages.value) return;
  page.value = targetPage;
  loadOrders();
}

async function openDetail(orderId: number) {
  showDetailModal.value = true;
  selectedOrder.value = null;
  detailLoading.value = true;

  try {
    selectedOrder.value = await orderService.getOrderDetail(orderId);
  } catch (error: any) {
    showDetailModal.value = false;
    await Swal.fire({
      icon: "error",
      title: "Không tải được chi tiết đơn hàng",
      text: error?.response?.data?.message || "Vui lòng thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  showDetailModal.value = false;
  selectedOrder.value = null;
}

async function confirmChangeStatus(order: AdminOrderResponse, nextStatus: number) {
  const nextStatusText = getStatusText(nextStatus);

  let returnInfoHtml = "";
  if (order.status === 6) {
    const reason = (order as any).returnReason || "Không có lý do cụ thể";
    const images: string[] = (order as any).returnImages || [];
    const videos: string[] = (order as any).returnVideos || [];

    let mediaHtml = "";
    if (images.length > 0 || videos.length > 0) {
      mediaHtml += `<div style="margin-top: 10px;"><b>Bằng chứng (Ảnh/Video):</b><div style="display: flex; gap: 6px; flex-wrap: wrap; margin-top: 6px;">`;
      images.forEach((img) => {
        mediaHtml += `<img src="${img}" style="width: 65px; height: 65px; object-fit: cover; border-radius: 6px; border: 1px solid #ccc; cursor: pointer;" onclick="window.open('${img}', '_blank')" />`;
      });
      videos.forEach((vid) => {
        mediaHtml += `<video src="${vid}" style="width: 65px; height: 65px; object-fit: cover; border-radius: 6px; border: 1px solid #ccc;" controls></video>`;
      });
      mediaHtml += `</div></div>`;
    }

    returnInfoHtml = `
      <div style="margin-top: 12px; padding: 10px; background: #fffbeb; border: 1px dashed #f59e0b; border-radius: 8px; color: #b45309; text-align: left; font-size: 13px;">
        <p style="margin-bottom: 4px;"><b>Lý do hoàn:</b> <span style="color: #dc2626">${reason}</span></p>
        ${mediaHtml}
      </div>
    `;
  }

  let title = "Xác nhận cập nhật trạng thái?";
  let confirmBtnText = "Cập nhật";
  let confirmBtnColor = "#bd9a5f";

  if (order.status === 6 && nextStatus === 3) {
    title = "Từ chối yêu cầu hoàn hàng?";
    confirmBtnText = "Xác nhận từ chối";
    confirmBtnColor = "#dc3545";
  }

  const result = await Swal.fire({
    icon: order.status === 6 && nextStatus === 3 ? "warning" : "question",
    title: title,
    width: "40em",
    html: `
      <div style="text-align: left; font-size: 14px;">
        <p><b>Đơn hàng:</b> ${order.orderCode}</p>
        <p><b>Trạng thái hiện tại:</b> ${order.statusText}</p>
        <p><b>Chuyển sang:</b> <span style="color: ${confirmBtnColor}; font-weight: bold;">${nextStatusText}</span></p>
        ${returnInfoHtml}
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: confirmBtnText,
    cancelButtonText: "Hủy",
    confirmButtonColor: confirmBtnColor,
  });

  if (!result.isConfirmed) return;

  try {
    const response = await orderService.updateOrderStatus(order.orderId, nextStatus);

    await Swal.fire({
      icon: "success",
      title: "Thành công",
      text: (response as any)?.message || "Trạng thái đơn hàng đã được cập nhật.",
      confirmButtonColor: "#bd9a5f",
      timer: 1500,
      showConfirmButton: false,
    });

    await loadOrders();

    if (showDetailModal.value && selectedOrder.value?.orderId === order.orderId) {
      selectedOrder.value = await orderService.getOrderDetail(order.orderId);
    }
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Lỗi cập nhật",
      text: error?.response?.data?.message || "Không thể thay đổi trạng thái đơn hàng.",
      confirmButtonColor: "#bd9a5f",
    });
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 0: return "Chờ xác nhận";
    case 1: return "Đã xác nhận";
    case 2: return "Đang giao hàng";
    case 3: return "Hoàn thành";
    case 4: return "Đã hủy";
    case 5: return "Giao hàng thất bại";
    case 6: return "Yêu cầu hoàn hàng";
    case 7: return "Hoàn hàng hoàn tất";
    default: return "Không xác định";
  }
}
</script>

<style scoped>
.order-filter-tabs {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.order-status-tabs,
.order-type-tabs {
  display: flex;
  align-items: center;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;
}

.order-status-tabs::-webkit-scrollbar,
.order-type-tabs::-webkit-scrollbar {
  display: none;
}

.order-status-tabs {
  min-height: 58px;
}

.order-type-tabs {
  min-height: 46px;
  border-top: 1px solid #f1f5f9;
}

.underline-tab {
  position: relative;
  min-width: 128px;
  min-height: 58px;
  padding: 0 20px;
  border: none;
  background: transparent;
  color: #1f2937;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.18s ease;
}

.order-type-tabs .underline-tab {
  min-width: 116px;
  min-height: 46px;
  font-size: 13px;
}

.underline-tab:hover:not(:disabled) {
  color: #bd9a5f;
}

.underline-tab.active {
  color: #bd9a5f;
  font-weight: 700;
}

.underline-tab.active::after {
  content: "";
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: -1px;
  height: 3px;
  background: #bd9a5f;
}

.underline-tab:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.order-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
}
.pagination-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pagination-btn, .pagination-number {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #374151;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.pagination-number.active {
  border-color: #bd9a5f;
  background: #bd9a5f;
  color: #ffffff;
}

.pagination-btn:disabled,
.pagination-number:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 767.98px) {
  .order-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-info {
    justify-content: center;
    flex-wrap: wrap;
  }

  .pagination-actions {
    justify-content: center;
  }
}
</style>