<script setup lang="ts">
import { ref } from "vue";
import type {
  StockAdjustmentDetailResponse,
  StockAdjustmentItemResponse,
} from "../types/stock-adjustment.type";

defineProps<{
  visible: boolean;
  detail: StockAdjustmentDetailResponse | null;
  loading?: boolean;
}>();

const emit = defineEmits<{ (e: "close"): void }>();

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const formatSigned = (value?: number | null) => {
  const number = Number(value ?? 0);
  return number > 0 ? `+${formatNumber(number)}` : formatNumber(number);
};

const formatCapacity = (value?: number | null) => {
  if (value == null || !Number.isFinite(Number(value))) return "";

  return `${new Intl.NumberFormat("vi-VN", {
    maximumFractionDigits: 2,
  }).format(Number(value))} ml`;
};

const variantLabel = (item: {
  capacityValue?: number | null;
  bottleTypeName?: string | null;
}) => {
  const parts = [
    formatCapacity(item.capacityValue),
    String(item.bottleTypeName ?? "").trim(),
  ].filter(Boolean);

  return parts.join(" · ");
};

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const statusClass = (status?: string | null) =>
  `status-${String(status || "").toLowerCase().replace("_approval", "")}`;

const differenceClass = (value?: number | null) => {
  const number = Number(value ?? 0);
  if (number > 0) return "diff-up";
  if (number < 0) return "diff-down";
  return "diff-neutral";
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
  if (!hasUsableImage(imageUrl)) return;

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
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="detail-backdrop" @click.self="emit('close')">
      <div class="detail-dialog">
        <div class="detail-header">
          <div>
            <h3>{{ detail?.adjustmentNo || "Chi tiết phiếu kiểm kê" }}</h3>
            <div class="header-meta">
              <span class="type-badge">Kiểm kê thực tế</span>
              <span
                v-if="detail"
                class="status-badge"
                :class="statusClass(detail.status)"
              >
                {{ detail.statusLabel }}
              </span>
            </div>
          </div>
          <button type="button" class="close-btn" @click="emit('close')">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div v-if="loading" class="state">Đang tải chi tiết...</div>

        <div v-else-if="detail" class="detail-body">
          <section>
            <h4>Thông tin chung</h4>

            <div class="summary-grid">
              <div>
                <span>Tổng lô</span>
                <strong>{{ formatNumber(detail.totalLots) }}</strong>
              </div>
              <div>
                <span>Khớp tồn</span>
                <strong>{{ formatNumber(detail.matchedLots) }}</strong>
              </div>
              <div>
                <span>Có chênh lệch</span>
                <strong>{{ formatNumber(detail.mismatchLots) }}</strong>
              </div>
              <div>
                <span>Tổng tăng</span>
                <strong class="text-up">+{{ formatNumber(detail.totalIncrease) }}</strong>
              </div>
              <div>
                <span>Tổng giảm</span>
                <strong class="text-down">-{{ formatNumber(detail.totalDecrease) }}</strong>
              </div>
            </div>

            <div class="info-grid">
              <div>
                <span>Người tạo</span>
                <strong>{{ detail.createdByName || "—" }}</strong>
              </div>
              <div>
                <span>Ngày tạo</span>
                <strong>{{ formatDateTime(detail.createdAt) }}</strong>
              </div>
              <div>
                <span>Người gửi duyệt</span>
                <strong>{{ detail.submittedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian gửi</span>
                <strong>{{ formatDateTime(detail.submittedAt) }}</strong>
              </div>
              <div>
                <span>Người phê duyệt</span>
                <strong>{{ detail.approvedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian phê duyệt</span>
                <strong>{{ formatDateTime(detail.approvedAt) }}</strong>
              </div>
              <div>
                <span>Người từ chối</span>
                <strong>{{ detail.rejectedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian từ chối</span>
                <strong>{{ formatDateTime(detail.rejectedAt) }}</strong>
              </div>
              <div v-if="detail.cancelledByName || detail.cancelledAt">
                <span>Người hủy</span>
                <strong>{{ detail.cancelledByName || "—" }}</strong>
              </div>
              <div v-if="detail.cancelledByName || detail.cancelledAt">
                <span>Thời gian hủy</span>
                <strong>{{ formatDateTime(detail.cancelledAt) }}</strong>
              </div>
            </div>

            <div v-if="detail.note" class="note-box">
              <span>Ghi chú</span>
              <p>{{ detail.note }}</p>
            </div>

            <div v-if="detail.rejectionReason" class="reason-box">
              <strong>Lý do từ chối</strong>
              <p>{{ detail.rejectionReason }}</p>
            </div>

            <div v-if="detail.cancellationReason" class="reason-box cancel-reason-box">
              <strong>Lý do hủy</strong>
              <p>{{ detail.cancellationReason }}</p>
            </div>
          </section>

          <section>
            <h4>Đối chiếu theo lô</h4>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th class="image-column">Ảnh</th>
                    <th>Mã lô</th>
                    <th>SKU</th>
                    <th>Sản phẩm</th>
                    <th>Tồn hệ thống</th>
                    <th>Tồn thực tế</th>
                    <th>Chênh lệch</th>
                    <th>Tồn hiện tại</th>
                    <th>Kết quả</th>
                    <th>Lý do</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in detail.items" :key="item.id">
                    <td class="image-cell">
                      <button
                        type="button"
                        class="product-thumb"
                        :class="{ clickable: hasUsableImage(item.imageUrl) }"
                        :disabled="!hasUsableImage(item.imageUrl)"
                        :title="
                          hasUsableImage(item.imageUrl)
                            ? 'Bấm để xem ảnh lớn'
                            : 'Sản phẩm chưa có ảnh'
                        "
                        @click="
                          openImagePreview(
                            item.imageUrl,
                            item.productName,
                            item.sku
                          )
                        "
                      >
                        <i class="bi bi-image"></i>

                        <img
                          v-if="hasUsableImage(item.imageUrl)"
                          :src="item.imageUrl || ''"
                          :alt="item.productName || 'Sản phẩm'"
                          loading="lazy"
                          @error="onImageError"
                        />
                      </button>
                    </td>
                    <td>{{ item.lotCode || "—" }}</td>
                    <td><strong>{{ item.sku || "—" }}</strong></td>
                    <td>
                      <div class="product-cell">
                        <strong>{{ item.productName || "—" }}</strong>
                        <span
                          v-if="variantLabel(item)"
                          class="variant-info"
                        >
                          {{ variantLabel(item) }}
                        </span>
                      </div>
                    </td>
                    <td>{{ formatNumber(item.systemQuantity) }}</td>
                    <td>{{ formatNumber(item.actualQuantity) }}</td>
                    <td>
                      <strong :class="differenceClass(item.quantityDifference)">
                        {{ formatSigned(item.quantityDifference) }}
                      </strong>
                    </td>
                    <td>{{ formatNumber(item.currentQuantity) }}</td>
                    <td>{{ item.resultLabel || "—" }}</td>
                    <td>{{ item.reason || "—" }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="snapshot-note">
              <i class="bi bi-info-circle"></i>
              “Tồn hệ thống” là snapshot lúc lập/sửa phiếu. “Tồn hiện tại” có
              thể khác nếu kho phát sinh giao dịch trước khi phiếu được duyệt.
            </div>
          </section>
        </div>

        <div class="detail-footer">
          <button type="button" @click="emit('close')">Đóng</button>
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
.detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  overflow-y: auto;
  padding: 24px;
  background: rgba(15, 23, 42, 0.5);
}

.detail-dialog {
  width: min(1180px, 100%);
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

.detail-footer {
  justify-content: flex-end;
  border-top: 1px solid #e5e7eb;
  border-bottom: 0;
}

.detail-header h3 {
  margin: 0 0 7px;
}

.header-meta {
  display: flex;
  gap: 7px;
}

.type-badge,
.status-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.type-badge {
  background: #f3f4f6;
  color: #374151;
}

.status-draft {
  background: #f3f4f6;
  color: #4b5563;
}

.status-pending {
  background: #fff7ed;
  color: #c2410c;
}

.status-approved {
  background: #ecfdf5;
  color: #047857;
}

.status-rejected {
  background: #fef2f2;
  color: #b91c1c;
}

.status-cancelled {
  background: #f3f4f6;
  color: #6b7280;
}

.close-btn,
.detail-footer button {
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.close-btn {
  width: 36px;
  height: 36px;
  background: transparent;
}

.detail-footer button {
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

.summary-grid,
.info-grid {
  display: grid;
  gap: 12px;
}

.summary-grid {
  grid-template-columns: repeat(5, 1fr);
  margin-bottom: 12px;
}

.info-grid {
  grid-template-columns: repeat(4, 1fr);
}

.summary-grid > div,
.info-grid > div,
.note-box,
.reason-box,
.snapshot-note {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}

.summary-grid span,
.info-grid span,
.note-box span {
  display: block;
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
}

.note-box,
.reason-box,
.snapshot-note {
  margin-top: 12px;
}

.note-box p,
.reason-box p {
  margin: 5px 0 0;
}

.reason-box {
  border-color: #fecaca;
  background: #fef2f2;
}

.cancel-reason-box {
  border-color: #fed7aa;
  background: #fff7ed;
}

.text-up,
.diff-up {
  color: #047857;
}

.text-down,
.diff-down {
  color: #b91c1c;
}

.diff-neutral {
  color: #4b5563;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

table {
  width: 100%;
  min-width: 1200px;
  border-collapse: collapse;
}

th,
td {
  padding: 11px 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
  vertical-align: top;
  font-size: 13px;
}

th {
  background: #f9fafb;
  color: #4b5563;
}

.image-column,
.image-cell {
  width: 78px;
  min-width: 78px;
  text-align: center;
}

.product-cell {
  display: flex;
  min-width: 150px;
  flex-direction: column;
  gap: 4px;
}

.product-cell > strong {
  color: #111827;
}

.variant-info {
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
}

.product-thumb {
  position: relative;
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
  color: #9ca3af;
  cursor: default;
}

.product-thumb:disabled {
  opacity: 1;
}

.product-thumb.clickable {
  cursor: pointer;
}

.product-thumb.clickable:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.1);
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
  font-size: 18px;
}

.state {
  padding: 42px;
  color: #6b7280;
  text-align: center;
}

.snapshot-note {
  color: #4b5563;
  font-size: 13px;
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

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .detail-backdrop {
    padding: 0;
  }

  .detail-dialog {
    min-height: 100%;
    margin: 0;
    border-radius: 0;
  }

  .summary-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
