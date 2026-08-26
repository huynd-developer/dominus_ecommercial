<template>
  <div class="card border-0 shadow-sm">
    <div
      class="card-header bg-white border-0 py-3 d-flex justify-content-between align-items-center"
    >
      <div>
        <h5 class="mb-0 fw-bold">Lịch sử đơn hàng</h5>
        <div class="small text-muted mt-1">
          Đánh giá/hoàn hàng chỉ mở trong thời hạn sau khi đơn hoàn thành
        </div>
      </div>

      <button
        class="btn btn-outline-dark btn-sm"
        :disabled="store.orderLoading || reviewLoading"
        @click="fetchOrdersAndReviews(true)"
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
          :class="{ active: currentTab === 5 }"
          @click="currentTab = 5"
        >
          Giao thất bại
        </div>

        <div
          class="tab-item"
          :class="{ active: currentTab === 'RETURN' }"
          @click="currentTab = 'RETURN'"
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
                <span :class="['badge', getOrderHeaderStatusClass(order)]">
                  {{ getOrderHeaderStatusText(order) }}
                </span>

                <div class="fw-bold mt-1 order-header-total">
                  {{ formatMoney(getOrderFinalAmount(order)) }}
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

                        <div
                          v-if="
                            track.title === 'Đã giao' &&
                            Number(order.status) === 3 &&
                            getDeliverySuccessMedia(order).length > 0
                          "
                          class="tracking-delivery-media"
                        >
                          <div class="return-media-list">
                            <button
                              v-for="(
                                media, mediaIndex
                              ) in getDeliverySuccessMedia(order)"
                              :key="`delivery-success-${media.url}-${mediaIndex}`"
                              type="button"
                              class="return-media-button"
                              @click.stop="
                                openDeliverySuccessMediaPreview(
                                  order,
                                  mediaIndex
                                )
                              "
                            >
                              <img
                                :src="media.url"
                                class="return-media-thumb"
                                alt="Minh chứng giao hàng thành công"
                                @error="handleImageError"
                              />

                              <span class="return-media-overlay">
                                <i class="bi bi-zoom-in"></i>
                              </span>
                            </button>
                          </div>
                        </div>

                        <div
                          v-if="
                            track.title === 'Giao hàng thất bại' &&
                            Number(order.status) === 5 &&
                            (order.deliveryFailedDescription ||
                              getDeliveryFailedMedia(order).length > 0)
                          "
                          class="tracking-delivery-media is-failed"
                        >
                          <div
                            v-if="order.deliveryFailedDescription"
                            class="tracking-delivery-note"
                          >
                            <span>Mô tả:</span>
                            <strong>{{
                              order.deliveryFailedDescription
                            }}</strong>
                          </div>

                          <div
                            v-if="getDeliveryFailedMedia(order).length > 0"
                            class="tracking-delivery-proof"
                          >
                            <div
                              class="tracking-delivery-media-label text-danger"
                            >
                              <i class="bi bi-x-circle me-1"></i>
                              Ảnh minh chứng giao thất bại:
                            </div>

                            <div class="return-media-list">
                              <button
                                v-for="(
                                  media, mediaIndex
                                ) in getDeliveryFailedMedia(order)"
                                :key="`delivery-failed-${media.url}-${mediaIndex}`"
                                type="button"
                                class="return-media-button"
                                @click.stop="
                                  openDeliveryFailedMediaPreview(
                                    order,
                                    mediaIndex
                                  )
                                "
                              >
                                <img
                                  :src="media.url"
                                  class="return-media-thumb"
                                  alt="Minh chứng giao hàng thất bại"
                                  @error="handleImageError"
                                />

                                <span class="return-media-overlay">
                                  <i class="bi bi-zoom-in"></i>
                                </span>
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- KẾT THÚC KHỐI THEO DÕI -->

                <div
                  v-if="isDeliveryRefundInfoVisible(order)"
                  class="order-delivery-refund-info mb-3"
                  :class="getDeliveryRefundBoxClass(order)"
                >
                  <div class="delivery-refund-top">
                    <div>
                      <div class="delivery-refund-title">
                        <i class="bi bi-cash-coin me-1"></i>
                        {{
                          order.status === 8
                            ? "Thông tin tài khoản hoàn tiền"
                            : "Hoàn tiền giao hàng thất bại"
                        }}
                      </div>
                      <div class="delivery-refund-desc">
                        {{ getDeliveryRefundDescription(order) }}
                      </div>
                    </div>

                    <span class="delivery-refund-badge">
                      {{ getDeliveryRefundStatusText(order) }}
                    </span>
                  </div>

                  <div class="delivery-refund-grid">
                    <div class="delivery-refund-line delivery-refund-money">
                      <span>Số tiền cần hoàn:</span>
                      <strong>{{
                        formatMoney(getDeliveryRefundAmount(order))
                      }}</strong>
                    </div>

                    <template v-if="hasDeliveryRefundBankInfo(order)">
                      <div class="delivery-refund-line">
                        <span>Ngân hàng:</span>
                        <strong>{{
                          order.deliveryRefundBankName || "-"
                        }}</strong>
                      </div>

                      <div class="delivery-refund-line">
                        <span>Số tài khoản:</span>
                        <strong>{{
                          order.deliveryRefundBankAccountNumber || "-"
                        }}</strong>
                      </div>

                      <div class="delivery-refund-line">
                        <span>Chủ tài khoản:</span>
                        <strong>{{
                          order.deliveryRefundBankAccountHolder || "-"
                        }}</strong>
                      </div>
                    </template>

                    <div
                      v-if="order.deliveryRefundedAt"
                      class="delivery-refund-line"
                    >
                      <span>Hoàn lúc:</span>
                      <strong>{{
                        formatDate(order.deliveryRefundedAt)
                      }}</strong>
                    </div>

                    <div
                      v-if="order.deliveryRefundedByName"
                      class="delivery-refund-line"
                    >
                      <span>Người xác nhận:</span>
                      <strong>{{ order.deliveryRefundedByName }}</strong>
                    </div>
                  </div>

                  <div
                    v-if="canSubmitDeliveryRefundBank(order)"
                    class="delivery-refund-actions"
                  >
                    <button
                      type="button"
                      class="btn btn-sm btn-outline-primary"
                      :disabled="store.orderLoading"
                      @click.stop="openDeliveryRefundBankModal(order)"
                    >
                      <i class="bi bi-bank me-1"></i>
                      Nhập thông tin hoàn tiền
                    </button>
                  </div>

                  <div
                    v-else-if="
                      hasAnyDeliveryRefundBankInfo(order) &&
                      !isDeliveryRefundCompleted(order)
                    "
                    class="delivery-refund-once-note"
                  >
                    <i class="bi bi-info-circle me-1"></i>
                    Thông tin tài khoản hoàn tiền đã được gửi một lần. Nếu thông
                    tin chưa chính xác, vui lòng liên hệ shop trước khi shop
                    chuyển tiền.
                  </div>
                </div>

                <div class="order-items">
                  <div
                    v-for="item in order.items"
                    :key="item.orderItemId"
                    class="order-item"
                  >
                    <div
                      class="product-block"
                      role="button"
                      tabindex="0"
                      title="Xem chi tiết sản phẩm"
                      @click.stop="goToProductDetail(item)"
                      @keydown.enter.stop="goToProductDetail(item)"
                      @keydown.space.prevent.stop="goToProductDetail(item)"
                    >
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

                          <div
                            v-if="
                              getReviewApprovalText(
                                getMyReviewByOrderItemId(item.orderItemId)
                              )
                            "
                            class="review-approval-status mt-1"
                            :class="
                              getReviewApprovalClass(
                                getMyReviewByOrderItemId(item.orderItemId)
                              )
                            "
                          >
                            {{
                              getReviewApprovalText(
                                getMyReviewByOrderItemId(item.orderItemId)
                              )
                            }}
                          </div>

                          <div
                            v-if="
                              getReviewRejectReason(
                                getMyReviewByOrderItemId(item.orderItemId)
                              )
                            "
                            class="review-reject-reason mt-1"
                          >
                            <i class="bi bi-info-circle me-1"></i>
                            <span>Lý do:</span>
                            <strong>
                              {{
                                getReviewRejectReason(
                                  getMyReviewByOrderItemId(item.orderItemId)
                                )
                              }}
                            </strong>
                          </div>

                          <button
                            v-if="
                              canEditExistingReview(
                                getMyReviewByOrderItemId(item.orderItemId)
                              )
                            "
                            type="button"
                            class="btn btn-sm btn-outline-dark review-edit-btn mt-2"
                            :disabled="submittingReview"
                            @click.stop="openEditReview(order, item)"
                          >
                            <i class="bi bi-pencil-square me-1"></i>
                            Sửa đánh giá
                          </button>

                          <div
                            v-if="
                              getReviewMediaByOrderItemId(item.orderItemId)
                                .length > 0
                            "
                            class="review-media-section"
                          >
                            <div class="review-media-label">
                              Ảnh/video đánh giá:
                            </div>

                            <div class="review-media-list">
                              <button
                                v-for="(
                                  media, mediaIndex
                                ) in getReviewMediaByOrderItemId(
                                  item.orderItemId
                                )"
                                :key="`${media.url}-${mediaIndex}`"
                                type="button"
                                class="review-media-button"
                                :title="
                                  media.isVideo
                                    ? 'Xem video đánh giá'
                                    : 'Xem ảnh đánh giá'
                                "
                                @click.stop="
                                  openReviewMediaPreview(
                                    item.orderItemId,
                                    mediaIndex
                                  )
                                "
                              >
                                <video
                                  v-if="media.isVideo"
                                  :src="media.url"
                                  class="review-media-thumb"
                                  muted
                                  playsinline
                                  preload="metadata"
                                ></video>

                                <img
                                  v-else
                                  :src="media.url"
                                  class="review-media-thumb"
                                  alt="Ảnh đánh giá sản phẩm"
                                  @error="handleImageError"
                                />

                                <span class="review-media-overlay">
                                  <i
                                    class="bi"
                                    :class="
                                      media.isVideo
                                        ? 'bi-play-circle'
                                        : 'bi-zoom-in'
                                    "
                                  ></i>
                                </span>
                              </button>
                            </div>
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
                  <div
                    v-if="order.status === 4 || order.status === 8"
                    class="order-cancel-info"
                  >
                    <div class="cancel-info-title">
                      <i class="bi bi-x-circle me-1"></i>
                      Lý do hủy:
                    </div>

                    <div class="cancel-info-text">
                      {{ getOrderCancelReason(order) }}
                    </div>

                    <div
                      v-if="isPrepaidOrder(order)"
                      class="order-delivery-refund-info w-100"
                      :class="getCancelRefundBoxClass(order)"
                    >
                      <div class="delivery-refund-top">
                        <div>
                          <div class="delivery-refund-title">
                            <i class="bi bi-wallet2 me-1"></i>
                            Hoàn tiền đơn hủy
                          </div>
                          <div class="delivery-refund-desc">
                            {{ getCancelRefundDescription(order) }}
                          </div>
                        </div>
                        <span class="delivery-refund-badge">
                          {{ getCancelRefundStatusText(order) }}
                        </span>
                      </div>

                      <div class="delivery-refund-grid">
                        <div class="delivery-refund-line delivery-refund-money">
                          <span>Số tiền cần hoàn:</span>
                          <strong>{{
                            formatMoney(getOrderFinalAmount(order))
                          }}</strong>
                        </div>

                        <template v-if="hasCancelRefundBankInfo(order)">
                          <div class="delivery-refund-line">
                            <span>Ngân hàng:</span>
                            <strong>{{
                              order.deliveryRefundBankName || "-"
                            }}</strong>
                          </div>

                          <div class="delivery-refund-line">
                            <span>Số tài khoản:</span>
                            <strong>{{
                              order.deliveryRefundBankAccountNumber || "-"
                            }}</strong>
                          </div>

                          <div class="delivery-refund-line">
                            <span>Chủ tài khoản:</span>
                            <strong>{{
                              order.deliveryRefundBankAccountHolder || "-"
                            }}</strong>
                          </div>
                        </template>

                        <div
                          v-if="isCancelRefunded(order)"
                          class="delivery-refund-line"
                        >
                          <span>Hoàn lúc:</span>
                          <strong>{{
                            formatDate(order.deliveryRefundedAt) || "Đã xử lý"
                          }}</strong>
                        </div>
                      </div>

                      <div
                        v-if="canSubmitCancelRefundBank(order)"
                        class="delivery-refund-actions"
                      >
                        <button
                          type="button"
                          class="btn btn-sm btn-outline-primary"
                          :disabled="store.orderLoading"
                          @click.stop="openCancelRefundBankModal(order)"
                        >
                          <i class="bi bi-bank me-1"></i>
                          Nhập thông tin hoàn tiền
                        </button>
                      </div>

                      <div
                        v-else-if="
                          hasCancelRefundBankInfo(order) &&
                          !isCancelRefunded(order)
                        "
                        class="delivery-refund-once-note"
                      >
                        <i class="bi bi-info-circle me-1"></i>
                        Thông tin tài khoản hoàn tiền đã được gửi một lần. Nếu
                        thông tin chưa chính xác, vui lòng liên hệ shop trước
                        khi shop chuyển tiền.
                      </div>
                    </div>
                  </div>

                  <div
                    v-else-if="isReturnInfoVisible(order)"
                    class="order-return-info"
                    :class="getReturnProcessClass(order)"
                  >
                    <div class="return-info-top">
                      <div>
                        <div class="return-info-title">
                          <i class="bi bi-arrow-counterclockwise me-1"></i>
                          Thông tin hoàn hàng / hoàn tiền
                        </div>

                        <div class="return-request-meta">
                          <span>
                            ID yêu cầu:
                            <strong>{{ getReturnRequestCode(order) }}</strong>
                          </span>

                          <span v-if="getOrderReturnRequestedAt(order)">
                            Yêu cầu vào:
                            <strong>{{
                              formatDate(getOrderReturnRequestedAt(order))
                            }}</strong>
                          </span>
                        </div>
                      </div>

                      <span
                        class="return-process-badge"
                        :class="getReturnProcessBadgeClass(order)"
                      >
                        {{ getReturnProcessText(order) }}
                      </span>
                    </div>

                    <div class="return-process-timeline">
                      <div
                        v-for="step in getReturnProcessTimeline(order)"
                        :key="step.key"
                        class="return-process-step"
                        :class="{
                          'is-done': step.done,
                          'is-active': step.active,
                          'is-rejected': step.rejected,
                        }"
                      >
                        <div class="return-step-marker">
                          <i class="bi" :class="step.icon"></i>
                        </div>

                        <div class="return-step-content">
                          <div class="return-step-title">{{ step.title }}</div>
                          <div v-if="step.desc" class="return-step-desc">
                            {{ step.desc }}
                          </div>
                          <div v-if="step.time" class="return-step-time">
                            {{ formatDate(step.time) }}
                          </div>
                        </div>
                      </div>
                    </div>

                    <div
                      v-if="getReturnProcessAlert(order)"
                      class="return-process-alert"
                      :class="getReturnProcessAlert(order)?.className"
                    >
                      <i
                        class="bi"
                        :class="getReturnProcessAlert(order)?.icon"
                      ></i>
                      <div>
                        <strong>{{
                          getReturnProcessAlert(order)?.title
                        }}</strong>
                        <p>{{ getReturnProcessAlert(order)?.desc }}</p>
                      </div>
                    </div>

                    <div class="return-info-grid">
                      <div class="return-info-line">
                        <span>Lý do hoàn:</span>
                        <strong>{{ getOrderReturnReason(order) }}</strong>
                      </div>

                      <div
                        v-if="getOrderReturnDescription(order)"
                        class="return-info-description"
                      >
                        {{ getOrderReturnDescription(order) }}
                      </div>

                      <div
                        v-if="getOrderReturnRejectReason(order)"
                        class="return-info-line return-reject-line"
                      >
                        <span>Lý do từ chối:</span>
                        <strong>{{ getOrderReturnRejectReason(order) }}</strong>
                      </div>

                      <div
                        v-if="getOrderReturnRefundAmount(order) > 0"
                        class="return-info-line return-refund-line"
                      >
                        <span>{{ getReturnRefundLabel(order) }}:</span>
                        <strong>{{
                          formatMoney(getOrderReturnRefundAmount(order))
                        }}</strong>
                      </div>

                      <div
                        v-if="getOrderRefundMethodText(order)"
                        class="return-info-line"
                      >
                        <span>Hoàn tiền vào:</span>
                        <strong>{{ getOrderRefundMethodText(order) }}</strong>
                      </div>
                    </div>

                    <div class="return-selected-section">
                      <div class="return-media-label">
                        Sản phẩm yêu cầu hoàn:
                      </div>

                      <div
                        v-if="getOrderReturnSelectedItems(order).length > 0"
                        class="return-selected-list"
                      >
                        <button
                          v-for="returnItem in getOrderReturnSelectedItems(
                            order
                          )"
                          :key="`return-item-${
                            returnItem.orderItemId ||
                            returnItem.productVariantId ||
                            returnItem.productId
                          }`"
                          type="button"
                          class="return-selected-item"
                          @click.stop="goToProductDetail(returnItem)"
                        >
                          <img
                            :src="returnItem.image || FALLBACK_IMAGE"
                            class="return-selected-img"
                            :alt="
                              returnItem.productName || 'Sản phẩm hoàn hàng'
                            "
                            @error="handleImageError"
                          />

                          <div class="return-selected-content">
                            <div class="return-selected-name-row">
                              <div class="return-selected-name">
                                {{ returnItem.productName || "Sản phẩm" }}
                              </div>

                              <span
                                class="return-selected-status"
                                :class="getReturnItemStatusClass(returnItem)"
                              >
                                {{ getReturnItemStatusText(returnItem) }}
                              </span>
                            </div>

                            <div class="return-selected-meta">
                              <span v-if="returnItem.brandName">
                                {{ returnItem.brandName }}
                              </span>
                              <span>
                                {{ getCapacityText(returnItem) }}
                              </span>
                              <span>
                                {{ getBottleTypeText(returnItem) }}
                              </span>
                            </div>

                            <div class="return-selected-bottom">
                              <span>
                                SL mua:
                                <strong>{{
                                  returnItem.orderedQuantity || 0
                                }}</strong>
                              </span>

                              <span>
                                SL hoàn:
                                <strong>{{
                                  returnItem.returnQuantity || 0
                                }}</strong>
                              </span>

                              <span
                                v-if="Number(returnItem.itemAmount || 0) > 0"
                              >
                                Tiền hàng:
                                <strong>{{
                                  formatMoney(returnItem.itemAmount)
                                }}</strong>
                              </span>

                              <span
                                v-if="
                                  Number(
                                    returnItem.voucherAllocatedAmount || 0
                                  ) > 0
                                "
                                class="return-selected-discount"
                              >
                                Voucher phân bổ:
                                <strong>
                                  -{{
                                    formatMoney(
                                      returnItem.voucherAllocatedAmount
                                    )
                                  }}
                                </strong>
                              </span>

                              <span
                                v-if="Number(returnItem.refundAmount || 0) > 0"
                                class="return-selected-refund"
                              >
                                Hoàn:
                                <strong>{{
                                  formatMoney(returnItem.refundAmount)
                                }}</strong>
                              </span>
                            </div>

                            <div
                              v-if="getReturnItemRejectReason(returnItem)"
                              class="return-item-reject-note"
                            >
                              Lý do từ chối:
                              {{ getReturnItemRejectReason(returnItem) }}
                            </div>
                          </div>
                        </button>
                      </div>

                      <div v-else class="return-selected-empty">
                        Chưa có dữ liệu sản phẩm yêu cầu hoàn
                      </div>
                    </div>

                    <div
                      v-if="getOrderReturnMedia(order).length > 0"
                      class="return-media-section"
                    >
                      <div class="return-media-label">
                        Ảnh/video bằng chứng:
                      </div>

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
                              :class="
                                media.isVideo ? 'bi-play-circle' : 'bi-zoom-in'
                              "
                            ></i>
                          </span>
                        </button>
                      </div>
                    </div>
                  </div>

                  <div class="order-total-box">
                    <div>
                      <span>Tạm tính:</span>
                      <strong>{{
                        formatMoney(getOrderSubtotal(order))
                      }}</strong>
                    </div>

                    <div v-if="getOrderDiscountAmount(order) > 0">
                      <span>Giảm giá:</span>
                      <strong class="text-danger">
                        -{{ formatMoney(getOrderDiscountAmount(order)) }}
                      </strong>
                    </div>

                    <div v-if="getOrderShippingFee(order) > 0">
                      <span>Phí vận chuyển:</span>
                      <strong>{{
                        formatMoney(getOrderShippingFee(order))
                      }}</strong>
                    </div>

                    <div
                      class="d-flex justify-content-between fs-5 mt-2 pt-2 border-top"
                    >
                      <span>Tổng thanh toán:</span>
                      <strong class="text-danger">
                        {{ formatMoney(getOrderFinalAmount(order)) }}
                      </strong>
                    </div>
                  </div>
                </div>

                <!-- KHỐI CÁC NÚT THAO TÁC -->
                <div class="text-end mt-3 d-flex justify-content-end gap-2">
                  <template
                    v-if="
                      order.status === 0 &&
                      (order.paymentMethod || '')
                        .toUpperCase()
                        .includes('VNPAY')
                    "
                  >
                    <div
                      v-if="isOrderPendingVerification(order)"
                      class="d-flex align-items-center me-auto text-success fw-bold"
                      style="font-size: 14px"
                    >
                      <i class="bi bi-check-circle-fill me-1"></i> Đang chờ shop
                      đối soát...
                    </div>
                    <template v-else>
                      <div
                        class="d-flex align-items-center me-3 text-warning fw-bold"
                        style="font-size: 14px"
                      >
                        <i class="bi bi-clock-history me-1"></i> Đang chờ xác
                        nhận thanh toán
                      </div>
                    </template>
                  </template>

                  <template
                    v-if="
                      order.status === 0 &&
                      (order.paymentMethod || '')
                        .toUpperCase()
                        .includes('VIETQR')
                    "
                  >
                    <div
                      v-if="isOrderPendingVerification(order)"
                      class="d-flex align-items-center me-auto text-success fw-bold"
                      style="font-size: 14px"
                    >
                      <i class="bi bi-check-circle-fill me-1"></i> Đang chờ shop
                      đối soát...
                    </div>
                    <template v-else>
                      <div
                        class="d-flex align-items-center me-3 text-warning fw-bold"
                        style="font-size: 14px"
                      >
                        <i class="bi bi-clock-history me-1"></i> Đang chờ khách
                        chuyển khoản
                      </div>
                    </template>
                  </template>

                  <button
                    v-if="order.canCancel && isOnlineOrder(order)"
                    class="btn btn-outline-danger btn-sm"
                    :disabled="store.orderLoading"
                    @click="cancelOrder(order)"
                  >
                    Hủy đơn
                  </button>

                  <button
                    v-if="
                      order.status === 4 ||
                      order.status === 8 ||
                      order.status === 3
                    "
                    class="btn btn-primary btn-sm px-3 text-white"
                    style="background-color: #bd9a5f; border-color: #bd9a5f"
                    @click="handleReorder(order)"
                  >
                    <i class="bi bi-cart-plus me-1"></i> Mua lại
                  </button>

                  <button
                    v-if="canRequestReturn(order)"
                    type="button"
                    class="btn btn-outline-danger btn-sm"
                    :disabled="store.orderLoading"
                    @click="requestReturn(order)"
                  >
                    Yêu cầu hoàn hàng
                  </button>

                  <span
                    v-else-if="shouldShowReturnDeadlineText(order)"
                    class="text-muted small align-self-center"
                  >
                    {{ getReturnDeadlineText(order) }}
                  </span>

                  <!-- NÚT HỦY YÊU CẦU HOÀN HÀNG -->
                  <button
                    v-if="canCancelReturnRequest(order)"
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
                      order.status !== 4 &&
                      order.status !== 8 &&
                      order.status !== 0
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
      :mode="reviewModalMode"
      :existing-review="selectedEditingReview"
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
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
const route = useRoute();
const router = useRouter();

const currentTab = ref<number | "ALL" | "RETURN">("ALL");

const RETURN_REQUEST_DEADLINE_DAYS = 3;
const REVIEW_EDIT_DEADLINE_DAYS = 30;

const reviewLoading = ref(false);
const submittingReview = ref(false);
const reviewModalVisible = ref(false);
const reviewModalMode = ref<"create" | "edit">("create");
const selectedReviewItem = ref<ReviewableOrderItemResponse | null>(null);
const selectedEditingReview = ref<ReviewResponse | null>(null);
const myReviews = ref<ReviewResponse[]>([]);
const openedOrderId = ref<number | null>(null);

const returnModalVisible = ref(false);
const selectedReturnOrder = ref<CustomerOrderResponse | null>(null);
const submittingReturn = ref(false);
const refreshingOrdersOnFocus = ref(false);

const reviewableMap = reactive<Record<number, ReviewableOrderItemResponse[]>>(
  {}
);
const reviewLoadingByOrder = reactive<Record<number, boolean>>({});

const paidOrdersMap = ref<Record<string, boolean>>({});

const initPaidOrders = () => {
  try {
    const saved = localStorage.getItem("dominus_paid_orders");
    if (saved) {
      paidOrdersMap.value = JSON.parse(saved);
    }
  } catch (e) {}
};

// 💥 BƯỚC 1: LÀM LẠI BỘ HÀM TÍNH TOÁN BẤT CHẤP API TRẢ SAO
const toMoneyNumber = (value: unknown) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const pickMoneyValue = (...values: unknown[]) => {
  for (const value of values) {
    const numberValue = toMoneyNumber(value);
    if (numberValue > 0) return numberValue;
  }
  return 0;
};

const getOrderSubtotal = (order: any) => {
  const explicitSubtotal = pickMoneyValue(
    order?.totalAmount,
    order?.TotalAmount,
    order?.subTotal,
    order?.SubTotal,
    order?.subTotalAmount,
    order?.SubTotalAmount,
    order?.amount,
    order?.Amount
  );
  if (explicitSubtotal > 0) return explicitSubtotal;

  if (Array.isArray(order?.items)) {
    return order.items.reduce(
      (sum: number, item: any) => sum + getItemLineTotal(item),
      0
    );
  }
  return 0;
};

const getOrderShippingFee = (order: any) => {
  return pickMoneyValue(
    order?.shippingFee,
    order?.ShippingFee,
    order?.shippingfee,
    order?.shippingFeeAmount,
    order?.ShippingFeeAmount,
    order?.shipFee,
    order?.ShipFee,
    order?.deliveryFee,
    order?.DeliveryFee,
    order?.shippingAmount,
    order?.ShippingAmount
  );
};

// 💥 ĐÃ SỬA: LẤY TRỰC TIẾP GIẢM GIÁ TỪ BACKEND ĐỂ CHÍNH XÁC NHẤT THAY VÌ TỰ TÍNH LẠI
const getOrderDiscountAmount = (order: any) => {
  return pickMoneyValue(
    order?.discountAmount,
    order?.DiscountAmount,
    order?.discountValue,
    order?.DiscountValue,
    order?.discount,
    order?.Discount,
    order?.voucherDiscount,
    order?.VoucherDiscount,
    order?.voucherAmount,
    order?.VoucherAmount,
    order?.voucherDiscountAmount,
    order?.VoucherDiscountAmount,
    order?.promotionAmount,
    order?.PromotionAmount,
    order?.promotionDiscount,
    order?.PromotionDiscount
  );
};

const getOrderFinalAmount = (order: any) => {
  const explicitFinal = pickMoneyValue(
    order?.finalAmount,
    order?.FinalAmount,
    order?.totalPayment,
    order?.TotalPayment,
    order?.totalPay,
    order?.TotalPay,
    order?.paymentTotal,
    order?.PaymentTotal,
    order?.totalPrice,
    order?.TotalPrice
  );
  if (explicitFinal > 0) return explicitFinal;

  const subtotal = getOrderSubtotal(order);
  const discount = getOrderDiscountAmount(order);
  const ship = getOrderShippingFee(order);

  return Math.max(0, subtotal - discount) + ship;
};
// 💥 KẾT THÚC BƯỚC 1

const isPrepaidOrder = (order: any) => {
  if (!order || !order.paymentMethod) return false;
  const pm = String(order.paymentMethod).toUpperCase();
  return (
    pm.includes("VNPAY") ||
    pm.includes("VIETQR") ||
    pm.includes("MOMO") ||
    pm.includes("BANK") ||
    pm.includes("TRANSFER") ||
    pm.includes("MIXED")
  );
};

const hasCancelRefundBankInfo = (order: any) => {
  return hasDeliveryRefundBankInfo(order);
};

const isCancelRefunded = (order: any) => {
  return isDeliveryRefundCompleted(order);
};

const canSubmitCancelRefundBank = (order: any) => {
  return (
    Number(order?.status) === 8 &&
    isPrepaidOrder(order) &&
    getDeliveryRefundAmount(order) > 0 &&
    canSubmitDeliveryRefundBank(order)
  );
};

const getCancelRefundStatusText = (order: any) => {
  if (isCancelRefunded(order)) return "Đã hoàn tiền";
  if (hasCancelRefundBankInfo(order)) return "Chờ shop hoàn tiền";
  return "Chờ nhập STK";
};

const getCancelRefundDescription = (order: any) => {
  const amount = formatMoney(getOrderFinalAmount(order));
  if (isCancelRefunded(order)) return `Shop đã hoàn ${amount} cho đơn bị hủy.`;
  if (hasCancelRefundBankInfo(order))
    return `Shop đã nhận thông tin tài khoản và sẽ hoàn ${amount} cho bạn.`;
  return `Đơn đã thanh toán trước nhưng bị hủy. Vui lòng nhập số tài khoản ngân hàng để shop hoàn ${amount}.`;
};

const getCancelRefundBoxClass = (order: any) => ({
  "is-waiting-bank": canSubmitCancelRefundBank(order),
  "is-waiting-shop": hasCancelRefundBankInfo(order) && !isCancelRefunded(order),
  "is-refunded": isCancelRefunded(order),
});

const openCancelRefundBankModal = async (order: CustomerOrderResponse) => {
  if (!canSubmitCancelRefundBank(order)) return;

  try {
    await fetchDeliveryRefundBanksFromVietQr();
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không tải được ngân hàng",
      text:
        error?.message ||
        "Không lấy được danh sách ngân hàng từ VietQR. Vui lòng thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const currentBank = findDeliveryRefundBank(
    order.deliveryRefundBankName || ""
  );
  const currentBankName = currentBank?.displayName || "";

  const result = await Swal.fire<{
    bankName: string;
    bankAccountNumber: string;
    bankAccountHolder: string;
  }>({
    title: "Nhập thông tin hoàn tiền đơn hủy",
    html: `
      <div class="delivery-refund-modal">
        <div class="delivery-refund-modal-alert">
          <i class="bi bi-info-circle"></i>
          <span>Shop sẽ hoàn <strong>${escapeHtml(
            formatMoney(getOrderFinalAmount(order))
          )}</strong> cho đơn hủy ${escapeHtml(
      generateOrderCode(order.orderId)
    )}. Thông tin này chỉ gửi được 1 lần và không thể tự chỉnh sửa.</span>
        </div>

        <label for="cancel-refund-bank-search" class="delivery-refund-modal-label">
          Ngân hàng <span>*</span>
        </label>
        <div class="delivery-refund-bank-picker">
          <div class="delivery-refund-bank-search-wrap">
            <i class="bi bi-search"></i>
            <input
              id="cancel-refund-bank-search"
              class="delivery-refund-bank-search"
              autocomplete="off"
              placeholder="Tìm theo tên ngân hàng, mã ngân hàng hoặc BIN"
              value="${escapeHtml(currentBankName)}"
            />
          </div>
          <input
            id="cancel-refund-bank-name"
            type="hidden"
            value="${escapeHtml(currentBankName)}"
          />
          <div id="cancel-refund-bank-list" class="delivery-refund-bank-list"></div>
        </div>
        <div class="delivery-refund-modal-help">
          Danh sách ngân hàng và logo được lấy trực tiếp từ VietQR.
        </div>

        <label for="cancel-refund-account-number" class="delivery-refund-modal-label">
          Số tài khoản <span>*</span>
        </label>
        <input
          id="cancel-refund-account-number"
          class="swal2-input delivery-refund-modal-control"
          inputmode="numeric"
          maxlength="50"
          placeholder="Ví dụ: 0123456789"
          value="${escapeHtml(order.deliveryRefundBankAccountNumber || "")}"
        />

        <label for="cancel-refund-account-holder" class="delivery-refund-modal-label">
          Tên chủ tài khoản <span>*</span>
        </label>
        <input
          id="cancel-refund-account-holder"
          class="swal2-input delivery-refund-modal-control"
          maxlength="100"
          placeholder="Ví dụ: NGUYEN VAN NAM"
          value="${escapeHtml(order.deliveryRefundBankAccountHolder || "")}"
        />
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Gửi thông tin",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    focusConfirm: false,
    customClass: {
      popup: "swal-custom-popup delivery-refund-swal",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-gold-confirm",
    },
    didOpen: () => {
      const bankSearchInput = document.getElementById(
        "cancel-refund-bank-search"
      ) as HTMLInputElement | null;
      const bankHiddenInput = document.getElementById(
        "cancel-refund-bank-name"
      ) as HTMLInputElement | null;
      const accountInput = document.getElementById(
        "cancel-refund-account-number"
      ) as HTMLInputElement | null;
      const accountHolderInput = document.getElementById(
        "cancel-refund-account-holder"
      ) as HTMLInputElement | null;

      setCancelRefundBankListHtml("", currentBankName);

      bankSearchInput?.addEventListener("input", () => {
        const keyword = normalizeDeliveryRefundInput(bankSearchInput.value);
        const matchedBank = findDeliveryRefundBank(keyword);

        if (bankHiddenInput) {
          bankHiddenInput.value =
            matchedBank &&
            normalizeBankSearchText(matchedBank.displayName) ===
              normalizeBankSearchText(keyword)
              ? matchedBank.displayName
              : "";
        }

        setCancelRefundBankListHtml(keyword, bankHiddenInput?.value || "");
      });

      bankSearchInput?.addEventListener("focus", () => {
        setCancelRefundBankListHtml(
          normalizeDeliveryRefundInput(bankSearchInput.value),
          bankHiddenInput?.value || ""
        );
      });

      accountInput?.addEventListener("input", () => {
        accountInput.value = accountInput.value
          .replace(/[^0-9\s]/g, "")
          .replace(/\s{2,}/g, " ");
      });

      accountHolderInput?.addEventListener("input", () => {
        accountHolderInput.value = accountHolderInput.value
          .replace(/[^\p{L}\s'.-]/gu, "")
          .replace(/\s{2,}/g, " ")
          .toUpperCase();
      });
    },
    preConfirm: () => {
      const bankElement = document.getElementById(
        "cancel-refund-bank-name"
      ) as HTMLInputElement | null;
      const accountNumberElement = document.getElementById(
        "cancel-refund-account-number"
      ) as HTMLInputElement | null;
      const accountHolderElement = document.getElementById(
        "cancel-refund-account-holder"
      ) as HTMLInputElement | null;

      const bankName = normalizeDeliveryRefundInput(bankElement?.value);
      const bankAccountNumber = normalizeDeliveryRefundAccountNumber(
        accountNumberElement?.value
      );
      const bankAccountHolder = normalizeDeliveryRefundInput(
        accountHolderElement?.value
      ).toUpperCase();

      const validationMessage = validateDeliveryRefundBankForm(
        bankName,
        bankAccountNumber,
        bankAccountHolder
      );

      if (validationMessage) {
        Swal.showValidationMessage(validationMessage);
        return false;
      }

      return { bankName, bankAccountNumber, bankAccountHolder };
    },
  });

  if (!result.isConfirmed || !result.value) return;

  const confirmResult = await Swal.fire({
    icon: "warning",
    title: "Xác nhận thông tin hoàn tiền?",
    html: `
      <div style="text-align:left;line-height:1.6">
        <p style="margin-bottom:8px">Thông tin tài khoản hoàn tiền <b>chỉ gửi được 1 lần</b>. Sau khi gửi, bạn không thể tự chỉnh sửa trên hệ thống. Vui lòng kiểm tra thật kĩ</p>
        <p style="margin-bottom:4px"><b>Ngân hàng:</b> ${escapeHtml(
          result.value.bankName
        )}</p>
        <p style="margin-bottom:4px"><b>Số tài khoản:</b> ${escapeHtml(
          result.value.bankAccountNumber
        )}</p>
        <p style="margin-bottom:0"><b>Chủ tài khoản:</b> ${escapeHtml(
          result.value.bankAccountHolder
        )}</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Chắc chắn gửi",
    cancelButtonText: "Kiểm tra lại",
    reverseButtons: true,
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-gold-confirm",
    },
  });

  if (!confirmResult.isConfirmed) return;

  try {
    store.orderLoading = true;

    await customerProfileService.submitDeliveryRefundBank(
      order.orderId,
      result.value
    );

    await fetchOrdersAndReviews();
    openedOrderId.value = order.orderId;
    toast("success", "Đã gửi thông tin tài khoản hoàn tiền.");
  } catch (error) {
    if (
      !(await handleCustomerOrderConflict(
        error,
        order.orderId,
        "Thông tin hoàn tiền hoặc trạng thái đơn hàng đã thay đổi. Dữ liệu mới đã được tải lại, vui lòng kiểm tra lại."
      ))
    ) {
      showError(error, "Không thể gửi thông tin hoàn tiền lúc này.");
    }
  } finally {
    store.orderLoading = false;
  }
};

const setCancelRefundBankListHtml = (
  keyword = "",
  selectedBank?: string | null
) => {
  const bankListElement = document.getElementById("cancel-refund-bank-list");
  if (!bankListElement) return;

  bankListElement.innerHTML = buildDeliveryRefundBankOptionsHtml(
    keyword,
    selectedBank
  );

  bankListElement
    .querySelectorAll<HTMLButtonElement>(".delivery-refund-bank-option")
    .forEach((button) => {
      button.addEventListener("click", () => {
        const bankName = normalizeDeliveryRefundInput(button.dataset.bankValue);
        const hiddenInput = document.getElementById(
          "cancel-refund-bank-name"
        ) as HTMLInputElement | null;
        const searchInput = document.getElementById(
          "cancel-refund-bank-search"
        ) as HTMLInputElement | null;

        if (hiddenInput) hiddenInput.value = bankName;
        if (searchInput) searchInput.value = bankName;

        setCancelRefundBankListHtml(bankName, bankName);
      });
    });
};

const isOrderPendingVerification = (order: any) => {
  if (!order) return false;
  const isPaidLocally = paidOrdersMap.value[String(order.orderId)] === true;
  const rawStatus = (order as any).isPaymentReported;
  const isPaidApi =
    rawStatus === true ||
    rawStatus === 1 ||
    rawStatus === "1" ||
    String(rawStatus).toLowerCase() === "true";
  return isPaidLocally || isPaidApi;
};

const generateOrderCode = (id: number | string | null | undefined) => {
  if (!id) return "N/A";
  return `DH-${String(id).padStart(6, "0")}`;
};

const getOrderSortTime = (value: unknown) => {
  if (!value) {
    return 0;
  }

  const time = new Date(String(value)).getTime();

  return Number.isFinite(time) ? time : 0;
};

const getOrderLatestActionTime = (order: any) => {
  const status = Number(order?.status);

  if (status === 6 || status === 7) {
    return Math.max(
      getOrderSortTime(order?.returnRequestedAt),
      getOrderSortTime(order?.returnRequest?.createdAt),
      getOrderSortTime(order?.latestReturnRequest?.createdAt),
      getOrderSortTime(order?.returnInfo?.createdAt),
      getOrderSortTime(order?.updatedAt),
      getOrderSortTime(order?.createdAt)
    );
  }

  if (status === 4) {
    return Math.max(
      getOrderSortTime(order?.cancelledAt),
      getOrderSortTime(order?.canceledAt),
      getOrderSortTime(order?.cancelAt),
      getOrderSortTime(order?.updatedAt),
      getOrderSortTime(order?.createdAt)
    );
  }

  if (status === 3) {
    return Math.max(
      getOrderSortTime(order?.completedAt),
      getOrderSortTime(order?.updatedAt),
      getOrderSortTime(order?.createdAt)
    );
  }

  return Math.max(
    getOrderSortTime(order?.updatedAt),
    getOrderSortTime(order?.createdAt)
  );
};

const sortOrdersByLatestAction = (orders: CustomerOrderResponse[]) => {
  return [...orders].sort((firstOrder: any, secondOrder: any) => {
    const secondOrderTime = getOrderLatestActionTime(secondOrder);
    const firstOrderTime = getOrderLatestActionTime(firstOrder);

    if (secondOrderTime !== firstOrderTime) {
      return secondOrderTime - firstOrderTime;
    }

    return Number(secondOrder?.orderId || 0) - Number(firstOrder?.orderId || 0);
  });
};

const parseTime = (value: unknown) => {
  if (!value) {
    return 0;
  }

  const time = new Date(String(value)).getTime();

  return Number.isFinite(time) ? time : 0;
};

const addDaysToTime = (time: number, days: number) => {
  return time + days * 24 * 60 * 60 * 1000;
};

const getDaysLeftFromDeadline = (deadlineTime: number) => {
  if (!Number.isFinite(deadlineTime) || deadlineTime <= 0) {
    return 0;
  }

  const diff = deadlineTime - Date.now();

  if (diff <= 0) {
    return 0;
  }

  return Math.ceil(diff / (24 * 60 * 60 * 1000));
};

const getOrderCompletedBaseTime = (order: any) => {
  return (
    parseTime(order?.completedAt) ||
    parseTime(order?.completedDate) ||
    parseTime(order?.deliveredAt) ||
    parseTime(order?.updatedAt) ||
    parseTime(order?.createdAt)
  );
};

const isCompletedOrder = (order: any) => {
  return Number(order?.status) === 3;
};

const isOnlineOrder = (order: any) => {
  return (
    String(order?.orderType ?? "")
      .trim()
      .toUpperCase() === "ONLINE"
  );
};

const getReturnDeadlineTime = (order: any) => {
  const completedTime = getOrderCompletedBaseTime(order);

  if (!completedTime) {
    return 0;
  }

  return addDaysToTime(completedTime, RETURN_REQUEST_DEADLINE_DAYS);
};

const canRequestReturn = (order: any) => {
  if (!isOnlineOrder(order) || !isCompletedOrder(order)) {
    return false;
  }

  const deadlineTime = getReturnDeadlineTime(order);

  if (!(deadlineTime > 0 && Date.now() <= deadlineTime)) {
    return false;
  }

  if (!hasOrderReturnData(order)) {
    return true;
  }

  return getOrderReturnProcessStatus(order) === "CUSTOMER_CANCELLED";
};

const getReturnDeadlineText = (order: any) => {
  if (!isCompletedOrder(order)) {
    return "";
  }

  const deadlineTime = getReturnDeadlineTime(order);

  if (!deadlineTime) {
    return "Không xác định được hạn hoàn hàng";
  }

  const daysLeft = getDaysLeftFromDeadline(deadlineTime);

  if (daysLeft <= 0) {
    return "Đã quá hạn 3 ngày kể từ lúc đơn hàng hoàn thành, không thể yêu cầu hoàn hàng";
  }

  return `Còn ${daysLeft} ngày để yêu cầu hoàn hàng`;
};

const isReturnTabOrder = (order: any) => {
  const status = Number(order?.status);

  if (status === 6 || status === 7) {
    return true;
  }

  return status === 3 && getOrderReturnProcessStatus(order) === "REJECTED";
};

const filteredOrders = computed(() => {
  let orders: CustomerOrderResponse[];

  if (currentTab.value === "ALL") {
    orders = store.orders as CustomerOrderResponse[];
  } else if (currentTab.value === "RETURN") {
    orders = (store.orders as CustomerOrderResponse[]).filter((order) =>
      isReturnTabOrder(order)
    );
  } else {
    orders = (store.orders as CustomerOrderResponse[]).filter(
      (order) => Number(order.status) === currentTab.value
    );
  }

  return sortOrdersByLatestAction(orders);
});

const completedOrders = computed(() => {
  return store.orders.filter(
    (order: CustomerOrderResponse) => order.status === 3
  );
});

const handleWindowFocus = async () => {
  if (
    refreshingOrdersOnFocus.value ||
    store.orderLoading ||
    submittingReturn.value
  ) {
    return;
  }

  refreshingOrdersOnFocus.value = true;
  try {
    await fetchOrdersAndReviews(false);
  } finally {
    refreshingOrdersOnFocus.value = false;
  }
};

onMounted(() => {
  window.addEventListener("focus", handleWindowFocus);
  initPaidOrders();
  fetchOrdersAndReviews();
});

watch(
  () => route.query.tab,
  (currentTabQuery, previousTabQuery) => {
    const currentProfileTab = Array.isArray(currentTabQuery)
      ? currentTabQuery[0]
      : currentTabQuery;
    const previousProfileTab = Array.isArray(previousTabQuery)
      ? previousTabQuery[0]
      : previousTabQuery;

    if (
      currentProfileTab === "orders" &&
      previousProfileTab !== "orders" &&
      !store.orderLoading &&
      !submittingReturn.value
    ) {
      void fetchOrdersAndReviews(false);
    }
  }
);

onBeforeUnmount(() => {
  window.removeEventListener("focus", handleWindowFocus);
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

const isOrderOpen = (orderId: number) => openedOrderId.value === orderId;

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

const fetchOrdersAndReviews = async (showSuccessToast = true) => {
  try {
    await store.fetchOrders();
    await fetchMyReviews();

    const reviewPromises: Promise<any>[] = [];
    store.orders.forEach((order) => {
      if (order.status === 3) {
        reviewPromises.push(loadReviewableItems(order.orderId, false));
      }
    });
    if (reviewPromises.length > 0) {
      await Promise.all(reviewPromises);
    }

    if (
      openedOrderId.value &&
      !store.orders.some((order) => order.orderId === openedOrderId.value)
    ) {
      openedOrderId.value = null;
    }

    if (showSuccessToast) {
      toast("success", "Đã làm mới lịch sử đơn hàng");
    }
  } catch (error) {
    showError(error, "Không tải được lịch sử đơn hàng");
  }
};

const handleCustomerOrderConflict = async (
  error: any,
  orderId: number | null | undefined,
  fallback: string
) => {
  if (error?.response?.status !== 409) {
    return false;
  }

  await fetchOrdersAndReviews(false);

  if (orderId && store.orders.some((item) => item.orderId === orderId)) {
    openedOrderId.value = orderId;
  }

  await Swal.fire({
    icon: "warning",
    title: "Đơn hàng đã thay đổi",
    text: getErrorMessage(
      error,
      fallback ||
        "Trạng thái đơn hàng đã được cập nhật ở nơi khác. Dữ liệu mới đã được tải lại, vui lòng kiểm tra và thao tác lại."
    ),
    confirmButtonColor: "#bd9a5f",
    confirmButtonText: "Đã hiểu",
  });

  return true;
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
    if (showToast) toast("success", "Đã cập nhật trạng thái đánh giá");
  } catch (error) {
    showError(error, "Không tải được trạng thái đánh giá");
  } finally {
    reviewLoadingByOrder[orderId] = false;
  }
};

const getMyReviewByOrderItemId = (orderItemId: number) => {
  return myReviews.value.find((review) => review.orderItemId === orderItemId);
};

const getReviewApprovalStatus = (review: any) => {
  const rawStatus =
    review?.approvalStatus ?? review?.status ?? review?.reviewStatus ?? null;

  if (rawStatus === null || rawStatus === undefined || rawStatus === "") {
    return null;
  }

  const status = String(rawStatus).trim().toUpperCase();

  if (status === "0" || status === "PENDING" || status === "PENDING_APPROVAL") {
    return "PENDING_APPROVAL";
  }

  if (status === "1" || status === "APPROVED") {
    return "APPROVED";
  }

  if (status === "2" || status === "REJECTED") {
    return "REJECTED";
  }

  if (status === "3" || status === "HIDDEN") {
    return "HIDDEN";
  }

  return null;
};

const getReviewApprovalText = (review: any) => {
  const approvalStatusText = String(review?.approvalStatusText || "").trim();

  if (approvalStatusText) {
    return approvalStatusText;
  }

  const status = getReviewApprovalStatus(review);

  if (status === "PENDING_APPROVAL") {
    return "Đang chờ duyệt ảnh/video";
  }

  if (status === "APPROVED") {
    return "Đã hiển thị";
  }

  if (status === "REJECTED") {
    return "Đánh giá không được duyệt";
  }

  if (status === "HIDDEN") {
    return "Đánh giá đã bị ẩn";
  }

  return "";
};

const getReviewApprovalClass = (review: any) => {
  const status = getReviewApprovalStatus(review);

  return {
    "is-pending": status === "PENDING_APPROVAL",
    "is-approved": status === "APPROVED",
    "is-rejected": status === "REJECTED",
    "is-hidden": status === "HIDDEN",
  };
};

const getReviewRejectReason = (review: any) => {
  if (!review || getReviewApprovalStatus(review) !== "REJECTED") {
    return "";
  }

  const reason =
    review?.rejectedReason ??
    review?.rejectReason ??
    review?.rejectionReason ??
    review?.approvalRejectReason ??
    review?.reviewRejectReason ??
    review?.reasonReject ??
    null;

  return String(reason || "").trim();
};

const getReviewEditCount = (review: any) => {
  const editCount = Number(review?.editCount ?? review?.editedCount ?? 0);

  return Number.isFinite(editCount) && editCount > 0 ? editCount : 0;
};

const getReviewEditDeadlineTime = (review: any) => {
  const createdTime = parseTime(review?.createdAt);

  if (!createdTime) {
    return 0;
  }

  return addDaysToTime(createdTime, REVIEW_EDIT_DEADLINE_DAYS);
};

const canEditExistingReview = (review: any) => {
  if (!review) {
    return false;
  }

  if (review?.canEdit !== null && review?.canEdit !== undefined) {
    return Boolean(review.canEdit);
  }

  if (getReviewEditCount(review) >= 1 || Boolean(review?.editedAt)) {
    return false;
  }

  const deadlineTime = getReviewEditDeadlineTime(review);

  return deadlineTime > 0 && Date.now() <= deadlineTime;
};

const getReviewEditHint = (review: any) => {
  if (!review) {
    return "";
  }

  if (review?.canEdit === false) {
    return review?.editMessage || "Đánh giá này không còn được chỉnh sửa";
  }

  if (getReviewEditCount(review) >= 1 || Boolean(review?.editedAt)) {
    return "Đã sử dụng quyền sửa đánh giá";
  }

  const deadlineTime = getReviewEditDeadlineTime(review);

  if (!deadlineTime) {
    return "Không xác định được hạn sửa đánh giá";
  }

  const daysLeft = getDaysLeftFromDeadline(deadlineTime);

  if (daysLeft <= 0) {
    return "Đã quá hạn 30 ngày sửa đánh giá";
  }

  return `Còn ${daysLeft} ngày để sửa đánh giá`;
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

const buildReviewItemFromOrderItem = (
  order: any,
  item: any
): ReviewableOrderItemResponse => {
  return {
    orderItemId: Number(item?.orderItemId || item?.id || 0),
    orderId: Number(order?.orderId || order?.id || 0),

    productVariantId: item?.productVariantId ?? item?.variantId ?? null,
    productId: item?.productId ?? null,
    productName: item?.productName ?? item?.name ?? "Sản phẩm không xác định",
    brandName: item?.brandName ?? item?.brand ?? null,
    sku: item?.sku ?? null,
    image: item?.image ?? item?.imageUrl ?? null,

    orderStatus: Number(order?.status || 0),
    reviewed: true,
    canReview: false,
    message: "Bạn đã đánh giá sản phẩm này",
  };
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

  reviewModalMode.value = "create";
  selectedEditingReview.value = null;
  selectedReviewItem.value = state;
  reviewModalVisible.value = true;
};

const openEditReview = async (order: CustomerOrderResponse, item: any) => {
  const review = getMyReviewByOrderItemId(
    Number(item?.orderItemId || item?.id || 0)
  );

  if (!review) {
    await Swal.fire({
      icon: "error",
      title: "Không tìm thấy đánh giá",
      text: "Không tìm thấy đánh giá cần chỉnh sửa.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (!canEditExistingReview(review)) {
    await Swal.fire({
      icon: "info",
      title: "Không thể sửa đánh giá",
      text:
        getReviewEditHint(review) || "Đánh giá này không còn được chỉnh sửa.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  reviewModalMode.value = "edit";
  selectedEditingReview.value = review;
  selectedReviewItem.value =
    getReviewState(order.orderId, Number(item?.orderItemId || item?.id || 0)) ||
    buildReviewItemFromOrderItem(order, item);
  reviewModalVisible.value = true;
};

const submitReview = async (payload: {
  rating: number;
  comment: string | null;
  files: File[];
  deletedMediaIds?: number[];
}) => {
  if (!selectedReviewItem.value) {
    return;
  }

  const isEditMode =
    reviewModalMode.value === "edit" && selectedEditingReview.value;
  const orderId = selectedReviewItem.value.orderId;
  const orderItemId = selectedReviewItem.value.orderItemId;
  const hasReviewMedia =
    payload.files?.some((file) => file && file.size > 0) === true;

  try {
    submittingReview.value = true;
    const formData = new FormData();

    if (!isEditMode) {
      formData.append("orderItemId", String(orderItemId));
    }

    formData.append("rating", String(payload.rating));
    if (payload.comment) formData.append("comment", payload.comment);
    if (payload.files && payload.files.length > 0) {
      payload.files.forEach((file) => formData.append("mediaFiles", file));
    }

    if (
      isEditMode &&
      payload.deletedMediaIds &&
      payload.deletedMediaIds.length > 0
    ) {
      payload.deletedMediaIds.forEach((mediaId) => {
        formData.append("deletedMediaIds", String(mediaId));
      });
    }

    if (isEditMode) {
      await customerProfileService.updateReview(
        selectedEditingReview.value!.reviewId,
        formData as any
      );
    } else {
      await customerProfileService.createReview(formData as any);
    }

    reviewModalVisible.value = false;
    selectedReviewItem.value = null;
    selectedEditingReview.value = null;
    reviewModalMode.value = "create";

    toast(
      "success",
      isEditMode
        ? hasReviewMedia
          ? "Cập nhật đánh giá thành công. Ảnh/video đang chờ duyệt."
          : "Cập nhật đánh giá thành công"
        : hasReviewMedia
        ? "Gửi đánh giá thành công. Ảnh/video đang chờ duyệt."
        : "Gửi đánh giá thành công"
    );

    await fetchMyReviews();
    await loadReviewableItems(orderId, false);
  } catch (error) {
    showError(
      error,
      isEditMode ? "Không cập nhật được đánh giá" : "Không gửi được đánh giá"
    );
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

const getProductIdFromItem = (item: any) => {
  const rawId =
    item?.productId ??
    item?.product?.id ??
    item?.product?.productId ??
    item?.productVariant?.productId ??
    item?.productVariant?.product?.id ??
    item?.productVariant?.product?.productId ??
    null;
  const productId = Number(rawId);
  return Number.isFinite(productId) && productId > 0 ? productId : null;
};

const getProductVariantIdFromItem = (item: any) => {
  const rawId =
    item?.productVariantId ??
    item?.variantId ??
    item?.productVariant?.id ??
    item?.productVariant?.variantId ??
    null;
  const variantId = Number(rawId);
  return Number.isFinite(variantId) && variantId > 0 ? variantId : null;
};

const getProductDetailRoute = () => {
  return router.getRoutes().find((route) => {
    const routeName = String(route.name || "").toLowerCase();
    const routePath = String(route.path || "").toLowerCase();
    return (
      (routeName.includes("product") && routeName.includes("detail")) ||
      routePath.includes("/product/:") ||
      routePath.includes("/products/:") ||
      routePath.includes("/san-pham/:")
    );
  });
};

const buildProductDetailPathFromRoute = (routePath: string, item: any) => {
  const productId = getProductIdFromItem(item);
  const variantId = getProductVariantIdFromItem(item);
  if (!productId) return "";
  return routePath
    .replace(/:productId\??/g, String(productId))
    .replace(/:id\??/g, String(productId))
    .replace(/:variantId\??/g, String(variantId || productId));
};

const goToProductDetail = async (item: any) => {
  const productId = getProductIdFromItem(item);
  if (!productId) {
    toast("warning", "Không tìm thấy sản phẩm để xem chi tiết");
    return;
  }
  const variantId = getProductVariantIdFromItem(item);
  const productDetailRoute = getProductDetailRoute();
  const query = variantId ? { variantId: String(variantId) } : undefined;
  try {
    if (productDetailRoute?.name) {
      const params: Record<string, string> = {};
      const paramNames = Array.from(
        String(productDetailRoute.path || "").matchAll(/:([A-Za-z0-9_]+)/g)
      )
        .map((match) => match[1])
        .filter((paramName): paramName is string => Boolean(paramName));
      paramNames.forEach((paramName) => {
        if (paramName.toLowerCase().includes("variant")) {
          params[paramName] = String(variantId || productId);
          return;
        }
        params[paramName] = String(productId);
      });
      await router.push({ name: productDetailRoute.name, params, query });
      return;
    }
    if (productDetailRoute?.path) {
      const productPath = buildProductDetailPathFromRoute(
        productDetailRoute.path,
        item
      );
      if (productPath) {
        await router.push({ path: productPath, query });
        return;
      }
    }
    await router.push({ path: `/product/${productId}`, query });
  } catch (error) {
    showError(error, "Không thể mở chi tiết sản phẩm lúc này.");
  }
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
  if (
    !cleanReason ||
    cleanReason.toLowerCase() === "null" ||
    cleanReason === "Đơn hàng của bạn đã bị hủy"
  ) {
    return "Đơn hàng của bạn đã bị hủy";
  }
  return cleanReason;
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
  if (order.status >= 1 && order.status !== 4)
    history.push({
      time: new Date(baseDate + 2 * 60 * 60 * 1000),
      title: "Đang chuẩn bị hàng",
      desc: "Người bán đang chuẩn bị kiện hàng của bạn.",
      active: order.status === 1,
    });
  if (order.status >= 2 && order.status !== 4)
    history.push({
      time: new Date(baseDate + 14 * 60 * 60 * 1000),
      title: "Đã giao cho ĐVVC",
      desc: "Kiện hàng đã rời trung tâm phân loại và đang trên đường giao.",
      active: order.status === 2,
    });
  if (order.status === 3)
    history.push({
      time: order.completedAt
        ? new Date(order.completedAt)
        : new Date(baseDate + 48 * 60 * 60 * 1000),
      title: "Đã giao",
      desc: `Kiện hàng của bạn đã được giao. Người nhận: ${
        order.customerName || "Bạn"
      }`,
      active: true,
    });
  if (order.status === 4)
    history.push({
      time: getOrderCancelledAt(order)
        ? new Date(getOrderCancelledAt(order))
        : new Date(baseDate + 15 * 60 * 1000),
      title: "Đã hủy",
      desc: "Đơn hàng của bạn đã bị hủy.",
      active: true,
      isCancel: true,
    });
  if (order.status === 5)
    history.push({
      time: order.deliveryFailedAt
        ? new Date(order.deliveryFailedAt)
        : new Date(baseDate + 48 * 60 * 60 * 1000),
      title: "Giao hàng thất bại",
      desc: order.deliveryFailedReason || "Đơn hàng giao không thành công.",
      active: true,
      isCancel: true,
    });
  if (order.status === 8)
    history.push({
      time: getOrderCancelledAt(order)
        ? new Date(getOrderCancelledAt(order))
        : new Date(baseDate + 15 * 60 * 1000),
      title: "Đã hủy / Chờ hoàn tiền",
      desc: "Đơn hàng đã hủy, vui lòng cập nhật số tài khoản để nhận lại tiền.",
      active: true,
      isCancel: true,
    });
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
  if (match?.[1]) return `${match[1]}ml`;
  return "Đang cập nhật";
};

const getBottleTypeText = (item: any) => {
  const raw =
    item?.bottleType ??
    item?.bottleTypeName ??
    item?.bottleName ??
    item?.variantBottleType ??
    null;
  if (raw !== null && raw !== undefined && String(raw).trim() !== "")
    return String(raw).trim();
  const sku = String(item?.sku || "").toUpperCase();
  if (sku.includes("FULL")) return "Chai gốc Fullbox";
  if (sku.includes("CHIET")) return "Chai chiết";
  return "Đang cập nhật";
};

const getManufacturingDate = (item: any) =>
  item?.manufacturingDate ??
  item?.mfgDate ??
  item?.manufactureDate ??
  item?.productionDate ??
  null;
const getExpirationDate = (item: any) =>
  item?.expirationDate ??
  item?.expiryDate ??
  item?.expiredDate ??
  item?.expDate ??
  null;
const formatMoney = (value: number | null | undefined) =>
  Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });

const getItemUnitDiscount = (item: any) =>
  pickMoneyValue(
    item?.discountAmount,
    item?.unitDiscountAmount,
    item?.unitDiscount,
    item?.flashSaleDiscount,
    item?.itemDiscount
  );
const getItemFinalUnitPrice = (item: any) =>
  pickMoneyValue(
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
  if (originalPrice > finalPrice) return originalPrice;
  if (finalPrice > 0 && discountAmount > 0) return finalPrice + discountAmount;
  return originalPrice || finalPrice;
};
const hasItemPrice = (item: any) =>
  getItemFinalUnitPrice(item) > 0 || getItemOriginalUnitPrice(item) > 0;
const hasItemSale = (item: any) =>
  getItemOriginalUnitPrice(item) > getItemFinalUnitPrice(item);
const getItemLineTotal = (item: any) => {
  const lineTotal = pickMoneyValue(
    item?.lineTotal,
    item?.totalPrice,
    item?.itemTotal,
    item?.subtotal
  );
  if (lineTotal > 0) return lineTotal;
  return getItemFinalUnitPrice(item) * Number(item?.quantity || 0);
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("vi-VN", { hour12: false });
};

const formatDateOnly = (value: string | null | undefined) => {
  if (!value) return "Đang cập nhật";
  const dateOnly = String(value).substring(0, 10);
  const date = new Date(`${dateOnly}T00:00:00`);
  if (Number.isNaN(date.getTime())) return "Đang cập nhật";
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

const hasTextValue = (value: unknown) => {
  return String(value ?? "").trim().length > 0;
};

const hasArrayData = (value: unknown) => {
  return Array.isArray(value) && value.length > 0;
};

const hasPositiveMoneyValue = (...values: unknown[]) => {
  return values.some((value) => toMoneyNumber(value) > 0);
};

const hasRealReturnRequestObject = (request: any) => {
  if (!request) {
    return false;
  }

  return Boolean(
    hasTextValue(request?.id) ||
      hasTextValue(request?.returnRequestId) ||
      hasTextValue(request?.code) ||
      hasTextValue(request?.returnCode) ||
      hasTextValue(request?.requestCode) ||
      hasTextValue(request?.reason) ||
      hasTextValue(request?.returnReason) ||
      hasTextValue(request?.returnRequestReason) ||
      hasTextValue(request?.description) ||
      hasTextValue(request?.returnDescription) ||
      hasTextValue(request?.createdAt) ||
      hasTextValue(request?.returnRequestedAt) ||
      hasTextValue(request?.requestedAt) ||
      hasTextValue(request?.rejectReason) ||
      hasTextValue(request?.rejectedReason) ||
      hasTextValue(request?.returnRejectReason) ||
      hasPositiveMoneyValue(
        request?.refundAmount,
        request?.returnRefundAmount,
        request?.estimatedRefundAmount
      ) ||
      hasArrayData(request?.items) ||
      hasArrayData(request?.returnItems) ||
      hasArrayData(request?.returnRequestItems) ||
      hasArrayData(request?.returnedItems) ||
      hasArrayData(request?.refundItems) ||
      hasArrayData(request?.mediaFiles) ||
      hasArrayData(request?.returnMediaUrls) ||
      hasArrayData(request?.returnMediaFiles) ||
      hasArrayData(request?.returnMedias) ||
      hasArrayData(request?.returnImages) ||
      normalizeReturnStatusValue(
        request?.processStatus ??
          request?.status ??
          request?.returnStatus ??
          request?.refundStatus ??
          null
      ) !== null
  );
};

const hasOrderReturnData = (order: any) => {
  const request = getOrderReturnRequest(order);

  const directReturnStatus = normalizeReturnStatusValue(
    order?.returnProcessStatus ??
      order?.returnStatus ??
      order?.refundStatus ??
      order?.returnRequestStatus ??
      null
  );

  return Boolean(
    Number(order?.status) === 6 ||
      Number(order?.status) === 7 ||
      hasRealReturnRequestObject(request) ||
      hasTextValue(order?.returnReason) ||
      hasTextValue(order?.returnDescription) ||
      hasTextValue(order?.returnRequestedAt) ||
      hasTextValue(order?.returnRejectReason) ||
      hasTextValue(order?.rejectReason) ||
      hasTextValue(order?.rejectedReason) ||
      hasArrayData(order?.returnItems) ||
      hasArrayData(order?.returnRequestItems) ||
      hasArrayData(order?.returnedItems) ||
      hasArrayData(order?.refundItems) ||
      hasArrayData(order?.returnMediaUrls) ||
      hasArrayData(order?.returnImages) ||
      hasArrayData(order?.returnVideos) ||
      hasArrayData(order?.returnMediaFiles) ||
      hasArrayData(order?.returnMedias) ||
      hasPositiveMoneyValue(
        order?.returnRefundAmount,
        order?.estimatedRefundAmount,
        order?.returnEstimatedRefundAmount
      ) ||
      directReturnStatus !== null
  );
};

const isReturnInfoVisible = (order: any) => {
  if (!hasOrderReturnData(order)) {
    return false;
  }

  const processStatus = getOrderReturnProcessStatus(order);

  return processStatus !== "CUSTOMER_CANCELLED" && processStatus !== "UNKNOWN";
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
  return String(reason || "").trim() || "Chưa có lý do hoàn hàng";
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

const getOrderReturnRequestedAt = (order: any) => {
  const request = getOrderReturnRequest(order);

  return (
    order?.returnRequestedAt ??
    order?.returnRequestCreatedAt ??
    order?.returnCreatedAt ??
    request?.createdAt ??
    request?.returnRequestedAt ??
    request?.requestedAt ??
    null
  );
};

const getReturnRequestCode = (order: any) => {
  const request = getOrderReturnRequest(order);

  const rawCode =
    order?.returnRequestCode ??
    order?.returnCode ??
    order?.refundRequestCode ??
    request?.code ??
    request?.returnCode ??
    request?.requestCode ??
    request?.id ??
    request?.returnRequestId ??
    null;

  const cleanCode = String(rawCode || "").trim();

  if (cleanCode) {
    return cleanCode;
  }

  return `HT-${String(order?.orderId || order?.id || "").padStart(6, "0")}`;
};

const normalizeReturnStatusValue = (
  value: unknown
): ReturnProcessStatus | null => {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const normalized = String(value)
    .trim()
    .toUpperCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

  if (
    ["0", "PENDING", "WAITING", "WAITING_APPROVAL", "CHO XU LY"].includes(
      normalized
    )
  ) {
    return "PENDING";
  }

  if (
    ["1", "ACCEPTED", "APPROVED", "CONFIRMED", "DA CHAP NHAN"].includes(
      normalized
    )
  ) {
    return "ACCEPTED";
  }

  if (
    ["2", "REJECTED", "DENIED", "REFUSED", "TU CHOI", "DA TU CHOI"].includes(
      normalized
    )
  ) {
    return "REJECTED";
  }

  if (
    [
      "3",
      "REFUNDED",
      "COMPLETED",
      "DONE",
      "RETURN_COMPLETED",
      "DA HOAN TIEN",
    ].includes(normalized)
  ) {
    return "REFUNDED";
  }

  if (
    [
      "4",
      "CUSTOMER_CANCELLED",
      "CUSTOMER_CANCELED",
      "CANCELLED_BY_CUSTOMER",
      "CANCELED_BY_CUSTOMER",
      "CUSTOMER_CANCEL",
      "CUSTOMER_CANCELED_RETURN",
      "CUSTOMER_CANCELLED_RETURN",
      "KHACH HUY",
      "KHACH DA HUY",
      "KHACH HUY YEU CAU",
      "KHACH DA HUY YEU CAU",
    ].includes(normalized)
  ) {
    return "CUSTOMER_CANCELLED";
  }

  if (
    ["PARTIAL", "PARTIALLY_ACCEPTED", "PARTIALLY_REFUNDED"].includes(normalized)
  ) {
    return "PARTIAL";
  }

  return null;
};

const shouldShowReturnDeadlineText = (order: any) => {
  if (!isOnlineOrder(order) || !isCompletedOrder(order)) {
    return false;
  }

  const returnStatus = getOrderReturnProcessStatus(order);

  if (
    returnStatus === "PENDING" ||
    returnStatus === "ACCEPTED" ||
    returnStatus === "REJECTED" ||
    returnStatus === "REFUNDED" ||
    returnStatus === "PARTIAL"
  ) {
    return false;
  }

  return Boolean(getReturnDeadlineText(order));
};

const getReturnItemRawStatus = (item: any) => {
  return (
    item?.status ??
    item?.returnStatus ??
    item?.returnItemStatus ??
    item?.processStatus ??
    item?.refundStatus ??
    item?.orderItem?.returnStatus ??
    item?.orderItem?.status ??
    null
  );
};

const getOrderReturnProcessStatus = (order: any): ReturnProcessStatus => {
  const request = getOrderReturnRequest(order);

  const directStatus = normalizeReturnStatusValue(
    order?.returnProcessStatus ??
      order?.returnStatus ??
      order?.refundStatus ??
      order?.returnRequestStatus ??
      request?.processStatus ??
      request?.status ??
      request?.returnStatus ??
      request?.refundStatus ??
      null
  );

  if (directStatus) {
    return directStatus;
  }

  const itemStatuses = getRawReturnItemsPayload(order)
    .map((item: any) =>
      normalizeReturnStatusValue(getReturnItemRawStatus(item))
    )
    .filter((status): status is ReturnProcessStatus => Boolean(status));

  if (itemStatuses.length > 0) {
    if (itemStatuses.every((status) => status === "REFUNDED")) {
      return "REFUNDED";
    }

    if (itemStatuses.every((status) => status === "REJECTED")) {
      return "REJECTED";
    }

    if (itemStatuses.every((status) => status === "ACCEPTED")) {
      return "ACCEPTED";
    }

    if (itemStatuses.every((status) => status === "PENDING")) {
      return "PENDING";
    }

    if (itemStatuses.every((status) => status === "CUSTOMER_CANCELLED")) {
      return "CUSTOMER_CANCELLED";
    }

    return "PARTIAL";
  }

  if (Number(order?.status) === 7) {
    return "REFUNDED";
  }

  if (Number(order?.status) === 6) {
    return "PENDING";
  }

  return "UNKNOWN";
};

const getReturnProcessText = (order: any) => {
  const directText = String(
    order?.returnProcessStatusText ??
      order?.returnStatusText ??
      order?.refundStatusText ??
      getOrderReturnRequest(order)?.statusText ??
      ""
  ).trim();

  if (directText) {
    return directText;
  }

  switch (getOrderReturnProcessStatus(order)) {
    case "PENDING":
      return "Chờ shop xử lý";
    case "ACCEPTED":
      return "Đã chấp nhận / Chờ hoàn tiền";
    case "REJECTED":
      return "Đã từ chối hoàn hàng";
    case "REFUNDED":
      return "Đã xử lý hoàn tiền";
    case "PARTIAL":
      return "Đang xử lý một phần";
    case "CUSTOMER_CANCELLED":
      return "Khách đã hủy yêu cầu";
    default:
      return "Đang cập nhật";
  }
};

const getReturnProcessClass = (order: any) => {
  const status = getOrderReturnProcessStatus(order);

  return {
    "is-return-pending": status === "PENDING",
    "is-return-accepted": status === "ACCEPTED",
    "is-return-rejected": status === "REJECTED",
    "is-return-refunded": status === "REFUNDED",
    "is-return-partial": status === "PARTIAL",
    "is-return-cancelled": status === "CUSTOMER_CANCELLED",
  };
};

const getReturnProcessBadgeClass = (order: any) => {
  const status = getOrderReturnProcessStatus(order);

  return {
    "is-pending": status === "PENDING",
    "is-accepted": status === "ACCEPTED",
    "is-rejected": status === "REJECTED",
    "is-refunded": status === "REFUNDED",
    "is-partial": status === "PARTIAL",
    "is-cancelled": status === "CUSTOMER_CANCELLED",
  };
};

const getOrderReturnRejectReason = (order: any) => {
  const request = getOrderReturnRequest(order);

  const directReason =
    order?.returnRejectReason ??
    order?.rejectReason ??
    order?.rejectedReason ??
    order?.returnRejectedReason ??
    request?.rejectReason ??
    request?.rejectedReason ??
    request?.returnRejectReason ??
    null;

  const cleanDirectReason = String(directReason || "").trim();

  if (cleanDirectReason) {
    return cleanDirectReason;
  }

  const itemReason = getRawReturnItemsPayload(order)
    .map((item: any) => getReturnItemRejectReason(item))
    .find((reason) => reason.length > 0);

  return itemReason || "";
};

const getReturnProcessedAt = (order: any) => {
  const request = getOrderReturnRequest(order);

  return (
    order?.returnProcessedAt ??
    order?.returnReviewedAt ??
    order?.returnAcceptedAt ??
    order?.returnRejectedAt ??
    order?.returnRefundedAt ??
    order?.refundedAt ??
    order?.updatedAt ??
    request?.processedAt ??
    request?.reviewedAt ??
    request?.acceptedAt ??
    request?.rejectedAt ??
    request?.refundedAt ??
    request?.updatedAt ??
    null
  );
};

const getReturnProcessTimeline = (order: any): ReturnProcessTimelineStep[] => {
  const status = getOrderReturnProcessStatus(order);
  const requestedAt = getOrderReturnRequestedAt(order);
  const processedAt = getReturnProcessedAt(order);
  const rejectReason = getOrderReturnRejectReason(order);

  const requestedStep: ReturnProcessTimelineStep = {
    key: "requested",
    title: "Gửi yêu cầu",
    desc: getOrderReturnReason(order),
    time: requestedAt,
    icon: "bi-send-check",
    done: true,
    active: status === "PENDING",
  };

  if (status === "REJECTED") {
    return [
      requestedStep,
      {
        key: "rejected",
        title: "Đã từ chối",
        desc: rejectReason || "Yêu cầu hoàn hàng không được chấp nhận.",
        time: processedAt,
        icon: "bi-x-circle",
        done: true,
        active: true,
        rejected: true,
      },
    ];
  }

  if (status === "CUSTOMER_CANCELLED") {
    return [
      requestedStep,
      {
        key: "customer-cancelled",
        title: "Đã hủy yêu cầu",
        desc: "Bạn đã rút lại yêu cầu hoàn hàng trước khi shop xử lý.",
        time: processedAt,
        icon: "bi-arrow-counterclockwise",
        done: true,
        active: true,
      },
    ];
  }

  const acceptedDone =
    status === "ACCEPTED" || status === "REFUNDED" || status === "PARTIAL";
  const refundedDone = status === "REFUNDED";

  return [
    requestedStep,
    {
      key: "accepted",
      title: "Được chấp nhận",
      desc: acceptedDone
        ? "Shop đã chấp nhận yêu cầu hoàn hàng."
        : "Shop đang kiểm tra yêu cầu và bằng chứng của bạn.",
      time: acceptedDone ? processedAt : null,
      icon: "bi-check-circle",
      done: acceptedDone,
      active: status === "ACCEPTED" || status === "PARTIAL",
    },
    {
      key: "refunded",
      title: "Đã xử lý hoàn tiền",
      desc: refundedDone
        ? "Khoản hoàn tiền đã được shop xác nhận xử lý."
        : "Sau khi shop hoàn tiền thực tế, trạng thái sẽ được cập nhật tại đây.",
      time: refundedDone ? processedAt : null,
      icon: "bi-cash-coin",
      done: refundedDone,
      active: status === "REFUNDED",
    },
  ];
};

const getReturnProcessAlert = (order: any): ReturnProcessAlert | null => {
  const status = getOrderReturnProcessStatus(order);
  const refundAmount = getOrderReturnRefundAmount(order);
  const refundMethod = getOrderRefundMethodText(order) || "phương thức đã chọn";

  if (status === "REJECTED") {
    const reason = getOrderReturnRejectReason(order);

    return {
      title: "Yêu cầu hoàn hàng bị từ chối",
      desc: reason
        ? `Lý do: ${reason}`
        : "Shop đã từ chối yêu cầu hoàn hàng. Vui lòng xem chi tiết sản phẩm bên dưới.",
      icon: "bi-x-circle",
      className: "is-rejected",
    };
  }

  if (status === "REFUNDED") {
    return {
      title: "Đã xử lý hoàn tiền",
      desc:
        refundAmount > 0
          ? `Shop đã xác nhận hoàn ${formatMoney(
              refundAmount
            )} qua ${refundMethod}.`
          : "Shop đã xác nhận xử lý hoàn tiền cho yêu cầu này.",
      icon: "bi-check-circle",
      className: "is-refunded",
    };
  }

  if (status === "ACCEPTED") {
    return {
      title: "Yêu cầu đã được chấp nhận",
      desc:
        refundAmount > 0
          ? `Shop sẽ hoàn ${formatMoney(
              refundAmount
            )} qua ${refundMethod}. Chỉ khi shop xác nhận đã hoàn tiền, trạng thái mới chuyển sang hoàn tất.`
          : "Shop đã chấp nhận yêu cầu và đang xử lý bước hoàn tiền.",
      icon: "bi-clock-history",
      className: "is-accepted",
    };
  }

  if (status === "PENDING") {
    return {
      title: "Đang chờ shop xử lý",
      desc: "Yêu cầu hoàn hàng đã được gửi. Shop sẽ kiểm tra lý do, sản phẩm hoàn và ảnh/video bằng chứng trước khi chấp nhận hoặc từ chối.",
      icon: "bi-hourglass-split",
      className: "is-pending",
    };
  }

  if (status === "CUSTOMER_CANCELLED") {
    return {
      title: "Bạn đã hủy yêu cầu hoàn hàng",
      desc: "Yêu cầu cũ đã được rút lại. Nếu đơn còn trong hạn 3 ngày kể từ lúc đơn hàng hoàn thành, bạn có thể gửi yêu cầu hoàn hàng mới.",
      icon: "bi-arrow-counterclockwise",
      className: "is-cancelled",
    };
  }

  return null;
};

const getReturnRefundLabel = (order: any) => {
  switch (getOrderReturnProcessStatus(order)) {
    case "REFUNDED":
      return "Số tiền đã hoàn";
    case "ACCEPTED":
      return "Số tiền sẽ hoàn";
    case "REJECTED":
      return "Số tiền yêu cầu hoàn";
    case "CUSTOMER_CANCELLED":
      return "Số tiền yêu cầu hoàn";
    default:
      return "Tổng tiền hoàn";
  }
};

const getOrderRefundMethodText = (order: any) => {
  const request = getOrderReturnRequest(order);

  const rawMethod = String(
    order?.refundMethod ??
      order?.returnRefundMethod ??
      request?.refundMethod ??
      request?.returnRefundMethod ??
      ""
  )
    .trim()
    .toUpperCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

  if (!rawMethod) {
    return "";
  }

  if (
    rawMethod === "1" ||
    rawMethod === "BANK_TRANSFER" ||
    rawMethod.includes("BANK") ||
    rawMethod.includes("TRANSFER") ||
    rawMethod.includes("CHUYEN KHOAN") ||
    rawMethod.includes("NGAN HANG")
  ) {
    return "Tài khoản ngân hàng";
  }

  if (
    rawMethod === "2" ||
    rawMethod === "STORE" ||
    rawMethod.includes("CUA HANG") ||
    rawMethod.includes("TAI QUAY")
  ) {
    return "Hoàn tại cửa hàng";
  }

  return String(order?.refundMethod ?? request?.refundMethod ?? "");
};

const canCancelReturnRequest = (order: any) => {
  return (
    isOnlineOrder(order) &&
    Number(order?.status) === 6 &&
    getOrderReturnProcessStatus(order) === "PENDING"
  );
};

const getDeliveryRefundAmount = (order: any) =>
  toMoneyNumber(order?.deliveryRefundAmount);

const isDeliveryRefundInfoVisible = (order: any) => {
  const status = Number(order?.status);
  return (
    (status === 5 || status === 8 || status === 4) &&
    getDeliveryRefundAmount(order) > 0
  );
};

const hasDeliveryRefundBankInfo = (order: any) => {
  return Boolean(
    String(order?.deliveryRefundBankName || "").trim() &&
      String(order?.deliveryRefundBankAccountNumber || "").trim() &&
      String(order?.deliveryRefundBankAccountHolder || "").trim()
  );
};

const hasAnyDeliveryRefundBankInfo = (order: any) => {
  return Boolean(
    String(order?.deliveryRefundBankName || "").trim() ||
      String(order?.deliveryRefundBankAccountNumber || "").trim() ||
      String(order?.deliveryRefundBankAccountHolder || "").trim()
  );
};

const isDeliveryRefundCompleted = (order: any) => {
  return Boolean(order?.deliveryRefundedAt || order?.deliveryRefundCompleted);
};

const canSubmitDeliveryRefundBank = (order: any) => {
  if (order?.canSubmitDeliveryRefundBank === false) {
    return false;
  }

  return (
    isDeliveryRefundInfoVisible(order) &&
    !hasAnyDeliveryRefundBankInfo(order) &&
    !isDeliveryRefundCompleted(order)
  );
};

const getDeliveryRefundStatusText = (order: any) => {
  if (!isDeliveryRefundInfoVisible(order)) {
    return "Không cần hoàn tiền";
  }

  if (isDeliveryRefundCompleted(order)) {
    return "Đã hoàn tiền";
  }

  if (hasDeliveryRefundBankInfo(order)) {
    return "Chờ shop hoàn tiền";
  }

  if (hasAnyDeliveryRefundBankInfo(order)) {
    return "Cần liên hệ shop";
  }

  return "Chờ nhập STK";
};

const getDeliveryRefundDescription = (order: any) => {
  const amount = formatMoney(getDeliveryRefundAmount(order));

  if (isDeliveryRefundCompleted(order)) {
    return `Shop đã xác nhận hoàn ${amount} cho bạn.`;
  }

  if (hasDeliveryRefundBankInfo(order)) {
    return `Shop đã nhận thông tin tài khoản và sẽ hoàn ${amount} cho bạn. Thông tin này không thể tự chỉnh sửa.`;
  }

  if (hasAnyDeliveryRefundBankInfo(order)) {
    return "Thông tin tài khoản hoàn tiền chưa đầy đủ. Vui lòng liên hệ shop để được hỗ trợ.";
  }

  if (Number(order?.status) === 8) {
    return `Đơn hàng đã được hủy. Vui lòng nhập thông tin tài khoản ngân hàng để shop hoàn lại ${amount}.`;
  }

  return `Đơn đã thanh toán trước nhưng giao thất bại. Vui lòng nhập thông tin tài khoản ngân hàng để shop hoàn ${amount}.`;
};

const getDeliveryRefundBoxClass = (order: any) => ({
  "is-waiting-bank": canSubmitDeliveryRefundBank(order),
  "is-waiting-shop":
    isDeliveryRefundInfoVisible(order) &&
    hasDeliveryRefundBankInfo(order) &&
    !isDeliveryRefundCompleted(order),
  "is-refunded": isDeliveryRefundCompleted(order),
});

type VietQrBank = {
  id: string;
  name: string;
  code: string;
  bin: string;
  shortName: string;
  logo: string;
  displayName: string;
  searchText: string;
};

const VIETQR_BANKS_API_URL = "https://api.vietqr.io/v2/banks";
const VIETQR_BANK_CACHE_MS = 60 * 60 * 1000;

const deliveryRefundBanks = ref<VietQrBank[]>([]);
const deliveryRefundBanksLoadedAt = ref(0);

const normalizeDeliveryRefundInput = (value: unknown) =>
  String(value ?? "")
    .trim()
    .replace(/[\r\n\t]+/g, " ")
    .replace(/\s{2,}/g, " ");

const normalizeDeliveryRefundAccountNumber = (value: unknown) =>
  String(value ?? "")
    .replace(/\s+/g, "")
    .trim();

const normalizeBankSearchText = (value: unknown) =>
  normalizeDeliveryRefundInput(value)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

const buildBankSearchText = (values: unknown[]) =>
  values
    .map((value) => normalizeBankSearchText(value))
    .filter(Boolean)
    .join(" ");

const mapVietQrBank = (rawBank: unknown): VietQrBank | null => {
  if (!rawBank || typeof rawBank !== "object") {
    return null;
  }

  const bank = rawBank as Record<string, unknown>;
  const name = normalizeDeliveryRefundInput(bank.name);
  const shortName = normalizeDeliveryRefundInput(bank.shortName);
  const code = normalizeDeliveryRefundInput(bank.code);
  const bin = normalizeDeliveryRefundInput(bank.bin);
  const logo = normalizeDeliveryRefundInput(bank.logo);
  const displayName = shortName || name || code;

  if (!displayName) {
    return null;
  }

  return {
    id:
      normalizeDeliveryRefundInput(bank.id) || `${code}-${bin}-${displayName}`,
    name,
    code,
    bin,
    shortName,
    logo,
    displayName,
    searchText: buildBankSearchText([name, shortName, code, bin]),
  };
};

const fetchDeliveryRefundBanksFromVietQr = async (force = false) => {
  const now = Date.now();

  if (
    !force &&
    deliveryRefundBanks.value.length > 0 &&
    now - deliveryRefundBanksLoadedAt.value < VIETQR_BANK_CACHE_MS
  ) {
    return deliveryRefundBanks.value;
  }

  const response = await fetch(VIETQR_BANKS_API_URL, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error("Không tải được danh sách ngân hàng VietQR.");
  }

  const payload: { data?: unknown } = await response.json();
  const rawBanks: unknown[] = Array.isArray(payload.data) ? payload.data : [];

  const banks: VietQrBank[] = rawBanks
    .map((bank: unknown) => mapVietQrBank(bank))
    .filter((bank: VietQrBank | null): bank is VietQrBank => bank !== null)
    .filter((bank: VietQrBank) => bank.displayName.length <= 100);

  if (banks.length === 0) {
    throw new Error("Danh sách ngân hàng VietQR đang trống.");
  }

  deliveryRefundBanks.value = banks;
  deliveryRefundBanksLoadedAt.value = now;

  return banks;
};

const findDeliveryRefundBank = (bankName: string) => {
  const cleanBankName = normalizeBankSearchText(bankName);

  if (!cleanBankName) {
    return null;
  }

  return (
    deliveryRefundBanks.value.find((bank) => {
      return [
        bank.displayName,
        bank.shortName,
        bank.name,
        bank.code,
        bank.bin,
      ].some((value) => normalizeBankSearchText(value) === cleanBankName);
    }) || null
  );
};

const getDeliveryRefundBankSubtitle = (bank: VietQrBank) => {
  const meta = [bank.code, bank.bin].filter(Boolean).join(" · ");

  if (bank.name && bank.name !== bank.displayName) {
    return meta ? `${bank.name} · ${meta}` : bank.name;
  }

  return meta || "Ngân hàng hỗ trợ VietQR";
};

const buildDeliveryRefundBankOptionsHtml = (
  keyword = "",
  selectedBank?: string | null
) => {
  const cleanKeyword = normalizeBankSearchText(keyword);
  const selected = normalizeDeliveryRefundInput(selectedBank);

  const banks = cleanKeyword
    ? deliveryRefundBanks.value.filter((bank) =>
        bank.searchText.includes(cleanKeyword)
      )
    : deliveryRefundBanks.value;

  if (banks.length === 0) {
    return `
      <div class="delivery-refund-bank-empty">
        Không tìm thấy ngân hàng phù hợp.
      </div>
    `;
  }

  return banks
    .map((bank) => {
      const isSelected =
        normalizeBankSearchText(bank.displayName) ===
          normalizeBankSearchText(selected) ||
        normalizeBankSearchText(bank.name) ===
          normalizeBankSearchText(selected) ||
        normalizeBankSearchText(bank.shortName) ===
          normalizeBankSearchText(selected) ||
        normalizeBankSearchText(bank.code) ===
          normalizeBankSearchText(selected);

      const selectedClass = isSelected ? " is-selected" : "";
      const selectedIcon = isSelected ? '<i class="bi bi-check-lg"></i>' : "";
      const logoHtml = bank.logo
        ? `<img src="${escapeHtml(bank.logo)}" alt="${escapeHtml(
            bank.displayName
          )}" class="delivery-refund-bank-logo" />`
        : `<span class="delivery-refund-bank-logo is-empty"><i class="bi bi-bank"></i></span>`;

      return `
        <button
          type="button"
          class="delivery-refund-bank-option${selectedClass}"
          data-bank-value="${escapeHtml(bank.displayName)}"
        >
          ${logoHtml}
          <span class="delivery-refund-bank-content">
            <strong>${escapeHtml(bank.displayName)}</strong>
            <small>${escapeHtml(getDeliveryRefundBankSubtitle(bank))}</small>
          </span>
          ${selectedIcon}
        </button>
      `;
    })
    .join("");
};

const setDeliveryRefundBankListHtml = (
  keyword = "",
  selectedBank?: string | null
) => {
  const bankListElement = document.getElementById("delivery-refund-bank-list");

  if (!bankListElement) {
    return;
  }

  bankListElement.innerHTML = buildDeliveryRefundBankOptionsHtml(
    keyword,
    selectedBank
  );

  bankListElement
    .querySelectorAll<HTMLButtonElement>(".delivery-refund-bank-option")
    .forEach((button) => {
      button.addEventListener("click", () => {
        const bankName = normalizeDeliveryRefundInput(button.dataset.bankValue);
        const hiddenInput = document.getElementById(
          "delivery-refund-bank-name"
        ) as HTMLInputElement | null;
        const searchInput = document.getElementById(
          "delivery-refund-bank-search"
        ) as HTMLInputElement | null;

        if (hiddenInput) {
          hiddenInput.value = bankName;
        }

        if (searchInput) {
          searchInput.value = bankName;
        }

        setDeliveryRefundBankListHtml(bankName, bankName);
      });
    });
};

const validateDeliveryRefundBankForm = (
  bankName: string,
  bankAccountNumber: string,
  bankAccountHolder: string
) => {
  if (!bankName) {
    return "Vui lòng chọn ngân hàng.";
  }

  if (!findDeliveryRefundBank(bankName)) {
    return "Vui lòng chọn ngân hàng trong danh sách VietQR.";
  }

  if (bankName.length < 2 || bankName.length > 100) {
    return "Tên ngân hàng phải từ 2 đến 100 ký tự.";
  }

  if (!/^[\p{L}0-9\s.()\-/&]+$/u.test(bankName)) {
    return "Tên ngân hàng chứa ký tự không hợp lệ.";
  }

  if (!bankAccountNumber) {
    return "Vui lòng nhập số tài khoản.";
  }

  if (!/^[0-9]{6,30}$/.test(bankAccountNumber)) {
    return "Số tài khoản chỉ gồm số và phải từ 6 đến 30 chữ số.";
  }

  if (/^0+$/.test(bankAccountNumber)) {
    return "Số tài khoản không hợp lệ.";
  }

  if (!bankAccountHolder) {
    return "Vui lòng nhập tên chủ tài khoản.";
  }

  if (bankAccountHolder.length < 2 || bankAccountHolder.length > 100) {
    return "Tên chủ tài khoản phải từ 2 đến 100 ký tự.";
  }

  if (bankAccountHolder.split(/\s+/).length < 2) {
    return "Tên chủ tài khoản phải gồm ít nhất 2 từ.";
  }

  if (!/.*\p{L}.*/u.test(bankAccountHolder)) {
    return "Tên chủ tài khoản phải có ít nhất một chữ cái.";
  }

  if (/\d/.test(bankAccountHolder)) {
    return "Tên chủ tài khoản không được chứa số.";
  }

  if (!/^[\p{L}\s'.-]+$/u.test(bankAccountHolder)) {
    return "Tên chủ tài khoản chỉ được chứa chữ cái, khoảng trắng, dấu chấm, dấu nháy hoặc dấu gạch ngang.";
  }

  return "";
};

const openDeliveryRefundBankModal = async (order: CustomerOrderResponse) => {
  if (!canSubmitDeliveryRefundBank(order)) {
    return;
  }

  try {
    await fetchDeliveryRefundBanksFromVietQr();
  } catch (error: any) {
    await Swal.fire({
      icon: "error",
      title: "Không tải được ngân hàng",
      text:
        error?.message ||
        "Không lấy được danh sách ngân hàng từ VietQR. Vui lòng thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  const currentBank = findDeliveryRefundBank(
    order.deliveryRefundBankName || ""
  );
  const currentBankName = currentBank?.displayName || "";

  const result = await Swal.fire<{
    bankName: string;
    bankAccountNumber: string;
    bankAccountHolder: string;
  }>({
    title: "Nhập thông tin hoàn tiền",
    html: `
      <div class="delivery-refund-modal">
        <div class="delivery-refund-modal-alert">
          <i class="bi bi-info-circle"></i>
          <span>Shop sẽ hoàn <strong>${escapeHtml(
            formatMoney(getDeliveryRefundAmount(order))
          )}</strong> cho đơn ${escapeHtml(
      generateOrderCode(order.orderId)
    )}. Thông tin này chỉ gửi được 1 lần và không thể tự chỉnh sửa.</span>
        </div>

        <label for="delivery-refund-bank-search" class="delivery-refund-modal-label">
          Ngân hàng <span>*</span>
        </label>
        <div class="delivery-refund-bank-picker">
          <div class="delivery-refund-bank-search-wrap">
            <i class="bi bi-search"></i>
            <input
              id="delivery-refund-bank-search"
              class="delivery-refund-bank-search"
              autocomplete="off"
              placeholder="Tìm theo tên ngân hàng, mã ngân hàng hoặc BIN"
              value="${escapeHtml(currentBankName)}"
            />
          </div>
          <input
            id="delivery-refund-bank-name"
            type="hidden"
            value="${escapeHtml(currentBankName)}"
          />
          <div id="delivery-refund-bank-list" class="delivery-refund-bank-list"></div>
        </div>
        <div class="delivery-refund-modal-help">
          Danh sách ngân hàng và logo được lấy trực tiếp từ VietQR.
        </div>

        <label for="delivery-refund-account-number" class="delivery-refund-modal-label">
          Số tài khoản <span>*</span>
        </label>
        <input
          id="delivery-refund-account-number"
          class="swal2-input delivery-refund-modal-control"
          inputmode="numeric"
          maxlength="50"
          placeholder="Ví dụ: 0123456789"
          value="${escapeHtml(order.deliveryRefundBankAccountNumber || "")}"
        />
        <div class="delivery-refund-modal-help">
          Chỉ nhập số và khoảng trắng. Hệ thống sẽ tự bỏ khoảng trắng trước khi lưu.
        </div>

        <label for="delivery-refund-account-holder" class="delivery-refund-modal-label">
          Tên chủ tài khoản <span>*</span>
        </label>
        <input
          id="delivery-refund-account-holder"
          class="swal2-input delivery-refund-modal-control"
          maxlength="100"
          placeholder="Ví dụ: NGUYEN VAN NAM"
          value="${escapeHtml(order.deliveryRefundBankAccountHolder || "")}"
        />
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Gửi thông tin",
    cancelButtonText: "Quay lại",
    reverseButtons: true,
    focusConfirm: false,
    customClass: {
      popup: "swal-custom-popup delivery-refund-swal",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-gold-confirm",
    },
    didOpen: () => {
      const bankSearchInput = document.getElementById(
        "delivery-refund-bank-search"
      ) as HTMLInputElement | null;
      const bankHiddenInput = document.getElementById(
        "delivery-refund-bank-name"
      ) as HTMLInputElement | null;
      const accountInput = document.getElementById(
        "delivery-refund-account-number"
      ) as HTMLInputElement | null;
      const accountHolderInput = document.getElementById(
        "delivery-refund-account-holder"
      ) as HTMLInputElement | null;

      setDeliveryRefundBankListHtml("", currentBankName);

      bankSearchInput?.addEventListener("input", () => {
        const keyword = normalizeDeliveryRefundInput(bankSearchInput.value);
        const matchedBank = findDeliveryRefundBank(keyword);

        if (bankHiddenInput) {
          bankHiddenInput.value =
            matchedBank &&
            normalizeBankSearchText(matchedBank.displayName) ===
              normalizeBankSearchText(keyword)
              ? matchedBank.displayName
              : "";
        }

        setDeliveryRefundBankListHtml(keyword, bankHiddenInput?.value || "");
      });

      bankSearchInput?.addEventListener("focus", () => {
        setDeliveryRefundBankListHtml(
          normalizeDeliveryRefundInput(bankSearchInput.value),
          bankHiddenInput?.value || ""
        );
      });

      accountInput?.addEventListener("input", () => {
        accountInput.value = accountInput.value
          .replace(/[^0-9\s]/g, "")
          .replace(/\s{2,}/g, " ");
      });

      accountHolderInput?.addEventListener("input", () => {
        accountHolderInput.value = accountHolderInput.value
          .replace(/[^\p{L}\s'.-]/gu, "")
          .replace(/\s{2,}/g, " ")
          .toUpperCase();
      });
    },
    preConfirm: () => {
      const bankElement = document.getElementById(
        "delivery-refund-bank-name"
      ) as HTMLInputElement | null;
      const accountNumberElement = document.getElementById(
        "delivery-refund-account-number"
      ) as HTMLInputElement | null;
      const accountHolderElement = document.getElementById(
        "delivery-refund-account-holder"
      ) as HTMLInputElement | null;

      const bankName = normalizeDeliveryRefundInput(bankElement?.value);
      const bankAccountNumber = normalizeDeliveryRefundAccountNumber(
        accountNumberElement?.value
      );
      const bankAccountHolder = normalizeDeliveryRefundInput(
        accountHolderElement?.value
      ).toUpperCase();

      const validationMessage = validateDeliveryRefundBankForm(
        bankName,
        bankAccountNumber,
        bankAccountHolder
      );

      if (validationMessage) {
        Swal.showValidationMessage(validationMessage);
        return false;
      }

      return {
        bankName,
        bankAccountNumber,
        bankAccountHolder,
      };
    },
  });

  if (!result.isConfirmed || !result.value) {
    return;
  }

  const confirmResult = await Swal.fire({
    icon: "warning",
    title: "Xác nhận thông tin hoàn tiền?",
    html: `
      <div style="text-align:left;line-height:1.6">
        <p style="margin-bottom:8px">Thông tin tài khoản hoàn tiền <b>chỉ gửi được 1 lần</b>. Sau khi gửi, bạn không thể tự chỉnh sửa trên hệ thống. Vui lòng kiểm tra thật kĩ</p>
        <p style="margin-bottom:4px"><b>Ngân hàng:</b> ${escapeHtml(
          result.value.bankName
        )}</p>
        <p style="margin-bottom:4px"><b>Số tài khoản:</b> ${escapeHtml(
          result.value.bankAccountNumber
        )}</p>
        <p style="margin-bottom:0"><b>Chủ tài khoản:</b> ${escapeHtml(
          result.value.bankAccountHolder
        )}</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Chắc chắn gửi",
    cancelButtonText: "Kiểm tra lại",
    reverseButtons: true,
    customClass: {
      popup: "swal-custom-popup",
      title: "swal-custom-title",
      cancelButton: "swal-custom-cancel",
      confirmButton: "swal-gold-confirm",
    },
  });

  if (!confirmResult.isConfirmed) {
    return;
  }

  try {
    store.orderLoading = true;
    await customerProfileService.submitDeliveryRefundBank(
      order.orderId,
      result.value
    );
    await fetchOrdersAndReviews();
    openedOrderId.value = order.orderId;
    toast("success", "Đã gửi thông tin tài khoản hoàn tiền.");
  } catch (error) {
    if (
      !(await handleCustomerOrderConflict(
        error,
        order.orderId,
        "Thông tin hoàn tiền hoặc trạng thái đơn hàng đã thay đổi. Dữ liệu mới đã được tải lại, vui lòng kiểm tra lại."
      ))
    ) {
      showError(error, "Không thể gửi thông tin hoàn tiền lúc này.");
    }
  } finally {
    store.orderLoading = false;
  }
};

type ReturnProcessStatus =
  | "PENDING"
  | "ACCEPTED"
  | "REJECTED"
  | "REFUNDED"
  | "PARTIAL"
  | "CUSTOMER_CANCELLED"
  | "UNKNOWN";

type ReturnProcessTimelineStep = {
  key: string;
  title: string;
  desc: string;
  time: string | null;
  icon: string;
  done: boolean;
  active: boolean;
  rejected?: boolean;
};

type ReturnProcessAlert = {
  title: string;
  desc: string;
  icon: string;
  className: string;
};

type ReturnSelectedItemView = {
  orderItemId: number | null;
  productId: number | null;
  productVariantId: number | null;
  productName: string | null;
  brandName: string | null;
  sku: string | null;
  image: string;
  capacity: string | null;
  capacityName?: string | null;
  bottleType: string | null;
  bottleTypeName?: string | null;
  orderedQuantity: number;
  returnQuantity: number;
  unitFinalPrice: number;
  itemAmount: number;
  voucherAllocatedAmount: number;
  refundAmount: number;
  status: number | string | null;
  statusText?: string | null;
  rejectReason?: string | null;
};

const toPositiveNumberOrNull = (value: unknown) => {
  const numberValue = Number(value);

  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null;
};

const getOrderReturnShippingFee = (order: any) => {
  const request = getOrderReturnRequest(order);

  return pickMoneyValue(
    order?.returnShippingFee,
    order?.refundShippingFee,
    order?.shippingFeeRefundAmount,
    request?.returnShippingFee,
    request?.refundShippingFee,
    request?.shippingFeeRefundAmount
  );
};

const getOrderReturnProductRefundAmount = (order: any) => {
  const totalRefund = getOrderReturnRefundAmount(order);
  const returnShippingFee = getOrderReturnShippingFee(order);

  return Math.max(0, totalRefund - returnShippingFee);
};

const getOrderReturnRefundAmount = (order: any) => {
  const request = getOrderReturnRequest(order);

  return pickMoneyValue(
    order?.returnRefundAmount,
    order?.refundAmount,
    order?.estimatedRefundAmount,
    order?.returnEstimatedRefundAmount,
    request?.refundAmount,
    request?.returnRefundAmount,
    request?.estimatedRefundAmount
  );
};

const getRawReturnItemsPayload = (order: any) => {
  const request = getOrderReturnRequest(order);

  const rawItems =
    order?.returnItems ??
    order?.returnRequestItems ??
    order?.returnedItems ??
    order?.refundItems ??
    order?.exchangeItems ??
    request?.items ??
    request?.returnItems ??
    request?.returnRequestItems ??
    request?.returnedItems ??
    request?.refundItems ??
    [];

  return Array.isArray(rawItems) ? rawItems : [rawItems];
};

const getReturnPayloadOrderItemId = (payload: any) => {
  return toPositiveNumberOrNull(
    payload?.orderItemId ??
      payload?.orderItem?.id ??
      payload?.orderItem?.orderItemId ??
      payload?.itemId ??
      payload?.orderDetailId ??
      null
  );
};

const getReturnPayloadProductId = (payload: any) => {
  return toPositiveNumberOrNull(
    payload?.productId ??
      payload?.product?.id ??
      payload?.product?.productId ??
      payload?.orderItem?.productId ??
      payload?.orderItem?.product?.id ??
      payload?.orderItem?.product?.productId ??
      payload?.productVariant?.productId ??
      payload?.productVariant?.product?.id ??
      payload?.productVariant?.product?.productId ??
      null
  );
};

const getReturnPayloadVariantId = (payload: any) => {
  return toPositiveNumberOrNull(
    payload?.productVariantId ??
      payload?.variantId ??
      payload?.productVariant?.id ??
      payload?.productVariant?.variantId ??
      payload?.orderItem?.productVariantId ??
      payload?.orderItem?.variantId ??
      payload?.orderItem?.productVariant?.id ??
      payload?.orderItem?.productVariant?.variantId ??
      null
  );
};

const getReturnPayloadStatus = (payload: any) => {
  return getReturnItemRawStatus(payload);
};

const getReturnPayloadStatusText = (payload: any) => {
  const text =
    payload?.statusText ??
    payload?.returnStatusText ??
    payload?.returnItemStatusText ??
    payload?.processStatusText ??
    payload?.orderItem?.returnStatusText ??
    null;

  return String(text || "").trim() || null;
};

const getReturnItemRejectReason = (item: any) => {
  const reason =
    item?.rejectReason ??
    item?.rejectedReason ??
    item?.returnRejectReason ??
    item?.returnRejectedReason ??
    item?.orderItem?.rejectReason ??
    item?.orderItem?.rejectedReason ??
    null;

  return String(reason || "").trim();
};

const getReturnItemStatusText = (item: any) => {
  const directText = String(item?.statusText || "").trim();

  if (directText) {
    return directText;
  }

  const status = normalizeReturnStatusValue(item?.status);

  switch (status) {
    case "PENDING":
      return "Chờ xử lý";
    case "ACCEPTED":
      return "Đã chấp nhận";
    case "REJECTED":
      return "Từ chối";
    case "REFUNDED":
      return "Đã hoàn tiền";
    case "PARTIAL":
      return "Xử lý một phần";
    case "CUSTOMER_CANCELLED":
      return "Khách đã hủy yêu cầu";
    default:
      return "Đang cập nhật";
  }
};

const getReturnItemStatusClass = (item: any) => {
  const status = normalizeReturnStatusValue(item?.status);

  return {
    "is-pending": status === "PENDING" || status === null,
    "is-accepted": status === "ACCEPTED",
    "is-rejected": status === "REJECTED",
    "is-refunded": status === "REFUNDED",
    "is-partial": status === "PARTIAL",
    "is-cancelled": status === "CUSTOMER_CANCELLED",
  };
};

const findBaseOrderItemForReturn = (order: any, payload: any) => {
  const orderItems = Array.isArray(order?.items) ? order.items : [];

  const orderItemId = getReturnPayloadOrderItemId(payload);
  const variantId = getReturnPayloadVariantId(payload);
  const productId = getReturnPayloadProductId(payload);

  return (
    orderItems.find((item: any) => {
      if (
        orderItemId &&
        Number(item?.orderItemId ?? item?.id) === orderItemId
      ) {
        return true;
      }

      if (
        variantId &&
        Number(item?.productVariantId ?? item?.variantId) === variantId
      ) {
        return true;
      }

      if (productId && Number(item?.productId) === productId) {
        return true;
      }

      return false;
    }) ?? null
  );
};

const getReturnItemQuantity = (payload: any) => {
  return (
    toPositiveNumberOrNull(
      payload?.returnQuantity ??
        payload?.quantity ??
        payload?.qty ??
        payload?.returnedQuantity ??
        payload?.requestQuantity ??
        payload?.orderItemQuantity ??
        null
    ) ?? 0
  );
};

const getReturnItemRefundAmount = (
  order: any,
  payload: any,
  selectedItemCount: number
) => {
  const itemRefund = pickMoneyValue(
    payload?.refundAmount,
    payload?.returnRefundAmount,
    payload?.estimatedRefundAmount,
    payload?.amount,
    payload?.returnAmount
  );

  if (itemRefund > 0) {
    return itemRefund;
  }

  const orderRefund = getOrderReturnRefundAmount(order);

  if (selectedItemCount === 1 && orderRefund > 0) {
    return orderRefund;
  }

  return 0;
};

const getOrderReturnSelectedItems = (order: any): ReturnSelectedItemView[] => {
  const rawItems = getRawReturnItemsPayload(order).filter(
    (payload: any) => payload !== null && payload !== undefined
  );

  if (rawItems.length === 0) {
    return [];
  }

  return rawItems
    .map((payload: any) => {
      const baseItem = findBaseOrderItemForReturn(order, payload);

      const mergedItem = {
        ...(baseItem || {}),
        ...(payload?.orderItem || {}),
        ...payload,
      };

      const orderItemId =
        getReturnPayloadOrderItemId(payload) ??
        toPositiveNumberOrNull(baseItem?.orderItemId ?? baseItem?.id ?? null);

      const productId =
        getReturnPayloadProductId(payload) ?? getProductIdFromItem(baseItem);

      const productVariantId =
        getReturnPayloadVariantId(payload) ??
        getProductVariantIdFromItem(baseItem);

      const orderedQuantity =
        toPositiveNumberOrNull(
          payload?.orderedQuantity ??
            payload?.orderQuantity ??
            payload?.quantityPurchased ??
            payload?.orderItem?.quantity ??
            baseItem?.quantity ??
            null
        ) ?? 0;
      const returnQuantity = getReturnItemQuantity(payload);
      const unitFinalPrice = pickMoneyValue(
        payload?.unitFinalPrice,
        payload?.finalPrice,
        payload?.unitPrice,
        payload?.orderItem?.finalPrice,
        payload?.orderItem?.unitFinalPrice,
        baseItem?.finalPrice,
        baseItem?.unitFinalPrice,
        baseItem?.unitPrice
      );
      const rawItemAmount = pickMoneyValue(
        payload?.itemAmount,
        payload?.returnItemAmount,
        payload?.lineAmount,
        payload?.lineTotal,
        payload?.subtotal
      );
      const itemAmount =
        rawItemAmount > 0
          ? rawItemAmount
          : unitFinalPrice > 0 && returnQuantity > 0
          ? unitFinalPrice * returnQuantity
          : 0;
      const voucherAllocatedAmount = pickMoneyValue(
        payload?.voucherAllocatedAmount,
        payload?.allocatedVoucherAmount,
        payload?.discountAllocatedAmount,
        payload?.orderDiscountAllocatedAmount
      );
      const refundAmount = getReturnItemRefundAmount(
        order,
        payload,
        rawItems.length
      );

      return {
        orderItemId,
        productId,
        productVariantId,
        productName:
          payload?.productName ??
          payload?.name ??
          payload?.orderItem?.productName ??
          baseItem?.productName ??
          null,
        brandName:
          payload?.brandName ??
          payload?.brand ??
          payload?.orderItem?.brandName ??
          baseItem?.brandName ??
          null,
        sku: payload?.sku ?? payload?.orderItem?.sku ?? baseItem?.sku ?? null,
        image: getItemImage(mergedItem) || FALLBACK_IMAGE,
        capacity:
          payload?.capacity ??
          payload?.capacityName ??
          payload?.orderItem?.capacity ??
          payload?.orderItem?.capacityName ??
          baseItem?.capacity ??
          baseItem?.capacityName ??
          null,
        capacityName:
          payload?.capacityName ??
          payload?.orderItem?.capacityName ??
          baseItem?.capacityName ??
          null,
        bottleType:
          payload?.bottleType ??
          payload?.bottleTypeName ??
          payload?.orderItem?.bottleType ??
          payload?.orderItem?.bottleTypeName ??
          baseItem?.bottleType ??
          baseItem?.bottleTypeName ??
          null,
        bottleTypeName:
          payload?.bottleTypeName ??
          payload?.orderItem?.bottleTypeName ??
          baseItem?.bottleTypeName ??
          null,
        status: getReturnPayloadStatus(payload),
        statusText: getReturnPayloadStatusText(payload),
        rejectReason: getReturnItemRejectReason(payload),
        orderedQuantity,
        returnQuantity,
        unitFinalPrice,
        itemAmount,
        voucherAllocatedAmount,
        refundAmount,
      };
    })
    .filter(
      (item) => item.orderItemId || item.productVariantId || item.productId
    );
};

type ReturnMediaView = {
  url: string;
  isVideo: boolean;
};

const getLocalBackendBaseUrl = () => {
  const isLocalhost =
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1";
  if (isLocalhost)
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  return window.location.origin;
};

const getApiAssetBaseUrl = () => {
  const configuredBaseUrl = String(api.defaults.baseURL || "").trim();
  if (!configuredBaseUrl || configuredBaseUrl.startsWith("/"))
    return getLocalBackendBaseUrl();
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
  if (!cleanUrl) return "";
  if (
    cleanUrl.startsWith("http://") ||
    cleanUrl.startsWith("https://") ||
    cleanUrl.startsWith("data:") ||
    cleanUrl.startsWith("blob:")
  )
    return cleanUrl;
  cleanUrl = cleanUrl.replace(/^\/?api\//, "/");
  const assetBaseUrl = getApiAssetBaseUrl();
  if (cleanUrl.startsWith("/")) return `${assetBaseUrl}${cleanUrl}`;
  if (cleanUrl.startsWith("uploads/")) return `${assetBaseUrl}/${cleanUrl}`;
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
    rawType.includes("/video/upload/") ||
    rawType.includes("/raw/upload/") ||
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
      if (!url) return null;
      return { url, isVideo: isReturnMediaVideo(media, url) };
    })
    .filter((media): media is ReturnMediaView => media !== null);
};

const escapeHtml = (value: unknown) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

type MediaPreviewOptions = {
  title: string;
  counterLabel: string;
  emptyText: string;
  imageAlt: string;
  thumbTitle: string;
};
const RETURN_MEDIA_PREVIEW_OPTIONS: MediaPreviewOptions = {
  title: "Ảnh/video bằng chứng hoàn hàng",
  counterLabel: "bằng chứng hoàn hàng",
  emptyText: "Không có ảnh/video bằng chứng hoàn hàng để hiển thị.",
  imageAlt: "Ảnh bằng chứng hoàn hàng",
  thumbTitle: "Chọn ảnh/video bằng chứng",
};
const REVIEW_MEDIA_PREVIEW_OPTIONS: MediaPreviewOptions = {
  title: "Ảnh/video đánh giá sản phẩm",
  counterLabel: "đánh giá sản phẩm",
  emptyText: "Không có ảnh/video đánh giá để hiển thị.",
  imageAlt: "Ảnh đánh giá sản phẩm",
  thumbTitle: "Chọn ảnh/video đánh giá",
};

const DELIVERY_SUCCESS_MEDIA_PREVIEW_OPTIONS: MediaPreviewOptions = {
  title: "Ảnh minh chứng giao hàng thành công",
  counterLabel: "ảnh minh chứng giao hàng",
  emptyText: "Không có ảnh minh chứng giao hàng thành công để hiển thị.",
  imageAlt: "Ảnh minh chứng giao hàng thành công",
  thumbTitle: "Chọn ảnh minh chứng giao hàng",
};

const DELIVERY_FAILED_MEDIA_PREVIEW_OPTIONS: MediaPreviewOptions = {
  title: "Ảnh minh chứng giao hàng thất bại",
  counterLabel: "ảnh minh chứng giao thất bại",
  emptyText: "Không có ảnh minh chứng giao hàng thất bại để hiển thị.",
  imageAlt: "Ảnh minh chứng giao hàng thất bại",
  thumbTitle: "Chọn ảnh minh chứng giao thất bại",
};

const buildReturnPreviewMainHtml = (
  media: ReturnMediaView,
  options: MediaPreviewOptions
) => {
  const safeUrl = escapeHtml(media.url);
  const safeAlt = escapeHtml(options.imageAlt);
  if (media.isVideo) {
    return `<video src="${safeUrl}" class="return-preview-video" controls autoplay playsinline></video>`;
  }
  return `<img src="${safeUrl}" class="return-preview-image" alt="${safeAlt}" />`;
};

const buildReturnPreviewThumbHtml = (
  media: ReturnMediaView,
  index: number,
  activeIndex: number,
  options: MediaPreviewOptions
) => {
  const safeUrl = escapeHtml(media.url);
  const activeClass = index === activeIndex ? " active" : "";
  const mediaLabel = media.isVideo ? "Video" : "Ảnh";
  const safeAriaLabel = escapeHtml(
    `Xem ${mediaLabel.toLowerCase()} ${options.counterLabel} ${index + 1}`
  );
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
        alt="${escapeHtml(
          `${mediaLabel} ${options.counterLabel} ${index + 1}`
        )}"
      />
    `;

  return `
    <button
      type="button"
      class="return-preview-thumb-btn${activeClass}"
      data-preview-index="${index}"
      aria-label="${safeAriaLabel}"
    >
      ${thumb}
      <span class="return-preview-thumb-badge">
        ${mediaLabel} ${index + 1}
      </span>
      ${
        media.isVideo
          ? `<span class="return-preview-thumb-play"><i class="bi bi-play-fill"></i></span>`
          : ""
      }
    </button>
  `;
};

const buildReturnPreviewHtml = (
  mediaList: ReturnMediaView[],
  activeIndex: number,
  options: MediaPreviewOptions
) => {
  if (mediaList.length === 0)
    return `<div class="return-preview-modal"><div class="return-preview-empty">${escapeHtml(
      options.emptyText
    )}</div></div>`;
  const safeActiveIndex = Math.min(
    Math.max(Number.isFinite(activeIndex) ? activeIndex : 0, 0),
    mediaList.length - 1
  );
  const activeMedia = mediaList[safeActiveIndex];
  if (!activeMedia)
    return `<div class="return-preview-modal"><div class="return-preview-empty">${escapeHtml(
      options.emptyText
    )}</div></div>`;
  const hasMultipleMedia = mediaList.length > 1;
  const activeMediaLabel = activeMedia.isVideo ? "Video" : "Ảnh";
  return `<div class="return-preview-modal"><div class="return-preview-counter">${activeMediaLabel} ${escapeHtml(
    options.counterLabel
  )}<strong>${safeActiveIndex + 1}/${
    mediaList.length
  }</strong></div><div class="return-preview-stage">${
    hasMultipleMedia
      ? `<button type="button" class="return-preview-nav is-prev" data-preview-direction="-1" aria-label="Xem ảnh/video trước"><i class="bi bi-chevron-left"></i></button>`
      : ""
  }<div class="return-preview-main">${buildReturnPreviewMainHtml(
    activeMedia,
    options
  )}</div>${
    hasMultipleMedia
      ? `<button type="button" class="return-preview-nav is-next" data-preview-direction="1" aria-label="Xem ảnh/video tiếp theo"><i class="bi bi-chevron-right"></i></button>`
      : ""
  }</div>${
    hasMultipleMedia
      ? `<div class="return-preview-thumb-title">${escapeHtml(
          options.thumbTitle
        )}</div><div class="return-preview-thumb-list">${mediaList
          .map((media, index) =>
            buildReturnPreviewThumbHtml(media, index, safeActiveIndex, options)
          )
          .join("")}</div>`
      : ""
  }</div>`;
};

const openMediaPreview = async (
  mediaList: ReturnMediaView[],
  index: number,
  options: MediaPreviewOptions
) => {
  if (mediaList.length === 0) return;
  let activeIndex = Number.isInteger(index) ? index : 0;
  if (activeIndex < 0 || activeIndex >= mediaList.length) activeIndex = 0;
  const renderPreview = () => {
    const htmlContainer = Swal.getHtmlContainer();
    if (!htmlContainer) return;
    htmlContainer.innerHTML = buildReturnPreviewHtml(
      mediaList,
      activeIndex,
      options
    );
    bindPreviewEvents();
  };
  const goToPreview = (nextIndex: number) => {
    activeIndex = (nextIndex + mediaList.length) % mediaList.length;
    renderPreview();
  };
  const bindPreviewEvents = () => {
    const htmlContainer = Swal.getHtmlContainer();
    if (!htmlContainer) return;
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
    title: options.title,
    html: buildReturnPreviewHtml(mediaList, activeIndex, options),
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

const openReturnMediaPreview = async (order: any, index: number) => {
  await openMediaPreview(
    getOrderReturnMedia(order),
    index,
    RETURN_MEDIA_PREVIEW_OPTIONS
  );
};

const getDeliveryMedia = (rawMedia: any): ReturnMediaView[] => {
  const mediaList = Array.isArray(rawMedia)
    ? rawMedia
    : rawMedia
    ? [rawMedia]
    : [];

  return mediaList
    .map((media: any) => {
      const url = getReturnMediaUrl(media);
      if (!url) return null;
      return { url, isVideo: false };
    })
    .filter((media): media is ReturnMediaView => media !== null);
};

const getDeliverySuccessMedia = (order: any): ReturnMediaView[] => {
  return getDeliveryMedia(
    order?.deliverySuccessMediaUrls ??
      order?.deliveryCompletedMediaUrls ??
      order?.deliveryProofUrls ??
      []
  );
};

const getDeliveryFailedMedia = (order: any): ReturnMediaView[] => {
  return getDeliveryMedia(
    order?.deliveryFailedMediaUrls ?? order?.deliveryFailedProofUrls ?? []
  );
};

const openDeliverySuccessMediaPreview = async (order: any, index: number) => {
  await openMediaPreview(
    getDeliverySuccessMedia(order),
    index,
    DELIVERY_SUCCESS_MEDIA_PREVIEW_OPTIONS
  );
};

const openDeliveryFailedMediaPreview = async (order: any, index: number) => {
  await openMediaPreview(
    getDeliveryFailedMedia(order),
    index,
    DELIVERY_FAILED_MEDIA_PREVIEW_OPTIONS
  );
};

const getReviewMediaByOrderItemId = (
  orderItemId: number
): ReturnMediaView[] => {
  const review = getMyReviewByOrderItemId(orderItemId) as any;
  if (!review) return [];
  const rawMedia =
    review?.mediaFiles ??
    review?.mediaUrls ??
    review?.reviewMediaFiles ??
    review?.reviewMediaUrls ??
    review?.images ??
    review?.files ??
    [];
  const mediaList = Array.isArray(rawMedia) ? rawMedia : [rawMedia];
  return mediaList
    .map((media: any) => {
      const url = getReturnMediaUrl(media);
      if (!url) return null;
      return { url, isVideo: isReturnMediaVideo(media, url) };
    })
    .filter((media): media is ReturnMediaView => media !== null);
};

const openReviewMediaPreview = async (orderItemId: number, index: number) => {
  await openMediaPreview(
    getReviewMediaByOrderItemId(orderItemId),
    index,
    REVIEW_MEDIA_PREVIEW_OPTIONS
  );
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
    case 8:
      return "Đã hủy / Chờ hoàn tiền"; // THÊM DÒNG NÀY
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
    case 8:
      return "bg-warning text-dark"; // THÊM DÒNG NÀY
    default:
      return "bg-secondary";
  }
};

const shouldDisplayReturnStatusInHeader = (order: any) => {
  const orderStatus = Number(order?.status);
  const returnStatus = getOrderReturnProcessStatus(order);

  return orderStatus === 6 || orderStatus === 7 || returnStatus === "REJECTED";
};

const getOrderHeaderStatusText = (order: any) => {
  if (shouldDisplayReturnStatusInHeader(order)) {
    return getReturnProcessText(order);
  }

  return order?.statusText || getStatusText(Number(order?.status));
};

const getOrderHeaderStatusClass = (order: any) => {
  if (!shouldDisplayReturnStatusInHeader(order)) {
    return getStatusClass(Number(order?.status));
  }

  switch (getOrderReturnProcessStatus(order)) {
    case "PENDING":
      return "bg-secondary";
    case "ACCEPTED":
      return "bg-info text-dark";
    case "REJECTED":
      return "bg-danger";
    case "REFUNDED":
      return "bg-success";
    case "PARTIAL":
      return "bg-warning text-dark";
    default:
      return getStatusClass(Number(order?.status));
  }
};

const getErrorMessage = (error: any, fallback: string) => {
  const data = error?.response?.data;
  if (typeof data === "string") return data;
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
  if (!isOnlineOrder(order)) {
    toast("warning", "Chức năng hủy đơn tại đây chỉ áp dụng cho đơn Online.");
    return;
  }

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
              `
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
        'input[name="cancelReason"]:checked'
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
      if (
        !(await handleCustomerOrderConflict(
          error,
          order.orderId,
          "Đơn hàng đã được xử lý ở nơi khác. Dữ liệu mới đã được tải lại, vui lòng kiểm tra lại."
        ))
      ) {
        showError(error, "Không thể hủy đơn hàng lúc này. Vui lòng thử lại.");
      }
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
  if (!canRequestReturn(order)) {
    toast(
      "warning",
      getReturnDeadlineText(order) ||
        "Đơn hàng không còn đủ điều kiện hoàn hàng"
    );
    return;
  }

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
    currentTab.value = "RETURN";

    toast("success", "Đã gửi yêu cầu hoàn hàng thành công!");
  } catch (error) {
    const conflicted = await handleCustomerOrderConflict(
      error,
      payload.orderId,
      "Điều kiện hoàn hàng hoặc trạng thái đơn đã thay đổi. Dữ liệu mới đã được tải lại, vui lòng kiểm tra lại."
    );

    if (conflicted) {
      returnModalVisible.value = false;
      selectedReturnOrder.value = null;
    } else {
      showError(error, "Không thể gửi yêu cầu hoàn hàng lúc này.");
    }
  } finally {
    submittingReturn.value = false;
    store.orderLoading = false;
  }
};

const cancelReturnRequest = async (order: CustomerOrderResponse) => {
  if (!isOnlineOrder(order)) {
    toast(
      "warning",
      "Chức năng hủy yêu cầu hoàn hàng tại đây chỉ áp dụng cho đơn Online."
    );
    return;
  }

  const result = await Swal.fire({
    title: "Rút lại yêu cầu?",
    html: `<div class="return-cancel-modal"><div class="return-cancel-alert"><div class="return-cancel-icon"><i class="bi bi-arrow-counterclockwise"></i></div><div class="return-cancel-content"><div class="return-cancel-title">Xác nhận rút lại yêu cầu hoàn hàng</div><div class="return-cancel-desc">Đơn hàng sẽ trở về trạng thái <strong>Hoàn thành</strong>. Sau khi rút lại, bạn cần gửi yêu cầu mới nếu muốn hoàn hàng/đổi trả tiếp.</div></div></div></div>`,
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
      if (
        !(await handleCustomerOrderConflict(
          error,
          order.orderId,
          "Yêu cầu hoàn hàng đã được xử lý hoặc thay đổi ở nơi khác. Dữ liệu mới đã được tải lại, vui lòng kiểm tra lại."
        ))
      ) {
        showError(error, "Không thể thao tác lúc này.");
      }
    } finally {
      store.orderLoading = false;
    }
  }
};

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="100%" height="100%" fill="#f3f4f6"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#9ca3af" font-family="Arial" font-size="20">Không có ảnh</text></svg>`
  );

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
      if (!url && item.productVariant.product.productImages?.length > 0)
        url = item.productVariant.product.productImages[0].imageUrl;
    }
  }
  if (!url && item.product) {
    url = item.product.mainImage || item.product.imageUrl;
    if (!url && item.product.productImages?.length > 0)
      url = item.product.productImages[0].imageUrl;
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

.tracking-delivery-media {
  margin-top: 10px;
  margin-bottom: 0;
  margin-left: 0;
  padding-top: 10px;
  border-top: 1px dashed #dbe3ef;
}

.tracking-delivery-media.is-failed {
  color: #ef4444;
}

.tracking-delivery-media-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 8px;
}

.tracking-delivery-note {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
}

.tracking-delivery-note strong {
  color: #0f172a;
  font-weight: 700;
}

.tracking-delivery-proof {
  margin-top: 10px;
}

@media (max-width: 640px) {
  .tracking-delivery-media {
    margin-left: 0;
  }
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
  cursor: pointer;
  border-radius: 12px;
  transition: background-color 0.18s ease, box-shadow 0.18s ease;
}

.product-block:focus-visible {
  outline: 2px solid #bd9a5f;
  outline-offset: 3px;
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

.order-delivery-refund-info {
  width: 100%;
  min-width: 0;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-left: 4px solid #f97316;
  border-radius: 14px;
  padding: 16px 18px;
}

.order-delivery-refund-info.is-waiting-shop {
  background: #eff6ff;
  border-color: #bfdbfe;
  border-left-color: #2563eb;
}

.order-delivery-refund-info.is-refunded {
  background: #f0fdf4;
  border-color: #bbf7d0;
  border-left-color: #16a34a;
}

.delivery-refund-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.delivery-refund-title {
  color: #9a3412;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.order-delivery-refund-info.is-waiting-shop .delivery-refund-title {
  color: #1d4ed8;
}

.order-delivery-refund-info.is-refunded .delivery-refund-title {
  color: #15803d;
}

.delivery-refund-desc {
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
}

.delivery-refund-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #fed7aa;
  color: #9a3412;
  padding: 4px 9px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.order-delivery-refund-info.is-waiting-shop .delivery-refund-badge {
  background: #dbeafe;
  color: #1d4ed8;
}

.order-delivery-refund-info.is-refunded .delivery-refund-badge {
  background: #dcfce7;
  color: #15803d;
}

.delivery-refund-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

.delivery-refund-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
}

.delivery-refund-line strong {
  color: #0f172a;
  font-weight: 800;
  word-break: break-word;
}

.delivery-refund-money strong {
  color: #dc2626;
}

.delivery-refund-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-start;
}

.delivery-refund-once-note {
  margin-top: 12px;
  color: #475569;
  font-size: 12px;
  line-height: 1.45;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 9px 10px;
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

.return-refund-line {
  margin-top: 8px;
}

.return-refund-line strong {
  color: #9a6a1f;
}

.return-info-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.return-request-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.return-request-meta strong {
  color: #0f172a;
  font-weight: 800;
}

.return-process-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  border: 1px solid transparent;
}

.return-process-badge.is-pending {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.return-process-badge.is-accepted,
.return-process-badge.is-partial {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: #bfdbfe;
}

.return-process-badge.is-rejected {
  color: #b91c1c;
  background: #fee2e2;
  border-color: #fecaca;
}

.return-process-badge.is-refunded {
  color: #047857;
  background: #d1fae5;
  border-color: #a7f3d0;
}
.return-process-badge.is-cancelled {
  color: #475569;
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.return-process-timeline {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.return-process-step {
  position: relative;
  display: flex;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #f3e2bd;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.64);
  color: #64748b;
}

.return-process-step.is-done {
  border-color: #d9c392;
  color: #334155;
}

.return-process-step.is-active {
  background: #ffffff;
  border-color: #bd9a5f;
  box-shadow: 0 8px 16px rgba(189, 154, 95, 0.12);
}

.return-process-step.is-rejected {
  border-color: #fecaca;
  background: #fff1f2;
}

.return-step-marker {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 14px;
}

.return-process-step.is-done .return-step-marker,
.return-process-step.is-active .return-step-marker {
  background: #bd9a5f;
  color: #ffffff;
}

.return-process-step.is-rejected .return-step-marker {
  background: #dc2626;
  color: #ffffff;
}

.return-step-content {
  min-width: 0;
  flex: 1;
}

.return-step-title {
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.3;
}

.return-step-desc {
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.4;
}

.return-step-time {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 700;
}

.return-process-alert {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid transparent;
}

.return-process-alert i {
  font-size: 18px;
  line-height: 1.2;
  flex-shrink: 0;
}

.return-process-alert strong {
  display: block;
  color: #0f172a;
  font-weight: 800;
  margin-bottom: 2px;
}

.return-process-alert p {
  margin: 0;
  color: #475569;
}

.return-process-alert.is-pending {
  background: #fffbeb;
  border-color: #fde68a;
  color: #92400e;
}

.return-process-alert.is-accepted {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.return-process-alert.is-rejected {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}

.return-process-alert.is-refunded {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #047857;
}
.return-process-alert.is-cancelled {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #475569;
}

.return-info-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.return-reject-line strong {
  color: #dc2626;
}

.return-selected-section {
  margin-top: 12px;
}

.return-selected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.return-selected-item {
  width: 100%;
  display: flex;
  gap: 10px;
  padding: 9px;
  border: 1px solid #f3e2bd;
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease,
    transform 0.18s ease;
}

.return-selected-item:hover {
  border-color: #bd9a5f;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.return-selected-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  flex-shrink: 0;
  background: #f8fafc;
}

.return-selected-content {
  min-width: 0;
  flex: 1;
}

.return-selected-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.return-selected-name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.return-selected-name-row .return-selected-name {
  flex: 1;
}

.return-selected-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
  border: 1px solid transparent;
}

.return-selected-status.is-pending {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.return-selected-status.is-accepted,
.return-selected-status.is-partial {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: #bfdbfe;
}

.return-selected-status.is-rejected {
  color: #b91c1c;
  background: #fee2e2;
  border-color: #fecaca;
}

.return-selected-status.is-refunded {
  color: #047857;
  background: #d1fae5;
  border-color: #a7f3d0;
}
.return-selected-status.is-cancelled {
  color: #475569;
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.return-item-reject-note {
  margin-top: 6px;
  padding: 6px 8px;
  border-radius: 8px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
}

.return-selected-meta,
.return-selected-bottom {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

.return-selected-bottom strong {
  color: #0f172a;
  font-weight: 800;
}

.return-selected-discount strong {
  color: #dc2626;
}

.return-selected-refund strong {
  color: #9a6a1f;
}

.return-selected-empty {
  padding: 10px 12px;
  border: 1px dashed #f3e2bd;
  border-radius: 12px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.58);
  font-size: 12px;
  font-weight: 700;
}

.delivery-info-box {
  background: #fffdf8;
  border: 1px solid #f3e2bd;
  border-radius: 14px;
  padding: 16px;
}

.delivery-info-title {
  color: #111827;
  font-weight: 800;
  margin-bottom: 12px;
}

.delivery-info-card {
  background: #ffffff;
  border: 1px solid #edf0f3;
  border-radius: 12px;
  padding: 12px;
}

.delivery-info-card + .delivery-info-card {
  margin-top: 12px;
}

.delivery-info-card.is-failed {
  border-color: #fecaca;
}

.delivery-info-head {
  font-weight: 800;
  margin-bottom: 10px;
}

.delivery-info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
}

.delivery-info-row span {
  color: #64748b;
}

.delivery-info-row strong {
  color: #111827;
  text-align: right;
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
  transition: transform 0.18s ease, box-shadow 0.18s ease,
    border-color 0.18s ease;
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

.review-approval-status {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 12px;
  font-weight: 700;
}

.review-approval-status.is-pending {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
}

.review-approval-status.is-approved {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.review-approval-status.is-rejected,
.review-approval-status.is-hidden {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.review-reject-reason {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.45;
}

.review-reject-reason span {
  font-weight: 700;
}

.review-reject-reason strong {
  font-weight: 600;
}

.review-edit-hint {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.review-edit-btn {
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
}

.review-media-section {
  margin-top: 9px;
}

.review-media-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 7px;
}

.review-media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-media-button {
  position: relative;
  width: 58px;
  height: 58px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #f3e2bd;
  border-radius: 10px;
  background: #ffffff;
  cursor: zoom-in;
  transition: border-color 0.18s ease, box-shadow 0.18s ease,
    transform 0.18s ease;
}

.review-media-button:hover {
  border-color: #bd9a5f;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
  transform: translateY(-1px);
}

.review-media-thumb {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  background: #ffffff;
}

.review-media-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 18px;
  background: rgba(15, 23, 42, 0.28);
  opacity: 0;
  transition: opacity 0.18s ease;
}

.review-media-button:hover .review-media-overlay {
  opacity: 1;
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
  .order-delivery-refund-info,
  .order-total-box {
    width: 100%;
    max-width: 100%;
  }

  .delivery-refund-grid {
    grid-template-columns: 1fr;
  }

  .delivery-refund-top {
    flex-direction: column;
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
  transition: border-color 0.18s ease, background-color 0.18s ease,
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
  transition: background-color 0.18s ease, border-color 0.18s ease,
    color 0.18s ease, transform 0.18s ease;
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
  transition: border-color 0.18s ease, box-shadow 0.18s ease,
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

@media (max-width: 991.98px) {
  .return-info-top {
    flex-direction: column;
    align-items: stretch;
  }

  .return-process-badge {
    align-self: flex-start;
  }

  .return-process-timeline {
    grid-template-columns: 1fr;
  }
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

.delivery-refund-swal {
  width: min(620px, calc(100vw - 28px)) !important;
}

.delivery-refund-modal {
  padding: 0 8px 4px;
}

.delivery-refund-modal-alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 14px;
  font-size: 13px;
  line-height: 1.45;
}

.delivery-refund-modal-label {
  display: block;
  margin: 12px 0 6px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.delivery-refund-modal-label span {
  color: #dc2626;
}

.delivery-refund-modal-control {
  display: block !important;
  width: 100% !important;
  margin: 0 !important;
  border: 1px solid #cbd5e1 !important;
  border-radius: 9px !important;
  box-shadow: none !important;
  font-size: 14px !important;
}

select.delivery-refund-modal-control {
  height: 42px !important;
  padding: 0 10px !important;
}

input.delivery-refund-modal-control {
  height: 42px !important;
  padding: 0 10px !important;
}

.delivery-refund-modal-help {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.delivery-refund-bank-picker {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.delivery-refund-bank-search-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 10px;
  border-bottom: 1px solid #e2e8f0;
  color: #64748b;
}

.delivery-refund-bank-search {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  box-shadow: none;
  font-size: 14px;
  color: #0f172a;
  background: transparent;
}

.delivery-refund-bank-list {
  max-height: 260px;
  overflow-y: auto;
  padding: 6px;
}

.delivery-refund-bank-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: 0;
  border-radius: 9px;
  background: transparent;
  padding: 8px;
  text-align: left;
  color: #0f172a;
  cursor: pointer;
}

.delivery-refund-bank-option:hover,
.delivery-refund-bank-option.is-selected {
  background: #f8fafc;
}

.delivery-refund-bank-option.is-selected {
  outline: 1px solid #bd9a5f;
}

.delivery-refund-bank-logo {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  object-fit: contain;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 3px;
}

.delivery-refund-bank-logo.is-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.delivery-refund-bank-content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.delivery-refund-bank-content strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delivery-refund-bank-content small {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delivery-refund-bank-option > .bi-check-lg {
  color: #16a34a;
  font-size: 16px;
}

.delivery-refund-bank-empty {
  padding: 18px 10px;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}
</style>