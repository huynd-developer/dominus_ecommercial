<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3 d-flex justify-content-between">
      <h5 class="mb-0 fw-bold">Lịch sử đơn hàng</h5>

      <button
        class="btn btn-outline-dark btn-sm"
        :disabled="store.orderLoading"
        @click="store.fetchOrders()"
      >
        Làm mới
      </button>
    </div>

    <div class="card-body">
      <div v-if="store.orderLoading" class="text-center py-5">
        <div class="spinner-border"></div>
      </div>

      <div v-else-if="store.orders.length === 0" class="empty-box">
        Bạn chưa có đơn hàng nào
      </div>

      <div v-else class="accordion" id="orderAccordion">
        <div
          v-for="order in store.orders"
          :key="order.orderId"
          class="accordion-item mb-3 border rounded-3 overflow-hidden"
        >
          <h2 class="accordion-header">
            <button
              class="accordion-button collapsed"
              type="button"
              data-bs-toggle="collapse"
              :data-bs-target="`#order-${order.orderId}`"
            >
              <div class="w-100 d-flex flex-wrap justify-content-between gap-2 pe-3">
                <div>
                  <strong>Đơn #{{ order.orderId }}</strong>
                  <div class="small text-muted">
                    {{ formatDate(order.createdAt) }}
                  </div>
                </div>

                <div class="text-end">
                  <span :class="['badge', getStatusClass(order.status)]">
                    {{ order.statusText }}
                  </span>
                  <div class="fw-bold mt-1">
                    {{ formatMoney(order.finalAmount) }}
                  </div>
                </div>
              </div>
            </button>
          </h2>

          <div
            :id="`order-${order.orderId}`"
            class="accordion-collapse collapse"
            data-bs-parent="#orderAccordion"
          >
            <div class="accordion-body">
              <div class="row g-2 mb-3 small">
                <div class="col-12 col-md-6">
                  <strong>Người nhận:</strong> {{ order.customerName }}
                </div>

                <div class="col-12 col-md-6">
                  <strong>SĐT:</strong> {{ order.customerPhone }}
                </div>

                <div class="col-12">
                  <strong>Địa chỉ:</strong> {{ order.shippingAddress }}
                </div>

                <div class="col-12 col-md-6">
                  <strong>Thanh toán:</strong> {{ order.paymentMethod }}
                </div>

                <div class="col-12 col-md-6">
                  <strong>Loại đơn:</strong> {{ order.orderType }}
                </div>
              </div>

              <div class="table-responsive">
                <table class="table table-sm align-middle">
                  <thead class="table-light">
                    <tr>
                      <th>Sản phẩm</th>
                      <th>SKU</th>
                      <th class="text-end">SL</th>
                      <th class="text-end">Giá gốc</th>
                      <th class="text-end">Giảm</th>
                      <th class="text-end">Thành tiền</th>
                    </tr>
                  </thead>

                  <tbody>
                    <tr v-for="item in order.items" :key="item.orderItemId">
                      <td>
                        <div class="d-flex align-items-center gap-2">
                          <img
                            v-if="item.image"
                            :src="item.image"
                            class="item-img"
                            alt="item"
                          />

                          <div v-else class="item-img placeholder-img">
                            No
                          </div>

                          <div>
                            <div class="fw-semibold">
                              {{ item.productName }}
                            </div>
                            <div class="small text-muted">
                              {{ item.brandName }}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td>{{ item.sku }}</td>
                      <td class="text-end">{{ item.quantity }}</td>
                      <td class="text-end">
                        {{ formatMoney(item.originalPrice) }}
                      </td>
                      <td class="text-end">
                        {{ formatMoney(item.discountAmount) }}
                      </td>
                      <td class="text-end fw-bold">
                        {{ formatMoney(item.finalPrice) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="order-total-box">
                <div>
                  <span>Tạm tính:</span>
                  <strong>{{ formatMoney(order.totalAmount) }}</strong>
                </div>

                <div>
                  <span>Giảm giá:</span>
                  <strong>{{ formatMoney(order.discountAmount) }}</strong>
                </div>

                <div>
                  <span>Tổng thanh toán:</span>
                  <strong class="fs-5">{{ formatMoney(order.finalAmount) }}</strong>
                </div>
              </div>

              <div class="text-end mt-3">
                <button
                  v-if="order.status === 0"
                  class="btn btn-outline-danger"
                  :disabled="store.orderLoading"
                  @click="store.cancelOrder(order)"
                >
                  Hủy đơn
                </button>

                <span v-else class="text-muted small">
                  Đơn hàng không còn được hủy
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { useCustomerProfileStore } from "../stores/customerProfile.store";

const store = useCustomerProfileStore();

const formatMoney = (value: number) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};

const formatDate = (value: string) => {
  if (!value) return "-";

  return new Date(value).toLocaleString("vi-VN");
};

const getStatusClass = (status: number) => {
  switch (status) {
    case 0:
      return "bg-warning text-dark";
    case 1:
      return "bg-info text-dark";
    case 2:
      return "bg-primary";
    case 3:
      return "bg-success";
    case 4:
      return "bg-danger";
    default:
      return "bg-secondary";
  }
};
</script>

<style scoped>
.empty-box {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 16px;
}

.item-img {
  width: 46px;
  height: 46px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 11px;
}

.order-total-box {
  max-width: 360px;
  margin-left: auto;
  background: #f9fafb;
  border-radius: 14px;
  padding: 14px;
}

.order-total-box > div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
</style>