<script setup lang="ts">
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

              <div class="info-grid">
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

@media (max-width: 700px) {
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
