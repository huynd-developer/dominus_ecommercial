<template>
  <div class="home-view">
    <div class="container-fluid px-3 px-lg-5 py-4">
      <HomeBanner />

      <section class="product-section mt-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-4">
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h2 class="section-title mb-0">FLASH SALE</h2>
            <CountdownTimer />
          </div>

          <RouterLink to="/flash-sale" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div v-for="product in flashSaleProducts" :key="product.id" class="col">
            <ProductCard :product="product" />
          </div>
        </div>
      </section>

      <section class="product-section mt-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-4">
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
              <img :src="collectionImage" alt="Bộ sưu tập đặc biệt" class="collection-main-image" />
            </div>
          </div>

          <div class="col-12 col-lg-3">
            <div class="collection-copy h-100">
              <p class="collection-kicker mb-2">BỘ SƯU TẬP</p>
              <h2 class="collection-title mb-3">ĐẶC BIỆT</h2>
              <p class="collection-desc mb-4">
                Tuyển chọn những tuyệt tác hương thơm hiếm có, dành riêng cho những dấu ấn khác biệt.
              </p>

              <RouterLink to="/collections/special" class="btn collection-btn">
                KHÁM PHÁ NGAY
                <i class="bi bi-arrow-right-short ms-1"></i>
              </RouterLink>
            </div>
          </div>

          <div v-for="product in specialProducts" :key="product.id" class="col-12 col-md-6 col-lg-2 special-product-col">
            <div class="special-product-wrap h-100">
              <ProductCard :product="product" />
            </div>
          </div>
        </div>
      </section>

      <section class="product-section mt-5 mb-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-4">
          <h2 class="section-title mb-0">SẢN PHẨM NỔI BẬT</h2>

          <RouterLink to="/products/featured" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div v-for="product in featuredProducts" :key="product.id" class="col">
            <ProductCard :product="product" />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import axios from "axios";
import HomeBanner from "../components/HomeBanner.vue";
import CountdownTimer from "../components/CountdownTimer.vue";
import ProductCard from "../components/ProductCard.vue";
import collectionImage from "@/assets/images/collection-aura.png";

// Khai báo rõ ràng kiểu dữ liệu để tránh lỗi 'never' của TypeScript compiler
const flashSaleProducts = ref<any[]>([]);
const newestProducts = ref<any[]>([]);
const featuredProducts = ref<any[]>([]);
const specialProducts = ref<any[]>([]);

const fetchAllProducts = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/products');
    const all = res.data.data?.content || res.data.data || [];
    
    const formatted = all.map((p: any) => {
      const basePrice = p.price || (p.variants && p.variants.length > 0 ? p.variants[0].price : 0) || 0;
      const discount = p.discountPercent || 0;
      const salePrice = discount > 0 ? basePrice * (1 - discount / 100) : basePrice;

      return {
        id: p.id,
        name: p.name,
        brand: p.brand?.name || p.brand || 'Premium',
        imageUrl: p.imageUrl,
        color: p.color || '#0a192f', 
        salePrice: salePrice,
        originalPrice: basePrice,
        discountPercent: discount,
        rating: p.rating || 5, 
        reviewCount: p.reviewCount || 0
      };
    });

    flashSaleProducts.value = formatted.slice(0, 4);
    newestProducts.value = formatted.slice(0, 4);
    featuredProducts.value = formatted.slice(0, 4);
    specialProducts.value = formatted.slice(0, 2);
  } catch (err) {
    console.error("Lỗi API trang chủ:", err);
  }
};

onMounted(fetchAllProducts);
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

.collection-image-card {
  height: 100%;
  background: #fffaf2;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 0;
  overflow: hidden;
}

.collection-main-image {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 430px;
  object-fit: cover;
  object-position: center center;
  border-radius: 0;
}

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