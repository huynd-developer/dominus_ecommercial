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

  loading.value = true;

  try {
    await orderService.markReturnRefunded(order.orderId);

    await Swal.fire({
      icon: "success",
      title: "Đã cập nhật hoàn tiền",
      text: "Đơn hàng đã chuyển sang trạng thái hoàn hàng hoàn tất.",
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
