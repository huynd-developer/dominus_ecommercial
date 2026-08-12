<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import Swal from "sweetalert2";
import goodsReceiptService from "../services/goods-receipt.service";
import type {
  GoodsReceiptDetailResponse,
  GoodsReceiptItemRequest,
  GoodsReceiptSaveRequest,
  InventorySkuOption,
} from "../types/goods-receipt.type";

interface EditableItem {
  productVariantId: number | null;
  sku: string;
  productName: string;
  skuSearch: string;
  suggestions: InventorySkuOption[];
  searchingSku: boolean;
  lotCode: string;
  quantity: number | null;
  unitCost: number | null;
  manufacturedDate: string;
  receivedDate: string;
  expirationDate: string;
  note: string;
}

const props = defineProps<{
  visible: boolean;
  detail?: GoodsReceiptDetailResponse | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "save", payload: GoodsReceiptSaveRequest): void;
}>();

const note = ref("");
const items = ref<EditableItem[]>([]);
const isEdit = computed(() => Boolean(props.detail?.id));
const title = computed(() =>
  isEdit.value
    ? `Sửa phiếu ${props.detail?.receiptNo ?? ""}`
    : "Tạo phiếu nhập kho"
);

const today = () => new Date().toISOString().slice(0, 10);

const createEmptyItem = (): EditableItem => ({
  productVariantId: null,
  sku: "",
  productName: "",
  skuSearch: "",
  suggestions: [],
  searchingSku: false,
  lotCode: "",
  quantity: null,
  unitCost: null,
  manufacturedDate: "",
  receivedDate: today(),
  expirationDate: "",
  note: "",
});

const resetForm = () => {
  note.value = props.detail?.note ?? "";

  if (props.detail?.items?.length) {
    items.value = props.detail.items.map((item) => ({
      productVariantId: item.productVariantId,
      sku: item.sku,
      productName: item.productName,
      skuSearch: `${item.sku} - ${item.productName}`,
      suggestions: [],
      searchingSku: false,
      lotCode: item.lotCode,
      quantity: item.quantity,
      unitCost: item.unitCost,
      manufacturedDate: item.manufacturedDate ?? "",
      receivedDate: item.receivedDate,
      expirationDate: item.expirationDate,
      note: item.note ?? "",
    }));
  } else {
    items.value = [createEmptyItem()];
  }
};

watch(
  () => props.visible,
  async (visible) => {
    if (visible) {
      resetForm();
      await nextTick();
    }
  }
);

watch(
  () => props.detail,
  () => {
    if (props.visible) resetForm();
  },
  { deep: true }
);

const addItem = () => items.value.push(createEmptyItem());
const removeItem = (index: number) => {
  if (items.value.length > 1) items.value.splice(index, 1);
};

const openSkuSuggestions = async (index: number) => {
  const row = items.value[index];
  if (!row || row.suggestions.length) return;

  row.searchingSku = true;
  try {
    row.suggestions = await goodsReceiptService.searchSku("");
  } catch {
    row.suggestions = [];
  } finally {
    row.searchingSku = false;
  }
};

const selectSku = (index: number, option: InventorySkuOption) => {
  const row = items.value[index];
  if (!row) return;

  row.productVariantId = option.productVariantId;
  row.sku = option.sku;
  row.productName = option.productName;
  row.skuSearch = `${option.sku} - ${option.productName}`;
  row.suggestions = [];
};

const closeSuggestionsLater = (index: number) => {
  setTimeout(() => {
    const row = items.value[index];
    if (row) row.suggestions = [];
  }, 180);
};

const validate = async () => {
  if (note.value.length > 1000) {
    await Swal.fire(
      "Dữ liệu không hợp lệ",
      "Ghi chú phiếu không được vượt quá 1000 ký tự.",
      "warning"
    );
    return false;
  }

  const duplicateSet = new Set<string>();

  for (let index = 0; index < items.value.length; index++) {
    const row = items.value[index];

    if (!row) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Không tìm thấy dữ liệu sản phẩm tại dòng ${index + 1}.`,
        "warning"
      );
      return false;
    }

    const line = index + 1;
    const lotCode = row.lotCode.trim();

    if (!row.productVariantId || row.productVariantId <= 0) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: vui lòng chọn SKU.`,
        "warning"
      );
      return false;
    }
    if (!lotCode) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: mã lô không được để trống.`,
        "warning"
      );
      return false;
    }
    if (lotCode.length > 100) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: mã lô không được vượt quá 100 ký tự.`,
        "warning"
      );
      return false;
    }
    if (
      row.quantity == null ||
      !Number.isInteger(Number(row.quantity)) ||
      Number(row.quantity) <= 0
    ) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: số lượng phải là số nguyên lớn hơn 0.`,
        "warning"
      );
      return false;
    }
    if (row.unitCost != null && Number(row.unitCost) < 0) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: đơn giá nhập phải lớn hơn hoặc bằng 0.`,
        "warning"
      );
      return false;
    }
    if (!row.receivedDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: ngày nhận hàng không được để trống.`,
        "warning"
      );
      return false;
    }
    if (!row.expirationDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: hạn sử dụng không được để trống.`,
        "warning"
      );
      return false;
    }
    if (row.expirationDate < row.receivedDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: hạn sử dụng phải lớn hơn hoặc bằng ngày nhận hàng.`,
        "warning"
      );
      return false;
    }
    if (row.manufacturedDate && row.manufacturedDate > row.receivedDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: ngày sản xuất phải nhỏ hơn hoặc bằng ngày nhận hàng.`,
        "warning"
      );
      return false;
    }
    if (row.manufacturedDate && row.manufacturedDate > row.expirationDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: ngày sản xuất phải nhỏ hơn hoặc bằng hạn sử dụng.`,
        "warning"
      );
      return false;
    }
    if (row.note.length > 500) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: ghi chú không được vượt quá 500 ký tự.`,
        "warning"
      );
      return false;
    }

    const duplicateKey = `${row.productVariantId}|${lotCode.toUpperCase()}`;
    if (duplicateSet.has(duplicateKey)) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: không được trùng SKU + Mã lô trong cùng phiếu.`,
        "warning"
      );
      return false;
    }
    duplicateSet.add(duplicateKey);
  }

  return true;
};

const submit = async () => {
  if (!(await validate())) return;

  const payload: GoodsReceiptSaveRequest = {
    receiptType: "NORMAL_RECEIPT",
    note: note.value.trim() || null,
    items: items.value.map(
      (row): GoodsReceiptItemRequest => ({
        productVariantId: Number(row.productVariantId),
        lotCode: row.lotCode.trim(),
        quantity: Number(row.quantity),
        unitCost: row.unitCost == null ? null : Number(row.unitCost),
        manufacturedDate: row.manufacturedDate || null,
        receivedDate: row.receivedDate,
        expirationDate: row.expirationDate,
        note: row.note.trim() || null,
      })
    ),
  };

  emit("save", payload);
};

const close = () => {
  if (!props.saving) emit("close");
};

const formatMoney = (value: number | null) =>
  value == null ? "" : new Intl.NumberFormat("vi-VN").format(value);

const onUnitCostInput = (index: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const raw = input.value.replace(/\D/g, "");
  const row = items.value[index];
  if (!row) return;
  row.unitCost = raw ? Number(raw) : null;
  input.value = raw ? formatMoney(Number(raw)) : "";
};
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="gr-backdrop">
      <div class="gr-dialog">
        <div class="gr-header">
          <div>
            <h3>{{ title }}</h3>
            <p>Phiếu nhập thường · Tạo mới luôn ở trạng thái lưu tạm.</p>
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
              <label>Loại phiếu</label>
              <input class="readonly-field" value="Nhập kho" readonly />
            </div>
            <div class="field wide-3">
              <label>Ghi chú</label>
              <textarea
                v-model="note"
                maxlength="1000"
                rows="2"
                placeholder="Ghi chú phiếu nhập..."
              ></textarea>
            </div>
          </div>

          <div class="items-head">
            <div>
              <h4>Danh sách sản phẩm</h4>
              <p>Mỗi dòng là một SKU + mã lô.</p>
            </div>
            <button type="button" class="secondary-btn" @click="addItem">
              <i class="bi bi-plus-lg"></i> Thêm dòng
            </button>
          </div>

          <div class="items-wrap">
            <div v-for="(row, index) in items" :key="index" class="item-card">
              <div class="item-title">
                <strong>Sản phẩm {{ index + 1 }}</strong>
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
                <div class="field sku-field">
                  <label>SKU <span>*</span></label>
                  <div class="sku-wrap">
                    <input
                      :value="row.skuSearch"
                      readonly
                      placeholder="Bấm để chọn SKU..."
                      autocomplete="off"
                      @focus="openSkuSuggestions(index)"
                      @click="openSkuSuggestions(index)"
                      @blur="closeSuggestionsLater(index)"
                    />
                    <div v-if="row.searchingSku" class="sku-loading">
                      Đang tìm...
                    </div>
                    <div
                      v-else-if="row.suggestions.length"
                      class="sku-dropdown"
                    >
                      <button
                        v-for="option in row.suggestions"
                        :key="option.productVariantId"
                        type="button"
                        @mousedown.prevent="selectSku(index, option)"
                      >
                        <strong>{{ option.sku }}</strong>
                        <span>{{ option.productName }}</span>
                      </button>
                    </div>
                  </div>
                </div>

                <div class="field">
                  <label>Mã lô <span>*</span></label>
                  <input
                    v-model="row.lotCode"
                    maxlength="100"
                    placeholder="LOT-..."
                  />
                </div>
                <div class="field">
                  <label>Số lượng <span>*</span></label>
                  <input
                    v-model.number="row.quantity"
                    type="number"
                    min="1"
                    step="1"
                    inputmode="numeric"
                  />
                </div>
                <div class="field">
                  <label>Đơn giá nhập</label>
                  <input
                    :value="formatMoney(row.unitCost)"
                    inputmode="numeric"
                    placeholder="0"
                    @input="onUnitCostInput(index, $event)"
                  />
                </div>
                <div class="field">
                  <label>Ngày sản xuất</label>
                  <input v-model="row.manufacturedDate" type="date" />
                </div>
                <div class="field">
                  <label>Ngày nhận <span>*</span></label>
                  <input v-model="row.receivedDate" type="date" />
                </div>
                <div class="field">
                  <label>Hạn sử dụng <span>*</span></label>
                  <input v-model="row.expirationDate" type="date" />
                </div>
                <div class="field wide-2">
                  <label>Ghi chú dòng</label>
                  <input
                    v-model="row.note"
                    maxlength="500"
                    placeholder="Ghi chú..."
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="gr-footer">
            <button
              type="button"
              class="secondary-btn"
              :disabled="saving"
              @click="close"
            >
              Hủy
            </button>
            <button type="submit" class="primary-btn" :disabled="saving">
              {{
                saving ? "Đang lưu..." : isEdit ? "Cập nhật phiếu" : "Lưu tạm"
              }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.gr-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
  background: rgba(15, 23, 42, 0.5);
}
.gr-dialog {
  width: min(1180px, 100%);
  margin: 20px auto;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}
.gr-header,
.gr-footer,
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
}
.gr-header {
  border-bottom: 1px solid #e5e7eb;
}
.gr-footer {
  justify-content: flex-end;
  border-top: 1px solid #e5e7eb;
}
.gr-header h3,
.items-head h4 {
  margin: 0;
}
.gr-header p,
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
.wide-2,
.sku-field {
  grid-column: span 2;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.field label span {
  color: #dc2626;
}
.field input,
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
.field textarea:focus {
  border-color: #111827;
}
.field small {
  color: #6b7280;
  font-size: 12px;
}
.sku-wrap {
  position: relative;
}
.sku-loading,
.sku-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 20;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
}
.sku-loading {
  padding: 10px;
  color: #6b7280;
}
.sku-dropdown {
  max-height: 240px;
  overflow-y: auto;
}
.sku-dropdown button {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 9px 11px;
  border: 0;
  border-bottom: 1px solid #f3f4f6;
  background: #fff;
  text-align: left;
  cursor: pointer;
}
.sku-dropdown button:hover {
  background: #f9fafb;
}
.sku-dropdown span {
  font-size: 12px;
  color: #6b7280;
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
  .wide-2,
  .sku-field {
    grid-column: span 2;
  }
}
@media (max-width: 600px) {
  .gr-backdrop {
    padding: 0;
  }
  .gr-dialog {
    margin: 0;
    min-height: 100%;
    border-radius: 0;
  }
  .general-grid,
  .item-grid {
    grid-template-columns: 1fr;
  }
  .wide-3,
  .wide-2,
  .sku-field {
    grid-column: span 1;
  }
}
/* SweetAlert phải nằm trên modal phiếu nhập */
:global(.swal2-container) {
  z-index: 1000000 !important;
}
.readonly-field {
  background: #f3f4f6 !important;
  color: #6b7280;
  cursor: not-allowed;
}
</style>
