<script setup lang="ts">
import { ref } from "vue";
import type { InventoryLotDetailResponse } from "../types/expiry-alert.type";

defineProps<{
  visible: boolean;
  detail: InventoryLotDetailResponse | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
}>();

const formatNumber = (value?: number | null) => {
  return new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));
};

const formatDate = (value?: string | null) => {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`));
};

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
};

const expiryLabel = (detail: InventoryLotDetailResponse) => {
  if (detail.isExpired) {
    const days = Math.abs(detail.daysToExpiry);

    return days === 0 ? "Đã hết hạn" : `Đã hết hạn ${days} ngày`;
  }

  if (detail.isNearExpiry) {
    return detail.daysToExpiry === 0
      ? "Hết hạn hôm nay"
      : `Sắp hết hạn · còn ${detail.daysToExpiry} ngày`;
  }

  return `Còn ${detail.daysToExpiry} ngày`;
};

const expiryClass = (detail: InventoryLotDetailResponse) => {
  if (detail.isExpired) {
    return "badge-danger";
  }

  if (detail.isNearExpiry) {
    return "badge-warning";
  }

  return "badge-success";
};


const previewImageUrl = ref("");
const previewProductName = ref("");
const previewSku = ref("");
const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(imageUrl && !failedImageUrls.value.has(imageUrl));

const openImagePreview = (
  imageUrl?: string | null,
  productName?: string | null,
  sku?: string | null
) => {
  if (!hasUsableImage(imageUrl)) {
    return;
  }

  previewImageUrl.value = imageUrl!;
  previewProductName.value = productName || "Sản phẩm";
  previewSku.value = sku || "";
};

const closeImagePreview = () => {
  previewImageUrl.value = "";
  previewProductName.value = "";
  previewSku.value = "";
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

const close = () => {
  emit("close");
};
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="expiry-detail-backdrop" @click.self="close">
      <div class="expiry-detail-dialog" role="dialog" aria-modal="true">
        <!-- HEADER -->
        <div class="modal-header">
          <div>
            <h3>Chi tiết lô cảnh báo</h3>

            <p v-if="detail">
              {{ detail.sku }}
              ·
              {{ detail.lotCode }}
            </p>
          </div>

          <button
            type="button"
            class="icon-close"
            @click="close"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <!-- BODY -->
        <div class="modal-body">
          <div v-if="loading" class="state">
            <span class="spinner-border spinner-border-sm"></span>

            Đang tải chi tiết lô...
          </div>

          <template v-else-if="detail">
            <section>
              <div class="section-head">
                <h4>Thông tin sản phẩm</h4>

                <span class="status-badge" :class="expiryClass(detail)">
                  {{ expiryLabel(detail) }}
                </span>
              </div>

              <div class="product-info-layout">
                <button
                  type="button"
                  class="detail-product-thumb"
                  :class="{ clickable: hasUsableImage(detail.imageUrl) }"
                  :disabled="!hasUsableImage(detail.imageUrl)"
                  :title="
                    hasUsableImage(detail.imageUrl)
                      ? 'Bấm để xem ảnh lớn'
                      : 'Sản phẩm chưa có ảnh'
                  "
                  @click="
                    openImagePreview(
                      detail.imageUrl,
                      detail.productName,
                      detail.sku
                    )
                  "
                >
                  <i class="bi bi-image"></i>

                  <img
                    v-if="hasUsableImage(detail.imageUrl)"
                    :src="detail.imageUrl || ''"
                    :alt="detail.productName"
                    loading="lazy"
                    @error="onImageError"
                  />
                </button>

                <div class="info-grid product-info-grid">
                  <div>
                    <span>SKU</span>
                    <strong>
                      {{ detail.sku }}
                    </strong>
                  </div>

                  <div>
                    <span>Tên sản phẩm</span>
                    <strong>
                      {{ detail.productName }}
                    </strong>
                  </div>

                  <div>
                    <span>Mã lô</span>
                    <strong>
                      {{ detail.lotCode }}
                    </strong>
                  </div>
                </div>
              </div>
            </section>

            <section>
              <h4>Hạn sử dụng</h4>

              <div class="info-grid">
                <div>
                  <span>Ngày sản xuất</span>
                  <strong>
                    {{ formatDate(detail.manufacturedDate) }}
                  </strong>
                </div>

                <div>
                  <span>Ngày nhập kho</span>
                  <strong>
                    {{ formatDate(detail.receivedDate) }}
                  </strong>
                </div>

                <div>
                  <span>Hạn sử dụng</span>
                  <strong>
                    {{ formatDate(detail.expirationDate) }}
                  </strong>
                </div>

                <div>
                  <span>Số ngày còn lại</span>

                  <strong
                    :class="{
                      'text-danger': detail.daysToExpiry < 0,
                      'text-warning':
                        detail.daysToExpiry >= 0 && detail.isNearExpiry,
                    }"
                  >
                    {{ detail.daysToExpiry }}
                    ngày
                  </strong>
                </div>
              </div>
            </section>

            <section>
              <h4>Tồn kho</h4>

              <div class="info-grid">
                <div>
                  <span>Số lượng ban đầu</span>
                  <strong>
                    {{ formatNumber(detail.initialQuantity) }}
                  </strong>
                </div>

                <div>
                  <span>Tồn hiện tại</span>
                  <strong>
                    {{ formatNumber(detail.quantityOnHand) }}
                  </strong>
                </div>

                <div>
                  <span>Có thể bán</span>

                  <strong
                    :class="
                      detail.sellableQuantity > 0
                        ? 'text-success'
                        : 'text-danger'
                    "
                  >
                    {{ formatNumber(detail.sellableQuantity) }}
                  </strong>
                </div>
              </div>
            </section>

            <section>
              <h4>Nguồn nhập</h4>

              <div class="info-grid">
                <div>
                  <span>Mã phiếu nhập</span>
                  <strong>
                    {{ detail.receiptNo || "—" }}
                  </strong>
                </div>

                <div>
                  <span>Loại phiếu</span>
                  <strong>
                    {{ detail.receiptTypeLabel || detail.receiptType || "—" }}
                  </strong>
                </div>

                <div>
                  <span>Trạng thái phiếu</span>
                  <strong>
                    {{
                      detail.receiptStatusLabel || detail.receiptStatus || "—"
                    }}
                  </strong>
                </div>
              </div>
            </section>

            <section>
              <h4>Thông tin tạo lô</h4>

              <div class="info-grid">
                <div>
                  <span>Người tạo</span>
                  <strong>
                    {{ detail.createdByName || "—" }}
                  </strong>
                </div>

                <div>
                  <span>Thời gian tạo</span>
                  <strong>
                    {{ formatDateTime(detail.createdAt) }}
                  </strong>
                </div>
              </div>
            </section>
          </template>

          <div v-else class="state">Không có dữ liệu lô.</div>
        </div>

        <div class="modal-footer">
          <button
            type="button"
            class="close-btn"
            @click="close"
          >
            Đóng
          </button>
        </div>
      </div>


    <div
      v-if="previewImageUrl"
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
          :alt="previewProductName"
          class="image-preview-img"
          @error="onPreviewImageError"
        />

        <div class="image-preview-info">
          <strong>{{ previewProductName }}</strong>
          <span v-if="previewSku">{{ previewSku }}</span>
        </div>
      </div>
    </div>
    </div>
  </Teleport>
</template>

<style scoped>
.expiry-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;

  display: flex;
  align-items: flex-start;
  justify-content: center;

  overflow-y: auto;

  padding: 32px 20px;

  background: rgba(15, 23, 42, 0.52);
}

.expiry-detail-dialog {
  width: min(920px, 100%);

  border-radius: 16px;

  background: #ffffff;

  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);

  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  padding: 20px 24px;

  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0 0 5px;

  color: #111827;

  font-size: 21px;
  font-weight: 700;
}

.modal-header p {
  margin: 0;

  color: #6b7280;
  font-size: 13px;
}

.icon-close {
  width: 38px;
  height: 38px;

  border: 0;
  border-radius: 9px;

  background: #f3f4f6;

  color: #4b5563;

  cursor: pointer;
}

.modal-body {
  max-height: 70vh;

  overflow-y: auto;

  padding: 24px;
}

section + section {
  margin-top: 24px;
}

section h4 {
  margin: 0 0 12px;

  color: #111827;

  font-size: 15px;
  font-weight: 700;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  margin-bottom: 12px;
}

.section-head h4 {
  margin: 0;
}

.product-info-layout {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  align-items: stretch;
  gap: 12px;
}

.detail-product-thumb {
  position: relative;
  width: 92px;
  height: 92px;
  display: inline-flex;
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
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
  transform: scale(1.03);
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
  font-size: 22px;
}

.product-info-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.info-grid {
  display: grid;

  grid-template-columns: repeat(2, minmax(0, 1fr));

  gap: 12px;
}

.info-grid > div {
  padding: 13px 14px;

  border: 1px solid #e5e7eb;
  border-radius: 10px;

  background: #f9fafb;
}

.info-grid .full {
  grid-column: 1 / -1;
}

.info-grid span {
  display: block;

  margin-bottom: 5px;

  color: #6b7280;

  font-size: 12px;
}

.info-grid strong {
  display: block;

  color: #111827;

  font-size: 14px;
  font-weight: 650;

  word-break: break-word;
}

.status-badge {
  display: inline-flex;
  align-items: center;

  padding: 5px 10px;

  border-radius: 999px;

  font-size: 12px;
  font-weight: 700;

  white-space: nowrap;
}

.badge-danger {
  background: #fef2f2;
  color: #b91c1c;
}

.badge-warning {
  background: #fff7ed;
  color: #c2410c;
}

.badge-success {
  background: #ecfdf5;
  color: #047857;
}

.text-danger {
  color: #b91c1c !important;
}

.text-warning {
  color: #c2410c !important;
}

.text-success {
  color: #047857 !important;
}

.state {
  padding: 36px;

  text-align: center;

  color: #6b7280;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  padding: 16px 24px;

  border-top: 1px solid #e5e7eb;

  background: #f9fafb;
}

.close-btn {
  min-height: 40px;

  padding: 0 16px;

  border-radius: 9px;

  font-size: 14px;
  font-weight: 650;

  cursor: pointer;
}

.close-btn {
  border: 1px solid #d1d5db;

  background: #ffffff;
  color: #374151;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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
  color: #111827;
  font-size: 14px;
}

.image-preview-info span {
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 700px) {
  .product-info-layout {
    grid-template-columns: 1fr;
  }

  .detail-product-thumb {
    width: 80px;
    height: 80px;
  }

  .product-info-grid {
    grid-template-columns: 1fr;
  }
  .expiry-detail-backdrop {
    padding: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-grid .full {
    grid-column: auto;
  }
}
</style>
