<script setup lang="ts">
import { ref } from "vue";

import {
  REFERENCE_TYPE_OPTIONS,
  STOCK_MOVEMENT_TYPE_OPTIONS,
} from "../types/stock-movement.type";

import type {
  StockMovementDetailResponse,
} from "../types/stock-movement.type";

const props = defineProps<{
  visible: boolean;
  detail: StockMovementDetailResponse | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
}>();

const failedImageUrls =
  ref<Set<string>>(new Set());

const previewImageUrl = ref("");

const hasUsableImage = (
  imageUrl?: string | null
) =>
  Boolean(
    imageUrl &&
      !failedImageUrls.value.has(
        imageUrl
      )
  );

const markImageFailed = (
  imageUrl?: string | null
) => {
  if (!imageUrl) {
    return;
  }

  const next =
    new Set(
      failedImageUrls.value
    );

  next.add(imageUrl);

  failedImageUrls.value =
    next;
};

const openImagePreview = () => {
  const imageUrl =
    props.detail?.imageUrl;

  if (
    !hasUsableImage(imageUrl)
  ) {
    return;
  }

  previewImageUrl.value =
    imageUrl!;
};

const closeImagePreview = () => {
  previewImageUrl.value = "";
};

const onImageError = (event: Event) => {
  const image = event.currentTarget as HTMLImageElement;

  markImageFailed(
    props.detail?.imageUrl ||
      image.currentSrc ||
      image.src
  );
};

const onPreviewImageError =
  () => {
    markImageFailed(
      previewImageUrl.value
    );

    closeImagePreview();
  };

const close = () => {
  closeImagePreview();
  emit("close");
};

const formatNumber = (
  value?: number | null
) =>
  new Intl.NumberFormat("vi-VN")
    .format(Number(value ?? 0));

const formatCapacity = (
  value?: number | null
) => {
  if (
    value == null ||
    !Number.isFinite(Number(value))
  ) {
    return "—";
  }

  return `${new Intl.NumberFormat(
    "vi-VN",
    {
      maximumFractionDigits: 2,
    }
  ).format(Number(value))} ml`;
};

const formatSignedNumber = (
  value?: number | null
) => {
  const numberValue =
    Number(value ?? 0);

  const formatted =
    formatNumber(
      Math.abs(numberValue)
    );

  if (numberValue > 0) {
    return `+${formatted}`;
  }

  if (numberValue < 0) {
    return `-${formatted}`;
  }

  return "0";
};

const formatDateTime = (
  value?: string | null
) => {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat(
    "vi-VN",
    {
      dateStyle: "short",
      timeStyle: "medium",
    }
  ).format(new Date(value));
};

const movementLabel = (
  value?: string | null,
  backendLabel?: string | null
) => {
  if (backendLabel) {
    return backendLabel;
  }

  const option =
    STOCK_MOVEMENT_TYPE_OPTIONS.find(
      item =>
        item.value === value
    );

  return (
    option?.label ||
    value ||
    "Không xác định"
  );
};

const movementClass = (
  value?: number | null
) => {
  const numberValue =
    Number(value ?? 0);

  if (numberValue > 0) {
    return "movement-in";
  }

  if (numberValue < 0) {
    return "movement-out";
  }

  return "movement-neutral";
};

const referenceLabel = (
  value?: string | null
) => {
  if (!value) {
    return "Không có chứng từ nguồn";
  }

  const option =
    REFERENCE_TYPE_OPTIONS.find(
      item =>
        item.value === value
    );

  return option?.label || value;
};
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="modal-backdrop"
      @click.self="close"
    >
      <div
        class="dialog"
        role="dialog"
        aria-modal="true"
      >
        <div class="modal-header">
          <div>
            <h3>
              Chi tiết biến động kho
            </h3>

            <p v-if="detail">
              {{ detail.sku }}
              · Lô
              {{ detail.lotCode }}
            </p>
          </div>

          <button
            type="button"
            class="icon-close"
            @click="close"
          >
            <i
              class="bi bi-x-lg"
            ></i>
          </button>
        </div>

        <div class="modal-body">
          <div
            v-if="loading"
            class="state"
          >
            <span
              class="spinner-border spinner-border-sm"
            ></span>

            Đang tải chi tiết biến động...
          </div>

          <template
            v-else-if="detail"
          >
            <section>
              <div
                class="section-head"
              >
                <h4>
                  Thông tin biến động
                </h4>

                <span
                  class="movement-badge"
                  :class="
                    movementClass(
                      detail.quantityChange
                    )
                  "
                >
                  {{
                    movementLabel(
                      detail.movementType,
                      detail.movementTypeLabel
                    )
                  }}
                </span>
              </div>

              <div
                class="info-grid"
              >
                <div>
                  <span>
                    Thời gian
                  </span>

                  <strong>
                    {{
                      formatDateTime(
                        detail.createdAt
                      )
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Biến động
                  </span>

                  <strong
                    :class="
                      movementClass(
                        detail.quantityChange
                      )
                    "
                  >
                    {{
                      formatSignedNumber(
                        detail.quantityChange
                      )
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Tồn trước
                  </span>

                  <strong>
                    {{
                      formatNumber(
                        detail.quantityBefore
                      )
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Tồn sau
                  </span>

                  <strong>
                    {{
                      formatNumber(
                        detail.quantityAfter
                      )
                    }}
                  </strong>
                </div>
              </div>
            </section>

            <section>
              <h4>
                Sản phẩm và lô
              </h4>

              <div
                class="product-section"
              >
                <button
                  type="button"
                  class="detail-product-image"
                  :class="{
                    clickable:
                      hasUsableImage(
                        detail.imageUrl
                      ),
                  }"
                  :disabled="
                    !hasUsableImage(
                      detail.imageUrl
                    )
                  "
                  :title="
                    hasUsableImage(
                      detail.imageUrl
                    )
                      ? 'Bấm để xem ảnh lớn'
                      : 'Sản phẩm chưa có ảnh'
                  "
                  @click="
                    openImagePreview
                  "
                >
                  <i
                    class="bi bi-image"
                  ></i>

                  <img
                    v-if="
                      hasUsableImage(
                        detail.imageUrl
                      )
                    "
                    :src="
                      detail.imageUrl ||
                      ''
                    "
                    :alt="
                      detail.productName
                    "
                    @error="
                      onImageError
                    "
                  />
                </button>

                <div
                  class="info-grid product-info-grid"
                >
                  <div>
                    <span>SKU</span>

                    <strong>
                      {{ detail.sku }}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Sản phẩm
                    </span>

                    <strong>
                      {{
                        detail.productName
                      }}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Dung tích
                    </span>

                    <strong>
                      {{
                        formatCapacity(
                          detail.capacityValue
                        )
                      }}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Loại chai
                    </span>

                    <strong>
                      {{
                        detail.bottleTypeName ||
                        "—"
                      }}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Mã lô
                    </span>

                    <strong>
                      {{
                        detail.lotCode
                      }}
                    </strong>
                  </div>
                </div>
              </div>
            </section>

            <section>
              <h4>
                Chứng từ nguồn
              </h4>

              <div
                class="info-grid"
              >
                <div>
                  <span>
                    Loại chứng từ
                  </span>

                  <strong>
                    {{
                      referenceLabel(
                        detail.referenceType
                      )
                    }}
                  </strong>
                </div>
              </div>
            </section>

            <section>
              <h4>
                Thông tin thao tác
              </h4>

              <div
                class="info-grid"
              >
                <div>
                  <span>
                    Người thao tác
                  </span>

                  <strong>
                    {{
                      detail.createdByName ||
                      "—"
                    }}
                  </strong>
                </div>

                <div class="full">
                  <span>
                    Lý do / ghi chú
                  </span>

                  <strong>
                    {{
                      detail.reason ||
                      "Không có ghi chú"
                    }}
                  </strong>
                </div>
              </div>
            </section>
          </template>

          <div
            v-else
            class="state"
          >
            Không có dữ liệu biến động.
          </div>
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
    </div>
  </Teleport>

  <Teleport to="body">
    <div
      v-if="
        previewImageUrl
      "
      class="image-preview-backdrop"
      @click.self="
        closeImagePreview
      "
    >
      <div
        class="image-preview-dialog"
      >
        <button
          type="button"
          class="image-preview-close"
          aria-label="Đóng ảnh"
          @click="
            closeImagePreview
          "
        >
          <i
            class="bi bi-x-lg"
          ></i>
        </button>

        <img
          :src="
            previewImageUrl
          "
          :alt="
            detail?.productName ||
            'Sản phẩm'
          "
          class="image-preview-img"
          @error="
            onPreviewImageError
          "
        />

        <div
          v-if="detail"
          class="image-preview-info"
        >
          <strong>
            {{
              detail.productName
            }}
          </strong>

          <span>
            {{ detail.sku }}
          </span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow-y: auto;
  padding: 32px 20px;
  background:
    rgba(15, 23, 42, 0.52);
}

.dialog {
  width: min(900px, 100%);
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow:
    0 24px 60px
      rgba(15, 23, 42, 0.2);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom:
    1px solid #e5e7eb;
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
  max-height: 72vh;
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

.info-grid {
  display: grid;
  grid-template-columns:
    repeat(
      2,
      minmax(0, 1fr)
    );
  gap: 12px;
}

.info-grid > div {
  padding: 13px 14px;
  border:
    1px solid #e5e7eb;
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

/* =========================
   PRODUCT
   ========================= */

.product-section {
  display: flex;
  align-items: stretch;
  gap: 14px;
}

.detail-product-image {
  position: relative;
  width: 120px;
  min-width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  border:
    1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
  color: #9ca3af;
  cursor: default;
}

.detail-product-image:disabled {
  opacity: 1;
}

.detail-product-image.clickable {
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.detail-product-image.clickable:hover {
  border-color: #b6b6b6;
  box-shadow:
    0 4px 14px
      rgba(15, 23, 42, 0.12);
  transform: scale(1.02);
}

.detail-product-image i {
  font-size: 26px;
}

.detail-product-image img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}

.product-info-grid {
  flex: 1;
}

/* ========================= */

.movement-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.movement-in {
  color: #047857 !important;
}

.movement-badge.movement-in {
  background: #ecfdf5;
}

.movement-out {
  color: #b91c1c !important;
}

.movement-badge.movement-out {
  background: #fef2f2;
}

.movement-neutral {
  color: #4b5563 !important;
}

.movement-badge.movement-neutral {
  background: #f3f4f6;
}

.state {
  padding: 36px;
  text-align: center;
  color: #6b7280;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top:
    1px solid #e5e7eb;
  background: #f9fafb;
}

.close-btn {
  min-height: 40px;
  padding: 0 16px;
  border:
    1px solid #d1d5db;
  border-radius: 9px;
  background: #fff;
  color: #374151;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
}

/* =========================
   IMAGE PREVIEW
   ========================= */

.image-preview-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100001;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    rgba(15, 23, 42, 0.72);
}

.image-preview-dialog {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(760px, 100%);
  max-height:
    calc(100vh - 48px);
  padding: 18px;
  border-radius: 14px;
  background: #fff;
  box-shadow:
    0 24px 70px
      rgba(0, 0, 0, 0.28);
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
  background:
    rgba(255, 255, 255, 0.92);
  color: #333;
  cursor: pointer;
  box-shadow:
    0 2px 8px
      rgba(0, 0, 0, 0.12);
}

.image-preview-close:hover {
  background: #f3f4f6;
}

.image-preview-img {
  display: block;
  max-width: 100%;
  max-height:
    calc(100vh - 170px);
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

@media (max-width: 700px) {
  .modal-backdrop {
    padding: 12px;
  }

  .info-grid {
    grid-template-columns:
      1fr;
  }

  .info-grid .full {
    grid-column: auto;
  }

  .product-section {
    flex-direction: column;
  }

  .detail-product-image {
    width: 120px;
    min-width: 120px;
  }

  .image-preview-backdrop {
    padding: 12px;
  }

  .image-preview-dialog {
    padding: 12px;
  }

  .image-preview-img {
    max-height:
      calc(100vh - 140px);
  }
}
</style>