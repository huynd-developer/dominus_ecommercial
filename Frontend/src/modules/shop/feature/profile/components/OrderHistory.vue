<template>
  <div class="card border-0 shadow-sm">
    <div
      class="card-header bg-white border-0 py-3 d-flex justify-content-between align-items-center"
    >
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
      <!-- THANH TAB TRẠNG THÁI -->
      <div v-if="!store.orderLoading" class="status-tabs mb-4">
        <div
          class="tab-item"
          :class="{ active: currentTab === 'ALL' }"
          @click="currentTab = 'ALL'"
        >
          Tất cả
        </div>
        <div
          class="tab-item"
          :class="{ active: currentTab === 0 }"
          @click="currentTab = 0"
        >
          Chờ xác nhận
        </div>
        <div
          class="tab-item"
          :class="{ active: currentTab === 1 }"
          @click="currentTab = 1"
        >
          Đã xác nhận
        </div>
        <div
          class="tab-item"
          :class="{ active: currentTab === 2 }"
          @click="currentTab = 2"
        >
          Đang giao
        </div>
        <div
          class="tab-item"
          :class="{ active: currentTab === 3 }"
          @click="currentTab = 3"
        >
          Hoàn thành
        </div>
        <div
          class="tab-item"
          :class="{ active: currentTab === 4 }"
          @click="currentTab = 4"
        >
          Đã hủy
        </div>

        <div
          class="tab-item"
          :class="{ active: currentTab === 6 }"
          @click="currentTab = 6"
        >
          Hoàn hàng
        </div>
      </div>

      <div v-if="store.orderLoading" class="text-center py-5">
        <div class="spinner-border"></div>
        <p class="text-muted mt-3 mb-0">Đang tải đơn hàng...</p>
      </div>

      <div v-else-if="filteredOrders.length === 0" class="empty-box">
        <span v-if="store.orders.length === 0">Bạn chưa có đơn hàng nào</span>
        <span v-else>Không có đơn hàng nào ở trạng thái này</span>
      </div>

      <div v-else class="order-list">
        <div
          v-for="order in filteredOrders"
          :key="order.orderId"
          class="order-card"
          :class="{ opened: isOrderOpen(order.orderId) }"
        >
          <button
            type="button"
            class="order-header-button"
            @click="toggleOrder(order.orderId)"
            :aria-expanded="isOrderOpen(order.orderId)"
          >
            <div class="order-header-content">
              <div>
                <strong>Đơn {{ generateOrderCode(order.orderId) }}</strong>
                <div class="small text-muted">
                  {{ formatDate(order.createdAt) }}
                </div>
              </div>

              <div class="order-header-right">
                <span :class="['badge', getStatusClass(order.status)]">
                  {{ getStatusText(order.status, order) }}
                </span>

                <div class="fw-bold mt-1 order-header-total">
                  {{ formatMoney(order.finalAmount) }}
                </div>
              </div>

              <i
                class="bi bi-chevron-down order-chevron"
                :class="{ rotated: isOrderOpen(order.orderId) }"
              ></i>
            </div>
          </button>

          <Transition
            name="order-collapse"
            @before-enter="beforeEnter"
            @enter="enter"
            @after-enter="afterEnter"
            @before-leave="beforeLeave"
            @leave="leave"
            @after-leave="afterLeave"
          >
            <div
              v-show="isOrderOpen(order.orderId)"
              class="order-collapse-body"
            >
              <div class="accordion-body custom-order-body">
                <div class="order-info-box">
                  <div class="info-item">
                    <span>Người nhận</span>
                    <strong>{{ order.customerName || "-" }}</strong>
                  </div>

                  <div class="info-item">
                    <span>SĐT</span>
                    <strong>{{ order.customerPhone || "-" }}</strong>
                  </div>

                  <div class="info-item full">
                    <span>Địa chỉ</span>
                    <strong>{{ order.shippingAddress || "-" }}</strong>
                  </div>

                  <div class="info-item">
                    <span>Thanh toán</span>
                    <strong>{{
                      formatPaymentMethod(order.paymentMethod)
                    }}</strong>
                  </div>

                  <div class="info-item">
                    <span>Loại đơn</span>
                    <strong>{{ formatOrderType(order.orderType) }}</strong>
                  </div>
                </div>

                <!-- BẮT ĐẦU KHỐI THEO DÕI ĐƠN HÀNG -->
                <div class="tracking-container mt-2 mb-4">
                  <h6 class="fw-bold mb-3">
                    <i class="bi bi-truck me-2"></i>Theo dõi kiện hàng
                  </h6>
                  <div class="timeline">
                    <div
                      v-for="(track, index) in getTrackingHistory(order)"
                      :key="index"
                      class="timeline-item"
                      :class="{
                        'is-active': track.active,
                        'is-cancel': track.isCancel,
                      }"
                    >
                      <div class="timeline-time">
                        <div class="t-date">
                          {{ formatTrackingTime(track.time).date }}
                        </div>
                        <div class="t-time">
                          {{ formatTrackingTime(track.time).time }}
                        </div>
                      </div>

                      <div class="timeline-marker">
                        <div class="dot"></div>
                        <div
                          v-if="index !== getTrackingHistory(order).length - 1"
                          class="line"
                        ></div>
                      </div>

                      <div class="timeline-content">
                        <div class="t-title">{{ track.title }}</div>
                        <div class="t-desc">{{ track.desc }}</div>
                        <img
                          v-if="track.img"
                          :src="track.img"
                          class="tracking-img mt-2"
                          alt="Bằng chứng giao hàng"
                        />
                      </div>
                    </div>
                  </div>
                </div>
                <!-- KẾT THÚC KHỐI THEO DÕI -->

                <div class="order-items">
                  <div
                    v-for="item in order.items"
                    :key="item.orderItemId"
                    class="order-item"
                  >
                    <div class="product-block">
                      <img
                        :src="getItemImage(item)"
                        class="item-img"
                        :alt="item.productName || 'Sản phẩm'"
                        @error="handleImageError"
                      />

                      <div class="product-info">
                        <div class="product-name">
                          {{ item.productName || "Sản phẩm" }}
                        </div>

                        <div class="brand-name">
                          {{ item.brandName || "Không rõ thương hiệu" }}
                        </div>

                        <div class="variant-line">
                          <span>
                            Dung tích:
                            <strong>{{ getCapacityText(item) }}</strong>
                          </span>

                          <span>
                            Loại chai:
                            <strong>{{ getBottleTypeText(item) }}</strong>
                          </span>

                          <span>
                            SL:
                            <strong>{{ item.quantity || 0 }}</strong>
                          </span>
                        </div>

                        <div class="date-line">
                          <span>
                            NSX:
                            <strong>{{
                              formatDateOnly(getManufacturingDate(item))
                            }}</strong>
                          </span>

                          <span>
                            HSD:
                            <strong>{{
                              formatDateOnly(getExpirationDate(item))
                            }}</strong>
                          </span>
                        </div>

                        <!-- KHỐI HIỂN THỊ ĐÁNH GIÁ -->
                        <div
                          v-if="reviewMap.has(item.orderItemId)"
                          class="my-review-box mt-3 w-100"
                        >
                          <div
                            class="d-flex justify-content-between align-items-center mb-2"
                          >
                            <div class="review-stars">
                              <i
                                v-for="star in 5"
                                :key="star"
                                class="bi"
                                :class="
                                  star <= (reviewMap.get(item.orderItemId)?.rating || 0)
                                    ? 'bi-star-fill text-warning'
                                    : 'bi-star text-muted'
                                "
                              ></i>
                              <span class="ms-2 fw-bold text-dark"
                                >{{ reviewMap.get(item.orderItemId)?.rating }}/5</span
                              >
                            </div>
                            <div
                              class="small text-muted"
                              style="font-size: 12px"
                            >
                              {{
                                formatDate(
                                  reviewMap.get(item.orderItemId)?.createdAt
                                )
                              }}
                            </div>
                          </div>

                          <div
                            v-if="reviewMap.get(item.orderItemId)?.comment"
                            class="review-comment small"
                          >
                            "{{ reviewMap.get(item.orderItemId)?.comment }}"
                          </div>

                          <div
                            v-if="getReviewMedia(item.orderItemId).length > 0"
                            class="d-flex gap-2 mt-2 flex-wrap"
                          >
                            <template
                              v-for="(url, idx) in getReviewMedia(item.orderItemId)"
                              :key="idx"
                            >
                              <video
                                v-if="String(url).match(/\.(mp4|webm|mov)/i)"
                                :src="String(url)"
                                class="review-media-preview"
                                controls
                                @click="openMediaPreview(String(url))"
                              ></video>
                              <img
                                v-else
                                :src="String(url)"
                                class="review-media-preview"
                                alt="Ảnh đánh giá"
                                @click="openMediaPreview(String(url))"
                              />
                            </template>
                          </div>
                        </div>

                        <div
                          v-else-if="
                            getReviewState(order.orderId, item.orderItemId)
                              ?.message
                          "
                          class="small mt-1"
                          :class="
                            getReviewState(order.orderId, item.orderItemId)
                              ?.canReview
                              ? 'text-success'
                              : 'text-muted'
                          "
                        >
                          {{
                            getReviewState(order.orderId, item.orderItemId)
                              ?.message
                          }}
                        </div>
                      </div>
                    </div>

                    <div class="review-action">
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
                        @click.stop="
                          openReview(order.orderId, item.orderItemId)
                        "
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
                    </div>
                  </div>
                </div>

                <div class="order-total-box">
                  <div>
                    <span>Tạm tính:</span>
                    <strong>{{ formatMoney(order.totalAmount) }}</strong>
                  </div>

                  <div v-if="order.discountAmount > 0">
                    <span>Giảm giá:</span>
                    <strong class="text-danger">
                      -{{ formatMoney(order.discountAmount) }}
                    </strong>
                  </div>

                  <div>
                    <span>Tổng thanh toán:</span>
                    <strong class="fs-5">
                      {{ formatMoney(order.finalAmount) }}
                    </strong>
                  </div>
                </div>

                <!-- KHỐI CÁC NÚT THAO TÁC -->
                <div class="text-end mt-3 d-flex justify-content-end align-items-center gap-2 flex-wrap">
                  <button
                    v-if="order.status === 3"
                    type="button"
                    class="btn btn-outline-dark btn-sm"
                    :disabled="reviewLoadingByOrder[order.orderId]"
                    @click="loadReviewableItems(order.orderId, true)"
                  >
                    Cập nhật đánh giá
                  </button>

                  <!-- 1. THANH TOÁN VNPAY -->
                  <template v-if="order.status === 0 && (order.paymentMethod || '').toUpperCase().includes('VNPAY')">
                    <!-- ĐỌC TRỰC TIẾP TỪ OBJECT MAP REACTIVE -->
                    <div
                      v-if="paidOrdersMap[String(order.orderId)] || (order as any).isPaymentReported"
                      class="d-flex align-items-center me-auto text-success fw-bold"
                      style="font-size: 14px"
                    >
                      <i class="bi bi-check-circle-fill me-1"></i> Đang chờ shop đối soát...
                    </div>
                    <template v-else-if="getRemainingSeconds(order.createdAt) > 0">
                      <div class="d-flex align-items-center me-3 text-danger fw-bold" style="font-size: 14px">
                        <i class="bi bi-clock-history me-1"></i> Hủy sau: {{ formatCountdown(getRemainingSeconds(order.createdAt)) }}
                      </div>
                      <button
                        class="btn btn-sm text-white"
                        style="background-color: #10b981; border-color: #10b981"
                        :disabled="store.orderLoading"
                        @click="repayVnpayOrder(order)"
                      >
                        <i class="bi bi-credit-card me-1"></i> Thanh toán VNPay ngay
                      </button>
                    </template>
                    <div v-else class="d-flex align-items-center me-3 text-muted fw-bold" style="font-size: 14px">
                      <i class="bi bi-clock-history me-1"></i> Đã quá hạn thanh toán
                    </div>
                  </template>

                  <!-- 2. THANH TOÁN VIETQR -->
                  <template v-if="order.status === 0 && (order.paymentMethod || '').toUpperCase().includes('VIETQR')">
                    <!-- ĐỌC TRỰC TIẾP TỪ OBJECT MAP REACTIVE -->
                    <div
                      v-if="paidOrdersMap[String(order.orderId)] || (order as any).isPaymentReported"
                      class="d-flex align-items-center me-auto text-success fw-bold"
                      style="font-size: 14px"
                    >
                      <i class="bi bi-check-circle-fill me-1"></i> Đang chờ shop đối soát...
                    </div>
                    <template v-else-if="getRemainingSeconds(order.createdAt) > 0">
                      <div class="d-flex align-items-center me-3 text-danger fw-bold" style="font-size: 14px">
                        <i class="bi bi-clock-history me-1"></i> Hủy sau: {{ formatCountdown(getRemainingSeconds(order.createdAt)) }}
                      </div>
                      <button
                        class="btn btn-sm text-white"
                        style="background-color: #0ea5e9; border-color: #0ea5e9"
                        :disabled="store.orderLoading"
                        @click="repayVietQrOrder(order)"
                      >
                        <i class="bi bi-qr-code-scan me-1"></i> Quét mã VietQR ngay
                      </button>
                    </template>
                    <div v-else class="d-flex align-items-center me-3 text-muted fw-bold" style="font-size: 14px">
                      <i class="bi bi-clock-history me-1"></i> Đã quá hạn thanh toán
                    </div>
                  </template>

                  <!-- CÁC NÚT CHUNG KHÁC -->
                  <button
                    v-if="order.canCancel"
                    class="btn btn-outline-danger btn-sm"
                    :disabled="store.orderLoading"
                    @click="cancelOrder(order)"
                  >
                    Hủy đơn
                  </button>

                  <button
                    v-if="order.status === 4 || order.status === 3"
                    class="btn btn-primary btn-sm px-3 text-white"
                    style="background-color: #bd9a5f; border-color: #bd9a5f"
                    @click="handleReorder(order)"
                  >
                    <i class="bi bi-cart-plus me-1"></i> Mua lại
                  </button>

                  <button
                    v-if="order.status === 3"
                    type="button"
                    class="btn btn-outline-danger btn-sm"
                    :disabled="store.orderLoading"
                    @click="requestReturn(order)"
                  >
                    Yêu cầu hoàn hàng
                  </button>

                  <button
                    v-if="order.status === 6"
                    type="button"
                    class="btn btn-outline-secondary btn-sm"
                    :disabled="store.orderLoading"
                    @click="cancelReturnRequest(order)"
                  >
                    Hủy yêu cầu hoàn hàng
                  </button>
                </div>
              </div>
            </div>
          </Transition>
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
import { computed, onMounted, ref, reactive, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import ReviewModal from "./ReviewModal.vue";
import { customerProfileService } from "../services/customerProfile.service";
import { useCustomerProfileStore } from "../stores/customerProfile.store";
import api from "@/common/api";
import type {
  CustomerOrderResponse,
  ReviewResponse,
  ReviewableOrderItemResponse,
} from "../types/profile.type";

const store = useCustomerProfileStore();
const router = useRouter();

const currentTab = ref<number | "ALL">("ALL");
const currentTime = ref(Date.now());
let countdownTimer: any = null;

const reviewLoading = ref(false);
const submittingReview = ref(false);
const reviewModalVisible = ref(false);
const selectedReviewItem = ref<ReviewableOrderItemResponse | null>(null);
const myReviews = ref<ReviewResponse[]>([]);
const openedOrderId = ref<number | null>(null);

// ==== GIẢI PHÁP TỐI THƯỢNG: REF MAP CỨNG REACTIVITY ====
// 1. Dùng Record map string -> boolean để Vue bắt dính sự thay đổi
const paidOrdersMap = ref<Record<string, boolean>>({});

// 2. Load dữ liệu lúc khởi tạo
const initPaidOrders = () => {
  try {
    const saved = localStorage.getItem('dominus_paid_orders');
    if (saved) {
      paidOrdersMap.value = JSON.parse(saved);
    }
  } catch (e) {}
};

// 3. Hàm đánh dấu và ép Vue render lại
const markAsPaid = (orderId: number | string) => {
  const idStr = String(orderId);
  paidOrdersMap.value[idStr] = true;
  // Trick phá vỡ object ép Vue nhận diện reactivity tức thì:
  paidOrdersMap.value = { ...paidOrdersMap.value }; 
  localStorage.setItem('dominus_paid_orders', JSON.stringify(paidOrdersMap.value));
};
// ========================================================

const reviewMap = computed(() => {
  const map = new Map<number, ReviewResponse>();
  myReviews.value.forEach((review) => {
    if (review.orderItemId) {
      map.set(review.orderItemId, review);
    }
  });
  return map;
});

const reviewableMap = reactive<Record<number, ReviewableOrderItemResponse[]>>({});
const reviewLoadingByOrder = reactive<Record<number, boolean>>({});

const generateOrderCode = (id: number | string | null | undefined) => {
  if (!id) return "N/A";
  return `DH-${String(id).padStart(6, "0")}`;
};

const filteredOrders = computed(() => {
  if (currentTab.value === "ALL") {
    return store.orders;
  }
  return store.orders.filter(
    (order: CustomerOrderResponse) => order.status === currentTab.value,
  );
});

const getRemainingSeconds = (createdAt: string | Date) => {
  const createTime = new Date(createdAt).getTime();
  const expireTime = createTime + 15 * 60 * 1000;
  const diff = Math.floor((expireTime - currentTime.value) / 1000);
  return diff > 0 ? diff : 0;
};

const formatCountdown = (seconds: number) => {
  const m = Math.floor(seconds / 60).toString().padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
};

onMounted(() => {
  initPaidOrders(); // Nạp dữ liệu lúc F5
  fetchOrdersAndReviews();

  countdownTimer = setInterval(() => {
    currentTime.value = Date.now();
    checkExpiredOrders();
  }, 1000);
});

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});

const toggleOrder = async (orderId: number) => {
  openedOrderId.value = openedOrderId.value === orderId ? null : orderId;

  if (openedOrderId.value === orderId) {
    const order = store.orders.find((o) => o.orderId === orderId);
    if (order && order.status === 3 && !reviewableMap[orderId]) {
      await loadReviewableItems(orderId, false);
    }
  }
};

const isOrderOpen = (orderId: number) => {
  return openedOrderId.value === orderId;
};

const beforeEnter = (el: Element) => {
  const element = el as HTMLElement;
  element.style.height = "0";
  element.style.opacity = "0";
  element.style.overflow = "hidden";
};

const enter = (el: Element) => {
  const element = el as HTMLElement;
  const height = element.scrollHeight;

  element.style.transition = "height 0.32s ease, opacity 0.24s ease";
  requestAnimationFrame(() => {
    element.style.height = `${height}px`;
    element.style.opacity = "1";
  });
};

const afterEnter = (el: Element) => {
  const element = el as HTMLElement;
  element.style.height = "auto";
  element.style.overflow = "";
  element.style.transition = "";
};

const beforeLeave = (el: Element) => {
  const element = el as HTMLElement;
  element.style.height = `${element.scrollHeight}px`;
  element.style.opacity = "1";
  element.style.overflow = "hidden";
};

const leave = (el: Element) => {
  const element = el as HTMLElement;

  element.style.transition = "height 0.28s ease, opacity 0.2s ease";
  requestAnimationFrame(() => {
    element.style.height = "0";
    element.style.opacity = "0";
  });
};

const afterLeave = (el: Element) => {
  const element = el as HTMLElement;
  element.style.height = "";
  element.style.opacity = "";
  element.style.overflow = "";
  element.style.transition = "";
};

const fetchOrdersAndReviews = async () => {
  try {
    await store.fetchOrders();
    await fetchMyReviews();

    // Phục hồi lại dữ liệu cho những đơn hàng đã chuyển khoản từ Map
    store.orders.forEach((order: any) => {
      if (paidOrdersMap.value[String(order.orderId)]) {
        order.isPaymentReported = true;
      }
    });

    if (
      openedOrderId.value &&
      !store.orders.some((order) => order.orderId === openedOrderId.value)
    ) {
      openedOrderId.value = null;
    }
  } catch (error) {
    showError(error, "Không tải được lịch sử đơn hàng");
  }
};

const checkExpiredOrders = () => {
  store.orders.forEach(async (order: any) => {
    const method = (order.paymentMethod || "").toUpperCase();
    if (
      order.status === 0 &&
      (method.includes("VIETQR") || method.includes("VNPAY"))
    ) {
      // Ngưng tự động hủy nếu đã ghi nhận thanh toán
      if (paidOrdersMap.value[String(order.orderId)] || order.isPaymentReported) {
        return;
      }

      // Hủy nếu quá hạn
      if (getRemainingSeconds(order.createdAt) <= 0) {
        try {
          await api.patch(`/customer/orders/${order.orderId}/cancel`);
          await fetchOrdersAndReviews();
        } catch (e) {
          // Bỏ qua lỗi ngầm
        }
      }
    }
  });
};

const fetchMyReviews = async () => {
  const res = await customerProfileService.getMyReviews();
  myReviews.value = res.data || [];
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
  return reviewMap.value.get(orderItemId);
};

const getReviewMedia = (orderItemId: number): string[] => {
  const review = getMyReviewByOrderItemId(orderItemId) as any;
  if (!review) return [];
  return (
    review.mediaFiles ||
    review.imageUrls ||
    review.images ||
    review.reviewImages ||
    []
  );
};

const openMediaPreview = (url: string) => {
  const isVideo = String(url).match(/\.(mp4|webm|mov)/i);
  Swal.fire({
    html: isVideo 
      ? `<video src="${url}" class="img-fluid rounded" controls autoplay style="max-height: 80vh;"></video>`
      : `<img src="${url}" class="img-fluid rounded" style="max-height: 80vh;" />`,
    showCloseButton: true,
    showConfirmButton: false,
    width: 'auto',
    background: '#000',
    customClass: {
      popup: 'swal-media-popup'
    }
  });
};

const getReviewState = (orderId: number, orderItemId: number) => {
  return reviewableMap[orderId]?.find(
    (item) => item.orderItemId === orderItemId,
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

const submitReview = async (payload: {
  rating: number;
  comment: string | null;
  files: File[];
}) => {
  if (!selectedReviewItem.value) {
    return;
  }

  const orderId = selectedReviewItem.value.orderId;
  const orderItemId = selectedReviewItem.value.orderItemId;

  try {
    submittingReview.value = true;

    const formData = new FormData();
    formData.append("orderItemId", String(orderItemId));
    formData.append("rating", String(payload.rating));

    if (payload.comment) {
      formData.append("comment", payload.comment);
    }

    if (payload.files && payload.files.length > 0) {
      payload.files.forEach((file) => {
        formData.append("mediaFiles", file);
      });
    }

    await customerProfileService.createReview(formData as any);

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

const handleReorder = async (order: any) => {
  const result = await Swal.fire({
    title: "Mua lại đơn hàng?",
    text: "Các sản phẩm trong đơn này sẽ được thêm vào giỏ hàng của bạn.",
    icon: "question",
    showCancelButton: true,
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#f8fafc",
    confirmButtonText: "Thêm vào giỏ",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-custom-confirm",
    },
  });

  if (result.isConfirmed) {
    try {
      store.orderLoading = true;

      const addPromises = order.items.map((item: any) => {
        const variantId =
          item.productVariantId || item.variantId || item.productId;

        return api.post("/v1/customer/cart/add", {
          productVariantId: Number(variantId),
          quantity: Number(item.quantity || 1),
        });
      });

      await Promise.all(addPromises);

      window.dispatchEvent(new Event("cart-updated"));

      toast("success", "Đã thêm sản phẩm vào giỏ hàng!");

      router.push("/cart");
    } catch (error) {
      showError(
        error,
        "Không thể thêm sản phẩm vào giỏ hàng lúc này. Vui lòng thử lại.",
      );
    } finally {
      store.orderLoading = false;
    }
  }
};

const getTrackingHistory = (order: any) => {
  const history = [];
  const baseDate = new Date(order.createdAt).getTime();

  history.push({
    time: new Date(baseDate),
    title: "Đơn hàng đã đặt",
    desc: "Đơn hàng đang chờ shop xác nhận.",
    active: order.status === 0,
  });

  if (order.status >= 1 && order.status !== 4) {
    history.push({
      time: new Date(baseDate + 2 * 60 * 60 * 1000),
      title: "Đang chuẩn bị hàng",
      desc: "Người bán đang chuẩn bị kiện hàng của bạn.",
      active: order.status === 1,
    });
  }

  if (order.status >= 2 && order.status !== 4) {
    history.push({
      time: new Date(baseDate + 14 * 60 * 60 * 1000),
      title: "Đã giao cho ĐVVC",
      desc: "Kiện hàng đã rời trung tâm phân loại và đang trên đường giao.",
      active: order.status === 2,
    });
  }

  if (order.status === 3) {
    history.push({
      time: order.completedAt
        ? new Date(order.completedAt)
        : new Date(baseDate + 48 * 60 * 60 * 1000),
      title: "Đã giao",
      desc: `Kiện hàng của bạn đã được giao. Người nhận: ${order.customerName || "Bạn"}`,
      img: "https://images.unsplash.com/photo-1615460549969-36fa19521a4f?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80",
      active: true,
    });
  }

  if (order.status === 4) {
    history.push({
      time: new Date(baseDate + 30 * 60 * 1000),
      title: "Đã hủy",
      desc: "Đơn hàng đã được hủy bỏ.",
      active: true,
      isCancel: true,
    });
  }

  return history.reverse();
};

const formatTrackingTime = (dateStr: string | number | Date) => {
  const date = new Date(dateStr);
  const day = date.getDate().toString().padStart(2, "0");
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const time = date.toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });
  return { date: `${day} tháng ${month}`, time };
};

const getCapacityText = (item: any) => {
  const raw =
    item?.capacity ??
    item?.capacityName ??
    item?.capacityText ??
    item?.capacityValue ??
    item?.volume ??
    item?.volumeValue ??
    null;

  if (raw !== null && raw !== undefined && String(raw).trim() !== "") {
    const text = String(raw).trim();
    return text.toLowerCase().includes("ml") ? text : `${text}ml`;
  }

  const sku = String(item?.sku || "");
  const match = sku.match(/-(\d+(?:\.\d+)?)-/);

  if (match?.[1]) {
    return `${match[1]}ml`;
  }

  return "Đang cập nhật";
};

const getBottleTypeText = (item: any) => {
  const raw =
    item?.bottleType ??
    item?.bottleTypeName ??
    item?.bottleName ??
    item?.variantBottleType ??
    null;

  if (raw !== null && raw !== undefined && String(raw).trim() !== "") {
    return String(raw).trim();
  }

  const sku = String(item?.sku || "").toUpperCase();

  if (sku.includes("FULL")) {
    return "Chai gốc Fullbox";
  }

  if (sku.includes("CHIET")) {
    return "Chai chiết";
  }

  return "Đang cập nhật";
};

const getManufacturingDate = (item: any) => {
  return (
    item?.manufacturingDate ??
    item?.mfgDate ??
    item?.manufactureDate ??
    item?.productionDate ??
    null
  );
};

const getExpirationDate = (item: any) => {
  return (
    item?.expirationDate ??
    item?.expiryDate ??
    item?.expiredDate ??
    item?.expDate ??
    null
  );
};

const formatMoney = (value: number | null | undefined) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return "-";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return date.toLocaleString("vi-VN", {
    hour12: false,
  });
};

const formatDateOnly = (value: string | null | undefined) => {
  if (!value) return "Đang cập nhật";

  const dateOnly = String(value).substring(0, 10);
  const date = new Date(`${dateOnly}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return "Đang cập nhật";
  }

  return date.toLocaleDateString("vi-VN");
};

const formatPaymentMethod = (value: string | null | undefined) => {
  if (!value) return "-";

  const normalized = String(value).toUpperCase();

  if (normalized === "COD") return "Thanh toán khi nhận hàng";
  if (normalized === "VNPAY") return "VNPay";
  if (normalized === "CASH") return "Tiền mặt";
  if (normalized === "BANK_TRANSFER") return "Chuyển khoản";
  if (normalized === "TRANSFER") return "Chuyển khoản";
  if (normalized === "MIXED") return "Tiền mặt + chuyển khoản";

  return value;
};

const formatOrderType = (value: string | null | undefined) => {
  if (!value) return "-";

  const normalized = String(value).toUpperCase();

  if (normalized === "ONLINE") return "Đơn online";
  if (normalized === "POS") return "Đơn tại quầy";

  return value;
};

const getStatusText = (status: number, order?: any) => {
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
      if (order && ["VIETQR", "VNPAY"].includes((order.paymentMethod || "").toUpperCase())) {
        const createTime = new Date(order.createdAt).getTime();
        const now = Date.now();
        if ((now - createTime >= 15 * 60 * 1000) || (order.statusText && order.statusText.includes("quá hạn"))) {
          return "Đã hủy (Quá hạn thanh toán)";
        }
      }
      return order?.statusText || "Đã hủy";
    case 5:
      return "Giao hàng thất bại";
    case 6:
      return "Yêu cầu hoàn hàng / đổi trả";
    case 7:
      return "Hoàn hàng / đổi trả hoàn tất";
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
    case 5:
      return "bg-dark";
    case 6:
      return "bg-secondary";
    case 7:
      return "bg-success";
    default:
      return "bg-secondary";
  }
};

const getErrorMessage = (error: any, fallback: string) => {
  const data = error?.response?.data;

  if (typeof data === "string") {
    return data;
  }

  return data?.message || error?.message || fallback;
};

const showError = (error: any, fallback: string) => {
  Swal.fire({
    icon: "error",
    title: "Có lỗi xảy ra",
    text: getErrorMessage(error, fallback),
    confirmButtonColor: "#bd9a5f",
  });
};

const toast = (
  icon: "success" | "error" | "warning" | "info",
  title: string,
) => {
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

const cancelOrder = async (order: CustomerOrderResponse) => {
  const { value: reason, isConfirmed } = await Swal.fire({
    title: "Hủy đơn hàng?",
    html: `
      <div style="text-align: left;">
        <label style="font-weight: 600; color: #1e293b; margin-bottom: 8px; display: block;">Vui lòng chọn lý do bạn muốn hủy đơn hàng này:</label>
        <div id="swal-cancel-reasons" style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 10px;">
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; color: #334155;">
            <input type="radio" name="swal-cancel-reason" value="Thay đổi thông tin" style="accent-color: #bd9a5f;" checked> Tôi muốn cập nhật địa chỉ / SĐT nhận hàng
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; color: #334155;">
            <input type="radio" name="swal-cancel-reason" value="Thay đổi sản phẩm" style="accent-color: #bd9a5f;"> Tôi muốn đổi phân loại hoặc số lượng
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; color: #334155;">
            <input type="radio" name="swal-cancel-reason" value="Thay đổi thanh toán" style="accent-color: #bd9a5f;"> Tôi muốn đổi phương thức thanh toán
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; color: #334155;">
            <input type="radio" name="swal-cancel-reason" value="Đổi ý" style="accent-color: #bd9a5f;"> Tôi không còn nhu cầu mua nữa
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; color: #334155;">
            <input type="radio" name="swal-cancel-reason" value="Giao hàng lâu" style="accent-color: #bd9a5f;"> Thời gian chuẩn bị/giao hàng quá lâu
          </label>
          <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; color: #334155;">
            <input type="radio" name="swal-cancel-reason" value="Khác" style="accent-color: #bd9a5f;"> Lý do khác
          </label>
        </div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận hủy",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    focusConfirm: false,
    preConfirm: () => {
      const selectedRadio = document.querySelector(
        'input[name="swal-cancel-reason"]:checked',
      ) as HTMLInputElement;

      if (!selectedRadio) {
        Swal.showValidationMessage("Vui lòng chọn một lý do để tiếp tục!");
        return false;
      }

      return selectedRadio.value;
    },
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-custom-confirm",
    },
  });

  if (isConfirmed && reason) {
    try {
      store.orderLoading = true;
      await api.patch(`/customer/orders/${order.orderId}/cancel`);
      await fetchOrdersAndReviews();
      toast("success", "Đã hủy đơn hàng thành công!");
    } catch (error) {
      showError(error, "Không thể hủy đơn hàng lúc này. Vui lòng thử lại.");
    } finally {
      store.orderLoading = false;
    }
  }
};

const requestReturn = async (order: CustomerOrderResponse) => {
  const { isConfirmed, value } = await Swal.fire({
    title: "Yêu cầu hoàn trả?",
    html: `
      <div style="text-align: left;">
        <label style="font-weight: 600; color: #1e293b; margin-bottom: 8px; display: block;">Vui lòng chọn lý do hoàn trả đơn hàng:</label>
        <div id="swal-return-reasons" style="display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px;">
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
            <input type="radio" name="swal-reason" value="Thiếu hàng" style="accent-color: #bd9a5f;"> Thiếu sản phẩm, phụ kiện, quà tặng
          </label>
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
            <input type="radio" name="swal-reason" value="Bể vỡ" style="accent-color: #bd9a5f;"> Sản phẩm bị bể vỡ, tràn đổ do vận chuyển
          </label>
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
            <input type="radio" name="swal-reason" value="Sai hàng" style="accent-color: #bd9a5f;"> Giao sai sản phẩm (sai mẫu mã, dung tích...)
          </label>
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
            <input type="radio" name="swal-reason" value="Hàng lỗi" style="accent-color: #bd9a5f;"> Sản phẩm bị lỗi (vòi xịt hỏng, mùi lạ...)
          </label>
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
            <input type="radio" name="swal-reason" value="Hàng giả" style="accent-color: #bd9a5f;"> Nghi ngờ sản phẩm không chính hãng
          </label>
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px;">
            <input type="radio" name="swal-reason" value="Khác" style="accent-color: #bd9a5f;"> Lý do khác
          </label>
        </div>

        <label style="font-weight: 600; color: #1e293b; margin-bottom: 6px; display: block;">Đính kèm hình ảnh / video bằng chứng:</label>
        <input type="file" id="swal-return-files" multiple accept="image/png, image/jpeg, image/jpg, image/webp, video/mp4, video/quicktime, video/webm" class="form-control form-control-sm" style="cursor: pointer;" />
        <small class="text-muted" style="display: block; margin-top: 4px;">Chỉ chấp nhận file ảnh hoặc video liên quan đến vấn đề sản phẩm.</small>
      </div>
    `,
    didOpen: () => {
      const fileInput = document.getElementById(
        "swal-return-files",
      ) as HTMLInputElement;
      if (fileInput) {
        fileInput.addEventListener("change", (e) => {
          const target = e.target as HTMLInputElement;
          if (target.files && target.files.length > 0) {
            const files = Array.from(target.files) as File[];
            for (const file of files) {
              const isImage = file.type.startsWith("image/");
              const isVideo = file.type.startsWith("video/");

              if (!isImage && !isVideo) {
                Swal.showValidationMessage(
                  `File "${file.name}" không hợp lệ! Vui lòng chỉ chọn ảnh hoặc video.`,
                );
                target.value = "";
                return;
              }
            }
            Swal.resetValidationMessage();
          }
        });
      }
    },
    showCancelButton: true,
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#f8fafc",
    confirmButtonText: "Gửi yêu cầu",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    focusConfirm: false,
    preConfirm: () => {
      const selectedRadio = document.querySelector(
        'input[name="swal-reason"]:checked',
      ) as HTMLInputElement;
      const fileInput = document.getElementById(
        "swal-return-files",
      ) as HTMLInputElement;

      if (!selectedRadio) {
        Swal.showValidationMessage("Vui lòng chọn một lý do hoàn trả!");
        return false;
      }

      const files = fileInput?.files
        ? (Array.from(fileInput.files) as File[])
        : [];

      for (const file of files) {
        const isImage = file.type.startsWith("image/");
        const isVideo = file.type.startsWith("video/");

        if (!isImage && !isVideo) {
          Swal.showValidationMessage(
            `File "${file.name}" không hợp lệ! Hệ thống chỉ chấp nhận file ảnh hoặc video.`,
          );
          return false;
        }
      }

      return {
        reason: selectedRadio.value,
        files: files,
      };
    },
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-custom-confirm",
    },
  });

  if (isConfirmed && value) {
    try {
      store.orderLoading = true;

      const formData = new FormData();
      formData.append("reason", value.reason);

      if (value.files && value.files.length > 0) {
        value.files.forEach((file: File) => {
          formData.append("mediaFiles", file);
        });
      }

      await api.put(
        `/customer/orders/${order.orderId}/request-return`,
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        },
      );

      await fetchOrdersAndReviews();
      toast("success", "Đã gửi yêu cầu hoàn hàng thành công!");
    } catch (error) {
      showError(error, "Không thể gửi yêu cầu hoàn hàng lúc này.");
    } finally {
      store.orderLoading = false;
    }
  }
};

const cancelReturnRequest = async (order: CustomerOrderResponse) => {
  const result = await Swal.fire({
    title: "Rút lại yêu cầu?",
    text: "Bạn có chắc chắn muốn hủy yêu cầu hoàn hàng/đổi trả này không? Đơn hàng sẽ trở về trạng thái Hoàn thành.",
    icon: "question",
    showCancelButton: true,
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#f8fafc",
    confirmButtonText: "Đồng ý",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-custom-confirm",
    },
  });

  if (result.isConfirmed) {
    try {
      store.orderLoading = true;
      await api.put(`/customer/orders/${order.orderId}/cancel-return`);
      await fetchOrdersAndReviews();
      toast("success", "Đã hủy yêu cầu hoàn trả thành công!");
    } catch (error) {
      showError(error, "Không thể thao tác lúc này.");
    } finally {
      store.orderLoading = false;
    }
  }
};

const repayVnpayOrder = async (order: CustomerOrderResponse) => {
  try {
    store.orderLoading = true;
    const res = await api.get(`/v1/orders/${order.orderId}/vnpay-url`);

    if (res.data?.paymentUrl) {
      toast("success", "Đang chuyển hướng đến VNPay...");
      window.location.href = res.data.paymentUrl;
    } else {
      showError(null, "Không lấy được đường dẫn thanh toán từ hệ thống.");
    }
  } catch (error) {
    showError(
      error,
      "Không thể tạo lại phiên thanh toán lúc này. Vui lòng thử lại sau.",
    );
  } finally {
    store.orderLoading = false;
  }
};

const repayVietQrOrder = async (order: CustomerOrderResponse) => {
  const amount = order.finalAmount;
  const orderIdStr = (order as any).orderCode || order.orderId;
  const qrUrl = `https://img.vietqr.io/image/970422-0123456789-compact2.png?amount=${amount}&addInfo=Thanh toan don ${orderIdStr}&accountName=SHOP DOMINUS`;

  const result = await Swal.fire({
    title: "Thanh toán VietQR",
    html: `
      <div class="text-center">
        <p class="text-muted small mb-3">Quét mã QR dưới đây bằng ứng dụng ngân hàng để thanh toán cho đơn hàng <b>${orderIdStr}</b>.</p>
        <img src="${qrUrl}" alt="Mã VietQR" class="img-fluid rounded mb-3" style="border: 2px dashed #bd9a5f; padding: 8px; max-width: 250px;" />
        <div class="alert alert-warning py-2 px-3 mb-0 w-100 text-start" style="font-size: 0.85rem;">
          <i class="bi bi-info-circle me-1"></i> Sau khi chuyển khoản thành công, shop sẽ kiểm tra và xác nhận đơn hàng của bạn.
        </div>
      </div>
    `,
    showConfirmButton: true,
    confirmButtonText: "Đã chuyển khoản",
    confirmButtonColor: "#10b981",
    showCancelButton: true,
    cancelButtonText: "Đóng",
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-custom-confirm",
    },
  });

  if (result.isConfirmed) {
    // Ép giao diện update tức thì
    markAsPaid(order.orderId);
    (order as any).isPaymentReported = true;

    Swal.fire({
      icon: "success",
      title: "Đã ghi nhận thanh toán",
      text: "Hệ thống đang chờ đối soát giao dịch từ ngân hàng. Đơn hàng sẽ được xác nhận trong ít phút nữa.",
      confirmButtonColor: "#10b981",
    });

    try {
      await api.post(`/v1/orders/${order.orderId}/report-payment`);
    } catch (error) {
      console.error("Lỗi đồng bộ báo cáo thanh toán:", error);
    }
  }
};

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
      <rect width="100%" height="100%" fill="#f3f4f6"/>
      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
        fill="#9ca3af" font-family="Arial" font-size="20">
        Không có ảnh
      </text>
    </svg>
  `);

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement;
  target.src = FALLBACK_IMAGE;
};

const getItemImage = (item: any) => {
  if (!item) return FALLBACK_IMAGE;
  let url = item.image || item.imageUrl || item.thumbnailUrl || item.mainImage;

  if (!url && item.productVariant) {
    url = item.productVariant.imageUrl || item.productVariant.image;
    if (!url && item.productVariant.product) {
      url =
        item.productVariant.product.mainImage ||
        item.productVariant.product.imageUrl;
      if (!url && item.productVariant.product.productImages?.length > 0) {
        url = item.productVariant.product.productImages[0].imageUrl;
      }
    }
  }
  if (!url && item.product) {
    url = item.product.mainImage || item.product.imageUrl;
    if (!url && item.product.productImages?.length > 0) {
      url = item.product.productImages[0].imageUrl;
    }
  }
  return url ? url : FALLBACK_IMAGE;
};
</script>

<style scoped>
.status-tabs {
  display: flex;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  border-radius: 8px 8px 0 0;
  overflow-x: auto;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 14px 12px;
  font-size: 15px;
  color: #555;
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
}

.tab-item:hover {
  color: #bd9a5f;
}

.tab-item.active {
  color: #bd9a5f;
  font-weight: bold;
  border-bottom: 3px solid #bd9a5f;
}

.tracking-container {
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.timeline {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  min-height: 80px;
  color: #94a3b8;
}

.timeline-item.is-active {
  color: #0f172a;
}

.timeline-item.is-cancel {
  color: #ef4444;
}

.timeline-time {
  width: 90px;
  flex-shrink: 0;
  text-align: right;
  padding-right: 15px;
  padding-top: 2px;
}

.t-date {
  font-size: 13px;
  font-weight: 600;
}

.t-time {
  font-size: 12px;
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 15px;
}

.timeline-marker .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #cbd5e1;
  margin-top: 5px;
  z-index: 2;
}

.timeline-item.is-active .timeline-marker .dot {
  background-color: #22c55e;
  box-shadow: 0 0 0 3px #dcfce7;
}

.timeline-item.is-cancel .timeline-marker .dot {
  background-color: #ef4444;
}

.timeline-marker .line {
  width: 2px;
  flex-grow: 1;
  background-color: #e2e8f0;
  margin-top: 5px;
}

.timeline-content {
  padding-bottom: 25px;
  flex-grow: 1;
}

.t-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 4px;
}

.t-desc {
  font-size: 13px;
  line-height: 1.5;
}

.tracking-img {
  width: 100%;
  max-width: 200px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.empty-box {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 16px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  border: 1px solid #dbeafe;
  border-radius: 14px;
  overflow: hidden;
  background: #ffffff;
  transition:
    border-color 0.25s ease,
    box-shadow 0.25s ease,
    transform 0.25s ease;
}

.order-card.opened {
  border-color: #93c5fd;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}

.order-header-button {
  width: 100%;
  border: none;
  background: #dbeafe;
  color: #0f172a;
  padding: 16px 20px;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.25s ease,
    color 0.25s ease;
}

.order-card.opened .order-header-button {
  background: #bfdbfe;
}

.order-header-button:hover {
  background: #bfdbfe;
}

.order-header-content {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto 24px;
  align-items: center;
  gap: 14px;
}

.order-header-right {
  text-align: right;
}

.order-header-total {
  color: #0f172a;
  font-size: 17px;
}

.order-chevron {
  font-size: 18px;
  color: #0f172a;
  transition: transform 0.28s ease;
}

.order-chevron.rotated {
  transform: rotate(180deg);
}

.order-collapse-body {
  background: #ffffff;
}

.custom-order-body {
  padding: 20px 24px 22px;
}

.order-info-box {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-item.full {
  grid-column: 1 / -1;
}

.info-item span {
  color: #64748b;
  font-size: 13px;
}

.info-item strong {
  color: #0f172a;
  font-size: 14px;
  word-break: break-word;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 14px;
  background: #ffffff;
}

.product-block {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.item-img {
  width: 58px;
  height: 58px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.product-info {
  min-width: 0;
}

.product-name {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.brand-name {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

.variant-line,
.date-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 7px;
  color: #475569;
  font-size: 13px;
}

.variant-line strong,
.date-line strong {
  color: #0f172a;
}

.review-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
}

.order-total-box {
  max-width: 450px;
  margin-left: auto;
  margin-top: 18px;
  background: #f8fafc;
  border-radius: 16px;
  padding: 18px;
}

.order-total-box > div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 10px;
}

.order-total-box > div:last-child {
  margin-bottom: 0;
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
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  width: 100%;
}

.review-stars {
  font-size: 15px;
}

.review-comment {
  color: #334155;
  background: #ffffff;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  font-style: italic;
  margin-top: 8px;
}

.review-media-preview {
  width: 68px;
  height: 68px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.review-media-preview:hover {
  transform: scale(1.05);
  border-color: #bd9a5f;
}

@media (max-width: 768px) {
  .order-header-content {
    grid-template-columns: 1fr 24px;
  }

  .order-header-right {
    grid-column: 1 / -1;
    grid-row: 2;
    text-align: left;
  }

  .order-chevron {
    grid-column: 2;
    grid-row: 1;
  }

  .order-info-box {
    grid-template-columns: 1fr;
  }

  .order-item {
    flex-direction: column;
  }

  .review-action {
    justify-content: flex-start;
  }

  .order-total-box {
    max-width: 100%;
  }
}
</style>

<style>
.swal-custom-popup {
  border-radius: 16px !important;
  font-family: inherit !important;
  padding: 0 0 24px 0 !important;
  overflow: hidden !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
}

.swal-media-popup {
  background: rgba(0, 0, 0, 0.9) !important;
  border-radius: 12px !important;
}

.swal-custom-popup .swal2-title {
  background-color: #06132b !important;
  color: #bd9a5f !important;
  padding: 20px 24px !important;
  margin: 0 0 16px 0 !important;
  font-size: 22px !important;
  font-weight: 700 !important;
  border-bottom: 3px solid #bd9a5f !important;
}

.swal-custom-popup .swal2-html-container {
  color: #475569 !important;
  font-size: 15px !important;
  margin: 0 24px !important;
  text-align: left !important;
}

.swal-custom-popup .swal2-actions {
  gap: 12px !important;
  margin-top: 20px !important;
  padding: 0 24px !important;
}

.swal-custom-cancel {
  background-color: #f1f5f9 !important;
  color: #475569 !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 12px 24px !important;
  font-weight: 600 !important;
  transition: all 0.2s ease !important;
}

.swal-custom-cancel:hover {
  background-color: #e2e8f0 !important;
  color: #0f172a !important;
}

.swal-custom-confirm {
  background-color: #dc2626 !important;
  color: #fff !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 12px 24px !important;
  font-weight: 600 !important;
  transition: all 0.2s ease !important;
}

.swal-custom-confirm:hover {
  background-color: #b91c1c !important;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3) !important;
}
</style>