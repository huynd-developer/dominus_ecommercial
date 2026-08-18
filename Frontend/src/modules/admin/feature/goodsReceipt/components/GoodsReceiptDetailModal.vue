<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  GoodsReceiptApprovalHistoryResponse,
  GoodsReceiptDetailResponse,
} from "../types/goods-receipt.type";

const props = defineProps<{
  visible: boolean;
  detail: GoodsReceiptDetailResponse | null;
  history: GoodsReceiptApprovalHistoryResponse[];
  loading?: boolean;
  loadingHistory?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
}>();

const formatDateTime = (
  value?: string | null
) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const formatDate = (
  value?: string | null
) =>
  value
    ? new Intl.DateTimeFormat(
        "vi-VN"
      ).format(
        new Date(`${value}T00:00:00`)
      )
    : "—";

const formatMoney = (
  value?: number | null
) =>
  value == null
    ? "—"
    : new Intl.NumberFormat(
        "vi-VN",
        {
          style: "currency",
          currency: "VND",
        }
      ).format(value);

const formatCapacity = (
  value?: number | null
) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return "";
  }

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
    item.bottleTypeName?.trim() || "",
  ].filter(Boolean);

  return parts.join(" · ");
};

const lineTotal = (
  quantity?: number | null,
  unitCost?: number | null
) =>
  Number(quantity ?? 0) * Number(unitCost ?? 0);

const totalValue = computed(() =>
  props.detail?.items?.reduce(
    (sum, item) =>
      sum + lineTotal(item.quantity, item.unitCost),
    0
  ) ?? 0
);

const statusClass = (
  status?: string
) =>
  `status-${String(status || "")
    .toLowerCase()
    .replace("_approval", "")}`;

const previewImageUrl = ref("");
const previewProductName = ref("");
const previewSku = ref("");
const failedImageUrls = ref<Set<string>>(new Set());

const hasUsableImage = (imageUrl?: string | null) =>
  Boolean(imageUrl && !failedImageUrls.value.has(imageUrl));

const openImagePreview = (item: {
  imageUrl?: string | null;
  productName?: string | null;
  sku?: string | null;
}) => {
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
  <Teleport to="body">
    <div
      v-if="visible"
      class="detail-backdrop"
      @click.self="emit('close')"
    >
      <div class="detail-dialog">
        <!-- HEADER -->
        <div class="detail-header">
          <div>
            <h3>
              {{
                detail?.receiptNo ||
                "Chi tiết phiếu nhập"
              }}
            </h3>

            <span
              v-if="detail"
              class="status-badge"
              :class="
                statusClass(detail.status)
              "
            >
              {{ detail.statusLabel }}
            </span>
          </div>

          <button
            type="button"
            class="close-btn"
            @click="emit('close')"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <!-- LOADING -->
        <div
          v-if="loading"
          class="state"
        >
          Đang tải chi tiết...
        </div>

        <!-- BODY -->
        <div
          v-else-if="detail"
          class="detail-body"
        >
          <!-- THÔNG TIN CHUNG -->
          <section>
            <h4>Thông tin chung</h4>

            <div class="info-grid">
              <!-- Luôn hiển thị -->
              <div>
                <span>Loại phiếu</span>
                <strong>
                  {{
                    detail.receiptTypeLabel
                  }}
                </strong>
              </div>

              <div>
                <span>Trạng thái</span>
                <strong>
                  {{ detail.statusLabel }}
                </strong>
              </div>

              <div>
                <span>Người tạo</span>
                <strong>
                  {{
                    detail.createdByName ||
                    "—"
                  }}
                </strong>
              </div>

              <div>
                <span>Ngày tạo</span>
                <strong>
                  {{
                    formatDateTime(
                      detail.createdAt
                    )
                  }}
                </strong>
              </div>

              <div>
                <span>Tổng SKU</span>
                <strong>
                  {{ detail.totalSku }}
                </strong>
              </div>

              <div>
                <span>Tổng số lượng</span>
                <strong>
                  {{
                    detail.totalQuantity
                  }}
                </strong>
              </div>

              <div>
                <span>Tổng giá trị</span>
                <strong class="total-value">
                  {{ formatMoney(totalValue) }}
                </strong>
              </div>

              <!--
                Chỉ hiện nếu phiếu đã từng gửi duyệt:
                PENDING / APPROVED / REJECTED
              -->
              <template
                v-if="
                  detail.status ===
                    'PENDING_APPROVAL' ||
                  detail.status ===
                    'APPROVED' ||
                  detail.status ===
                    'REJECTED'
                "
              >
                <div>
                  <span>
                    Người gửi duyệt
                  </span>

                  <strong>
                    {{
                      detail.submittedByName ||
                      "—"
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Thời gian gửi duyệt
                  </span>

                  <strong>
                    {{
                      formatDateTime(
                        detail.submittedAt
                      )
                    }}
                  </strong>
                </div>
              </template>

              <!-- Chỉ hiện khi APPROVED -->
              <template
                v-if="
                  detail.status ===
                  'APPROVED'
                "
              >
                <div>
                  <span>
                    Người phê duyệt
                  </span>

                  <strong>
                    {{
                      detail.approvedByName ||
                      "—"
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Thời gian phê duyệt
                  </span>

                  <strong>
                    {{
                      formatDateTime(
                        detail.approvedAt
                      )
                    }}
                  </strong>
                </div>
              </template>

              <!-- Chỉ hiện khi REJECTED -->
              <template
                v-if="
                  detail.status ===
                  'REJECTED'
                "
              >
                <div>
                  <span>
                    Người từ chối
                  </span>

                  <strong>
                    {{
                      detail.rejectedByName ||
                      "—"
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Thời gian từ chối
                  </span>

                  <strong>
                    {{
                      formatDateTime(
                        detail.rejectedAt
                      )
                    }}
                  </strong>
                </div>
              </template>

              <!-- Chỉ hiện khi CANCELLED -->
              <template
                v-if="
                  detail.status ===
                  'CANCELLED'
                "
              >
                <div>
                  <span>Người hủy</span>

                  <strong>
                    {{
                      detail.cancelledByName ||
                      "—"
                    }}
                  </strong>
                </div>

                <div>
                  <span>
                    Thời gian hủy
                  </span>

                  <strong>
                    {{
                      formatDateTime(
                        detail.cancelledAt
                      )
                    }}
                  </strong>
                </div>
              </template>
            </div>

            <!-- LÝ DO TỪ CHỐI -->
            <div
              v-if="
                detail.rejectionReason
              "
              class="reason-box danger"
            >
              <strong>
                Lý do từ chối
              </strong>

              <p>
                {{
                  detail.rejectionReason
                }}
              </p>
            </div>

            <!-- LÝ DO HỦY -->
            <div
              v-if="
                detail.cancellationReason
              "
              class="reason-box"
            >
              <strong>
                Lý do hủy
              </strong>

              <p>
                {{
                  detail.cancellationReason
                }}
              </p>
            </div>
          </section>

          <!-- DANH SÁCH SẢN PHẨM -->
          <section>
            <h4>Danh sách sản phẩm</h4>

            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th class="image-column">Ảnh</th>
                    <th>SKU</th>
                    <th>Sản phẩm / Biến thể</th>
                    <th>Mã lô</th>
                    <th>SL</th>
                    <th>Đơn giá</th>
                    <th>Thành tiền</th>
                    <th>NSX</th>
                    <th>Ngày nhận</th>
                    <th>HSD</th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="item in detail.items"
                    :key="item.id"
                  >
                    <td class="image-cell">
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
                    </td>

                    <td>
                      <strong>
                        {{ item.sku }}
                      </strong>
                    </td>

                    <td>
                      <div class="product-cell">
                        <strong>
                          {{ item.productName }}
                        </strong>

                        <span
                          v-if="variantLabel(item)"
                          class="variant-info"
                        >
                          {{ variantLabel(item) }}
                        </span>
                      </div>
                    </td>

                    <td>
                      {{ item.lotCode }}
                    </td>

                    <td>
                      {{ item.quantity }}
                    </td>

                    <td>
                      {{
                        formatMoney(
                          item.unitCost
                        )
                      }}
                    </td>

                    <td class="line-total">
                      {{
                        formatMoney(
                          lineTotal(
                            item.quantity,
                            item.unitCost
                          )
                        )
                      }}
                    </td>

                    <td>
                      {{
                        formatDate(
                          item.manufacturedDate
                        )
                      }}
                    </td>

                    <td>
                      {{
                        formatDate(
                          item.receivedDate
                        )
                      }}
                    </td>

                    <td>
                      {{
                        formatDate(
                          item.expirationDate
                        )
                      }}
                    </td>

                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <!-- LỊCH SỬ DUYỆT -->
          <section>
            <h4>Lịch sử duyệt</h4>

            <div
              v-if="loadingHistory"
              class="state"
            >
              Đang tải lịch sử...
            </div>

            <div
              v-else-if="
                history.length === 0
              "
              class="state"
            >
              Chưa có lịch sử duyệt.
            </div>

            <div
              v-else
              class="timeline"
            >
              <div
                v-for="entry in history"
                :key="entry.id"
                class="timeline-item"
              >
                <div class="dot"></div>

                <div>
                  <strong>
                    {{
                      entry.fromStatusLabel ||
                      "Khởi tạo"
                    }}
                    →
                    {{
                      entry.toStatusLabel
                    }}
                  </strong>

                  <p>
                    {{
                      entry.actionByName
                    }}
                    ·
                    {{
                      formatDateTime(
                        entry.actionAt
                      )
                    }}
                  </p>

                  <p
                    v-if="entry.reason"
                    class="timeline-reason"
                  >
                    {{ entry.reason }}
                  </p>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- FOOTER -->
        <div class="detail-footer">
          <button
            type="button"
            @click="emit('close')"
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
.detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 24px;

  background: rgba(
    15,
    23,
    42,
    0.5
  );

  overflow: hidden;
}

.detail-dialog {
  width: min(1120px, 100%);
  max-height: calc(100vh - 48px);

  display: flex;
  flex-direction: column;

  margin: 0;

  overflow: hidden;

  border-radius: 16px;
  background: #fff;

  box-shadow:
    0 24px 60px
    rgba(15, 23, 42, 0.22);
}

.detail-header,
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;

  flex-shrink: 0;

  gap: 14px;
  padding: 18px 22px;

  border-bottom:
    1px solid #e5e7eb;
}

.detail-footer {
  justify-content: flex-end;

  border-top:
    1px solid #e5e7eb;

  border-bottom: 0;
}

.detail-header h3 {
  margin: 0 0 7px;
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
  flex: 1;
  min-height: 0;

  padding: 20px 22px;

  overflow-y: auto;
  overflow-x: hidden;
}

section + section {
  margin-top: 24px;
}

section h4 {
  margin: 0 0 12px;
}

.info-grid {
  display: grid;

  grid-template-columns:
    repeat(4, 1fr);

  gap: 12px;
}

.info-grid > div,
.reason-box {
  padding: 12px;

  border:
    1px solid #e5e7eb;

  border-radius: 10px;

  background: #fafafa;
}

.info-grid span {
  display: block;

  margin-bottom: 4px;

  color: #6b7280;
  font-size: 12px;
}

.reason-box {
  margin-top: 12px;
}

.reason-box p {
  margin: 4px 0 0;
}

.reason-box.danger {
  border-color: #fecaca;
  background: #fef2f2;
}

.table-wrap {
  overflow-x: auto;

  border:
    1px solid #e5e7eb;

  border-radius: 10px;
}

table {
  width: 100%;
  min-width: 1080px;

  border-collapse: collapse;
}

th,
td {
  padding: 10px 12px;

  border-bottom:
    1px solid #eee;

  text-align: left;

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
  text-align: center !important;
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
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.product-thumb.clickable:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.1);
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
  font-size: 18px;
}

.product-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-cell > strong {
  color: #111827;
}

.variant-info {
  color: #6b7280;
  font-size: 12px;
  font-weight: 500;
}

.line-total,
.total-value {
  color: #15803d;
  font-weight: 700;
  white-space: nowrap;
}

.timeline {
  display: flex;
  flex-direction: column;

  gap: 14px;
}

.timeline-item {
  display: grid;

  grid-template-columns:
    16px 1fr;

  gap: 10px;
}

.dot {
  width: 10px;
  height: 10px;

  margin-top: 5px;

  border-radius: 50%;

  background: #111827;
}

.timeline-item p {
  margin: 3px 0 0;

  color: #6b7280;
  font-size: 13px;
}

.timeline-reason {
  color:
    #374151 !important;
}

.status-badge {
  display: inline-flex;

  padding: 4px 9px;

  border-radius: 999px;

  font-size: 12px;
  font-weight: 600;
}

.status-draft {
  background: #f3f4f6;
  color: #374151;
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

.state {
  padding: 24px;

  text-align: center;

  color: #6b7280;
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
  .info-grid {
    grid-template-columns:
      repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .detail-backdrop {
    padding: 0;
  }

  .detail-dialog {
    width: 100%;
    max-height: 100vh;

    border-radius: 0;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>