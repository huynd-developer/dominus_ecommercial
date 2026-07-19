<template>
  <div class="product-panel d-flex flex-column h-100 select-none">
    <div class="filter-search-section mb-2 shrink-0">
      <div class="filter-top-row d-flex align-items-center gap-2 mb-2">
        <div
          class="category-slider d-flex gap-2 overflow-auto pb-1 no-scrollbar flex-grow-1 min-w-0"
        >
          <button
            v-for="cat in posStore.categories"
            :key="cat"
            type="button"
            :class="[
              'luxury-pill',
              { active: posStore.selectedCategory === cat },
            ]"
            :disabled="posStore.cashPaid > 0"
            @click="posStore.selectedCategory = cat"
          >
            {{ cat }}
          </button>
        </div>

        <div class="product-search-wrapper position-relative shrink-0">
          <i class="bi bi-search search-icon"></i>

          <input
            ref="productSearchRef"
            v-model.trim="posStore.searchQuery"
            type="text"
            class="product-search-input"
            placeholder="Tìm tên, SKU, biến thể..."
            autocomplete="off"
            :disabled="posStore.cashPaid > 0"
            @keydown.enter="handleSearchEnter"
            @keydown.esc="posStore.searchQuery = ''"
          />

          <button
            v-if="posStore.searchQuery"
            type="button"
            class="btn-clear-search"
            title="Xóa tìm kiếm"
            @click="posStore.searchQuery = ''"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
      </div>

      <div class="filter-bottom-row d-flex align-items-center gap-2">
        <select
          v-model="selectedCapacity"
          class="pos-filter-select"
          :disabled="posStore.cashPaid > 0"
        >
          <option value="">Tất cả dung tích</option>
          <option
            v-for="capacity in capacityOptions"
            :key="capacity"
            :value="capacity"
          >
            {{ capacity }}
          </option>
        </select>

        <select
          v-model="selectedBottleType"
          class="pos-filter-select"
          :disabled="posStore.cashPaid > 0"
        >
          <option value="">Tất cả loại chai</option>
          <option
            v-for="bottleType in bottleTypeOptions"
            :key="bottleType"
            :value="bottleType"
          >
            {{ bottleType }}
          </option>
        </select>

        <select
          v-model="selectedStockFilter"
          class="pos-filter-select stock-filter"
          :disabled="posStore.cashPaid > 0"
        >
          <option value="">Tất cả tồn kho</option>
          <option value="AVAILABLE">Còn hàng</option>
          <option value="LOW">Sắp hết hàng</option>
          <option value="OUT">Hết hàng</option>
        </select>

        <button
          type="button"
          class="btn-reset-filter"
          :disabled="posStore.cashPaid > 0 || !hasAdvancedFilter"
          @click="clearAdvancedFilters"
        >
          <i class="bi bi-arrow-counterclockwise me-1"></i>
          Xóa lọc
        </button>
      </div>
    </div>

    <div
      v-if="posStore.isLoading"
      class="flex-grow-1 d-flex flex-column align-items-center justify-content-center text-muted-custom font-sm"
    >
      <i class="bi bi-arrow-repeat spin fs-3"></i>
      <p class="mt-2 mb-0">Đang tải sản phẩm từ máy chủ...</p>
    </div>

    <div
      v-else-if="displayProducts.length === 0"
      class="flex-grow-1 d-flex align-items-center justify-content-center text-muted-custom font-sm"
    >
      Không tìm thấy sản phẩm phù hợp.
    </div>

    <div
      v-else
      class="product-table-wrapper flex-grow-1 overflow-auto rounded-3 custom-scrollbar"
    >
      <table class="table table-dark-custom align-middle mb-0">
        <thead>
          <tr>
            <th class="text-center col-check">Chọn</th>
            <th class="col-product">Sản phẩm</th>
            <th class="col-variant">Biến thể</th>
            <th class="text-end col-price">Giá</th>
            <th class="text-center col-stock">Kho</th>
            <th class="text-end col-action">Số lượng</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="product in displayProducts"
            :key="product.sku"
            :class="{
              'row-disabled': isProductDisabled(product),
              'row-selected': isProductSelected(product),
              'row-expired': isExpired(product),
              'row-edit-held': posStore.activeHeldOrderId && !posStore.cashPaid,
            }"
            :title="getDisabledReason(product) || getProductTitle(product)"
          >
            <td class="text-center check-cell">
              <input
                type="checkbox"
                class="product-checkbox"
                :checked="isProductSelected(product)"
                :disabled="isProductDisabled(product)"
                :title="getDisabledReason(product) || checkboxTitle(product)"
                @change.stop="handleToggleProduct(product)"
                @click.stop
              />
            </td>

            <td>
              <div class="product-cell d-flex align-items-center gap-2 min-w-0">
                <img
                  :src="product.image || placeholderImage(product.name)"
                  :alt="getProductTitle(product)"
                  class="product-thumb shrink-0"
                />

                <div class="min-w-0 product-info">
                  <div
                    class="product-name-row d-flex align-items-center gap-2 min-w-0"
                    :title="`${product.name} - SKU: ${product.sku}`"
                  >
                    <span class="product-name text-truncate">
                      {{ product.name }}
                    </span>

                    <span class="sku-inline shrink-0" :title="product.sku">
                      {{ product.sku }}
                    </span>
                  </div>

                  <div
                    class="product-category text-truncate"
                    :title="product.category"
                  >
                    {{ product.category || "Chưa phân loại" }}
                  </div>
                </div>
              </div>
            </td>

            <td>
              <div
                class="variant-text text-truncate"
                :title="getVariantText(product)"
              >
                <i class="bi bi-droplet-half me-1"></i>
                {{ getVariantText(product) }}
              </div>
            </td>

            <td class="text-end">
              <strong class="price-text">
                {{ formatPrice(product.price) }} ₫
              </strong>
            </td>

            <td class="text-center">
              <span
                class="stock-badge"
                :class="product.stockQuantity > 0 ? 'stock-ok' : 'stock-empty'"
              >
                {{ product.stockQuantity }}
              </span>
            </td>

            <td class="text-end action-cell">
              <div
                v-if="getCartItem(product)"
                class="product-qty-actions d-inline-flex align-items-center justify-content-end gap-1"
              >
                <button
                  type="button"
                  class="qty-btn-grid"
                  :disabled="posStore.cashPaid > 0"
                  title="Giảm số lượng"
                  @click.stop="decreaseQuantity(product)"
                >
                  <i class="bi bi-dash"></i>
                </button>

                <span class="qty-number-grid">
                  {{ getCartItem(product)?.quantity || 0 }}
                </span>

                <button
                  type="button"
                  class="qty-btn-grid"
                  :disabled="
                    posStore.cashPaid > 0 ||
                    (getCartItem(product)?.quantity || 0) >=
                      Number(product.stockQuantity || 0)
                  "
                  title="Tăng số lượng"
                  @click.stop="increaseQuantity(product)"
                >
                  <i class="bi bi-plus"></i>
                </button>

                <button
                  type="button"
                  class="btn-remove-grid"
                  :disabled="posStore.cashPaid > 0"
                  title="Xóa khỏi đơn"
                  @click.stop="removeFromOrder(product)"
                >
                  <i class="bi bi-trash3"></i>
                </button>
              </div>

              <span v-else class="not-selected-text"> Chưa chọn </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="table-hint mt-2 font-xs text-muted-custom">
      <i class="bi bi-info-circle me-1"></i>

      <template v-if="posStore.cashPaid > 0">
        Đơn đã nhận tiền mặt một phần: không được thêm sản phẩm, sửa số lượng
        hoặc đổi voucher.
      </template>

      <template v-else-if="posStore.activeHeldOrderId">
        Đang sửa phiếu treo: được tick thêm sản phẩm, tăng giảm số lượng, xóa
        sản phẩm, đổi voucher; không được đổi khách hàng.
      </template>

      <template v-else>
        Tick checkbox để thêm sản phẩm vào đơn. Sản phẩm đã tick sẽ hiện nút
        tăng giảm số lượng và xóa.
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import {
  usePosStore,
  type PosProduct,
  type CartItem,
} from "@/modules/pos/stores/posStore";

const posStore = usePosStore();
const selectedCapacity = ref<string>("");
const selectedBottleType = ref<string>("");
const selectedStockFilter = ref<"" | "AVAILABLE" | "LOW" | "OUT">("");

const normalizeFilterText = (value?: string | null) => {
  return String(value || "").trim();
};

const isNonEmptyString = (value: unknown): value is string => {
  return typeof value === "string" && value.trim().length > 0;
};

const getCapacityFromProduct = (product: PosProduct) => {
  const text = normalizeFilterText(product.subName);
  const match = text.match(/(\d+(?:[.,]\d+)?)\s*ml/i);

  const rawValue = match?.[1];

  if (!rawValue) {
    return "";
  }

  const value = rawValue.replace(",", ".");
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return "";
  }

  return `${numberValue} ml`;
};

const getBottleTypeFromProduct = (product: PosProduct): string => {
  const text = normalizeFilterText(product.subName);

  if (!text) {
    return "";
  }

  const parts = text
    .split("-")
    .map((item) => item.trim())
    .filter(isNonEmptyString);

  if (parts.length <= 1) {
    return "";
  }

  const lastPart = parts[parts.length - 1];

  return lastPart || "";
};

const capacityOptions = computed<string[]>(() => {
  const values = posStore.filteredProducts
    .map(getCapacityFromProduct)
    .filter(isNonEmptyString);

  return [...new Set(values)].sort((a, b) => {
    return Number(a.replace(/\D/g, "")) - Number(b.replace(/\D/g, ""));
  });
});

const bottleTypeOptions = computed<string[]>(() => {
  const values = posStore.filteredProducts
    .map(getBottleTypeFromProduct)
    .filter(isNonEmptyString);

  return [...new Set(values)].sort((a, b) => a.localeCompare(b, "vi"));
});

const hasAdvancedFilter = computed(() => {
  return Boolean(
    selectedCapacity.value ||
      selectedBottleType.value ||
      selectedStockFilter.value
  );
});

const clearAdvancedFilters = () => {
  selectedCapacity.value = "";
  selectedBottleType.value = "";
  selectedStockFilter.value = "";
};

const displayProducts = computed(() => {
  return posStore.filteredProducts.filter((product) => {
    const capacity = getCapacityFromProduct(product);
    const bottleType = getBottleTypeFromProduct(product);
    const stock = Number(product.stockQuantity || 0);

    const matchesCapacity =
      !selectedCapacity.value || capacity === selectedCapacity.value;

    const matchesBottleType =
      !selectedBottleType.value || bottleType === selectedBottleType.value;

    let matchesStock = true;

    if (selectedStockFilter.value === "AVAILABLE") {
      matchesStock = stock > 0;
    }

    if (selectedStockFilter.value === "LOW") {
      matchesStock = stock > 0 && stock <= 10;
    }

    if (selectedStockFilter.value === "OUT") {
      matchesStock = stock <= 0;
    }

    return matchesCapacity && matchesBottleType && matchesStock;
  });
});
const productSearchRef = ref<HTMLInputElement | null>(null);

const focusProductSearch = () => {
  productSearchRef.value?.focus();
};

const handleSearchEnter = async () => {
  const keyword = String(posStore.searchQuery || "").trim();

  if (!keyword) {
    return;
  }

  if (posStore.cashPaid > 0) {
    posStore.errorMsg =
      "Đơn đã nhận tiền mặt một phần, không được quét/thêm sản phẩm.";
    return;
  }

  /*
   * Chỉ khi nhập đúng SKU thì Enter mới thêm sản phẩm.
   * Nếu đang tìm theo tên/thương hiệu/biến thể thì giữ nguyên kết quả lọc,
   * không gọi API để tránh báo lỗi sai.
   */
  const exactSkuProduct = posStore.allProducts.find((product) => {
    return String(product.sku || "").toLowerCase() === keyword.toLowerCase();
  });

  if (!exactSkuProduct) {
    return;
  }

  await posStore.handleBarcodeScan(keyword);
  posStore.searchQuery = "";
};

onMounted(() => {
  window.addEventListener("pos-focus-product-search", focusProductSearch);
});

onUnmounted(() => {
  window.removeEventListener("pos-focus-product-search", focusProductSearch);
});

const buttonTitle = computed(() => {
  if (posStore.cashPaid > 0) {
    return "Đơn đã nhận tiền mặt một phần, không được thêm sản phẩm";
  }

  if (posStore.activeHeldOrderId) {
    return "Thêm sản phẩm vào phiếu treo đang sửa";
  }

  return "Chọn sản phẩm";
});

const formatPrice = (val?: number | null) => {
  return new Intl.NumberFormat("vi-VN").format(Number(val || 0));
};

const isBeforeToday = (value?: string | null) => {
  if (!value) return false;

  const date = new Date(`${String(value).substring(0, 10)}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return !Number.isNaN(date.getTime()) && date.getTime() < today.getTime();
};

const isAfterToday = (value?: string | null) => {
  if (!value) return false;

  const date = new Date(`${String(value).substring(0, 10)}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return !Number.isNaN(date.getTime()) && date.getTime() > today.getTime();
};

const placeholderImage = (name?: string) => {
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(
    name || "Product"
  )}&background=random&color=fff&size=200`;
};

const getVariantText = (product: any) => {
  return product?.subName && String(product.subName).trim()
    ? product.subName
    : "Biến thể mặc định";
};

const getProductTitle = (product: any) => {
  return `${product.name} - SKU: ${product.sku} - ${getVariantText(product)}`;
};

const isExpired = (product: any) => {
  return Boolean(product?.expired) || isBeforeToday(product?.expirationDate);
};

const getDisabledReason = (product: any) => {
  /*
   * Không khóa sản phẩm bằng activeHeldOrderId.
   * Mở phiếu treo vẫn được tick thêm sản phẩm và chỉnh số lượng.
   * Chỉ khóa chọn/sửa sản phẩm khi đã nhận tiền mặt tạm.
   */
  if (posStore.cashPaid > 0) {
    return "Đơn đã nhận tiền mặt một phần, không được thêm sản phẩm";
  }

  if (!product) {
    return "Sản phẩm không hợp lệ";
  }

  if (!product.sku) {
    return "Sản phẩm thiếu SKU";
  }

  if (product.unavailableReason) {
    return product.unavailableReason;
  }

  if (product.sellable === false) {
    return "Sản phẩm hiện không được bán";
  }

  if (product.status != null && Number(product.status) !== 1) {
    return "Sản phẩm đang ngừng bán";
  }

  if (Number(product.stockQuantity || 0) <= 0) {
    return "Hết hàng";
  }

  if (isAfterToday(product.manufacturingDate)) {
    return "Chưa tới ngày bán";
  }

  if (isExpired(product)) {
    return "Đã hết hạn sử dụng";
  }

  return "";
};

const isProductDisabled = (product: any) => {
  return Boolean(getDisabledReason(product));
};

const getCartItem = (product: PosProduct): CartItem | null => {
  if (!product?.sku) {
    return null;
  }

  return posStore.getCartItemBySku(product.sku);
};

const isProductSelected = (product: PosProduct) => {
  return Boolean(getCartItem(product));
};

const checkboxTitle = (product: PosProduct) => {
  return isProductSelected(product)
    ? "Bỏ chọn sản phẩm khỏi đơn"
    : buttonTitle.value;
};

const handleToggleProduct = (product: PosProduct) => {
  const reason = getDisabledReason(product);

  if (reason) {
    posStore.errorMsg = reason;
    return;
  }

  if (isProductSelected(product)) {
    posStore.removeFromCart(product.sku);
    return;
  }

  posStore.addToCart(product);
};

const increaseQuantity = (product: PosProduct) => {
  const reason = getDisabledReason(product);

  if (reason) {
    posStore.errorMsg = reason;
    return;
  }

  const item = getCartItem(product);

  if (!item) {
    posStore.addToCart(product);
    return;
  }

  posStore.updateQuantity(product.sku, item.quantity + 1);
};

const decreaseQuantity = (product: PosProduct) => {
  if (posStore.cashPaid > 0) {
    posStore.errorMsg =
      "Đơn đã nhận tiền mặt một phần, không được sửa số lượng.";
    return;
  }

  const item = getCartItem(product);

  if (!item) {
    return;
  }

  if (item.quantity <= 1) {
    posStore.removeFromCart(product.sku);
    return;
  }

  posStore.updateQuantity(product.sku, item.quantity - 1);
};

const removeFromOrder = (product: PosProduct) => {
  if (posStore.cashPaid > 0) {
    posStore.errorMsg =
      "Đơn đã nhận tiền mặt một phần, không được xóa sản phẩm.";
    return;
  }

  if (!product?.sku) {
    return;
  }

  posStore.removeFromCart(product.sku);
};
</script>

<style scoped>
.product-panel {
  min-height: 0;
}

.text-muted-custom {
  color: #64748b;
}

.font-sm {
  font-size: 0.875rem;
}

.font-xs {
  font-size: 0.75rem;
}

.shrink-0 {
  flex-shrink: 0;
}

.min-w-0 {
  min-width: 0;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(243, 198, 63, 0.55) rgba(15, 23, 42, 0.45);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 7px;
  height: 7px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.72);
  border-radius: 999px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: linear-gradient(
    180deg,
    rgba(243, 198, 63, 0.85),
    rgba(147, 197, 253, 0.5)
  );
  border-radius: 999px;
  border: 2px solid rgba(15, 23, 42, 0.82);
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(
    180deg,
    rgba(243, 198, 63, 1),
    rgba(147, 197, 253, 0.78)
  );
}

.product-search-wrapper {
  width: 330px;
  max-width: 38%;
}

.search-icon {
  position: absolute;
  left: 11px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 0.82rem;
  pointer-events: none;
}

.product-search-input {
  width: 100%;
  height: 36px;
  border-radius: 12px;
  border: 1px solid #22304d;
  background: #10182a;
  color: #f8fafc;
  padding: 0 34px 0 33px;
  font-size: 0.78rem;
  font-weight: 700;
  outline: none;
}

.product-search-input::placeholder {
  color: #64748b;
}

.product-search-input:focus {
  border-color: rgba(243, 198, 63, 0.72);
  box-shadow: 0 0 0 3px rgba(243, 198, 63, 0.08);
}

.btn-clear-search {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  color: #94a3b8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
}

.btn-clear-search:hover {
  background: rgba(239, 68, 68, 0.18);
  color: #fca5a5;
}

.luxury-pill {
  background-color: #131c31;
  border: 1px solid #222f4f;
  color: #9fb0d3;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.luxury-pill:hover:not(:disabled) {
  border-color: #384f80;
  color: #f1f5f9;
}

.luxury-pill:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.luxury-pill.active {
  background-color: #f3c63f !important;
  color: #000 !important;
  border-color: #f3c63f !important;
  font-weight: 900;
}

.product-table-wrapper {
  border: 1px solid #1d2740;
  background: #0b1120;
  min-height: 0;
}

.table-dark-custom {
  color: #dbeafe;
  background: #0b1120;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  font-size: 0.75rem;
}

.table-dark-custom thead th {
  position: sticky;
  top: 0;
  z-index: 5;
  background: #10182a;
  color: #9fb0d3;
  border-bottom: 1px solid #263654;
  padding: 6px 6px;
  text-transform: uppercase;
  font-size: 0.67rem;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.table-dark-custom tbody td {
  background: #0b1120;
  border-bottom: 1px solid #18243c;
  padding: 5px 6px;
  vertical-align: middle;
}

.table-dark-custom tbody tr:hover td {
  background: #121c31;
}

.table-dark-custom tbody tr.row-selected td {
  background: rgba(243, 198, 63, 0.055);
}

.table-dark-custom tbody tr.row-edit-held:hover td {
  background: rgba(243, 198, 63, 0.06);
}

.col-check {
  width: 44px;
}

.col-product {
  width: 35%;
}

.col-variant {
  width: 24%;
}

.col-price {
  width: 11%;
}

.col-stock {
  width: 58px;
}

.col-action {
  width: 160px;
}

.check-cell {
  white-space: nowrap;
}

.product-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #f3c63f;
}

.product-checkbox:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.product-cell {
  min-height: 34px;
}

.product-thumb {
  width: 30px;
  height: 30px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #263654;
  background: #111827;
}

.product-info {
  max-width: 100%;
}

.product-name-row {
  max-width: 100%;
  line-height: 1.1;
}

.product-name {
  color: #f8fafc;
  font-weight: 900;
  min-width: 0;
  max-width: 100%;
  font-size: 0.78rem;
}

.sku-inline {
  color: #93c5fd;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(147, 197, 253, 0.22);
  border-radius: 999px;
  padding: 1px 6px;
  font-size: 0.62rem;
  font-weight: 900;
  line-height: 1.15;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: none;
  overflow: visible;
  text-overflow: clip;
}

.product-category {
  color: #64748b;
  font-size: 0.68rem;
  max-width: 100%;
  margin-top: 2px;
  line-height: 1.1;
}

.variant-text {
  color: #f3c63f;
  font-weight: 900;
  max-width: 100%;
  font-size: 0.76rem;
}

.price-text {
  color: #f8fafc;
  white-space: nowrap;
  font-size: 0.76rem;
}

.stock-badge {
  display: inline-flex;
  min-width: 30px;
  justify-content: center;
  padding: 3px 7px;
  border-radius: 999px;
  font-weight: 900;
  font-size: 0.68rem;
}

.stock-ok {
  color: #86efac;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.stock-empty {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.product-qty-actions {
  min-width: 126px;
}

.qty-btn-grid,
.btn-remove-grid {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid #293957;
  background: rgba(15, 23, 42, 0.9);
  color: #dbeafe;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.15s ease;
}

.qty-btn-grid:hover:not(:disabled) {
  background: #f3c63f;
  color: #0b1120;
  border-color: #f3c63f;
}

.btn-remove-grid {
  color: #fca5a5;
  border-color: rgba(239, 68, 68, 0.35);
}

.btn-remove-grid:hover:not(:disabled) {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

.qty-btn-grid:disabled,
.btn-remove-grid:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.qty-number-grid {
  min-width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(243, 198, 63, 0.1);
  border: 1px solid rgba(243, 198, 63, 0.28);
  color: #f3c63f;
  font-weight: 900;
  font-size: 0.72rem;
}

.not-selected-text {
  color: #64748b;
  font-size: 0.66rem;
  font-weight: 700;
}

.row-disabled {
  opacity: 0.58;
}

.row-expired td {
  background: rgba(127, 29, 29, 0.08) !important;
}

.row-edit-held td {
  border-bottom-color: rgba(243, 198, 63, 0.16);
}

.table-hint {
  flex-shrink: 0;
  line-height: 1.25;
}

.spin {
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
/* ================= POS FILTER BAR ================= */

.filter-search-section {
  min-width: 0;
  flex-shrink: 0;
  padding: 2px 0 4px;
}

.filter-top-row,
.filter-bottom-row {
  min-width: 0;
}

.filter-top-row {
  align-items: center;
}

.filter-bottom-row {
  flex-wrap: wrap;
  row-gap: 6px;
}

/* Brand pill */
.category-slider {
  min-height: 34px;
}

.luxury-pill {
  height: 32px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
}

/* Search */
.product-search-wrapper {
  width: 330px;
  max-width: 38%;
}

.product-search-input {
  height: 34px;
  border-radius: 11px;
}

/* Select filters */
.pos-filter-select {
  height: 32px;
  min-width: 145px;
  border-radius: 10px;
  border: 1px solid #22304d;
  background: #10182a;
  color: #dbeafe;
  padding: 0 10px;
  font-size: 0.72rem;
  font-weight: 800;
  outline: none;
}

.pos-filter-select:focus {
  border-color: rgba(243, 198, 63, 0.72);
  box-shadow: 0 0 0 3px rgba(243, 198, 63, 0.08);
}

.pos-filter-select:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.stock-filter {
  min-width: 125px;
}

.btn-reset-filter {
  height: 32px;
  border-radius: 10px;
  border: 1px solid rgba(243, 198, 63, 0.35);
  background: rgba(243, 198, 63, 0.08);
  color: #f3c63f;
  padding: 0 11px;
  font-size: 0.7rem;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.btn-reset-filter:hover:not(:disabled) {
  background: rgba(243, 198, 63, 0.16);
  border-color: rgba(243, 198, 63, 0.75);
}

.btn-reset-filter:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 1200px) {
  .filter-top-row {
    flex-direction: column;
    align-items: stretch !important;
  }

  .product-search-wrapper {
    width: 100%;
    max-width: 100%;
  }

  .filter-bottom-row {
    display: grid !important;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pos-filter-select,
  .btn-reset-filter {
    width: 100%;
    min-width: 0;
  }
}
</style>
