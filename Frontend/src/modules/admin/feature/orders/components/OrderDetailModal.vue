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
                    <td class="text-end">{{ formatMoney(item.finalPrice) }}</td>
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

                <span class="return-badge">
                  {{ order.status === 7 ? "Đã hoàn tiền" : "Chờ hoàn tiền" }}
                </span>
              </div>

              <div class="row g-3 mb-3">
                <div :class="shouldShowBankRefundInfo(order) ? 'col-md-7' : 'col-12'">
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
                      <span>Phương án hoàn tiền:</span>
                      <strong>{{
                        formatRefundMethod(order.refundMethod)
                      }}</strong>
                    </div>

                    <div class="return-info-row return-money-row">
                      <span>Số tiền cần hoàn:</span>
                      <strong>{{
                        formatMoney(getReturnRefundAmount(order))
                      }}</strong>
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
                      <td colspan="8" class="text-center text-muted py-4">
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
                      <td class="text-end fw-semibold text-danger">
                        {{ formatMoney(item.refundAmount) }}
                      </td>
                      <td>
                        <span class="return-item-status">
                          {{
                            item.statusText ||
                            formatReturnItemStatus(item.status)
                          }}
                        </span>
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

defineProps<{
  show: boolean;
  order: AdminOrderResponse | null;
}>();

defineEmits<{
  close: [];
  "mark-return-refunded": [order: AdminOrderResponse];
}>();

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

function getReturnRefundAmount(order: AdminOrderResponse) {
  return Number(order.returnRefundAmount ?? order.refundAmount ?? 0);
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
    .toUpperCase();

  if (!value) {
    return "";
  }

  if (
    value === "1" ||
    value === "BANK_TRANSFER" ||
    value.includes("BANK") ||
    value.includes("TRANSFER") ||
    value.includes("CHUYEN KHOAN") ||
    value.includes("CHUYỂN KHOẢN")
  ) {
    return "BANK_TRANSFER";
  }

  if (
    value === "2" ||
    value === "STORE" ||
    value.includes("CUA HANG") ||
    value.includes("CỬA HÀNG")
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
    order.bankName ||
      order.bankAccountNumber ||
      order.bankAccountHolder
  );
}

function canMarkReturnRefunded(order: AdminOrderResponse) {
  if (
    order.canMarkReturnRefunded !== undefined &&
    order.canMarkReturnRefunded !== null
  ) {
    return order.canMarkReturnRefunded === true;
  }

  return Number(order.status) === 6;
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
