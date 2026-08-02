<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3 d-flex justify-content-between">
      <h5 class="mb-0 fw-bold">Sản phẩm yêu thích</h5>

      <button
        class="btn btn-outline-dark btn-sm"
        :disabled="store.favoriteLoading || extraLoading"
        @click="refreshFavorites"
      >
        Làm mới
      </button>
    </div>

    <div class="card-body">
      <div v-if="store.favoriteLoading" class="text-center py-5">
        <div class="spinner-border"></div>
      </div>

      <div v-else-if="store.favorites.length === 0" class="empty-box">
        Bạn chưa có sản phẩm yêu thích nào
      </div>

      <div v-else class="row g-3">
        <div
          v-for="item in store.favorites"
          :key="item.favoriteId"
          class="col-12 col-md-6 col-xl-4"
        >
          <div
            class="favorite-card"
            role="button"
            tabindex="0"
            @click="goToProduct(item)"
            @keyup.enter="goToProduct(item)"
          >
            <div class="d-flex gap-3 align-items-start">
              <img
                v-if="getFavoriteImage(item) && !imageErrorMap[item.favoriteId]"
                :src="getFavoriteImage(item)"
                class="product-img"
                :alt="item.productName || 'Sản phẩm yêu thích'"
                @error="markImageError(item.favoriteId)"
              />

              <div v-else class="product-img placeholder-img">
                No Image
              </div>

              <div class="flex-grow-1 min-w-0">
                <div class="fw-bold text-truncate product-name">
                  {{ item.productName || "Sản phẩm" }}
                </div>

                <div class="price-row mt-2">
                  <span class="fw-bold product-price">
                    {{ formatMoney(getDisplayPrice(item)) }}
                  </span>

                  <span
                    v-if="getDiscountPercent(item) > 0"
                    class="original-price"
                  >
                    {{ formatMoney(getOriginalPrice(item)) }}
                  </span>

                  <span
                    v-if="getDiscountPercent(item) > 0"
                    class="flash-badge"
                  >
                    -{{ getDiscountPercent(item) }}%
                  </span>
                </div>

                <div class="small text-muted mt-1">
                  Tồn kho: {{ item.stockQuantity ?? 0 }}
                </div>
              </div>
            </div>

            <button
              class="btn btn-outline-danger btn-sm w-100 mt-3"
              :disabled="store.favoriteLoading"
              @click.stop="store.deleteFavorite(item.favoriteId)"
            >
              Bỏ yêu thích
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import api from "@/common/api";
import { useCustomerProfileStore } from "../stores/customerProfile.store";

const store = useCustomerProfileStore();
const router = useRouter();

const BACKEND_URL = "http://localhost:8080";

const imageErrorMap = ref<Record<number, boolean>>({});
const productDetailMap = ref<Record<number, any>>({});
const flashSaleVariantMap = ref<Record<number, any>>({});
const extraLoading = ref(false);

const formatMoney = (value: number) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};

const toNumber = (value: unknown, fallback = 0) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : fallback;
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

const getProductId = (item: any) => {
  return toNumber(item?.productId ?? item?.id, 0);
};

const getVariantId = (item: any) => {
  return toNumber(
    item?.productVariantId ?? item?.variantId ?? item?.id,
    0
  );
};

const getProductDetail = (item: any) => {
  const productId = getProductId(item);
  return productId > 0 ? productDetailMap.value[productId] : null;
};

const getFavoriteImage = (item: any) => {
  const images: string[] = [];

  appendImage(images, item?.imageUrl);
  appendImage(images, item?.ImageUrl);
  appendImage(images, item?.image);
  appendImage(images, item?.Image);
  appendImage(images, item?.thumbnailUrl);
  appendImage(images, item?.ThumbnailUrl);
  appendImage(images, item?.mainImage);
  appendImage(images, item?.MainImage);
  appendImage(images, item?.mainImageUrl);
  appendImage(images, item?.MainImageUrl);

  appendImageList(images, item?.images);
  appendImageList(images, item?.Images);
  appendImageList(images, item?.productImages);
  appendImageList(images, item?.ProductImages);

  const detail = getProductDetail(item);
  const variantId = getVariantId(item);

  const detailVariants =
    detail?.variants ||
    detail?.productVariants ||
    detail?.productVariantList ||
    [];

  if (Array.isArray(detailVariants) && variantId > 0) {
    const matchedVariant = detailVariants.find((variant: any) => {
      const currentVariantId = toNumber(
        variant?.productVariantId ?? variant?.variantId ?? variant?.id,
        0
      );

      return currentVariantId === variantId;
    });

    if (matchedVariant) {
      appendImage(images, matchedVariant?.imageUrl);
      appendImage(images, matchedVariant?.ImageUrl);
      appendImage(images, matchedVariant?.image);
      appendImage(images, matchedVariant?.Image);
      appendImage(images, matchedVariant?.thumbnailUrl);
      appendImage(images, matchedVariant?.ThumbnailUrl);
      appendImage(images, matchedVariant?.mainImage);
      appendImage(images, matchedVariant?.MainImage);
      appendImage(images, matchedVariant?.mainImageUrl);
      appendImage(images, matchedVariant?.MainImageUrl);

      appendImageList(images, matchedVariant?.images);
      appendImageList(images, matchedVariant?.Images);
      appendImageList(images, matchedVariant?.productImages);
      appendImageList(images, matchedVariant?.ProductImages);
    }
  }

  appendImage(images, detail?.primaryImageUrl);
  appendImage(images, detail?.mainImage);
  appendImage(images, detail?.MainImage);
  appendImage(images, detail?.mainImageUrl);
  appendImage(images, detail?.MainImageUrl);
  appendImage(images, detail?.imageUrl);
  appendImage(images, detail?.ImageUrl);
  appendImage(images, detail?.image);
  appendImage(images, detail?.Image);
  appendImage(images, detail?.thumbnailUrl);
  appendImage(images, detail?.ThumbnailUrl);

  appendImageList(images, detail?.images);
  appendImageList(images, detail?.Images);
  appendImageList(images, detail?.productImages);
  appendImageList(images, detail?.ProductImages);
  appendImageList(images, detail?.galleryImages);
  appendImageList(images, detail?.imageList);

  return images[0] || "";
};

const markImageError = (favoriteId: number) => {
  imageErrorMap.value = {
    ...imageErrorMap.value,
    [favoriteId]: true,
  };
};

const getFlashSaleInfo = (item: any) => {
  const variantId = getVariantId(item);

  if (variantId > 0 && flashSaleVariantMap.value[variantId]) {
    return flashSaleVariantMap.value[variantId];
  }

  return null;
};

const getOriginalPrice = (item: any) => {
  const flashSale = getFlashSaleInfo(item);

  if (flashSale) {
    return toNumber(flashSale.originalPrice, item.price || 0);
  }

  return toNumber(item.price, 0);
};

const getDisplayPrice = (item: any) => {
  const flashSale = getFlashSaleInfo(item);

  if (flashSale) {
    return toNumber(flashSale.salePrice, item.price || 0);
  }

  return toNumber(item.price, 0);
};

const getDiscountPercent = (item: any) => {
  const flashSale = getFlashSaleInfo(item);

  if (!flashSale) {
    return 0;
  }

  const discountPercent = toNumber(flashSale.discountPercent, 0);
  if (discountPercent > 0) {
    return Math.round(discountPercent);
  }

  const originalPrice = getOriginalPrice(item);
  const salePrice = getDisplayPrice(item);

  if (originalPrice > 0 && salePrice > 0 && salePrice < originalPrice) {
    return Math.round(((originalPrice - salePrice) / originalPrice) * 100);
  }

  return 0;
};

const fetchFlashSaleProducts = async () => {
  try {
    const res = await api.get("/promotions/flash-sale", {
      params: {
        page: 0,
        size: 200,
      },
    });

    const rows = resolvePageContent<any>(res.data);
    const nextVariantMap: Record<number, any> = {};

    rows.forEach((item: any) => {
      const variantId = toNumber(
        item?.productVariantId ?? item?.variantId,
        0
      );

      const salePrice = toNumber(item?.salePrice, 0);
      const originalPrice = toNumber(item?.originalPrice, 0);

      if (variantId > 0 && salePrice > 0 && originalPrice > salePrice) {
        nextVariantMap[variantId] = item;
      }
    });

    flashSaleVariantMap.value = nextVariantMap;
  } catch (error) {
    console.error("Lỗi tải Flash Sale cho yêu thích:", error);
    flashSaleVariantMap.value = {};
  }
};

const fetchProductDetailsForFavorites = async () => {
  const productIds = Array.from(
    new Set(
      store.favorites
        .map((item: any) => getProductId(item))
        .filter((productId: number) => productId > 0)
    )
  );

  const missingProductIds = productIds.filter(
    (productId) => !productDetailMap.value[productId]
  );

  if (missingProductIds.length === 0) {
    return;
  }

  const detailEntries = await Promise.all(
    missingProductIds.map(async (productId) => {
      try {
        const res = await api.get(`/v1/products/${productId}`);
        const data = res.data?.data || res.data;

        return [productId, data] as const;
      } catch (error) {
        console.error(`Lỗi tải chi tiết sản phẩm ${productId}:`, error);
        return [productId, null] as const;
      }
    })
  );

  const nextMap = {
    ...productDetailMap.value,
  };

  detailEntries.forEach(([productId, detail]) => {
    if (detail) {
      nextMap[productId] = detail;
    }
  });

  productDetailMap.value = nextMap;
};

const loadExtraData = async () => {
  try {
    extraLoading.value = true;

    await Promise.all([
      fetchFlashSaleProducts(),
      fetchProductDetailsForFavorites(),
    ]);
  } finally {
    extraLoading.value = false;
  }
};

const refreshFavorites = async () => {
  await store.fetchFavorites();
  imageErrorMap.value = {};
  await loadExtraData();
};

const goToProduct = (item: any) => {
  const productId = getProductId(item);

  if (productId > 0) {
    router.push({
      name: "SingleProduct",
      params: { id: productId },
    });
  }
};

onMounted(async () => {
  if (store.favorites.length === 0) {
    await store.fetchFavorites();
  }

  await loadExtraData();
});

watch(
  () =>
    store.favorites
      .map((item: any) => `${item.favoriteId}-${item.productId}-${item.productVariantId}`)
      .join("|"),
  async () => {
    imageErrorMap.value = {};
    await loadExtraData();
  }
);
</script>

<style scoped>
.favorite-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.favorite-card:hover {
  transform: translateY(-2px);
  border-color: rgba(183, 141, 82, 0.45);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.favorite-card:focus {
  outline: 2px solid rgba(183, 141, 82, 0.35);
  outline-offset: 3px;
}

.product-img {
  width: 76px;
  height: 76px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
  background: #f9fafb;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 12px;
}

.product-name {
  color: #06132b;
}

.price-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.product-price {
  color: #06132b;
}

.original-price {
  color: #9ca3af;
  font-size: 12px;
  text-decoration: line-through;
}

.flash-badge {
  background: #b31320;
  color: #ffffff;
  border-radius: 999px;
  padding: 2px 7px;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.3;
}

.min-w-0 {
  min-width: 0;
}

.empty-box {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 16px;
}
</style>