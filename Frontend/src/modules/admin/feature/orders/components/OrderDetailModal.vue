<template>
  <div v-if="show" class="modal d-block" tabindex="-1">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
      <div class="modal-content">
        <div class="modal-header">
          <div>
            <h5 class="modal-title fw-bold mb-1">
              Chi tiết đơn hàng {{ order?.orderCode }}
            </h5>
            <small class="text-muted">
              Mã đơn #{{ order?.orderId }}
            </small>
          </div>

          <button type="button" class="btn-close" @click="$emit('close')"></button>
        </div>

        <div class="modal-body">
          <div v-if="!order" class="text-center text-muted py-4">
            Đang tải chi tiết đơn hàng...
          </div>

          <template v-else>
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <div class="border rounded p-3 h-100">
                  <h6 class="fw-bold mb-3">Thông tin khách hàng</h6>
                  <p class="mb-1">
                    <strong>Tên:</strong>
                    {{ order.customerName || "Khách vãng lai" }}
                  </p>
                  <p class="mb-1">
                    <strong>SĐT:</strong>
                    {{ order.customerPhone || "-" }}
                  </p>
                  <p class="mb-0">
                    <strong>Địa chỉ:</strong>
                    {{ order.shippingAddress || "-" }}
                  </p>
                </div>
              </div>

              <div class="col-md-6">
                <div class="border rounded p-3 h-100">
                  <h6 class="fw-bold mb-3">Thông tin đơn hàng</h6>
                  <p class="mb-1">
                    <strong>Loại đơn:</strong>
                    {{ formatOrderType(order.orderType) }}
                  </p>
                  <p class="mb-1">
                    <strong>Thanh toán:</strong>
                    {{ order.paymentMethod || "-" }}
                  </p>
                  <p class="mb-1">
                    <strong>Trạng thái:</strong>
                    <OrderStatusBadge
                      :status="order.status"
                      :status-text="order.statusText"
                    />
                  </p>
                  <p class="mb-0">
                    <strong>Ngày tạo:</strong>
                    {{ formatDate(order.createdAt) }}
                  </p>
                </div>
              </div>
            </div>

            <div class="table-responsive border rounded">
              <table class="table align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th>Sản phẩm</th>
                    <th>SKU</th>
                    <th>Dung tích</th>
                    <th>Loại chai</th>
                    <th class="text-end">SL</th>
                    <th class="text-end">Giá cuối</th>
                    <th class="text-end">Thành tiền</th>
                  </tr>
                </thead>

                <tbody>
                  <tr v-if="order.items.length === 0">
                    <td colspan="7" class="text-center text-muted py-4">
                      Đơn hàng này chưa có sản phẩm.
                    </td>
                  </tr>

                  <tr v-for="item in order.items" :key="item.orderItemId">
                    <td>
                      <div class="d-flex align-items-center gap-2">
                        <img
                          :src="getImageUrl(item.imageUrl)"
                          alt="product"
                          class="rounded border"
                          style="width: 48px; height: 48px; object-fit: cover"
                        />
                        <div>
                          <div class="fw-semibold">
                            {{ item.productName || "Sản phẩm" }}
                          </div>
                          <small v-if="item.note" class="text-muted">
                            {{ item.note }}
                          </small>
                        </div>
                      </div>
                    </td>
                    <td>{{ item.sku || "-" }}</td>
                    <td>{{ item.capacity || "-" }}</td>
                    <td>{{ item.bottleType || "-" }}</td>
                    <td class="text-end">{{ item.quantity }}</td>
                    <td class="text-end">{{ formatMoney(item.finalPrice) }}</td>
                    <td class="text-end fw-semibold">
                      {{ formatMoney(item.lineTotal) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="row justify-content-end mt-3">
              <div class="col-md-4">
                <div class="border rounded p-3">
                  <div class="d-flex justify-content-between mb-2">
                    <span>Tổng tiền:</span>
                    <strong>{{ formatMoney(order.totalAmount) }}</strong>
                  </div>

                  <div class="d-flex justify-content-between mb-2">
                    <span>Giảm giá:</span>
                    <strong>{{ formatMoney(order.discountAmount) }}</strong>
                  </div>

                  <div class="d-flex justify-content-between fs-5">
                    <span>Thanh toán:</span>
                    <strong class="text-danger">
                      {{ formatMoney(order.finalAmount) }}
                    </strong>
                  </div>

                  <div v-if="order.voucher" class="mt-2 small text-muted">
                    Voucher: {{ order.voucher.voucherCode }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="$emit('close')">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>

  <div v-if="show" class="modal-backdrop show"></div>
</template>

<script setup lang="ts">
import type { AdminOrderResponse } from "../types/order.type";
import OrderStatusBadge from "./OrderStatusBadge.vue";

defineProps<{
  show: boolean;
  order: AdminOrderResponse | null;
}>();

defineEmits<{
  close: [];
}>();

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
      <rect width="100%" height="100%" fill="#f2f2f2"/>
      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#999" font-size="12">
        No image
      </text>
    </svg>
  `);

function getImageUrl(value?: string | null) {
  if (!value || !value.trim()) return FALLBACK_IMAGE;

  const cleanValue = value.trim();

  if (cleanValue.startsWith("http")) return cleanValue;
  if (cleanValue.startsWith("/")) return `http://localhost:8080${cleanValue}`;

  return `http://localhost:8080/${cleanValue}`;
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

<style scoped>
.modal {
  background: rgba(0, 0, 0, 0.1);
}
</style>
