<template>
  <div class="home-view">
    <div class="container-fluid px-3 px-lg-5 py-4">
      <HomeBanner />

      <section class="product-section mt-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-4">
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h2 class="section-title mb-0">FLASH SALE</h2>

            <CountdownTimer
              v-if="flashSaleEndDate"
              :target-date="flashSaleEndDate"
              @expired="handleFlashSaleExpired"
            />
          </div>

          <RouterLink to="/products" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

<<<<<<< HEAD
        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div v-for="product in flashSaleProducts" :key="product.id" class="col"
            @click="$router.push({ name: 'ProductDetail', params: { id: product.id } })" style="cursor: pointer;">
=======
        <div v-if="flashSaleLoading" class="text-center py-5">
          <span class="spinner-border spinner-border-sm me-2"></span>
          Đang tải Flash Sale...
        </div>

        <div v-else-if="flashSaleProducts.length === 0" class="empty-box">
          Hiện chưa có sản phẩm Flash Sale đang diễn ra.
        </div>

        <div v-else class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div
            v-for="product in flashSaleProducts"
            :key="product.productVariantId || product.id"
            class="col"
          >
>>>>>>> b7504e3ce6a0e42a0d6a324ec60919d297777aae
            <ProductCard :product="product" />
          </div>
        </div>
      </section>

      <section class="product-section mt-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-4">
          <h2 class="section-title mb-0">NƯỚC HOA MỚI NHẤT</h2>

          <RouterLink to="/products" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

<<<<<<< HEAD
        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <!-- Cập nhật đường dẫn chuẩn: /product/:id -->
          <div v-for="product in flashSaleProducts" :key="product.id" class="col"
            @click="$router.push({ name: 'ProductDetail', params: { id: product.id } })" style="cursor: pointer;">
=======
        <div v-if="productLoading" class="text-center py-5">
          <span class="spinner-border spinner-border-sm me-2"></span>
          Đang tải sản phẩm...
        </div>

        <div v-else-if="newestProducts.length === 0" class="empty-box">
          Chưa có sản phẩm mới nhất.
        </div>

        <div v-else class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div
            v-for="product in newestProducts"
            :key="product.id"
            class="col"
          >
>>>>>>> b7504e3ce6a0e42a0d6a324ec60919d297777aae
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

              <RouterLink to="/products" class="btn collection-btn">
                KHÁM PHÁ NGAY
                <i class="bi bi-arrow-right-short ms-1"></i>
              </RouterLink>
            </div>
          </div>

<<<<<<< HEAD
          <!-- Cập nhật đường dẫn chuẩn: /product/:id -->
          <div v-for="product in flashSaleProducts" :key="product.id" class="col"
            @click="$router.push({ name: 'ProductDetail', params: { id: product.id } })" style="cursor: pointer;">
            <ProductCard :product="product" />
=======
          <div
            v-for="product in specialProducts"
            :key="product.id"
            class="col-12 col-lg-2 special-product-col"
          >
            <div class="special-product-wrap">
              <ProductCard :product="product" />
            </div>
>>>>>>> b7504e3ce6a0e42a0d6a324ec60919d297777aae
          </div>
        </div>
      </section>

      <section class="product-section mt-5 mb-5">
        <div class="section-header d-flex align-items-center justify-content-between mb-4">
          <h2 class="section-title mb-0">SẢN PHẨM NỔI BẬT</h2>

          <RouterLink to="/products" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

<<<<<<< HEAD
        <div class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <!-- Cập nhật đường dẫn chuẩn: /product/:id -->
          <div v-for="product in flashSaleProducts" :key="product.id" class="col"
            @click="$router.push({ name: 'ProductDetail', params: { id: product.id } })" style="cursor: pointer;">
=======
        <div v-if="productLoading" class="text-center py-5">
          <span class="spinner-border spinner-border-sm me-2"></span>
          Đang tải sản phẩm...
        </div>

        <div v-else-if="featuredProducts.length === 0" class="empty-box">
          Chưa có sản phẩm nổi bật.
        </div>

        <div v-else class="row row-cols-2 row-cols-md-4 row-cols-lg-4 g-4">
          <div
            v-for="product in featuredProducts"
            :key="product.id"
            class="col"
          >
>>>>>>> b7504e3ce6a0e42a0d6a324ec60919d297777aae
            <ProductCard :product="product" />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import api from "@/common/api";
import HomeBanner from "../components/HomeBanner.vue";
import CountdownTimer from "../components/CountdownTimer.vue";
import ProductCard from "../components/ProductCard.vue";
import collectionImage from "@/assets/images/collection-aura.png";

interface PageResponse<T> {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

interface FlashSaleProductResponse {
  promotionId: number;
  promotionName: string;
  endDate: string;
  productVariantId: number;
  productId: number | null;
  productName: string | null;
  sku: string | null;
  capacity: string | null;
  bottleType: string | null;
  originalPrice: number;
  discountPercent: number;
  salePrice: number;
  stockQuantity: number | null;
}

interface ProductCardVariant {
  id?: number;
  productVariantId?: number;
  variantId?: number;
  price?: number;
  stockQuantity?: number;
  status?: number;
}

interface ProductCardItem {
  id: number;
  productId?: number;
  productVariantId?: number;
  variantId?: number;

  name: string;
  brand: string;
  color?: string;
  imageUrl?: string;

  salePrice: number;
  originalPrice: number;
  discountPercent: number;

  rating: number;
  reviewCount: number;

  stockQuantity?: number;
  isFlashSale?: boolean;
  endDate?: string;

  variants?: ProductCardVariant[];
}

const flashSaleProducts = ref<ProductCardItem[]>([]);
const newestProducts = ref<ProductCardItem[]>([]);
const featuredProducts = ref<ProductCardItem[]>([]);
const specialProducts = ref<ProductCardItem[]>([]);

const flashSaleLoading = ref(false);
const productLoading = ref(false);

let flashSaleRefreshTimer: ReturnType<typeof window.setInterval> | null = null;

const flashSaleEndDate = computed(() => {
  const validEndDates = flashSaleProducts.value
    .map((item) => item.endDate)
    .filter((value): value is string => Boolean(value))
    .sort((a, b) => new Date(a).getTime() - new Date(b).getTime());

  return validEndDates[0] || null;
});

const toNumber = (value: unknown, fallback = 0) => {
  const numberValue = Number(value);

  if (Number.isNaN(numberValue)) {
    return fallback;
  }

  return numberValue;
};

const resolvePageContent = <T,>(data: any): T[] => {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  if (Array.isArray(data?.data?.content)) {
    return data.data.content;
  }

  if (Array.isArray(data?.data)) {
    return data.data;
  }

  return [];
};

const formatBrand = (raw: any) => {
  return raw?.brand?.name || raw?.brandName || raw?.brand || "Premium";
};

const resolveProductImage = (raw: any) => {
  return (
    raw?.imageUrl ||
    raw?.image ||
    raw?.thumbnailUrl ||
    raw?.mainImage ||
    raw?.images?.[0]?.url ||
    raw?.images?.[0]?.imageUrl ||
    ""
  );
};

const mapNormalProduct = (p: any): ProductCardItem => {
  const firstVariant =
    Array.isArray(p.variants) && p.variants.length > 0
      ? p.variants[0]
      : null;

  const basePrice = toNumber(
    p.price ??
      firstVariant?.price ??
      p.minPrice ??
      p.originalPrice,
    0
  );

  const discountPercent = toNumber(p.discountPercent ?? p.discount, 0);

  const salePrice =
    discountPercent > 0
      ? basePrice - (basePrice * discountPercent) / 100
      : toNumber(p.salePrice, basePrice);

  const productId = toNumber(p.productId ?? p.id, 0);

  const productVariantId = toNumber(
    p.productVariantId ??
      p.variantId ??
      firstVariant?.productVariantId ??
      firstVariant?.variantId ??
      firstVariant?.id,
    0
  );

  const stockQuantity = toNumber(
    p.stockQuantity ??
      p.stock ??
      firstVariant?.stockQuantity ??
      firstVariant?.stock,
    1
  );

  return {
    id: productId,
    productId,
    productVariantId: productVariantId || undefined,
    variantId: productVariantId || undefined,

    name: p.name || p.productName || "Sản phẩm",
    brand: formatBrand(p),
    color: p.color || "#0a192f",
    imageUrl: resolveProductImage(p),

    salePrice,
    originalPrice: basePrice,
    discountPercent,

    rating: toNumber(p.rating, 5),
    reviewCount: toNumber(p.reviewCount ?? p.reviews, 0),

    stockQuantity,

    variants: productVariantId
      ? [
          {
            id: productVariantId,
            productVariantId,
            variantId: productVariantId,
            price: salePrice,
            stockQuantity,
            status: toNumber(firstVariant?.status, 1),
          },
        ]
      : undefined,
  };
};

const mapFlashSaleProduct = (item: FlashSaleProductResponse): ProductCardItem => {
  const productVariantId = toNumber(item.productVariantId, 0);
  const productId = toNumber(item.productId ?? item.productVariantId, 0);
  const stockQuantity = toNumber(item.stockQuantity, 0);
  const salePrice = toNumber(item.salePrice, 0);
  const originalPrice = toNumber(item.originalPrice, 0);

  return {
    id: productId,
    productId,
    productVariantId,
    variantId: productVariantId,

    name: item.productName || "Sản phẩm Flash Sale",
    brand: item.promotionName || "Flash Sale",
    color: "#0a192f",

    salePrice,
    originalPrice,
    discountPercent: toNumber(item.discountPercent, 0),

    rating: 5,
    reviewCount: 0,

    stockQuantity,
    isFlashSale: true,
    endDate: item.endDate,

    variants: [
      {
        id: productVariantId,
        productVariantId,
        variantId: productVariantId,
        price: salePrice,
        stockQuantity,
        status: 1,
      },
    ],
  };
};

const fetchFlashSaleProducts = async () => {
  try {
    flashSaleLoading.value = true;

    const res = await api.get<PageResponse<FlashSaleProductResponse>>(
      "/promotions/flash-sale",
      {
        params: {
          page: 0,
          size: 4,
        },
      }
    );

    const rows = resolvePageContent<FlashSaleProductResponse>(res.data);
    flashSaleProducts.value = rows.map(mapFlashSaleProduct);
  } catch (error) {
    console.error("Lỗi tải Flash Sale:", error);
    flashSaleProducts.value = [];
  } finally {
    flashSaleLoading.value = false;
  }
};

const fetchNormalProducts = async () => {
  try {
    productLoading.value = true;

    const res = await api.get("/products", {
      params: {
        page: 0,
        size: 12,
      },
    });

    const rows = resolvePageContent<any>(res.data);
    const formatted = rows.map(mapNormalProduct);

    newestProducts.value = formatted.slice(0, 4);

    const featured = formatted.slice(4, 8);
    featuredProducts.value = featured.length > 0 ? featured : formatted.slice(0, 4);

    const special = formatted.slice(8, 10);
    specialProducts.value = special.length > 0 ? special : formatted.slice(0, 2);
  } catch (error) {
    console.error("Lỗi tải sản phẩm trang chủ:", error);

    newestProducts.value = [];
    featuredProducts.value = [];
    specialProducts.value = [];
  } finally {
    productLoading.value = false;
  }
};

const handleFlashSaleExpired = async () => {
  await fetchFlashSaleProducts();
};

const startFlashSaleRealtimeRefresh = () => {
  stopFlashSaleRealtimeRefresh();

  flashSaleRefreshTimer = window.setInterval(() => {
    fetchFlashSaleProducts();
  }, 60000);
};

const stopFlashSaleRealtimeRefresh = () => {
  if (flashSaleRefreshTimer) {
    window.clearInterval(flashSaleRefreshTimer);
    flashSaleRefreshTimer = null;
  }
};

onMounted(async () => {
  await Promise.all([fetchFlashSaleProducts(), fetchNormalProducts()]);
  startFlashSaleRealtimeRefresh();
});

onBeforeUnmount(() => {
  stopFlashSaleRealtimeRefresh();
});
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

.empty-box {
  border: 1px dashed rgba(189, 154, 95, 0.34);
  background: #fffaf2;
  color: #777777;
  border-radius: 14px;
  padding: 28px 20px;
  text-align: center;
  font-weight: 600;
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