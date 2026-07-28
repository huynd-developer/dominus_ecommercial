<template>
  <!-- ĐÃ SỬA: Thêm thuộc tính :locale để việt hóa chữ No data -->
  <a-table
    :columns="columns"
    :data-source="store.orders"
    :loading="store.loading"
    :pagination="false"
    row-key="id"
    :locale="{ emptyText: 'Không có dữ liệu' }"
  >
    <template #bodyCell="{ column, record }">
      <!-- Loại đơn (Việt hóa) -->
      <template v-if="column.key === 'orderType'">
        <a-tag :color="record.orderType === 'ONLINE' ? 'blue' : 'green'">
          <i
            class="bi"
            :class="record.orderType === 'ONLINE' ? 'bi-truck' : 'bi-shop'"
          ></i>
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
        <OrderStatusTag :status="record.status" />
      </template>

      <!-- Ngày -->
      <template v-if="column.key === 'createdAt'">
        {{ formatDate(record.createdAt) }}
      </template>

      <!-- Thao tác -->
      <template v-if="column.key === 'action'">
        <a-space>
          <a-button
            type="primary"
            ghost
            size="small"
            @click="detail(record.id)"
          >
            Chi tiết
          </a-button>

          <!-- Thao tác hủy đơn nhanh từ thầy yêu cầu -->
          <a-button
            v-if="record.status === 0"
            danger
            size="small"
            @click="cancelOrder(record.id)"
          >
            Hủy đơn
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
</template>

<script setup lang="ts">
import Swal from "sweetalert2";
import { useOrderStore } from "../stores/orderStore";
import OrderStatusTag from "./OrderStatusTag.vue";

const emit = defineEmits<{ (e: "detail", id: number): void }>();
const store = useOrderStore();

const columns = [
  { title: "Mã đơn", dataIndex: "id", key: "id", width: 90 },
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

function formatDate(date: string) {
  return new Date(date).toLocaleString("vi-VN");
}

function detail(id: number) {
  emit("detail", id);
}

// Xử lý Hủy đơn nhanh
async function cancelOrder(id: number) {
  const result = await Swal.fire({
    title: "Xác nhận hủy đơn?",
    text: `Bạn có chắc chắn muốn hủy đơn hàng #${id}?`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Vâng, hủy đơn",
    cancelButtonText: "Đóng",
    confirmButtonColor: "#dc3545",
  });

  if (!result.isConfirmed) return;

  try {
    await store.cancelOrder(id);
    store.loadOrders(); // Tải lại danh sách
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
  if (upper.includes("VNPAY")) return "Chuyển khoản VNPay";
  if (upper.includes("MOMO")) return "Chuyển khoản MoMo";
  if (upper.includes("CASH")) return "Tiền mặt";
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
  
  return "cyan";
}
</script>