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
                  {{ order.statusText || getStatusText(order.status) }}
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

                <!-- BẮT ĐẦU KHỐI THEO DÕI ĐƠN HÀNG (FAKE TRACKING) -->
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
                                star <=
                                (getMyReviewByOrderItemId(item.orderItemId)
                                  ?.rating || 0)
                                  ? 'bi-star-fill'
                                  : 'bi-star'
                              "
                            ></i>

                            <span class="ms-2 small text-muted">
                              {{
                                getMyReviewByOrderItemId(item.orderItemId)
                                  ?.rating
                              }}/5
                            </span>
                          </div>

                          <div
                            v-if="
                              getMyReviewByOrderItemId(item.orderItemId)
                                ?.comment
                            "
                            class="small review-comment"
                          >
                            "{{
                              getMyReviewByOrderItemId(item.orderItemId)
                                ?.comment
                            }}"
                          </div>

                          <div class="small text-muted mt-1">
                            Đã đánh giá:
                            {{
                              formatDate(
                                getMyReviewByOrderItemId(item.orderItemId)
                                  ?.createdAt
                              )
                            }}
                          </div>
                        </div>

                        <div
                          v-else-if="
                            getReviewState(order.orderId, item.orderItemId)
                              ?.message &&
                            !getReviewState(order.orderId, item.orderItemId)
                              ?.canReview
                          "
                          class="small mt-1 text-muted"
                        >
                          {{
                            getReviewState(order.orderId, item.orderItemId)
                              ?.message
                          }}
                        </div>
                      </div>
                    </div>

                    <div class="order-item-side">
                      <div v-if="hasItemPrice(item)" class="item-price-box">
                        <div
                          v-if="hasItemSale(item)"
                          class="item-original-price"
                        >
                          {{ formatMoney(getItemOriginalUnitPrice(item)) }}
                        </div>

                        <div class="item-final-price">
                          {{ formatMoney(getItemFinalUnitPrice(item)) }}
                        </div>

                        <div
                          v-if="Number(item.quantity || 0) > 1"
                          class="item-line-total"
                        >
                          Thành tiền:
                          {{ formatMoney(getItemLineTotal(item)) }}
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
                </div>

                <div class="order-summary-row">
                  <div v-if="order.status === 4" class="order-cancel-info">
                    <div class="cancel-info-title">
                      <i class="bi bi-x-circle me-1"></i>
                      Lý do hủy:
                    </div>

                    <div class="cancel-info-text">
                      {{ getOrderCancelReason(order) }}
                    </div>

                    <div
                      v-if="getOrderCancelledAt(order)"
                      class="cancel-info-time"
                    >
                      Thời gian hủy: {{ formatDate(getOrderCancelledAt(order)) }}
                    </div>
                  </div>

                  <div
                    v-else-if="isReturnInfoVisible(order)"
                    class="order-return-info"
                  >
                    <div class="return-info-title">
                      <i class="bi bi-arrow-counterclockwise me-1"></i>
                      Thông tin hoàn hàng:
                    </div>

                    <div class="return-info-line">
                      <span>Lý do:</span>
                      <strong>{{ getOrderReturnReason(order) }}</strong>
                    </div>

                    <div
                      v-if="getOrderReturnDescription(order)"
                      class="return-info-description"
                    >
                      {{ getOrderReturnDescription(order) }}
                    </div>

                    <div
                      v-if="getOrderReturnMedia(order).length > 0"
                      class="return-media-section"
                    >
                      <div class="return-media-label">Ảnh/video bằng chứng:</div>

                      <div class="return-media-list">
                        <button
                          v-for="(media, index) in getOrderReturnMedia(order)"
                          :key="`${media.url}-${index}`"
                          type="button"
                          class="return-media-button"
                          @click.stop="openReturnMediaPreview(order, index)"
                        >
                          <video
                            v-if="media.isVideo"
                            :src="media.url"
                            class="return-media-thumb"
                            muted
                            preload="metadata"
                          ></video>

                          <img
                            v-else
                            :src="media.url"
                            class="return-media-thumb"
                            alt="Ảnh bằng chứng hoàn hàng"
                          />

                          <span class="return-media-overlay">
                            <i
                              class="bi"
                              :class="media.isVideo ? 'bi-play-circle' : 'bi-zoom-in'"
                            ></i>
                          </span>
                        </button>
                      </div>
                    </div>
                  </div>

                  <div class="order-total-box">
                    <div>
                      <span>Tạm tính:</span>
                      <strong>{{ formatMoney(order.totalAmount) }}</strong>
                    </div>

                    <div>
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
                </div>

                <!-- KHỐI CÁC NÚT THAO TÁC -->
                <div class="text-end mt-3 d-flex justify-content-end gap-2">
                  <button
                    v-if="order.status === 0 && order.paymentMethod === 'VNPAY'"
                    class="btn btn-sm text-white"
                    style="background-color: #10b981; border-color: #10b981"
                    :disabled="store.orderLoading"
                    @click="repayVnpayOrder(order)"
                  >
                    <i class="bi bi-credit-card me-1"></i> Thanh toán VNPay ngay
                  </button>

                  <button
                    v-if="order.canCancel"
                    class="btn btn-outline-danger btn-sm"
                    :disabled="store.orderLoading"
                    @click="cancelOrder(order)"
                  >
                    Hủy đơn
                  </button>

                  <!-- NÚT MUA LẠI -->
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

                  <!-- NÚT HỦY YÊU CẦU HOÀN HÀNG -->
                  <button
                    v-if="order.status === 6"
                    type="button"
                    class="btn btn-outline-secondary btn-sm"
                    :disabled="store.orderLoading"
                    @click="cancelReturnRequest(order)"
                  >
                    Hủy yêu cầu hoàn hàng
                  </button>

                  <span
                    v-if="
                      !order.canCancel &&
                      order.status !== 3 &&
                      order.status !== 4
                    "
                    class="text-muted small align-self-center"
                  >
                    Đơn hàng không còn được hủy
                  </span>
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
    <ReturnRequestModal
      v-model="returnModalVisible"
      :order="selectedReturnOrder"
      :loading="submittingReturn"
      :default-email="getDefaultReturnEmail()"
      @submit="submitReturnRequest"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import ReviewModal from "./ReviewModal.vue";
import ReturnRequestModal from "./ReturnRequestModal.vue";
import { customerProfileService } from "../services/customerProfile.service";
import { useCustomerProfileStore } from "../stores/customerProfile.store";
import api from "@/common/api";
import type {
  CustomerOrderResponse,
  ReturnRequestSubmitPayload,
  ReviewResponse,
  ReviewableOrderItemResponse,
} from "../types/profile.type";

const store = useCustomerProfileStore();
const router = useRouter();

const currentTab = ref<number | "ALL">("ALL");

const reviewLoading = ref(false);
const submittingReview = ref(false);
const reviewModalVisible = ref(false);
const selectedReviewItem = ref<ReviewableOrderItemResponse | null>(null);
const myReviews = ref<ReviewResponse[]>([]);
const openedOrderId = ref<number | null>(null);

const returnModalVisible = ref(false);
const selectedReturnOrder = ref<CustomerOrderResponse | null>(null);
const submittingReturn = ref(false);

const reviewableMap = reactive<Record<number, ReviewableOrderItemResponse[]>>(
  {}
);
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
    (order: CustomerOrderResponse) => order.status === currentTab.value
  );
});

const completedOrders = computed(() => {
  return store.orders.filter(
    (order: CustomerOrderResponse) => order.status === 3
  );
});

onMounted(() => {
  fetchOrdersAndReviews();
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
        "Không thể thêm sản phẩm vào giỏ hàng lúc này. Vui lòng thử lại."
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
      desc: `Kiện hàng của bạn đã được giao. Người nhận: ${
        order.customerName || "Bạn"
      }`,
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

const toMoneyNumber = (value: unknown) => {
  if (value === null || value === undefined || value === "") {
    return 0;
  }

  const numberValue = Number(value);

  return Number.isFinite(numberValue) ? numberValue : 0;
};

const pickMoneyValue = (...values: unknown[]) => {
  for (const value of values) {
    const numberValue = toMoneyNumber(value);

    if (numberValue > 0) {
      return numberValue;
    }
  }

  return 0;
};

const getItemUnitDiscount = (item: any) => {
  return pickMoneyValue(
    item?.discountAmount,
    item?.unitDiscountAmount,
    item?.unitDiscount,
    item?.flashSaleDiscount,
    item?.itemDiscount
  );
};

const getItemFinalUnitPrice = (item: any) => {
  return pickMoneyValue(
    item?.finalPrice,
    item?.unitFinalPrice,
    item?.priceAfterDiscount,
    item?.discountedPrice,
    item?.salePrice,
    item?.sellingPrice,
    item?.unitPrice,
    item?.price,
    item?.originalPrice
  );
};

const getItemOriginalUnitPrice = (item: any) => {
  const originalPrice = pickMoneyValue(
    item?.originalPrice,
    item?.unitOriginalPrice,
    item?.basePrice,
    item?.listedPrice,
    item?.priceBeforeDiscount,
    item?.regularPrice,
    item?.unitPrice,
    item?.price
  );

  const finalPrice = getItemFinalUnitPrice(item);
  const discountAmount = getItemUnitDiscount(item);

  if (originalPrice > finalPrice) {
    return originalPrice;
  }

  if (finalPrice > 0 && discountAmount > 0) {
    return finalPrice + discountAmount;
  }

  return originalPrice || finalPrice;
};

const hasItemPrice = (item: any) => {
  return getItemFinalUnitPrice(item) > 0 || getItemOriginalUnitPrice(item) > 0;
};

const hasItemSale = (item: any) => {
  return getItemOriginalUnitPrice(item) > getItemFinalUnitPrice(item);
};

const getItemLineTotal = (item: any) => {
  const lineTotal = pickMoneyValue(
    item?.lineTotal,
    item?.totalPrice,
    item?.itemTotal,
    item?.subtotal
  );

  if (lineTotal > 0) {
    return lineTotal;
  }

  return getItemFinalUnitPrice(item) * Number(item?.quantity || 0);
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

const getOrderCancelReason = (order: any) => {
  const reason =
    order?.cancelReason ??
    order?.cancellationReason ??
    order?.cancelNote ??
    order?.cancelDescription ??
    order?.reason ??
    null;

  const cleanReason = String(reason || "").trim();

  return cleanReason || "Chưa có lý do hủy";
};

const getOrderCancelledAt = (order: any) => {
  return (
    order?.cancelledAt ??
    order?.canceledAt ??
    order?.cancelAt ??
    order?.cancelDate ??
    null
  );
};

const getOrderReturnRequest = (order: any) => {
  return (
    order?.returnRequest ??
    order?.latestReturnRequest ??
    order?.returnInfo ??
    order?.refundRequest ??
    null
  );
};

const isReturnInfoVisible = (order: any) => {
  return Number(order?.status) === 6 || Number(order?.status) === 7;
};

const getOrderReturnReason = (order: any) => {
  const request = getOrderReturnRequest(order);

  const reason =
    order?.returnReason ??
    order?.returnRequestReason ??
    order?.refundReason ??
    order?.exchangeReason ??
    request?.reason ??
    request?.returnReason ??
    request?.returnRequestReason ??
    request?.refundReason ??
    null;

  const cleanReason = String(reason || "").trim();

  return cleanReason || "Chưa có lý do hoàn hàng";
};

const getOrderReturnDescription = (order: any) => {
  const request = getOrderReturnRequest(order);

  const description =
    order?.returnDescription ??
    order?.returnRequestDescription ??
    order?.refundDescription ??
    order?.returnNote ??
    order?.description ??
    request?.description ??
    request?.returnDescription ??
    request?.returnRequestDescription ??
    request?.refundDescription ??
    request?.note ??
    null;

  return String(description || "").trim();
};

type ReturnMediaView = {
  url: string;
  isVideo: boolean;
};

const getLocalBackendBaseUrl = () => {
  const isLocalhost =
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1";

  if (isLocalhost) {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }

  return window.location.origin;
};

const getApiAssetBaseUrl = () => {
  const configuredBaseUrl = String(api.defaults.baseURL || "").trim();

  /**
   * Khi axios baseURL là "/api", nếu ghép ảnh theo window.origin
   * thì ảnh sẽ thành localhost:5173/uploads/... và Vite không phục vụ file đó.
   * File hoàn hàng đang được BE lưu ở /uploads/return-requests nên ở môi trường dev
   * phải trỏ thẳng sang backend localhost:8080.
   */
  if (!configuredBaseUrl || configuredBaseUrl.startsWith("/")) {
    return getLocalBackendBaseUrl();
  }

  try {
    const parsedUrl = new URL(configuredBaseUrl, window.location.origin);

    parsedUrl.pathname = parsedUrl.pathname
      .replace(/\/api\/?$/, "")
      .replace(/\/$/, "");
    parsedUrl.search = "";
    parsedUrl.hash = "";

    return parsedUrl.toString().replace(/\/$/, "");
  } catch {
    return getLocalBackendBaseUrl();
  }
};

const normalizeReturnMediaUrl = (url: string) => {
  let cleanUrl = String(url || "").trim();

  if (!cleanUrl) {
    return "";
  }

  if (
    cleanUrl.startsWith("http://") ||
    cleanUrl.startsWith("https://") ||
    cleanUrl.startsWith("data:") ||
    cleanUrl.startsWith("blob:")
  ) {
    return cleanUrl;
  }

  cleanUrl = cleanUrl.replace(/^\/?api\//, "/");

  const assetBaseUrl = getApiAssetBaseUrl();

  if (cleanUrl.startsWith("/")) {
    return `${assetBaseUrl}${cleanUrl}`;
  }

  if (cleanUrl.startsWith("uploads/")) {
    return `${assetBaseUrl}/${cleanUrl}`;
  }

  return cleanUrl;
};

const getReturnMediaUrl = (media: any) => {
  const rawUrl =
    typeof media === "string"
      ? media
      : media?.mediaUrl ??
        media?.url ??
        media?.imageUrl ??
        media?.fileUrl ??
        media?.src ??
        media?.path ??
        "";

  return normalizeReturnMediaUrl(String(rawUrl || ""));
};

const isReturnMediaVideo = (media: any, url: string) => {
  const rawType = String(
    media?.mediaType ??
      media?.type ??
      media?.contentType ??
      media?.mimeType ??
      url
  ).toLowerCase();

  return (
    rawType.includes("video") ||
    rawType.includes("mp4") ||
    rawType.includes("mov") ||
    rawType.includes("webm") ||
    rawType === "2"
  );
};

const getOrderReturnMedia = (order: any): ReturnMediaView[] => {
  const request = getOrderReturnRequest(order);

  const rawMedia =
    order?.returnMediaUrls ??
    order?.returnMediaFiles ??
    order?.returnMedias ??
    order?.returnMedia ??
    order?.returnImages ??
    order?.returnEvidenceFiles ??
    order?.mediaFiles ??
    order?.files ??
    order?.images ??
    request?.returnMediaUrls ??
    request?.mediaFiles ??
    request?.returnMediaFiles ??
    request?.returnMedias ??
    request?.returnMedia ??
    request?.returnImages ??
    request?.evidenceFiles ??
    request?.files ??
    request?.images ??
    [];

  const mediaList = Array.isArray(rawMedia) ? rawMedia : [rawMedia];

  return mediaList
    .map((media: any) => {
      const url = getReturnMediaUrl(media);

      if (!url) {
        return null;
      }

      return {
        url,
        isVideo: isReturnMediaVideo(media, url),
      };
    })
    .filter((media): media is ReturnMediaView => media !== null);
};

const escapeHtml = (value: unknown) => {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
};

const buildReturnPreviewMainHtml = (media: ReturnMediaView) => {
  const safeUrl = escapeHtml(media.url);

  if (media.isVideo) {
    return `
      <video
        src="${safeUrl}"
        class="return-preview-video"
        controls
        autoplay
        playsinline
      ></video>
    `;
  }

  return `
    <img
      src="${safeUrl}"
      class="return-preview-image"
      alt="Ảnh bằng chứng hoàn hàng"
    />
  `;
};

const buildReturnPreviewThumbHtml = (
  media: ReturnMediaView,
  index: number,
  activeIndex: number
) => {
  const safeUrl = escapeHtml(media.url);
  const activeClass = index === activeIndex ? " active" : "";

  const thumb = media.isVideo
    ? `
      <video
        src="${safeUrl}"
        class="return-preview-thumb-video"
        muted
        playsinline
        preload="metadata"
      ></video>
    `
    : `
      <img
        src="${safeUrl}"
        class="return-preview-thumb-image"
        alt="Ảnh bằng chứng ${index + 1}"
      />
    `;

  return `
    <button
      type="button"
      class="return-preview-thumb-btn${activeClass}"
      data-preview-index="${index}"
      aria-label="Xem ${media.isVideo ? "video" : "ảnh"} bằng chứng ${index + 1}"
    >
      ${thumb}
      <span class="return-preview-thumb-badge">
        ${media.isVideo ? "Video" : "Ảnh"} ${index + 1}
      </span>
      ${media.isVideo ? `<span class="return-preview-thumb-play"><i class="bi bi-play-fill"></i></span>` : ""}
    </button>
  `;
};

const buildReturnPreviewHtml = (
  mediaList: ReturnMediaView[],
  activeIndex: number
) => {
  if (mediaList.length === 0) {
    return `
      <div class="return-preview-modal">
        <div class="return-preview-empty">
          Không có ảnh/video bằng chứng để hiển thị.
        </div>
      </div>
    `;
  }

  const safeActiveIndex = Math.min(
    Math.max(Number.isFinite(activeIndex) ? activeIndex : 0, 0),
    mediaList.length - 1
  );

  const activeMedia = mediaList[safeActiveIndex];

  if (!activeMedia) {
    return `
      <div class="return-preview-modal">
        <div class="return-preview-empty">
          Không có ảnh/video bằng chứng để hiển thị.
        </div>
      </div>
    `;
  }

  const hasMultipleMedia = mediaList.length > 1;

  return `
    <div class="return-preview-modal">
      <div class="return-preview-counter">
        ${activeMedia.isVideo ? "Video" : "Ảnh"} bằng chứng
        <strong>${safeActiveIndex + 1}/${mediaList.length}</strong>
      </div>

      <div class="return-preview-stage">
        ${
          hasMultipleMedia
            ? `
              <button
                type="button"
                class="return-preview-nav is-prev"
                data-preview-direction="-1"
                aria-label="Xem ảnh/video trước"
              >
                <i class="bi bi-chevron-left"></i>
              </button>
            `
            : ""
        }

        <div class="return-preview-main">
          ${buildReturnPreviewMainHtml(activeMedia)}
        </div>

        ${
          hasMultipleMedia
            ? `
              <button
                type="button"
                class="return-preview-nav is-next"
                data-preview-direction="1"
                aria-label="Xem ảnh/video tiếp theo"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
            `
            : ""
        }
      </div>

      ${
        hasMultipleMedia
          ? `
            <div class="return-preview-thumb-title">
              Chọn ảnh/video bằng chứng
            </div>
            <div class="return-preview-thumb-list">
              ${mediaList
                .map((media, index) =>
                  buildReturnPreviewThumbHtml(media, index, safeActiveIndex)
                )
                .join("")}
            </div>
          `
          : ""
      }
    </div>
  `;
};

const openReturnMediaPreview = async (order: any, index: number) => {
  const mediaList = getOrderReturnMedia(order);

  if (mediaList.length === 0) {
    return;
  }

  let activeIndex = Number.isInteger(index) ? index : 0;

  if (activeIndex < 0 || activeIndex >= mediaList.length) {
    activeIndex = 0;
  }

  const renderPreview = () => {
    const htmlContainer = Swal.getHtmlContainer();

    if (!htmlContainer) {
      return;
    }

    htmlContainer.innerHTML = buildReturnPreviewHtml(mediaList, activeIndex);
    bindPreviewEvents();
  };

  const goToPreview = (nextIndex: number) => {
    activeIndex = (nextIndex + mediaList.length) % mediaList.length;
    renderPreview();
  };

  const bindPreviewEvents = () => {
    const htmlContainer = Swal.getHtmlContainer();

    if (!htmlContainer) {
      return;
    }

    htmlContainer
      .querySelectorAll<HTMLButtonElement>("[data-preview-direction]")
      .forEach((button) => {
        button.addEventListener("click", () => {
          const direction = Number(button.dataset.previewDirection || 0);
          goToPreview(activeIndex + direction);
        });
      });

    htmlContainer
      .querySelectorAll<HTMLButtonElement>("[data-preview-index]")
      .forEach((button) => {
        button.addEventListener("click", () => {
          const selectedIndex = Number(button.dataset.previewIndex || 0);
          goToPreview(selectedIndex);
        });
      });
  };

  await Swal.fire({
    title: "Ảnh/video bằng chứng",
    html: buildReturnPreviewHtml(mediaList, activeIndex),
    width: 700,
    showConfirmButton: true,
    confirmButtonText: "Đóng",
    confirmButtonColor: "#bd9a5f",
    didOpen: bindPreviewEvents,
    customClass: {
      popup: "swal-custom-popup return-media-preview-popup",
      title: "swal-custom-title",
      confirmButton: "swal-preview-confirm",
    },
  });
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
  title: string
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
  const cancelReasons = [
    "Muốn thay đổi địa chỉ nhận hàng",
    "Muốn thay đổi số điện thoại nhận hàng",
    "Muốn thay đổi sản phẩm hoặc phân loại",
    "Muốn thay đổi số lượng sản phẩm",
    "Muốn thay đổi phương thức thanh toán",
    "Quên áp dụng mã giảm giá",
    "Đặt nhầm sản phẩm",
    "Không còn nhu cầu mua nữa",
    "Tìm thấy sản phẩm phù hợp hơn",
    "Khác",
  ];

  const { value: reason, isConfirmed } = await Swal.fire<string>({
    title: "Hủy đơn hàng?",
    html: `
      <div class="cancel-order-modal">
        <div class="cancel-order-desc">
          Vui lòng chọn lý do bạn muốn hủy đơn hàng này:
        </div>

        <div class="cancel-reason-grid">
          ${cancelReasons
            .map(
              (item, index) => `
                <label class="cancel-reason-card" for="cancel-reason-${index}">
                  <input
                    id="cancel-reason-${index}"
                    type="radio"
                    name="cancelReason"
                    value="${item}"
                  />
                  <span class="cancel-radio-dot"></span>
                  <span class="cancel-reason-text">${item}</span>
                </label>
              `,
            )
            .join("")}
        </div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận hủy",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    focusConfirm: false,
    width: 680,
    preConfirm: () => {
      const checkedReason = Swal.getPopup()?.querySelector<HTMLInputElement>(
        'input[name="cancelReason"]:checked',
      );

      if (!checkedReason?.value) {
        Swal.showValidationMessage("Vui lòng chọn một lý do để tiếp tục!");
        return false;
      }

      return checkedReason.value;
    },
    customClass: {
      popup: "swal-custom-popup cancel-order-swal",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-custom-confirm",
    },
  });

  if (isConfirmed) {
    try {
      store.orderLoading = true;
      await api.patch(`/customer/orders/${order.orderId}/cancel`, {
        cancelReason: reason,
      });
      await fetchOrdersAndReviews();
      toast("success", "Đã hủy đơn hàng thành công!");
    } catch (error) {
      showError(error, "Không thể hủy đơn hàng lúc này. Vui lòng thử lại.");
    } finally {
      store.orderLoading = false;
    }
  }
};


const getDefaultReturnEmail = () => {
  try {
    const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");

    return String(
      currentUser.email ||
        currentUser.Email ||
        localStorage.getItem("email") ||
        ""
    ).trim();
  } catch {
    return String(localStorage.getItem("email") || "").trim();
  }
};

const requestReturn = (order: CustomerOrderResponse) => {
  selectedReturnOrder.value = order;
  returnModalVisible.value = true;
};

const submitReturnRequest = async (payload: ReturnRequestSubmitPayload) => {
  try {
    submittingReturn.value = true;
    store.orderLoading = true;

    await customerProfileService.requestReturnOrder(payload.orderId, payload);

    returnModalVisible.value = false;
    selectedReturnOrder.value = null;

    await fetchOrdersAndReviews();
    currentTab.value = 6;

    toast("success", "Đã gửi yêu cầu hoàn hàng thành công!");
  } catch (error) {
    showError(error, "Không thể gửi yêu cầu hoàn hàng lúc này.");
  } finally {
    submittingReturn.value = false;
    store.orderLoading = false;
  }
};

const cancelReturnRequest = async (order: CustomerOrderResponse) => {
  const result = await Swal.fire({
    title: "Rút lại yêu cầu?",
    html: `
      <div class="return-cancel-modal">
        <div class="return-cancel-alert">
          <div class="return-cancel-icon">
            <i class="bi bi-arrow-counterclockwise"></i>
          </div>

          <div class="return-cancel-content">
            <div class="return-cancel-title">Xác nhận rút lại yêu cầu hoàn hàng</div>
            <div class="return-cancel-desc">
              Đơn hàng sẽ trở về trạng thái <strong>Hoàn thành</strong>.
              Sau khi rút lại, bạn cần gửi yêu cầu mới nếu muốn hoàn hàng/đổi trả tiếp.
            </div>
          </div>
        </div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Đồng ý rút lại",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    focusCancel: true,
    customClass: {
      popup: "swal-custom-popup return-cancel-swal",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-gold-confirm",
    },
  });

  if (result.isConfirmed) {
    try {
      store.orderLoading = true;
      await customerProfileService.cancelReturnRequest(order.orderId);
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
      "Không thể tạo lại phiên thanh toán lúc này. Vui lòng thử lại sau."
    );
  } finally {
    store.orderLoading = false;
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
  transition: border-color 0.25s ease, box-shadow 0.25s ease,
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
  transition: background 0.25s ease, color 0.25s ease;
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
  flex: 1;
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

.order-item-side {
  margin-left: auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  flex-shrink: 0;
}

.item-price-box {
  min-width: 140px;
  text-align: right;
  line-height: 1.25;
}

.item-original-price {
  color: #94a3b8;
  font-size: 13px;
  text-decoration: line-through;
  margin-bottom: 3px;
}

.item-final-price {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.item-line-total {
  margin-top: 5px;
  color: #9a6a1f;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.review-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
}

.order-summary-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 390px);
  align-items: flex-start;
  gap: 18px;
  margin-top: 18px;
}

.order-cancel-info,
.order-return-info {
  width: 100%;
  min-width: 0;
  min-height: 118px;
  background: #fffaf0;
  border: 1px solid #f3e2bd;
  border-left: 4px solid #bd9a5f;
  border-radius: 14px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.cancel-info-title,
.return-info-title {
  color: #9a6a1f;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.cancel-info-text,
.return-info-line strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.cancel-info-time {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.return-info-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
}

.return-info-line span {
  color: #64748b;
}

.return-info-description {
  margin-top: 8px;
  color: #334155;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.return-media-section {
  margin-top: 12px;
}

.return-media-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
}

.return-media-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(78px, 78px));
  gap: 10px;
}

.return-media-button {
  position: relative;
  width: 78px;
  height: 78px;
  padding: 0;
  border: 1px solid #f3e2bd;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.return-media-button:hover {
  transform: translateY(-1px);
  border-color: #bd9a5f;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
}

.return-media-thumb {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  background: #ffffff;
}

.return-media-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 22px;
  background: rgba(15, 23, 42, 0.2);
  opacity: 0;
  transition: opacity 0.18s ease;
}

.return-media-button:hover .return-media-overlay {
  opacity: 1;
}

.order-total-box {
  width: 100%;
  max-width: none;
  margin-left: 0;
  margin-top: 0;
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

  .order-item-side {
    width: 100%;
    margin-left: 0;
    align-items: flex-start;
    justify-content: space-between;
  }

  .order-summary-row {
    grid-template-columns: 1fr;
  }

  .order-cancel-info,
  .order-return-info,
  .order-total-box {
    width: 100%;
    max-width: 100%;
  }

  .item-price-box {
    text-align: left;
  }

  .review-action {
    justify-content: flex-end;
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
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
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

.cancel-order-swal {
  width: min(680px, calc(100vw - 28px)) !important;
}

.cancel-order-modal {
  padding: 0 24px;
}

.cancel-order-desc {
  color: #475569;
  font-size: 15px;
  line-height: 1.5;
  margin-bottom: 14px;
}

.cancel-reason-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.cancel-reason-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-height: 54px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.cancel-reason-card:hover {
  border-color: #bd9a5f;
  background: #fffaf0;
}

.cancel-reason-card input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.cancel-radio-dot {
  position: relative;
  width: 18px;
  height: 18px;
  margin-top: 1px;
  border: 2px solid #cbd5e1;
  border-radius: 50%;
  background: #ffffff;
  flex-shrink: 0;
}

.cancel-radio-dot::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dc2626;
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.18s ease;
}

.cancel-reason-card:has(input:checked) {
  border-color: #dc2626;
  background: #fff7ed;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.08);
}

.cancel-reason-card:has(input:checked) .cancel-radio-dot {
  border-color: #dc2626;
}

.cancel-reason-card:has(input:checked) .cancel-radio-dot::after {
  transform: translate(-50%, -50%) scale(1);
}

.cancel-reason-text {
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
}

.cancel-order-swal .swal2-validation-message {
  margin: 14px 24px 0 !important;
  border-radius: 10px !important;
  background: #fef2f2 !important;
  color: #b91c1c !important;
  font-size: 14px !important;
}

@media (max-width: 640px) {
  .cancel-order-modal {
    padding: 0 16px;
  }

  .cancel-reason-grid {
    grid-template-columns: 1fr;
  }

  .cancel-reason-card {
    min-height: auto;
  }

  .return-cancel-swal .swal2-html-container {
    margin: 0 16px !important;
  }

  .return-cancel-alert {
    padding: 14px;
  }
}

.return-cancel-swal {
  width: min(520px, calc(100vw - 28px)) !important;
}

.return-cancel-swal .swal2-html-container {
  margin: 0 22px !important;
}

.return-cancel-modal {
  padding: 0 6px;
}

.return-cancel-alert {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: #fffaf0;
  border: 1px solid #f3e2bd;
  border-left: 4px solid #bd9a5f;
  border-radius: 14px;
}

.return-cancel-icon {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: #06132b;
  color: #bd9a5f;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.return-cancel-content {
  min-width: 0;
}

.return-cancel-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
  margin-bottom: 6px;
}

.return-cancel-desc {
  color: #475569;
  font-size: 14px;
  line-height: 1.55;
}

.return-cancel-desc strong {
  color: #9a6a1f;
  font-weight: 800;
}

.swal-gold-confirm {
  background-color: #bd9a5f !important;
  color: #ffffff !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 12px 24px !important;
  font-weight: 700 !important;
  transition: all 0.2s ease !important;
}

.swal-gold-confirm:hover {
  background-color: #9a6a1f !important;
  box-shadow: 0 4px 12px rgba(189, 154, 95, 0.28) !important;
}

.return-preview-empty {
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
  padding: 36px 16px;
}

.return-media-preview-popup {
  width: min(700px, calc(100vw - 28px)) !important;
  max-width: calc(100vw - 28px) !important;
}

.return-media-preview-popup .swal2-html-container {
  margin: 0 18px !important;
}

.return-preview-modal {
  width: 100%;
}

.return-preview-counter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
}

.return-preview-counter strong {
  color: #9a6a1f;
  font-size: 14px;
}

.return-preview-stage {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px;
}

.return-preview-main {
  min-width: 0;
  min-height: 280px;
  max-height: 54vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.return-preview-image,
.return-preview-video {
  display: block;
  width: 100%;
  max-width: 100%;
  max-height: 54vh;
  object-fit: contain;
  border-radius: 10px;
  background: #ffffff;
}

.return-preview-nav {
  width: 38px;
  height: 38px;
  border: 1px solid #f3e2bd;
  border-radius: 999px;
  background: #fffaf0;
  color: #9a6a1f;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.return-preview-nav:hover {
  background: #bd9a5f;
  border-color: #bd9a5f;
  color: #ffffff;
  transform: translateY(-1px);
}

.return-preview-thumb-title {
  margin-top: 12px;
  margin-bottom: 7px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.return-preview-thumb-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 2px 2px 8px;
  scrollbar-width: thin;
}

.return-preview-thumb-btn {
  position: relative;
  width: 68px;
  height: 68px;
  flex: 0 0 68px;
  padding: 0;
  overflow: hidden;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.return-preview-thumb-btn:hover {
  border-color: #bd9a5f;
  transform: translateY(-1px);
}

.return-preview-thumb-btn.active {
  border-color: #bd9a5f;
  box-shadow: 0 0 0 3px rgba(189, 154, 95, 0.18);
}

.return-preview-thumb-image,
.return-preview-thumb-video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  background: #f8fafc;
}

.return-preview-thumb-badge {
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 4px;
  border-radius: 999px;
  padding: 2px 5px;
  background: rgba(15, 23, 42, 0.72);
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
  text-align: center;
  line-height: 1.2;
}

.return-preview-thumb-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 24px;
  text-shadow: 0 2px 8px rgba(15, 23, 42, 0.45);
  pointer-events: none;
}

.swal-preview-confirm {
  background-color: #bd9a5f !important;
  color: #ffffff !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 12px 24px !important;
  font-weight: 700 !important;
}

.swal-preview-confirm:hover {
  background-color: #9a6a1f !important;
}

@media (max-width: 640px) {
  .return-media-preview-popup .swal2-html-container {
    margin: 0 14px !important;
  }

  .return-preview-stage {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .return-preview-main {
    min-height: 240px;
  }

  .return-preview-nav {
    width: 100%;
    height: 38px;
    border-radius: 10px;
  }

  .return-preview-nav.is-prev {
    order: 2;
  }

  .return-preview-main {
    order: 1;
  }

  .return-preview-nav.is-next {
    order: 3;
  }
}

</style>

