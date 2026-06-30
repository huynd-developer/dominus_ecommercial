<template>
  <div class="home-view">
    <div class="container-fluid px-3 px-lg-5 py-4">
      <HomeBanner />

      <section class="product-section mt-5">
        <div
          class="section-header d-flex align-items-center justify-content-between mb-4"
        >
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h2 class="section-title mb-0">FLASH SALE</h2>
            <CountdownTimer />
          </div>

          <RouterLink to="/flash-sale" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div
            v-for="product in flashSaleProducts"
            :key="product.id"
            class="col"
          >
            <ProductCard :product="product" />
          </div>
        </div>
      </section>

      <section class="product-section mt-5">
        <div
          class="section-header d-flex align-items-center justify-content-between mb-4"
        >
          <h2 class="section-title mb-0">NƯỚC HOA MỚI NHẤT</h2>

          <RouterLink to="/products/new" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div v-for="product in newestProducts" :key="product.id" class="col">
            <ProductCard :product="product" />
          </div>
        </div>
      </section>

      <section class="special-collection mt-5">
        <div class="row g-0 align-items-stretch special-collection-inner">
          <div class="col-12 col-lg-5">
            <div class="collection-image-card h-100">
              <img
                :src="collectionImage"
                alt="Bộ sưu tập đặc biệt"
                class="collection-main-image"
              />
            </div>
          </div>

          <div class="col-12 col-lg-3">
            <div class="collection-copy h-100">
              <p class="collection-kicker mb-2">BỘ SƯU TẬP</p>
              <h2 class="collection-title mb-3">ĐẶC BIỆT</h2>
              <p class="collection-desc mb-4">
                Tuyển chọn những tuyệt tác hương thơm hiếm có, dành riêng cho
                những dấu ấn khác biệt.
              </p>

              <RouterLink to="/collections/special" class="btn collection-btn">
                KHÁM PHÁ NGAY
                <i class="bi bi-arrow-right-short ms-1"></i>
              </RouterLink>
            </div>
          </div>

          <div
            v-for="product in specialProducts"
            :key="product.id"
            class="col-12 col-md-6 col-lg-2 special-product-col"
          >
            <div class="special-product-wrap h-100">
              <ProductCard :product="product" />
            </div>
          </div>
        </div>
      </section>

      <section class="product-section mt-5 mb-5">
        <div
          class="section-header d-flex align-items-center justify-content-between mb-4"
        >
          <h2 class="section-title mb-0">SẢN PHẨM NỔI BẬT</h2>

          <RouterLink to="/products/featured" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div
            v-for="product in featuredProducts"
            :key="product.id"
            class="col"
          >
            <ProductCard :product="product" />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from "vue-router";
import HomeBanner from "../components/HomeBanner.vue";
import CountdownTimer from "../components/CountdownTimer.vue";
import ProductCard from "../components/ProductCard.vue";
import collectionImage from "@/assets/images/collection-aura.png";
import type { Product } from "../components/ProductCard.vue";

const flashSaleProducts: Product[] = [
  {
    id: 1,
    name: "Bleu De Chanel EDP",
    brand: "Chanel",
    color: "#07111f",
    salePrice: 2625000,
    originalPrice: 3500000,
    discountPercent: 25,
    rating: 4.8,
    reviewCount: 128,
  },
  {
    id: 2,
    name: "Miss Dior Blooming Bouquet",
    brand: "Dior",
    color: "#eaa6ba",
    salePrice: 2550000,
    originalPrice: 3400000,
    discountPercent: 25,
    rating: 4.9,
    reviewCount: 96,
  },
  {
    id: 3,
    name: "Sauvage EDP",
    brand: "Dior",
    color: "#0b1f3a",
    salePrice: 2625000,
    originalPrice: 3500000,
    discountPercent: 25,
    rating: 4.8,
    reviewCount: 154,
  },
  {
    id: 4,
    name: "1 Million EDT",
    brand: "Paco Rabanne",
    color: "#c99a36",
    salePrice: 2550000,
    originalPrice: 3400000,
    discountPercent: 25,
    rating: 4.7,
    reviewCount: 87,
  },
];

const newestProducts: Product[] = [
  {
    id: 5,
    name: "My Way EDP",
    brand: "Giorgio Armani",
    color: "#f0a7bc",
    salePrice: 2950000,
    originalPrice: 3600000,
    discountPercent: 18,
    rating: 4.8,
    reviewCount: 102,
  },
  {
    id: 6,
    name: "Libre Le Parfum",
    brand: "Yves Saint Laurent",
    color: "#b86b2b",
    salePrice: 3350000,
    originalPrice: 4200000,
    discountPercent: 20,
    rating: 4.9,
    reviewCount: 87,
  },
  {
    id: 7,
    name: "J'adore Parfum d'Eau",
    brand: "Dior",
    color: "#f5f1df",
    salePrice: 3300000,
    originalPrice: 4100000,
    discountPercent: 20,
    rating: 4.8,
    reviewCount: 90,
  },
  {
    id: 8,
    name: "Gentleman Givenchy EDP",
    brand: "Givenchy",
    color: "#111111",
    salePrice: 2850000,
    originalPrice: 3500000,
    discountPercent: 19,
    rating: 4.7,
    reviewCount: 78,
  },
];

const specialProducts: Product[] = [
  {
    id: 9,
    name: "Aventus EDP",
    brand: "Creed",
    color: "#f5f5f0",
    salePrice: 6200000,
    originalPrice: 7600000,
    discountPercent: 18,
    rating: 4.9,
    reviewCount: 88,
  },
  {
    id: 10,
    name: "Blanche EDP",
    brand: "Byredo",
    color: "#f1eadf",
    salePrice: 6800000,
    originalPrice: 8000000,
    discountPercent: 15,
    rating: 4.7,
    reviewCount: 62,
  },
];

const featuredProducts: Product[] = [
  {
    id: 11,
    name: "Black Opium EDP",
    brand: "Yves Saint Laurent",
    color: "#1a1116",
    salePrice: 2625000,
    originalPrice: 3500000,
    discountPercent: 25,
    rating: 4.8,
    reviewCount: 135,
  },
  {
    id: 12,
    name: "Tobacco Vanille EDP",
    brand: "Tom Ford",
    color: "#4b2a18",
    salePrice: 4900000,
    originalPrice: 6600000,
    discountPercent: 20,
    rating: 4.9,
    reviewCount: 96,
  },
  {
    id: 13,
    name: "Baccarat Rouge 540 EDP",
    brand: "Maison Francis Kurkdjian",
    color: "#9c1f16",
    salePrice: 7200000,
    originalPrice: 9600000,
    discountPercent: 20,
    rating: 4.9,
    reviewCount: 125,
  },
  {
    id: 14,
    name: "Santal 33 EDP",
    brand: "Le Labo",
    color: "#e7dfce",
    salePrice: 6800000,
    originalPrice: 8000000,
    discountPercent: 15,
    rating: 4.8,
    reviewCount: 112,
  },
];
</script>

<style scoped>
.home-view {
  color: var(--aura-black);
}

.section-title {
  font-family: Arial, sans-serif !important;
  font-size: clamp(28px, 2.4vw, 38px);
  color: var(--aura-black);
  font-weight: 700;
  letter-spacing: -0.5px;
  line-height: 1.15;
}

.view-all-link {
  color: var(--aura-black);
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  transition: color 0.22s ease;
}

.view-all-link:hover {
  color: var(--aura-gold);
}

.special-collection {
  border-radius: 22px;
  overflow: hidden;
  background: #fffaf2;
  border: 1px solid rgba(189, 154, 95, 0.22);
  box-shadow: 0 22px 55px rgba(5, 16, 36, 0.08);
}

.special-collection-inner {
  min-height: 430px;
  background: #fffaf2;
}

/* Ảnh bên trái */
.collection-image-card {
  height: 100%;
  background: #fffaf2;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 0;
  overflow: hidden;
}

/* Quan trọng: cover để ảnh ôm full khung */
.collection-main-image {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 430px;
  object-fit: cover;
  object-position: center center;
  border-radius: 0;
}

/* Phần chữ giữa */
.collection-copy {
  height: 100%;
  background: #fffaf2;
  padding: 52px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid rgba(189, 154, 95, 0.14);
  border-right: 1px solid rgba(189, 154, 95, 0.14);
}

.collection-kicker {
  color: var(--aura-gold);
  letter-spacing: 5px;
  font-size: 12px;
  font-weight: 800;
}

.collection-title {
  font-family: var(--aura-serif);
  font-size: 42px;
  font-weight: 800;
  color: var(--aura-black);
  line-height: 1.05;
  letter-spacing: -0.5px;
}

.collection-desc {
  color: #555555;
  font-size: 15px;
  line-height: 1.9;
}

.collection-btn {
  width: fit-content;
  background: var(--aura-gold);
  color: #ffffff;
  border-radius: 6px;
  border: none;
  padding: 12px 24px;
  font-size: 13px;
  font-weight: 800;
  transition: all 0.22s ease;
}

.collection-btn:hover {
  background: var(--aura-gold-hover);
  color: #ffffff;
  transform: translateY(-2px);
}

/* 2 card bên phải */
.special-product-col {
  background: #fffaf2;
  padding: 22px 16px;
}

.special-product-wrap {
  height: 100%;
}

.special-product-wrap :deep(.product-card) {
  height: 100%;
  background: #ffffff;
}

.special-product-wrap :deep(.product-image-wrapper) {
  height: 210px;
}

.special-product-wrap :deep(.product-bottle) {
  transform: scale(0.9);
}

.special-product-wrap :deep(.product-card:hover .product-bottle) {
  transform: scale(0.95);
}

.special-product-wrap :deep(.product-name) {
  font-size: 16px;
}

@media (max-width: 991.98px) {
  .collection-main-image {
    min-height: 360px;
  }

  .collection-copy {
    border-left: none;
    border-right: none;
    border-top: 1px solid rgba(189, 154, 95, 0.14);
    border-bottom: 1px solid rgba(189, 154, 95, 0.14);
  }
}

@media (max-width: 767.98px) {
  .section-title {
    font-size: 24px;
  }

  .collection-main-image {
    min-height: 300px;
  }

  .collection-copy {
    padding: 34px 24px;
  }

  .collection-title {
    font-size: 34px;
  }
}
</style>
