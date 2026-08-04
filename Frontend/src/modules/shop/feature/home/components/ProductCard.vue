<template>
  <div
    class="product-card h-100 position-relative overflow-hidden"
    @click="goToDetail"
  >
    <span v-if="cardDiscountPercent > 0" class="discount-badge">
      -{{ cardDiscountPercent }}%
    </span>

    <button
      type="button"
      class="btn-favorite"
      :class="{ active: isFavorited }"
      :disabled="favoriteLoading"
      @click.stop="handleToggleFavorite"
      :title="isFavorited ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
    >
      <i v-if="favoriteLoading" class="spinner-border spinner-border-sm"></i>
      <i
        v-else
        class="bi"
        :class="isFavorited ? 'bi-heart-fill' : 'bi-heart'"
      ></i>
    </button>

    <div class="product-image-wrapper">
      <img
        v-if="hasProductImage"
        :src="productImage"
        :alt="product.name"
        class="product-real-image"
        @error="handleProductImageError"
      />

      <div v-else class="product-bottle" :style="getBottleStyle(product.color)">
        <div class="product-bottle-cap"></div>
        <div class="product-bottle-neck"></div>

        <div class="product-bottle-body">
          <div class="product-bottle-label">
            <strong>{{ shortBrand }}</strong>
            <span>PERFUME</span>
          </div>
        </div>
      </div>
    </div>

    <div class="product-content">
      <p class="product-brand mb-1">
        {{ product.brand }}
      </p>

      <h3 class="product-name text-truncate mb-2">
        {{ product.name }}
      </h3>

      <div class="rating-row d-flex align-items-center gap-2 mb-2">
        <span class="stars">{{ starsDisplay }}</span>

        <span class="review-count">
          {{ ratingDisplay }} | {{ normalizedReviewCount }} đánh giá
        </span>
      </div>

      <div class="price-row d-flex align-items-end gap-2 mb-3 flex-wrap">
        <span v-if="hasVariantPriceRange" class="price-prefix">Từ</span>

        <span class="sale-price">
          {{ formatCurrency(cardSalePrice) }}
        </span>

        <span
          v-if="cardDiscountPercent > 0 && cardOriginalPrice > cardSalePrice"
          class="original-price text-decoration-line-through"
        >
          {{ formatCurrency(cardOriginalPrice) }}
        </span>
      </div>

      <div class="product-actions" style="position: relative; z-index: 10">
        <button
          type="button"
          class="btn buy-now-btn"
          @click.stop="openVariantModal('BUY')"
        >
          <i class="bi bi-lightning-charge me-2"></i> Mua ngay
        </button>

        <button
          type="button"
          class="btn add-cart-btn"
          @click.stop="openVariantModal('CART')"
        >
          <i class="bi bi-bag-plus me-2"></i> Thêm vào giỏ
        </button>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="toast-slide">
        <div v-if="toast.show" class="custom-cart-toast" :class="toast.type">
          <div class="toast-icon">
            <i
              class="bi"
              :class="{
                'bi-check2': toast.type === 'success',
                'bi-exclamation-triangle': toast.type === 'warning',
                'bi-x-lg': toast.type === 'error',
              }"
            ></i>
          </div>

          <div class="toast-info">
            <h4>{{ toast.title }}</h4>
            <p>{{ toast.message }}</p>
          </div>

          <RouterLink
            v-if="toast.showCartLink"
            to="/cart"
            class="toast-view-cart"
          >
            XEM GIỎ HÀNG <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showVariantModal"
        class="custom-modal-overlay"
        @click.self="showVariantModal = false"
      >
        <div class="variant-modal-box">
          <div class="vm-header">
            <h5>Chọn Phân Loại</h5>
            <button class="vm-close" @click="showVariantModal = false">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="vm-product-info">
            <div class="vm-img-box">
              <img
                :src="modalImage"
                alt="Product Image"
                @error="handleModalImageError"
              />
            </div>

            <div class="vm-details">
              <h6>{{ product.name }}</h6>
              <div class="d-flex align-items-end gap-2 flex-wrap">
                <p class="vm-price mb-0">
                  {{
                    formatCurrency(
                      selectedVariant
                        ? selectedVariant.salePrice ||
                            selectedVariant.price ||
                            cardSalePrice
                        : cardSalePrice,
                    )
                  }}
                </p>
                <span
                  v-if="
                    selectedVariant &&
                    selectedVariant.salePrice &&
                    selectedVariant.salePrice < selectedVariant.price
                  "
                  class="text-decoration-line-through text-muted small"
                >
                  {{ formatCurrency(selectedVariant.price) }}
                </span>
                <span
                  v-else-if="!selectedVariant && cardDiscountPercent > 0"
                  class="text-decoration-line-through text-muted small"
                >
                  {{ formatCurrency(cardOriginalPrice) }}
                </span>

                <span
                  v-if="calculatedDiscountPercent > 0"
                  class="flash-sale-badge"
                >
                  -{{ calculatedDiscountPercent }}%
                </span>
              </div>
            </div>
          </div>

          <div class="vm-variants">
            <p class="vm-label">TÙY CHỌN PHÂN LOẠI:</p>

            <div v-if="isLoadingVariants" class="text-center py-4">
              <span
                class="spinner-border spinner-border-sm me-2"
                style="color: #b78d52"
              ></span>
              <span style="color: #718096; font-size: 13px">
                Đang tải thông tin...
              </span>
            </div>

            <div v-else class="vm-grid">
              <button
                v-for="v in fullVariants"
                :key="v.productVariantId || v.id"
                class="vm-variant-btn"
                :class="{
                  selected:
                    (selectedVariant?.productVariantId ||
                      selectedVariant?.id) === (v.productVariantId || v.id),
                  disabled: Number(v.stockQuantity || v.stock || 0) <= 0,
                }"
                @click="
                  selectedVariant = v;
                  quantity = 1;
                "
              >
                <span class="vm-v-name">
                  {{ v.displayCapacity || formatVariantName(v) }}
                </span>
              </button>
            </div>

            <hr class="variant-divider" v-if="selectedVariant" />

            <div class="quantity-section" v-if="selectedVariant">
              <p class="vm-label mb-0">SỐ LƯỢNG:</p>
              <div class="quantity-control">
                <div class="qty-wrapper">
                  <!-- Nút trừ -->
                  <button
                    type="button"
                    @click="quantity > 1 ? quantity-- : null"
                    :disabled="quantity <= 1"
                  >
                    −
                  </button>

                  <!-- Ô nhập -->
                  <input
                    type="number"
                    v-model="quantity"
                    @input="validateQuantity"
                    @blur="validateQuantity"
                    @keyup.enter="validateQuantity"
                  />

                  <!-- Nút cộng (Gọi hàm để bắn thông báo) -->
                  <button type="button" @click="increaseQuantity">+</button>
                </div>
                <span class="stock-info"> Kho: {{ maxQuantity }} </span>
              </div>
            </div>
          </div>

          <button
            class="vm-confirm-btn"
            :disabled="
              !selectedVariant ||
              addCartLoading ||
              buyNowLoading ||
              isLoadingVariants
            "
            @click="confirmAction"
          >
            <span
              v-if="addCartLoading || buyNowLoading"
              class="spinner-border spinner-border-sm me-2"
            ></span>
            XÁC NHẬN {{ actionType === "CART" ? "THÊM VÀO GIỎ" : "MUA NGAY" }}
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import api from "@/common/api";
import { favoriteService } from "@/modules/shop/feature/product/services/favorite.service";

const quantity = ref(1);

interface ProductVariant {
  id?: number;
  Id?: number;
  variantId?: number;
  productVariantId?: number;
  price?: number;
  originalPrice?: number;
  oldPrice?: number;
  salePrice?: number;
  promotionPrice?: number;
  flashSalePrice?: number;
  currentPrice?: number;
  displayPrice?: number;
  finalPrice?: number;
  minPrice?: number;
  discountPercent?: number;
  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  quantity?: number;
  status?: number;
  capacity?: string | number | any;
  capacityName?: string | number;
  capacityValue?: string | number;
  volume?: string | number;
  bottleType?: string | any;
  bottleTypeName?: string;

  imageUrl?: string;
  ImageUrl?: string;
  image?: string;
  Image?: string;
  mainImage?: string;
  mainImageUrl?: string;
  thumbnailUrl?: string;
  images?: any[];
  Images?: any[];
  imageList?: any[];
  productImages?: any[];
  ProductImages?: any[];
  productImageList?: any[];
  ProductImageList?: any[];
}

interface Product {
  id: number;
  productId?: number;
  productVariantId?: number;
  variantId?: number;
  name: string;
  brand: string;
  color?: string;
  price?: number;
  oldPrice?: number;
  promotionPrice?: number;
  flashSalePrice?: number;
  currentPrice?: number;
  displayPrice?: number;
  finalPrice?: number;
  minPrice?: number;
  salePrice: number;
  originalPrice: number;
  discountPercent: number;
  rating: number;
  averageRating?: number;
  reviewCount: number;

  imageUrl?: string;
  ImageUrl?: string;
  image?: string;
  Image?: string;
  mainImage?: string;
  MainImage?: string;
  mainImageUrl?: string;
  MainImageUrl?: string;
  thumbnailUrl?: string;
  ThumbnailUrl?: string;
  images?: any[];
  Images?: any[];
  imageList?: any[];
  ImageList?: any[];
  galleryImages?: any[];
  GalleryImages?: any[];
  productImages?: any[];
  ProductImages?: any[];
  productImageList?: any[];
  ProductImageList?: any[];

  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  status?: number;
  isFlashSale?: boolean;
  variants?: ProductVariant[];
}

const props = defineProps<{
  product: Product;
}>();

const router = useRouter();

const addCartLoading = ref(false);
const buyNowLoading = ref(false);
const favoriteLoading = ref(false);
const isFavorited = ref(false);
const favoritedMap = ref<Record<number, boolean>>({});

const showVariantModal = ref(false);
const isLoadingVariants = ref(false);
const actionType = ref<"CART" | "BUY">("CART");
const selectedVariant = ref<any>(null);
const fullVariants = ref<any[]>([]);
const imageLoadError = ref(false);

const toast = ref({
  show: false,
  type: "success" as "success" | "warning" | "error",
  title: "",
  message: "",
  showCartLink: false,
});

let toastTimer: ReturnType<typeof window.setTimeout> | undefined;

const BACKEND_URL = "http://localhost:8080";

const brandMap: Record<string, string> = {
  Chanel: "CHANEL",
  Dior: "DIOR",
  "Yves Saint Laurent": "YSL",
  "Giorgio Armani": "ARMANI",
  Givenchy: "GIVENCHY",
  Creed: "CREED",
  Byredo: "BYREDO",
  "Tom Ford": "TOM FORD",
  "Maison Francis Kurkdjian": "MFK",
  "Le Labo": "LE LABO",
  "Paco Rabanne": "PACO",
};

const shortBrand = computed(
  () =>
    brandMap[props.product.brand] ||
    String(props.product.brand || "AURA")
      .slice(0, 8)
      .toUpperCase(),
);


const toFiniteNumber = (value: unknown, fallback = 0) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : fallback;
};

const clampRating = (value: unknown) => {
  return Math.min(MAX_RATING, Math.max(0, toFiniteNumber(value, 0)));
};

const DEFAULT_RATING = 5;
const MAX_RATING = 5;

// Thêm 2 state để lưu data đồng bộ ngầm
const syncedRating = ref<number | null>(null);
const syncedReviews = ref<number | null>(null);

const normalizedReviewCount = computed(() => {
  // Ưu tiên data fetch bù nếu có
  if (syncedReviews.value !== null && syncedReviews.value > 0) {
    return syncedReviews.value;
  }
  const p = props.product as any;
  const count = Number(p?.reviewCount || p?.reviews || p?.totalReviews || 0);
  return Math.max(0, Math.floor(count));
});

const rawAverageRating = computed(() => {
  if (syncedRating.value !== null && syncedRating.value > 0) {
    return syncedRating.value;
  }
  const p = props.product as any;
  const raw = Number(p?.averageRating || p?.avgRating || p?.rating || 0);
  return Math.min(MAX_RATING, Math.max(0, raw));
});

const ratingValue = computed(() => {
  // Nếu có đánh giá thật hoặc điểm > 0 thì hiện điểm thật
  if (normalizedReviewCount.value > 0 || rawAverageRating.value > 0) {
    return rawAverageRating.value;
  }
  return DEFAULT_RATING;
});

const ratingDisplay = computed(() => {
  return ratingValue.value.toFixed(1);
});

const starsDisplay = computed(() => {
  const rounded = Math.round(ratingValue.value);
  const filled = Math.max(0, Math.min(MAX_RATING, rounded));
  return "★".repeat(filled) + "☆".repeat(MAX_RATING - filled);
});

// Hàm chạy ngầm kéo dữ liệu thật nếu thẻ đang bị mắc kẹt ở 0 đánh giá do backend cache
const syncProductData = async () => {
  const p = props.product as any;
  const currentCount = Number(p?.reviewCount || p?.reviews || p?.totalReviews || 0);
  const productId = Number(p?.productId || p?.id || 0);

  // Chỉ gọi API ngầm nếu API trang chủ đang báo 0 đánh giá
  if (currentCount === 0 && productId > 0) {
    try {
      const res = await api.get(`/v1/products/${productId}`);
      const data = res.data?.data || res.data;
      if (data) {
        syncedRating.value = Number(data.averageRating || data.avgRating || data.rating || 0);
        syncedReviews.value = Number(data.reviewCount || data.reviews || data.totalReviews || 0);
      }
    } catch (error) {
      // Bỏ qua nếu lỗi
    }
  }
};

const hasReviewCountSource = computed(() => {
  return (
    props.product.reviewCount !== null &&
    props.product.reviewCount !== undefined
  );
});

const hasActualReviews = computed(() => {
  if (hasReviewCountSource.value) {
    return normalizedReviewCount.value > 0;
  }
  return rawAverageRating.value > 0;
});

const getBottleStyle = (color?: string): Record<string, string> => ({
  "--bottle-color": color || "#0a192f",
});

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ";

const getVariantIdValue = (value: any) =>
  Number(
    value?.productVariantId ?? value?.variantId ?? value?.id ?? value?.Id ?? 0,
  );

const getRawOriginalPrice = (value: any) =>
  toFiniteNumber(
    value?.originalPrice ??
      value?.oldPrice ??
      value?.listPrice ??
      value?.basePrice ??
      value?.Price ??
      value?.price,
    0,
  );

const getRawSalePrice = (value: any) =>
  toFiniteNumber(
    value?.salePrice ??
      value?.promotionPrice ??
      value?.flashSalePrice ??
      value?.currentPrice ??
      value?.displayPrice ??
      value?.finalPrice ??
      value?.minPrice ??
      value?.price ??
      value?.Price,
    0,
  );

const getRawDiscountPercent = (value: any) =>
  Math.max(
    0,
    Math.round(
      toFiniteNumber(
        value?.discountPercent ??
          value?.discount ??
          value?.salePercent ??
          value?.promotionPercent,
        0,
      ),
    ),
  );

const getStockValue = (value: any) =>
  toFiniteNumber(
    value?.stockQuantity ??
      value?.stock ??
      value?.availableQuantity ??
      value?.quantity,
    -1,
  );

const getPriceInfo = (value: any) => {
  const salePrice = getRawSalePrice(value);
  const originalPrice = getRawOriginalPrice(value) || salePrice;
  const discountFromField = getRawDiscountPercent(value);
  const discountFromPrice =
    originalPrice > 0 && salePrice > 0 && salePrice < originalPrice
      ? Math.round(((originalPrice - salePrice) / originalPrice) * 100)
      : 0;
  const discountPercent = discountFromField || discountFromPrice;

  return {
    variantId: getVariantIdValue(value),
    salePrice: salePrice || originalPrice,
    originalPrice: originalPrice || salePrice,
    discountPercent,
    stock: getStockValue(value),
    source: value,
  };
};

const cardPriceInfo = computed(() => {
  const p = props.product as any;
  const variants = Array.isArray(p?.variants) ? p.variants : [];
  const variantPrices = variants
    .map((variant: any) => getPriceInfo(variant))
    .filter((info: any) => info.salePrice > 0);

  const inStockVariantPrices = variantPrices.filter(
    (info: any) => info.stock !== 0,
  );
  const candidates =
    inStockVariantPrices.length > 0 ? inStockVariantPrices : variantPrices;

  if (candidates.length > 0) {
    return candidates.slice().sort((a: any, b: any) => {
      if (a.salePrice !== b.salePrice) {
        return a.salePrice - b.salePrice;
      }

      return b.discountPercent - a.discountPercent;
    })[0];
  }

  return getPriceInfo(p);
});

const cardSalePrice = computed(() => cardPriceInfo.value.salePrice || 0);

const cardOriginalPrice = computed(
  () => cardPriceInfo.value.originalPrice || cardSalePrice.value,
);

const cardDiscountPercent = computed(() =>
  Math.max(0, Math.round(cardPriceInfo.value.discountPercent || 0)),
);

const cardRepresentativeVariantId = computed(() =>
  Number(cardPriceInfo.value.variantId || 0),
);

const hasVariantPriceRange = computed(() => {
  const variants = Array.isArray((props.product as any)?.variants)
    ? (props.product as any).variants
    : [];

  if (variants.length <= 1) {
    return false;
  }

  const prices = variants
    .map((variant: any) => getPriceInfo(variant).salePrice)
    .filter((price: number) => price > 0);

  return new Set(prices).size > 1;
});

const normalizeImageUrl = (url: unknown) => {
  const rawUrl = String(url || "").trim();
  if (!rawUrl) return "";
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
  if (!value) return "";
  if (typeof value === "string") return normalizeImageUrl(value);
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
      "",
  );
};

const appendImage = (images: string[], value: any) => {
  const imageUrl = getImageUrlFromObject(value);
  if (imageUrl && !images.includes(imageUrl)) {
    images.push(imageUrl);
  }
};

const appendImageList = (images: string[], value: any) => {
  if (!value) return;
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

const productImages = computed(() => {
  const images: string[] = [];
  const p = props.product as any;

  const addUnique = (url: unknown) => {
    const formatted = getImageUrlFromObject(url);
    if (formatted && !images.includes(formatted)) {
      images.push(formatted);
    }
  };

  const imageArrays = [
    p?.images,
    p?.productImages,
    p?.galleryImages,
    p?.imageList,
  ];
  for (const arr of imageArrays) {
    if (Array.isArray(arr)) {
      const primaryObj = arr.find((img: any) =>
        Boolean(img?.isPrimary || img?.is_primary || img?.primary),
      );
      if (primaryObj) {
        addUnique(primaryObj?.imageUrl || primaryObj?.url || primaryObj);
      }
    }
  }

  addUnique(p?.primaryImageUrl);
  addUnique(p?.mainImage);
  addUnique(p?.MainImage);
  addUnique(p?.imageUrl);
  addUnique(p?.ImageUrl);
  addUnique(p?.image);
  addUnique(p?.thumbnailUrl);

  imageArrays.forEach((arr) => {
    appendImageList(images, arr);
  });

  if (Array.isArray(p?.variants)) {
    p.variants.forEach((variant: any) => {
      getVariantImageList(variant).forEach((imageUrl: any) => {
        addUnique(imageUrl);
      });
    });
  }

  return images;
});

const productImage = computed(() => productImages.value[0] || "");
const hasProductImage = computed(
  () => Boolean(productImage.value) && !imageLoadError.value,
);

const modalImage = computed(() => {
  if (selectedVariant.value) {
    const variantImage = getVariantImageList(selectedVariant.value)[0];
    if (variantImage) return variantImage;
  }
  return productImage.value || getPlaceholderImage();
});

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

const handleProductImageError = () => {
  imageLoadError.value = true;
};

const handleModalImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;
  if (!target) return;
  target.onerror = null;
  target.src = getPlaceholderImage();
};

const formatVariantName = (v: any) => {
  if (!v) return "Loại";

  let cap = null;
  if (v.capacity && typeof v.capacity === "object") {
    cap = v.capacity.value ?? v.capacity.name;
  } else if (v.capacityValue != null) {
    cap = v.capacityValue;
  } else if (v.volume != null) {
    cap = v.volume;
  } else if (typeof v.capacity === "string" || typeof v.capacity === "number") {
    cap = v.capacity;
  }

  let capString = "";
  if (cap != null && cap !== "") {
    const numeric = Number(cap);
    if (!Number.isNaN(numeric)) {
      capString = `${numeric}ml`;
    } else {
      const text = String(cap);
      capString = text.toLowerCase().includes("ml") ? text : `${text}ml`;
    }
  }

  const bottle = v.bottleTypeName || v.bottleType;
  const bottleString = typeof bottle === "object" ? bottle?.name : bottle;

  if (capString && bottleString) {
    return `${capString} - ${bottleString}`;
  }

  return capString || bottleString || "Loại " + (v.productVariantId || v.id);
};

const showToast = (
  type: "success" | "warning" | "error",
  title: string,
  message: string,
  showCartLink = false,
) => {
  toast.value = { show: true, type, title, message, showCartLink };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toast.value.show = false;
  }, 2800);
};

const checkLoginBeforeAction = () => {
  const token = localStorage.getItem("token");
  const rawRole =
    localStorage.getItem("role") || localStorage.getItem("userRole") || "";
  const role = rawRole.replace("ROLE_", "").toUpperCase().trim();

  if (!token) {
    showToast(
      "warning",
      "Yêu cầu đăng nhập",
      "Vui lòng đăng nhập để tiếp tục trải nghiệm mua sắm tại Dominus.",
    );
    setTimeout(() => {
      router
        .push({
          name: "Login",
          query: { redirect: router.currentRoute.value.fullPath },
        })
        .catch(() => {});
    }, 1500);
    return false;
  }

  if (role !== "USER" && role !== "CUSTOMER") {
    showToast(
      "error",
      "Từ chối thao tác",
      "Chức năng này chỉ dành cho tài khoản Khách hàng.",
    );
    return false;
  }
  return true;
};

const openVariantModal = async (type: "CART" | "BUY") => {
  if (!checkLoginBeforeAction()) return;

  actionType.value = type;
  selectedVariant.value = null;
  fullVariants.value = [];
  quantity.value = 1;
  showVariantModal.value = true;
  isLoadingVariants.value = true;

  try {
    const flashSalePriceMap = new Map<number, number>();
    const flashSaleVariantIds = new Set<number>();

    if (
      props.product?.isFlashSale &&
      props.product?.variants &&
      Array.isArray(props.product.variants)
    ) {
      props.product.variants.forEach((pv: any) => {
        const vId = Number(pv.productVariantId || pv.variantId || pv.id);
        if (vId) {
          flashSaleVariantIds.add(vId);
          if (pv.salePrice != null) {
            flashSalePriceMap.set(vId, Number(pv.salePrice));
          }
        }
      });
    }

    const res = await api.get(`/v1/products/${getProductId()}`);
    const data = res.data?.data || res.data;
    let rawVariants =
      data?.variants || data?.productVariants || data?.productVariantList;

    if (!rawVariants || rawVariants.length === 0) {
      rawVariants = props.product.variants || [props.product];
    }

    const processedVariants = rawVariants.map((v: any) => {
      const vId = Number(v.productVariantId || v.variantId || v.id);

      let cap = null;
      if (v.capacityName != null) {
        cap = v.capacityName;
      } else if (v.capacity && typeof v.capacity === "object") {
        cap = v.capacity.value ?? v.capacity.name;
      } else if (v.capacityValue != null) {
        cap = v.capacityValue;
      } else if (v.volume != null) {
        cap = v.volume;
      } else if (
        typeof v.capacity === "string" ||
        typeof v.capacity === "number"
      ) {
        cap = v.capacity;
      }

      let displayCap = "";
      let numericCap = 0;

      if (cap != null && cap !== "") {
        numericCap = parseFloat(String(cap).replace("ml", "")) || 0;
        displayCap = numericCap > 0 ? `${numericCap}ml` : String(cap);
      }

      const bottle = v.bottleTypeName || v.bottleType;
      const bottleName = typeof bottle === "object" ? bottle?.name : bottle;

      if (displayCap && bottleName) {
        displayCap = `${displayCap} - ${bottleName}`;
      } else if (bottleName) {
        displayCap = bottleName;
      } else if (!displayCap) {
        displayCap = "Loại " + (vId || "");
      }

      const mappedSalePrice =
        flashSalePriceMap.get(vId) ??
        v.salePrice ??
        v.promotionPrice ??
        v.price;
      const mappedPrice = v.price ?? v.originalPrice;

      return {
        ...v,
        productVariantId: vId,
        id: vId,
        salePrice: mappedSalePrice,
        price: mappedPrice,
        displayCapacity: displayCap,
        numericCapacity: numericCap,
      };
    });

    processedVariants.sort(
      (a: any, b: any) => a.numericCapacity - b.numericCapacity,
    );
    fullVariants.value = processedVariants;

    if (fullVariants.value.length > 0) {
      selectedVariant.value =
        fullVariants.value.find((variant: any) => {
          const variantId = Number(
            variant?.productVariantId || variant?.variantId || variant?.id || 0,
          );

          return (
            cardRepresentativeVariantId.value > 0 &&
            variantId === cardRepresentativeVariantId.value
          );
        }) || fullVariants.value[0];
    }
  } catch (error) {
    console.error("Lỗi lấy danh sách biến thể:", error);
    const fallbackVariants = props.product.variants || [props.product];
    fullVariants.value = fallbackVariants.map((v: any) => ({
      ...v,
      displayCapacity: formatVariantName(v),
      numericCapacity: 0,
    }));
    if (fullVariants.value.length > 0) {
      selectedVariant.value = fullVariants.value[0];
    }
  } finally {
    isLoadingVariants.value = false;
  }
};

const confirmAction = async () => {
  if (!selectedVariant.value) return;

  const variantId = Number(
    selectedVariant.value.productVariantId ||
      selectedVariant.value.variantId ||
      selectedVariant.value.id ||
      props.product.id,
  );

  if (actionType.value === "CART") {
    try {
      addCartLoading.value = true;
      await api.post("/v1/customer/cart/add", {
        productVariantId: variantId,
        quantity: quantity.value,
      });
      window.dispatchEvent(new Event("cart-updated"));
      showVariantModal.value = false;
      showToast(
        "success",
        "Thêm thành công",
        "Đã thêm sản phẩm vào giỏ.",
        true,
      );
    } catch (error: any) {
      showToast(
        "error",
        "Lỗi",
        error?.response?.data?.message || "Không thể thêm vào giỏ.",
      );
    } finally {
      addCartLoading.value = false;
    }
  } else {
    try {
      buyNowLoading.value = true;
      await api.post("/v1/customer/cart/add", {
        productVariantId: variantId,
        quantity: quantity.value,
      });
      window.dispatchEvent(new Event("cart-updated"));
      showVariantModal.value = false;
      router.push({ name: "Checkout" });
    } catch (error: any) {
      showToast(
        "error",
        "Lỗi",
        error?.response?.data?.message || "Không thể mua ngay lúc này.",
      );
    } finally {
      buyNowLoading.value = false;
    }
  }
};

// Hàm lấy ID biến thể chính đại diện cho card sản phẩm
const getPrimaryVariantId = () => {
  if (
    props.product?.variants &&
    Array.isArray(props.product.variants) &&
    props.product.variants.length > 0
  ) {
    const v = props.product.variants[0];
    return Number(v?.productVariantId || v?.variantId || v?.id || 0);
  }
  return Number(
    props.product.productVariantId ||
      props.product.variantId ||
      props.product.id ||
      0,
  );
};

const loadFavoriteStatus = async () => {
  const token = localStorage.getItem("token");
  const rawRole =
    localStorage.getItem("role") || localStorage.getItem("userRole") || "";
  const role = rawRole.replace("ROLE_", "").toUpperCase().trim();

  if (!token || (role !== "USER" && role !== "CUSTOMER")) {
    favoritedMap.value = {};
    isFavorited.value = false;
    return;
  }

  try {
    const res = await favoriteService.getFavorites();
    const list = Array.isArray(res.data) ? res.data : [];
    const nextMap: Record<number, boolean> = {};

    list.forEach((item: any) => {
      const variantId = Number(item?.productVariantId || 0);
      if (variantId > 0) {
        nextMap[variantId] = true;
      }
    });

    favoritedMap.value = nextMap;

    // Kiểm tra xem biến thể của card này hoặc bất kỳ biến thể nào thuộc sản phẩm này có nằm trong danh sách yêu thích không
    const primaryId = getPrimaryVariantId();
    let matched = primaryId ? Boolean(nextMap[primaryId]) : false;

    if (
      !matched &&
      props.product?.variants &&
      Array.isArray(props.product.variants)
    ) {
      matched = props.product.variants.some((v: any) => {
        const vId = Number(v?.productVariantId || v?.variantId || v?.id || 0);
        return vId && nextMap[vId];
      });
    }

    isFavorited.value = matched;
  } catch (error) {
    favoritedMap.value = {};
    isFavorited.value = false;
  }
};

const handleToggleFavorite = async () => {
  const variantId = getPrimaryVariantId();
  if (!variantId || Number.isNaN(variantId)) return;
  if (!checkLoginBeforeAction()) return;

  try {
    favoriteLoading.value = true;
    const res = await favoriteService.toggleFavorite(variantId);
    const favorited = Boolean(res.data?.favorited);

    favoritedMap.value = {
      ...favoritedMap.value,
      [variantId]: favorited,
    };
    isFavorited.value = favorited;

    window.dispatchEvent(
      new CustomEvent("favorite-updated", {
        detail: { productVariantId: variantId, favorited },
      }),
    );

    showToast(
      favorited ? "success" : "warning",
      favorited ? "Đã thêm yêu thích" : "Đã bỏ yêu thích",
      res.data?.message || "",
    );
  } catch (error: any) {
    showToast("error", "Lỗi", "Không thể xử lý yêu thích");
  } finally {
    favoriteLoading.value = false;
  }
};

const handleFavoriteUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{
    productVariantId?: number;
    favorited?: boolean;
  }>;
  const variantId = Number(customEvent.detail?.productVariantId || 0);
  const favorited = Boolean(customEvent.detail?.favorited);

  if (!variantId) return;

  favoritedMap.value = {
    ...favoritedMap.value,
    [variantId]: favorited,
  };

  const primaryId = getPrimaryVariantId();
  let matched = primaryId === variantId ? favorited : isFavorited.value;

  if (
    !matched &&
    props.product?.variants &&
    Array.isArray(props.product.variants)
  ) {
    matched = props.product.variants.some((v: any) => {
      const vId = Number(v?.productVariantId || v?.variantId || v?.id || 0);
      return vId && favoritedMap.value[vId];
    });
  }

  isFavorited.value = matched;
};

const getProductId = () =>
  Number(props.product.productId || props.product.id || 0);

const goToDetail = () => {
  const productId = getProductId();
  if (productId > 0) {
    router.push({ name: "SingleProduct", params: { id: productId } });
  } else {
    router.push("/product");
  }
};

const calculatedDiscountPercent = computed(() => {
  if (selectedVariant.value) {
    const original =
      selectedVariant.value.originalPrice ||
      selectedVariant.value.oldPrice ||
      selectedVariant.value.price ||
      cardOriginalPrice.value;
    const sale =
      selectedVariant.value.salePrice ||
      selectedVariant.value.promotionPrice ||
      selectedVariant.value.flashSalePrice ||
      selectedVariant.value.price ||
      cardSalePrice.value;

    if (original && sale && original > sale) {
      return Math.round(((original - sale) / original) * 100);
    }

    return 0;
  }

  return cardDiscountPercent.value;
});

const maxQuantity = computed(() => {
  return selectedVariant.value
    ? Number(
        selectedVariant.value.stockQuantity || selectedVariant.value.stock || 0,
      )
    : 0;
});

// Sửa hàm validate (khi khách nhập tay)
const validateQuantity = () => {
  let val = Number(quantity.value);

  if (Number.isNaN(val) || val < 1) {
    quantity.value = 1;
  }
  // Thêm cảnh báo nếu nhập > 10
  else if (val > 10) {
    quantity.value = 10;
    showToast(
      "warning",
      "Giới hạn mua",
      "Bạn chỉ được mua tối đa 10 sản phẩm cho mỗi phân loại.",
    );
  } else if (val > maxQuantity.value) {
    quantity.value = maxQuantity.value;
    showToast(
      "warning",
      "Giới hạn tồn kho",
      `Sản phẩm chỉ còn ${maxQuantity.value} trong kho.`,
    );
  } else {
    quantity.value = Math.floor(val);
  }
};

// Tạo thêm hàm này để xử lý nút bấm dấu +
const increaseQuantity = () => {
  if (quantity.value >= 10) {
    showToast(
      "warning",
      "Giới hạn mua",
      "Bạn chỉ được mua tối đa 10 sản phẩm cho mỗi phân loại.",
    );
    return;
  }
  if (quantity.value >= maxQuantity.value) {
    showToast(
      "warning",
      "Giới hạn tồn kho",
      `Sản phẩm chỉ còn ${maxQuantity.value} trong kho.`,
    );
    return;
  }
  quantity.value++;
};

onMounted(() => {
  window.addEventListener("favorite-updated", handleFavoriteUpdated);
  loadFavoriteStatus();
  syncProductData(); // <-- Bổ sung thêm dòng này
});

onBeforeUnmount(() => {
  window.removeEventListener("favorite-updated", handleFavoriteUpdated);
  if (toastTimer) window.clearTimeout(toastTimer);
});

watch(
  () => props.product,
  () => {
    loadFavoriteStatus();
  },
  { deep: true },
);
</script>

<style scoped>
.product-card {
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(26, 26, 26, 0.055);
  box-shadow: 0 8px 28px rgba(5, 16, 36, 0.045);
  transition: all 0.28s ease;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 22px 48px rgba(5, 16, 36, 0.105);
}

.discount-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 3;
  background: #b31320;
  color: #ffffff;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.btn-favorite {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 5;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid rgba(189, 154, 95, 0.35);
  background: rgba(255, 255, 255, 0.94);
  color: #8c8c8c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: 0 8px 18px rgba(5, 16, 36, 0.08);
}

.btn-favorite:hover:not(:disabled) {
  color: #dc2626;
  border-color: #dc2626;
  transform: scale(1.05);
}

.btn-favorite.active {
  color: #dc2626;
  border-color: #dc2626;
  background: #fff5f5;
}

.btn-favorite:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.product-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  background-color: #f9fafb;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-real-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transition: transform 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.product-card:hover .product-real-image {
  transform: scale(1.08);
}

.product-bottle {
  width: 120px;
  height: 190px;
  display: flex;
  flex-direction: column;
  align-items: center;
  align-self: center;
  filter: drop-shadow(0 18px 24px rgba(5, 16, 36, 0.18));
  transition: transform 0.28s ease;
}

.product-card:hover .product-bottle {
  transform: scale(1.05);
}

.product-bottle-cap {
  width: 58px;
  height: 34px;
  border-radius: 15px 15px 6px 6px;
  background: linear-gradient(135deg, #f1d08a, #9b6f2e);
}

.product-bottle-neck {
  width: 32px;
  height: 22px;
  background: linear-gradient(135deg, #d2ad68, #8b642c);
}

.product-bottle-body {
  width: 120px;
  height: 134px;
  border-radius: 16px 16px 24px 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.32), transparent 32%),
    linear-gradient(145deg, var(--bottle-color), #080808 86%);
  border: 2px solid rgba(255, 255, 255, 0.24);
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-bottle-label {
  width: 82px;
  height: 70px;
  border: 1px solid var(--aura-gold);
  background: rgba(5, 16, 36, 0.88);
  color: var(--aura-gold);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.product-bottle-label strong {
  font-family: var(--aura-serif);
  font-size: 14px;
  letter-spacing: 1.5px;
  line-height: 1;
}

.product-bottle-label span {
  margin-top: 5px;
  font-size: 7px;
  letter-spacing: 2px;
  font-family: var(--aura-sans);
}

.product-content {
  padding: 18px 20px 20px;
}

.product-brand {
  color: #8c8c8c;
  font-family: var(--aura-sans);
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  font-weight: 700;
}

.product-name {
  font-family: var(--aura-serif);
  font-size: 17px;
  font-weight: 700;
  color: var(--aura-black);
  line-height: 1.28;
  letter-spacing: 0;
}

.stars {
  color: var(--aura-gold);
  font-size: 13px;
  letter-spacing: 1px;
}

.review-count {
  color: #777777;
  font-size: 12px;
  font-weight: 500;
}

.price-prefix {
  color: #718096;
  font-size: 12px;
  font-weight: 700;
  align-self: center;
}

.sale-price {
  color: #111111;
  font-size: 18px;
  font-weight: 800;
  font-family: var(--aura-sans);
}

.original-price {
  color: #a8a8a8;
  font-size: 13px;
  font-weight: 500;
}

.product-actions {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 10px;
  width: 100%;
}

.buy-now-btn,
.add-cart-btn {
  font-size: 13px;
  font-weight: 800;
  border-radius: 7px;
  padding: 10px 10px;
  transition: all 0.22s ease;
  min-height: 42px;
}

.buy-now-btn {
  border: none;
  background: var(--aura-gold);
  color: #ffffff;
}

.buy-now-btn:hover:not(:disabled) {
  background: #a3824d;
  color: #ffffff;
}

.add-cart-btn {
  border: 1px solid var(--aura-gold);
  color: var(--aura-gold);
  background: #ffffff;
}

.add-cart-btn:hover:not(:disabled) {
  background: var(--aura-gold);
  color: #ffffff;
}

.buy-now-btn:disabled,
.add-cart-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>

<style>
.custom-cart-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background-color: #0b1120;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 999999; /* ĐÃ SỬA: Thêm 1 số 9 vào đây để nó luôn nổi lên trên cùng */
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  max-width: 520px;
}

.custom-cart-toast.success .toast-icon {
  background-color: rgba(34, 197, 94, 0.16);
  color: #86efac;
}

.custom-cart-toast.warning .toast-icon {
  background-color: rgba(245, 158, 11, 0.16);
  color: #fbbf24;
}

.custom-cart-toast.error .toast-icon {
  background-color: rgba(239, 68, 68, 0.16);
  color: #fca5a5;
}

.toast-icon {
  width: 34px;
  height: 34px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  flex-shrink: 0;
}

.toast-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
}

.toast-info p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.toast-view-cart {
  margin-left: 12px;
  color: #d2ad68;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
  display: flex;
  align-items: center;
  transition: color 0.25s ease;
  white-space: nowrap;
}

.toast-view-cart:hover {
  color: #f1d08a;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  transform: translateX(120%);
  opacity: 0;
}

.custom-modal-overlay {
  backdrop-filter: blur(5px);
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.variant-modal-box {
  background: #ffffff;
  width: 100%;
  max-width: 440px;
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 24px 54px rgba(6, 19, 43, 0.25);
  animation: modalFadeIn 0.3s ease-out forwards;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.vm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(189, 154, 95, 0.15);
  padding-bottom: 16px;
}

.vm-header h5 {
  margin: 0;
  font-family: "Playfair Display", serif;
  font-weight: 800;
  color: #06132b;
  font-size: 20px;
  letter-spacing: -0.5px;
}

.vm-close {
  background: transparent;
  border: none;
  font-size: 20px;
  color: #a0aec0;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 4px;
  border-radius: 50%;
}

.vm-close:hover {
  color: #e53e3e;
  background: #fff5f5;
  transform: rotate(90deg);
}

.vm-product-info {
  display: flex;
  gap: 18px;
  margin-bottom: 28px;
  align-items: center;
}

.vm-img-box {
  width: 82px;
  height: 82px;
  border-radius: 14px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid rgba(6, 19, 43, 0.08);
  flex-shrink: 0;
  padding: 6px;
}

.vm-img-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.vm-details h6 {
  margin: 0 0 6px 0;
  font-weight: 800;
  font-size: 16px;
  color: #06132b;
  font-family: "Playfair Display", serif;
  line-height: 1.3;
}

.vm-price {
  margin: 0;
  font-weight: 800;
  color: #b78d52;
  font-size: 18px;
}

.vm-label {
  font-weight: 700;
  font-size: 13px;
  color: #4a5568;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.vm-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}

.vm-variant-btn {
  background: #ffffff;
  border: 1px solid #cbd5e0;
  border-radius: 12px;
  padding: 12px 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.vm-variant-btn:hover:not(.disabled) {
  border-color: #b78d52;
  background: #fffcf7;
  transform: translateY(-2px);
}

.vm-variant-btn.selected {
  border-color: #b78d52;
  background: #fffcf7;
  border-width: 2px;
  box-shadow: 0 6px 14px rgba(183, 141, 82, 0.15);
  padding: 11px 5px;
}

.vm-v-name {
  font-size: 14px;
  font-weight: 800;
  color: #06132b;
}

.vm-variant-btn.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #f1f5f9;
  border-color: #e2e8f0;
}

.vm-variant-btn.disabled .vm-v-name {
  text-decoration: line-through;
  color: #a0aec0;
}

.vm-confirm-btn {
  width: 100%;
  background: #b78d52;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
  transition: all 0.25s ease;
  text-transform: uppercase;
}

.vm-confirm-btn:hover:not(:disabled) {
  background: #9b7541;
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(183, 141, 82, 0.35);
}

.vm-confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.flash-sale-badge {
  background: #b31320;
  color: #ffffff;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 2px;
}

.variant-divider {
  border: 0;
  border-top: 1px dashed #cbd5e0;
  margin: 20px 0 16px 0;
}

.quantity-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qty-wrapper {
  display: inline-flex;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  overflow: hidden;
}

.qty-wrapper button {
  width: 32px;
  height: 32px;
  background: #ffffff;
  border: none;
  cursor: pointer;
  color: #06132b;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}

.qty-wrapper button:hover:not(:disabled) {
  background: #f1f5f9;
}

.qty-wrapper button:disabled {
  color: #cbd5e0;
  cursor: not-allowed;
}

.qty-wrapper input {
  width: 44px;
  text-align: center;
  border: none;
  font-size: 15px;
  font-weight: 700;
  outline: none;
  border-left: 1px solid #cbd5e0;
  border-right: 1px solid #cbd5e0;
  color: #06132b;
}

.stock-info {
  font-size: 13px;
  color: #718096;
}

/* Ẩn mũi tên tăng giảm mặc định của trình duyệt */
.qty-wrapper input[type="number"]::-webkit-inner-spin-button,
.qty-wrapper input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.qty-wrapper input[type="number"] {
  appearance: textfield;
  -moz-appearance: textfield;
}

.qty-wrapper input[type="number"]::-webkit-inner-spin-button,
.qty-wrapper input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
</style>
