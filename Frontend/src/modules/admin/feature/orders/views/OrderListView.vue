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
      @accept-return="confirmAcceptReturn"
      @reject-return="confirmRejectReturn"
      @mark-return-refunded="confirmMarkReturnRefunded"
      @mark-delivery-refunded="confirmMarkDeliveryRefunded"
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
import api from "@/common/api";
import type { AdminCancelOrderRequest, AdminOrderResponse, MarkDeliveryFailedRequest } from "../types/order.type";

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

const returnRejectReasonOptions = [
  {
    value: "INVALID_EVIDENCE",
    label: "Bằng chứng không hợp lệ / không rõ ràng",
  },
  {
    value: "ORDER_ITEM_CORRECT",
    label: "Sản phẩm giao đúng theo đơn hàng",
  },
  {
    value: "NOT_ELIGIBLE",
    label: "Sản phẩm không thuộc điều kiện hoàn hàng",
  },
  {
    value: "RETURN_PERIOD_EXPIRED",
    label: "Quá thời hạn yêu cầu hoàn hàng",
  },
  {
    value: "USED_OR_NOT_INTACT",
    label: "Sản phẩm đã qua sử dụng / không còn nguyên trạng",
  },
  {
    value: "WRONG_RETURN_REASON",
    label: "Khách chọn sai lý do hoàn hàng",
  },
  {
    value: "OTHER",
    label: "Khác",
  },
];

const adminCancelReasonOptions = [
  {
    value: "CUSTOMER_REQUEST",
    label: "Khách yêu cầu hủy đơn",
  },
  {
    value: "CONTACT_FAILED",
    label: "Không liên hệ được khách hàng",
  },
  {
    value: "INVALID_SHIPPING_INFO",
    label: "Thông tin nhận hàng không hợp lệ",
  },
  {
    value: "OUT_OF_STOCK",
    label: "Sản phẩm tạm hết hàng",
  },
  {
    value: "DUPLICATE_ORDER",
    label: "Khách đặt trùng đơn",
  },
  {
    value: "SUSPICIOUS_ORDER",
    label: "Đơn hàng có dấu hiệu bất thường",
  },
  {
    value: "WRONG_PRICE_OR_INFO",
    label: "Sai giá / sai thông tin sản phẩm",
  },
  {
    value: "OTHER",
    label: "Khác",
  },
];

const deliveryFailedReasonOptions = [
  {
    value: "CONTACT_FAILED",
    label: "Không liên hệ được khách hàng",
    evidenceRequired: true,
  },
  {
    value: "CUSTOMER_REJECTED",
    label: "Khách từ chối nhận hàng",
    evidenceRequired: true,
  },
  {
    value: "WRONG_OR_MISSING_ADDRESS",
    label: "Sai hoặc thiếu địa chỉ giao hàng",
    evidenceRequired: true,
  },
  {
    value: "NO_RECEIVER",
    label: "Không có người nhận hàng",
    evidenceRequired: false,
  },
  {
    value: "DAMAGED_WHEN_DELIVERING",
    label: "Hàng bị hư hỏng khi giao",
    evidenceRequired: true,
  },
  {
    value: "RESCHEDULED_BUT_CANNOT_CONTINUE",
    label: "Khách hẹn giao lại nhưng shop không thể tiếp tục giao",
    evidenceRequired: false,
  },
  {
    value: "OTHER",
    label: "Khác",
    evidenceRequired: true,
  },
];

const MAX_DELIVERY_EVIDENCE_IMAGE_COUNT = 2;
const MAX_DELIVERY_EVIDENCE_TOTAL_SIZE = 10 * 1024 * 1024;

const safeTotalPages = computed(() => {
  if (totalElements.value <= 0) {
    return 1;
  }

  return Math.max(totalPages.value, 1);
});

const showingFrom = computed(() => {
  if (totalElements.value <= 0 || orders.value.length === 0) {
    return 0;
  }

  return page.value * size.value + 1;
});

const showingTo = computed(() => {
  if (totalElements.value <= 0 || orders.value.length === 0) {
    return 0;
  }

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

    orders.value = getOrdersForCurrentTab(pageData.content);
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
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra lại kết nối hoặc quyền truy cập.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

function resolvePageData(rawData: any) {
  const data = rawData?.data?.data ?? rawData?.data ?? rawData;

  const content = Array.isArray(data?.content)
    ? data.content
    : Array.isArray(data?.data?.content)
    ? data.data.content
    : Array.isArray(data?.data)
    ? data.data
    : Array.isArray(data)
    ? data
    : [];

  const totalElementsValue = Number(
    data?.totalElements ??
      data?.page?.totalElements ??
      data?.total ??
      content.length ??
      0
  );

  let totalPagesValue = Number(data?.totalPages ?? data?.page?.totalPages ?? 0);

  if (!Number.isFinite(totalPagesValue) || totalPagesValue <= 0) {
    totalPagesValue =
      totalElementsValue > 0 ? Math.ceil(totalElementsValue / size.value) : 0;
  }

  const responsePage = Number(data?.number ?? data?.page?.number);

  return {
    content,
    totalElements: Number.isFinite(totalElementsValue) ? totalElementsValue : 0,
    totalPages: Number.isFinite(totalPagesValue) ? totalPagesValue : 0,
    currentPage: Number.isFinite(responsePage) ? responsePage : null,
  };
}

function getOrdersForCurrentTab(list: AdminOrderResponse[]) {
  if (Number(status.value) === 6) {
    return prioritizeReturnRequestedOrders(list);
  }

  return list;
}

function getReturnSortTime(order: AdminOrderResponse) {
  const rawDate = order.returnRequestedAt || order.createdAt || order.completedAt || null;

  if (!rawDate) {
    return 0;
  }

  const time = new Date(rawDate).getTime();

  return Number.isFinite(time) ? time : 0;
}

function prioritizeReturnRequestedOrders(list: AdminOrderResponse[]) {
  return [...list].sort((left, right) => {
    const leftIsReturnRequested = Number(left.status) === 6;
    const rightIsReturnRequested = Number(right.status) === 6;

    if (leftIsReturnRequested !== rightIsReturnRequested) {
      return leftIsReturnRequested ? -1 : 1;
    }

    if (leftIsReturnRequested && rightIsReturnRequested) {
      const leftStatus = getReturnProcessStatus(left);
      const rightStatus = getReturnProcessStatus(right);

      if (leftStatus !== rightStatus) {
        return leftStatus - rightStatus;
      }

      return getReturnSortTime(right) - getReturnSortTime(left);
    }

    return 0;
  });
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

async function confirmChangeStatus(
  order: AdminOrderResponse,
  nextStatus: number
) {
  if (isReturnWorkflowOrder(order)) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể hoàn tất bằng trạng thái thường",
      text: "Đơn đang có yêu cầu hoàn hàng. Hãy mở chi tiết để Chấp nhận/Từ chối, sau đó mới xác nhận Đã hoàn tiền.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (nextStatus === 1) {
    await confirmAdminConfirmOrder(order);
    return;
  }

  if (nextStatus === 4) {
    await confirmAdminCancelOrder(order);
    return;
  }

  if (nextStatus === 3) {
    await confirmDeliveryCompleted(order);
    return;
  }

  if (nextStatus === 5) {
    await confirmDeliveryFailed(order);
    return;
  }

  if (nextStatus === 7) {
    await Swal.fire({
      icon: "warning",
      title: "Sai luồng xử lý hoàn hàng",
      text: "Không được chuyển thẳng sang Hoàn hàng hoàn tất. Phải xử lý bằng luồng Chấp nhận/Từ chối và Đã hoàn tiền.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const nextStatusText = getStatusText(nextStatus);

  const result = await Swal.fire({
    icon: "question",
    title: "Xác nhận cập nhật trạng thái?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode)}</p>
        <p><b>Trạng thái hiện tại:</b> ${escapeAlertHtml(order.statusText)}</p>
        <p><b>Chuyển sang:</b> ${escapeAlertHtml(nextStatusText)}</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Cập nhật",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#bd9a5f",
  });

  if (!result.isConfirmed) return;

  try {
    const response = await orderService.updateOrderStatus(
      order.orderId,
      nextStatus
    );

    await Swal.fire({
      icon: "success",
      title: "Cập nhật thành công",
      text: response.message || "Trạng thái đơn hàng đã được cập nhật.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể cập nhật trạng thái",
      text:
        error?.response?.data?.message ||
        "Trạng thái chuyển không hợp lệ hoặc đơn hàng không thể cập nhật.",
      confirmButtonColor: "#bd9a5f",
    });
  }
}

async function confirmAdminConfirmOrder(order: AdminOrderResponse) {
  if (!order || !order.orderId) return;

  if (Number(order.status) !== 0) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể xác nhận đơn",
      text: "Chỉ được xác nhận đơn khi đơn còn ở trạng thái Chờ xác nhận.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire({
    icon: "question",
    title: "Xác nhận đơn hàng?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Trạng thái hiện tại:</b> ${escapeAlertHtml(
          order.statusText || getStatusText(Number(order.status))
        )}</p>
        <p>Sau khi xác nhận, hệ thống sẽ trừ tồn kho theo số lượng sản phẩm trong đơn.</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận đơn",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#6b7280",
  });

  if (!result.isConfirmed) return;

  loading.value = true;

  try {
    const response = await orderService.confirmOrder(order.orderId);

    await Swal.fire({
      icon: "success",
      title: "Đã xác nhận đơn hàng",
      text:
        response.statusText ||
        "Đơn hàng đã chuyển sang trạng thái Đã xác nhận và tồn kho đã được cập nhật.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể xác nhận đơn",
      text:
        error?.response?.data?.message ||
        "Tồn kho không đủ hoặc đơn hàng không thể xác nhận.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

async function confirmDeliveryCompleted(order: AdminOrderResponse) {
  if (!order || !order.orderId) return;

  let selectedDeliverySuccessFiles: File[] = [];

  if (Number(order.status) !== 2) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể hoàn thành đơn",
      text: "Chỉ được xác nhận giao thành công khi đơn đang ở trạng thái Đang giao hàng.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire<File[]>({
    icon: "question",
    title: "Xác nhận giao hàng thành công?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Trạng thái hiện tại:</b> ${escapeAlertHtml(order.statusText || getStatusText(Number(order.status)))}</p>

        <label for="delivery-success-files" style="display:block;font-weight:700;margin:14px 0 6px">
          Ảnh minh chứng <span style="color:#dc2626">*</span>
        </label>
        <input
          id="delivery-success-files"
          type="file"
          class="swal2-file"
          multiple
          accept="image/png,image/jpeg,image/jpg,image/webp"
          style="display:block;width:100%;margin:0;border:1px solid #d1d5db;border-radius:8px;padding:10px"
        />
        <div style="font-size:12px;color:#6b7280;margin-top:6px">
          Bắt buộc có ảnh minh chứng vì hệ thống đang xử lý theo hướng cửa hàng tự giao hàng. Tối đa 2 ảnh, tổng dung lượng tối đa 10MB.
        </div>

        <div id="delivery-success-preview" style="margin-top:10px"></div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận hoàn thành",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#16a34a",
    cancelButtonColor: "#6b7280",
    focusConfirm: false,
    didOpen: () => {
      setupDeliveryImagePicker({
        inputId: "delivery-success-files",
        previewId: "delivery-success-preview",
        getFiles: () => selectedDeliverySuccessFiles,
        setFiles: (files) => {
          selectedDeliverySuccessFiles = files;
        },
      });
    },
    preConfirm: () => {
      const fileInput = document.getElementById(
        "delivery-success-files"
      ) as HTMLInputElement | null;

      const files = selectedDeliverySuccessFiles.length > 0
        ? selectedDeliverySuccessFiles
        : Array.from(fileInput?.files || []);

      if (files.length === 0) {
        Swal.showValidationMessage("Vui lòng tải lên ảnh minh chứng giao hàng thành công.");
        return false;
      }

      const invalidMessage = validateDeliveryFiles(files);

      if (invalidMessage) {
        Swal.showValidationMessage(invalidMessage);
        return false;
      }

      return files;
    },
  });

  if (!result.isConfirmed || !result.value) return;

  loading.value = true;

  try {
    await orderService.markDeliveryCompleted(order.orderId, {
      files: result.value,
    });

    await Swal.fire({
      icon: "success",
      title: "Đã xác nhận giao hàng thành công",
      text: "Đơn hàng đã chuyển sang trạng thái Hoàn thành.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể xác nhận giao hàng",
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra trạng thái đơn hàng hoặc thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

async function confirmDeliveryFailed(order: AdminOrderResponse) {
  if (!order || !order.orderId) return;

  let selectedDeliveryFailedFiles: File[] = [];

  if (Number(order.status) !== 2) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể cập nhật giao thất bại",
      text: "Chỉ được xác nhận giao thất bại khi đơn đang ở trạng thái Đang giao hàng.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire<MarkDeliveryFailedRequest>({
    icon: "warning",
    title: "Xác nhận giao hàng thất bại?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Trạng thái hiện tại:</b> ${escapeAlertHtml(order.statusText || getStatusText(Number(order.status)))}</p>

        <label for="delivery-failed-reason-code" style="display:block;font-weight:700;margin:14px 0 6px">
          Lý do giao thất bại <span style="color:#dc2626">*</span>
        </label>
        <select
          id="delivery-failed-reason-code"
          class="swal2-select"
          style="display:block;width:100%;margin:0 0 12px 0;height:42px;border:1px solid #d1d5db;border-radius:8px;padding:0 10px"
        >
          <option value="">-- Chọn lý do giao thất bại --</option>
          ${buildDeliveryFailedReasonOptionsHtml()}
        </select>

        <label for="delivery-failed-description" style="display:block;font-weight:700;margin:0 0 6px">
          Mô tả chi tiết <span style="color:#6b7280;font-weight:500">(bắt buộc nếu chọn Khác)</span>
        </label>
        <textarea
          id="delivery-failed-description"
          class="swal2-textarea"
          maxlength="500"
          placeholder="Ví dụ: Gọi khách 3 lần không nghe máy..."
          style="display:block;width:100%;height:100px;margin:0;border:1px solid #d1d5db;border-radius:8px;padding:10px;resize:vertical"
        ></textarea>

        <label for="delivery-failed-files" style="display:block;font-weight:700;margin:14px 0 6px">
          Ảnh minh chứng
        </label>
        <input
          id="delivery-failed-files"
          type="file"
          class="swal2-file"
          multiple
          accept="image/png,image/jpeg,image/jpg,image/webp"
          style="display:block;width:100%;margin:0;border:1px solid #d1d5db;border-radius:8px;padding:10px"
        />
        <div style="font-size:12px;color:#6b7280;margin-top:6px">
          Bắt buộc ảnh minh chứng với lý do nhạy cảm: khách từ chối, sai địa chỉ, hư hỏng, không liên hệ được nhiều lần hoặc Khác. Tối đa 2 ảnh, tổng dung lượng tối đa 10MB.
        </div>

        <div id="delivery-failed-preview" style="margin-top:10px"></div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận thất bại",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#6b7280",
    focusConfirm: false,
    didOpen: () => {
      setupDeliveryImagePicker({
        inputId: "delivery-failed-files",
        previewId: "delivery-failed-preview",
        getFiles: () => selectedDeliveryFailedFiles,
        setFiles: (files) => {
          selectedDeliveryFailedFiles = files;
        },
      });
    },
    preConfirm: () => {
      const reasonCodeElement = document.getElementById(
        "delivery-failed-reason-code"
      ) as HTMLSelectElement | null;
      const descriptionElement = document.getElementById(
        "delivery-failed-description"
      ) as HTMLTextAreaElement | null;
      const fileInput = document.getElementById(
        "delivery-failed-files"
      ) as HTMLInputElement | null;

      const reasonCode = String(reasonCodeElement?.value || "").trim();
      const description = normalizeAdminCancelDescription(descriptionElement?.value || "");
      const files = selectedDeliveryFailedFiles.length > 0
        ? selectedDeliveryFailedFiles
        : Array.from(fileInput?.files || []);

      if (!reasonCode) {
        Swal.showValidationMessage("Vui lòng chọn lý do giao hàng thất bại.");
        return false;
      }

      if (reasonCode === "OTHER" && !description) {
        Swal.showValidationMessage("Vui lòng nhập mô tả chi tiết khi chọn lý do Khác.");
        return false;
      }

      if (description && description.length < 5) {
        Swal.showValidationMessage("Mô tả chi tiết phải có ít nhất 5 ký tự.");
        return false;
      }

      const invalidMessage = validateDeliveryFiles(files);

      if (invalidMessage) {
        Swal.showValidationMessage(invalidMessage);
        return false;
      }

      const reasonOption = getDeliveryFailedReasonOption(reasonCode);

      if (reasonOption?.evidenceRequired && files.length === 0) {
        Swal.showValidationMessage("Lý do này cần ảnh minh chứng giao hàng thất bại.");
        return false;
      }

      return {
        reason: getDeliveryFailedReasonLabel(reasonCode),
        description: description || null,
        files,
      };
    },
  });

  if (!result.isConfirmed || !result.value) return;

  loading.value = true;

  try {
    await orderService.markDeliveryFailed(order.orderId, result.value);

    await Swal.fire({
      icon: "success",
      title: "Đã cập nhật giao hàng thất bại",
      text: "Đơn hàng đã chuyển sang trạng thái Giao hàng thất bại.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể cập nhật giao hàng thất bại",
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra trạng thái đơn hàng hoặc thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

async function confirmAdminCancelOrder(order: AdminOrderResponse) {
  if (!order || !order.orderId) return;

  if (Number(order.status) !== 0) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể hủy đơn",
      text: "Chỉ được hủy đơn khi đơn còn ở trạng thái Chờ xác nhận.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire<AdminCancelOrderRequest>({
    icon: "warning",
    title: "Hủy đơn hàng?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Trạng thái hiện tại:</b> ${escapeAlertHtml(order.statusText || getStatusText(Number(order.status)))}</p>

        <label for="admin-cancel-reason-code" style="display:block;font-weight:700;margin:14px 0 6px">
          Lý do hủy <span style="color:#dc2626">*</span>
        </label>
        <select
          id="admin-cancel-reason-code"
          class="swal2-select"
          style="display:block;width:100%;margin:0 0 12px 0;height:42px;border:1px solid #d1d5db;border-radius:8px;padding:0 10px"
        >
          <option value="">-- Chọn lý do hủy đơn --</option>
          ${buildAdminCancelReasonOptionsHtml()}
        </select>

        <label for="admin-cancel-reason-detail" style="display:block;font-weight:700;margin:0 0 6px">
          Mô tả chi tiết <span style="color:#6b7280;font-weight:500">(không bắt buộc, trừ khi chọn Khác)</span>
        </label>
        <textarea
          id="admin-cancel-reason-detail"
          class="swal2-textarea"
          maxlength="180"
          placeholder="Có thể nhập thêm ghi chú hủy đơn để lưu lịch sử xử lý..."
          style="display:block;width:100%;height:110px;margin:0;border:1px solid #d1d5db;border-radius:8px;padding:10px;resize:vertical"
        ></textarea>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận hủy",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#6b7280",
    focusConfirm: false,
    preConfirm: () => {
      const reasonCodeElement = document.getElementById(
        "admin-cancel-reason-code"
      ) as HTMLSelectElement | null;
      const detailElement = document.getElementById(
        "admin-cancel-reason-detail"
      ) as HTMLTextAreaElement | null;

      const reasonCode = String(reasonCodeElement?.value || "").trim();
      const description = normalizeAdminCancelDescription(detailElement?.value || "");

      if (!reasonCode) {
        Swal.showValidationMessage("Vui lòng chọn lý do hủy đơn.");
        return false;
      }

      if (reasonCode === "OTHER" && !description) {
        Swal.showValidationMessage("Vui lòng nhập mô tả chi tiết khi chọn lý do Khác.");
        return false;
      }

      if (description && description.length < 5) {
        Swal.showValidationMessage("Mô tả chi tiết phải có ít nhất 5 ký tự.");
        return false;
      }

      const reasonLabel = getAdminCancelReasonLabel(reasonCode);
      const fullReason = description ? `${reasonLabel} - ${description}` : reasonLabel;

      if (fullReason.length > 255) {
        Swal.showValidationMessage("Tổng lý do hủy không được vượt quá 255 ký tự.");
        return false;
      }

      return {
        reason: reasonLabel,
        description: description || null,
      };
    },
  });

  if (!result.isConfirmed || !result.value) return;

  loading.value = true;

  try {
    const response = await orderService.cancelOrder(order.orderId, result.value);

    await Swal.fire({
      icon: "success",
      title: "Đã hủy đơn hàng",
      text: response.statusText || "Lý do hủy đã được lưu vào đơn hàng.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể hủy đơn hàng",
      text:
        error?.response?.data?.message ||
        "Chỉ được hủy đơn khi đơn còn ở trạng thái Chờ xác nhận.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

function getDeliveryFailedReasonOption(reasonCode: string) {
  return deliveryFailedReasonOptions.find((item) => item.value === reasonCode);
}

function getDeliveryFailedReasonLabel(reasonCode: string) {
  return getDeliveryFailedReasonOption(reasonCode)?.label || "Khác";
}

function buildDeliveryFailedReasonOptionsHtml() {
  return deliveryFailedReasonOptions
    .map(
      (item) =>
        `<option value="${escapeAlertHtml(item.value)}">${escapeAlertHtml(item.label)}</option>`
    )
    .join("");
}

function validateDeliveryFiles(files: File[]) {
  if (files.length > MAX_DELIVERY_EVIDENCE_IMAGE_COUNT) {
    return `Chỉ được tải tối đa ${MAX_DELIVERY_EVIDENCE_IMAGE_COUNT} ảnh minh chứng.`;
  }

  const contentMessage = validateDeliveryImageContent(files);

  if (contentMessage) {
    return contentMessage;
  }

  const totalSize = getDeliveryFilesTotalSize(files);

  if (totalSize > MAX_DELIVERY_EVIDENCE_TOTAL_SIZE) {
    return "Tổng dung lượng ảnh minh chứng không được vượt quá 10MB.";
  }

  return "";
}

function validateDeliveryImageContent(files: File[]) {
  const allowedImageTypes = ["image/jpeg", "image/png", "image/webp"];
  const allowedImageExtensions = /\.(jpg|jpeg|png|webp)$/i;

  for (const file of files) {
    const fileType = String(file.type || "").toLowerCase().trim();
    const fileName = String(file.name || "").trim();

    if (!file || file.size <= 0 || !fileName) {
      return "Ảnh minh chứng không hợp lệ.";
    }

    const isAllowedImageType = !fileType || allowedImageTypes.includes(fileType);
    const hasAllowedExtension = allowedImageExtensions.test(fileName);

    if (!isAllowedImageType || !hasAllowedExtension) {
      return "Ảnh minh chứng chỉ hỗ trợ JPG, JPEG, PNG hoặc WEBP.";
    }
  }

  return "";
}

function getDeliveryFilesTotalSize(files: File[]) {
  return files.reduce((total, file) => total + Math.max(0, file?.size || 0), 0);
}

function formatDeliveryFileSize(size: number) {
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(2)}MB`;
  }

  return `${Math.max(1, Math.round(size / 1024))}KB`;
}


type DeliveryImagePickerOptions = {
  inputId: string;
  previewId: string;
  getFiles: () => File[];
  setFiles: (files: File[]) => void;
};

function setupDeliveryImagePicker(options: DeliveryImagePickerOptions) {
  const input = document.getElementById(options.inputId) as HTMLInputElement | null;

  if (!input) {
    return;
  }

  input.addEventListener("change", () => {
    const chosenFiles = Array.from(input.files || []);
    const currentFiles = options.getFiles();

    Swal.resetValidationMessage();

    if (chosenFiles.length === 0) {
      options.setFiles([]);
      renderDeliveryImagePreview(options);
      return;
    }

    const cleanFiles = chosenFiles.filter((file) => file && file.size > 0);

    if (cleanFiles.length === 0) {
      input.value = "";
      syncDeliveryImageInput(input, currentFiles);
      renderDeliveryImagePreview(options);
      Swal.showValidationMessage("Ảnh minh chứng không hợp lệ.");
      return;
    }

    const contentMessage = validateDeliveryImageContent(cleanFiles);

    if (contentMessage) {
      input.value = "";
      syncDeliveryImageInput(input, currentFiles);
      renderDeliveryImagePreview(options);
      Swal.showValidationMessage(contentMessage);
      return;
    }

    const limitedFiles = cleanFiles.slice(0, MAX_DELIVERY_EVIDENCE_IMAGE_COUNT);
    const invalidMessage = validateDeliveryFiles(limitedFiles);

    if (invalidMessage) {
      input.value = "";
      syncDeliveryImageInput(input, currentFiles);
      renderDeliveryImagePreview(options);
      Swal.showValidationMessage(invalidMessage);
      return;
    }

    options.setFiles(limitedFiles);
    syncDeliveryImageInput(input, limitedFiles);
    renderDeliveryImagePreview(options);

    if (cleanFiles.length > MAX_DELIVERY_EVIDENCE_IMAGE_COUNT) {
      Swal.showValidationMessage(
        `Chỉ được chọn tối đa ${MAX_DELIVERY_EVIDENCE_IMAGE_COUNT} ảnh. Hệ thống đã giữ ${MAX_DELIVERY_EVIDENCE_IMAGE_COUNT} ảnh đầu tiên hợp lệ.`
      );
      return;
    }

    Swal.resetValidationMessage();
  });

  renderDeliveryImagePreview(options);
}

function syncDeliveryImageInput(input: HTMLInputElement, files: File[]) {
  try {
    const dataTransfer = new DataTransfer();
    files.forEach((file) => dataTransfer.items.add(file));
    input.files = dataTransfer.files;
  } catch {
    input.value = "";
  }
}

function renderDeliveryImagePreview(options: DeliveryImagePickerOptions) {
  const preview = document.getElementById(options.previewId) as HTMLDivElement | null;
  const input = document.getElementById(options.inputId) as HTMLInputElement | null;

  if (!preview) {
    return;
  }

  const files = options.getFiles();
  const totalSize = getDeliveryFilesTotalSize(files);

  if (files.length === 0) {
    preview.innerHTML = `
      <div style="font-size:12px;color:#9ca3af;border:1px dashed #d1d5db;border-radius:10px;padding:10px;text-align:center">
        Chưa chọn ảnh minh chứng.
      </div>
    `;
    return;
  }

  preview.innerHTML = `
    <div style="display:flex;gap:10px;flex-wrap:wrap">
      ${files
        .map((file, index) => {
          const imageUrl = URL.createObjectURL(file);
          const fileSizeText = formatDeliveryFileSize(file.size);

          return `
            <div style="position:relative;width:96px">
              <button
                type="button"
                class="delivery-preview-image"
                data-index="${index}"
                style="width:96px;height:96px;border:1px solid #e5e7eb;border-radius:10px;padding:0;overflow:hidden;background:#f9fafb;cursor:pointer"
                title="Bấm để xem ảnh"
              >
                <img
                  src="${imageUrl}"
                  alt="${escapeAlertHtml(file.name || 'Ảnh minh chứng')}"
                  style="width:100%;height:100%;object-fit:cover;display:block"
                />
              </button>

              <button
                type="button"
                class="delivery-preview-remove"
                data-index="${index}"
                style="position:absolute;top:-7px;right:-7px;width:24px;height:24px;border-radius:999px;border:0;background:#dc2626;color:white;font-weight:800;line-height:24px;cursor:pointer"
                title="Xóa ảnh"
              >×</button>

              <div style="font-size:11px;color:#6b7280;margin-top:4px;line-height:1.25;word-break:break-word">
                ${escapeAlertHtml(file.name || `Ảnh ${index + 1}`)}<br />${fileSizeText}
              </div>
            </div>
          `;
        })
        .join("")}
    </div>
    <div style="font-size:12px;color:#6b7280;margin-top:8px">
      Tổng dung lượng: ${formatDeliveryFileSize(totalSize)} / ${formatDeliveryFileSize(MAX_DELIVERY_EVIDENCE_TOTAL_SIZE)}
    </div>
  `;

  preview.querySelectorAll<HTMLButtonElement>(".delivery-preview-remove").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();

      const index = Number(button.dataset.index);
      const currentFiles = options.getFiles();
      const nextFiles = currentFiles.filter((_, fileIndex) => fileIndex !== index);

      options.setFiles(nextFiles);

      if (input) {
        syncDeliveryImageInput(input, nextFiles);
      }

      renderDeliveryImagePreview(options);
      Swal.resetValidationMessage();
    });
  });

  preview.querySelectorAll<HTMLButtonElement>(".delivery-preview-image").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();

      const index = Number(button.dataset.index);
      const file = options.getFiles()[index];

      if (file) {
        openDeliveryLocalImagePreview(file);
      }
    });
  });
}

function openDeliveryLocalImagePreview(file: File) {
  const objectUrl = URL.createObjectURL(file);
  const overlay = document.createElement("div");

  overlay.style.position = "fixed";
  overlay.style.inset = "0";
  overlay.style.zIndex = "99999";
  overlay.style.background = "rgba(15, 23, 42, 0.82)";
  overlay.style.display = "flex";
  overlay.style.alignItems = "center";
  overlay.style.justifyContent = "center";
  overlay.style.padding = "24px";
  overlay.style.cursor = "zoom-out";

  overlay.innerHTML = `
    <div style="position:relative;max-width:92vw;max-height:92vh">
      <img
        src="${objectUrl}"
        alt="${escapeAlertHtml(file.name || 'Ảnh minh chứng')}"
        style="max-width:92vw;max-height:92vh;object-fit:contain;border-radius:12px;background:#fff"
      />
      <button
        type="button"
        style="position:absolute;top:-12px;right:-12px;width:34px;height:34px;border-radius:999px;border:0;background:#ffffff;color:#111827;font-weight:900;cursor:pointer"
        aria-label="Đóng"
      >×</button>
    </div>
  `;

  const close = () => {
    URL.revokeObjectURL(objectUrl);
    overlay.remove();
  };

  overlay.addEventListener("click", close);
  overlay.querySelector("button")?.addEventListener("click", (event) => {
    event.preventDefault();
    event.stopPropagation();
    close();
  });

  document.body.appendChild(overlay);
}

function buildAdminCancelReasonOptionsHtml() {
  return adminCancelReasonOptions
    .map(
      (item) =>
        `<option value="${escapeAlertHtml(item.value)}">${escapeAlertHtml(
          item.label
        )}</option>`
    )
    .join("");
}

function getAdminCancelReasonLabel(value: string) {
  const option = adminCancelReasonOptions.find((item) => item.value === value);

  return option?.label || value;
}

function normalizeAdminCancelDescription(value: string) {
  return String(value || "")
    .trim()
    .replace(/[\r\n\t]+/g, " ")
    .replace(/\s{2,}/g, " ");
}

async function confirmAcceptReturn(order: AdminOrderResponse) {
  if (!order || !order.orderId) return;

  if (!canAcceptReturn(order)) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể chấp nhận yêu cầu",
      text: "Yêu cầu hoàn hàng này không còn ở trạng thái chờ xử lý.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire({
    icon: "question",
    title: "Chấp nhận yêu cầu hoàn hàng?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Số tiền cần hoàn:</b> ${formatMoneyForAlert(getReturnRefundAmount(order))}</p>
        <p><b>Phương án hoàn tiền:</b> ${escapeAlertHtml(getRefundMethodTextForAlert(order))}</p>
        <p class="mb-0 text-danger"><b>Lưu ý:</b> Sau khi chấp nhận, mới được thực hiện bước đã hoàn tiền.</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Chấp nhận",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#16a34a",
    cancelButtonColor: "#6b7280",
  });

  if (!result.isConfirmed) return;

  loading.value = true;

  try {
    await orderService.acceptReturn(order.orderId);

    await Swal.fire({
      icon: "success",
      title: "Đã chấp nhận hoàn hàng",
      text: "Yêu cầu hoàn hàng đã được chấp nhận. Hãy hoàn tiền thực tế cho khách trước khi bấm Đã hoàn tiền.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể chấp nhận hoàn hàng",
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra trạng thái yêu cầu hoàn hàng hoặc thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

async function confirmRejectReturn(order: AdminOrderResponse) {
  if (!order || !order.orderId) return;

  if (!canRejectReturn(order)) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể từ chối yêu cầu",
      text: "Yêu cầu hoàn hàng này không còn ở trạng thái chờ xử lý.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire({
    icon: "warning",
    title: "Từ chối yêu cầu hoàn hàng?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Số tiền khách yêu cầu hoàn:</b> ${formatMoneyForAlert(getReturnRefundAmount(order))}</p>

        <label for="return-reject-reason-code" style="display:block;font-weight:700;margin:14px 0 6px">
          Lý do từ chối <span style="color:#dc2626">*</span>
        </label>
        <select
          id="return-reject-reason-code"
          class="swal2-select"
          style="display:block;width:100%;margin:0 0 12px 0;height:42px;border:1px solid #d1d5db;border-radius:8px;padding:0 10px"
        >
          <option value="">-- Chọn lý do từ chối --</option>
          ${buildReturnRejectReasonOptionsHtml()}
        </select>

        <label for="return-reject-reason-detail" style="display:block;font-weight:700;margin:0 0 6px">
          Mô tả chi tiết <span style="color:#6b7280;font-weight:500">(không bắt buộc, trừ khi chọn Khác)</span>
        </label>
        <textarea
          id="return-reject-reason-detail"
          class="swal2-textarea"
          maxlength="180"
          placeholder="Có thể nhập thêm giải thích cụ thể cho khách hàng..."
          style="display:block;width:100%;height:110px;margin:0;border:1px solid #d1d5db;border-radius:8px;padding:10px;resize:vertical"
        ></textarea>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Từ chối",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#6b7280",
    focusConfirm: false,
    preConfirm: () => {
      const reasonCodeElement = document.getElementById(
        "return-reject-reason-code"
      ) as HTMLSelectElement | null;
      const detailElement = document.getElementById(
        "return-reject-reason-detail"
      ) as HTMLTextAreaElement | null;

      const reasonCode = String(reasonCodeElement?.value || "").trim();
      const detail = normalizeRejectReasonDetail(detailElement?.value || "");

      if (!reasonCode) {
        Swal.showValidationMessage("Vui lòng chọn lý do từ chối hoàn hàng.");
        return false;
      }

      const isOtherReason = reasonCode === "OTHER";

      if (isOtherReason && !detail) {
        Swal.showValidationMessage(
          "Vui lòng nhập mô tả chi tiết khi chọn lý do Khác."
        );
        return false;
      }

      if (detail && detail.length < 5) {
        Swal.showValidationMessage("Mô tả chi tiết phải có ít nhất 5 ký tự.");
        return false;
      }

      const reasonLabel = getReturnRejectReasonLabel(reasonCode);
      const reason = detail ? `${reasonLabel} - ${detail}` : reasonLabel;

      if (reason.length > 255) {
        Swal.showValidationMessage(
          "Tổng lý do từ chối không được vượt quá 255 ký tự."
        );
        return false;
      }

      return reason;
    },
  });

  if (!result.isConfirmed) return;

  const reason = String(result.value || "").trim();
  loading.value = true;

  try {
    await orderService.rejectReturn(order.orderId, { reason });

    await Swal.fire({
      icon: "success",
      title: "Đã từ chối hoàn hàng",
      text: "Lý do từ chối đã được lưu để khách hàng có thể xem.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể từ chối hoàn hàng",
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra trạng thái yêu cầu hoàn hàng hoặc thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

function buildReturnRejectReasonOptionsHtml() {
  return returnRejectReasonOptions
    .map(
      (item) =>
        `<option value="${escapeAlertHtml(item.value)}">${escapeAlertHtml(
          item.label
        )}</option>`
    )
    .join("");
}

function getReturnRejectReasonLabel(value: string) {
  const option = returnRejectReasonOptions.find((item) => item.value === value);

  return option?.label || value;
}

function normalizeRejectReasonDetail(value: string) {
  return String(value || "")
    .trim()
    .replace(/[\r\n\t]+/g, " ")
    .replace(/\s{2,}/g, " ");
}

type RefundRestockMode = "DELIVERY_FAILED" | "RETURN_REQUEST";

type RefundRestockItemView = {
  productName: string;
  sku: string;
  capacity: string;
  bottleType: string;
  quantity: number;
};

const REFUND_RESTOCK_PARAM_NAME = "restoreStock";

function toPositiveInteger(value: unknown) {
  const numberValue = Number(value);

  return Number.isFinite(numberValue) && numberValue > 0
    ? Math.floor(numberValue)
    : 0;
}

function normalizeRestockText(value: unknown, fallback = "-") {
  const text = String(value ?? "").trim();

  return text || fallback;
}

function getRestockCapacityText(item: any) {
  return normalizeRestockText(
    item?.capacity ?? item?.capacityName ?? item?.capacityText ?? item?.volume,
    "Đang cập nhật"
  );
}

function getRestockBottleTypeText(item: any) {
  return normalizeRestockText(
    item?.bottleType ?? item?.bottleTypeName ?? item?.bottleName,
    "Đang cập nhật"
  );
}

function mapOrderItemToRestockItem(item: any): RefundRestockItemView | null {
  const quantity = toPositiveInteger(item?.quantity ?? item?.orderedQuantity);

  if (quantity <= 0) {
    return null;
  }

  return {
    productName: normalizeRestockText(
      item?.productName ?? item?.name,
      "Sản phẩm"
    ),
    sku: normalizeRestockText(item?.sku),
    capacity: getRestockCapacityText(item),
    bottleType: getRestockBottleTypeText(item),
    quantity,
  };
}

function mapReturnItemToRestockItem(item: any): RefundRestockItemView | null {
  const quantity = toPositiveInteger(
    item?.returnQuantity ??
      item?.returnedQuantity ??
      item?.requestQuantity ??
      item?.quantity
  );

  if (quantity <= 0) {
    return null;
  }

  return {
    productName: normalizeRestockText(
      item?.productName ?? item?.name,
      "Sản phẩm"
    ),
    sku: normalizeRestockText(item?.sku),
    capacity: getRestockCapacityText(item),
    bottleType: getRestockBottleTypeText(item),
    quantity,
  };
}

function getDeliveryRefundRestockItems(order: AdminOrderResponse) {
  const rawItems = Array.isArray((order as any)?.items)
    ? ((order as any).items as any[])
    : [];

  return rawItems
    .map(mapOrderItemToRestockItem)
    .filter((item): item is RefundRestockItemView => item !== null);
}

function getReturnRefundRestockItems(order: AdminOrderResponse) {
  const rawItems = Array.isArray((order as any)?.returnItems)
    ? ((order as any).returnItems as any[])
    : [];

  return rawItems
    .map(mapReturnItemToRestockItem)
    .filter((item): item is RefundRestockItemView => item !== null);
}

function buildRefundRestockItemsHtml(items: RefundRestockItemView[]) {
  if (items.length === 0) {
    return `
      <div style="padding:12px;border:1px dashed #f59e0b;border-radius:10px;background:#fffbeb;color:#92400e;font-size:13px;font-weight:700">
        Không tìm thấy danh sách sản phẩm để nhập kho. Nếu vẫn chọn Có, backend sẽ xử lý theo dữ liệu đơn hàng hiện có.
      </div>
    `;
  }

  const rows = items
    .map(
      (item, index) => `
        <tr>
          <td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#64748b">${index + 1}</td>
          <td style="padding:8px;border-bottom:1px solid #e5e7eb">
            <div style="font-weight:800;color:#0f172a">${escapeAlertHtml(item.productName)}</div>
            <div style="font-size:12px;color:#64748b;margin-top:2px">
              SKU: ${escapeAlertHtml(item.sku)} · ${escapeAlertHtml(item.capacity)} · ${escapeAlertHtml(item.bottleType)}
            </div>
          </td>
          <td style="padding:8px;border-bottom:1px solid #e5e7eb;text-align:right;font-weight:900;color:#16a34a">+${item.quantity}</td>
        </tr>
      `
    )
    .join("");

  return `
    <div style="max-height:260px;overflow:auto;border:1px solid #e5e7eb;border-radius:10px;background:#ffffff">
      <table style="width:100%;border-collapse:collapse;font-size:13px">
        <thead>
          <tr style="background:#f8fafc;color:#475569">
            <th style="padding:8px;text-align:left;width:42px">#</th>
            <th style="padding:8px;text-align:left">Sản phẩm</th>
            <th style="padding:8px;text-align:right;width:86px">SL nhập</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;
}

async function askRestoreStockAfterRefund(
  order: AdminOrderResponse,
  mode: RefundRestockMode
) {
  const items =
    mode === "DELIVERY_FAILED"
      ? getDeliveryRefundRestockItems(order)
      : getReturnRefundRestockItems(order);

  const modeText =
    mode === "DELIVERY_FAILED"
      ? "đơn giao hàng thất bại"
      : "yêu cầu hoàn hàng";

  const result = await Swal.fire({
    icon: "question",
    title: "Có nhập lại kho không?",
    html: `
      <div style="text-align:left;line-height:1.55">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p style="margin-bottom:10px">Shop đã xác nhận bước hoàn tiền cho ${escapeAlertHtml(modeText)}. Chọn tiếp cách xử lý kho:</p>
        ${buildRefundRestockItemsHtml(items)}
        <div style="margin-top:12px;padding:10px 12px;border-radius:10px;background:#f8fafc;color:#475569;font-size:13px">
          <p style="margin:0 0 4px"><b>Có, nhập kho:</b> hệ thống cộng lại số lượng sản phẩm ở bảng trên.</p>
          <p style="margin:0"><b>Không nhập kho:</b> chỉ ghi nhận đã hoàn tiền, không cộng tồn kho.</p>
        </div>
      </div>
    `,
    showCancelButton: true,
    showDenyButton: true,
    confirmButtonText: "Có, nhập kho",
    denyButtonText: "Không nhập kho",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#16a34a",
    denyButtonColor: "#6b7280",
    cancelButtonColor: "#9ca3af",
  });

  if (result.isConfirmed) {
    return true;
  }

  if (result.isDenied) {
    return false;
  }

  return null;
}

async function runRefundActionWithRestockParam<T>(
  restoreStock: boolean,
  action: () => Promise<T>
): Promise<T> {
  const previousParams = api.defaults.params;

  api.defaults.params = {
    ...(previousParams || {}),
    [REFUND_RESTOCK_PARAM_NAME]: restoreStock ? "true" : "false",
  };

  try {
    return await action();
  } finally {
    api.defaults.params = previousParams;
  }
}


async function confirmMarkDeliveryRefunded(order: AdminOrderResponse) {
  if (!order || !order.orderId) {
    return;
  }

  if (!canMarkDeliveryRefunded(order)) {
    await Swal.fire({
      icon: "warning",
      title: "Chưa thể hoàn tiền",
      text: "Chỉ được xác nhận khi đơn giao thất bại, có số tiền cần hoàn và khách đã nhập đủ thông tin tài khoản.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const result = await Swal.fire({
    icon: "question",
    title: "Xác nhận đã chuyển tiền?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Số tiền hoàn:</b> ${formatMoneyForAlert(getDeliveryRefundAmount(order))}</p>
        <p><b>Ngân hàng:</b> ${escapeAlertHtml(order.deliveryRefundBankName || "-")}</p>
        <p><b>Số tài khoản:</b> ${escapeAlertHtml(order.deliveryRefundBankAccountNumber || "-")}</p>
        <p><b>Chủ tài khoản:</b> ${escapeAlertHtml(order.deliveryRefundBankAccountHolder || "-")}</p>
        <p class="mb-0 text-danger"><b>Lưu ý:</b> Chỉ bấm khi shop đã chuyển khoản hoàn tiền thực tế cho khách.</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Đã chuyển tiền",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#16a34a",
    cancelButtonColor: "#6b7280",
  });

  if (!result.isConfirmed) {
    return;
  }

  const shouldRestoreStock = await askRestoreStockAfterRefund(
    order,
    "DELIVERY_FAILED"
  );

  if (shouldRestoreStock === null) {
    return;
  }

  loading.value = true;

  try {
    await runRefundActionWithRestockParam(shouldRestoreStock, () =>
      orderService.markDeliveryRefunded(order.orderId)
    );

    await Swal.fire({
      icon: "success",
      title: shouldRestoreStock
        ? "Đã hoàn tiền và nhập kho"
        : "Đã xác nhận hoàn tiền",
      text: shouldRestoreStock
        ? "Hệ thống đã ghi nhận shop đã chuyển tiền hoàn cho khách và cộng sản phẩm về kho."
        : "Hệ thống đã ghi nhận shop đã chuyển tiền hoàn cho khách. Sản phẩm không được cộng về kho.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể xác nhận hoàn tiền",
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra thông tin hoàn tiền hoặc thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

async function confirmMarkReturnRefunded(order: AdminOrderResponse) {
  if (!order || !order.orderId) {
    return;
  }

  if (!canMarkReturnRefunded(order)) {
    await Swal.fire({
      icon: "warning",
      title: "Chưa thể hoàn tiền",
      text: "Phải chấp nhận yêu cầu hoàn hàng trước, sau đó mới được bấm Đã hoàn tiền.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const bankTransferHtml = isBankTransferRefund(order)
    ? `
        <p><b>Ngân hàng:</b> ${escapeAlertHtml(order.bankName || "-")}</p>
        <p><b>Số tài khoản:</b> ${escapeAlertHtml(
          order.bankAccountNumber || "-"
        )}</p>
        <p><b>Chủ tài khoản:</b> ${escapeAlertHtml(
          order.bankAccountHolder || "-"
        )}</p>
      `
    : "";

  const result = await Swal.fire({
    icon: "question",
    title: "Xác nhận đã hoàn tiền?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${escapeAlertHtml(order.orderCode || "-")}</p>
        <p><b>Số tiền hoàn:</b> ${formatMoneyForAlert(getReturnRefundAmount(order))}</p>
        <p><b>Phương án:</b> ${escapeAlertHtml(
          getRefundMethodTextForAlert(order)
        )}</p>
        ${bankTransferHtml}
        <p class="mb-0 text-danger"><b>Lưu ý:</b> Chỉ bấm khi đã hoàn tiền thực tế cho khách.</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Đã hoàn tiền",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#16a34a",
    cancelButtonColor: "#6b7280",
  });

  if (!result.isConfirmed) {
    return;
  }

  const shouldRestoreStock = await askRestoreStockAfterRefund(
    order,
    "RETURN_REQUEST"
  );

  if (shouldRestoreStock === null) {
    return;
  }

  loading.value = true;

  try {
    await runRefundActionWithRestockParam(shouldRestoreStock, () =>
      orderService.markReturnRefunded(order.orderId)
    );

    await Swal.fire({
      icon: "success",
      title: shouldRestoreStock
        ? "Đã hoàn tiền và nhập kho"
        : "Đã cập nhật hoàn tiền",
      text: shouldRestoreStock
        ? "Đơn hàng đã chuyển sang trạng thái hoàn hàng hoàn tất và sản phẩm hoàn đã được cộng về kho."
        : "Đơn hàng đã chuyển sang trạng thái hoàn hàng hoàn tất. Sản phẩm hoàn không được cộng về kho.",
      confirmButtonColor: "#bd9a5f",
    });

    await refreshOrderAfterWorkflow(order.orderId);
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không thể cập nhật hoàn tiền",
      text:
        error?.response?.data?.message ||
        "Vui lòng kiểm tra trạng thái đơn hàng hoặc thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    loading.value = false;
  }
}

async function refreshOrderAfterWorkflow(orderId: number) {
  await loadOrders();

  if (showDetailModal.value && selectedOrder.value?.orderId === orderId) {
    selectedOrder.value = await orderService.getOrderDetail(orderId);
  }
}

function getReturnRefundAmount(order: AdminOrderResponse) {
  return Number(order.returnRefundAmount ?? order.refundAmount ?? 0);
}

function getReturnProcessStatus(order: AdminOrderResponse) {
  const directStatus = Number(order.returnProcessStatus);

  if (Number.isFinite(directStatus)) {
    return directStatus;
  }

  const itemStatuses = (order.returnItems || [])
    .map((item) => Number(item.status))
    .filter((value) => Number.isFinite(value));

  if (Number(order.status) === 7 || itemStatuses.some((value) => value === 3)) {
    return 3;
  }

  if (itemStatuses.length > 0 && itemStatuses.every((value) => value === 2)) {
    return 2;
  }

  if (itemStatuses.length > 0 && itemStatuses.every((value) => value === 1)) {
    return 1;
  }

  if (Number(order.status) === 6) {
    return 0;
  }

  return 99;
}

function isReturnWorkflowOrder(order: AdminOrderResponse | null) {
  if (!order) {
    return false;
  }

  return (
    Number(order.status) === 6 ||
    Boolean(order.returnReason) ||
    Boolean(order.returnDescription) ||
    Boolean(order.returnRequestedAt) ||
    Boolean(order.returnItems && order.returnItems.length > 0) ||
    order.canAcceptReturn === true ||
    order.canRejectReturn === true ||
    order.canMarkReturnRefunded === true
  );
}

function canAcceptReturn(order: AdminOrderResponse) {
  if (order.canAcceptReturn !== undefined && order.canAcceptReturn !== null) {
    return order.canAcceptReturn === true;
  }

  return Number(order.status) === 6 && getReturnProcessStatus(order) === 0;
}

function canRejectReturn(order: AdminOrderResponse) {
  if (order.canRejectReturn !== undefined && order.canRejectReturn !== null) {
    return order.canRejectReturn === true;
  }

  return Number(order.status) === 6 && getReturnProcessStatus(order) === 0;
}

function canMarkReturnRefunded(order: AdminOrderResponse) {
  if (
    order.canMarkReturnRefunded !== undefined &&
    order.canMarkReturnRefunded !== null
  ) {
    return order.canMarkReturnRefunded === true;
  }

  return Number(order.status) === 6 && getReturnProcessStatus(order) === 1;
}

function getDeliveryRefundAmount(order: AdminOrderResponse | null) {
  return Number((order as any)?.deliveryRefundAmount ?? 0);
}

function hasDeliveryRefundBankInfo(order: AdminOrderResponse | null) {
  return Boolean(
    String((order as any)?.deliveryRefundBankName || "").trim() &&
      String((order as any)?.deliveryRefundBankAccountNumber || "").trim() &&
      String((order as any)?.deliveryRefundBankAccountHolder || "").trim()
  );
}

function canMarkDeliveryRefunded(order: AdminOrderResponse | null) {
  if (!order) {
    return false;
  }

  if (
    order.canMarkDeliveryRefunded !== undefined &&
    order.canMarkDeliveryRefunded !== null
  ) {
    return order.canMarkDeliveryRefunded === true;
  }

  return (
    Number(order.status) === 5 &&
    getDeliveryRefundAmount(order) > 0 &&
    hasDeliveryRefundBankInfo(order) &&
    !order.deliveryRefundedAt
  );
}

function formatMoneyForAlert(value?: number | null) {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
}

function normalizeRefundMethodForAlert(value?: string | number | null) {
  return String(value || "")
    .trim()
    .toUpperCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function isBankTransferRefund(order: AdminOrderResponse) {
  const method = normalizeRefundMethodForAlert(order.refundMethod);

  return (
    method === "1" ||
    method === "BANK_TRANSFER" ||
    method.includes("BANK") ||
    method.includes("TRANSFER") ||
    method.includes("CHUYEN KHOAN") ||
    method.includes("NGAN HANG")
  );
}

function getRefundMethodTextForAlert(order: AdminOrderResponse) {
  const method = normalizeRefundMethodForAlert(order.refundMethod);

  if (
    method === "1" ||
    method === "BANK_TRANSFER" ||
    method.includes("BANK") ||
    method.includes("TRANSFER") ||
    method.includes("CHUYEN KHOAN") ||
    method.includes("NGAN HANG")
  ) {
    return "Chuyển khoản ngân hàng";
  }

  if (
    method === "2" ||
    method === "STORE" ||
    method.includes("CUA HANG") ||
    method.includes("TAI QUAY")
  ) {
    return "Hoàn tại cửa hàng";
  }

  return String(order.refundMethod || "-");
}

function escapeAlertHtml(value?: string | number | null) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function getStatusText(status: number) {
  switch (status) {
    case 0:
      return "Chờ xác nhận";
    case 1:
      return "Đã xác nhận";
    case 2:
      return "Đang giao hàng";
    case 3:
      return "Hoàn thành";
    case 4:
      return "Đã hủy";
    case 5:
      return "Giao hàng thất bại";
    case 6:
      return "Yêu cầu hoàn hàng";
    case 7:
      return "Hoàn hàng hoàn tất";
    default:
      return "Không xác định";
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
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
}

.pagination-total strong {
  color: #111827;
  font-weight: 800;
}

.pagination-range {
  padding-left: 10px;
  border-left: 1px solid #e5e7eb;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-btn,
.pagination-number {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s ease;
}

.pagination-btn:hover:not(:disabled),
.pagination-number:hover:not(:disabled) {
  border-color: #bd9a5f;
  color: #bd9a5f;
  background: #fffaf2;
}

.pagination-number.active {
  border-color: #bd9a5f;
  background: #bd9a5f;
  color: #ffffff;
  box-shadow: 0 6px 14px rgba(189, 154, 95, 0.28);
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
