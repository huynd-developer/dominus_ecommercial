<template>
  <!-- SỬA 1: Bọc tất cả vào 1 thẻ div để tránh lỗi Fragment -->
  <div class="order-table-wrapper">
    <a-table
      :columns="columns"
      :data-source="orders" 
      :loading="loading"
      :pagination="false"
      row-key="orderId"
      :locale="{ emptyText: 'Không có dữ liệu' }"
    >
      <template #bodyCell="{ column, record }">
        <!-- Format lại cột Mã đơn (Dùng luôn orderCode từ backend) -->
        <template v-if="column.key === 'orderCode'">
          <span class="fw-bold text-dark">{{ record.orderCode }}</span>
        </template>

        <!-- Loại đơn (Việt hóa) -->
        <template v-if="column.key === 'orderType'">
          <a-tag :color="record.orderType === 'ONLINE' ? 'blue' : 'green'">
            <i class="bi" :class="record.orderType === 'ONLINE' ? 'bi-truck' : 'bi-shop'"></i>
            {{ record.orderType === "ONLINE" ? " Online" : " Tại quầy" }}
          </a-tag>
        </template>

        <!-- Thanh toán -->
        <template v-if="column.key === 'paymentMethod'">
          <a-tag :color="getPaymentColor(record.paymentMethod)">
            {{ formatPaymentMethod(record.paymentMethod) }}
          </a-tag>
        </template>

        <!-- Tiền -->
        <template v-if="column.key === 'finalAmount'">
          <span class="fw-bold text-danger">{{ money(record.finalAmount) }}</span>
        </template>

        <!-- Trạng thái -->
        <template v-if="column.key === 'status'">
          <OrderStatusBadge :status="record.status" :status-text="record.statusText" />
        </template>

        <!-- Ngày -->
        <template v-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>

        <!-- Thao tác -->
        <template v-if="column.key === 'action'">
          <a-space>
            <!-- Nút Xem chi tiết (Lúc nào cũng có) -->
            <a-button type="default" size="small" @click="emit('viewDetail', record.orderId)">
              Chi tiết
            </a-button>

            <!-- Vòng lặp render các nút thao tác theo đúng trạng thái đơn hàng -->
            <a-button
              v-for="action in getAvailableActions(record.status)"
              :key="action.status"
              size="small"
              :type="action.type"
              :danger="action.danger"
              @click="emit('changeStatus', record, action.status)"
            >
              {{ action.label }}
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <div class="mt-4 text-end">
      <a-pagination
        :current="store.currentPage + 1"
        :pageSize="store.pageSize"
        :total="store.totalElements"
        show-size-changer
        @change="changePage"
        @showSizeChange="changeSize"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import Swal from "sweetalert2";
import { useOrderStore } from "../stores/orderStore";
import OrderStatusBadge from "./OrderStatusBadge.vue";

// SỬA 3: Khai báo props để hứng data (orders, loading) từ component cha truyền xuống
const props = defineProps<{
  orders: any[];
  loading: boolean;
}>();

// SỬA 4: Khai báo emits chuẩn với tên mà component cha đang vòi (viewDetail, changeStatus)
const emit = defineEmits<{
  (e: "viewDetail", id: number): void;
  (e: "changeStatus", order: any, status: number): void;
}>();

const store = useOrderStore();

const columns = [
  { title: "Mã đơn", dataIndex: "orderCode", key: "orderCode", width: 110 },
  { title: "Khách hàng", dataIndex: "customerName", key: "customerName" },
  { title: "SĐT", dataIndex: "customerPhone", key: "customerPhone" },
  { title: "Loại đơn", key: "orderType" },
  { title: "Thanh toán", dataIndex: "paymentMethod", key: "paymentMethod" },
  { title: "Tổng tiền", key: "finalAmount" },
  { title: "Trạng thái", key: "status" },
  { title: "Ngày tạo", key: "createdAt" },
  { title: "Thao tác", key: "action", align: "center", width: 180 },
];

function money(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
}

function formatDate(date?: string | null) {
  if (!date) return "-";
  return new Date(date).toLocaleString("vi-VN");
}

async function cancelOrder(id: number, code: string) {
  const result = await Swal.fire({
    title: "Xác nhận hủy đơn?",
    text: `Bạn có chắc chắn muốn hủy đơn hàng ${code}?`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Vâng, hủy đơn",
    cancelButtonText: "Đóng",
    confirmButtonColor: "#dc3545",
  });

  if (!result.isConfirmed) return;

  try {
    await store.cancelOrder(id);
    Swal.fire({
      icon: "success",
      title: "Đã hủy đơn",
      timer: 1500,
      showConfirmButton: false,
    });
  } catch (e: any) {
    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: e.response?.data?.message || "Có lỗi xảy ra",
    });
  }
}

function changePage(page: number) {
  store.changePage(page - 1);
}

function changeSize(page: number, size: number) {
  store.currentPage = 0;
  store.pageSize = size;
  store.loadOrders();
}

function formatPaymentMethod(method?: string) {
  if (!method) return "Không xác định";
  const upper = method.toUpperCase();
  
  if (upper.includes("COD")) return "Thanh toán khi nhận hàng (COD)";
  if (upper.includes("VIETQR") || upper.includes("QR")) return "Chuyển khoản VietQR";
  if (upper.includes("VNPAY")) return "Thanh toán qua VNPay";
  if (upper.includes("MOMO")) return "Thanh toán qua MoMo";
  
  if (upper.includes("CASH")) return "Tiền mặt (Tại quầy)";
  if (upper.includes("MIXED")) return "Thanh toán hỗn hợp";
  if (upper === "HOLD") return "Phiếu treo";
  if (upper.includes("BANK") || upper.includes("TRANSFER")) return "Chuyển khoản ngân hàng";
  
  return method; 
}

function getPaymentColor(method?: string) {
  if (!method) return "default";
  const upper = method.toUpperCase();
  
  if (upper.includes("COD") || upper.includes("CASH")) return "orange";
  if (upper.includes("VNPAY")) return "blue";
  if (upper.includes("MOMO")) return "magenta";
  if (upper.includes("VIETQR") || upper.includes("QR")) return "purple"; 
  if (upper.includes("MIXED")) return "geekblue";
  if (upper === "HOLD") return "volcano";
  
  return "cyan";
}

function getAvailableActions(status: number) {
  switch (status) {
    case 0:
      return [
        { status: 1, label: "Xác nhận", type: "primary", danger: false },
        { status: 4, label: "Hủy", type: "primary", danger: true },
      ];
    case 1:
      return [
        { status: 2, label: "Giao hàng", type: "primary", danger: false },
        { status: 4, label: "Hủy", type: "primary", danger: true },
      ];
    case 2:
      return [
        { status: 3, label: "Hoàn thành", type: "primary", danger: false },
        { status: 5, label: "Giao thất bại", type: "default", danger: true },
      ];
    case 3:
      return [
        { status: 6, label: "Yêu cầu hoàn", type: "dashed", danger: true },
      ];
    case 6:
      return [
        { status: 7, label: "Hoàn tất", type: "primary", danger: false },
      ];
    default:
      return [];
  }
}
</script>