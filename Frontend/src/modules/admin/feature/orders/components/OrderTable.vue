<template>
  <div class="order-table-card">
    <a-table
      :columns="columns"
      :data-source="orders"
      :loading="loading"
      :pagination="false"
      row-key="orderId"
      size="middle"
      :scroll="{ x: 1100 }"
      :locale="{ emptyText: 'Không có dữ liệu' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'orderCode'">
          <div class="order-code-cell">
            <span class="order-code">{{ record.orderCode }}</span>
          </div>
        </template>

        <template v-if="column.key === 'customer'">
          <div class="customer-cell">
            <span class="customer-name">
              {{ record.customerName || "Khách vãng lai" }}
            </span>
            <span class="customer-phone">
              {{ record.customerPhone || "-" }}
            </span>
          </div>
        </template>

        <template v-if="column.key === 'orderType'">
          <a-tag
            class="order-type-tag"
            :color="record.orderType === 'ONLINE' ? 'blue' : 'green'"
          >
            <i
              class="bi"
              :class="record.orderType === 'ONLINE' ? 'bi-truck' : 'bi-shop'"
            ></i>
            {{ record.orderType === "ONLINE" ? "Online" : "Tại quầy" }}
          </a-tag>
        </template>

        <template v-if="column.key === 'paymentMethod'">
          <div class="payment-cell">
            <a-tag class="payment-tag" :color="getPaymentColor(record.paymentMethod)">
              {{ formatPaymentMethod(record.paymentMethod) }}
            </a-tag>

            <template v-if="record.status === 0 && ['VIETQR', 'VNPAY'].includes((record.paymentMethod || '').toUpperCase())">
              <a-tag v-if="record.isPaymentReported" color="success" class="mt-1 fw-bold">
                <i class="bi bi-check-circle-fill me-1"></i> Khách đã thanh toán!
              </a-tag>
            </template>
          </div>
        </template>

        <template v-if="column.key === 'finalAmount'">
          <span class="amount-text">
            {{ money(record.finalAmount) }}
          </span>
        </template>

        <template v-if="column.key === 'status'">
          <OrderStatusBadge
            :status="record.status"
            :status-text="record.statusText"
          />
        </template>

        <template v-if="column.key === 'createdAt'">
          <div class="date-cell">
            {{ formatDate(record.createdAt) }}
          </div>
        </template>

        <template v-if="column.key === 'action'">
          <div class="action-cell">
            <a-button
              class="detail-btn"
              type="default"
              size="small"
              @click="emit('viewDetail', record.orderId)"
            >
              Chi tiết
            </a-button>

            <a-button
              v-for="action in getAvailableActions(record)"
              :key="action.status"
              size="small"
              :type="action.type"
              :danger="action.danger"
              @click="emit('changeStatus', record, action.status)"
            >
              {{ action.label }}
            </a-button>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import OrderStatusBadge from "./OrderStatusBadge.vue";

const props = defineProps<{
  orders: any[];
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "viewDetail", id: number): void;
  (e: "changeStatus", order: any, status: number): void;
}>();

void props;

const columns = [
  { title: "Mã đơn", key: "orderCode", width: 130 },
  { title: "Khách hàng", key: "customer", width: 190 },
  { title: "Loại đơn", key: "orderType", width: 120 },
  { title: "Thanh toán", dataIndex: "paymentMethod", key: "paymentMethod", width: 210 },
  { title: "Tổng tiền", key: "finalAmount", width: 140, align: "right" },
  { title: "Trạng thái", key: "status", width: 170 },
  { title: "Ngày tạo", key: "createdAt", width: 160 },
  { title: "Thao tác", key: "action", align: "right", width: 220 },
];

function money(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
}

function formatDate(date?: string | null) {
  if (!date) return "-";
  return new Date(date).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function formatPaymentMethod(method?: string) {
  if (!method) return "Không xác định";

  const upper = method.toUpperCase().trim();
  if (upper === "MIXED_VIETQR") return "Tiền mặt + VietQR";
  if (upper === "MIXED_VNPAY") return "Tiền mặt + VNPay";
  if (upper === "MIXED_CASH") return "Thanh toán hỗn hợp";
  if (upper.includes("MIXED")) return "Thanh toán hỗn hợp";
  if (upper.includes("COD")) return "Thanh toán tiền mặt";
  if (upper.includes("VIETQR") || upper.includes("QR")) return "Chuyển khoản VietQR";
  if (upper.includes("VNPAY")) return "VNPay";
  if (upper.includes("MOMO")) return "MoMo";
  if (upper.includes("CASH")) return "Tiền mặt";
  if (upper === "HOLD") return "Phiếu treo";
  if (upper.includes("BANK") || upper.includes("TRANSFER")) return "Chuyển khoản";

  return method;
}

function getPaymentColor(method?: string) {
  if (!method) return "default";

  const upper = method.toUpperCase().trim();
  if (upper.includes("MIXED")) return "geekblue";
  if (upper.includes("COD") || upper.includes("CASH")) return "orange";
  if (upper.includes("VNPAY")) return "blue";
  if (upper.includes("MOMO")) return "magenta";
  if (upper.includes("VIETQR") || upper.includes("QR")) return "purple";
  if (upper === "HOLD") return "volcano";

  return "cyan";
}

function isReturnWorkflowOrder(order: any) {
  return Number(order?.status) === 6;
}

function getAvailableActions(order: any) {
  const status = Number(order?.status);

  /*
   * Đơn đang yêu cầu hoàn hàng không được xử lý bằng nút đổi trạng thái thường.
   * Luồng đúng phải xử lý trong chi tiết: Chấp nhận/Từ chối -> Đã hoàn tiền.
   */
  if (isReturnWorkflowOrder(order)) {
    return [];
  }

  switch (status) {
    case 0:
      return [
        { status: 1, label: "Xác nhận", type: "primary", danger: false },
        { status: 4, label: "Hủy", type: "primary", danger: true },
      ];

    case 1:
      return [
        { status: 2, label: "Giao hàng", type: "primary", danger: false },
      ];

    case 2:
      return [
        { status: 3, label: "Hoàn thành", type: "primary", danger: false },
        { status: 5, label: "Giao thất bại", type: "default", danger: true },
      ];

    case 3:
      return [];

    case 6:
      return [];

    default:
      return [];
  }
}
</script>

<style scoped>
.order-table-card {
  background: #ffffff;
  border: 1px solid #edf0f3;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.order-code-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-code {
  color: #111827;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.order-id {
  color: #9ca3af;
  font-size: 12px;
  font-weight: 600;
}

.customer-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.customer-name {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.customer-phone {
  color: #6b7280;
  font-size: 12px;
}

.payment-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.order-type-tag,
.payment-tag {
  border-radius: 999px;
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 700;
}

.payment-tag {
  max-width: 190px;
  white-space: normal;
  line-height: 1.35;
}

.amount-text {
  color: #ef233c;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.date-cell {
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.action-cell {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.detail-btn {
  border-color: #d1d5db;
  color: #374151;
}

.detail-btn:hover {
  border-color: #bd9a5f;
  color: #bd9a5f;
}

:deep(.ant-table) {
  color: #111827;
  font-size: 14px;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc !important;
  color: #374151;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.35px;
  border-bottom: 1px solid #e5e7eb !important;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px !important;
  border-bottom: 1px solid #f1f5f9 !important;
  vertical-align: middle;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: #fffaf2 !important;
}

:deep(.ant-btn-sm) {
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

:deep(.ant-table-container) {
  border-radius: 16px;
}
</style>
