<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="single-product-main">
      <div
        v-if="isLoading"
        style="padding: 100px; text-align: center; color: #666;"
      >
        Đang tải thông tin sản phẩm...
      </div>

      <div
        v-else-if="!product"
        style="padding: 100px; text-align: center; color: #e53e3e;"
      >
        Không tìm thấy sản phẩm!
      </div>

      <ProductDetail
        v-else
        :product="product"
        @back="goBack"
        @buy-now="handleBuyNow"
      />
    </main>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import ProductDetail from "@/modules/shop/feature/product/components/ProductDetail.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";

const route = useRoute();
const router = useRouter();

const product = ref<any>(null);
const isLoading = ref(true);

const BACKEND_URL = "http://localhost:8080";

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
}

interface FlashSaleIndex {
  byVariantId: Map<number, FlashSaleProductResponse>;
  bySku: Map<string, FlashSaleProductResponse>;
}

const extractArrayData = (data: any): any[] => {
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

const extractObjectData = (data: any) => {
  if (data?.data && !Array.isArray(data.data)) {
    return data.data;
  }

  return data || null;
};

const toNumber = (value: unknown, fallback = 0) => {
  const numberValue = Number(value);

  if (Number.isNaN(numberValue)) {
    return fallback;
  }

  return numberValue;
};

const normalizeSku = (value: unknown) => {
  return String(value || "")
    .trim()
    .toLowerCase();
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

const getImageUrlFromObject = (item: any) => {
  if (!item) {
    return "";
  }

  if (typeof item === "string") {
    return normalizeImageUrl(item);
  }

  return normalizeImageUrl(
    item?.imageUrl ??
      item?.ImageUrl ??
      item?.url ??
      item?.Url ??
      item?.mediaUrl ??
      item?.MediaUrl ??
      item?.path ??
      item?.Path ??
      item?.fileUrl ??
      item?.FileUrl ??
      ""
  );
};

const getPlaceholderImage = () => {
  return (
    "data:image/svg+xml;utf8," +
    encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
        <rect width="100%" height="100%" fill="#f3f4f6"/>
        <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
          fill="#9ca3af" font-family="Arial" font-size="20">
          No Image
        </text>
      </svg>
    `)
  );
};

const extractProductImages = (productData: any, variants: any[]) => {
  const imageUrls: string[] = [];

  const addImage = (url: unknown) => {
    const normalizedUrl = getImageUrlFromObject(url);

    if (normalizedUrl && !imageUrls.includes(normalizedUrl)) {
      imageUrls.push(normalizedUrl);
    }
  };

  addImage(productData?.imageUrl);
  addImage(productData?.ImageUrl);
  addImage(productData?.thumbnailUrl);
  addImage(productData?.ThumbnailUrl);
  addImage(productData?.mainImageUrl);
  addImage(productData?.MainImageUrl);
  addImage(productData?.image);

  const productImageSources =
    productData?.images ||
    productData?.Images ||
    productData?.productImages ||
    productData?.ProductImages ||
    productData?.imageList ||
    productData?.ImageList ||
    productData?.productImageList ||
    productData?.ProductImageList ||
    productData?.mediaFiles ||
    [];

  if (Array.isArray(productImageSources)) {
    productImageSources.forEach((imageItem: any) => {
      addImage(imageItem);
    });
  }

  if (Array.isArray(variants)) {
    variants.forEach((variant: any) => {
      addImage(variant?.imageUrl);
      addImage(variant?.ImageUrl);
      addImage(variant?.thumbnailUrl);
      addImage(variant?.ThumbnailUrl);
      addImage(variant?.image);

      const variantImages =
        variant?.images ||
        variant?.Images ||
        variant?.productImages ||
        variant?.ProductImages ||
        [];

      if (Array.isArray(variantImages)) {
        variantImages.forEach((imageItem: any) => {
          addImage(imageItem);
        });
      }
    });
  }

  return imageUrls;
};

const extractCapacity = (
  variant: any,
  flashSale?: FlashSaleProductResponse | null
) => {
  if (!variant && !flashSale) return "N/A";

  let value = null;

  if (variant?.capacityName != null) {
    value = variant.capacityName;
  } else if (variant?.capacity && typeof variant.capacity === "object") {
    value = variant.capacity.value ?? variant.capacity.name;
  } else if (variant?.capacityValue != null) {
    value = variant.capacityValue;
  } else if (variant?.volume != null) {
    value = variant.volume;
  } else if (variant?.capacity != null) {
    value = variant.capacity;
  } else if (flashSale?.capacity != null) {
    value = flashSale.capacity;
  }

  if (value == null || value === "") {
    return "N/A";
  }

  const numeric = parseFloat(String(value).replace("ml", ""));

  if (!Number.isNaN(numeric) && numeric > 0) {
    return `${numeric}ml`;
  }

  const text = String(value);
  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const normalizeStock = (
  variant: any,
  _flashSale?: FlashSaleProductResponse | null
) => {
  const value = Number(variant?.sellableQuantity ?? 0);

  if (!Number.isFinite(value) || value <= 0) {
    return 0;
  }

  return Math.trunc(value);
};

const getVariantId = (variant: any) => {
  return Number(
    variant?.productVariantId ??
      variant?.variantId ??
      variant?.id ??
      variant?.Id ??
      0
  );
};

const getVariantSku = (variant: any) => {
  return normalizeSku(variant?.sku ?? variant?.SKU);
};

const getPageTotalPages = (data: any) => {
  return Number(
    data?.page?.totalPages ??
      data?.totalPages ??
      data?.data?.page?.totalPages ??
      data?.data?.totalPages ??
      1
  );
};

const fetchFlashSalePage = async (page: number, size: number) => {
  const res = await api.get("/promotions/flash-sale", {
    params: {
      page,
      size,
    },
  });

  const data = res.data?.data || res.data;

  return {
    rows: extractArrayData(data) as FlashSaleProductResponse[],
    totalPages: getPageTotalPages(data),
  };
};

const fetchActiveFlashSaleIndex = async (): Promise<FlashSaleIndex> => {
  const index: FlashSaleIndex = {
    byVariantId: new Map<number, FlashSaleProductResponse>(),
    bySku: new Map<string, FlashSaleProductResponse>(),
  };

  try {
    const size = 24;
    const firstPage = await fetchFlashSalePage(0, size);

    const addRowsToIndex = (rows: FlashSaleProductResponse[]) => {
      rows.forEach((item) => {
        const variantId = Number(item?.productVariantId || 0);
        const sku = normalizeSku(item?.sku);

        if (variantId > 0) {
          index.byVariantId.set(variantId, item);
        }

        if (sku) {
          index.bySku.set(sku, item);
        }
      });
    };

    addRowsToIndex(firstPage.rows);

    if (firstPage.totalPages > 1) {
      const requests = [];

      for (let page = 1; page < firstPage.totalPages; page++) {
        requests.push(fetchFlashSalePage(page, size));
      }

      const pages = await Promise.all(requests);

      pages.forEach((pageResult) => {
        addRowsToIndex(pageResult.rows);
      });
    }
  } catch (error) {
    console.error("Lỗi tải Flash Sale để map vào chi tiết:", error);
  }

  return index;
};

const findFlashSaleForVariant = (
  variant: any,
  flashSaleIndex: FlashSaleIndex
) => {
  const variantId = getVariantId(variant);

  if (variantId > 0 && flashSaleIndex.byVariantId.has(variantId)) {
    return flashSaleIndex.byVariantId.get(variantId) || null;
  }

  const sku = getVariantSku(variant);

  if (sku && flashSaleIndex.bySku.has(sku)) {
    return flashSaleIndex.bySku.get(sku) || null;
  }

  return null;
};

const mapVariant = (
  variant: any,
  flashSaleIndex: FlashSaleIndex
) => {
  const flashSale = findFlashSaleForVariant(variant, flashSaleIndex);
  const variantId = getVariantId(variant);
  const stock = normalizeStock(variant, flashSale);

  const normalPrice = toNumber(variant?.price ?? variant?.Price, 0);

  const originalPrice = flashSale
    ? toNumber(flashSale.originalPrice, normalPrice)
    : normalPrice;

  const salePrice = flashSale
    ? toNumber(flashSale.salePrice, originalPrice)
    : originalPrice;

  return {
    ...variant,

    id: variantId,
    productVariantId: variantId,
    variantId,

    sku: variant?.sku || variant?.SKU || flashSale?.sku || "",
    capacity: extractCapacity(variant, flashSale),

    price: salePrice,
    salePrice,
    originalPrice,
    oldPrice: flashSale ? originalPrice : null,

    discountPercent: flashSale ? toNumber(flashSale.discountPercent, 0) : 0,
    isFlashSale: Boolean(flashSale),
    promotionId: flashSale?.promotionId ?? null,
    promotionName: flashSale?.promotionName ?? null,
    promotionEndDate: flashSale?.endDate ?? null,

    sellableQuantity: stock,

    // Compatibility cho code cũ; tồn thật vẫn là sellableQuantity.
    stock,
    stockQuantity: stock,

    bottleType:
      typeof variant?.bottleType === "object"
        ? variant?.bottleType?.name
        : variant?.bottleTypeName ||
          variant?.bottleType ||
          flashSale?.bottleType ||
          "",

    imageUrl: getImageUrlFromObject(variant),
  };
};

const mapProduct = (
  p: any,
  rawVariants: any[],
  flashSaleIndex: FlashSaleIndex
) => {
  const mappedVariants = Array.isArray(rawVariants)
    ? rawVariants
        .map((variant) => mapVariant(variant, flashSaleIndex))
        .sort((a: any, b: any) => {
          const valA = parseFloat(String(a.capacity).replace("ml", "")) || 0;
          const valB = parseFloat(String(b.capacity).replace("ml", "")) || 0;
          return valA - valB;
        })
    : [];

  const firstVariant = mappedVariants[0];

  const productImages = extractProductImages(p, mappedVariants);
  const mainImage = productImages[0] || getPlaceholderImage();

  return {
    ...p,

    id: Number(p?.id || p?.Id || 0),
    name: p?.name || p?.Name || "Sản phẩm",

    brand:
      typeof p?.brand === "object"
        ? p?.brand?.name
        : p?.brandName || p?.brand || "Premium",

    image: mainImage,
    imageUrl: mainImage,
    mainImage,
    images: productImages,
    imageList: productImages,
    galleryImages: productImages,
    productImages: productImages.map((imageUrl, index) => ({
      id: index + 1,
      imageUrl,
      url: imageUrl,
    })),

    description: p?.description || p?.Description || "",
    rating: Number(p?.rating || 0),

    gender: p?.gender,
    concentration: p?.concentration,
    concentrationName: p?.concentrationName,
    fragranceFamily: p?.fragranceFamily,
    fragranceFamilyName: p?.fragranceFamilyName,
    fragranceFamilies: p?.fragranceFamilies,
    scents: p?.scents,

    variants: mappedVariants,

    price: firstVariant
      ? Number(firstVariant.price || 0)
      : Number(p?.price || p?.Price || 0),

    salePrice: firstVariant
      ? Number(firstVariant.salePrice || firstVariant.price || 0)
      : Number(p?.salePrice || p?.price || p?.Price || 0),

    originalPrice: firstVariant
      ? Number(firstVariant.originalPrice || firstVariant.price || 0)
      : Number(p?.originalPrice || p?.price || p?.Price || 0),

    discountPercent: firstVariant
      ? Number(firstVariant.discountPercent || 0)
      : Number(p?.discountPercent || 0),

    isFlashSale: mappedVariants.some((variant: any) => variant.isFlashSale),
  };
};

const loadProductDetail = async () => {
  const productId = Number(route.params.id);

  if (!productId || Number.isNaN(productId)) {
    product.value = null;
    isLoading.value = false;
    return;
  }

  try {
    isLoading.value = true;
    product.value = null;

    const [productRes, flashSaleIndex] = await Promise.all([
      api.get(`/v1/products/${productId}`),
      fetchActiveFlashSaleIndex(),
    ]);

    const productData = extractObjectData(productRes.data);

    const variantData =
      productData?.variants ||
      productData?.productVariants ||
      productData?.productVariantList ||
      [];

    product.value = mapProduct(productData, variantData, flashSaleIndex);
  } catch (error: any) {
    console.error("Lỗi tải chi tiết sản phẩm:", error);

    product.value = null;

    await Swal.fire({
      icon: "error",
      title: "Không tải được sản phẩm",
      text:
        error?.response?.data?.message ||
        "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.",
      confirmButtonText: "Quay lại danh sách",
      confirmButtonColor: "#bd9a5f",
    });

    router.replace({
      name: "ProductList",
    });
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadProductDetail();
});

watch(
  () => route.params.id,
  () => {
    loadProductDetail();
  }
);

const goBack = () => {
  const previousPath = window.history.state?.back;

  if (previousPath) {
    router.back();
    return;
  }

  router.push({
    name: "ProductList",
  });
};

const handleBuyNow = () => {
  router.push({
    name: "Checkout",
  });
};
</script>

<style scoped>
.page-wrapper {
  font-family: "Inter", sans-serif;
  background-color: #ffffff;
  min-height: 100vh;
}

.single-product-main {
  max-width: 1320px;
  margin: 40px auto;
  padding: 0 20px;
  min-height: 50vh;
}
</style>