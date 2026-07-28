<template>
  <a-modal
    :open="open"
    title="Chi tiết đơn hàng"
    width="1100px"
    :footer="null"
    @cancel="close"
    style="top: 20px"
  >
    <div v-if="order">
      <!-- Thông tin đơn -->
      <a-descriptions bordered :column="2" size="small" class="mb-4">
        <a-descriptions-item label="Mã đơn">
          <span class="fw-bold">#{{ order.id }}</span>
        </a-descriptions-item>

        <a-descriptions-item label="Trạng thái">
          <OrderStatusTag :status="order.status" />
        </a-descriptions-item>

        <a-descriptions-item label="Khách hàng">
          <i class="bi bi-person-circle text-muted me-1"></i>
          {{ order.customerName }}
        </a-descriptions-item>

        <a-descriptions-item label="Số điện thoại">
          <i class="bi bi-telephone text-muted me-1"></i>
          {{ order.customerPhone }}
        </a-descriptions-item>

        <a-descriptions-item label="Loại đơn">
          <a-tag :color="order.orderType === 'ONLINE' ? 'blue' : 'green'">
            {{
              order.orderType === "ONLINE"
                ? "Online (Giao hàng)"
                : "Mua tại quầy"
            }}
          </a-tag>
        </a-descriptions-item>

        <!-- Sửa lại mục Thanh toán -->
        <a-descriptions-item label="Thanh toán">
          <a-tag :color="getPaymentColor(order.paymentMethod)">
            {{ formatPaymentMethod(order.paymentMethod) }}
          </a-tag>
        </a-descriptions-item>

        <a-descriptions-item :span="2" label="Địa chỉ giao hàng">
          <i class="bi bi-geo-alt text-muted me-1"></i>
          {{ order.shippingAddress || "Không có dữ liệu" }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- Danh sách sản phẩm -->
      <div class="fw-bold mb-2">Chi tiết sản phẩm</div>
      <a-table
        :pagination="false"
        :data-source="order.items"
        row-key="id"
        bordered
        size="small"
      >
        <a-table-column title="Ảnh" width="80px" align="center">
          <template #default="{ record }">
            <img :src="record.image" width="55" class="rounded border" />
          </template>
        </a-table-column>

        <a-table-column title="Tên sản phẩm & Biến thể">
          <template #default="{ record }">
            <div>
              <div class="fw-bold text-primary">{{ record.productName }}</div>
              <div class="text-muted small mb-1">Mã SKU: {{ record.sku }}</div>

              <!-- Chi tiết biến thể được bọc thành Tag rõ ràng -->
              <div>
                <a-tag color="cyan" class="me-1">{{
                  record.capacityName
                }}</a-tag>
                <a-tag color="blue">{{ record.bottleTypeName }}</a-tag>
              </div>
            </div>
          </template>
        </a-table-column>

        <a-table-column
          title="SL"
          dataIndex="quantity"
          width="80px"
          align="center"
        />

        <a-table-column title="Đơn giá" width="150px" align="right">
          <template #default="{ record }">
            {{ money(record.finalPrice) }}
          </template>
        </a-table-column>

        <a-table-column title="Thành tiền" width="160px" align="right">
          <template #default="{ record }">
            <b class="text-dark">{{ money(record.lineTotal) }}</b>
          </template>
        </a-table-column>
      </a-table>

      <!-- Tổng tiền -->
      <a-row class="mt-4">
        <a-col :span="12" :offset="12">
          <a-descriptions bordered :column="1" size="small">
            <a-descriptions-item label="Tạm tính">
              <div class="text-end w-100">{{ money(order.totalAmount) }}</div>
            </a-descriptions-item>
            <a-descriptions-item label="Giảm giá">
              <div class="text-end w-100 text-success">
                - {{ money(order.discountAmount) }}
              </div>
            </a-descriptions-item>
            <a-descriptions-item label="Khách phải trả">
              <div class="text-end w-100 fw-bold text-danger fs-6">
                {{ money(order.finalAmount) }}
              </div>
            </a-descriptions-item>
          </a-descriptions>
        </a-col>
      </a-row>

      <!-- Nút thao tác -->
      <div class="mt-4 d-flex justify-content-end gap-2 border-top pt-3">
        <a-button @click="close">Đóng</a-button>

        <a-button v-if="order.status === 0" type="primary" @click="update(1)">
          <i class="bi bi-check-circle me-1"></i> Xác nhận đơn
        </a-button>

        <a-button v-if="order.status === 0" danger @click="cancel">
          <i class="bi bi-x-circle me-1"></i> Hủy đơn
        </a-button>

        <a-button v-if="order.status === 1" type="primary" @click="update(2)">
          <i class="bi bi-truck me-1"></i> Giao cho vận chuyển
        </a-button>

        <a-button
          v-if="order.status === 2"
          type="primary"
          style="background-color: #52c41a"
          @click="update(3)"
        >
          <i class="bi bi-bag-check me-1"></i> Giao thành công
        </a-button>

        <a-button v-if="order.status === 2" danger @click="update(5)">
          <i class="bi bi-bag-x me-1"></i> Giao thất bại
        </a-button>

        <a-button v-if="order.status === 3" type="default" @click="update(6)">
          <i class="bi bi-arrow-return-left me-1"></i> Khách yêu cầu hoàn
        </a-button>

        <a-button
          v-if="order.status === 6"
          type="primary"
          danger
          @click="update(7)"
        >
          <i class="bi bi-box-arrow-in-down me-1"></i> Xác nhận nhận hàng hoàn
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from "vue";
import Swal from "sweetalert2";
import { useOrderStore } from "../stores/orderStore";
import OrderStatusTag from "./OrderStatusTag.vue";

const props = defineProps<{ open: boolean }>();
const emit = defineEmits(["close"]);
const store = useOrderStore();

const order = computed(() => store.selectedOrder);

function close() {
  emit("close");
}

function money(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
}

async function update(status: number) {
  if (!order.value) return;
  try {
    await store.updateStatus(order.value.id, status);
    store.loadOrders(); // Cập nhật lại list bên ngoài
    await Swal.fire({
      icon: "success",
      title: "Thành công",
      text: "Cập nhật trạng thái thành công",
      timer: 1500,
      showConfirmButton: false,
    });
    close();
  } catch (e: any) {
    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: e.response?.data?.message || "Có lỗi xảy ra",
    });
  }
}

async function cancel() {
  if (!order.value) return;
  const result = await Swal.fire({
    title: "Xác nhận hủy đơn?",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Hủy đơn",
    cancelButtonText: "Đóng",
    confirmButtonColor: "#dc3545",
  });

  if (!result.isConfirmed) return;
  try {
    await store.cancelOrder(order.value.id);
    store.loadOrders();
    Swal.fire({
      icon: "success",
      title: "Đã hủy đơn",
      timer: 1500,
      showConfirmButton: false,
    });
    close();
  } catch (e: any) {
    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: e.response?.data?.message || "Có lỗi xảy ra",
    });
  }
}

// Hàm dịch phương thức thanh toán sang tiếng Việt
function formatPaymentMethod(method?: string) {
  if (!method) return "Không xác định";
  const upper = method.toUpperCase();
  
  if (upper.includes("COD")) return "Thanh toán khi nhận hàng (COD)";
  if (upper.includes("VIETQR") || upper.includes("QR")) return "Chuyển khoản VietQR"; // Đã thêm VietQR
  if (upper.includes("VNPAY")) return "Chuyển khoản VNPay";
  if (upper.includes("MOMO")) return "Chuyển khoản MoMo";
  if (upper.includes("CASH")) return "Tiền mặt";
  if (upper.includes("BANK") || upper.includes("TRANSFER")) return "Chuyển khoản ngân hàng";
  
  return method; // Nếu có mã lạ chưa bắt thì trả về nguyên gốc
}

// Hàm đổi màu Tag cho từng loại thanh toán
function getPaymentColor(method?: string) {
  if (!method) return "default";
  const upper = method.toUpperCase();
  
  if (upper.includes("COD") || upper.includes("CASH")) return "orange";
  if (upper.includes("VNPAY")) return "blue";
  if (upper.includes("MOMO")) return "magenta";
  if (upper.includes("VIETQR") || upper.includes("QR")) return "purple"; // VietQR cho màu tím
  
  return "cyan";
}
</script>
