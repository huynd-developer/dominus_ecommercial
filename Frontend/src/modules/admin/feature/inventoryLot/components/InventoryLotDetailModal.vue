<script setup lang="ts">
import { ref } from "vue";
import type {
  InventoryLotDetailResponse,
} from "../types/inventory-lot.type";

defineProps<{
  visible: boolean;
  detail: InventoryLotDetailResponse | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
}>();

const formatDate = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`))
    : "—";

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const formatMoney = (value?: number | null) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return "—";
  }

  return `${new Intl.NumberFormat("vi-VN").format(Number(value))} đ`;
};

const inventoryValue = (detail: InventoryLotDetailResponse) => {
  if (
    detail.unitCost == null ||
    !Number.isFinite(Number(detail.unitCost))
  ) {
    return null;
  }

  return Number(detail.quantityOnHand ?? 0) * Number(detail.unitCost);
};

const expiryText = (detail: InventoryLotDetailResponse) => {
  if (detail.isExpired) {
    return `Đã hết hạn ${Math.abs(detail.daysToExpiry)} ngày`;
  }

  if (detail.isNearExpiry) {
    return `Sắp hết hạn · còn ${detail.daysToExpiry} ngày`;
  }

  return `Còn ${detail.daysToExpiry} ngày`;
};

const expiryClass = (detail: InventoryLotDetailResponse) => {
  if (detail.isExpired) return "badge-expired";
  if (detail.isNearExpiry) return "badge-warning";
  return "badge-ok";
};

const previewImageUrl = ref("");
const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(imageUrl && !failedImageUrls.value.has(imageUrl));

const openImagePreview = (detail: InventoryLotDetailResponse) => {
  if (!hasUsableImage(detail.imageUrl)) {
    return;
  }

  previewImageUrl.value = detail.imageUrl!;
};

const closeImagePreview = () => {
  previewImageUrl.value = "";
};

const onImageError = (event: Event) => {
  const image = event.currentTarget as HTMLImageElement;
  const src = image.currentSrc || image.src;

  if (src) {
    const next = new Set(failedImageUrls.value);
    next.add(src);
    failedImageUrls.value = next;
  }
};

const onPreviewImageError = () => {
  if (previewImageUrl.value) {
    const next = new Set(failedImageUrls.value);
    next.add(previewImageUrl.value);
    failedImageUrls.value = next;
  }

  closeImagePreview();
};
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="detail-backdrop" @click.self="emit('close')">
      <div class="detail-dialog">
        <div class="detail-header">
          <div class="detail-header-main">
            <button
              v-if="detail"
              type="button"
              class="detail-product-thumb"
              :class="{ clickable: hasUsableImage(detail.imageUrl) }"
              :disabled="!hasUsableImage(detail.imageUrl)"
              :title="
                hasUsableImage(detail.imageUrl)
                  ? 'Bấm để xem ảnh lớn'
                  : 'Sản phẩm chưa có ảnh'
              "
              @click="openImagePreview(detail)"
            >
              <i class="bi bi-image"></i>

              <img
                v-if="hasUsableImage(detail.imageUrl)"
                :src="detail.imageUrl || ''"
                :alt="detail.productName"
                @error="onImageError"
              />
            </button>

            <div>
              <div class="title-line">
                <h3>{{ detail?.lotCode || "Chi tiết lô hàng" }}</h3>
                <span
                  v-if="detail"
                  class="status-badge"
                  :class="expiryClass(detail)"
                >
                  {{ expiryText(detail) }}
                </span>
              </div>
              <p v-if="detail">{{ detail.sku }} · {{ detail.productName }}</p>
            </div>
          </div>

          <button type="button" class="close-btn" @click="emit('close')">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div v-if="loading" class="state">Đang tải chi tiết lô...</div>

        <div v-else-if="detail" class="detail-body">
          <section>
            <h4>Thông tin lô</h4>
            <div class="info-grid">
              <div>
                <span>Mã lô</span>
                <strong>{{ detail.lotCode }}</strong>
              </div>
              <div>
                <span>SKU</span>
                <strong>{{ detail.sku }}</strong>
              </div>
              <div>
                <span>Sản phẩm</span>
                <strong>{{ detail.productName }}</strong>
              </div>
              <div>
                <span>Ngày sản xuất</span>
                <strong>{{ formatDate(detail.manufacturedDate) }}</strong>
              </div>
              <div>
                <span>Ngày nhận</span>
                <strong>{{ formatDate(detail.receivedDate) }}</strong>
              </div>
              <div>
                <span>Hạn sử dụng</span>
                <strong>{{ formatDate(detail.expirationDate) }}</strong>
              </div>
              <div>
                <span>Trạng thái HSD</span>
                <strong>{{ expiryText(detail) }}</strong>
              </div>
            </div>
          </section>

          <section>
            <h4>Tồn kho</h4>
            <div class="stock-grid">
              <div>
                <span>Tồn ban đầu</span>
                <strong>{{ formatNumber(detail.initialQuantity) }}</strong>
              </div>
              <div>
                <span>Tồn hiện tại</span>
                <strong>{{ formatNumber(detail.quantityOnHand) }}</strong>
              </div>
              <div>
                <span>Có thể bán</span>
                <strong>{{ formatNumber(detail.sellableQuantity) }}</strong>
              </div>
              <div>
                <span>Đơn giá nhập</span>
                <strong>{{ formatMoney(detail.unitCost) }}</strong>
              </div>
              <div>
                <span>Giá trị tồn</span>
                <strong>{{ formatMoney(inventoryValue(detail)) }}</strong>
              </div>
            </div>

            <div v-if="detail.isExpired" class="notice danger">
              <i class="bi bi-exclamation-triangle"></i>
              Lô đã hết hạn nên số lượng có thể bán bằng 0.
            </div>
          </section>

          <section>
            <h4>Thông tin tạo lô</h4>
            <div class="info-grid">
              <div>
                <span>Người tạo</span>
                <strong>{{ detail.createdByName }}</strong>
              </div>
              <div>
                <span>Thời gian tạo</span>
                <strong>{{ formatDateTime(detail.createdAt) }}</strong>
              </div>
            </div>
          </section>
        </div>

        <div class="detail-footer">
          <button
            type="button"
            class="close-footer-btn"
            @click="emit('close')"
          >
            Đóng
          </button>
        </div>
      </div>

      <div
        v-if="previewImageUrl && detail"
        class="image-preview-backdrop"
        @click.self="closeImagePreview"
      >
        <div class="image-preview-dialog">
          <button
            type="button"
            class="image-preview-close"
            aria-label="Đóng ảnh"
            @click="closeImagePreview"
          >
            <i class="bi bi-x-lg"></i>
          </button>

          <img
            :src="previewImageUrl"
            :alt="detail.productName"
            class="image-preview-img"
            @error="onPreviewImageError"
          />

          <div class="image-preview-info">
            <strong>{{ detail.productName }}</strong>
            <span>{{ detail.sku }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  overflow-y: auto;
  padding: 24px;
  background: rgba(15, 23, 42, 0.5);
}
:global(.swal2-container) {
  z-index: 100000 !important;
}
.detail-dialog {
  width: min(1120px, 100%);
  margin: 20px auto;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}
.detail-header,
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 22px;
  border-bottom: 1px solid #e5e7eb;
}
.detail-header-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}
.detail-product-thumb {
  position: relative;
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  color: #9ca3af;
  cursor: default;
}
.detail-product-thumb:disabled {
  opacity: 1;
}
.detail-product-thumb.clickable {
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}
.detail-product-thumb.clickable:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.1);
  transform: scale(1.04);
}
.detail-product-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}
.detail-product-thumb i {
  font-size: 20px;
}
.detail-header h3 {
  margin: 0;
}
.detail-header p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}
.title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-footer {
  border-top: 1px solid #e5e7eb;
  border-bottom: 0;
}
.detail-footer > div {
  display: flex;
  gap: 8px;
}
.close-btn {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}
.close-footer-btn {
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.close-footer-btn {
  padding: 9px 16px;
  background: #111827;
  color: #fff;
}
.detail-body {
  padding: 20px 22px;
}
section + section {
  margin-top: 24px;
}
section h4 {
  margin: 0 0 12px;
}
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title-row h4 {
  margin: 0;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.info-grid > div,
.stock-grid > div {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}
.info-grid span,
.stock-grid span {
  display: block;
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
}
.stock-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.stock-grid strong {
  font-size: 22px;
}
.notice {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  font-size: 13px;
}
.notice.danger {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
}
.status-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.badge-expired {
  background: #fef2f2;
  color: #b91c1c;
}
.badge-warning {
  background: #fff7ed;
  color: #c2410c;
}
.badge-ok {
  background: #ecfdf5;
  color: #047857;
}
.state {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}
.state.small {
  padding: 14px;
}

.image-preview-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100001;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.72);
}

.image-preview-dialog {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(760px, calc(100% - 48px));
  max-height: calc(100vh - 48px);
  padding: 18px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.28);
}

.image-preview-close {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #333;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.image-preview-img {
  display: block;
  max-width: 100%;
  max-height: calc(100vh - 170px);
  object-fit: contain;
  border-radius: 10px;
}

.image-preview-info {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  text-align: center;
}

.image-preview-info strong {
  color: #333;
  font-size: 14px;
}

.image-preview-info span {
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 800px) {
  .detail-backdrop {
    padding: 10px;
  }
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stock-grid {
    grid-template-columns: 1fr;
  }
}
</style>
