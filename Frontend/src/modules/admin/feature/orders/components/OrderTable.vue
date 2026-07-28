<template>
  <div class="card border-0 shadow-sm">
    <div class="table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>Loại đơn</th>
            <th>Thanh toán</th>
            <th class="text-end">Tổng tiền</th>
            <th>Trạng thái</th>
            <th>Ngày tạo</th>
            <th class="text-end">Thao tác</th>
          </tr>
        </thead>

        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="text-center py-4 text-muted">
              Đang tải đơn hàng...
            </td>
          </tr>

          <tr v-else-if="orders.length === 0">
            <td colspan="8" class="text-center py-4 text-muted">
              Không có đơn hàng nào.
            </td>
          </tr>

          <tr v-for="order in orders" :key="order.orderId">
            <td>
              <div class="fw-semibold">{{ order.orderCode }}</div>
              <small class="text-muted">#{{ order.orderId }}</small>
            </td>

            <td>
              <div class="fw-semibold">
                {{ order.customerName || "Khách vãng lai" }}
              </div>
              <small class="text-muted">
                {{ order.customerPhone || "Không có SĐT" }}
              </small>
            </td>

            <td>
              <span class="badge text-bg-light border">
                {{ formatOrderType(order.orderType) }}
              </span>
            </td>

            <td>{{ order.paymentMethod || "-" }}</td>

            <td class="text-end fw-semibold">
              {{ formatMoney(order.finalAmount) }}
            </td>

            <td>
              <OrderStatusBadge
                :status="order.status"
                :status-text="order.statusText"
              />
            </td>

            <td>
              <small>{{ formatDate(order.createdAt) }}</small>
            </td>

            <td class="text-end">
              <div class="btn-group">
                <button
                  class="btn btn-sm btn-outline-primary"
                  @click="$emit('view-detail', order.orderId)"
                >
                  Xem
                </button>

                <button
                  v-for="action in getAvailableActions(order.status)"
                  :key="action.status"
                  class="btn btn-sm"
                  :class="action.class"
                  @click="$emit('change-status', order, action.status)"
                >
                  {{ action.label }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AdminOrderResponse } from "../types/order.type";
import OrderStatusBadge from "./OrderStatusBadge.vue";

defineProps<{
  orders: AdminOrderResponse[];
  loading: boolean;
}>();

defineEmits<{
  "view-detail": [orderId: number];
  "change-status": [order: AdminOrderResponse, status: number];
}>();

function getAvailableActions(status: number) {
  switch (status) {
    case 0:
      return [
        {
          status: 1,
          label: "Xác nhận",
          class: "btn-outline-success",
        },
        {
          status: 4,
          label: "Hủy",
          class: "btn-outline-danger",
        },
      ];
    case 1:
      return [
        {
          status: 2,
          label: "Giao hàng",
          class: "btn-outline-primary",
        },
        {
          status: 4,
          label: "Hủy",
          class: "btn-outline-danger",
        },
      ];
    case 2:
      return [
        {
          status: 3,
          label: "Hoàn thành",
          class: "btn-outline-success",
        },
        {
          status: 5,
          label: "Giao thất bại",
          class: "btn-outline-dark",
        },
      ];
    case 3:
      return [
        {
          status: 6,
          label: "Yêu cầu hoàn",
          class: "btn-outline-warning",
        },
      ];
    case 6:
      return [
        {
          status: 7,
          label: "Hoàn tất",
          class: "btn-outline-success",
        },
      ];
    default:
      return [];
  }
}

function formatMoney(value?: number | null) {
  const amount = Number(value || 0);

  return amount.toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
}

function formatDate(value?: string | null) {
  if (!value) return "-";

  return new Date(value).toLocaleString("vi-VN");
}

function formatOrderType(type?: string | null) {
  switch ((type || "").toUpperCase()) {
    case "ONLINE":
      return "Online";
    case "IN_STORE":
    case "POS":
      return "Tại quầy";
    default:
      return "-";
  }
}
</script>