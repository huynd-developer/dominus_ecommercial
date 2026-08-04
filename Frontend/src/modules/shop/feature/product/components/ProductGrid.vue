<template>
  <div class="product-grid-wrapper">
    <div v-if="productList.length === 0" class="empty-product">
      Không tìm thấy sản phẩm phù hợp.
    </div>

    <div v-else class="product-grid">
      <div
        v-for="item in productList"
        :key="item.id"
        class="product-card luxury-card"
        @click="emit('open-detail', item)"
      >
        <div class="card-img-wrapper">
          <div
            v-if="getDiscountPercent(item) > 0"
            class="sale-badge"
          >
            -{{ getDiscountPercent(item) }}%
          </div>

          <img
            :src="getProductImage(item)"
            :alt="item?.name || 'Sản phẩm'"
            @error="handleImageError"
          />

          <button
            class="btn-heart-small"
            type="button"
            :class="{ active: isFavorited(item) }"
            :disabled="isFavoriteLoading(item)"
            @click.stop="toggleFavorite(item)"
            :title="isFavorited(item) ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
          >
            <span
              v-if="isFavoriteLoading(item)"
              class="spinner-border spinner-border-sm"
            ></span>

            <svg
              v-else
              viewBox="0 0 24 24"
              :fill="isFavorited(item) ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.7"
            >
              <path
                d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"
              />
            </svg>
          </button>
        </div>

        <div class="card-info">
          <div class="card-brand">
            {{ getBrandName(item) }}
          </div>

          <h3 class="card-name">
            {{ item?.name || "Tên sản phẩm" }}
          </h3>

          <div class="card-rating">
            <span class="stars">{{ getStarsDisplay(item) }}</span>

            <span class="score">
              {{ getRatingScore(item) }} | {{ getReviewCount(item) }} đánh giá
            </span>
          </div>

          <div class="card-price-box">
            <span class="card-price">{{ formatPrice(item) }}</span>

            <span
              v-if="getOldPrice(item) > getVariantPrice(item)"
              class="card-old-price"
            >
              {{ formatCurrency(getOldPrice(item)) }}
            </span>
          </div>

          <div class="card-actions">
            <!-- ĐÃ SỬA: Đổi hàm handleBuyNow thành openVariantModal -->
            <button
              type="button"
              class="btn-buy-now-small"
              :disabled="isBuyNowDisabled(item)"
              @click.stop="openVariantModal(item)"
            >
              Mua ngay
            </button>

            <button
              type="button"
              class="btn-view-detail"
              @click.stop="emit('open-detail', item)"
            >
              Chi tiết
            </button>
          </div>

          <div
            v-if="getVariantStock(item) <= 0"
            class="card-stock-warning"
          >
            Tạm hết hàng
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL MUA NHANH CHỌN BIẾN THỂ -->
    <Teleport to="body">
      <div v-if="showVariantModal" class="custom-modal-overlay" @click.self="showVariantModal = false">
        <div class="variant-modal-box">
          <div class="vm-header">
            <h5>Chọn Phân Loại</h5>
            <button class="vm-close" @click="showVariantModal = false">
              <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>

          <div class="vm-product-info">
            <div class="vm-img-box">
              <img :src="modalImage" alt="Product Image" @error="handleImageError" />
            </div>
            <div class="vm-details">
              <h6>{{ modalProduct?.name }}</h6>
              <div class="d-flex align-items-end gap-2 flex-wrap">
                <p class="vm-price mb-0">
                  {{ formatCurrency(selectedVariant ? (selectedVariant.salePrice || selectedVariant.price) : getVariantPrice(modalProduct)) }}
                </p>
                <span
                  v-if="selectedVariant && selectedVariant.salePrice && selectedVariant.salePrice < selectedVariant.originalPrice"
                  class="text-decoration-line-through text-muted small"
                >
                  {{ formatCurrency(selectedVariant.originalPrice) }}
                </span>
                
                <!-- BỔ SUNG BADGE FLASH SALE -->
                <span v-if="calculatedDiscountPercent > 0" class="flash-sale-badge">
                  -{{ calculatedDiscountPercent }}%
                </span>
              </div>
            </div>
          </div>

          <div class="vm-variants">
            <p class="vm-label">TÙY CHỌN PHÂN LOẠI:</p>
            <div v-if="isLoadingVariants" class="text-center py-4">
              <span class="spinner-border spinner-border-sm me-2" style="color: #b78d52"></span>
              <span style="color: #718096; font-size: 13px">Đang tải thông tin...</span>
            </div>
            <div v-else class="vm-grid">
              <button
                v-for="v in fullVariants"
                :key="v.productVariantId || v.id"
                class="vm-variant-btn"
                :class="{
                  selected: (selectedVariant?.productVariantId || selectedVariant?.id) === (v.productVariantId || v.id),
                  disabled: Number(v.stockQuantity || v.stock || 0) <= 0,
                }"
                @click="selectedVariant = v; quantity = 1;"
              >
                <span class="vm-v-name">{{ v.displayCapacity }}</span>
              </button>
            </div>

            <hr class="variant-divider" v-if="selectedVariant" />

            <div class="quantity-section" v-if="selectedVariant">
              <p class="vm-label mb-0">SỐ LƯỢNG:</p>
              <div class="quantity-control">
                <div class="qty-wrapper">
                  <button type="button" @click="quantity > 1 ? quantity-- : null" :disabled="quantity <= 1">−</button>
                  <input type="number" v-model="quantity" @input="validateQuantity" @blur="validateQuantity" @keyup.enter="validateQuantity" />
                  <button type="button" @click="increaseQuantity">+</button>
                </div>
                <span class="stock-info"> Kho: {{ maxQuantity }} </span>
              </div>
            </div>
          </div>

          <!-- ĐÃ SỬA: CHIA 2 NÚT THÊM VÀO GIỎ VÀ MUA NGAY -->
          <div class="vm-actions d-flex gap-2 mt-3">
            <button class="vm-btn-cart flex-grow-1" :disabled="!selectedVariant || actionLoading || isLoadingVariants" @click="confirmAction('CART')">
              <span v-if="actionLoading && actionType === 'CART'" class="spinner-border spinner-border-sm me-2"></span>
              THÊM VÀO GIỎ
            </button>
            <button class="vm-btn-buy flex-grow-1" :disabled="!selectedVariant || actionLoading || isLoadingVariants" @click="confirmAction('BUY')">
              <span v-if="actionLoading && actionType === 'BUY'" class="spinner-border spinner-border-sm me-2"></span>
              MUA NGAY
            </button>
          </div>

        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, computed } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";
import { favoriteService } from "../services/favorite.service";

const props = defineProps<{
  productList: any[];
}>();

const emit = defineEmits<{
  (e: "open-detail", item: any): void;
}>();

const router = useRouter();

const favoritedMap = ref<Record<number, boolean>>({});
const favoriteLoadingMap = ref<Record<number, boolean>>({});

const BACKEND_URL = "http://localhost:8080";

const getToken = () => {
  return localStorage.getItem("token");
};

const getCurrentRole = () => {
  return String(localStorage.getItem("role") || localStorage.getItem("userRole") || "")
    .replace("ROLE_", "")
    .toUpperCase()
    .trim();
};

const hasToken = () => {
  return Boolean(getToken());
};

const isCustomerLoggedIn = () => {
  return hasToken() && getCurrentRole() === "USER";
};

const getBrandName = (item: any) => {
  if (typeof item?.brand === "object") {
    return item?.brand?.name || "Premium";
  }

  return item?.brandName || item?.brand || "Premium";
};

const DEFAULT_RATING = 5;

// Thêm 2 state để lưu data đồng bộ ngầm cho danh sách
const syncedRatingsMap = ref<Record<number, number>>({});
const syncedReviewsMap = ref<Record<number, number>>({});

const getReviewCount = (item: any) => {
  const id = Number(item?.id || item?.productId || 0);
  // Nếu đã kéo được data thật thì lấy ra dùng
  if (id > 0 && syncedReviewsMap.value[id] !== undefined) {
    return syncedReviewsMap.value[id];
  }
  return Number(item?.reviewCount || item?.reviews || item?.totalReviews || 0);
};

const getRatingValue = (item: any) => {
  const id = Number(item?.id || item?.productId || 0);
  let raw = 0;
  
  // Ưu tiên data đồng bộ ngầm
  if (id > 0 && syncedRatingsMap.value[id] !== undefined) {
    raw = syncedRatingsMap.value[id];
  } else {
    raw = Number(item?.averageRating || item?.avgRating || item?.rating || 0);
  }
  
  const reviews = getReviewCount(item);
  if (reviews > 0 || raw > 0) {
    return Math.min(5, Math.max(0, raw));
  }
  return DEFAULT_RATING; // Mặc định 5 sao nếu trắng trơn
};

const getRatingScore = (item: any) => {
  return getRatingValue(item).toFixed(1);
};

const getStarsDisplay = (item: any) => {
  const rounded = Math.round(getRatingValue(item));
  const filled = Math.max(0, Math.min(5, rounded));
  return "★".repeat(filled) + "☆".repeat(5 - filled);
};

// Hàm chạy ngầm kéo data thật cho những ông nào bị báo 0 đánh giá
const syncGridRatings = () => {
  if (!props.productList || !Array.isArray(props.productList)) return;
  
  props.productList.forEach(async (item) => {
    const id = Number(item?.id || item?.productId || 0);
    const currentCount = Number(item?.reviewCount || item?.reviews || item?.totalReviews || 0);
    
    if (id > 0 && currentCount === 0 && syncedReviewsMap.value[id] === undefined) {
      try {
        const res = await api.get(`/v1/products/${id}`);
        const data = res.data?.data || res.data;
        if (data) {
          syncedRatingsMap.value[id] = Number(data.averageRating || data.avgRating || data.rating || 0);
          syncedReviewsMap.value[id] = Number(data.reviewCount || data.reviews || data.totalReviews || 0);
        }
      } catch (e) {
        // Lỗi thì âm thầm bỏ qua
      }
    }
  });
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

const getProductImages = (item: any) => {
  const images: string[] = [];

  appendImage(images, item?.mainImage);
  appendImage(images, item?.mainImageUrl);
  appendImage(images, item?.thumbnailUrl);
  appendImage(images, item?.imageUrl);
  appendImage(images, item?.ImageUrl);
  appendImage(images, item?.image);

  appendImageList(images, item?.images);
  appendImageList(images, item?.Images);
  appendImageList(images, item?.galleryImages);
  appendImageList(images, item?.imageList);
  appendImageList(images, item?.ImageList);
  appendImageList(images, item?.productImages);
  appendImageList(images, item?.ProductImages);
  appendImageList(images, item?.productImageList);
  appendImageList(images, item?.ProductImageList);

  if (Array.isArray(item?.variants)) {
    item.variants.forEach((variant: any) => {
      appendImage(images, variant?.mainImage);
      appendImage(images, variant?.mainImageUrl);
      appendImage(images, variant?.thumbnailUrl);
      appendImage(images, variant?.imageUrl);
      appendImage(images, variant?.ImageUrl);
      appendImage(images, variant?.image);

      appendImageList(images, variant?.images);
      appendImageList(images, variant?.Images);
      appendImageList(images, variant?.productImages);
      appendImageList(images, variant?.ProductImages);
    });
  }

  return images;
};

const getProductImage = (item: any) => {
  return getProductImages(item)[0] || getPlaceholderImage();
};

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;

  if (!target) {
    return;
  }

  target.onerror = null;
  target.src = getPlaceholderImage();
};

const normalizeStock = (variant: any) => {
  return Number(
    variant?.stockQuantity ??
      variant?.stock ??
      variant?.availableQuantity ??
      variant?.quantity ??
      0
  );
};

const normalizePrice = (variant: any) => {
  return Number(variant?.price ?? variant?.Price ?? 0);
};

const getPrimaryVariant = (item: any) => {
  if (!item) {
    return null;
  }

  if (Array.isArray(item?.variants) && item.variants.length > 0) {
    return (
      item.variants.find((variant: any) => {
        const stock = normalizeStock(variant);
        const price = normalizePrice(variant);
        const status = Number(variant?.status ?? 1);

        return status === 1 && stock > 0 && price > 0;
      }) || item.variants[0]
    );
  }

  return item;
};

const getPrimaryVariantId = (item: any) => {
  const variant = getPrimaryVariant(item);

  if (!variant) {
    return 0;
  }

  return Number(
    variant?.productVariantId ??
      variant?.variantId ??
      variant?.id ??
      variant?.Id ??
      item?.productVariantId ??
      item?.variantId ??
      0
  );
};

const getVariantStock = (item: any) => {
  const variant = getPrimaryVariant(item);
  return normalizeStock(variant);
};

const getVariantPrice = (item: any) => {
  const variant = getPrimaryVariant(item);

  if (variant?.price != null || variant?.Price != null) {
    return normalizePrice(variant);
  }

  return Number(item?.salePrice ?? item?.price ?? item?.Price ?? 0);
};

const getOldPrice = (item: any) => {
  return Number(item?.oldPrice ?? item?.originalPrice ?? 0);
};

const getDiscountPercent = (item: any) => {
  return Number(item?.discountPercent ?? item?.discount ?? 0);
};

const isBuyNowDisabled = (item: any) => {
  const variantId = getPrimaryVariantId(item);
  const price = getVariantPrice(item);
  const stock = getVariantStock(item);

  return !variantId || price <= 0 || stock <= 0;
};

const isFavorited = (item: any) => {
  const variantId = getPrimaryVariantId(item);

  if (!variantId) {
    return false;
  }

  return Boolean(favoritedMap.value[variantId]);
};

const isFavoriteLoading = (item: any) => {
  const variantId = getPrimaryVariantId(item);

  if (!variantId) {
    return false;
  }

  return Boolean(favoriteLoadingMap.value[variantId]);
};

const setFavoriteLoading = (variantId: number, value: boolean) => {
  favoriteLoadingMap.value = {
    ...favoriteLoadingMap.value,
    [variantId]: value,
  };
};

const setFavorited = (variantId: number, value: boolean) => {
  favoritedMap.value = {
    ...favoritedMap.value,
    [variantId]: value,
  };
};

const askLogin = async (
  message = "Vui lòng đăng nhập để sử dụng chức năng này."
) => {
  const result = await Swal.fire({
    icon: "info",
    title: "Bạn chưa đăng nhập",
    text: message,
    showCancelButton: true,
    confirmButtonText: "Đăng nhập ngay",
    cancelButtonText: "Ở lại xem tiếp",
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#6b7280",
  });

  if (result.isConfirmed) {
    router.push({
      name: "Login",
      query: {
        redirect: router.currentRoute.value.fullPath,
      },
    });
  }
};

const loadMyFavorites = async () => {
  if (!isCustomerLoggedIn()) {
    favoritedMap.value = {};
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
  } catch (error) {
    console.error("Lỗi tải danh sách yêu thích:", error);
    favoritedMap.value = {};
  }
};

const toggleFavorite = async (item: any) => {
  const variantId = getPrimaryVariantId(item);

  if (!variantId || Number.isNaN(variantId)) {
    await Swal.fire({
      icon: "warning",
      title: "Không xác định được biến thể",
      text: "Sản phẩm này chưa có biến thể hợp lệ để thêm vào yêu thích.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để thêm sản phẩm vào danh sách yêu thích.");
    return;
  }

  if (!isCustomerLoggedIn()) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể sử dụng chức năng này",
      text: "Chỉ tài khoản khách hàng mới được thêm sản phẩm yêu thích.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  try {
    setFavoriteLoading(variantId, true);

    const res = await favoriteService.toggleFavorite(variantId);
    const favorited = Boolean(res.data?.favorited);

    setFavorited(variantId, favorited);

    window.dispatchEvent(
      new CustomEvent("favorite-updated", {
        detail: {
          productVariantId: variantId,
          favorited,
        },
      })
    );

    await Swal.fire({
      toast: true,
      position: "top-end",
      icon: favorited ? "success" : "info",
      title:
        res.data?.message ||
        (favorited ? "Đã thêm vào yêu thích" : "Đã bỏ yêu thích"),
      showConfirmButton: false,
      timer: 1400,
      timerProgressBar: true,
    });
  } catch (error: any) {
    console.error("Lỗi xử lý yêu thích:", error);

    await Swal.fire({
      icon: "error",
      title: "Không thể xử lý yêu thích",
      text:
        error?.response?.data?.message ||
        error?.response?.data ||
        "Vui lòng thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    setFavoriteLoading(variantId, false);
  }
};

// --- LOGIC MODAL MUA NHANH ---
const showVariantModal = ref(false);
const modalProduct = ref<any>(null);
const isLoadingVariants = ref(false);
const selectedVariant = ref<any>(null);
const fullVariants = ref<any[]>([]);
const quantity = ref(1);
const actionLoading = ref(false);
const actionType = ref('');

const modalImage = computed(() => {
  if (selectedVariant.value) {
    const variantImages = getProductImages(selectedVariant.value);
    if (variantImages[0]) return variantImages[0];
  }
  return modalProduct.value ? getProductImage(modalProduct.value) : getPlaceholderImage();
});

const maxQuantity = computed(() => {
  return selectedVariant.value
    ? Number(selectedVariant.value.stockQuantity || selectedVariant.value.stock || 0)
    : 0;
});

const calculatedDiscountPercent = computed(() => {
  if (selectedVariant.value) {
    const original = selectedVariant.value.originalPrice || selectedVariant.value.oldPrice || selectedVariant.value.price;
    const sale = selectedVariant.value.salePrice || selectedVariant.value.price;
    if (original && sale && original > sale) {
      return Math.round(((original - sale) / original) * 100);
    }
    return 0;
  }
  return modalProduct.value ? getDiscountPercent(modalProduct.value) : 0;
});

const validateQuantity = () => {
  let val = Number(quantity.value);
  if (Number.isNaN(val) || val < 1) {
    quantity.value = 1;
  } else if (val > 10) {
    quantity.value = 10;
    Swal.fire({ toast: true, position: 'top-end', icon: 'warning', title: 'Chỉ được mua tối đa 10 sản phẩm!', showConfirmButton: false, timer: 2000 });
  } else if (val > maxQuantity.value) {
    quantity.value = maxQuantity.value;
    Swal.fire({ toast: true, position: 'top-end', icon: 'warning', title: `Chỉ còn ${maxQuantity.value} trong kho!`, showConfirmButton: false, timer: 2000 });
  } else {
    quantity.value = Math.floor(val);
  }
};

const increaseQuantity = () => {
  if (quantity.value >= 10) {
    Swal.fire({ toast: true, position: 'top-end', icon: 'warning', title: 'Chỉ được mua tối đa 10 sản phẩm!', showConfirmButton: false, timer: 2000 });
    return;
  }
  if (quantity.value >= maxQuantity.value) {
    Swal.fire({ toast: true, position: 'top-end', icon: 'warning', title: `Chỉ còn ${maxQuantity.value} trong kho!`, showConfirmButton: false, timer: 2000 });
    return;
  }
  quantity.value++;
};

const openVariantModal = async (item: any) => {
  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để mua sản phẩm.");
    return;
  }
  if (!isCustomerLoggedIn()) {
    await Swal.fire({ icon: "warning", title: "Từ chối thao tác", text: "Chỉ tài khoản khách hàng mới được mua hàng.", confirmButtonColor: "#bd9a5f" });
    return;
  }

  modalProduct.value = item;
  selectedVariant.value = null;
  fullVariants.value = [];
  quantity.value = 1;
  showVariantModal.value = true;
  isLoadingVariants.value = true;

  try {
    // ÉP GIÁ FLASH SALE TỪ CARD VÀO MODAL
    const flashSalePriceMap = new Map<number, number>();
    const originalPriceMap = new Map<number, number>();
    if (item?.variants && Array.isArray(item.variants)) {
      item.variants.forEach((pv: any) => {
        const vId = Number(pv.productVariantId || pv.variantId || pv.id);
        if (vId) {
          if (pv.salePrice != null) flashSalePriceMap.set(vId, Number(pv.salePrice));
          if (pv.originalPrice != null || pv.oldPrice != null || pv.price != null) {
            originalPriceMap.set(vId, Number(pv.originalPrice || pv.oldPrice || pv.price));
          }
        }
      });
    }

    const res = await api.get(`/v1/products/${item.id || item.productId}`);
    const data = res.data?.data || res.data;
    let rawVariants = data?.variants || data?.productVariants || data?.productVariantList || item.variants || [item];

    const processedVariants = rawVariants.map((v: any) => {
      const vId = Number(v.productVariantId || v.variantId || v.id);
      let cap = v.capacityName || v.capacityValue || v.volume || v.capacity;
      if (typeof cap === "object") cap = cap?.value ?? cap?.name;

      let numericCap = parseFloat(String(cap).replace("ml", "")) || 0;
      let displayCap = numericCap > 0 ? `${numericCap}ml` : String(cap || "");

      const bottle = v.bottleTypeName || v.bottleType;
      const bottleName = typeof bottle === "object" ? bottle?.name : bottle;

      if (displayCap && bottleName) displayCap = `${displayCap} - ${bottleName}`;
      else if (bottleName) displayCap = bottleName;
      else if (!displayCap) displayCap = "Loại " + (vId || "");

      // Lấy giá trị đã map hoặc dự phòng lấy giá của v
      const mappedSalePrice = flashSalePriceMap.get(vId) ?? v.salePrice ?? v.promotionPrice ?? v.price;
      const mappedOriginalPrice = originalPriceMap.get(vId) ?? v.originalPrice ?? v.price;

      return {
        ...v,
        productVariantId: vId,
        id: vId,
        originalPrice: mappedOriginalPrice,
        salePrice: mappedSalePrice,
        displayCapacity: displayCap,
        numericCapacity: numericCap,
      };
    });

    processedVariants.sort((a: any, b: any) => a.numericCapacity - b.numericCapacity);
    fullVariants.value = processedVariants;

    if (fullVariants.value.length > 0) {
      const primaryId = getPrimaryVariantId(item);
      selectedVariant.value = fullVariants.value.find((v: any) => v.productVariantId === primaryId) || fullVariants.value[0];
    }
  } catch (error) {
    console.error("Lỗi lấy biến thể:", error);
    fullVariants.value = (item.variants || [item]).map((v: any) => ({
      ...v,
      displayCapacity: "Phân loại",
      numericCapacity: 0
    }));
    selectedVariant.value = fullVariants.value[0];
  } finally {
    isLoadingVariants.value = false;
  }
};

const confirmAction = async (type: 'CART' | 'BUY') => {
  if (!selectedVariant.value || !modalProduct.value) return;
  const variantId = Number(selectedVariant.value.productVariantId || selectedVariant.value.variantId || selectedVariant.value.id || modalProduct.value.id);

  try {
    actionType.value = type;
    actionLoading.value = true;
    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: quantity.value,
    });
    window.dispatchEvent(new Event("cart-updated"));
    showVariantModal.value = false;
    
    if (type === 'BUY') {
      router.push({ name: "Checkout" });
    } else {
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Thêm vào giỏ thành công', showConfirmButton: false, timer: 1500 });
    }
  } catch (error: any) {
    Swal.fire({ icon: 'error', title: 'Lỗi', text: error?.response?.data?.message || 'Không thể thực hiện.', confirmButtonColor: '#bd9a5f' });
  } finally {
    actionLoading.value = false;
    actionType.value = '';
  }
};

const handleFavoriteUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{
    productVariantId?: number;
    favorited?: boolean;
  }>;

  const variantId = Number(customEvent.detail?.productVariantId || 0);

  if (!variantId) {
    return;
  }

  setFavorited(variantId, Boolean(customEvent.detail?.favorited));
};

const formatCurrency = (value: number) => {
  if (value == null || Number.isNaN(Number(value)) || Number(value) <= 0) {
    return "Liên hệ";
  }

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value));
};

const formatPrice = (item: any) => {
  return formatCurrency(getVariantPrice(item));
};

onMounted(() => {
  window.addEventListener("favorite-updated", handleFavoriteUpdated);
  loadMyFavorites();
  syncGridRatings(); // <--- Bổ sung dòng này
});

onBeforeUnmount(() => {
  window.removeEventListener("favorite-updated", handleFavoriteUpdated);
});

watch(
  () => props.productList,
  () => {
    loadMyFavorites();
    syncGridRatings(); // <--- Bổ sung thêm dòng này nữa
  },
  {
    deep: true,
  }
);
</script>

<style scoped>
.product-grid-wrapper {
  width: 100%;
}

.empty-product {
  padding: 60px 20px;
  text-align: center;
  color: #718096;
  background: #f9fafb;
  border-radius: 14px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
}

.product-card.luxury-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid #eaeaea;
}

.product-card.luxury-card:hover {
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
}

.card-img-wrapper {
  position: relative;
  background: #f8f9fa;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  overflow: hidden;
  aspect-ratio: 1 / 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: 0.4s ease;
}

.product-card.luxury-card:hover .card-img-wrapper img {
  transform: scale(1.06);
}

.btn-heart-small {
  position: absolute;
  top: 15px;
  right: 15px;
  background: white;
  border: 1px solid #eaeaea;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #a0aec0;
  transition: 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  z-index: 2;
}

.btn-heart-small:hover:not(:disabled) {
  color: #e53e3e;
  border-color: #e53e3e;
}

.btn-heart-small.active {
  color: #e53e3e;
  border-color: #e53e3e;
  background: #fff5f5;
}

.btn-heart-small:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-heart-small svg {
  width: 18px;
  height: 18px;
}

.sale-badge {
  position: absolute;
  top: 15px;
  left: 15px;
  background: #e53e3e;
  color: white;
  font-size: 11px;
  font-weight: bold;
  padding: 4px 8px;
  border-radius: 4px;
  z-index: 2;
}

.card-info {
  padding: 18px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.card-brand {
  font-size: 12px;
  font-weight: 700;
  color: #bd9a5f;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.card-name {
  font-size: 17px;
  color: #0a142f;
  font-weight: 700;
  min-height: 44px;
  line-height: 1.3;
  margin: 0 0 10px;
}

.card-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.stars {
  color: #bd9a5f;
  font-size: 12px;
}

.score {
  color: #718096;
  font-size: 13px;
}

.card-price-box {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
}

.card-price {
  color: #0a142f;
  font-weight: 800;
  font-size: 17px;
}

.card-old-price {
  color: #a0aec0;
  font-size: 13px;
  text-decoration: line-through;
}

.card-actions {
  margin-top: auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.btn-buy-now-small,
.btn-view-detail {
  border-radius: 8px;
  padding: 9px 10px;
  font-size: 13px;
  font-weight: 700;
  transition: 0.2s;
}

.btn-buy-now-small {
  border: none;
  background: #bd9a5f;
  color: #ffffff;
}

.btn-buy-now-small:hover:not(:disabled) {
  background: #a3824d;
}

.btn-buy-now-small:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-view-detail {
  border: 1px solid #0a142f;
  background: #ffffff;
  color: #0a142f;
}

.btn-view-detail:hover {
  background: #0a142f;
  color: #ffffff;
}

.card-stock-warning {
  margin-top: 10px;
  color: #dc2626;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1199px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 575px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}

/* --- CSS MODAL MUA NHANH --- */
.custom-modal-overlay { backdrop-filter: blur(5px); position: fixed; inset: 0; background: rgba(0, 0, 0, 0.6); z-index: 999999; display: flex; align-items: center; justify-content: center; }
.variant-modal-box { background: #ffffff; width: 100%; max-width: 440px; border-radius: 20px; padding: 28px; box-shadow: 0 24px 54px rgba(6, 19, 43, 0.25); animation: modalFadeIn 0.3s ease-out forwards; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(20px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
.vm-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-bottom: 1px solid rgba(189, 154, 95, 0.15); padding-bottom: 16px; }
.vm-header h5 { margin: 0; font-family: "Playfair Display", serif; font-weight: 800; color: #06132b; font-size: 20px; letter-spacing: -0.5px; }
.vm-close { background: transparent; border: none; padding: 4px; color: #a0aec0; cursor: pointer; transition: 0.2s; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.vm-close:hover { color: #e53e3e; background: #fff5f5; transform: rotate(90deg); }
.vm-product-info { display: flex; gap: 18px; margin-bottom: 28px; align-items: center; }
.vm-img-box { width: 82px; height: 82px; border-radius: 14px; background: #f8fafc; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid rgba(6, 19, 43, 0.08); flex-shrink: 0; padding: 6px; }
.vm-img-box img { width: 100%; height: 100%; object-fit: contain; }
.vm-details h6 { margin: 0 0 6px 0; font-weight: 800; font-size: 16px; color: #06132b; font-family: "Playfair Display", serif; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.vm-price { margin: 0; font-weight: 800; color: #b78d52; font-size: 18px; }
.vm-label { font-weight: 700; font-size: 13px; color: #4a5568; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.vm-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 28px; }
.vm-variant-btn { background: #ffffff; border: 1px solid #cbd5e0; border-radius: 12px; padding: 12px 6px; text-align: center; cursor: pointer; transition: all 0.25s ease; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; }
.vm-variant-btn:hover:not(.disabled) { border-color: #b78d52; background: #fffcf7; transform: translateY(-2px); }
.vm-variant-btn.selected { border-color: #b78d52; background: #fffcf7; border-width: 2px; box-shadow: 0 6px 14px rgba(183, 141, 82, 0.15); padding: 11px 5px; }
.vm-v-name { font-size: 14px; font-weight: 800; color: #06132b; }
.vm-variant-btn.disabled { opacity: 0.45; cursor: not-allowed; background: #f1f5f9; border-color: #e2e8f0; }
.vm-variant-btn.disabled .vm-v-name { text-decoration: line-through; color: #a0aec0; }
.variant-divider { border: 0; border-top: 1px dashed #cbd5e0; margin: 20px 0 16px 0; }
.quantity-section { display: flex; align-items: center; gap: 24px; margin-bottom: 24px; }
.quantity-control { display: flex; align-items: center; gap: 12px; }
.qty-wrapper { display: inline-flex; border: 1px solid #cbd5e0; border-radius: 6px; overflow: hidden; }
.qty-wrapper button { width: 32px; height: 32px; background: #ffffff; border: none; cursor: pointer; color: #06132b; font-size: 18px; display: flex; align-items: center; justify-content: center; transition: 0.2s; }
.qty-wrapper button:hover:not(:disabled) { background: #f1f5f9; }
.qty-wrapper button:disabled { color: #cbd5e0; cursor: not-allowed; }
.qty-wrapper input { width: 44px; text-align: center; border: none; font-size: 15px; font-weight: 700; outline: none; border-left: 1px solid #cbd5e0; border-right: 1px solid #cbd5e0; color: #06132b; }
.stock-info { font-size: 13px; color: #718096; }

/* 2 NÚT THÊM VÀ MUA NGAY TRONG MODAL */
.vm-btn-cart, .vm-btn-buy { border-radius: 12px; padding: 14px; font-size: 13px; font-weight: 800; letter-spacing: 0.5px; transition: all 0.25s ease; text-transform: uppercase; display: flex; justify-content: center; align-items: center; border: none; }
.vm-btn-cart { background: #0a142f; color: #ffffff; }
.vm-btn-cart:hover:not(:disabled) { background: #13275a; transform: translateY(-2px); box-shadow: 0 6px 14px rgba(10, 20, 47, 0.2); }
.vm-btn-buy { background: #b78d52; color: #ffffff; }
.vm-btn-buy:hover:not(:disabled) { background: #9b7541; transform: translateY(-2px); box-shadow: 0 6px 14px rgba(183, 141, 82, 0.25); }
.vm-btn-cart:disabled, .vm-btn-buy:disabled { opacity: 0.6; cursor: not-allowed; transform: none; box-shadow: none; }

/* Ẩn mũi tên input */
.qty-wrapper input[type="number"]::-webkit-inner-spin-button, .qty-wrapper input[type="number"]::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.qty-wrapper input[type="number"] { appearance: textfield; -moz-appearance: textfield; }

/* Badge cho phần trăm Sale */
.flash-sale-badge { 
  background: #b31320; 
  color: #ffffff; 
  border-radius: 999px; 
  padding: 3px 10px; 
  font-size: 12px; 
  font-weight: 800; 
  margin-bottom: 2px; 
}
</style>