<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3 d-flex justify-content-between align-items-center">
      <div>
        <h5 class="mb-0 fw-bold">Lịch sử đơn hàng</h5>
        <div class="small text-muted mt-1">
          Đánh giá sản phẩm chỉ mở khi đơn hàng đã hoàn thành
        </div>
      </div>

      <button
        class="btn btn-outline-dark btn-sm"
        :disabled="store.orderLoading || reviewLoading"
        @click="fetchOrdersAndReviews"
      >
        <span
          v-if="store.orderLoading || reviewLoading"
          class="spinner-border spinner-border-sm me-1"
        ></span>
        Làm mới
      </button>
    </div>

    <div class="card-body">
      <div v-if="store.orderLoading" class="text-center py-5">
        <div class="spinner-border"></div>
        <p class="text-muted mt-3 mb-0">Đang tải đơn hàng...</p>
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
                    {{ order.statusText || getStatusText(order.status) }}
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
                      <th class="text-end">Đánh giá</th>
                    </tr>
                  </thead>

                  <tbody>
                    <tr v-for="item in order.items" :key="item.orderItemId">
                      <td>
                        <div class="d-flex align-items-start gap-2">
                          <img
                            v-if="item.image"
                            :src="item.image"
                            class="item-img"
                            alt="item"
                          />

                          <div v-else class="item-img placeholder-img">
                            No
                          </div>

                          <div class="min-width-0">
                            <div class="fw-semibold">
                              {{ item.productName }}
                            </div>

                            <div class="small text-muted">
                              {{ item.brandName || "Không rõ thương hiệu" }}
                            </div>

                            <div
                              v-if="getMyReviewByOrderItemId(item.orderItemId)"
                              class="my-review-box mt-2"
                            >
                              <div class="review-stars">
                                <i
                                  v-for="star in 5"
                                  :key="star"
                                  class="bi"
                                  :class="
                                    star <= (getMyReviewByOrderItemId(item.orderItemId)?.rating || 0)
                                      ? 'bi-star-fill'
                                      : 'bi-star'
                                  "
                                ></i>

                                <span class="ms-2 small text-muted">
                                  {{ getMyReviewByOrderItemId(item.orderItemId)?.rating }}/5
                                </span>
                              </div>

                              <div
                                v-if="getMyReviewByOrderItemId(item.orderItemId)?.comment"
                                class="small review-comment"
                              >
                                "{{ getMyReviewByOrderItemId(item.orderItemId)?.comment }}"
                              </div>

                              <div class="small text-muted mt-1">
                                Đã đánh giá:
                                {{ formatDate(getMyReviewByOrderItemId(item.orderItemId)?.createdAt) }}
                              </div>
                            </div>

                            <div
                              v-else-if="getReviewState(order.orderId, item.orderItemId)?.message"
                              class="small mt-1"
                              :class="
                                getReviewState(order.orderId, item.orderItemId)?.canReview
                                  ? 'text-success'
                                  : 'text-muted'
                              "
                            >
                              {{ getReviewState(order.orderId, item.orderItemId)?.message }}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td>{{ item.sku || "-" }}</td>

                      <td class="text-end">
                        {{ item.quantity }}
                      </td>

                      <td class="text-end">
                        {{ formatMoney(item.originalPrice) }}
                      </td>

                      <td class="text-end">
                        {{ formatMoney(item.discountAmount) }}
                      </td>

                      <td class="text-end fw-bold">
                        {{ formatMoney(item.finalPrice) }}
                      </td>

                      <td class="text-end">
                        <button
                          v-if="order.status === 3"
                          type="button"
                          class="btn btn-sm"
                          :class="
                            isReviewed(order.orderId, item.orderItemId)
                              ? 'btn-outline-secondary'
                              : 'btn-review'
                          "
                          :disabled="
                            reviewLoadingByOrder[order.orderId] ||
                            !canReview(order.orderId, item.orderItemId)
                          "
                          @click="openReview(order.orderId, item.orderItemId)"
                        >
                          <span
                            v-if="reviewLoadingByOrder[order.orderId]"
                            class="spinner-border spinner-border-sm me-1"
                          ></span>

                          {{
                            isReviewed(order.orderId, item.orderItemId)
                              ? "Đã đánh giá"
                              : "Đánh giá"
                          }}
                        </button>

                        <button
                          v-else
                          type="button"
                          class="btn btn-sm btn-outline-secondary"
                          disabled
                        >
                          Chưa mở
                        </button>
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

              <div class="text-end mt-3 d-flex justify-content-end gap-2">
                <button
                  v-if="order.status === 3"
                  type="button"
                  class="btn btn-outline-dark btn-sm"
                  :disabled="reviewLoadingByOrder[order.orderId]"
                  @click="loadReviewableItems(order.orderId, true)"
                >
                  Cập nhật đánh giá
                </button>

                <button
                  v-if="order.status === 0"
                  class="btn btn-outline-danger"
                  :disabled="store.orderLoading"
                  @click="cancelOrder(order)"
                >
                  Hủy đơn
                </button>

                <span
                  v-else-if="order.status !== 0"
                  class="text-muted small align-self-center"
                >
                  Đơn hàng không còn được hủy
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ReviewModal
      v-model="reviewModalVisible"
      :item="selectedReviewItem"
      :loading="submittingReview"
      @submit="submitReview"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import ReviewModal from "./ReviewModal.vue";
import { customerProfileService } from "../services/customerProfile.service";
import { useCustomerProfileStore } from "../stores/customerProfile.store";
import type {
  CustomerOrderResponse,
  ReviewResponse,
  ReviewableOrderItemResponse,
} from "../types/profile.type";

const store = useCustomerProfileStore();

const reviewLoading = ref(false);
const submittingReview = ref(false);
const reviewModalVisible = ref(false);
const selectedReviewItem = ref<ReviewableOrderItemResponse | null>(null);
const myReviews = ref<ReviewResponse[]>([]);

const reviewableMap = reactive<Record<number, ReviewableOrderItemResponse[]>>({});
const reviewLoadingByOrder = reactive<Record<number, boolean>>({});

const completedOrders = computed(() => {
  return store.orders.filter((order: CustomerOrderResponse) => order.status === 3);
});

onMounted(() => {
  fetchOrdersAndReviews();
});

const fetchOrdersAndReviews = async () => {
  try {
    await store.fetchOrders();
    await fetchMyReviews();
    await loadCompletedReviewStates();
  } catch (error) {
    showError(error, "Không tải được lịch sử đơn hàng");
  }
};

const fetchMyReviews = async () => {
  const res = await customerProfileService.getMyReviews();
  myReviews.value = res.data || [];
};

const loadCompletedReviewStates = async () => {
  if (completedOrders.value.length === 0) {
    return;
  }

  reviewLoading.value = true;

  try {
    await Promise.all(
      completedOrders.value.map((order: CustomerOrderResponse) =>
        loadReviewableItems(order.orderId, false)
      )
    );
  } finally {
    reviewLoading.value = false;
  }
};

const loadReviewableItems = async (orderId: number, showToast: boolean) => {
  try {
    reviewLoadingByOrder[orderId] = true;

    const res = await customerProfileService.getReviewableItemsByOrder(orderId);
    reviewableMap[orderId] = res.data || [];

    if (showToast) {
      toast("success", "Đã cập nhật trạng thái đánh giá");
    }
  } catch (error) {
    showError(error, "Không tải được trạng thái đánh giá");
  } finally {
    reviewLoadingByOrder[orderId] = false;
  }
};

const getMyReviewByOrderItemId = (orderItemId: number) => {
  return myReviews.value.find((review) => review.orderItemId === orderItemId);
};

const getReviewState = (orderId: number, orderItemId: number) => {
  return reviewableMap[orderId]?.find(
    (item) => item.orderItemId === orderItemId
  );
};

const canReview = (orderId: number, orderItemId: number) => {
  return getReviewState(orderId, orderItemId)?.canReview === true;
};

const isReviewed = (orderId: number, orderItemId: number) => {
  return getReviewState(orderId, orderItemId)?.reviewed === true;
};

const openReview = async (orderId: number, orderItemId: number) => {
  let state = getReviewState(orderId, orderItemId);

  if (!state) {
    await loadReviewableItems(orderId, false);
    state = getReviewState(orderId, orderItemId);
  }

  if (!state) {
    await Swal.fire({
      icon: "error",
      title: "Không tìm thấy sản phẩm",
      text: "Không tìm thấy sản phẩm cần đánh giá trong đơn hàng.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (!state.canReview) {
    await Swal.fire({
      icon: "info",
      title: "Chưa thể đánh giá",
      text: state.message || "Sản phẩm này chưa đủ điều kiện đánh giá.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  selectedReviewItem.value = state;
  reviewModalVisible.value = true;
};

const submitReview = async (payload: { rating: number; comment: string | null }) => {
  if (!selectedReviewItem.value) {
    return;
  }

  const orderId = selectedReviewItem.value.orderId;
  const orderItemId = selectedReviewItem.value.orderItemId;

  try {
    submittingReview.value = true;

    await customerProfileService.createReview({
      orderItemId,
      rating: payload.rating,
      comment: payload.comment,
    });

    reviewModalVisible.value = false;
    selectedReviewItem.value = null;

    toast("success", "Gửi đánh giá thành công");

    await fetchMyReviews();
    await loadReviewableItems(orderId, false);
  } catch (error) {
    showError(error, "Không gửi được đánh giá");
  } finally {
    submittingReview.value = false;
  }
};

const cancelOrder = async (order: CustomerOrderResponse) => {
  await store.cancelOrder(order);
  await fetchOrdersAndReviews();
};

const formatMoney = (value: number | null | undefined) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return "-";
  return new Date(value).toLocaleString("vi-VN");
};

const getStatusText = (status: number) => {
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
    default:
      return "Không xác định";
  }
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

const getErrorMessage = (error: any, fallback: string) => {
  return (
    error?.response?.data?.message ||
    error?.response?.data ||
    error?.message ||
    fallback
  );
};

const showError = (error: any, fallback: string) => {
  Swal.fire({
    icon: "error",
    title: "Có lỗi xảy ra",
    text: getErrorMessage(error, fallback),
    confirmButtonColor: "#bd9a5f",
  });
};

const toast = (icon: "success" | "error" | "warning" | "info", title: string) => {
  Swal.fire({
    toast: true,
    position: "top-end",
    icon,
    title,
    showConfirmButton: false,
    timer: 1800,
    timerProgressBar: true,
  });
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
  flex-shrink: 0;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 11px;
}

.min-width-0 {
  min-width: 0;
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

.btn-review {
  background: #111827;
  color: #ffffff;
  border-radius: 999px;
}

.btn-review:hover:not(:disabled) {
  background: #bd9a5f;
  color: #ffffff;
}

.btn-review:disabled {
  background: #d1d5db;
  border-color: #d1d5db;
  color: #6b7280;
}

.my-review-box {
  background: #fffaf0;
  border: 1px solid #f3e2bd;
  border-radius: 12px;
  padding: 8px 10px;
  max-width: 420px;
}

.review-stars {
  color: #bd9a5f;
  font-size: 14px;
}

.review-comment {
  color: #374151;
  margin-top: 4px;
  font-style: italic;
}
</style>