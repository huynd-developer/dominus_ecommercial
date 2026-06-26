<template>
  <div class="product-card card h-100 border-0 position-relative overflow-hidden">
    <span v-if="product.discountPercent" class="discount-badge">
      -{{ product.discountPercent }}%
    </span>

    <div class="product-image-wrapper">
      <img
        :src="product.image"
        :alt="product.name"
        class="product-image img-fluid"
      />
    </div>

    <div class="card-body px-3 pb-3 pt-2">
      <h3 class="product-name text-truncate mb-1">
        {{ product.name }}
      </h3>

      <p class="product-brand text-muted mb-2">
        {{ product.brand }}
      </p>

      <div class="rating-row d-flex align-items-center gap-1 mb-2">
        <span class="stars">★★★★★</span>
        <span class="review-count">({{ product.reviewCount }})</span>
      </div>

      <div class="price-row d-flex align-items-center gap-2 mb-3 flex-wrap">
        <span class="sale-price">
          {{ formatCurrency(product.salePrice) }}
        </span>

        <span class="original-price text-decoration-line-through">
          {{ formatCurrency(product.originalPrice) }}
        </span>
      </div>

      <button type="button" class="btn add-cart-btn w-100">
        <i class="bi bi-bag-plus me-2"></i>
        Thêm vào giỏ
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
export interface Product {
  id: number;
  name: string;
  brand: string;
  image: string;
  salePrice: number;
  originalPrice: number;
  discountPercent: number;
  reviewCount: number;
}

defineProps<{
  product: Product;
}>();

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('vi-VN').format(value) + ' đ';
};
</script>

<style scoped>
.product-card {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(10, 25, 47, 0.08) !important;
  box-shadow: 0 4px 16px rgba(10, 25, 47, 0.05);
  transition: all 0.25s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(10, 25, 47, 0.12);
}

.discount-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 3;
  background: #dc3545;
  color: #ffffff;
  border-radius: 6px;
  padding: 4px 9px;
  font-size: 12px;
  font-weight: 800;
}

.product-image-wrapper {
  height: 190px;
  padding: 18px 18px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image {
  max-height: 165px;
  width: 100%;
  object-fit: contain;
  transition: transform 0.25s ease;
}

.product-card:hover .product-image {
  transform: scale(1.06);
}

.product-name {
  font-size: 14px;
  font-weight: 700;
  color: #0a192f;
}

.product-brand {
  font-size: 13px;
  line-height: 1.2;
}

.stars {
  color: #f5b301;
  font-size: 13px;
  letter-spacing: 1px;
}

.review-count {
  color: #6c757d;
  font-size: 12px;
}

.sale-price {
  color: #c82333;
  font-size: 15px;
  font-weight: 800;
}

.original-price {
  color: #8a8f98;
  font-size: 12px;
}

.add-cart-btn {
  border: 1px solid #bd9a5f;
  color: #bd9a5f;
  background: #ffffff;
  font-size: 13px;
  font-weight: 700;
  border-radius: 8px;
  padding: 8px 10px;
  transition: all 0.2s ease;
}

.add-cart-btn i {
  font-size: 15px;
}

.add-cart-btn:hover {
  background: #bd9a5f;
  color: #ffffff;
}

@media (max-width: 767.98px) {
  .product-image-wrapper {
    height: 150px;
  }

  .product-image {
    max-height: 125px;
  }

  .product-name {
    font-size: 13px;
  }

  .sale-price {
    font-size: 14px;
  }
}
</style>