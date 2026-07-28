<template>
  <div class="container-fluid py-3">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h4 class="fw-bold mb-1">Quản lý đơn hàng</h4>
        <small class="text-muted">
          Theo dõi đơn online, đơn tại quầy và cập nhật trạng thái đơn hàng.
        </small>
      </div>

      <button class="btn btn-outline-primary" :disabled="loading" @click="loadOrders">
        Làm mới
      </button>
    </div>

    <OrderFilter
      :keyword="keyword"
      :status="status"
      :order-type="orderType"
      @search="handleSearch"
    />

    <OrderTable
      :orders="orders"
      :loading="loading"
      @view-detail="openDetail"
      @change-status="confirmChangeStatus"
    />

    <div
      v-if="totalPages > 1"
      class="d-flex justify-content-between align-items-center mt-3"
    >
      <div class="text-muted small">
        Tổng {{ totalElements }} đơn hàng
      </div>

      <div class="btn-group">
        <button
          class="btn btn-outline-secondary"
          :disabled="page <= 0 || loading"
          @click="goToPage(page - 1)"
        >
          Trước
        </button>

        <button class="btn btn-outline-secondary disabled">
          Trang {{ page + 1 }} / {{ totalPages }}
        </button>

        <button
          class="btn btn-outline-secondary"
          :disabled="page >= totalPages - 1 || loading"
          @click="goToPage(page + 1)"
        >
          Sau
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
import { onMounted, ref } from "vue";
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

const page = ref(0);
const size = ref(10);
const totalElements = ref(0);
const totalPages = ref(0);

onMounted(() => {
  loadOrders();
});

async function loadOrders() {
  loading.value = true;

  try {
    const data = await orderService.getOrders({
      keyword: keyword.value,
      status: status.value,
      orderType: orderType.value,
      page: page.value,
      size: size.value,
    });

    orders.value = data.content || [];
    totalElements.value = data.totalElements || 0;
    totalPages.value = data.totalPages || 0;
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

function handleSearch(payload: {
  keyword: string;
  status: number | null;
  orderType: string;
}) {
  keyword.value = payload.keyword;
  status.value = payload.status;
  orderType.value = payload.orderType;
  page.value = 0;
  loadOrders();
}

function goToPage(targetPage: number) {
  if (targetPage < 0 || targetPage >= totalPages.value) return;

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
      text:
        error?.response?.data?.message ||
        "Vui lòng thử lại sau.",
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

  const result = await Swal.fire({
    icon: "question",
    title: "Xác nhận cập nhật trạng thái?",
    html: `
      <div style="text-align:left">
        <p><b>Đơn hàng:</b> ${order.orderCode}</p>
        <p><b>Trạng thái hiện tại:</b> ${order.statusText}</p>
        <p><b>Chuyển sang:</b> ${nextStatusText}</p>
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

    await loadOrders();

    if (showDetailModal.value && selectedOrder.value?.orderId === order.orderId) {
      selectedOrder.value = await orderService.getOrderDetail(order.orderId);
    }
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