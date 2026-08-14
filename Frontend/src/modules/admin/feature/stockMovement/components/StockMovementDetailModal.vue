<script setup lang="ts">
import {
  REFERENCE_TYPE_OPTIONS,
  STOCK_MOVEMENT_TYPE_OPTIONS,
} from "../types/stock-movement.type";

import type {
  StockMovementDetailResponse,
} from "../types/stock-movement.type";

defineProps<{
  visible: boolean;
  detail: StockMovementDetailResponse | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
}>();

const close = () => {
  emit("close");
};

const formatNumber = (
  value?: number | null
) =>
  new Intl.NumberFormat("vi-VN")
    .format(Number(value ?? 0));

const formatSignedNumber = (
  value?: number | null
) => {
  const numberValue =
    Number(value ?? 0);

  const formatted =
    formatNumber(Math.abs(numberValue));

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
      item => item.value === value
    );

  return option?.label ||
    value ||
    "Không xác định";
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
      item => item.value === value
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
            <h3>Chi tiết biến động kho</h3>

            <p v-if="detail">
              {{ detail.sku }}
              · Lô {{ detail.lotCode }}
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

          <template v-else-if="detail">
            <section>
              <div class="section-head">
                <h4>Thông tin biến động</h4>

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

              <div class="info-grid">
                <div>
                  <span>Thời gian</span>
                  <strong>
                    {{
                      formatDateTime(
                        detail.createdAt
                      )
                    }}
                  </strong>
                </div>

                <div>
                  <span>Biến động</span>
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
                  <span>Tồn trước</span>
                  <strong>
                    {{
                      formatNumber(
                        detail.quantityBefore
                      )
                    }}
                  </strong>
                </div>

                <div>
                  <span>Tồn sau</span>
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
              <h4>Sản phẩm và lô</h4>

              <div class="info-grid">
                <div>
                  <span>SKU</span>
                  <strong>{{ detail.sku }}</strong>
                </div>

                <div>
                  <span>Sản phẩm</span>
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
              <h4>Chứng từ nguồn</h4>

              <div class="info-grid">
                <div>
                  <span>Loại chứng từ</span>
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
              <h4>Thông tin thao tác</h4>

              <div class="info-grid">
                <div>
                  <span>Người thao tác</span>
                  <strong>
                    {{
                      detail.createdByName ||
                      "—"
                    }}
                  </strong>
                </div>

                <div class="full">
                  <span>Lý do / ghi chú</span>
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
  background: rgba(15, 23, 42, 0.52);
}

.dialog {
  width: min(900px, 100%);
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
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
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
}

.close-btn {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #d1d5db;
  border-radius: 9px;
  background: #fff;
  color: #374151;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
}

@media (max-width: 700px) {
  .modal-backdrop {
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
