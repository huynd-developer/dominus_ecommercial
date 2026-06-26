<template>
  <div class="home-view bg-light">
    <div class="container-fluid px-3 px-lg-5 py-4">
      <HomeBanner />

      <!-- FLASH SALE -->
      <section class="product-section mt-4">
        <div class="section-header d-flex align-items-center justify-content-between mb-3">
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h2 class="section-title mb-0">FLASH SALE</h2>
            <CountdownTimer />
          </div>

          <RouterLink to="/flash-sale" class="view-all-link">
            Xem tất cả ›
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-6 g-3">
          <div
            v-for="product in flashSaleProducts"
            :key="product.id"
            class="col"
          >
            <ProductCard :product="product" />
          </div>
        </div>
      </section>

      <!-- NEW PRODUCTS -->
      <section class="product-section mt-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-3">
          <h2 class="section-title mb-0">NƯỚC HOA MỚI NHẤT</h2>

          <RouterLink to="/products/new" class="view-all-link">
            Xem tất cả ›
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-6 g-3">
          <div
            v-for="product in newestProducts"
            :key="product.id"
            class="col"
          >
            <ProductCard :product="product" />
          </div>
        </div>
      </section>

      <!-- FEATURED PRODUCTS -->
      <section class="product-section mt-5 mb-4">
        <div class="section-header d-flex align-items-center justify-content-between mb-3">
          <h2 class="section-title mb-0">SẢN PHẨM NỔI BẬT</h2>

          <RouterLink to="/products/featured" class="view-all-link">
            Xem tất cả ›
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-6 g-3">
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
import { RouterLink } from 'vue-router';
import HomeBanner from '../components/HomeBanner.vue';
import CountdownTimer from '../components/CountdownTimer.vue';
import ProductCard from '../components/ProductCard.vue';
import type { Product } from '../components/ProductCard.vue';

const flashSaleProducts: Product[] = [
  {
    id: 1,
    name: 'Bleu De Chanel EDP',
    brand: 'Chanel',
    image: 'https://i.imgur.com/7M6p09L.png',
    salePrice: 2625000,
    originalPrice: 3500000,
    discountPercent: 25,
    reviewCount: 126
  },
  {
    id: 2,
    name: 'Miss Dior Blooming Bouquet',
    brand: 'Dior',
    image: 'https://i.imgur.com/tgn7C3h.png',
    salePrice: 2550000,
    originalPrice: 3400000,
    discountPercent: 25,
    reviewCount: 98
  },
  {
    id: 3,
    name: 'Sauvage EDP',
    brand: 'Dior',
    image: 'https://i.imgur.com/6uC8J8C.png',
    salePrice: 2625000,
    originalPrice: 3500000,
    discountPercent: 25,
    reviewCount: 154
  },
  {
    id: 4,
    name: '1 Million EDT',
    brand: 'Paco Rabanne',
    image: 'https://i.imgur.com/fOXYE9A.png',
    salePrice: 2550000,
    originalPrice: 3400000,
    discountPercent: 25,
    reviewCount: 87
  },
  {
    id: 5,
    name: 'Black Opium EDP',
    brand: 'Yves Saint Laurent',
    image: 'https://i.imgur.com/WLqqeTc.png',
    salePrice: 2625000,
    originalPrice: 3500000,
    discountPercent: 25,
    reviewCount: 113
  },
  {
    id: 6,
    name: 'Bright Crystal EDT',
    brand: 'Versace',
    image: 'https://i.imgur.com/PxM4bPL.png',
    salePrice: 2250000,
    originalPrice: 3000000,
    discountPercent: 25,
    reviewCount: 78
  }
];

const newestProducts: Product[] = [
  {
    id: 7,
    name: 'Paradoxe EDP',
    brand: 'Prada',
    image: 'https://i.imgur.com/uZ0XMDu.png',
    salePrice: 3200000,
    originalPrice: 3900000,
    discountPercent: 18,
    reviewCount: 94
  },
  {
    id: 8,
    name: 'My Way EDP',
    brand: 'Giorgio Armani',
    image: 'https://i.imgur.com/60r3NZu.png',
    salePrice: 2950000,
    originalPrice: 3600000,
    discountPercent: 18,
    reviewCount: 86
  },
  {
    id: 9,
    name: 'Libre Le Parfum',
    brand: 'Yves Saint Laurent',
    image: 'https://i.imgur.com/ULWt4Hy.png',
    salePrice: 3350000,
    originalPrice: 4200000,
    discountPercent: 20,
    reviewCount: 75
  },
  {
    id: 10,
    name: "J'adore Parfum d'Eau",
    brand: 'Dior',
    image: 'https://i.imgur.com/6h0o4Di.png',
    salePrice: 3300000,
    originalPrice: 4100000,
    discountPercent: 20,
    reviewCount: 67
  },
  {
    id: 11,
    name: 'Gentleman Givenchy EDP',
    brand: 'Givenchy',
    image: 'https://i.imgur.com/a8xxXG8.png',
    salePrice: 2850000,
    originalPrice: 3500000,
    discountPercent: 19,
    reviewCount: 64
  },
  {
    id: 12,
    name: 'Wulóng Chá Extrait',
    brand: 'Nishane',
    image: 'https://i.imgur.com/RU2awpz.png',
    salePrice: 6250000,
    originalPrice: 7400000,
    discountPercent: 16,
    reviewCount: 52
  }
];

const featuredProducts: Product[] = [
  {
    id: 13,
    name: "N°5 Chanel EDP",
    brand: 'Chanel',
    image: 'https://i.imgur.com/0IkLM9x.png',
    salePrice: 3780000,
    originalPrice: 4600000,
    discountPercent: 18,
    reviewCount: 111
  },
  {
    id: 14,
    name: 'Creed Aventus',
    brand: 'Creed',
    image: 'https://i.imgur.com/Q9XW0yI.png',
    salePrice: 6200000,
    originalPrice: 7600000,
    discountPercent: 18,
    reviewCount: 89
  },
  {
    id: 15,
    name: 'Tom Ford Noir Extreme',
    brand: 'Tom Ford',
    image: 'https://i.imgur.com/gQ1pKpF.png',
    salePrice: 4900000,
    originalPrice: 5900000,
    discountPercent: 17,
    reviewCount: 74
  },
  {
    id: 16,
    name: 'Baccarat Rouge 540 EDP',
    brand: 'Maison Francis Kurkdjian',
    image: 'https://i.imgur.com/Kb2fP0A.png',
    salePrice: 7200000,
    originalPrice: 8600000,
    discountPercent: 16,
    reviewCount: 96
  },
  {
    id: 17,
    name: 'Le Labo Santal 33 EDP',
    brand: 'Le Labo',
    image: 'https://i.imgur.com/xUmkkWB.png',
    salePrice: 6800000,
    originalPrice: 7900000,
    discountPercent: 14,
    reviewCount: 68
  },
  {
    id: 18,
    name: 'Byredo Blanche EDP',
    brand: 'Byredo',
    image: 'https://i.imgur.com/hBFGYkO.png',
    salePrice: 6800000,
    originalPrice: 8000000,
    discountPercent: 15,
    reviewCount: 68
  }
];
</script>

<style scoped>
.home-view {
  color: #0a192f;
}

.section-title {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 30px;
  color: #0a192f;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.view-all-link {
  color: #0a192f;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: color 0.2s ease;
}

.view-all-link:hover {
  color: #bd9a5f;
}

@media (max-width: 767.98px) {
  .section-title {
    font-size: 22px;
  }
}
</style>