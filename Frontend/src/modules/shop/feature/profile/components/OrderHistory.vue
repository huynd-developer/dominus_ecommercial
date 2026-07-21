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
                <!-- ĐÃ SỬA: Dùng hàm generateOrderCode để tạo mã đơn -->
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
                    <strong>{{ formatPaymentMethod(order.paymentMethod) }}</strong>
                  </div>

                  <div class="info-item">
                    <span>Loại đơn</span>
                    <strong>{{ formatOrderType(order.orderType) }}</strong>
                  </div>
                </div>

                <!-- BẮT ĐẦU KHỐI THEO DÕI ĐƠN HÀNG (FAKE TRACKING) -->
                <div class="tracking-container mt-2 mb-4">
                  <h6 class="fw-bold mb-3"><i class="bi bi-truck me-2"></i>Theo dõi kiện hàng</h6>
                  <div class="timeline">
                    <div 
                      v-for="(track, index) in getTrackingHistory(order)" 
                      :key="index"
                      class="timeline-item"
                      :class="{ 'is-active': track.active, 'is-cancel': track.isCancel }"
                    >
                      <div class="timeline-time">
                        <div class="t-date">{{ formatTrackingTime(track.time).date }}</div>
                        <div class="t-time">{{ formatTrackingTime(track.time).time }}</div>
                      </div>
                      
                      <div class="timeline-marker">
                        <div class="dot"></div>
                        <div v-if="index !== getTrackingHistory(order).length - 1" class="line"></div>
                      </div>
                      
                      <div class="timeline-content">
                        <div class="t-title">{{ track.title }}</div>
                        <div class="t-desc">{{ track.desc }}</div>
                        <img v-if="track.img" :src="track.img" class="tracking-img mt-2" alt="Bằng chứng giao hàng">
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
                        v-if="item.image"
                        :src="item.image"
                        class="item-img"
                        alt="item"
                      />
                      <!-- ĐÃ SỬA: Việt hóa chữ "No" thành "Không có ảnh" -->
                      <div v-else class="item-img placeholder-img" style="text-align: center; line-height: 1.2;">
                        Không<br>có ảnh
                      </div>

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
                            <strong>{{ formatDateOnly(getManufacturingDate(item)) }}</strong>
                          </span>

                          <span>
                            HSD:
                            <strong>{{ formatDateOnly(getExpirationDate(item)) }}</strong>
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
                                getMyReviewByOrderItemId(item.orderItemId)?.rating
                              }}/5
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
                            getReviewState(order.orderId, item.orderItemId)?.message
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
                        @click.stop="openReview(order.orderId, item.orderItemId)"
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

                <!-- KHỐI CÁC NÚT THAO TÁC -->
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
                    style="background-color: #bd9a5f; border-color: #bd9a5f;"
                    @click="handleReorder(order)"
                  >
                    <i class="bi bi-cart-plus me-1"></i> Mua lại
                  </button>

                  <span
                    v-if="!order.canCancel && order.status !== 3 && order.status !== 4"
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
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

// Biến lưu trạng thái Tab hiện tại
const currentTab = ref<number | 'ALL'>('ALL');

const reviewLoading = ref(false);
const submittingReview = ref(false);
const reviewModalVisible = ref(false);
const selectedReviewItem = ref<ReviewableOrderItemResponse | null>(null);
const myReviews = ref<ReviewResponse[]>([]);
const openedOrderId = ref<number | null>(null);

const reviewableMap = reactive<Record<number, ReviewableOrderItemResponse[]>>({});
const reviewLoadingByOrder = reactive<Record<number, boolean>>({});

// ĐÃ THÊM: Hàm "phù phép" ID thành Mã đơn hàng (VD: DH-000015)
const generateOrderCode = (id: number | string | null | undefined) => {
  if (!id) return "N/A";
  return `DH-${String(id).padStart(6, '0')}`;
};

// Lọc đơn hàng theo Tab
const filteredOrders = computed(() => {
  if (currentTab.value === 'ALL') {
    return store.orders;
  }
  return store.orders.filter((order: CustomerOrderResponse) => order.status === currentTab.value);
});

const completedOrders = computed(() => {
  return store.orders.filter((order: CustomerOrderResponse) => order.status === 3);
});

onMounted(() => {
  fetchOrdersAndReviews();
});

const toggleOrder = (orderId: number) => {
  openedOrderId.value = openedOrderId.value === orderId ? null : orderId;
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
    await loadCompletedReviewStates();

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

const submitReview = async (payload: {
  rating: number;
  comment: string | null;
  files: File[]; // Nhận thêm mảng file từ Modal
}) => {
  if (!selectedReviewItem.value) {
    return;
  }

  const orderId = selectedReviewItem.value.orderId;
  const orderItemId = selectedReviewItem.value.orderItemId;

  try {
    submittingReview.value = true;

    // 1. Tạo FormData để chứa dữ liệu
    const formData = new FormData();
    formData.append('orderItemId', String(orderItemId));
    formData.append('rating', String(payload.rating));
    
    if (payload.comment) {
      formData.append('comment', payload.comment);
    }

    // 2. Nhét từng file ảnh/video vào FormData
    if (payload.files && payload.files.length > 0) {
      payload.files.forEach((file) => {
        formData.append('mediaFiles', file); // 'mediaFiles' là tên key, nhớ dặn Backend hứng đúng tên này nhé
      });
    }

    // 3. Gọi API (nhớ báo Backend đổi API nhận form-data thay vì application/json)
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

const cancelOrder = async (order: CustomerOrderResponse) => {
  const { value: reason, isConfirmed } = await Swal.fire({
    title: "Lý do hủy đơn hàng?",
    text: "Vui lòng cho chúng tôi biết lý do bạn muốn hủy đơn này:",
    input: "radio",
    inputOptions: {
      "Thay đổi thông tin": "Tôi muốn cập nhật địa chỉ / sđt nhận hàng",
      "Thay đổi sản phẩm": "Tôi muốn thêm/bớt sản phẩm hoặc đổi phân loại",
      "Thay đổi phương thức thanh toán": "Tôi muốn thay đổi phương thức thanh toán",
      "Đổi ý": "Tôi không còn nhu cầu mua nữa",
      "Giao hàng lâu": "Thời gian giao hàng dự kiến quá lâu",
      "Khác": "Lý do khác",
    },
    inputValidator: (value) => {
      if (!value) {
        return "Vui lòng chọn một lý do để tiếp tục!";
      }
    },
    showCancelButton: true,
    confirmButtonColor: "#dc2626", 
    cancelButtonColor: "#f8fafc", // Đổi nút Hủy thành màu nền xám nhạt cho sang
    confirmButtonText: "Xác nhận hủy",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    // THÊM ĐOẠN CUSTOM CLASS NÀY VÀO:
    customClass: {
      popup: 'swal-custom-popup',
      title: 'swal-custom-title',
      cancelButton: 'swal-custom-cancel',
      confirmButton: 'swal-custom-confirm'
    }
  });

  if (isConfirmed) {
    try {
      store.orderLoading = true;
      await store.cancelOrder(order);
      await fetchOrdersAndReviews();
      toast("success", "Đã hủy đơn hàng thành công!");
    } catch (error) {
      showError(error, "Không thể hủy đơn hàng lúc này. Vui lòng thử lại.");
    } finally {
      store.orderLoading = false;
    }
  }
};

const handleReorder = async (order: any) => {
  const result = await Swal.fire({
    title: "Mua lại đơn hàng?",
    text: "Sản phẩm sẽ được thêm vào giỏ và chuyển đến trang thanh toán ngay.",
    icon: "question",
    showCancelButton: true,
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#6c757d",
    confirmButtonText: "Mua lại ngay",
    cancelButtonText: "Hủy",
    reverseButtons: true,
  });

  if (result.isConfirmed) {
    try {
      store.orderLoading = true;

      const addPromises = order.items.map((item: any) => {
        const variantId = item.productVariantId || item.variantId || item.productId;
        
        return api.post("/v1/customer/cart/add", {
          productVariantId: Number(variantId),
          quantity: Number(item.quantity || 1)
        });
      });

      await Promise.all(addPromises);

      toast("success", "Đang chuyển đến trang thanh toán...");
      
      router.push("/checkout"); 
      
    } catch (error) {
      showError(error, "Không thể thêm sản phẩm vào giỏ hàng lúc này. Vui lòng thử lại.");
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
    title: 'Đơn hàng đã đặt',
    desc: 'Đơn hàng đang chờ shop xác nhận.',
    active: order.status === 0
  });

  if (order.status >= 1 && order.status !== 4) {
    history.push({
      time: new Date(baseDate + 2 * 60 * 60 * 1000), 
      title: 'Đang chuẩn bị hàng',
      desc: 'Người bán đang chuẩn bị kiện hàng của bạn.',
      active: order.status === 1
    });
  }

  if (order.status >= 2 && order.status !== 4) {
    history.push({
      time: new Date(baseDate + 14 * 60 * 60 * 1000), 
      title: 'Đã giao cho ĐVVC',
      desc: 'Kiện hàng đã rời trung tâm phân loại và đang trên đường giao.',
      active: order.status === 2
    });
  }

  if (order.status === 3) {
    history.push({
      time: order.completedAt ? new Date(order.completedAt) : new Date(baseDate + 48 * 60 * 60 * 1000),
      title: 'Đã giao',
      desc: `Kiện hàng của bạn đã được giao. Người nhận: ${order.customerName || 'Bạn'}`,
      img: 'https://images.unsplash.com/photo-1615460549969-36fa19521a4f?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80', 
      active: true
    });
  }

  if (order.status === 4) {
    history.push({
      time: new Date(baseDate + 30 * 60 * 1000), 
      title: 'Đã hủy',
      desc: 'Đơn hàng đã được hủy bỏ.',
      active: true,
      isCancel: true
    });
  }

  return history.reverse();
};

const formatTrackingTime = (dateStr: string | number | Date) => {
  const date = new Date(dateStr);
  const day = date.getDate().toString().padStart(2, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
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
</script>

<style scoped>
/* CSS CHO PHẦN THANH TAB TRẠNG THÁI */
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

/* CSS KHỐI THEO DÕI GIAO HÀNG (TIMELINE) */
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
/* KẾT THÚC CSS TIMELINE */

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

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 11px;
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

.sku-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
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

code {
  color: #111827;
  background: #f3f4f6;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
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
  /* 
  CSS FIX GIAO DIỆN SWEETALERT2 (KHÔNG SCOPED)
  Do SweetAlert2 render thẻ HTML ở ngoài cùng của body nên phải để un-scoped 
*/
.swal-custom-popup {
  border-radius: 16px !important;
  padding: 2em 1.5em !important;
  font-family: inherit !important;
}

.swal-custom-title {
  font-size: 22px !important;
  color: #06132b !important; /* Xanh đen đồng bộ web */
}

/* Ép các tùy chọn Radio xếp dọc, căn trái */
.swal2-radio {
  display: flex !important;
  flex-direction: column !important;
  align-items: flex-start !important;
  gap: 12px !important;
  text-align: left !important;
  margin-top: 1.5em !important;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.swal2-radio label {
  display: flex !important;
  align-items: center !important;
  font-size: 15px !important;
  color: #475569 !important;
  width: 100% !important;
  cursor: pointer;
  margin: 0 !important;
}

/* Đổi màu dấu chấm radio thành màu vàng đồng của web */
.swal2-radio input[type="radio"] {
  margin-right: 12px !important;
  accent-color: #bd9a5f !important; 
  width: 18px !important;
  height: 18px !important;
  cursor: pointer;
}

.swal-custom-cancel {
  color: #64748b !important;
  border: 1px solid #e2e8f0 !important;
  font-weight: 600 !important;
  box-shadow: none !important;
}

.swal-custom-confirm {
  font-weight: 600 !important;
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.2) !important;
}
}

</style>