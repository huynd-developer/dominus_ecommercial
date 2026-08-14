<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import Swal from "sweetalert2";
import stockAdjustmentService from "../services/stock-adjustment.service";

import type { InventoryLotListResponse } from "@/modules/admin/feature/inventoryLot/types/inventory-lot.type";
import type {
  StockAdjustmentDetailResponse,
  StockAdjustmentItemRequest,
  StockAdjustmentSaveRequest,
} from "../types/stock-adjustment.type";

interface EditableItem {
  inventoryLotId: number | null;
  sku: string;
  productName: string;
  lotCode: string;
  lotSearch: string;
  systemQuantity: number;
  actualQuantity: number | null;
  reasonPreset: string;
  customReason: string;
  suggestions: InventoryLotListResponse[];
  searching: boolean;
}

const props = defineProps<{
  visible: boolean;
  detail?: StockAdjustmentDetailResponse | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "save", payload: StockAdjustmentSaveRequest): void;
}>();

const note = ref("");
const items = ref<EditableItem[]>([]);
const timers = new Map<number, ReturnType<typeof setTimeout>>();

const OTHER_REASON = "__OTHER__";

const INCREASE_REASONS = [
  "Phát hiện hàng thực tế chưa được ghi nhận",
  "Nhập thiếu số lượng trên hệ thống",
  "Sai lệch khi kiểm đếm",
  "Điều chỉnh sau kiểm kê định kỳ",
] as const;

const DECREASE_REASONS = [
  "Hàng hư hỏng / thất thoát",
  "Xuất kho thực tế chưa được ghi nhận",
  "Ghi nhận thừa số lượng trên hệ thống",
  "Sai lệch khi kiểm đếm",
  "Điều chỉnh sau kiểm kê định kỳ",
] as const;

const reasonsForDifference = (difference: number | null): readonly string[] => {
  if (difference === null || difference === 0) return [];
  return difference > 0 ? INCREASE_REASONS : DECREASE_REASONS;
};

const buildReasonState = (
  reason: string | null | undefined,
  difference: number | null
) => {
  const normalized = String(reason ?? "").trim();

  if (!normalized) {
    return {
      reasonPreset: "",
      customReason: "",
    };
  }

  if (reasonsForDifference(difference).includes(normalized as never)) {
    return {
      reasonPreset: normalized,
      customReason: "",
    };
  }

  return {
    reasonPreset: OTHER_REASON,
    customReason: normalized,
  };
};


const isEdit = computed(() => Boolean(props.detail?.id));
const title = computed(() =>
  isEdit.value
    ? `Sửa phiếu kiểm kê ${props.detail?.adjustmentNo ?? ""}`
    : "Tạo phiếu kiểm kê thực tế"
);

const createEmptyItem = (): EditableItem => ({
  inventoryLotId: null,
  sku: "",
  productName: "",
  lotCode: "",
  lotSearch: "",
  systemQuantity: 0,
  actualQuantity: null,
  reasonPreset: "",
  customReason: "",
  suggestions: [],
  searching: false,
});

const resetForm = () => {
  note.value = props.detail?.note ?? "";

  if (props.detail?.items?.length) {
    items.value = props.detail.items.map((item) => {
      const systemQuantity = Number(item.systemQuantity ?? 0);
      const actualQuantity = Number(item.actualQuantity ?? 0);
      const difference = actualQuantity - systemQuantity;
      const reasonState = buildReasonState(item.reason, difference);

      return {
        inventoryLotId: item.inventoryLotId,
        sku: item.sku ?? "",
        productName: item.productName ?? "",
        lotCode: item.lotCode ?? "",
        lotSearch: `${item.sku ?? "—"} - ${item.productName ?? "—"} · Lô ${item.lotCode ?? "—"}`,
        systemQuantity,
        actualQuantity,
        ...reasonState,
        suggestions: [],
        searching: false,
      };
    });
  } else {
    items.value = [createEmptyItem()];
  }
};

watch(
  () => props.visible,
  (visible) => {
    if (visible) resetForm();
  }
);

watch(
  () => props.detail,
  () => {
    if (props.visible) resetForm();
  },
  { deep: true }
);

onBeforeUnmount(() => {
  timers.forEach((timer) => clearTimeout(timer));
  timers.clear();
});

const addItem = () => items.value.push(createEmptyItem());

const removeItem = (index: number) => {
  if (items.value.length <= 1) return;
  items.value.splice(index, 1);
};

const displayLot = (lot: InventoryLotListResponse) =>
  `${lot.sku} - ${lot.productName} · Lô ${lot.lotCode}`;

const clearSelectedLot = (row: EditableItem) => {
  row.inventoryLotId = null;
  row.sku = "";
  row.productName = "";
  row.lotCode = "";
  row.systemQuantity = 0;
  row.actualQuantity = null;
  row.reasonPreset = "";
  row.customReason = "";
};

const loadSuggestions = async (index: number, keyword: string) => {
  const row = items.value[index];
  if (!row) return;

  row.searching = true;

  try {
    row.suggestions = await stockAdjustmentService.searchLots(keyword);
  } catch {
    row.suggestions = [];
  } finally {
    row.searching = false;
  }
};

const searchLot = (index: number, event: Event) => {
  const row = items.value[index];
  if (!row) return;

  const value = (event.target as HTMLInputElement).value;
  row.lotSearch = value;

  if (
    row.inventoryLotId &&
    value !== `${row.sku} - ${row.productName} · Lô ${row.lotCode}`
  ) {
    clearSelectedLot(row);
  }

  const oldTimer = timers.get(index);
  if (oldTimer) clearTimeout(oldTimer);

  const timer = setTimeout(() => {
    loadSuggestions(index, row.lotSearch);
  }, 250);

  timers.set(index, timer);
};

const openLotSuggestions = async (index: number) => {
  const row = items.value[index];
  if (!row || row.suggestions.length > 0) return;
  await loadSuggestions(index, "");
};

const selectLot = (index: number, lot: InventoryLotListResponse) => {
  const row = items.value[index];
  if (!row) return;

  row.inventoryLotId = lot.id;
  row.sku = lot.sku;
  row.productName = lot.productName;
  row.lotCode = lot.lotCode;
  row.lotSearch = displayLot(lot);
  row.systemQuantity = Number(lot.quantityOnHand ?? 0);
  row.actualQuantity = null;
  row.reasonPreset = "";
  row.customReason = "";
  row.suggestions = [];
};

const closeSuggestionsLater = (index: number) => {
  setTimeout(() => {
    const row = items.value[index];
    if (row) row.suggestions = [];
  }, 180);
};

const differenceOf = (row: EditableItem): number | null => {
  if (
    row.actualQuantity === null ||
    row.actualQuantity === undefined ||
    !Number.isFinite(Number(row.actualQuantity))
  ) {
    return null;
  }

  return Number(row.actualQuantity) - Number(row.systemQuantity ?? 0);
};


const reasonOptionsOf = (row: EditableItem): readonly string[] =>
  reasonsForDifference(differenceOf(row));

const resolveReason = (row: EditableItem): string => {
  const preset = String(row.reasonPreset ?? "").trim();

  if (preset === OTHER_REASON) {
    return String(row.customReason ?? "").trim();
  }

  return preset;
};

const actualQuantityChanged = (row: EditableItem) => {
  const difference = differenceOf(row);

  if (difference === null || difference === 0) {
    row.reasonPreset = "";
    row.customReason = "";
    return;
  }

  const preset = String(row.reasonPreset ?? "").trim();

  if (
    preset &&
    preset !== OTHER_REASON &&
    !reasonOptionsOf(row).includes(preset as never)
  ) {
    row.reasonPreset = "";
    row.customReason = "";
  }
};

const resultLabel = (row: EditableItem) => {
  const difference = differenceOf(row);

  if (difference === null) return "Chưa nhập tồn thực tế";
  if (difference > 0) return `Điều chỉnh tăng ${difference}`;
  if (difference < 0) return `Điều chỉnh giảm ${Math.abs(difference)}`;
  return "Khớp tồn";
};

const differenceClass = (row: EditableItem) => {
  const difference = differenceOf(row);

  if (difference === null || difference === 0) return "difference-neutral";
  return difference > 0 ? "difference-up" : "difference-down";
};

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const validate = async () => {
  if (String(note.value ?? "").trim().length > 1000) {
    await Swal.fire(
      "Dữ liệu không hợp lệ",
      "Ghi chú phiếu không được vượt quá 1000 ký tự.",
      "warning"
    );
    return false;
  }

  if (!items.value.length) {
    await Swal.fire(
      "Dữ liệu không hợp lệ",
      "Phiếu kiểm kê phải có ít nhất một lô hàng.",
      "warning"
    );
    return false;
  }

  const lotIds = new Set<number>();

  for (let index = 0; index < items.value.length; index++) {
    const row = items.value[index];
    const line = index + 1;

    if (!row?.inventoryLotId || row.inventoryLotId <= 0) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: vui lòng chọn lô hàng.`,
        "warning"
      );
      return false;
    }

    if (lotIds.has(row.inventoryLotId)) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: lô hàng bị trùng trong cùng phiếu.`,
        "warning"
      );
      return false;
    }
    lotIds.add(row.inventoryLotId);

    if (
      row.actualQuantity === null ||
      !Number.isInteger(Number(row.actualQuantity)) ||
      Number(row.actualQuantity) < 0
    ) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: số lượng thực tế phải là số nguyên lớn hơn hoặc bằng 0.`,
        "warning"
      );
      return false;
    }

    const reason = resolveReason(row);

    if (reason.length > 500) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: lý do không được vượt quá 500 ký tự.`,
        "warning"
      );
      return false;
    }

    const difference = differenceOf(row);

    if (difference !== null && difference !== 0 && !reason) {
      await Swal.fire(
        "Thiếu lý do chênh lệch",
        `Dòng ${line}: lô ${row.lotCode} có chênh lệch ${
          Number(difference) > 0 ? "+" : ""
        }${difference}, bắt buộc nhập lý do.`,
        "warning"
      );
      return false;
    }
  }

  return true;
};

const submit = async () => {
  if (!(await validate())) return;

  const payload: StockAdjustmentSaveRequest = {
    note: String(note.value ?? "").trim() || null,
    items: items.value.map(
      (row): StockAdjustmentItemRequest => ({
        inventoryLotId: Number(row.inventoryLotId),
        actualQuantity: Number(row.actualQuantity),
        reason: resolveReason(row) || null,
      })
    ),
  };

  emit("save", payload);
};

const close = () => {
  if (!props.saving) emit("close");
};
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="sa-backdrop">
      <div class="sa-dialog">
        <div class="sa-header">
          <div>
            <h3>{{ title }}</h3>
            <p>
              Chọn lô đã có trong kho, nhập số lượng đếm thực tế. Hệ thống tự
              tính chênh lệch; không chọn thủ công tăng/giảm tồn.
            </p>
          </div>
          <button
            type="button"
            class="icon-btn"
            :disabled="saving"
            @click="close"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <form @submit.prevent="submit">
          <div class="general-grid">
            <div class="field">
              <label>Loại nghiệp vụ</label>
              <input value="Kiểm kê thực tế" readonly />
            </div>

            <div class="field wide-3">
              <label>Ghi chú</label>
              <textarea
                v-model="note"
                maxlength="1000"
                rows="2"
                placeholder="Ghi chú chung cho phiếu kiểm kê..."
              ></textarea>
            </div>
          </div>

          <div class="items-head">
            <div>
              <h4>Đối chiếu tồn theo lô</h4>
              <p>
                Tồn hệ thống là số lượng tại thời điểm lập/sửa phiếu. Tồn thực
                tế là số lượng bạn kiểm đếm.
              </p>
            </div>
            <button type="button" class="secondary-btn" @click="addItem">
              <i class="bi bi-plus-lg"></i> Thêm lô
            </button>
          </div>

          <div class="items-wrap">
            <div v-for="(row, index) in items" :key="index" class="item-card">
              <div class="item-title">
                <strong>Lô kiểm kê {{ index + 1 }}</strong>
                <button
                  type="button"
                  class="remove-btn"
                  :disabled="items.length <= 1"
                  @click="removeItem(index)"
                >
                  <i class="bi bi-trash"></i>
                </button>
              </div>

              <div class="item-grid">
                <div class="field lot-field">
                  <label>Sản phẩm / lô hàng <span>*</span></label>
                  <div class="lot-wrap">
                    <input
                      :value="row.lotSearch"
                      placeholder="Tìm theo SKU, sản phẩm hoặc mã lô..."
                      autocomplete="off"
                      @input="searchLot(index, $event)"
                      @focus="openLotSuggestions(index)"
                      @click="openLotSuggestions(index)"
                      @blur="closeSuggestionsLater(index)"
                    />

                    <div v-if="row.searching" class="lot-loading">
                      Đang tìm lô...
                    </div>

                    <div
                      v-else-if="row.suggestions.length"
                      class="lot-dropdown"
                    >
                      <button
                        v-for="option in row.suggestions"
                        :key="option.id"
                        type="button"
                        @mousedown.prevent="selectLot(index, option)"
                      >
                        <strong>{{ option.sku }} · Lô {{ option.lotCode }}</strong>
                        <span>{{ option.productName }}</span>
                        <small>
                          Tồn hệ thống:
                          {{ formatNumber(option.quantityOnHand) }}
                          <template v-if="option.isExpired"> · Đã hết hạn</template>
                        </small>
                      </button>
                    </div>
                  </div>

                  <small v-if="row.inventoryLotId" class="selected-lot">
                    <i class="bi bi-check-circle-fill"></i>
                    {{ row.sku }} · {{ row.productName }} · Lô {{ row.lotCode }}
                  </small>
                </div>

                <div class="field">
                  <label>Tồn hệ thống</label>
                  <input :value="formatNumber(row.systemQuantity)" readonly />
                </div>

                <div class="field">
                  <label>Tồn thực tế <span>*</span></label>
                  <input
                    v-model.number="row.actualQuantity"
                    type="number"
                    min="0"
                    step="1"
                    inputmode="numeric"
                    placeholder="Nhập số đếm thực tế"
                    @input="actualQuantityChanged(row)"
                  />
                </div>

                <div class="result-card" :class="differenceClass(row)">
                  <span>Chênh lệch</span>
                  <strong>
                    {{
                      differenceOf(row) === null
                        ? "—"
                        : `${Number(differenceOf(row)) > 0 ? "+" : ""}${differenceOf(row)}`
                    }}
                  </strong>
                  <small>{{ resultLabel(row) }}</small>
                </div>

                <div class="field reason-field">
                  <label>
                    Lý do
                    <span v-if="differenceOf(row) !== null && differenceOf(row) !== 0">
                      *
                    </span>
                  </label>

                  <select
                    v-model="row.reasonPreset"
                    :disabled="differenceOf(row) === null || differenceOf(row) === 0"
                  >
                    <option value="">
                      {{
                        differenceOf(row) === null
                          ? "Nhập tồn thực tế trước"
                          : differenceOf(row) === 0
                            ? "Không bắt buộc khi tồn khớp"
                            : "-- Chọn lý do chênh lệch --"
                      }}
                    </option>

                    <option
                      v-for="reason in reasonOptionsOf(row)"
                      :key="reason"
                      :value="reason"
                    >
                      {{ reason }}
                    </option>

                    <option
                      v-if="differenceOf(row) !== null && differenceOf(row) !== 0"
                      :value="OTHER_REASON"
                    >
                      Khác
                    </option>
                  </select>

                  <textarea
                    v-if="row.reasonPreset === OTHER_REASON"
                    v-model="row.customReason"
                    maxlength="500"
                    rows="2"
                    placeholder="Nhập lý do cụ thể..."
                  ></textarea>
                </div>
              </div>
            </div>
          </div>

          <div class="sa-footer">
            <button
              type="button"
              class="secondary-btn"
              :disabled="saving"
              @click="close"
            >
              Đóng
            </button>
            <button type="submit" class="primary-btn" :disabled="saving">
              {{
                saving
                  ? "Đang lưu..."
                  : isEdit
                    ? "Cập nhật phiếu"
                    : "Lưu tạm"
              }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.sa-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow-y: auto;
  padding: 24px;
  background: rgba(15, 23, 42, 0.5);
}

.sa-dialog {
  width: min(1180px, 100%);
  margin: 20px auto;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.sa-header,
.sa-footer,
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
}

.sa-header {
  border-bottom: 1px solid #e5e7eb;
}

.sa-footer {
  justify-content: flex-end;
  border-top: 1px solid #e5e7eb;
}

.sa-header h3,
.items-head h4 {
  margin: 0;
}

.sa-header p,
.items-head p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.general-grid,
.item-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.general-grid {
  padding: 20px 22px;
  border-bottom: 1px solid #eee;
}

.items-wrap {
  padding: 0 22px 22px;
}

.item-card {
  margin-top: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
}

.item-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wide-3 {
  grid-column: span 3;
}

.lot-field,
.reason-field {
  grid-column: span 2;
}

.field label {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.field label span {
  color: #dc2626;
}

.field input,
.field select,
.field textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  outline: none;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: #111827;
}

.field select:disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.field input[readonly] {
  background: #f9fafb;
  color: #4b5563;
  font-weight: 600;
}

.lot-wrap {
  position: relative;
}

.lot-loading,
.lot-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  left: 0;
  z-index: 30;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
}

.lot-loading {
  padding: 10px;
  color: #6b7280;
}

.lot-dropdown {
  max-height: 280px;
  overflow-y: auto;
}

.lot-dropdown button {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-bottom: 1px solid #f3f4f6;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.lot-dropdown button:hover {
  background: #f9fafb;
}

.lot-dropdown span,
.lot-dropdown small {
  color: #6b7280;
  font-size: 12px;
}

.selected-lot {
  color: #047857;
  font-size: 12px;
}

.result-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  min-height: 78px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.result-card span,
.result-card small {
  font-size: 12px;
}

.result-card strong {
  font-size: 18px;
}

.difference-neutral {
  background: #f9fafb;
  color: #4b5563;
}

.difference-up {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #047857;
}

.difference-down {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.primary-btn,
.secondary-btn,
.icon-btn,
.remove-btn {
  border-radius: 8px;
  cursor: pointer;
}

.primary-btn {
  padding: 10px 16px;
  border: 0;
  background: #111827;
  color: #fff;
}

.secondary-btn {
  padding: 9px 14px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

.icon-btn,
.remove-btn {
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
}

.remove-btn {
  color: #dc2626;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .general-grid,
  .item-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .wide-3,
  .lot-field,
  .reason-field {
    grid-column: span 2;
  }
}

@media (max-width: 600px) {
  .sa-backdrop {
    padding: 0;
  }

  .sa-dialog {
    min-height: 100%;
    margin: 0;
    border-radius: 0;
  }

  .general-grid,
  .item-grid {
    grid-template-columns: 1fr;
  }

  .wide-3,
  .lot-field,
  .reason-field {
    grid-column: span 1;
  }
}

:global(.swal2-container) {
  z-index: 1000000 !important;
}
</style>
