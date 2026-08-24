<template>
  <div class="card border-0 shadow-sm p-4 bg-light rounded-3">
    <div class="d-flex justify-content-between align-items-center gap-2 mb-4">
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

      <span class="badge bg-dark py-2 px-3 fs-6 rounded-pill">
        Đã chọn {{ modelValue.length }}
      </span>
    </div>

    <div v-if="!readonly" class="row g-3 mb-4">
      <div class="col-md-9">
        <input
          v-model.trim="keyword"
          type="text"
          class="form-control form-control-lg bg-white"
          placeholder="Tìm theo tên sản phẩm / SKU..."
          :disabled="store.optionLoading"
          @keyup.enter="handleSearch(0)"
        />
      </div>

      <div class="col-md-3 d-grid">
        <button
          type="button"
          class="btn btn-outline-dark btn-lg"
          :disabled="store.optionLoading"
          @click="handleSearch(0)"
        >
          <span
            v-if="store.optionLoading"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          Tìm biến thể
        </button>
      </div>
    </div>

    <div
      v-if="!readonly && (!startDate || !endDate)"
      class="alert alert-warning py-3 mb-4 fw-semibold"
    >
      <i class="bi bi-exclamation-triangle-fill me-2"></i> Chọn thời gian bắt đầu và kết thúc trước để hệ thống kiểm tra trùng khuyến mãi.
    </div>

    <div v-if="!readonly" class="card border-0 shadow-sm mb-4">
      <div class="card-body p-0 table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th class="ps-4" style="width: 60px">Chọn</th>
              <th>Sản phẩm</th>
              <th>Dung tích</th>
              <th>Loại chai</th>
              <th class="text-end">Giá gốc</th>
              <th class="text-end">Tồn</th>
              <th class="pe-4">Trạng thái</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="store.optionLoading">
              <td colspan="7" class="text-center py-5">
                <span class="spinner-border spinner-border-sm me-2 text-dark"></span>
                Đang tải biến thể...
              </td>
            </tr>

            <tr v-else-if="store.variantOptions.length === 0">
              <td colspan="7" class="text-center text-muted py-5">
                Chưa có dữ liệu. Nhập từ khóa rồi bấm tìm biến thể.
              </td>
            </tr>

            <tr v-else-if="displayOptions.length === 0">
              <td colspan="7" class="text-center text-muted py-5">
                Các biến thể tìm được đều ở trạng thái Ngừng bán.
              </td>
            </tr>

            <template v-else>
              <tr
                v-for="item in displayOptions"
                :key="item.productVariantId"
                :class="{
                  'table-secondary':
                    !item.availableForPromotion && !isSelected(item.productVariantId),
                }"
              >
                <td class="ps-4">
                  <input
                    class="form-check-input"
                    style="transform: scale(1.2)"
                    type="checkbox"
                    :checked="isSelected(item.productVariantId)"
                    :disabled="
                      !item.availableForPromotion && !isSelected(item.productVariantId)
                    "
                    @change="toggleVariant(item)"
                  />
                </td>

                <td>
                  <div class="fw-bold">
                    {{ item.productName || "Không rõ tên sản phẩm" }}
                  </div>
                  <small v-if="item.manufacturingDate" class="text-muted d-block mt-1">
                    NSX: {{ formatDate(item.manufacturingDate) }}
                  </small>
                  <small v-if="item.expirationDate" class="text-muted d-block">
                    HSD: {{ formatDate(item.expirationDate) }}
                  </small>
                </td>

                <td class="fw-semibold">{{ item.capacity || "N/A" }}</td>
                <td>{{ item.bottleType || "N/A" }}</td>
                <td class="text-end fw-bold">{{ formatCurrency(item.price) }}</td>
                <td class="text-end fw-semibold">{{ item.stockQuantity ?? 0 }}</td>
                <td class="pe-4">
                  <span v-if="item.availableForPromotion" class="badge bg-success px-2 py-1">
                    Có thể chọn
                  </span>
                  <span v-else class="badge bg-danger text-wrap px-2 py-1" :title="item.unavailableReason || ''">
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
        class="card-footer bg-white border-0 py-3 d-flex justify-content-end align-items-center gap-3"
      >
        <button
          type="button"
          class="btn btn-outline-secondary"
          :disabled="store.optionPageNumber <= 0 || store.optionLoading"
          @click="handleSearch(store.optionPageNumber - 1)"
        >
          Trang Trước
        </button>

        <span class="fw-bold">
          Trang {{ store.optionPageNumber + 1 }} / {{ store.optionTotalPages }}
        </span>

        <button
          type="button"
          class="btn btn-outline-secondary"
          :disabled="
            store.optionPageNumber + 1 >= store.optionTotalPages ||
            store.optionLoading
          "
          @click="handleSearch(store.optionPageNumber + 1)"
        >
          Trang Sau
        </button>
      </div>
    </div>

    <div v-if="modelValue.length > 0" class="mt-4 pt-3 border-top">
      <h6 class="fw-bold mb-3">
        {{ readonly ? "Biến thể trong chiến dịch" : "Biến thể đã chọn" }}
      </h6>

      <div class="card border-0 shadow-sm">
        <div class="card-body p-0 table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th class="ps-4">Sản phẩm</th>
                <th>NSX/HSD</th>
                <th class="text-end">Giá gốc</th>
                <th style="width: 170px">% giảm</th>
                <th class="text-end">Giá sau giảm</th>
                <th v-if="!readonly" class="text-center pe-4" style="width: 100px">Thao tác</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="selected in modelValue" :key="selected.productVariantId">
                <td class="ps-4">
                  <div class="fw-bold text-success">
                    {{ selected.productName || "Biến thể #" + selected.productVariantId }}
                  </div>
                  <small class="text-muted fw-semibold">
                    {{ selected.capacity || "N/A" }} - {{ selected.bottleType || "N/A" }}
                  </small>
                </td>
                <td class="small">
                  <span class="text-muted d-block">NSX: {{ selected.manufacturingDate ? formatDate(selected.manufacturingDate) : "-" }}</span>
                  <span class="text-muted d-block">HSD: {{ selected.expirationDate ? formatDate(selected.expirationDate) : "-" }}</span>
                </td>
                <td class="text-end fw-semibold">
                  {{ formatCurrency(resolveOriginalPrice(selected)) }}
                </td>
                <td>
                  <input
                    type="number"
                    min="0.01"
                    max="99"
                    step="0.01"
                    class="form-control"
                    :value="selected.discountPercent"
                    :disabled="readonly"
                    @input="onDiscountInput(selected.productVariantId, $event)"
                  />
                </td>
                <td class="text-end fw-bold text-danger fs-6">
                  {{ formatCurrency(calculateSalePrice(selected)) }}
                </td>
                <td v-if="!readonly" class="text-center pe-4">
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-danger"
                    @click="removeSelected(selected.productVariantId)"
                  >
                    Bỏ chọn
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <small class="text-muted d-block mt-3 fst-italic">
        * Giá sau giảm chỉ để hiển thị. Khi checkout, BE sẽ tự tính lại giá Flash Sale.
      </small>
    </div>

    <div v-else-if="readonly" class="alert alert-light border mt-3 mb-0">
      Chiến dịch này chưa có biến thể sản phẩm.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import Swal from "sweetalert2";
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

// Lọc ẩn các biến thể đang ngừng bán
const displayOptions = computed(() => {
  return store.variantOptions.filter(item => {
    const isDiscontinued = item.unavailableReason && item.unavailableReason.toLowerCase().includes('ngừng bán');
    // Chỉ hiển thị nếu KHÔNG PHẢI ngừng bán, HOẶC biến thể đó đang được chọn (trường hợp lỡ tick chọn trước đó)
    return !isDiscontinued || isSelected(item.productVariantId);
  });
});

const handleSearch = async (page = 0) => {
  if (props.readonly) return;

  if (!props.startDate || !props.endDate) {
    await Swal.fire({
      icon: "warning",
      title: "Thiếu thời gian khuyến mãi",
      text: "Vui lòng chọn ngày bắt đầu và ngày kết thúc trước khi tìm biến thể.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

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

const toggleVariant = async (item: PromotionProductVariantOptionResponse) => {
  if (props.readonly) return;

  if (isSelected(item.productVariantId)) {
    removeSelected(item.productVariantId);
    return;
  }

  if (!item.availableForPromotion) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể chọn biến thể",
      text: item.unavailableReason || "Biến thể này không đủ điều kiện khuyến mãi.",
      confirmButtonColor: "#bd9a5f",
    });
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
      manufacturingDate: item.manufacturingDate,
      expirationDate: item.expirationDate,
      availableForPromotion: item.availableForPromotion,
      unavailableReason: item.unavailableReason,
    },
  ];

  emit("update:modelValue", next);
};

const removeSelected = (productVariantId: number) => {
  if (props.readonly) return;
  emit(
    "update:modelValue",
    props.modelValue.filter((item) => item.productVariantId !== productVariantId)
  );
};

// ===================== CHẶN KÝ TỰ / SỐ QUÁ 99 =====================
const onDiscountInput = (productVariantId: number, event: Event) => {
  if (props.readonly) return;
  const target = event.target as HTMLInputElement;
  let val = Number(target.value);
  
  if (val > 99) {
    val = 99;
    target.value = "99";
  } else if (val < 0) {
    val = 0;
    target.value = "0";
  }
  
  updateDiscount(productVariantId, val);
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
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleDateString("vi-VN");
};
</script>