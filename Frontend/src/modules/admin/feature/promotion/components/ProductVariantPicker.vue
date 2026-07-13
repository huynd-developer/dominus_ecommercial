<template>
  <div class="border rounded-3 p-3 bg-light">
    <div class="d-flex justify-content-between align-items-center gap-2 mb-3">
      <div>
        <h6 class="mb-1 fw-bold">Chọn biến thể sản phẩm</h6>
        <small class="text-muted">
          <template v-if="readonly">
            Danh sách biến thể thuộc chiến dịch. Chế độ chỉ xem.
          </template>
          <template v-else>
            Tìm theo tên sản phẩm hoặc SKU. Admin không nhập ID thủ công.
          </template>
        </small>
      </div>

      <span class="badge bg-dark">
        Đã chọn {{ modelValue.length }}
      </span>
    </div>

    <div v-if="!readonly" class="row g-2 mb-3">
      <div class="col-md-8">
        <input
          v-model.trim="keyword"
          type="text"
          class="form-control"
          placeholder="Tìm theo tên sản phẩm / SKU..."
          :disabled="store.optionLoading"
          @keyup.enter="handleSearch(0)"
        />
      </div>

      <div class="col-md-4 d-grid">
        <button
          type="button"
          class="btn btn-outline-dark"
          :disabled="store.optionLoading"
          @click="handleSearch(0)"
        >
          <span
            v-if="store.optionLoading"
            class="spinner-border spinner-border-sm me-1"
          ></span>
          Tìm biến thể
        </button>
      </div>
    </div>

    <div
      v-if="!readonly && (!startDate || !endDate)"
      class="alert alert-warning py-2 mb-3"
    >
      Chọn thời gian bắt đầu và kết thúc trước để hệ thống kiểm tra trùng
      khuyến mãi, ngày sản xuất và hạn sử dụng.
    </div>

    <div v-if="!readonly" class="table-responsive border rounded bg-white">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th style="width: 60px">Chọn</th>
            <th>Sản phẩm</th>
            <th>SKU</th>
            <th>Dung tích</th>
            <th>Loại chai</th>
            <th class="text-end">Giá gốc</th>
            <th class="text-end">Tồn</th>
            <th>Trạng thái</th>
          </tr>
        </thead>

        <tbody>
          <tr v-if="store.optionLoading">
            <td colspan="8" class="text-center py-4">
              <span class="spinner-border spinner-border-sm me-2"></span>
              Đang tải biến thể...
            </td>
          </tr>

          <tr v-else-if="store.variantOptions.length === 0">
            <td colspan="8" class="text-center text-muted py-4">
              Chưa có dữ liệu. Nhập từ khóa rồi bấm tìm biến thể.
            </td>
          </tr>

          <template v-else>
            <tr
              v-for="item in store.variantOptions"
              :key="item.productVariantId"
              :class="{
                'table-secondary':
                  !item.availableForPromotion && !isSelected(item.productVariantId),
              }"
            >
              <td>
                <input
                  class="form-check-input"
                  type="checkbox"
                  :checked="isSelected(item.productVariantId)"
                  :disabled="
                    !item.availableForPromotion && !isSelected(item.productVariantId)
                  "
                  @change="toggleVariant(item)"
                />
              </td>

              <td>
                <div class="fw-semibold">
                  {{ item.productName || "Không rõ tên sản phẩm" }}
                </div>

                <small v-if="item.manufacturingDate" class="text-muted d-block">
                  NSX: {{ formatDate(item.manufacturingDate) }}
                </small>

                <small v-if="item.expirationDate" class="text-muted d-block">
                  HSD: {{ formatDate(item.expirationDate) }}
                </small>
              </td>

              <td>
                <code>{{ item.sku || "N/A" }}</code>
              </td>

              <td>{{ item.capacity || "N/A" }}</td>

              <td>{{ item.bottleType || "N/A" }}</td>

              <td class="text-end fw-semibold">
                {{ formatCurrency(item.price) }}
              </td>

              <td class="text-end">
                {{ item.stockQuantity ?? 0 }}
              </td>

              <td>
                <span
                  v-if="item.availableForPromotion"
                  class="badge bg-success"
                >
                  Có thể chọn
                </span>

                <span
                  v-else
                  class="badge bg-danger text-wrap"
                  :title="item.unavailableReason || ''"
                >
                  {{ item.unavailableReason || "Không thể chọn" }}
                </span>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <div
      v-if="!readonly && store.optionTotalPages > 1"
      class="d-flex justify-content-end align-items-center gap-2 mt-3"
    >
      <button
        type="button"
        class="btn btn-sm btn-outline-secondary"
        :disabled="store.optionPageNumber <= 0 || store.optionLoading"
        @click="handleSearch(store.optionPageNumber - 1)"
      >
        Trước
      </button>

      <span class="small text-muted">
        Trang {{ store.optionPageNumber + 1 }} / {{ store.optionTotalPages }}
      </span>

      <button
        type="button"
        class="btn btn-sm btn-outline-secondary"
        :disabled="
          store.optionPageNumber + 1 >= store.optionTotalPages ||
          store.optionLoading
        "
        @click="handleSearch(store.optionPageNumber + 1)"
      >
        Sau
      </button>
    </div>

    <div v-if="modelValue.length > 0" class="mt-4">
      <h6 class="fw-bold mb-2">
        {{ readonly ? "Biến thể trong chiến dịch" : "Biến thể đã chọn" }}
      </h6>

      <div class="table-responsive border rounded bg-white">
        <table class="table align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Sản phẩm</th>
              <th>SKU</th>
              <th class="text-end">Giá gốc</th>
              <th style="width: 170px">% giảm</th>
              <th class="text-end">Giá sau giảm</th>
              <th v-if="!readonly" style="width: 70px"></th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="selected in modelValue" :key="selected.productVariantId">
              <td>
                <div class="fw-semibold">
                  {{ selected.productName || "Biến thể #" + selected.productVariantId }}
                </div>

                <small class="text-muted">
                  {{ selected.capacity || "N/A" }} - {{ selected.bottleType || "N/A" }}
                </small>
              </td>

              <td>
                <code>{{ selected.sku || "N/A" }}</code>
              </td>

              <td class="text-end">
                {{ formatCurrency(resolveOriginalPrice(selected)) }}
              </td>

              <td>
                <input
                  type="number"
                  min="0.01"
                  max="99.99"
                  step="0.01"
                  class="form-control"
                  :value="selected.discountPercent"
                  :disabled="readonly"
                  @input="onDiscountInput(selected.productVariantId, $event)"
                />
              </td>

              <td class="text-end fw-bold text-danger">
                {{ formatCurrency(calculateSalePrice(selected)) }}
              </td>

              <td v-if="!readonly" class="text-end">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger"
                  @click="removeSelected(selected.productVariantId)"
                >
                  Xóa
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <small class="text-muted d-block mt-2">
        Giá sau giảm chỉ để hiển thị. Khi checkout, BE phải tự tính lại giá Flash Sale.
      </small>
    </div>

    <div v-else-if="readonly" class="alert alert-light border mb-0">
      Chiến dịch này chưa có biến thể sản phẩm.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { usePromotionStore } from "../stores/promotion.store";
import type {
  PromotionProductVariantOptionResponse,
  PromotionVariantFormItem,
} from "../types/promotion.types";

const props = withDefaults(
  defineProps<{
    modelValue: PromotionVariantFormItem[];
    startDate?: string;
    endDate?: string;
    ignorePromotionId?: number | null;
    readonly?: boolean;
  }>(),
  {
    readonly: false,
  }
);

const emit = defineEmits<{
  (e: "update:modelValue", value: PromotionVariantFormItem[]): void;
}>();

const store = usePromotionStore();
const keyword = ref("");

const handleSearch = async (page = 0) => {
  if (props.readonly) return;

  await store.searchProductVariants({
    keyword: keyword.value,
    startDate: props.startDate,
    endDate: props.endDate,
    ignorePromotionId: props.ignorePromotionId ?? undefined,
    page,
    size: store.optionPageSize,
  });
};

const isSelected = (productVariantId: number) => {
  return props.modelValue.some(
    (item) => item.productVariantId === productVariantId
  );
};

const toggleVariant = (item: PromotionProductVariantOptionResponse) => {
  if (props.readonly) return;

  if (isSelected(item.productVariantId)) {
    removeSelected(item.productVariantId);
    return;
  }

  const next: PromotionVariantFormItem[] = [
    ...props.modelValue,
    {
      productVariantId: item.productVariantId,
      discountPercent: 10,
      sku: item.sku,
      productName: item.productName,
      capacity: item.capacity,
      bottleType: item.bottleType,
      price: item.price,
      originalPrice: item.price,
      stockQuantity: item.stockQuantity,
    },
  ];

  emit("update:modelValue", next);
};

const removeSelected = (productVariantId: number) => {
  if (props.readonly) return;

  emit(
    "update:modelValue",
    props.modelValue.filter(
      (item) => item.productVariantId !== productVariantId
    )
  );
};

const onDiscountInput = (productVariantId: number, event: Event) => {
  if (props.readonly) return;

  const target = event.target as HTMLInputElement;
  updateDiscount(productVariantId, Number(target.value));
};

const updateDiscount = (productVariantId: number, discountPercent: number) => {
  if (props.readonly) return;

  const next = props.modelValue.map((item) => {
    if (item.productVariantId !== productVariantId) {
      return item;
    }

    return {
      ...item,
      discountPercent,
    };
  });

  emit("update:modelValue", next);
};

const resolveOriginalPrice = (item: PromotionVariantFormItem) => {
  return item.originalPrice ?? item.price ?? 0;
};

const calculateSalePrice = (item: PromotionVariantFormItem) => {
  const originalPrice = resolveOriginalPrice(item);
  const discountPercent = item.discountPercent || 0;

  return Math.max(0, originalPrice - (originalPrice * discountPercent) / 100);
};

const formatCurrency = (value?: number | null) => {
  return Number(value || 0).toLocaleString("vi-VN") + " ₫";
};

const formatDate = (value: string) => {
  return new Date(value).toLocaleDateString("vi-VN");
};
</script>