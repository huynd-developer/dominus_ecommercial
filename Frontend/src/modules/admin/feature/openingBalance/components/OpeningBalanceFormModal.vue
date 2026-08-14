<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import Swal from "sweetalert2";
import openingBalanceService from "../services/opening-balance.service";
import type {
  InventorySkuOption,
  OpeningBalanceDetailResponse,
  OpeningBalanceItemRequest,
  OpeningBalanceSaveRequest,
} from "../types/opening-balance.type";

interface EditableItem {
  productVariantId: number;
  sku: string;
  productName: string;
  capacityValue: number | null;
  bottleTypeName: string | null;
  quantity: number | null;
  manufacturedDate: string;
  receivedDate: string;
  expirationDate: string;
  note: string;
}

const props = defineProps<{
  visible: boolean;
  detail?: OpeningBalanceDetailResponse | null;
  saving?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "save", payload: OpeningBalanceSaveRequest): void;
}>();

/**
 * Ghi chú vẫn được giữ trong state để khi sửa phiếu cũ không làm mất dữ liệu.
 * UI không hiển thị theo yêu cầu.
 */
const note = ref("");

const items = ref<EditableItem[]>([]);

const skuKeyword = ref("");
const skuOptions = ref<InventorySkuOption[]>([]);
const loadingSku = ref(false);
const skuLoadError = ref("");

const isEdit = computed(() => Boolean(props.detail?.id));

const title = computed(() =>
  isEdit.value
    ? `Sửa phiếu tồn đầu kỳ ${props.detail?.receiptNo ?? ""}`
    : "Khởi tạo tồn đầu kỳ"
);

const localToday = () => {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
};

const formatCapacity = (
  value: number | null | undefined
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

const resetForm = () => {
  // Giữ dữ liệu cũ khi edit, chỉ ẩn khỏi UI.
  note.value = props.detail?.note ?? "";
  skuKeyword.value = "";
  skuLoadError.value = "";

  if (props.detail?.items?.length) {
    items.value = props.detail.items.map((item) => ({
      productVariantId: item.productVariantId,
      sku: item.sku,
      productName: item.productName,
      capacityValue: item.capacityValue ?? null,
      bottleTypeName: item.bottleTypeName ?? null,
      quantity: item.quantity,
      manufacturedDate: item.manufacturedDate ?? "",
      // Ngày nhận bị ẩn khỏi UI nhưng vẫn giữ đúng dữ liệu cũ khi sửa.
      receivedDate: item.receivedDate || localToday(),
      expirationDate: item.expirationDate,
      // Ghi chú dòng bị ẩn nhưng vẫn giữ dữ liệu cũ khi sửa.
      note: item.note ?? "",
    }));
  } else {
    items.value = [];
  }
};

const loadSkuOptions = async () => {
  loadingSku.value = true;
  skuLoadError.value = "";

  try {
    skuOptions.value = await openingBalanceService.searchSku(
      skuKeyword.value.trim()
    );

    const optionMap = new Map(
      skuOptions.value.map((option) => [
        option.productVariantId,
        option,
      ])
    );

    items.value = items.value.map((item) => {
      const option = optionMap.get(item.productVariantId);

      if (!option) {
        return item;
      }

      return {
        ...item,
        capacityValue:
          option.capacityValue ?? item.capacityValue ?? null,
        bottleTypeName:
          option.bottleTypeName ?? item.bottleTypeName ?? null,
      };
    });
  } catch {
    skuOptions.value = [];
    skuLoadError.value = "Không thể tải danh sách SKU.";
  } finally {
    loadingSku.value = false;
  }
};

const searchSku = async () => {
  await loadSkuOptions();
};

const clearSkuSearch = async () => {
  skuKeyword.value = "";
  await loadSkuOptions();
};

watch(
  () => props.visible,
  async (visible) => {
    if (visible) {
      resetForm();
      await loadSkuOptions();
      await nextTick();
    }
  }
);

watch(
  () => props.detail,
  () => {
    if (props.visible) {
      resetForm();
    }
  },
  { deep: true }
);

/**
 * Tồn đầu kỳ có thể có nhiều lô cho cùng một SKU.
 * Vì vậy bấm cùng SKU nhiều lần sẽ thêm nhiều dòng lô.
 */
const selectedCountForSku = (productVariantId: number) =>
  items.value.filter(
    (item) => item.productVariantId === productVariantId
  ).length;

const addSkuLot = (option: InventorySkuOption) => {
  items.value.push({
    productVariantId: option.productVariantId,
    sku: option.sku,
    productName: option.productName,
    capacityValue: option.capacityValue ?? null,
    bottleTypeName: option.bottleTypeName ?? null,
    quantity: null,
    manufacturedDate: "",
    // Ẩn khỏi UI, tự ghi nhận ngày hiện tại cho lô mới.
    receivedDate: localToday(),
    expirationDate: "",
    note: "",
  });
};

const removeItem = (index: number) => {
  items.value.splice(index, 1);
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

  if (!items.value.length) {
    await Swal.fire(
      "Dữ liệu không hợp lệ",
      "Phiếu tồn đầu kỳ phải có ít nhất một sản phẩm.",
      "warning"
    );

    return false;
  }

  for (let index = 0; index < items.value.length; index++) {
    const row = items.value[index];
    const line = index + 1;

    if (!row) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Không tìm thấy dữ liệu tại dòng ${line}.`,
        "warning"
      );

      return false;
    }

    if (!row.productVariantId || row.productVariantId <= 0) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: vui lòng chọn SKU.`,
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
        `Dòng ${line}: số lượng thực tế phải là số nguyên lớn hơn 0.`,
        "warning"
      );

      return false;
    }

    if (!row.receivedDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: ngày nhận không được để trống.`,
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
        `Dòng ${line}: hạn sử dụng phải lớn hơn hoặc bằng ngày nhận.`,
        "warning"
      );

      return false;
    }

    if (row.manufacturedDate && row.manufacturedDate > row.receivedDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: ngày sản xuất phải nhỏ hơn hoặc bằng ngày nhận.`,
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
  }

  return true;
};

const submit = async () => {
  if (!(await validate())) return;

  const payload: OpeningBalanceSaveRequest = {
    // Ghi chú bị ẩn nhưng vẫn bảo toàn dữ liệu cũ khi sửa.
    note: note.value.trim() || null,
    items: items.value.map(
      (row): OpeningBalanceItemRequest => ({
        productVariantId: Number(row.productVariantId),
        quantity: Number(row.quantity),
        manufacturedDate: row.manufacturedDate || null,
        receivedDate: row.receivedDate,
        expirationDate: row.expirationDate,
        // Ghi chú dòng bị ẩn nhưng vẫn bảo toàn dữ liệu cũ khi sửa.
        note: row.note.trim() || null,
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
    <div v-if="visible" class="ob-backdrop">
      <div class="ob-dialog">
        <div class="ob-header">
          <div>
            <h3>{{ title }}</h3>
            <p>
              Chọn SKU và khai báo số lượng thực tế, ngày sản xuất,
              hạn sử dụng. Mã lô và ngày ghi nhận được hệ thống tự sinh.
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
          <!--
            Đã ẩn:
            - Loại nghiệp vụ
            - Ghi chú phiếu
          -->

          <section class="sku-picker-section">
            <div class="section-head">
              <div>
                <h4>Chọn sản phẩm / SKU</h4>
                <p>
                  Chọn SKU để thêm lô tồn đầu kỳ. Có thể chọn cùng SKU nhiều
                  lần nếu thực tế có nhiều lô khác nhau.
                </p>
              </div>

              <div class="selected-badge">
                Đã thêm {{ items.length }} lô
              </div>
            </div>

            <div class="sku-search">
              <div class="search-input-wrap">
                <i class="bi bi-search"></i>

                <input
                  v-model="skuKeyword"
                  type="text"
                  maxlength="100"
                  placeholder="Tìm theo SKU hoặc tên sản phẩm..."
                  @keyup.enter.prevent="searchSku"
                />

                <button
                  v-if="skuKeyword"
                  type="button"
                  class="clear-search-btn"
                  aria-label="Xóa tìm kiếm"
                  @click="clearSkuSearch"
                >
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>

              <button
                type="button"
                class="search-btn"
                :disabled="loadingSku"
                @click="searchSku"
              >
                {{ loadingSku ? "Đang tìm..." : "Tìm kiếm" }}
              </button>
            </div>

            <div v-if="skuLoadError" class="sku-error">
              {{ skuLoadError }}
            </div>

            <div v-if="loadingSku" class="sku-state">
              Đang tải danh sách SKU...
            </div>

            <div
              v-else-if="skuOptions.length === 0"
              class="sku-state"
            >
              Không tìm thấy SKU phù hợp.
            </div>

            <div v-else class="sku-grid">
              <button
                v-for="option in skuOptions"
                :key="option.productVariantId"
                type="button"
                class="sku-card"
                @click="addSkuLot(option)"
              >
                <span class="sku-card-add">
                  <i class="bi bi-plus-lg"></i>
                </span>

                <strong>{{ option.productName }}</strong>

                <span
                  v-if="variantLabel(option)"
                  class="variant-info"
                >
                  {{ variantLabel(option) }}
                </span>

                <span class="sku-code">{{ option.sku }}</span>

                <span
                  v-if="selectedCountForSku(option.productVariantId) > 0"
                  class="sku-added-count"
                >
                  Đã thêm {{ selectedCountForSku(option.productVariantId) }} lô
                </span>
              </button>
            </div>
          </section>

          <section class="selected-section">
            <div class="section-head selected-head">
              <div>
                <h4>Tồn thực tế theo từng lô ({{ items.length }})</h4>
                <p>
                  Mỗi dòng là một lô tồn đầu kỳ của một SKU. Cùng SKU được phép
                  có nhiều lô; mã lô được hệ thống tự sinh.
                </p>
              </div>
            </div>

            <div
              v-if="items.length === 0"
              class="selected-empty"
            >
              Chưa có SKU nào được thêm vào phiếu tồn đầu kỳ.
            </div>

            <div v-else class="items-wrap">
              <div
                v-for="(row, index) in items"
                :key="`${row.productVariantId}-${index}`"
                class="item-card"
              >
                <div class="item-title">
                  <div class="item-product">
                    <div class="item-title-line">
                      <strong>Lô {{ index + 1 }} · {{ row.productName }}</strong>

                      <span
                        v-if="variantLabel(row)"
                        class="variant-info"
                      >
                        {{ variantLabel(row) }}
                      </span>
                    </div>

                    <span class="sku-code">{{ row.sku }}</span>
                  </div>

                  <button
                    type="button"
                    class="remove-btn"
                    title="Xóa lô khỏi phiếu"
                    @click="removeItem(index)"
                  >
                    <i class="bi bi-trash"></i>
                  </button>
                </div>

                <div class="item-grid">

                  <div class="field">
                    <label>Số lượng thực tế <span>*</span></label>
                    <input
                      v-model.number="row.quantity"
                      type="number"
                      min="1"
                      step="1"
                      inputmode="numeric"
                      placeholder="0"
                    />
                  </div>

                  <div class="field">
                    <label>Ngày sản xuất</label>
                    <input
                      v-model="row.manufacturedDate"
                      type="date"
                      :max="row.receivedDate"
                    />
                  </div>

                  <div class="field">
                    <label>Hạn sử dụng <span>*</span></label>
                    <input
                      v-model="row.expirationDate"
                      type="date"
                      :min="row.receivedDate"
                    />
                  </div>

                  <!--
                    Đã ẩn:
                    - Ngày nhận / ghi nhận
                    - Ghi chú dòng
                  -->
                </div>
              </div>
            </div>
          </section>

          <div class="ob-footer">
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
.ob-backdrop {
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

.ob-dialog {
  width: min(1240px, 100%);
  margin: 20px auto;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.ob-header,
.ob-footer,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.ob-header {
  padding: 18px 22px;
  border-bottom: 1px solid #e5e7eb;
}

.ob-footer {
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid #e5e7eb;
}

.ob-header h3,
.section-head h4 {
  margin: 0;
}

.ob-header p,
.section-head p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.sku-picker-section,
.selected-section {
  padding: 20px 22px;
}

.sku-picker-section {
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.selected-section {
  background: #fafafa;
}

.selected-badge {
  flex: 0 0 auto;
  padding: 7px 11px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.sku-search {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
}

.search-input-wrap > i {
  position: absolute;
  left: 12px;
  color: #9ca3af;
  pointer-events: none;
}

.search-input-wrap input {
  box-sizing: border-box;
  width: 100%;
  height: 42px;
  padding: 0 40px 0 38px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  outline: none;
}

.search-input-wrap input:focus {
  border-color: #111827;
}

.clear-search-btn {
  position: absolute;
  right: 6px;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
}

.clear-search-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.search-btn {
  min-width: 108px;
  height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 8px;
  background: #111827;
  color: #fff;
  cursor: pointer;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  max-height: 260px;
  margin-top: 14px;
  overflow-y: auto;
}

.sku-card {
  position: relative;
  display: flex;
  min-height: 94px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 5px;
  padding: 13px 40px 13px 14px;
  border: 1px solid #dfe3e8;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: 0.15s ease;
}

.sku-card:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.05);
}

.sku-card strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.35;
}

.sku-card-add {
  position: absolute;
  top: 11px;
  right: 11px;
  display: flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border: 1px solid #d1d5db;
  border-radius: 50%;
  background: #fff;
  color: #374151;
  font-size: 12px;
}

.sku-added-count {
  margin-top: 2px;
  color: #047857;
  font-size: 11px;
  font-weight: 600;
}

.variant-info {
  color: #374151;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
}

.sku-code {
  color: #6b7280;
  font-size: 12px;
  word-break: break-word;
}

.sku-state,
.sku-error,
.selected-empty {
  margin-top: 14px;
  padding: 18px;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: #fafafa;
  color: #6b7280;
  text-align: center;
  font-size: 13px;
}

.sku-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.selected-head {
  margin-bottom: 14px;
}

.items-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.item-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.item-product {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.item-title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.item-title-line strong {
  color: #111827;
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.field label span {
  color: #dc2626;
}

.field input {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  outline: none;
}

.field input:focus {
  border-color: #111827;
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

@media (max-width: 1000px) {
  .sku-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .item-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .sku-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sku-search {
    flex-direction: column;
  }

  .search-btn {
    width: 100%;
  }
}

@media (max-width: 600px) {
  .ob-backdrop {
    padding: 0;
  }

  .ob-dialog {
    min-height: 100%;
    margin: 0;
    border-radius: 0;
  }

  .item-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .sku-grid {
    grid-template-columns: 1fr;
  }
}

:global(.swal2-container) {
  z-index: 1000000 !important;
}
</style>
