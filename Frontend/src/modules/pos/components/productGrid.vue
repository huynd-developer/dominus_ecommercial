<template>
  <div class="d-flex flex-column h-100 pe-1 select-none">
    <!-- Bộ lọc danh mục / thương hiệu / nhóm -->
    <div class="filter-search-section mb-3 shrink-0">
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

    <!-- Danh sách biến thể sản phẩm -->
    <div class="flex-grow-1 overflow-y-auto pe-2 product-scroll-container">
      <div
        v-if="posStore.isLoading"
        class="text-center py-5 text-muted-custom font-sm"
      >
        <i class="bi bi-arrow-repeat spin fs-3"></i>
        <p class="mt-2">Đang tải sản phẩm từ máy chủ...</p>
      </div>

      <div
        v-else-if="posStore.filteredProducts.length === 0"
        class="text-center py-5 text-muted-custom font-sm"
      >
        Không tìm thấy sản phẩm phù hợp.
      </div>

      <div
        v-else
        class="row g-3 row-cols-2 row-cols-md-3 row-cols-lg-3 row-cols-xl-3 row-cols-xxl-4 pb-4"
      >
        <div
          class="col"
          v-for="product in posStore.filteredProducts"
          :key="product.sku"
        >
          <button
            type="button"
            class="product-luxury-card rounded-3 overflow-hidden position-relative h-100 w-100 d-flex flex-column border transition-all text-start"
            :class="{ 'is-out-of-stock': product.stockQuantity <= 0 }"
            :disabled="product.stockQuantity <= 0"
            @click="handleAddToCart(product)"
            :title="getProductTitle(product)"
          >
            <!-- Overlay hết hàng -->
            <div
              v-if="product.stockQuantity <= 0"
              class="out-stock-overlay position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
            >
              <span class="text-white fw-bold bg-danger px-3 py-1 rounded">
                HẾT HÀNG
              </span>
            </div>

            <!-- Ảnh + giá -->
            <div class="img-wrapper position-relative w-100">
              <img
                :src="product.image || placeholderImage(product.name)"
                class="w-100 h-100 object-fit-cover item-img"
                :alt="getProductTitle(product)"
              />

              <div
                class="premium-price-tag position-absolute fw-black text-dark px-2 py-1 rounded-2 shadow-sm font-sm"
              >
                {{ formatPrice(product.price) }} ₫
              </div>

              <div
                class="stock-badge position-absolute px-2 py-1 rounded-2 font-xs fw-bold"
                :class="product.stockQuantity > 0 ? 'stock-ok' : 'stock-empty'"
              >
                Kho: {{ product.stockQuantity }}
              </div>
            </div>

            <!-- Nội dung card -->
            <div class="p-3 card-body-bg d-flex flex-column justify-content-between flex-grow-1">
              <div>
                <!-- Tên sản phẩm cha -->
                <h6
                  class="mb-1 text-light-custom fw-bold text-truncate font-sm"
                  :title="product.name"
                >
                  {{ product.name }}
                </h6>

                <!-- Biến thể: dung tích / loại chai -->
                <div
                  class="variant-line text-gold-soft font-xs fw-bold text-truncate mb-2"
                  :title="getVariantText(product)"
                >
                  <i class="bi bi-droplet-half me-1"></i>
                  {{ getVariantText(product) }}
                </div>

                <!-- SKU -->
                <div class="sku-box d-flex align-items-center gap-1 mb-2">
                  <i class="bi bi-upc-scan text-muted-custom font-xs"></i>
                  <span
                    class="text-muted-custom font-xs text-truncate"
                    :title="product.sku"
                  >
                    SKU: {{ product.sku }}
                  </span>
                </div>
              </div>

              <!-- Dòng cuối -->
              <div class="d-flex justify-content-between align-items-center mt-1">
                <span
                  class="category-chip text-truncate"
                  :title="product.category"
                >
                  {{ product.category || "Chưa phân loại" }}
                </span>

                <span
                  class="add-hint font-xs fw-bold"
                  v-if="product.stockQuantity > 0"
                >
                  Bấm chọn
                </span>

                <span class="text-danger font-xs fw-bold" v-else>
                  Hết hàng
                </span>
              </div>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { usePosStore } from "@/modules/pos/stores/posStore";

const posStore = usePosStore();

const formatPrice = (val) => {
  return new Intl.NumberFormat("vi-VN").format(val || 0);
};

const placeholderImage = (name) => {
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(
    name || "Product"
  )}&background=random&color=fff&size=200`;
};

const getVariantText = (product) => {
  return product.subName && product.subName.trim()
    ? product.subName
    : "Biến thể mặc định";
};

const getProductTitle = (product) => {
  return `${product.name} - ${getVariantText(product)} - ${product.sku}`;
};

const handleAddToCart = (product) => {
  if (product.stockQuantity <= 0) return;
  posStore.addToCart(product);
};
</script>

<style scoped>
.text-muted-custom {
  color: #64748b;
}

.text-light-custom {
  color: #f1f5f9;
}

.text-gold-soft {
  color: #f3c63f;
}

.font-sm {
  font-size: 0.875rem;
}

.font-xs {
  font-size: 0.75rem;
}

.fw-black {
  font-weight: 900;
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
  padding: 8px 20px;
  border-radius: 30px;
  font-size: 0.8rem;
  font-weight: 600;
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
  color: #000000 !important;
  border-color: #f3c63f !important;
  font-weight: 800;
  box-shadow: 0 4px 10px rgba(243, 198, 63, 0.15);
}

.product-luxury-card {
  background-color: #131c31;
  border-color: #222f4f !important;
  cursor: pointer;
  padding: 0;
  color: inherit;
}

.product-luxury-card:hover:not(:disabled) {
  border-color: rgba(243, 198, 63, 0.4) !important;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4);
}

.product-luxury-card:hover:not(:disabled) .item-img {
  transform: scale(1.04);
}

.product-luxury-card:active:not(:disabled) {
  transform: scale(0.97);
}

.product-luxury-card:disabled {
  cursor: not-allowed;
}

.product-luxury-card.is-out-of-stock {
  opacity: 0.55;
}

.out-stock-overlay {
  background: rgba(0, 0, 0, 0.62);
  z-index: 10;
}

.img-wrapper {
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background-color: #070c18;
}

.item-img {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.premium-price-tag {
  bottom: 10px;
  right: 10px;
  background-color: #f3c63f;
  color: #000000 !important;
  font-weight: 800;
}

.stock-badge {
  top: 10px;
  left: 10px;
  backdrop-filter: blur(6px);
}

.stock-ok {
  background: rgba(15, 23, 42, 0.8);
  color: #dbeafe;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.stock-empty {
  background: rgba(239, 68, 68, 0.85);
  color: #fff;
}

.card-body-bg {
  background-color: #131c31;
}

.variant-line {
  max-width: 100%;
}

.sku-box {
  min-width: 0;
}

.category-chip {
  max-width: 60%;
  background-color: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.14);
  color: #94a3b8;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 0.7rem;
  font-weight: 700;
}

.add-hint {
  color: #f3c63f;
}

.transition-all {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Thanh cuộn grid */
.product-scroll-container::-webkit-scrollbar {
  width: 6px;
}

.product-scroll-container::-webkit-scrollbar-track {
  background: transparent;
}

.product-scroll-container::-webkit-scrollbar-thumb {
  background: #1d2740;
  border-radius: 4px;
}

.product-scroll-container::-webkit-scrollbar-thumb:hover {
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