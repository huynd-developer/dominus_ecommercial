<template>
  <div v-if="show" class="modal d-block" tabindex="-1">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
      <div class="modal-content">
        <div class="modal-header">
          <div>
            <h5 class="modal-title fw-bold mb-1">
              Chi tiết đơn hàng {{ order?.orderCode }}
            </h5>
            <small class="text-muted"> Mã đơn #{{ order?.orderId }} </small>
          </div>

          <button
            type="button"
            class="btn-close"
            @click="$emit('close')"
          ></button>
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
                    <strong>Thanh toán: </strong>
                    <span class="payment-method-text">
                      {{ formatPaymentMethod(order.paymentMethod) }}
                    </span>
                  </p>
                  <p class="mb-1">
                    <strong>Trạng thái: </strong>
                    <OrderStatusBadge
                      :status="order.status"
                      :status-text="order.statusText"
                    />
                  </p>
                  <p class="mb-1">
                    <strong>Ngày tạo:</strong>
                    {{ formatDate(order.createdAt) }}
                  </p>

                  <div
                    v-if="isCancelledOrder(order)"
                    class="cancel-reason-box mt-3"
                  >
                    <div class="cancel-reason-title">
                      <i class="bi bi-x-circle me-1"></i>
                      Lý do hủy đơn
                    </div>

                    <div class="cancel-reason-text">
                      {{ getOrderCancelReason(order) }}
                    </div>

                    <div
                      v-if="getOrderCancelledAt(order)"
                      class="cancel-reason-time"
                    >
                      Hủy lúc: {{ formatDate(getOrderCancelledAt(order)) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-if="hasDeliveryInfo(order)"
              class="delivery-section border rounded p-3 mb-3"
            >
              <div
                class="d-flex align-items-center justify-content-between gap-2 mb-3"
              >
                <div>
                  <h6 class="fw-bold mb-1">Thông tin giao hàng</h6>
                  <small class="text-muted">
                    Kết quả giao hàng và minh chứng do cửa hàng tự vận chuyển
                    cập nhật.
                  </small>
                </div>
              </div>

              <div class="row g-3">
                <div v-if="hasDeliveryCompletedInfo(order)" class="col-md-6">
                  <div class="delivery-info-card h-100">
                    <div class="delivery-info-title text-success">
                      <i class="bi bi-check-circle me-1"></i>
                      Giao hàng thành công
                    </div>

                    <div class="delivery-info-row">
                      <span>Thời gian hoàn thành:</span>
                      <strong>{{ formatDate(order.completedAt) }}</strong>
                    </div>

                    <div
                      v-if="hasDeliveryActorInfo(order.deliveryCompletedByName)"
                      class="delivery-info-row delivery-actor-row"
                    >
                      <span>Người xác nhận:</span>
                      <div class="delivery-actor-value">
                        <strong>{{
                          getDeliveryActorName(order.deliveryCompletedByName)
                        }}</strong>
                        <small
                          v-if="
                            getDeliveryActorEmail(order.deliveryCompletedByName)
                          "
                        >
                          {{
                            getDeliveryActorEmail(order.deliveryCompletedByName)
                          }}
                        </small>
                      </div>
                    </div>

                    <div
                      v-if="getDeliverySuccessMediaList(order).length > 0"
                      class="mt-2"
                    >
                      <div class="delivery-media-label">
                        Minh chứng giao hàng:
                      </div>
                      <div class="return-media-list">
                        <button
                          v-for="(
                            mediaUrl, index
                          ) in getDeliverySuccessMediaList(order)"
                          :key="`delivery-success-${mediaUrl}-${index}`"
                          type="button"
                          class="return-media-item"
                          @click="openMedia(mediaUrl)"
                        >
                          <video
                            v-if="isVideoUrl(mediaUrl)"
                            :src="normalizeImageUrl(mediaUrl)"
                            muted
                            playsinline
                            preload="metadata"
                          ></video>

                          <img
                            v-else
                            :src="normalizeImageUrl(mediaUrl)"
                            alt="Minh chứng giao hàng thành công"
                            @error="handleImageError"
                          />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="hasDeliveryFailedInfo(order)" class="col-md-6">
                  <div class="delivery-info-card h-100">
                    <div class="delivery-info-title text-danger">
                      <i class="bi bi-x-circle me-1"></i>
                      Giao hàng thất bại
                    </div>

                    <div class="delivery-info-row">
                      <span>Lý do:</span>
                      <strong>{{ order.deliveryFailedReason || "-" }}</strong>
                    </div>

                    <div class="delivery-info-row">
                      <span>Mô tả:</span>
                      <strong>{{
                        order.deliveryFailedDescription || "-"
                      }}</strong>
                    </div>

                    <div class="delivery-info-row">
                      <span>Thời gian:</span>
                      <strong>{{ formatDate(order.deliveryFailedAt) }}</strong>
                    </div>

                    <div
                      v-if="hasDeliveryActorInfo(order.deliveryFailedByName)"
                      class="delivery-info-row delivery-actor-row"
                    >
                      <span>Người xác nhận:</span>
                      <div class="delivery-actor-value">
                        <strong>{{
                          getDeliveryActorName(order.deliveryFailedByName)
                        }}</strong>
                        <small
                          v-if="
                            getDeliveryActorEmail(order.deliveryFailedByName)
                          "
                        >
                          {{
                            getDeliveryActorEmail(order.deliveryFailedByName)
                          }}
                        </small>
                      </div>
                    </div>

                    <div
                      v-if="getDeliveryFailedMediaList(order).length > 0"
                      class="mt-2"
                    >
                      <div class="delivery-media-label">
                        Minh chứng giao thất bại:
                      </div>
                      <div class="return-media-list">
                        <button
                          v-for="(
                            mediaUrl, index
                          ) in getDeliveryFailedMediaList(order)"
                          :key="`delivery-failed-${mediaUrl}-${index}`"
                          type="button"
                          class="return-media-item"
                          @click="openMedia(mediaUrl)"
                        >
                          <video
                            v-if="isVideoUrl(mediaUrl)"
                            :src="normalizeImageUrl(mediaUrl)"
                            muted
                            playsinline
                            preload="metadata"
                          ></video>

                          <img
                            v-else
                            :src="normalizeImageUrl(mediaUrl)"
                            alt="Minh chứng giao hàng thất bại"
                            @error="handleImageError"
                          />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-if="hasDeliveryRefundInfo(order)"
              class="delivery-refund-section border rounded p-3 mb-3"
            >
              <div
                class="d-flex align-items-center justify-content-between gap-2 mb-3"
              >
                <div>
                  <h6 class="fw-bold mb-1">
                    <i class="bi bi-cash-coin me-1"></i>
                    {{
                      order.status === 8 || order.status === 4
                        ? "Thông tin hoàn tiền (Hủy)"
                        : "Hoàn tiền giao hàng thất bại"
                    }}
                  </h6>
                  <small class="text-muted">
                    Chỉ áp dụng cho đơn đã thanh toán trước nhưng giao hàng thất
                    bại hoặc hủy đơn.
                  </small>
                </div>

                <span
                  class="delivery-refund-badge"
                  :class="getDeliveryRefundBadgeClass(order)"
                >
                  {{ getDeliveryRefundStatusText(order) }}
                </span>
              </div>

              <div class="row g-3">
                <div class="col-md-6">
                  <div class="delivery-refund-card h-100">
                    <div class="delivery-refund-row refund-money-row">
                      <span>Số tiền cần hoàn:</span>
                      <strong>{{
                        formatMoney(getDeliveryRefundAmount(order))
                      }}</strong>
                    </div>

                    <div class="delivery-refund-row">
                      <span>Phương thức thanh toán:</span>
                      <strong>{{
                        formatPaymentMethod(order.paymentMethod)
                      }}</strong>
                    </div>

                    <div
                      v-if="order.deliveryRefundedAt"
                      class="delivery-refund-row"
                    >
                      <span>Thời gian hoàn:</span>
                      <strong>{{
                        formatDate(order.deliveryRefundedAt)
                      }}</strong>
                    </div>

                    <div
                      v-if="hasDeliveryActorInfo(order.deliveryRefundedByName)"
                      class="delivery-refund-row delivery-actor-row"
                    >
                      <span>Người xác nhận:</span>
                      <div class="delivery-actor-value">
                        <strong>{{
                          getDeliveryActorName(order.deliveryRefundedByName)
                        }}</strong>
                        <small
                          v-if="
                            getDeliveryActorEmail(order.deliveryRefundedByName)
                          "
                        >
                          {{
                            getDeliveryActorEmail(order.deliveryRefundedByName)
                          }}
                        </small>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="col-md-6">
                  <div class="delivery-refund-card h-100">
                    <h6 class="fw-bold mb-2">Tài khoản khách nhận hoàn tiền</h6>

                    <template v-if="hasDeliveryRefundBankInfo(order)">
                      <div class="delivery-refund-row">
                        <span>Ngân hàng:</span>
                        <strong>{{
                          order.deliveryRefundBankName || "-"
                        }}</strong>
                      </div>

                      <div class="delivery-refund-row">
                        <span>Số tài khoản:</span>
                        <strong>{{
                          order.deliveryRefundBankAccountNumber || "-"
                        }}</strong>
                      </div>

                      <div class="delivery-refund-row">
                        <span>Chủ tài khoản:</span>
                        <strong>{{
                          order.deliveryRefundBankAccountHolder || "-"
                        }}</strong>
                      </div>
                    </template>

                    <div v-else class="delivery-refund-empty">
                      Khách chưa cung cấp thông tin tài khoản hoàn tiền.
                    </div>
                  </div>
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
                    <th class="text-end">Giá</th>
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
                        <div class="order-product-image-box">
                          <img
                            :src="getOrderItemImageUrl(item)"
                            :alt="item.productName || 'product'"
                            class="order-product-image"
                            @error="handleImageError"
                          />
                        </div>

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
                    <td class="text-end">
                      <div class="price-stack">
                        <span
                          v-if="hasOrderItemDiscount(item)"
                          class="old-price"
                        >
                          {{ formatMoney(getOrderItemOriginalPrice(item)) }}
                        </span>
                        <span class="final-price">
                          {{ formatMoney(getOrderItemFinalPrice(item)) }}
                        </span>
                      </div>
                    </td>
                    <td class="text-end fw-semibold">
                      {{ formatMoney(item.lineTotal) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-if="hasReturnInfo(order)" class="return-section mt-3">
              <div
                class="return-header d-flex align-items-center justify-content-between gap-2 mb-3"
              >
                <div>
                  <h6 class="fw-bold mb-1">Thông tin hoàn hàng / hoàn tiền</h6>
                  <small class="text-muted">
                    Sản phẩm khách yêu cầu hoàn và thông tin nhận hoàn tiền.
                  </small>
                </div>

                <span
                  class="return-badge"
                  :class="getReturnProcessBadgeClass(order)"
                >
                  {{ getReturnProcessStatusText(order) }}
                </span>
              </div>

              <div class="row g-3 mb-3">
                <div
                  :class="
                    shouldShowBankRefundInfo(order) ? 'col-md-7' : 'col-12'
                  "
                >
                  <div class="return-info-card h-100">
                    <div class="return-info-row">
                      <span>Lý do hoàn:</span>
                      <strong>{{ order.returnReason || "-" }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Mô tả:</span>
                      <strong>{{ order.returnDescription || "-" }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Email liên hệ:</span>
                      <strong>{{ order.returnEmail || "-" }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Ngày yêu cầu:</span>
                      <strong>{{ formatDate(order.returnRequestedAt) }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Trạng thái xử lý:</span>
                      <strong>{{ getReturnProcessStatusText(order) }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Phương án hoàn tiền:</span>
                      <strong>{{
                        formatRefundMethod(order.refundMethod)
                      }}</strong>
                    </div>

                    <div
                      v-if="getReturnProductRefundAmount(order) > 0"
                      class="return-info-row"
                    >
                      <span>Tiền hàng hoàn:</span>
                      <strong>{{
                        formatMoney(getReturnProductRefundAmount(order))
                      }}</strong>
                    </div>

                    <div
                      v-if="getReturnShippingFee(order) > 0"
                      class="return-info-row"
                    >
                      <span>Phí vận chuyển hoàn:</span>
                      <strong>{{
                        formatMoney(getReturnShippingFee(order))
                      }}</strong>
                    </div>

                    <div class="return-info-row return-money-row">
                      <span>Số tiền cần hoàn:</span>
                      <strong>{{
                        formatMoney(getReturnRefundAmount(order))
                      }}</strong>
                    </div>

                    <div
                      v-if="getOrderReturnRejectReason(order)"
                      class="return-reject-reason mt-2"
                    >
                      <strong>Lý do từ chối:</strong>
                      {{ getOrderReturnRejectReason(order) }}
                    </div>
                  </div>
                </div>

                <div v-if="shouldShowBankRefundInfo(order)" class="col-md-5">
                  <div class="return-info-card h-100">
                    <h6 class="fw-bold mb-2">Thông tin tài khoản nhận tiền</h6>

                    <div class="return-info-row">
                      <span>Ngân hàng:</span>
                      <strong>{{ order.bankName || "-" }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Số tài khoản:</span>
                      <strong>{{ order.bankAccountNumber || "-" }}</strong>
                    </div>

                    <div class="return-info-row">
                      <span>Chủ tài khoản:</span>
                      <strong>{{ order.bankAccountHolder || "-" }}</strong>
                    </div>
                  </div>
                </div>
              </div>

              <div
                v-if="getReturnMediaList(order).length > 0"
                class="return-media-list mb-3"
              >
                <button
                  v-for="(mediaUrl, index) in getReturnMediaList(order)"
                  :key="`${mediaUrl}-${index}`"
                  type="button"
                  class="return-media-item"
                  @click="openMedia(mediaUrl)"
                >
                  <video
                    v-if="isVideoUrl(mediaUrl)"
                    :src="normalizeImageUrl(mediaUrl)"
                    muted
                    playsinline
                    preload="metadata"
                  ></video>

                  <img
                    v-else
                    :src="normalizeImageUrl(mediaUrl)"
                    alt="Bằng chứng hoàn hàng"
                    @error="handleImageError"
                  />
                </button>
              </div>

              <div class="table-responsive border rounded">
                <table class="table align-middle mb-0">
                  <thead class="table-light">
                    <tr>
                      <th>Sản phẩm hoàn</th>
                      <th>SKU</th>
                      <th>Dung tích</th>
                      <th>Loại chai</th>
                      <th class="text-end">SL mua</th>
                      <th class="text-end">SL hoàn</th>
                      <th class="text-end">Giá</th>
                      <th class="text-end">Tiền hoàn</th>
                      <th>Trạng thái</th>
                    </tr>
                  </thead>

                  <tbody>
                    <tr
                      v-if="
                        !order.returnItems || order.returnItems.length === 0
                      "
                    >
                      <td colspan="9" class="text-center text-muted py-4">
                        Chưa có dữ liệu sản phẩm hoàn.
                      </td>
                    </tr>

                    <tr
                      v-for="(item, index) in order.returnItems"
                      :key="`return-item-${
                        item.returnRequestItemId ?? item.orderItemId ?? index
                      }`"
                    >
                      <td>
                        <div class="d-flex align-items-center gap-2">
                          <div class="order-product-image-box">
                            <img
                              :src="getReturnItemImageUrl(item)"
                              :alt="item.productName || 'return product'"
                              class="order-product-image"
                              @error="handleImageError"
                            />
                          </div>

                          <div>
                            <div class="fw-semibold">
                              {{ item.productName || "Sản phẩm" }}
                            </div>
                            <small v-if="item.brandName" class="text-muted">
                              {{ item.brandName }}
                            </small>
                          </div>
                        </div>
                      </td>

                      <td>{{ item.sku || "-" }}</td>
                      <td>{{ item.capacity || "-" }}</td>
                      <td>{{ item.bottleType || "-" }}</td>
                      <td class="text-end">
                        {{ item.orderedQuantity ?? "-" }}
                      </td>
                      <td class="text-end fw-semibold">
                        {{ item.returnQuantity ?? 0 }}
                      </td>
                      <td class="text-end">
                        <div class="price-stack">
                          <span
                            v-if="hasReturnItemDiscount(item)"
                            class="old-price"
                          >
                            {{ formatMoney(getReturnItemOriginalPrice(item)) }}
                          </span>
                          <span class="final-price">
                            {{ formatMoney(getReturnItemFinalPrice(item)) }}
                          </span>
                        </div>
                      </td>
                      <td class="text-end fw-semibold text-danger">
                        {{ formatMoney(item.refundAmount) }}
                      </td>
                      <td>
                        <span
                          class="return-item-status"
                          :class="getReturnItemStatusClass(item.status)"
                        >
                          {{
                            item.statusText ||
                            formatReturnItemStatus(item.status)
                          }}
                        </span>

                        <div
                          v-if="getReturnItemRejectReason(item)"
                          class="return-item-reject-reason mt-1"
                        >
                          Lý do: {{ getReturnItemRejectReason(item) }}
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="row justify-content-end mt-3">
              <div class="col-md-4">
                <div class="border rounded p-3">
                  <div class="d-flex justify-content-between mb-2">
                    <span>Tổng tiền hàng:</span>
                    <strong>{{ formatMoney(order.totalAmount) }}</strong>
                  </div>

                  <div
                    v-if="getOrderShippingFee(order) > 0"
                    class="d-flex justify-content-between mb-2"
                  >
                    <span>Phí vận chuyển:</span>
                    <strong>{{
                      formatMoney(getOrderShippingFee(order))
                    }}</strong>
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
                </div>
              </div>
            </div>
          </template>
        </div>

        <div class="modal-footer">
          <button
            v-if="order && canAcceptReturn(order)"
            class="btn btn-success"
            type="button"
            @click="$emit('accept-return', order)"
          >
            <i class="bi bi-check-circle me-1"></i>
            Chấp nhận hoàn
          </button>

          <button
            v-if="order && canRejectReturn(order)"
            class="btn btn-outline-danger"
            type="button"
            @click="$emit('reject-return', order)"
          >
            <i class="bi bi-x-circle me-1"></i>
            Từ chối
          </button>

          <button
            v-if="order && canMarkDeliveryRefunded(order)"
            class="btn btn-success"
            type="button"
            @click="$emit('mark-delivery-refunded', order)"
          >
            <i class="bi bi-cash-coin me-1"></i>
            Đã chuyển tiền
          </button>

          <!-- NÚT MỚI: XÁC NHẬN HOÀN TIỀN KHI HỦY -->
          <button
            v-if="order && order.status === 8"
            class="btn btn-success"
            type="button"
            @click="$emit('mark-cancel-refunded', order)"
          >
            <i class="bi bi-check-circle me-1"></i>
            Đã hoàn tiền
          </button>

          <button
            v-if="order && canMarkReturnRefunded(order)"
            class="btn btn-success"
            type="button"
            @click="$emit('mark-return-refunded', order)"
          >
            <i class="bi bi-cash-coin me-1"></i>
            Đã hoàn tiền
          </button>

          <button class="btn btn-secondary" @click="$emit('close')">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>

  <div v-if="selectedMediaUrl" class="return-media-modal" @click="closeMedia">
    <div class="return-media-modal-content" @click.stop>
      <button type="button" class="btn-close-media" @click="closeMedia">
        ×
      </button>

      <video
        v-if="isVideoUrl(selectedMediaUrl)"
        :src="normalizeImageUrl(selectedMediaUrl)"
        controls
        autoplay
      ></video>

      <img
        v-else
        :src="normalizeImageUrl(selectedMediaUrl)"
        alt="Bằng chứng hoàn hàng"
      />
    </div>
  </div>

  <div v-if="show" class="modal-backdrop show"></div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type {
  AdminOrderItemResponse,
  AdminOrderResponse,
  AdminReturnItemResponse,
} from "../types/order.type";
import OrderStatusBadge from "./OrderStatusBadge.vue";

const props = defineProps<{
  show: boolean;
  order: AdminOrderResponse | null;
}>();

const emit = defineEmits<{
  close: [];
  "accept-return": [order: AdminOrderResponse];
  "reject-return": [order: AdminOrderResponse];
  "mark-return-refunded": [order: AdminOrderResponse];
  "mark-delivery-refunded": [order: AdminOrderResponse];
  "mark-cancel-refunded": [order: AdminOrderResponse]; // BỔ SUNG KHAI BÁO EVENT
}>();

void props;

const BACKEND_URL = "http://localhost:8080";

const selectedMediaUrl = ref<string | null>(null);

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

type DeliveryActorInfo = {
  name: string;
  email: string | null;
};

function normalizeActorText(value?: string | null) {
  return String(value || "")
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .trim();
}

function extractEmail(value: string) {
  const match = value.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i);
  return match ? match[0] : null;
}

function parseDeliveryActorInfo(value?: string | null): DeliveryActorInfo {
  const cleanValue = normalizeActorText(value);

  if (!cleanValue) {
    return { name: "", email: null };
  }

  const lines = cleanValue
    .split("\n")
    .map((line) => line.trim())
    .filter((line): line is string => line.length > 0);

  if (lines.length >= 2) {
    const emailIndex = lines.findIndex((line) => Boolean(extractEmail(line)));

    if (emailIndex >= 0) {
      const emailLine = lines[emailIndex] || "";
      const email = extractEmail(emailLine);
      const name = lines
        .filter((_, index) => index !== emailIndex)
        .join(" ")
        .trim();

      return {
        name: name || email || "Nhân viên cửa hàng",
        email: email && email !== name ? email : null,
      };
    }

    const firstLine = lines[0] || "Nhân viên cửa hàng";
    const secondLine = lines[1] || null;

    return {
      name: firstLine,
      email: secondLine,
    };
  }

  const email = extractEmail(cleanValue);

  if (!email) {
    return { name: cleanValue, email: null };
  }

  const name = cleanValue
    .replace(email, " ")
    .replace(/[<>()\[\]]/g, " ")
    .replace(/[|/\-–—]+/g, " ")
    .replace(/\s{2,}/g, " ")
    .trim();

  return {
    name: name || "Nhân viên cửa hàng",
    email,
  };
}

function hasDeliveryActorInfo(value?: string | null) {
  return Boolean(parseDeliveryActorInfo(value).name);
}

function getDeliveryActorName(value?: string | null) {
  return parseDeliveryActorInfo(value).name || "-";
}

function getDeliveryActorEmail(value?: string | null) {
  return parseDeliveryActorInfo(value).email;
}

function normalizeImageUrl(value?: string | null) {
  if (!value || !value.trim()) return "";

  const cleanValue = value.trim();

  if (
    cleanValue.startsWith("http://") ||
    cleanValue.startsWith("https://") ||
    cleanValue.startsWith("data:image") ||
    cleanValue.startsWith("blob:")
  ) {
    return cleanValue;
  }

  if (cleanValue.startsWith("/")) {
    return `${BACKEND_URL}${cleanValue}`;
  }

  return `${BACKEND_URL}/${cleanValue}`;
}

function getImageUrlFromObject(value: any) {
  if (!value) return "";

  if (typeof value === "string") {
    return normalizeImageUrl(value);
  }

  return normalizeImageUrl(
    value.imageUrl ??
      value.ImageUrl ??
      value.productImageUrl ??
      value.ProductImageUrl ??
      value.variantImageUrl ??
      value.VariantImageUrl ??
      value.thumbnailUrl ??
      value.ThumbnailUrl ??
      value.mainImageUrl ??
      value.MainImageUrl ??
      value.image ??
      value.Image ??
      value.url ??
      value.Url ??
      value.path ??
      value.Path ??
      value.fileUrl ??
      value.FileUrl ??
      value.mediaUrl ??
      value.MediaUrl ??
      ""
  );
}

function getFirstImageFromList(value: any) {
  if (!Array.isArray(value)) return "";

  for (const item of value) {
    const imageUrl = getImageUrlFromObject(item);

    if (imageUrl) {
      return imageUrl;
    }
  }

  return "";
}

function getOrderItemImageUrl(item: AdminOrderItemResponse) {
  const directImage =
    getImageUrlFromObject(item.imageUrl) ||
    getImageUrlFromObject(item.ImageUrl) ||
    getImageUrlFromObject(item.productImageUrl) ||
    getImageUrlFromObject(item.ProductImageUrl) ||
    getImageUrlFromObject(item.variantImageUrl) ||
    getImageUrlFromObject(item.VariantImageUrl) ||
    getImageUrlFromObject(item.thumbnailUrl) ||
    getImageUrlFromObject(item.ThumbnailUrl) ||
    getImageUrlFromObject(item.mainImageUrl) ||
    getImageUrlFromObject(item.MainImageUrl) ||
    getImageUrlFromObject(item.image) ||
    getImageUrlFromObject(item.Image) ||
    getImageUrlFromObject(item.productImage) ||
    getImageUrlFromObject(item.ProductImage);

  if (directImage) {
    return directImage;
  }

  const listImage =
    getFirstImageFromList(item.images) ||
    getFirstImageFromList(item.Images) ||
    getFirstImageFromList(item.productImages) ||
    getFirstImageFromList(item.ProductImages) ||
    getFirstImageFromList(item.imageList) ||
    getFirstImageFromList(item.ImageList);

  return listImage || FALLBACK_IMAGE;
}

function getReturnItemImageUrl(item: AdminReturnItemResponse) {
  return getImageUrlFromObject(item.imageUrl) || FALLBACK_IMAGE;
}

function toMoneyNumber(value?: number | null) {
  const numberValue = Number(value || 0);

  return Number.isFinite(numberValue) ? numberValue : 0;
}

function pickMoneyValue(...values: unknown[]) {
  for (const value of values) {
    const numberValue = Number(value || 0);

    if (Number.isFinite(numberValue) && numberValue > 0) {
      return numberValue;
    }
  }

  return 0;
}

function getOrderShippingFee(order?: AdminOrderResponse | null) {
  return pickMoneyValue(
    (order as any)?.shippingFee,
    (order as any)?.shippingfee,
    (order as any)?.shippingFeeAmount,
    (order as any)?.shipFee,
    (order as any)?.deliveryFee,
    (order as any)?.shippingAmount
  );
}

function getReturnShippingFee(order?: AdminOrderResponse | null) {
  return pickMoneyValue(
    (order as any)?.returnShippingFee,
    (order as any)?.refundShippingFee,
    (order as any)?.shippingFeeRefundAmount
  );
}

function getReturnProductRefundAmount(order: AdminOrderResponse) {
  return Math.max(
    0,
    getReturnRefundAmount(order) - getReturnShippingFee(order)
  );
}

function getOrderItemOriginalPrice(item: AdminOrderItemResponse) {
  const originalPrice = toMoneyNumber(item.originalPrice);
  const finalPrice = toMoneyNumber(item.finalPrice);
  const discountAmount = toMoneyNumber(item.discountAmount);

  return originalPrice > 0 ? originalPrice : finalPrice + discountAmount;
}

function getOrderItemFinalPrice(item: AdminOrderItemResponse) {
  return toMoneyNumber(item.finalPrice);
}

function hasOrderItemDiscount(item: AdminOrderItemResponse) {
  return (
    toMoneyNumber(item.discountAmount) > 0 ||
    getOrderItemOriginalPrice(item) > getOrderItemFinalPrice(item)
  );
}

function getReturnItemOriginalPrice(item: AdminReturnItemResponse) {
  const originalPrice = toMoneyNumber(item.unitOriginalPrice);
  const finalPrice = toMoneyNumber(item.unitFinalPrice);
  const discountAmount = toMoneyNumber(item.unitDiscountAmount);

  return originalPrice > 0 ? originalPrice : finalPrice + discountAmount;
}

function getReturnItemFinalPrice(item: AdminReturnItemResponse) {
  return toMoneyNumber(item.unitFinalPrice);
}

function hasReturnItemDiscount(item: AdminReturnItemResponse) {
  return (
    toMoneyNumber(item.unitDiscountAmount) > 0 ||
    getReturnItemOriginalPrice(item) > getReturnItemFinalPrice(item)
  );
}

function getReturnRefundAmount(order: AdminOrderResponse) {
  return Number(order.returnRefundAmount ?? order.refundAmount ?? 0);
}

function getDeliverySuccessMediaList(order?: AdminOrderResponse | null) {
  return ((order as any)?.deliverySuccessMediaUrls || []).filter(
    (url: string) => Boolean(url && String(url).trim())
  );
}

function getDeliveryFailedMediaList(order?: AdminOrderResponse | null) {
  return ((order as any)?.deliveryFailedMediaUrls || []).filter((url: string) =>
    Boolean(url && String(url).trim())
  );
}

function hasDeliveryCompletedInfo(order?: AdminOrderResponse | null) {
  return Boolean(
    Number(order?.status) === 3 ||
      order?.completedAt ||
      (order as any)?.deliveryCompletedByName ||
      getDeliverySuccessMediaList(order).length > 0
  );
}

function hasDeliveryFailedInfo(order?: AdminOrderResponse | null) {
  return Boolean(
    Number(order?.status) === 5 ||
      (order as any)?.deliveryFailedReason ||
      (order as any)?.deliveryFailedDescription ||
      (order as any)?.deliveryFailedAt ||
      (order as any)?.deliveryFailedByName ||
      getDeliveryFailedMediaList(order).length > 0
  );
}

function hasDeliveryInfo(order?: AdminOrderResponse | null) {
  return hasDeliveryCompletedInfo(order) || hasDeliveryFailedInfo(order);
}

function hasReturnInfo(order: AdminOrderResponse) {
  return (
    Number(order.status) === 6 ||
    Number(order.status) === 7 ||
    Boolean(order.returnReason) ||
    Boolean(order.returnDescription) ||
    Boolean(order.returnRequestedAt) ||
    Boolean(order.bankAccountNumber) ||
    Boolean(order.returnItems && order.returnItems.length > 0)
  );
}

function normalizeRefundMethodValue(method?: string | number | null) {
  const value = String(method || "")
    .trim()
    .toUpperCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

  if (!value) {
    return "";
  }

  if (
    value === "1" ||
    value === "BANK_TRANSFER" ||
    value.includes("BANK") ||
    value.includes("TRANSFER") ||
    value.includes("CHUYEN KHOAN") ||
    value.includes("NGAN HANG")
  ) {
    return "BANK_TRANSFER";
  }

  if (
    value === "2" ||
    value === "STORE" ||
    value.includes("CUA HANG") ||
    value.includes("TAI QUAY")
  ) {
    return "STORE";
  }

  return value;
}

function shouldShowBankRefundInfo(order: AdminOrderResponse | null) {
  if (!order) {
    return false;
  }

  const refundMethod = normalizeRefundMethodValue(order.refundMethod);

  if (refundMethod === "STORE") {
    return false;
  }

  if (refundMethod === "BANK_TRANSFER") {
    return true;
  }

  return Boolean(
    order.bankName || order.bankAccountNumber || order.bankAccountHolder
  );
}

function getReturnProcessStatus(order: AdminOrderResponse) {
  const directStatus = Number(order.returnProcessStatus);

  if (Number.isFinite(directStatus)) {
    return directStatus;
  }

  const itemStatuses = (order.returnItems || [])
    .map((item) => Number(item.status))
    .filter((value) => Number.isFinite(value));

  if (Number(order.status) === 7 || itemStatuses.some((value) => value === 3)) {
    return 3;
  }

  if (itemStatuses.length > 0 && itemStatuses.every((value) => value === 2)) {
    return 2;
  }

  if (itemStatuses.length > 0 && itemStatuses.every((value) => value === 1)) {
    return 1;
  }

  if (Number(order.status) === 6) {
    return 0;
  }

  return null;
}

function getReturnProcessStatusText(order: AdminOrderResponse) {
  if (order.returnProcessStatusText) {
    return order.returnProcessStatusText;
  }

  switch (getReturnProcessStatus(order)) {
    case 0:
      return "Chờ xử lý";
    case 1:
      return "Đã chấp nhận / Chờ hoàn tiền";
    case 2:
      return "Đã từ chối";
    case 3:
      return "Đã xử lý hoàn tiền";
    default:
      return Number(order.status) === 7 ? "Đã xử lý hoàn tiền" : "Chờ xử lý";
  }
}

function getReturnProcessBadgeClass(order: AdminOrderResponse) {
  return {
    "is-pending": getReturnProcessStatus(order) === 0,
    "is-accepted": getReturnProcessStatus(order) === 1,
    "is-rejected": getReturnProcessStatus(order) === 2,
    "is-refunded": getReturnProcessStatus(order) === 3,
  };
}

function canAcceptReturn(order: AdminOrderResponse) {
  if (order.canAcceptReturn !== undefined && order.canAcceptReturn !== null) {
    return order.canAcceptReturn === true;
  }

  return Number(order.status) === 6 && getReturnProcessStatus(order) === 0;
}

function canRejectReturn(order: AdminOrderResponse) {
  if (order.canRejectReturn !== undefined && order.canRejectReturn !== null) {
    return order.canRejectReturn === true;
  }

  return Number(order.status) === 6 && getReturnProcessStatus(order) === 0;
}

function canMarkReturnRefunded(order: AdminOrderResponse) {
  if (
    order.canMarkReturnRefunded !== undefined &&
    order.canMarkReturnRefunded !== null
  ) {
    return order.canMarkReturnRefunded === true;
  }

  return Number(order.status) === 6 && getReturnProcessStatus(order) === 1;
}

function getDeliveryRefundAmount(order?: AdminOrderResponse | null) {
  return Number((order as any)?.deliveryRefundAmount ?? 0);
}

function hasDeliveryRefundBankInfo(order?: AdminOrderResponse | null) {
  return Boolean(
    String((order as any)?.deliveryRefundBankName || "").trim() &&
      String((order as any)?.deliveryRefundBankAccountNumber || "").trim() &&
      String((order as any)?.deliveryRefundBankAccountHolder || "").trim()
  );
}

function isDeliveryRefundCompleted(order?: AdminOrderResponse | null) {
  return Boolean((order as any)?.deliveryRefundedAt);
}

function hasDeliveryRefundInfo(order?: AdminOrderResponse | null) {
  const status = Number(order?.status);
  return Boolean(
    (status === 5 || status === 8 || status === 4) &&
      (getDeliveryRefundAmount(order) > 0 ||
        (order as any)?.deliveryRefundBankName ||
        (order as any)?.deliveryRefundBankAccountNumber ||
        (order as any)?.deliveryRefundBankAccountHolder ||
        (order as any)?.deliveryRefundedAt)
  );
}

function canMarkDeliveryRefunded(order?: AdminOrderResponse | null) {
  if (!order) {
    return false;
  }

  if (
    order.canMarkDeliveryRefunded !== undefined &&
    order.canMarkDeliveryRefunded !== null
  ) {
    return order.canMarkDeliveryRefunded === true;
  }

  // Nút Đã chuyển tiền ở box hoàn hàng chỉ bật nếu trạng thái là 5
  return (
    Number(order.status) === 5 &&
    getDeliveryRefundAmount(order) > 0 &&
    hasDeliveryRefundBankInfo(order) &&
    !isDeliveryRefundCompleted(order)
  );
}

function getDeliveryRefundStatusText(order?: AdminOrderResponse | null) {
  if (!hasDeliveryRefundInfo(order)) {
    return "Không cần hoàn tiền";
  }

  if (isDeliveryRefundCompleted(order)) {
    return "Đã hoàn tiền";
  }

  if (hasDeliveryRefundBankInfo(order)) {
    return "Chờ shop hoàn tiền";
  }

  return "Chờ khách nhập STK";
}

function getDeliveryRefundBadgeClass(order?: AdminOrderResponse | null) {
  return {
    "is-waiting-bank":
      hasDeliveryRefundInfo(order) && !hasDeliveryRefundBankInfo(order),
    "is-ready-refund":
      hasDeliveryRefundInfo(order) &&
      hasDeliveryRefundBankInfo(order) &&
      !isDeliveryRefundCompleted(order),
    "is-refunded": isDeliveryRefundCompleted(order),
  };
}

function getOrderReturnRejectReason(order: AdminOrderResponse) {
  const reason =
    order.returnRejectReason ??
    order.rejectReason ??
    order.rejectedReason ??
    "";

  return String(reason || "").trim();
}

function getReturnItemRejectReason(item: AdminReturnItemResponse) {
  const reason = item.rejectReason ?? item.rejectedReason ?? "";

  return String(reason || "").trim();
}

function getReturnMediaList(order: AdminOrderResponse) {
  return [
    ...(order.returnImages || []),
    ...(order.returnVideos || []),
    ...(order.returnMediaUrls || []),
  ].filter((url): url is string => Boolean(url && String(url).trim()));
}

function isVideoUrl(value?: string | null) {
  const url = String(value || "").toLowerCase();

  return (
    url.includes("/video/upload/") ||
    /\.(mp4|webm|ogg|mov|m4v)(\?|#|$)/i.test(url)
  );
}

function openMedia(mediaUrl: string) {
  selectedMediaUrl.value = mediaUrl;
}

function closeMedia() {
  selectedMediaUrl.value = null;
}

function handleImageError(event: Event) {
  const target = event.target as HTMLImageElement | null;

  if (!target) return;

  target.onerror = null;
  target.src = FALLBACK_IMAGE;
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

function isCancelledOrder(order?: AdminOrderResponse | null) {
  return Number(order?.status) === 4 || Number(order?.status) === 8;
}

function getOrderCancelReason(order?: AdminOrderResponse | null) {
  const rawReason =
    (order as any)?.cancelReason ??
    (order as any)?.cancellationReason ??
    (order as any)?.cancelNote ??
    (order as any)?.cancelDescription ??
    "";

  const reason = String(rawReason || "").trim();

  return reason || "Chưa có lý do hủy";
}

function getOrderCancelledAt(order?: AdminOrderResponse | null) {
  return (
    (order as any)?.cancelledAt ??
    (order as any)?.canceledAt ??
    (order as any)?.cancelAt ??
    (order as any)?.cancelDate ??
    null
  );
}

function formatRefundMethod(method?: string | number | null) {
  const refundMethod = normalizeRefundMethodValue(method);

  if (!refundMethod) {
    return "-";
  }

  if (refundMethod === "BANK_TRANSFER") {
    return "Chuyển khoản ngân hàng";
  }

  if (refundMethod === "STORE") {
    return "Hoàn tại cửa hàng";
  }

  return String(method);
}

function formatReturnItemStatus(status?: number | null) {
  switch (Number(status)) {
    case 0:
      return "Chờ xử lý";
    case 1:
      return "Đã chấp nhận";
    case 2:
      return "Từ chối";
    case 3:
      return "Đã hoàn tiền";
    default:
      return "-";
  }
}

function getReturnItemStatusClass(status?: number | null) {
  return {
    "is-pending": Number(status) === 0,
    "is-accepted": Number(status) === 1,
    "is-rejected": Number(status) === 2,
    "is-refunded": Number(status) === 3,
  };
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
function formatPaymentMethod(method?: string | null) {
  if (!method) return "-";

  const upper = method.toUpperCase().trim();

  if (upper === "MIXED_VIETQR") return "Tiền mặt + VietQR";
  if (upper === "MIXED_VNPAY") return "Tiền mặt + VNPay";
  if (upper === "MIXED_CASH") return "Thanh toán hỗn hợp";
  if (upper.includes("MIXED")) return "Thanh toán hỗn hợp";

  if (upper.includes("COD")) return "COD - thanh toán khi nhận hàng";
  if (upper.includes("VIETQR") || upper.includes("QR"))
    return "Chuyển khoản VietQR";
  if (upper.includes("VNPAY")) return "Thanh toán qua VNPay";
  if (upper.includes("MOMO")) return "Thanh toán qua MoMo";
  if (upper.includes("CASH")) return "Tiền mặt";
  if (upper === "HOLD") return "Phiếu treo";
  if (upper.includes("BANK") || upper.includes("TRANSFER"))
    return "Chuyển khoản ngân hàng";

  return method;
}
</script>

<style scoped>
.payment-method-text {
  color: #0f172a;
  font-weight: 600;
}
.modal {
  background: rgba(0, 0, 0, 0.1);
}

.order-product-image-box {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  border: 1px solid #dee2e6;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.order-product-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 3px;
  display: block;
}

.price-stack {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  line-height: 1.2;
}

.old-price {
  color: #9ca3af;
  font-size: 12px;
  text-decoration: line-through;
}

.final-price {
  color: #111827;
  font-weight: 800;
}

.delivery-refund-section {
  border-color: #bfdbfe !important;
  background: #eff6ff;
}

.delivery-refund-card {
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #ffffff;
  padding: 12px;
}

.delivery-refund-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
}

.delivery-refund-row:last-child {
  margin-bottom: 0;
}

.delivery-refund-row span {
  color: #64748b;
}

.delivery-refund-row strong {
  color: #0f172a;
  text-align: right;
  word-break: break-word;
}

.refund-money-row strong {
  color: #dc2626;
  font-size: 15px;
}

.delivery-refund-badge {
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.delivery-refund-badge.is-waiting-bank {
  background: #fef3c7;
  color: #92400e;
}

.delivery-refund-badge.is-ready-refund {
  background: #dbeafe;
  color: #1d4ed8;
}

.delivery-refund-badge.is-refunded {
  background: #dcfce7;
  color: #166534;
}

.delivery-refund-empty {
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  padding: 12px;
}

.return-section {
  border: 1px solid #fde68a;
  background: #fffbeb;
  border-radius: 10px;
  padding: 14px;
}

.return-header {
  border-bottom: 1px solid #fde68a;
  padding-bottom: 10px;
}

.return-badge {
  border-radius: 999px;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  color: #92400e;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.return-badge.is-accepted {
  background: #dcfce7;
  border-color: #16a34a;
  color: #166534;
}

.return-badge.is-rejected {
  background: #fee2e2;
  border-color: #ef4444;
  color: #991b1b;
}

.return-badge.is-refunded {
  background: #dbeafe;
  border-color: #2563eb;
  color: #1e40af;
}

.return-info-card {
  background: #ffffff;
  border: 1px solid #fde68a;
  border-radius: 10px;
  padding: 12px;
}

.return-info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 5px 0;
  font-size: 13px;
}

.return-info-row span {
  color: #6b7280;
  flex-shrink: 0;
}

.return-info-row strong {
  color: #111827;
  text-align: right;
}

.return-money-row strong {
  color: #dc2626;
}

.return-reject-reason,
.return-item-reject-reason {
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.35;
}

.delivery-section {
  background: #fffdf8;
}

.delivery-info-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 12px;
}

.delivery-info-title {
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
  color: #6b7280;
}

.delivery-info-row strong {
  color: #111827;
  text-align: right;
}

.delivery-actor-row {
  align-items: flex-start;
}

.delivery-actor-value {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 0;
  text-align: right;
}

.delivery-actor-value strong {
  color: #111827;
  font-weight: 800;
  line-height: 1.25;
  word-break: break-word;
}

.delivery-actor-value small {
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.25;
  word-break: break-all;
}

.delivery-media-label {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
}

.return-media-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.return-media-item {
  width: 74px;
  height: 74px;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  background: #ffffff;
}

.return-media-item img,
.return-media-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.return-item-status {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.return-item-status.is-pending {
  background: #fef3c7;
  color: #92400e;
}

.return-item-status.is-accepted {
  background: #dcfce7;
  color: #166534;
}

.return-item-status.is-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.return-item-status.is-refunded {
  background: #dbeafe;
  color: #1e40af;
}

.cancel-reason-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-left: 4px solid #dc2626;
  border-radius: 10px;
  padding: 10px 12px;
}

.cancel-reason-title {
  color: #b91c1c;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
}

.cancel-reason-text {
  color: #111827;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-word;
}

.cancel-reason-time {
  margin-top: 5px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.4;
}

.return-media-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  z-index: 1065;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.return-media-modal-content {
  position: relative;
  max-width: 92vw;
  max-height: 92vh;
  background: #111827;
  border-radius: 12px;
  padding: 12px;
}

.return-media-modal-content img,
.return-media-modal-content video {
  max-width: 88vw;
  max-height: 84vh;
  display: block;
  border-radius: 8px;
}

.btn-close-media {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: #ffffff;
  color: #111827;
  font-size: 22px;
  line-height: 1;
  font-weight: 700;
}
</style>