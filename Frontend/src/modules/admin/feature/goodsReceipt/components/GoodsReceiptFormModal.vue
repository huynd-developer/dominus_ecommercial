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
  rowKey: string;
  productVariantId: number;
  sku: string;
  productName: string;
  imageUrl: string | null;
  capacityValue: number | null;
  bottleTypeName: string | null;
  quantity: number | null;
  unitCost: number | null;
  manufacturedDate: string;
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

let rowSequence = 0;

const createRowKey = () => {
  rowSequence += 1;
  return `gr-row-${Date.now()}-${rowSequence}`;
};

const skuKeyword = ref("");
const skuOptions = ref<InventorySkuOption[]>([]);
const loadingSku = ref(false);
const skuLoadError = ref("");

const isEdit = computed(() => Boolean(props.detail?.id));

const title = computed(() =>
  isEdit.value
    ? `Sửa phiếu ${props.detail?.receiptNo ?? ""}`
    : "Tạo phiếu nhập kho"
);

const today = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
};

const effectiveReceivedDate = computed(() => {
  const createdAt = props.detail?.createdAt;

  if (createdAt && createdAt.length >= 10) {
    return createdAt.slice(0, 10);
  }

  return today();
});

const totalSku = computed(
  () => new Set(items.value.map((item) => item.productVariantId)).size
);

const totalLots = computed(() => items.value.length);

const totalQuantity = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.quantity ?? 0), 0)
);

const totalAmount = computed(() =>
  items.value.reduce(
    (sum, item) =>
      sum + Number(item.quantity ?? 0) * Number(item.unitCost ?? 0),
    0
  )
);

const lineTotal = (item: EditableItem) =>
  Number(item.quantity ?? 0) * Number(item.unitCost ?? 0);

const resetForm = () => {
  note.value = props.detail?.note ?? "";
  skuKeyword.value = "";
  skuLoadError.value = "";

  if (props.detail?.items?.length) {
    items.value = props.detail.items.map((item) => ({
      rowKey: `saved-${item.id}`,
      productVariantId: item.productVariantId,
      sku: item.sku,
      productName: item.productName,
      imageUrl: item.imageUrl ?? null,
      capacityValue: null,
      bottleTypeName: null,
      quantity: item.quantity,
      unitCost: item.unitCost,
      manufacturedDate: item.manufacturedDate ?? "",
      expirationDate: item.expirationDate,
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
    skuOptions.value = await goodsReceiptService.searchSku(
      skuKeyword.value.trim()
    );

    const optionMap = new Map(
      skuOptions.value.map((option) => [option.productVariantId, option])
    );

    items.value = items.value.map((item) => {
      const option = optionMap.get(item.productVariantId);

      if (!option) {
        return item;
      }

      return {
        ...item,
        capacityValue: option.capacityValue ?? item.capacityValue ?? null,
        bottleTypeName: option.bottleTypeName ?? item.bottleTypeName ?? null,
        imageUrl: option.imageUrl ?? item.imageUrl ?? null,
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

const isSkuSelected = (productVariantId: number) =>
  items.value.some((item) => item.productVariantId === productVariantId);

const addSkuLot = (option: InventorySkuOption) => {
  // Mỗi SKU chỉ được chọn một lần trong cùng một phiếu nhập.
  if (isSkuSelected(option.productVariantId)) {
    return;
  }

  items.value.push({
    rowKey: createRowKey(),
    productVariantId: option.productVariantId,
    sku: option.sku,
    productName: option.productName,
    imageUrl: option.imageUrl ?? null,
    capacityValue: option.capacityValue ?? null,
    bottleTypeName: option.bottleTypeName ?? null,
    quantity: null,
    unitCost: null,
    manufacturedDate: "",
    expirationDate: "",
    note: "",
  });
};

const removeItem = (rowKey: string) => {
  items.value = items.value.filter((item) => item.rowKey !== rowKey);
};

const validate = async () => {
  if (items.value.length === 0) {
    await Swal.fire(
      "Dữ liệu không hợp lệ",
      "Phiếu nhập phải có ít nhất một sản phẩm.",
      "warning"
    );

    return false;
  }

  const selectedVariantIds = new Set<number>();

  for (const row of items.value) {
    if (selectedVariantIds.has(row.productVariantId)) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `SKU ${
          row.sku || row.productVariantId
        } đã được chọn. Mỗi SKU chỉ được chọn một lần trong cùng một phiếu nhập.`,
        "warning"
      );

      return false;
    }

    selectedVariantIds.add(row.productVariantId);
  }

  for (let index = 0; index < items.value.length; index++) {
    const row = items.value[index];
    const line = index + 1;

    if (!row || !row.productVariantId || row.productVariantId <= 0) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: SKU không hợp lệ.`,
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

    if (!row.expirationDate) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: hạn sử dụng không được để trống.`,
        "warning"
      );
      return false;
    }

    if (row.expirationDate < effectiveReceivedDate.value) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: hạn sử dụng phải lớn hơn hoặc bằng ngày nhận hàng.`,
        "warning"
      );
      return false;
    }

    if (
      row.manufacturedDate &&
      row.manufacturedDate > effectiveReceivedDate.value
    ) {
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
        productVariantId: row.productVariantId,
        quantity: Number(row.quantity),
        unitCost: row.unitCost == null ? null : Number(row.unitCost),
        manufacturedDate: row.manufacturedDate || null,
        expirationDate: row.expirationDate,
        note: row.note.trim() || null,
      })
    ),
  };

  emit("save", payload);
};

const formatCapacity = (value: number | null | undefined) => {
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

const close = () => {
  if (!props.saving) {
    emit("close");
  }
};

const formatMoney = (value: number | null | undefined) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const onUnitCostInput = (index: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const raw = input.value.replace(/\D/g, "");
  const row = items.value[index];

  if (!row) return;

  row.unitCost = raw ? Number(raw) : null;
  input.value = raw ? formatMoney(Number(raw)) : "";
};

const onImageError = (event: Event) => {
  const image = event.currentTarget as HTMLImageElement;
  image.style.display = "none";
};
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="gr-backdrop">
      <div class="gr-dialog">
        <div class="gr-header">
          <div>
            <h3>{{ title }}</h3>
            <p>
              Chọn SKU và khai báo số lượng, giá, ngày sản xuất, hạn sử dụng.
              Mỗi dòng nhập được hệ thống tự sinh một mã lô riêng.
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
          <section class="sku-picker-section">
            <div class="section-head">
              <div>
                <h4>Chọn sản phẩm / SKU</h4>
                <p>
                  Bấm vào SKU để thêm vào phiếu. Mỗi SKU chỉ được chọn một lần
                  trong cùng một phiếu nhập.
                </p>
              </div>

              <div class="selected-badge">Đã chọn {{ totalSku }} SKU</div>
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

            <div v-else-if="skuOptions.length === 0" class="sku-state">
              Không tìm thấy SKU phù hợp.
            </div>

            <div v-else class="sku-grid">
              <button
                v-for="option in skuOptions"
                :key="option.productVariantId"
                type="button"
                class="sku-card"
                :class="{
                  selected: isSkuSelected(option.productVariantId),
                }"
                :disabled="isSkuSelected(option.productVariantId)"
                @click="addSkuLot(option)"
              >
                <span class="sku-card-check">
                  <i
                    v-if="isSkuSelected(option.productVariantId)"
                    class="bi bi-check-lg"
                  ></i>
                  <i v-else class="bi bi-plus-lg"></i>
                </span>

                <span class="sku-card-thumb" aria-hidden="true">
                  <i class="bi bi-image"></i>
                  <img
                    v-if="option.imageUrl"
                    :src="option.imageUrl"
                    :alt="option.productName"
                    loading="lazy"
                    @error="onImageError"
                  />
                </span>

                <strong>{{ option.productName }}</strong>

                <span v-if="variantLabel(option)" class="variant-info">
                  {{ variantLabel(option) }}
                </span>

                <span class="sku-code">{{ option.sku }}</span>

                <span
                  v-if="isSkuSelected(option.productVariantId)"
                  class="sku-added-count"
                >
                  Đã chọn
                </span>
              </button>
            </div>
          </section>

          <section class="selected-section">
            <div class="section-head selected-head">
              <div>
                <h4>Các lô nhập ({{ totalLots }})</h4>
                <p>
                  Mỗi dòng bên dưới sẽ tạo một lô riêng khi phiếu được duyệt.
                </p>
              </div>
            </div>

            <div v-if="items.length === 0" class="selected-empty">
              Chưa có SKU nào được chọn.
            </div>

            <div v-else class="table-wrapper">
              <table class="selected-table">
                <thead>
                  <tr>
                    <th class="image-column">Ảnh</th>
                    <th class="product-column">Sản phẩm / SKU</th>
                    <th class="quantity-column">Số lượng *</th>
                    <th class="cost-column">Đơn giá nhập</th>
                    <th class="date-column">Ngày sản xuất</th>
                    <th class="date-column">Hạn sử dụng *</th>
                    <th class="amount-column">Thành tiền</th>
                    <th class="action-column">Thao tác</th>
                  </tr>
                </thead>

                <tbody>
                  <tr v-for="(row, index) in items" :key="row.rowKey">
                    <td class="image-cell">
                      <div class="selected-thumb">
                        <i class="bi bi-image"></i>
                        <img
                          v-if="row.imageUrl"
                          :src="row.imageUrl"
                          :alt="row.productName"
                          loading="lazy"
                          @error="onImageError"
                        />
                      </div>
                    </td>

                    <td>
                      <div class="product-cell">
                        <strong>{{ row.productName }}</strong>

                        <span v-if="variantLabel(row)" class="variant-info">
                          {{ variantLabel(row) }}
                        </span>

                        <span class="sku-code">{{ row.sku }}</span>
                      </div>
                    </td>

                    <td>
                      <input
                        v-model.number="row.quantity"
                        class="table-input"
                        type="number"
                        min="1"
                        step="1"
                        inputmode="numeric"
                        placeholder="0"
                      />
                    </td>

                    <td>
                      <input
                        :value="
                          row.unitCost == null ? '' : formatMoney(row.unitCost)
                        "
                        class="table-input"
                        inputmode="numeric"
                        placeholder="0"
                        @input="onUnitCostInput(index, $event)"
                      />
                    </td>

                    <td>
                      <input
                        v-model="row.manufacturedDate"
                        class="table-input"
                        type="date"
                        :max="effectiveReceivedDate"
                      />
                    </td>

                    <td>
                      <input
                        v-model="row.expirationDate"
                        class="table-input"
                        type="date"
                        :min="effectiveReceivedDate"
                      />
                    </td>

                    <td class="line-total">
                      {{ formatMoney(lineTotal(row)) }} đ
                    </td>

                    <td class="action-cell">
                      <button
                        type="button"
                        class="remove-btn"
                        title="Xóa SKU khỏi phiếu"
                        @click="removeItem(row.rowKey)"
                      >
                        <i class="bi bi-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <div class="summary-bar">
            <div class="summary-item">
              <span>Tổng SKU</span>
              <strong>{{ totalSku }}</strong>
            </div>

            <div class="summary-item">
              <span>Tổng số lượng</span>
              <strong>{{ formatMoney(totalQuantity) }}</strong>
            </div>

            <div class="summary-item total-value">
              <span>Tổng giá trị</span>
              <strong>{{ formatMoney(totalAmount) }} đ</strong>
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
  width: min(1240px, 100%);
  margin: 20px auto;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.gr-header,
.gr-footer,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.gr-header {
  padding: 18px 22px;
  border-bottom: 1px solid #e5e7eb;
}

.gr-header h3,
.section-head h4 {
  margin: 0;
}

.gr-header p,
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
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  align-items: center;
  column-gap: 12px;
  row-gap: 5px;
  min-height: 96px;
  padding: 13px 40px 13px 13px;
  border: 1px solid #dfe3e8;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: 0.15s ease;
}

.sku-card-thumb {
  position: relative;
  grid-column: 1;
  grid-row: 1 / span 4;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
  color: #9ca3af;
}

.sku-card-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}

.sku-card-thumb i {
  font-size: 18px;
}

.sku-card > strong,
.sku-card > .variant-info,
.sku-card > .sku-code,
.sku-card > .sku-added-count {
  grid-column: 2;
}

.sku-card:hover:not(:disabled) {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.05);
}

.sku-card:disabled {
  opacity: 1;
}

.sku-card.selected {
  border-color: #65a30d;
  background: #f7fee7;
  cursor: default;
}

.sku-card strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.35;
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

.sku-added-count {
  margin-top: 2px;
  padding: 3px 7px;
  border-radius: 999px;
  background: #ecfccb;
  color: #3f6212;
  font-size: 11px;
  font-weight: 700;
}

.sku-card-check {
  position: absolute;
  top: 11px;
  right: 11px;
  display: flex;
  width: 21px;
  height: 21px;
  align-items: center;
  justify-content: center;
  border: 1px solid #d1d5db;
  border-radius: 50%;
  background: #fff;
  color: #65a30d;
  font-size: 13px;
}

.sku-card.selected .sku-card-check {
  border-color: #84cc16;
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

.table-wrapper {
  max-height: 330px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.selected-table {
  width: 100%;
  min-width: 1200px;
  border-collapse: collapse;
}

.selected-table th,
.selected-table td {
  padding: 11px 10px;
  border-bottom: 1px solid #eef0f2;
  vertical-align: middle;
}

.selected-table th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #f9fafb;
  color: #4b5563;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.selected-table tbody tr:last-child td {
  border-bottom: 0;
}

.image-column,
.image-cell {
  width: 78px;
  min-width: 78px;
  text-align: center !important;
}

.selected-thumb {
  position: relative;
  width: 56px;
  height: 56px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
  color: #9ca3af;
}

.selected-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}

.selected-thumb i {
  font-size: 18px;
}

.product-column {
  min-width: 210px;
}

.quantity-column {
  width: 110px;
}

.cost-column {
  width: 150px;
}

.date-column {
  width: 160px;
}

.amount-column {
  width: 150px;
}

.action-column {
  width: 74px;
  text-align: center !important;
}

.product-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-cell strong {
  color: #111827;
  font-size: 13px;
}

.product-cell span {
  color: #6b7280;
  font-size: 12px;
}

.table-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 38px;
  padding: 8px 9px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  background: #fff;
  outline: none;
}

.table-input:focus {
  border-color: #111827;
}

.line-total {
  color: #15803d;
  font-weight: 600;
  white-space: nowrap;
}

.action-cell {
  text-align: center;
}

.remove-btn {
  width: 34px;
  height: 34px;
  border: 1px solid #fecaca;
  border-radius: 50%;
  background: #fff;
  color: #dc2626;
  cursor: pointer;
}

.remove-btn:hover {
  background: #fef2f2;
}

.summary-bar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  border-top: 1px solid #e5e7eb;
  background: #e5e7eb;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 15px 22px;
  background: #fff;
}

.summary-item span {
  color: #6b7280;
  font-size: 12px;
}

.summary-item strong {
  color: #111827;
  font-size: 18px;
}

.summary-item.total-value strong {
  color: #15803d;
}

.gr-footer {
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}

.primary-btn,
.secondary-btn,
.icon-btn {
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

.icon-btn {
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
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

@media (max-width: 760px) {
  .gr-backdrop {
    padding: 0;
  }

  .gr-dialog {
    min-height: 100%;
    margin: 0;
    border-radius: 0;
  }

  .sku-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sku-search {
    flex-direction: column;
  }

  .search-btn {
    width: 100%;
  }

  .summary-bar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .sku-grid {
    grid-template-columns: 1fr;
  }
}

/* SweetAlert phải nằm trên modal phiếu nhập */
:global(.swal2-container) {
  z-index: 1000000 !important;
}
</style>
