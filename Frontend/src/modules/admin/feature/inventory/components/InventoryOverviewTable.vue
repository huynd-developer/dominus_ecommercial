<script setup lang="ts">
import { ref } from "vue";
import type { InventoryOverview } from "../types/inventory.type";

defineProps<{
  items: InventoryOverview[];
  loading?: boolean;
}>();

const formatNumber = (value: number) =>
  Number(value ?? 0).toLocaleString("vi-VN");

const previewImageUrl = ref("");
const previewProductName = ref("");
const previewSku = ref("");
const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(
    imageUrl &&
      !failedImageUrls.value.has(imageUrl)
  );

const openImagePreview = (item: InventoryOverview) => {
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
          <th class="number">Tổng tồn</th>
          <th class="number">Có thể bán</th>
          <th class="number">Sắp hết hạn</th>
          <th class="number">Hết hạn</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="loading">
          <td colspan="6" class="empty">
            Đang tải dữ liệu...
          </td>
        </tr>

        <tr v-else-if="items.length === 0">
          <td colspan="6" class="empty">
            Không có dữ liệu tồn kho
          </td>
        </tr>

        <tr
          v-for="item in items"
          v-else
          :key="item.productVariantId"
        >
          <td class="sku">
            {{ item.sku }}
          </td>

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

          <td class="number">
            {{ formatNumber(item.totalQuantity) }}
          </td>

          <td class="number sellable">
            {{ formatNumber(item.sellableQuantity) }}
          </td>

          <td class="number warning">
            {{ formatNumber(item.nearExpiryQuantity) }}
          </td>

          <td class="number danger">
            {{ formatNumber(item.expiredQuantity) }}
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
  width: 100%;
  overflow-x: auto;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

th {
  font-size: 13px;
  color: #666;
  background: #fafafa;
  font-weight: 600;
}

td {
  font-size: 14px;
  color: #333;
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

.sellable {
  color: #238636;
}

.warning {
  color: #b26a00;
}

.danger {
  color: #c62828;
}

.empty {
  padding: 40px;
  text-align: center;
  color: #999;
}

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
