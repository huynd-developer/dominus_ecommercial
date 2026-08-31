<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from "vue";
import Swal from "sweetalert2";
import stockAdjustmentService from "../services/stock-adjustment.service";

import type { InventoryLotListResponse } from "@/modules/admin/feature/inventoryLot/types/inventory-lot.type";
import type {
  StockAdjustmentDetailResponse,
  StockAdjustmentItemRequest,
  StockAdjustmentSaveRequest,
} from "../types/stock-adjustment.type";

interface EditableItem {
  inventoryLotId: number;
  sku: string;
  productName: string;
  imageUrl: string | null;
  capacityValue: number | null;
  bottleTypeName: string | null;
  lotCode: string;
  systemQuantity: number;
  actualQuantity: string;
  reasonPreset: string;
  customReason: string;
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

/**
 * Ghi chú vẫn được giữ trong state để khi sửa phiếu cũ không làm mất dữ liệu.
 * UI không hiển thị theo yêu cầu.
 */
const note = ref("");
const items = ref<EditableItem[]>([]);

const MAX_ACTUAL_QUANTITY = 1_000_000;
const MAX_REASON_LENGTH = 500;
const MAX_NOTE_LENGTH = 1000;

const lotKeyword = ref("");
const lotOptions = ref<InventoryLotListResponse[]>([]);
const loadingLots = ref(false);
const lotLoadError = ref("");

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

const selectedLotIds = computed(
  () => new Set(items.value.map((item) => item.inventoryLotId))
);

const resetForm = () => {
  // Giữ ghi chú cũ khi edit, chỉ ẩn khỏi UI.
  note.value = props.detail?.note ?? "";
  lotKeyword.value = "";
  lotLoadError.value = "";

  if (props.detail?.items?.length) {
    items.value = props.detail.items.map((item) => {
      const systemQuantity = Number(item.systemQuantity ?? 0);
      const actualQuantity = String(item.actualQuantity ?? 0);
      const difference = Number(actualQuantity) - systemQuantity;
      const reasonState = buildReasonState(item.reason, difference);

      return {
        inventoryLotId: Number(item.inventoryLotId),
        sku: item.sku ?? "",
        productName: item.productName ?? "",
        imageUrl: item.imageUrl ?? null,
        capacityValue: item.capacityValue ?? null,
        bottleTypeName: item.bottleTypeName ?? null,
        lotCode: item.lotCode ?? "",
        systemQuantity,
        actualQuantity,
        ...reasonState,
      };
    });
  } else {
    items.value = [];
  }
};

const loadLotOptions = async () => {
  loadingLots.value = true;
  lotLoadError.value = "";

  try {
    lotOptions.value = await stockAdjustmentService.searchLots(
      lotKeyword.value.trim()
    );
  } catch {
    lotOptions.value = [];
    lotLoadError.value = "Không thể tải danh sách lô hàng.";
  } finally {
    loadingLots.value = false;
  }
};

const searchLots = async () => {
  await loadLotOptions();
};

const clearLotSearch = async () => {
  lotKeyword.value = "";
  await loadLotOptions();
};

/**
 * Refresh danh sách lô khi user quay lại tab/window.
 *
 * Chỉ tải lại lotOptions.
 * KHÔNG reset form.
 * KHÔNG xóa lô đã chọn.
 * KHÔNG thay tồn thực tế/lý do user đang nhập.
 */
const refreshLotOptionsOnFocus = async () => {
  if (!props.visible || props.saving || loadingLots.value) {
    return;
  }

  await loadLotOptions();
};

const handleVisibilityChange = () => {
  if (document.visibilityState === "visible" && props.visible) {
    void refreshLotOptionsOnFocus();
  }
};

onMounted(() => {
  window.addEventListener("focus", refreshLotOptionsOnFocus);

  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onBeforeUnmount(() => {
  window.removeEventListener("focus", refreshLotOptionsOnFocus);

  document.removeEventListener("visibilitychange", handleVisibilityChange);
});

watch(
  () => props.visible,
  async (visible) => {
    if (visible) {
      resetForm();
      await loadLotOptions();
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

const isLotSelected = (inventoryLotId: number) =>
  selectedLotIds.value.has(inventoryLotId);

const selectLot = (lot: InventoryLotListResponse) => {
  if (isLotSelected(lot.id)) {
    items.value = items.value.filter((item) => item.inventoryLotId !== lot.id);
    return;
  }

  items.value.push({
    inventoryLotId: lot.id,
    sku: lot.sku,
    productName: lot.productName,
    imageUrl: lot.imageUrl ?? null,
    capacityValue: lot.capacityValue ?? null,
    bottleTypeName: lot.bottleTypeName ?? null,
    lotCode: lot.lotCode,
    systemQuantity: Number(lot.quantityOnHand ?? 0),
    actualQuantity: "",
    reasonPreset: "",
    customReason: "",
  });
};

const removeItem = (inventoryLotId: number) => {
  items.value = items.value.filter(
    (item) => item.inventoryLotId !== inventoryLotId
  );
};

const actualQuantityValue = (row: EditableItem): number | null => {
  const raw = String(row.actualQuantity ?? "").trim();

  if (!/^\d+$/.test(raw)) {
    return null;
  }

  const value = Number(raw);

  return Number.isSafeInteger(value) ? value : null;
};

const differenceOf = (row: EditableItem): number | null => {
  const actualQuantity = actualQuantityValue(row);

  if (actualQuantity === null) {
    return null;
  }

  return actualQuantity - Number(row.systemQuantity ?? 0);
};

const onActualQuantityInput = (row: EditableItem, event: Event) => {
  const input = event.target as HTMLInputElement;

  /*
   * Không dùng type="number" + v-model.number:
   * browser cho phép ký tự e/E và JS có thể biến chuỗi rất dài thành số khoa học.
   * Giữ raw digits giúp ô này chỉ nhận số nguyên không âm.
   */
  const raw = input.value
    .replace(/\D/g, "")
    .replace(/^0+(?=\d)/, "")
    .slice(0, String(MAX_ACTUAL_QUANTITY).length);

  row.actualQuantity = raw;
  input.value = raw;

  actualQuantityChanged(row);
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

const validate = async () => {
  if (String(note.value ?? "").trim().length > MAX_NOTE_LENGTH) {
    await Swal.fire(
      "Dữ liệu không hợp lệ",
      `Ghi chú phiếu không được vượt quá ${MAX_NOTE_LENGTH} ký tự.`,
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

    const actualQuantityRaw = String(row.actualQuantity ?? "").trim();

    if (!/^\d+$/.test(actualQuantityRaw)) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: số lượng thực tế phải là số nguyên lớn hơn hoặc bằng 0.`,
        "warning"
      );
      return false;
    }

    const actualQuantity = Number(actualQuantityRaw);

    if (
      !Number.isSafeInteger(actualQuantity) ||
      actualQuantity < 0 ||
      actualQuantity > MAX_ACTUAL_QUANTITY
    ) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: số lượng thực tế phải từ 0 đến ${formatNumber(
          MAX_ACTUAL_QUANTITY
        )}.`,
        "warning"
      );
      return false;
    }

    const reason = resolveReason(row);

    if (reason.length > MAX_REASON_LENGTH) {
      await Swal.fire(
        "Dữ liệu không hợp lệ",
        `Dòng ${line}: lý do không được vượt quá ${MAX_REASON_LENGTH} ký tự.`,
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
    // Ghi chú bị ẩn nhưng vẫn bảo toàn dữ liệu cũ khi sửa.
    note: String(note.value ?? "").trim() || null,
    items: items.value.map(
      (row): StockAdjustmentItemRequest => ({
        inventoryLotId: Number(row.inventoryLotId),
        actualQuantity: Number(row.actualQuantity),
        reason: resolveReason(row) || null,
      })
    ),

    // CREATE không gửi revision; UPDATE gửi đúng snapshot mà người dùng đang nhìn.
    expectedRevision: isEdit.value ? props.detail?.revision ?? null : undefined,
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
              Chọn lô cần kiểm kê, nhập số lượng đếm thực tế. Hệ thống tự tính
              chênh lệch; không chọn thủ công tăng/giảm tồn.
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
            - Ghi chú chung
          -->

          <section class="lot-picker-section">
            <div class="section-head">
              <div>
                <h4>Chọn lô cần kiểm kê</h4>
                <p>
                  Tìm theo SKU, sản phẩm hoặc mã lô. Mỗi lô chỉ được chọn một
                  lần trong cùng phiếu.
                </p>
              </div>

              <div class="selected-badge">Đã chọn {{ items.length }} lô</div>
            </div>

            <div class="lot-search">
              <div class="search-input-wrap">
                <i class="bi bi-search"></i>

                <input
                  v-model="lotKeyword"
                  type="text"
                  maxlength="100"
                  placeholder="Tìm theo SKU, sản phẩm hoặc mã lô..."
                  @keyup.enter.prevent="searchLots"
                />

                <button
                  v-if="lotKeyword"
                  type="button"
                  class="clear-search-btn"
                  aria-label="Xóa tìm kiếm"
                  @click="clearLotSearch"
                >
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>

              <button
                type="button"
                class="search-btn"
                :disabled="loadingLots"
                @click="searchLots"
              >
                {{ loadingLots ? "Đang tìm..." : "Tìm kiếm" }}
              </button>
            </div>

            <div v-if="lotLoadError" class="lot-error">
              {{ lotLoadError }}
            </div>

            <div v-if="loadingLots" class="lot-state">
              Đang tải danh sách lô...
            </div>

            <div v-else-if="lotOptions.length === 0" class="lot-state">
              Không tìm thấy lô phù hợp.
            </div>

            <div v-else class="lot-grid">
              <button
                v-for="option in lotOptions"
                :key="option.id"
                type="button"
                class="lot-card"
                :class="{ selected: isLotSelected(option.id) }"
                @click="selectLot(option)"
              >
                <span class="lot-card-check">
                  <i v-if="isLotSelected(option.id)" class="bi bi-check-lg"></i>
                </span>

                <span
                  class="lot-card-thumb"
                  :class="{ clickable: hasUsableImage(option.imageUrl) }"
                  :title="
                    hasUsableImage(option.imageUrl)
                      ? 'Bấm để xem ảnh lớn'
                      : 'Sản phẩm chưa có ảnh'
                  "
                  @click.stop="
                    openImagePreview(
                      option.imageUrl,
                      option.productName,
                      option.sku
                    )
                  "
                >
                  <i class="bi bi-image"></i>

                  <img
                    v-if="hasUsableImage(option.imageUrl)"
                    :src="option.imageUrl || ''"
                    :alt="option.productName"
                    loading="lazy"
                    @error="onImageError"
                  />
                </span>

                <span class="lot-card-body">
                  <strong>{{ option.productName }}</strong>
                  <span v-if="variantLabel(option)" class="variant-info">
                    {{ variantLabel(option) }}
                  </span>
                  <span class="sku-code">{{ option.sku }}</span>
                  <span class="lot-code">Lô {{ option.lotCode }}</span>

                  <span class="lot-meta">
                    <span>
                      Tồn hệ thống:
                      <strong>{{ formatNumber(option.quantityOnHand) }}</strong>
                    </span>

                    <span v-if="option.isExpired" class="expired-text">
                      Đã hết hạn
                    </span>
                  </span>
                </span>
              </button>
            </div>
          </section>

          <section class="selected-section">
            <div class="section-head selected-head">
              <div>
                <h4>Lô đã chọn ({{ items.length }})</h4>
                <p>
                  Nhập số lượng kiểm đếm thực tế. Khi có chênh lệch, bắt buộc
                  chọn lý do.
                </p>
              </div>
            </div>

            <div v-if="items.length === 0" class="selected-empty">
              Chưa có lô nào được chọn.
            </div>

            <div v-else class="table-wrapper">
              <table class="selected-table">
                <thead>
                  <tr>
                    <th class="image-column">Ảnh</th>
                    <th class="product-column">Sản phẩm / lô</th>
                    <th class="quantity-column">Tồn hệ thống</th>
                    <th class="quantity-column">Tồn thực tế *</th>
                    <th class="difference-column">Chênh lệch</th>
                    <th class="reason-column">Lý do</th>
                    <th class="action-column">Thao tác</th>
                  </tr>
                </thead>

                <tbody>
                  <tr v-for="row in items" :key="row.inventoryLotId">
                    <td class="image-cell">
                      <span
                        class="selected-thumb"
                        :class="{ clickable: hasUsableImage(row.imageUrl) }"
                        :title="
                          hasUsableImage(row.imageUrl)
                            ? 'Bấm để xem ảnh lớn'
                            : 'Sản phẩm chưa có ảnh'
                        "
                        @click="
                          openImagePreview(
                            row.imageUrl,
                            row.productName,
                            row.sku
                          )
                        "
                      >
                        <i class="bi bi-image"></i>

                        <img
                          v-if="hasUsableImage(row.imageUrl)"
                          :src="row.imageUrl || ''"
                          :alt="row.productName"
                          loading="lazy"
                          @error="onImageError"
                        />
                      </span>
                    </td>

                    <td>
                      <div class="product-cell">
                        <strong>{{ row.productName }}</strong>
                        <span v-if="variantLabel(row)" class="variant-info">
                          {{ variantLabel(row) }}
                        </span>
                        <span>{{ row.sku }}</span>
                        <span>Lô {{ row.lotCode }}</span>
                      </div>
                    </td>

                    <td class="system-quantity">
                      {{ formatNumber(row.systemQuantity) }}
                    </td>

                    <td>
                      <input
                        :value="row.actualQuantity"
                        class="table-input"
                        type="text"
                        inputmode="numeric"
                        autocomplete="off"
                        maxlength="7"
                        placeholder="0"
                        :title="`Tối đa ${formatNumber(MAX_ACTUAL_QUANTITY)}`"
                        @input="onActualQuantityInput(row, $event)"
                      />
                    </td>

                    <td>
                      <div class="difference-box" :class="differenceClass(row)">
                        <strong>
                          {{
                            differenceOf(row) === null
                              ? "—"
                              : `${
                                  Number(differenceOf(row)) > 0 ? "+" : ""
                                }${differenceOf(row)}`
                          }}
                        </strong>
                        <small>{{ resultLabel(row) }}</small>
                      </div>
                    </td>

                    <td>
                      <div class="reason-wrap">
                        <select
                          v-model="row.reasonPreset"
                          class="table-input"
                          :disabled="
                            differenceOf(row) === null ||
                            differenceOf(row) === 0
                          "
                        >
                          <option value="">
                            {{
                              differenceOf(row) === null
                                ? "Nhập tồn thực tế trước"
                                : differenceOf(row) === 0
                                ? "Không cần lý do"
                                : "-- Chọn lý do --"
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
                            v-if="
                              differenceOf(row) !== null &&
                              differenceOf(row) !== 0
                            "
                            :value="OTHER_REASON"
                          >
                            Khác
                          </option>
                        </select>

                        <textarea
                          v-if="row.reasonPreset === OTHER_REASON"
                          v-model="row.customReason"
                          class="reason-textarea"
                          rows="2"
                          placeholder="Nhập lý do cụ thể..."
                        ></textarea>
                      </div>
                    </td>

                    <td class="action-cell">
                      <button
                        type="button"
                        class="remove-btn"
                        title="Xóa lô khỏi phiếu"
                        @click="removeItem(row.inventoryLotId)"
                      >
                        <i class="bi bi-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

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
                saving ? "Đang lưu..." : isEdit ? "Cập nhật phiếu" : "Lưu tạm"
              }}
            </button>
          </div>
        </form>
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
  width: min(1240px, 100%);
  margin: 20px auto;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.sa-header,
.sa-footer,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.sa-header {
  padding: 18px 22px;
  border-bottom: 1px solid #e5e7eb;
}

.sa-footer {
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}

.sa-header h3,
.section-head h4 {
  margin: 0;
}

.sa-header p,
.section-head p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.lot-picker-section,
.selected-section {
  padding: 20px 22px;
}

.lot-picker-section {
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

.lot-search {
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

.lot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  max-height: 280px;
  margin-top: 14px;
  overflow-y: auto;
}

.lot-card {
  position: relative;
  display: grid;
  min-height: 116px;
  grid-template-columns: 56px minmax(0, 1fr);
  align-items: center;
  gap: 11px;
  padding: 13px 40px 13px 14px;
  border: 1px solid #dfe3e8;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: 0.15s ease;
}

.lot-card:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.05);
}

.lot-card.selected {
  border-color: #65a30d;
  background: #f7fee7;
}

.lot-card-body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}

.lot-card-body > strong {
  color: #111827;
  font-size: 14px;
}

.lot-card-thumb,
.selected-thumb {
  position: relative;
  display: inline-flex;
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
  color: #9ca3af;
}

.lot-card-thumb.clickable,
.selected-thumb.clickable {
  cursor: pointer;
}

.lot-card-thumb.clickable:hover,
.selected-thumb.clickable:hover {
  border-color: #9ca3af;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.1);
}

.lot-card-thumb img,
.selected-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #fff;
}

.lot-card-thumb i,
.selected-thumb i {
  font-size: 18px;
}

.lot-card-check {
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

.lot-card.selected .lot-card-check {
  border-color: #84cc16;
}

.sku-code,
.lot-code {
  color: #6b7280;
  font-size: 12px;
  word-break: break-word;
}

.variant-info {
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
}

.lot-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
  color: #4b5563;
  font-size: 12px;
}

.lot-meta strong {
  color: #111827;
}

.expired-text {
  color: #b91c1c;
  font-weight: 600;
}

.lot-state,
.lot-error,
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

.lot-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.selected-head {
  margin-bottom: 14px;
}

.table-wrapper {
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

.product-column {
  min-width: 220px;
}

.quantity-column {
  width: 125px;
}

.difference-column {
  width: 150px;
}

.reason-column {
  min-width: 280px;
}

.action-column {
  width: 72px;
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

.system-quantity {
  color: #111827;
  font-weight: 700;
}

.table-input,
.reason-textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 38px;
  padding: 8px 9px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  background: #fff;
  outline: none;
}

.table-input:focus,
.reason-textarea:focus {
  border-color: #111827;
}

.table-input:disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.reason-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reason-textarea {
  resize: vertical;
}

.difference-box {
  display: flex;
  min-height: 52px;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  padding: 7px 9px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.difference-box strong {
  font-size: 16px;
}

.difference-box small {
  font-size: 11px;
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

.action-cell {
  text-align: center;
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

@media (max-width: 1000px) {
  .lot-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .lot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .lot-search {
    flex-direction: column;
  }

  .search-btn {
    width: 100%;
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
}

@media (max-width: 480px) {
  .lot-grid {
    grid-template-columns: 1fr;
  }
}

:global(.swal2-container) {
  z-index: 1000000 !important;
}
</style>
