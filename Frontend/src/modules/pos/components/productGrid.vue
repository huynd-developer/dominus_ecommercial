<template>
  <div class="product-panel d-flex flex-column h-100 select-none">
    <!-- Bộ lọc -->
    <div class="filter-search-section mb-2 shrink-0">
      <div class="category-slider d-flex gap-2 overflow-auto pb-2 no-scrollbar">
        <button
          v-for="cat in posStore.categories"
          :key="cat"
          type="button"
          :class="['luxury-pill', { active: posStore.selectedCategory === cat }]"
          @click="posStore.selectedCategory = cat"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="posStore.isLoading"
      class="flex-grow-1 d-flex flex-column align-items-center justify-content-center text-muted-custom font-sm"
    >
      <i class="bi bi-arrow-repeat spin fs-3"></i>
      <p class="mt-2 mb-0">Đang tải sản phẩm từ máy chủ...</p>
    </div>

    <!-- Empty -->
    <div
      v-else-if="posStore.filteredProducts.length === 0"
      class="flex-grow-1 d-flex align-items-center justify-content-center text-muted-custom font-sm"
    >
      Không tìm thấy sản phẩm phù hợp.
    </div>

    <!-- Table sản phẩm -->
    <div v-else class="product-table-wrapper flex-grow-1 overflow-auto rounded-3">
      <table class="table table-dark-custom align-middle mb-0">
        <thead>
          <tr>
            <th class="col-product">Sản phẩm</th>
            <th class="col-variant">Biến thể</th>
            <th class="col-sku">SKU</th>
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
              'row-disabled': product.stockQuantity <= 0 || posStore.isOrderLocked
            }"
          >
            <td>
              <div class="d-flex align-items-center gap-2 min-w-0">
                <img
                  :src="product.image || placeholderImage(product.name)"
                  :alt="getProductTitle(product)"
                  class="product-thumb shrink-0"
                />

                <div class="min-w-0">
                  <div
                    class="product-name text-truncate"
                    :title="product.name"
                  >
                    {{ product.name }}
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

            <td>
              <span
                class="sku-text text-truncate d-inline-block"
                :title="product.sku"
              >
                {{ product.sku }}
              </span>
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
                :disabled="product.stockQuantity <= 0 || posStore.isOrderLocked"
                @click.stop="handleAddToCart(product)"
              >
                <i class="bi bi-plus-lg"></i>
                Chọn
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="table-hint mt-2 font-xs text-muted-custom">
      <i class="bi bi-info-circle me-1"></i>
      Chỉ bấm nút <strong>Chọn</strong> để thêm sản phẩm vào giỏ.
    </div>
  </div>
</template>

<script setup lang="ts">
import { usePosStore } from "@/modules/pos/stores/posStore";

const posStore = usePosStore();

const formatPrice = (val?: number | null) => {
  return new Intl.NumberFormat("vi-VN").format(Number(val || 0));
};

const placeholderImage = (name?: string) => {
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(
    name || "Product"
  )}&background=random&color=fff&size=200`;
};

const getVariantText = (product: any) => {
  return product.subName && String(product.subName).trim()
    ? product.subName
    : "Biến thể mặc định";
};

const getProductTitle = (product: any) => {
  return `${product.name} - ${getVariantText(product)} - ${product.sku}`;
};

const handleAddToCart = (product: any) => {
  if (!product) return;
  if (product.stockQuantity <= 0) return;
  if (posStore.isOrderLocked) return;

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

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.luxury-pill {
  background-color: #131c31;
  border: 1px solid #222f4f;
  color: #9fb0d3;
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.luxury-pill:hover {
  border-color: #384f80;
  color: #f1f5f9;
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
  width: 100%;
  color: #dbeafe;
  background: #0b1120;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 0.78rem;
}

.table-dark-custom thead th {
  position: sticky;
  top: 0;
  z-index: 5;
  background: #111a2f;
  color: #94a3b8;
  border-bottom: 1px solid #263654;
  padding: 9px 10px;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

.table-dark-custom tbody td {
  background: #0b1120;
  border-bottom: 1px solid #18243c;
  padding: 8px 10px;
  vertical-align: middle;
}

.table-dark-custom tbody tr {
  cursor: default;
  transition: all 0.12s ease;
}

.table-dark-custom tbody tr:hover td {
  background: #121c31;
}

.row-disabled {
  opacity: 0.52;
}

.product-thumb {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  background: #070c18;
  border: 1px solid #253553;
}

.product-name {
  color: #f8fafc;
  font-weight: 800;
  max-width: 220px;
}

.product-category {
  color: #64748b;
  font-size: 0.7rem;
  max-width: 220px;
}

.variant-text {
  color: #f3c63f;
  font-weight: 800;
  max-width: 170px;
}

.sku-text {
  color: #94a3b8;
  font-size: 0.72rem;
  max-width: 130px;
}

.price-text {
  color: #f8fafc;
  white-space: nowrap;
}

.stock-badge {
  display: inline-flex;
  min-width: 34px;
  justify-content: center;
  padding: 3px 7px;
  border-radius: 999px;
  font-weight: 900;
  font-size: 0.72rem;
}

.stock-ok {
  background: rgba(34, 197, 94, 0.1);
  color: #86efac;
  border: 1px solid rgba(34, 197, 94, 0.22);
}

.stock-empty {
  background: rgba(239, 68, 68, 0.12);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.25);
}

.btn-add-product {
  border: 1px solid rgba(243, 198, 63, 0.35);
  background: rgba(243, 198, 63, 0.08);
  color: #f3c63f;
  border-radius: 8px;
  padding: 5px 9px;
  font-size: 0.72rem;
  font-weight: 900;
  white-space: nowrap;
  transition: all 0.15s ease;
  cursor: pointer;
}

.btn-add-product:hover:not(:disabled) {
  background: #f3c63f;
  color: #0b1120;
}

.btn-add-product:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.min-w-0 {
  min-width: 0;
}

.shrink-0 {
  flex-shrink: 0;
}

.col-product {
  min-width: 260px;
}

.col-variant {
  min-width: 170px;
}

.col-sku {
  min-width: 130px;
}

.col-price {
  min-width: 110px;
}

.col-stock {
  min-width: 65px;
}

.col-action {
  min-width: 85px;
}

.table-hint {
  padding-left: 2px;
}

.product-table-wrapper::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.product-table-wrapper::-webkit-scrollbar-track {
  background: #070c18;
}

.product-table-wrapper::-webkit-scrollbar-thumb {
  background: #1d2740;
  border-radius: 4px;
}

.product-table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #384f80;
}

.spin {
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}
</style>