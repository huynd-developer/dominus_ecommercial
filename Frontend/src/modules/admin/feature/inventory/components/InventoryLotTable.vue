<script setup lang="ts">
import { ref } from "vue";
import type { InventoryLotStatus } from "../types/inventory.type";

defineProps<{
  items: InventoryLotStatus[];
  loading?: boolean;
}>();

const formatNumber = (value: number) =>
  Number(value ?? 0).toLocaleString("vi-VN");

const formatDate = (value?: string | null) => {
  if (!value) return "-";

  const date = new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("vi-VN");
};

const previewImageUrl = ref("");
const previewProductName = ref("");
const previewSku = ref("");
const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(
    imageUrl &&
      !failedImageUrls.value.has(imageUrl)
  );

const openImagePreview = (item: InventoryLotStatus) => {
  if (!hasUsableImage(item.imageUrl)) {
    return;
  }

  previewImageUrl.value = item.imageUrl!;
  previewProductName.value = item.productName || "Sản phẩm";
  previewSku.value = item.sku || "";
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
</script>

<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>SKU</th>
          <th>Sản phẩm</th>
          <th>Mã lô</th>
          <th class="number">Tồn</th>
          <th>Hạn sử dụng</th>
          <th class="number">Còn lại</th>
          <th>Trạng thái</th>
        </tr>
      </thead>

      <tbody>
        <!-- LOADING -->
        <tr v-if="loading">
          <td colspan="7" class="empty">
            Đang tải dữ liệu...
          </td>
        </tr>

        <!-- EMPTY -->
        <tr v-else-if="items.length === 0">
          <td colspan="7" class="empty">
            Không có dữ liệu
          </td>
        </tr>

        <!-- DATA -->
        <tr
          v-for="item in items"
          v-else
          :key="item.inventoryLotId"
        >
          <!-- SKU -->
          <td class="sku">
            {{ item.sku }}
          </td>

          <!-- PRODUCT -->
          <td>
            <div class="product-cell">
              <button
                type="button"
                class="product-thumb"
                :class="{
                  clickable: hasUsableImage(item.imageUrl),
                }"
                :disabled="!hasUsableImage(item.imageUrl)"
                :title="
                  hasUsableImage(item.imageUrl)
                    ? 'Bấm để xem ảnh lớn'
                    : 'Sản phẩm chưa có ảnh'
                "
                @click="openImagePreview(item)"
              >
                <i class="bi bi-image"></i>

                <img
                  v-if="hasUsableImage(item.imageUrl)"
                  :src="item.imageUrl || ''"
                  :alt="item.productName"
                  loading="lazy"
                  @error="onImageError"
                />
              </button>

              <span>{{ item.productName }}</span>
            </div>
          </td>

          <!-- LOT -->
          <td>
            {{ item.lotCode }}
          </td>

          <!-- QUANTITY -->
          <td class="number">
            {{ formatNumber(item.quantityOnHand) }}
          </td>

          <!-- EXPIRATION DATE -->
          <td>
            {{ formatDate(item.expirationDate) }}
          </td>

          <!-- DAYS TO EXPIRY -->
          <td class="number">
            <span
              :class="{
                danger: item.daysToExpiry < 0,
                warning:
                  item.daysToExpiry >= 0 &&
                  item.nearExpiry,
              }"
            >
              {{
                item.daysToExpiry < 0
                  ? `Quá hạn ${Math.abs(item.daysToExpiry)} ngày`
                  : `${item.daysToExpiry} ngày`
              }}
            </span>
          </td>

          <!-- STATUS -->
          <td>
            <div class="status-badges">
              <!-- HẾT HẠN -->
              <span
                v-if="item.expired"
                class="badge badge-danger"
              >
                Hết hạn
              </span>

              <!-- SẮP HẾT HẠN -->
              <span
                v-if="item.nearExpiry && !item.expired"
                class="badge badge-warning"
              >
                Sắp hết hạn
              </span>

              <!-- BÌNH THƯỜNG -->
              <span
                v-if="
                  !item.expired &&
                  !item.nearExpiry
                "
                class="badge badge-success"
              >
                Bình thường
              </span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <Teleport to="body">
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
  </Teleport>
</template>

<style scoped>
.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  background: white;
}

table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid #eee;
}

th {
  background: #fafafa;
  color: #666;
  font-size: 13px;
  text-align: left;
}

td {
  font-size: 14px;
}

.number {
  text-align: right;
}

.sku {
  font-weight: 600;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.product-thumb {
  position: relative;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8f9fa;
  color: #9ca3af;
  cursor: default;
}

.product-thumb:disabled {
  opacity: 1;
}

.product-thumb.clickable {
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.product-thumb.clickable:hover {
  border-color: #b6b6b6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: scale(1.04);
}

.product-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}

.product-thumb i {
  font-size: 16px;
}

/* =========================
   STATUS BADGES
   ========================= */

.status-badges {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.badge {
  display: inline-block;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-danger {
  background: #ffebee;
  color: #c62828;
}

.badge-warning {
  background: #fff8e1;
  color: #b26a00;
}

.badge-success {
  background: #e8f5e9;
  color: #238636;
}

/* =========================
   DAYS TO EXPIRY
   ========================= */

.warning {
  color: #b26a00;
}

.danger {
  color: #c62828;
}

/* =========================
   EMPTY / LOADING
   ========================= */

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}

/* =========================
   IMAGE PREVIEW
   ========================= */

.image-preview-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100000;
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
  width: min(760px, 100%);
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

.image-preview-close:hover {
  background: #f3f4f6;
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

@media (max-width: 640px) {
  .image-preview-backdrop {
    padding: 12px;
  }

  .image-preview-dialog {
    padding: 12px;
  }

  .image-preview-img {
    max-height: calc(100vh - 140px);
  }
}
</style>
