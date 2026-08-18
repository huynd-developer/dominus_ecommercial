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

          <!-- ĐÃ SỬA: Thêm query param flashSale=true để dẫn đến danh sách sản phẩm Flash Sale -->
          <RouterLink :to="{ path: '/products', query: { flashSale: 'true' } }" class="view-all-link">
            Xem tất cả <i class="bi bi-chevron-right ms-1"></i>
          </RouterLink>
        </div>

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

          <div
            v-for="product in specialProducts"
            :key="product.id"
            class="col-12 col-lg-2 special-product-col"
          >
            <div class="special-product-wrap">
              <ProductCard :product="product" />
            </div>
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
  sellableQuantity?: number | null;

  imageUrl?: string | null;
  image?: string | null;
  thumbnailUrl?: string | null;
  mainImage?: string | null;
  productImages?: any[];
  images?: any[];
}

interface ProductCardVariant {
  id?: number;
  productVariantId?: number;
  variantId?: number;
  price?: number;
  originalPrice?: number;
  salePrice?: number;
  promotionPrice?: number;
  flashSalePrice?: number;
  stockQuantity?: number;
  sellableQuantity?: number;
  status?: number;
  capacity?: string | null;
  bottleType?: string | null;
  imageUrl?: string;
  image?: string;
  images?: string[];
  productImages?: any[];
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
  image?: string;
  mainImage?: string;
  images?: string[];
  productImages?: any[];

  salePrice: number;
  originalPrice: number;
  discountPercent: number;

  rating: number;
  reviewCount: number;

  stockQuantity?: number;
  sellableQuantity?: number;
  isFlashSale?: boolean;
  endDate?: string;

  variants?: ProductCardVariant[];
}

const flashSaleProducts = ref<ProductCardItem[]>([]);
const normalHomeProducts = ref<ProductCardItem[]>([]);
const newestProducts = ref<ProductCardItem[]>([]);
const featuredProducts = ref<ProductCardItem[]>([]);
const specialProducts = ref<ProductCardItem[]>([]);

const flashSaleLoading = ref(false);
const productLoading = ref(false);

const BACKEND_URL = "http://localhost:8080";

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

const normalizeImageUrl = (url: unknown) => {
  const rawUrl = String(url || "").trim();

  if (!rawUrl) {
    return "";
  }

  if (
    rawUrl.startsWith("http://") ||
    rawUrl.startsWith("https://") ||
    rawUrl.startsWith("data:image") ||
    rawUrl.startsWith("blob:")
  ) {
    return rawUrl;
  }

  if (rawUrl.startsWith("/")) {
    return `${BACKEND_URL}${rawUrl}`;
  }

  return `${BACKEND_URL}/${rawUrl}`;
};

const getImageUrlFromObject = (value: any) => {
  if (!value) {
    return "";
  }

  if (typeof value === "string") {
    return normalizeImageUrl(value);
  }

  return normalizeImageUrl(
    value?.imageUrl ??
      value?.ImageUrl ??
      value?.url ??
      value?.Url ??
      value?.mediaUrl ??
      value?.MediaUrl ??
      value?.path ??
      value?.Path ??
      value?.fileUrl ??
      value?.FileUrl ??
      value?.thumbnailUrl ??
      value?.ThumbnailUrl ??
      value?.mainImageUrl ??
      value?.MainImageUrl ??
      value?.mainImage ??
      value?.MainImage ??
      ""
  );
};

const appendImage = (images: string[], value: any) => {
  const imageUrl = getImageUrlFromObject(value);

  if (imageUrl && !images.includes(imageUrl)) {
    images.push(imageUrl);
  }
};

const appendImageList = (images: string[], value: any) => {
  if (!value) {
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((item) => appendImage(images, item));
    return;
  }

  appendImage(images, value);
};

const getVariantImageList = (variant: any) => {
  const images: string[] = [];

  appendImage(images, variant?.mainImage);
  appendImage(images, variant?.mainImageUrl);
  appendImage(images, variant?.MainImageUrl);
  appendImage(images, variant?.thumbnailUrl);
  appendImage(images, variant?.ThumbnailUrl);
  appendImage(images, variant?.imageUrl);
  appendImage(images, variant?.ImageUrl);
  appendImage(images, variant?.image);
  appendImage(images, variant?.Image);

  appendImageList(images, variant?.images);
  appendImageList(images, variant?.Images);
  appendImageList(images, variant?.imageList);
  appendImageList(images, variant?.ImageList);
  appendImageList(images, variant?.productImages);
  appendImageList(images, variant?.ProductImages);
  appendImageList(images, variant?.productImageList);
  appendImageList(images, variant?.ProductImageList);

  return images;
};

const resolveProductImages = (raw: any) => {
  const images: string[] = [];

  const addUnique = (url: unknown) => {
    const imageUrl = getImageUrlFromObject(url);
    if (imageUrl && !images.includes(imageUrl)) {
      images.push(imageUrl);
    }
  };

  const imageArrays = [
    raw?.images,
    raw?.Images,
    raw?.productImages,
    raw?.ProductImages,
    raw?.galleryImages,
    raw?.imageList,
  ];

  // 1. Ưu tiên tuyệt đối tìm ảnh có đánh dấu isPrimary đưa lên đầu tiên
  for (const arr of imageArrays) {
    if (Array.isArray(arr)) {
      const primaryObj = arr.find((img: any) => 
        Boolean(img?.isPrimary || img?.is_primary || img?.primary)
      );
      if (primaryObj) {
        addUnique(primaryObj?.imageUrl || primaryObj?.url || primaryObj);
      }
    }
  }

  // 2. Tiếp theo đến ảnh chính đơn lẻ ngoài cùng
  addUnique(raw?.mainImage);
  addUnique(raw?.mainImageUrl);
  addUnique(raw?.imageUrl);
  addUnique(raw?.image);
  addUnique(raw?.thumbnailUrl);

  // 3. Đưa toàn bộ các ảnh còn lại vào mảng
  imageArrays.forEach((arr) => {
    appendImageList(images, arr);
  });

  // Quét vét biến thể nếu có
  const variants = raw?.variants || raw?.productVariants || [];
  if (Array.isArray(variants)) {
    variants.forEach((variant: any) => {
      getVariantImageList(variant).forEach((imageUrl) => addUnique(imageUrl));
    });
  }

  return images;
};

const resolveProductImage = (raw: any) => {
  return resolveProductImages(raw)[0] || "";
};

const resolveMinVariantPrice = (rawVariants: any[]): number => {
  const variantPrices = rawVariants
    .map((v: any) => toNumber(v?.salePrice ?? v?.price, 0))
    .filter((n: number) => n > 0);

  return variantPrices.length > 0 ? Math.min(...variantPrices) : 0;
};

const mapNormalProduct = (p: any): ProductCardItem => {
  const rawVariants = p?.variants || p?.productVariants || p?.productVariantList || [];
  const variantList = Array.isArray(rawVariants) ? rawVariants : [];
  const firstVariant = variantList.length > 0 ? variantList[0] : null;
  const representativeVariant =
    variantList.find(
      (variant: any) =>
        Number(variant?.status ?? 1) === 1 &&
        toNumber(variant?.sellableQuantity, 0) > 0
    ) ||
    variantList.find((variant: any) => Number(variant?.status ?? 1) === 1) ||
    firstVariant;

  const minVariantPrice = resolveMinVariantPrice(variantList);
  const discountPercent = toNumber(p.discountPercent ?? p.discount, 0);

  let salePrice = toNumber(p.salePrice ?? p.minPrice, 0);
  let originalPrice = toNumber(p.originalPrice ?? p.price ?? p.maxPrice, 0);

  if (salePrice === 0) salePrice = minVariantPrice || originalPrice;
  if (originalPrice === 0) originalPrice = salePrice;

  // ĐÃ SỬA: TRIỆT TIÊU LỖI GIẢM GIÁ ẢO TỪ BACKEND (VD: 10ml 350k vs 30ml 2.1M)
  if (discountPercent <= 0) {
    const actualBase = salePrice > 0 ? salePrice : originalPrice;
    salePrice = actualBase;
    originalPrice = actualBase;
  } else if (salePrice >= originalPrice && discountPercent > 0) {
    salePrice = originalPrice - (originalPrice * discountPercent) / 100;
  }

  const productId = toNumber(p.productId ?? p.id, 0);
  const productVariantId = toNumber(
    p.productVariantId ??
      p.variantId ??
      representativeVariant?.productVariantId ??
      representativeVariant?.variantId ??
      representativeVariant?.id,
    0
  );

  const sellableQuantity = variantList.reduce(
    (sum: number, variant: any) =>
      sum + Math.max(0, toNumber(variant?.sellableQuantity, 0)),
    0
  );

  const representativeSellableQuantity = Math.max(
    0,
    toNumber(representativeVariant?.sellableQuantity, 0)
  );

  const productImages = resolveProductImages(p);
  const productImage = productImages[0] || "";

  return {
    id: productId,
    productId,
    productVariantId: productVariantId || undefined,
    variantId: productVariantId || undefined,
    name: p.name || p.productName || "Sản phẩm",
    brand: formatBrand(p),
    color: p.color || "#0a192f",
    imageUrl: productImage,
    image: productImage,
    mainImage: productImage,
    images: productImages,
    productImages: productImages.map((imageUrl, index) => ({ id: index + 1, imageUrl, url: imageUrl })),
    salePrice,
    originalPrice,
    discountPercent,
    rating: toNumber(p.rating, 5),
    reviewCount: toNumber(p.reviewCount ?? p.reviews, 0),
    sellableQuantity,

    // Compatibility: không còn là nguồn tồn thật.
    stockQuantity: sellableQuantity,
    variants: productVariantId ? [{
      id: productVariantId,
      productVariantId,
      variantId: productVariantId,
      price: originalPrice,
      originalPrice,
      salePrice,
      promotionPrice: discountPercent > 0 ? salePrice : undefined,
      flashSalePrice: undefined,
      sellableQuantity: representativeSellableQuantity,
      stockQuantity: representativeSellableQuantity,
      status: toNumber(representativeVariant?.status, 1),
      imageUrl: productImage,
      image: productImage,
      images: productImages,
      productImages: productImages.map((imageUrl, index) => ({ id: index + 1, imageUrl, url: imageUrl })),
    }] : undefined,
  };
};

const mapFlashSaleProduct = (item: FlashSaleProductResponse): ProductCardItem => {
  const productVariantId = toNumber(item.productVariantId, 0);
  const productId = toNumber(item.productId ?? item.productVariantId, 0);
  const sellableQuantity = Math.max(0, toNumber(item.sellableQuantity, 0));
  const salePrice = toNumber(item.salePrice, 0);
  const originalPrice = toNumber(item.originalPrice, 0);

  const productImages = resolveProductImages(item);
  const productImage = productImages[0] || resolveProductImage(item);

  return {
    id: productId,
    productId,
    productVariantId,
    variantId: productVariantId,

    name: item.productName || "Sản phẩm Flash Sale",
    brand: item.promotionName || "Flash Sale",
    color: "#0a192f",

    imageUrl: productImage,
    image: productImage,
    mainImage: productImage,
    images: productImages,
    productImages: productImages.map((imageUrl, index) => ({
      id: index + 1,
      imageUrl,
      url: imageUrl,
    })),

    salePrice,
    originalPrice,
    discountPercent: toNumber(item.discountPercent, 0),

    rating: 5,
    reviewCount: 0,

    sellableQuantity,
    stockQuantity: sellableQuantity,
    isFlashSale: true,
    endDate: item.endDate,

    variants: [
      {
        id: productVariantId,
        productVariantId,
        variantId: productVariantId,
        price: originalPrice,
        originalPrice,
        salePrice,
        promotionPrice: salePrice,
        flashSalePrice: salePrice,
        sellableQuantity,
        stockQuantity: sellableQuantity,
        status: 1,
        capacity: item.capacity,
        bottleType: item.bottleType,
        imageUrl: productImage,
        image: productImage,
        images: productImages,
        productImages: productImages.map((imageUrl, index) => ({
          id: index + 1,
          imageUrl,
          url: imageUrl,
        })),
      },
    ],
  };
};

const getProductMapId = (product: ProductCardItem) =>
  toNumber(product.productId ?? product.id, 0);

const cloneProductImages = (product: ProductCardItem) => {
  const images = Array.isArray(product.images) ? product.images : [];

  if (images.length > 0) {
    return images;
  }

  return [product.imageUrl, product.image, product.mainImage].filter(
    (value): value is string => Boolean(value)
  );
};

const mergeFlashSaleIntoProduct = (
  product: ProductCardItem,
  flashSaleMap: Map<number, ProductCardItem>
): ProductCardItem => {
  const productId = getProductMapId(product);
  const flashSale = flashSaleMap.get(productId);

  // NẾU SẢN PHẨM KHÔNG CÓ TRONG FLASH SALE -> GIỮ NGUYÊN BẢN, TUYỆT ĐỐI KHÔNG GÁN ĐÈ GIÁ SALE
  if (!flashSale) {
    return {
      ...product,
      isFlashSale: false,
      salePrice: product.originalPrice > 0 ? product.salePrice : product.salePrice,
      discountPercent: 0, // Đảm bảo phần trăm giảm giá về 0
      variants: product.variants?.map((variant) => ({
        ...variant,
        salePrice: variant.originalPrice || variant.price,
        promotionPrice: undefined,
        flashSalePrice: undefined,
      })),
    };
  }

  // Ngược lại nếu đúng có trong flash sale thì mới tiến hành gộp giá sale...
  const salePrice = toNumber(flashSale.salePrice, product.salePrice);
  const originalPrice = toNumber(
    flashSale.originalPrice,
    product.originalPrice || product.salePrice
  );
  const discountPercent = toNumber(
    flashSale.discountPercent,
    originalPrice > salePrice && originalPrice > 0
      ? Math.round(((originalPrice - salePrice) / originalPrice) * 100)
      : 0
  );

  const productImages = cloneProductImages(product);
  const productImage =
    product.imageUrl ||
    product.image ||
    product.mainImage ||
    productImages[0] ||
    flashSale.imageUrl ||
    "";

  const flashSaleVariants =
    Array.isArray(flashSale.variants) && flashSale.variants.length > 0
      ? flashSale.variants
      : [];

  const productVariants =
    Array.isArray(product.variants) && product.variants.length > 0
      ? product.variants
      : [];

  /*
   * Flash Sale chỉ ghi đè giá/metadata khuyến mãi.
   * Tồn sellable của variant luôn được giữ từ /v1/products.
   */
  const mergedVariants =
    productVariants.length > 0
      ? productVariants.map((variant) => {
          const variantId = toNumber(
            variant.productVariantId ?? variant.variantId ?? variant.id,
            0
          );

          const flashVariant = flashSaleVariants.find(
            (item) =>
              toNumber(
                item.productVariantId ?? item.variantId ?? item.id,
                0
              ) === variantId
          );

          if (!flashVariant) {
            return variant;
          }

          const variantOriginalPrice = toNumber(
            flashVariant.originalPrice ??
              flashVariant.price ??
              variant.originalPrice ??
              variant.price,
            originalPrice
          );

          const variantSalePrice = toNumber(
            flashVariant.salePrice ??
              flashVariant.promotionPrice ??
              flashVariant.flashSalePrice,
            salePrice
          );

          return {
            ...variant,
            price: variantOriginalPrice,
            originalPrice: variantOriginalPrice,
            salePrice: variantSalePrice,
            promotionPrice: variantSalePrice,
            flashSalePrice: variantSalePrice,
            imageUrl: variant.imageUrl || productImage,
            image: variant.image || productImage,
            images:
              Array.isArray(variant.images) && variant.images.length > 0
                ? variant.images
                : productImages,
          };
        })
      : flashSaleVariants.map((variant) => ({
          ...variant,
          sellableQuantity: toNumber(variant.sellableQuantity, 0),
          stockQuantity: toNumber(variant.sellableQuantity, 0),
        }));

  return {
    ...product,
    salePrice,
    originalPrice,
    discountPercent,
    isFlashSale: true,
    endDate: flashSale.endDate,
    productVariantId: flashSale.productVariantId || product.productVariantId,
    variantId: flashSale.variantId || product.variantId,
    sellableQuantity: product.sellableQuantity,
    stockQuantity: product.sellableQuantity ?? 0,
    imageUrl: productImage,
    image: productImage,
    mainImage: productImage,
    images: productImages,
    productImages:
      product.productImages && product.productImages.length > 0
        ? product.productImages
        : productImages.map((imageUrl, index) => ({
            id: index + 1,
            imageUrl,
            url: imageUrl,
          })),
    variants: mergedVariants,
  };
};

const refreshHomeProductSections = () => {
  const flashSaleMap = new Map<number, ProductCardItem>();

  flashSaleProducts.value.forEach((product) => {
    const productId = getProductMapId(product);

    if (productId > 0) {
      flashSaleMap.set(productId, product);
    }
  });

  const formatted = normalHomeProducts.value.map((product) =>
    mergeFlashSaleIntoProduct(product, flashSaleMap)
  );

  newestProducts.value = formatted.slice(0, 4);

  const featured = formatted.slice(4, 8);
  featuredProducts.value =
    featured.length > 0 ? featured : formatted.slice(0, 4);

  const special = formatted.slice(8, 10);
  specialProducts.value = special.length > 0 ? special : formatted.slice(0, 2);
};

const fetchFlashSaleProducts = async () => {
  try {
    flashSaleLoading.value = true;

    const res = await api.get<PageResponse<FlashSaleProductResponse>>(
      "/promotions/flash-sale",
      {
        params: {
          page: 0,
          size: 20,
        },
      }
    );

    const rows = resolvePageContent<FlashSaleProductResponse>(res.data);

    const productMap = new Map<number, FlashSaleProductResponse[]>();
    rows.forEach((item) => {
      const pId = toNumber(item.productId ?? item.productVariantId, 0);
      if (!productMap.has(pId)) {
        productMap.set(pId, []);
      }
      productMap.get(pId)!.push(item);
    });

    const groupedProducts: ProductCardItem[] = [];

    productMap.forEach((variants, productId) => {
      if (!variants || variants.length === 0) return;
      const firstItem = variants[0];
      
      const productImages = resolveProductImages(firstItem);
      const productImage = productImages[0] || resolveProductImage(firstItem);

      const salePrices = variants.map(v => toNumber(v.salePrice, 0)).filter(p => p > 0);
      const minSalePrice = salePrices.length > 0 ? Math.min(...salePrices) : toNumber(firstItem?.salePrice, 0);

      const originalPrices = variants.map(v => toNumber(v.originalPrice, 0)).filter(p => p > 0);
      const minOriginalPrice = originalPrices.length > 0 ? Math.min(...originalPrices) : toNumber(firstItem?.originalPrice, 0);

      const maxDiscount = Math.max(...variants.map(v => toNumber(v.discountPercent, 0)));

      const mappedVariants = variants.map(v => {
        const vId = toNumber(v.productVariantId, 0);
        const vSalePrice = toNumber(v.salePrice, 0);
        const vOriginalPrice = toNumber(v.originalPrice, 0);
        const vStock = Math.max(0, toNumber(v.sellableQuantity, 0));

        return {
          id: vId,
          productVariantId: vId,
          variantId: vId,
          price: vOriginalPrice,
          originalPrice: vOriginalPrice,
          salePrice: vSalePrice,
          sellableQuantity: vStock,
          stockQuantity: vStock,
          status: 1,
          capacity: v.capacity,
          bottleType: v.bottleType,
          imageUrl: productImage,
          image: productImage,
          images: productImages,
        };
      });

      groupedProducts.push({
        id: productId,
        productId: productId,
        productVariantId: mappedVariants[0]?.productVariantId,
        variantId: mappedVariants[0]?.productVariantId,

        name: firstItem?.productName || "Sản phẩm Flash Sale",
        brand: firstItem?.promotionName || "Flash Sale",
        color: "#0a192f",

        imageUrl: productImage,
        image: productImage,
        mainImage: productImage,
        images: productImages,
        productImages: productImages.map((imageUrl, index) => ({
          id: index + 1,
          imageUrl,
          url: imageUrl,
        })),

        salePrice: minSalePrice,
        originalPrice: minOriginalPrice,
        discountPercent: maxDiscount,

        rating: 5,
        reviewCount: 0,

        sellableQuantity: mappedVariants.reduce(
          (sum, v) => sum + (v.sellableQuantity || 0),
          0
        ),
        stockQuantity: mappedVariants.reduce(
          (sum, v) => sum + (v.sellableQuantity || 0),
          0
        ),
        isFlashSale: true,
        endDate: firstItem?.endDate,

        variants: mappedVariants,
      });
    });

    flashSaleProducts.value = groupedProducts;
    refreshHomeProductSections();
  } catch (error) {
    console.error("Lỗi tải Flash Sale:", error);
    flashSaleProducts.value = [];
    refreshHomeProductSections();
  } finally {
    flashSaleLoading.value = false;
  }
};

const fetchNormalProducts = async () => {
  try {
    productLoading.value = true;

    const res = await api.get("/v1/products", {
      params: {
        page: 0,
        size: 12,
      },
    });

    const rows = resolvePageContent<any>(res.data);
    normalHomeProducts.value = rows.map(mapNormalProduct);
    refreshHomeProductSections();
  } catch (error) {
    console.error("Lỗi tải sản phẩm trang chủ:", error);

    normalHomeProducts.value = [];
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

// BẮT SỰ KIỆN CLICK CHUỘT QUAY LẠI CỬA SỔ ĐỂ TỰ ĐỘNG CẬP NHẬT FLASH SALE MỚI NHẤT
const handleFocus = async () => {
  await Promise.all([fetchFlashSaleProducts(), fetchNormalProducts()]);
};

onMounted(async () => {
  await Promise.all([fetchFlashSaleProducts(), fetchNormalProducts()]);
  startFlashSaleRealtimeRefresh();
  window.addEventListener("focus", handleFocus);
});

onBeforeUnmount(() => {
  stopFlashSaleRealtimeRefresh();
  window.removeEventListener("focus", handleFocus);
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