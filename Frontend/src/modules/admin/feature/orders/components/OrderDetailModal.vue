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
          <button class="btn btn-secondary" @click="$emit('close')">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>

  <div v-if="show" class="modal-backdrop show"></div>
</template>

<script setup lang="ts">
import type {
  AdminOrderItemResponse,
  AdminOrderResponse,
} from "../types/order.type";
import OrderStatusBadge from "./OrderStatusBadge.vue";

defineProps<{
  show: boolean;
  order: AdminOrderResponse | null;
}>();

defineEmits<{
  close: [];
}>();

const BACKEND_URL = "http://localhost:8080";

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
</style>