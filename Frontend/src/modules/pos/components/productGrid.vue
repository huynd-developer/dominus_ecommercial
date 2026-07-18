<template>
  <div class="product-panel d-flex flex-column h-100 select-none">
    <div class="filter-search-section mb-2 shrink-0">
      <div class="category-slider d-flex gap-2 overflow-auto pb-1 no-scrollbar">
        <button
          v-for="cat in posStore.categories"
          :key="cat"
          type="button"
          :class="['luxury-pill', { active: posStore.selectedCategory === cat }]"
          :disabled="posStore.cashPaid > 0"
          @click="posStore.selectedCategory = cat"
        >
          {{ cat }}
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
      v-else-if="posStore.filteredProducts.length === 0"
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
            <th class="col-product">Sản phẩm</th>
            <th class="col-variant">Biến thể</th>
            <th class="text-end col-price">Giá</th>
            <th class="text-center col-stock">Kho</th>
            <th class="text-end col-action">Thao tác</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="product in posStore.filteredProducts"
            :key="product.sku"
            :class="{
              'row-disabled': isProductDisabled(product),
              'row-expired': isExpired(product),
              'row-edit-held': posStore.activeHeldOrderId && !posStore.cashPaid,
            }"
            :title="getDisabledReason(product) || getProductTitle(product)"
          >
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

            <td class="text-end">
              <button
                type="button"
                class="btn-add-product"
                :disabled="isProductDisabled(product)"
                :title="getDisabledReason(product) || buttonTitle"
                @click.stop="handleAddToCart(product)"
              >
                <i class="bi bi-plus-lg"></i>
                {{ posStore.activeHeldOrderId ? "Chọn" : "Chọn" }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="table-hint mt-2 font-xs text-muted-custom">
      <i class="bi bi-info-circle me-1"></i>

      <template v-if="posStore.cashPaid > 0">
        Đơn đã nhận tiền mặt một phần: không được thêm sản phẩm, sửa số lượng hoặc đổi voucher.
      </template>

      <template v-else-if="posStore.activeHeldOrderId">
        Đang sửa phiếu treo: được thêm sản phẩm, sửa số lượng, đổi voucher; không được đổi khách hàng.
      </template>

      <template v-else>
        POS chỉ cho chọn sản phẩm còn hàng, đang hoạt động và đủ điều kiện bán.
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { usePosStore } from "@/modules/pos/stores/posStore";

const posStore = usePosStore();

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
   * QUAN TRỌNG:
   * Không khóa sản phẩm bằng activeHeldOrderId.
   * Mở phiếu treo vẫn phải thêm được sản phẩm.
   * Chỉ khóa khi đã nhận tiền mặt tạm.
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

const handleAddToCart = (product: any) => {
  const reason = getDisabledReason(product);

  if (reason) {
    posStore.errorMsg = reason;
    return;
  }

  posStore.addToCart(product);
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
  padding: 7px 9px;
  text-transform: uppercase;
  font-size: 0.67rem;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.table-dark-custom tbody td {
  background: #0b1120;
  border-bottom: 1px solid #18243c;
  padding: 6px 9px;
  vertical-align: middle;
}

.table-dark-custom tbody tr:hover td {
  background: #121c31;
}

.table-dark-custom tbody tr.row-edit-held:hover td {
  background: rgba(243, 198, 63, 0.06);
}

.col-product {
  width: 48%;
}

.col-variant {
  width: 23%;
}

.col-price {
  width: 12%;
}

.col-stock {
  width: 7%;
}

.col-action {
  width: 10%;
}

.product-cell {
  min-height: 38px;
}

.product-thumb {
  width: 34px;
  height: 34px;
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
  max-width: 112px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.btn-add-product {
  background: rgba(243, 198, 63, 0.1);
  color: #f3c63f;
  border: 1px solid rgba(243, 198, 63, 0.35);
  border-radius: 8px;
  padding: 5px 9px;
  font-size: 0.7rem;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-add-product:hover:not(:disabled) {
  background: #f3c63f;
  color: #0b1120;
  border-color: #f3c63f;
}

.btn-add-product:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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
</style>