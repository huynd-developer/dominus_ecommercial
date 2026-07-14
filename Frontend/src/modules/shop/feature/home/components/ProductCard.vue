<template>
  <div class="product-card h-100 position-relative overflow-hidden" @click="goToDetail">
    <span v-if="product.discountPercent" class="discount-badge">
      -{{ product.discountPercent }}%
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
      <i v-else class="bi" :class="isFavorited ? 'bi-heart-fill' : 'bi-heart'"></i>
    </button>

    <div class="product-image-wrapper">
      <div class="product-bottle" :style="getBottleStyle(product.color)">
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
        <span class="stars">★★★★★</span>
        <span class="review-count">
          {{ product.rating }} | {{ product.reviewCount }} đánh giá
        </span>
      </div>

      <div class="price-row d-flex align-items-end gap-2 mb-3 flex-wrap">
        <span class="sale-price">
          {{ formatCurrency(product.salePrice) }}
        </span>

        <span
          v-if="product.discountPercent > 0"
          class="original-price text-decoration-line-through"
        >
          {{ formatCurrency(product.originalPrice) }}
        </span>
      </div>

      <div class="product-actions">
        <button
          type="button"
          class="btn buy-now-btn"
          :disabled="buyNowLoading || addCartLoading"
          @click.stop="handleBuyNow"
        >
          <i v-if="buyNowLoading" class="spinner-border spinner-border-sm me-2"></i>
          <i v-else class="bi bi-lightning-charge me-2"></i>
          {{ buyNowLoading ? "Đang xử lý..." : "Mua ngay" }}
        </button>

        <button
          type="button"
          class="btn add-cart-btn"
          :disabled="addCartLoading || buyNowLoading"
          @click.stop="handleAddToCart"
        >
          <i v-if="addCartLoading" class="spinner-border spinner-border-sm me-2"></i>
          <i v-else class="bi bi-bag-plus me-2"></i>
          {{ addCartLoading ? "Đang thêm..." : "Thêm vào giỏ" }}
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
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import api from "@/common/api";
import { favoriteService } from "@/modules/shop/feature/product/services/favorite.service";

interface ProductVariant {
  id?: number;
  Id?: number;
  variantId?: number;
  productVariantId?: number;
  price?: number;
  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  quantity?: number;
  status?: number;
}

interface Product {
  id: number;
  productId?: number;
  productVariantId?: number;
  variantId?: number;
  name: string;
  brand: string;
  color?: string;
  salePrice: number;
  originalPrice: number;
  discountPercent: number;
  rating: number;
  reviewCount: number;
  imageUrl?: string;
  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  status?: number;
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

const toast = ref({
  show: false,
  type: "success" as "success" | "warning" | "error",
  title: "",
  message: "",
  showCartLink: false,
});

let toastTimer: ReturnType<typeof window.setTimeout> | undefined;

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

const shortBrand = computed(() => {
  return (
    brandMap[props.product.brand] ||
    String(props.product.brand || "AURA").slice(0, 8).toUpperCase()
  );
});

const getBottleStyle = (color?: string): Record<string, string> => {
  return {
    "--bottle-color": color || "#0a192f",
  };
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ";
};

const showToast = (
  type: "success" | "warning" | "error",
  title: string,
  message: string,
  showCartLink = false
) => {
  toast.value = {
    show: true,
    type,
    title,
    message,
    showCartLink,
  };

  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }

  toastTimer = window.setTimeout(() => {
    toast.value.show = false;
  }, 2800);
};

const getCurrentRole = () => {
  return String(
    localStorage.getItem("role") ||
      localStorage.getItem("userRole") ||
      ""
  )
    .replace("ROLE_", "")
    .toUpperCase()
    .trim();
};

const hasToken = () => {
  return Boolean(localStorage.getItem("token"));
};

const isCustomerLoggedIn = () => {
  return hasToken() && getCurrentRole() === "USER";
};

const getPrimaryVariant = () => {
  if (Array.isArray(props.product.variants) && props.product.variants.length > 0) {
    const availableVariant =
      props.product.variants.find((variant) => {
        const stock = Number(
          variant.stockQuantity ??
            variant.stock ??
            variant.availableQuantity ??
            variant.quantity ??
            0
        );

        const price = Number(variant.price ?? props.product.salePrice ?? 0);
        const status = Number(variant.status ?? 1);

        return status === 1 && stock > 0 && price > 0;
      }) || props.product.variants[0];

    return availableVariant;
  }

  return props.product;
};

const getProductVariantId = () => {
  const variant: any = getPrimaryVariant();

  return Number(
    variant?.productVariantId ??
      variant?.variantId ??
      variant?.id ??
      variant?.Id ??
      props.product.productVariantId ??
      props.product.variantId ??
      props.product.id ??
      0
  );
};

const getProductId = () => {
  return Number(props.product.productId || props.product.id || 0);
};

const getStock = () => {
  const variant: any = getPrimaryVariant();

  return Number(
    variant?.stockQuantity ??
      variant?.stock ??
      variant?.availableQuantity ??
      variant?.quantity ??
      props.product.stockQuantity ??
      props.product.stock ??
      props.product.availableQuantity ??
      1
  );
};

const getPrice = () => {
  const variant: any = getPrimaryVariant();

  return Number(
    variant?.price ??
      props.product.salePrice ??
      props.product.originalPrice ??
      0
  );
};

const getProductDetailPath = () => {
  const productId = getProductId();

  if (productId > 0) {
    const resolved = router.resolve({
      name: "SingleProduct",
      params: {
        id: productId,
      },
    });

    return resolved.fullPath;
  }

  return "/product";
};

const goToDetail = () => {
  const productId = getProductId();

  if (productId > 0) {
    router.push({
      name: "SingleProduct",
      params: {
        id: productId,
      },
    });

    return;
  }

  router.push("/product");
};

const redirectToLogin = (redirectPath?: string) => {
  router.push({
    name: "Login",
    query: {
      redirect: redirectPath || router.currentRoute.value.fullPath,
    },
  });
};

const validateProductInfo = () => {
  const variantId = getProductVariantId();

  if (!variantId || Number.isNaN(variantId)) {
    showToast(
      "warning",
      "Không xác định được biến thể",
      "Sản phẩm này chưa có biến thể hợp lệ."
    );
    return false;
  }

  if (getPrice() <= 0) {
    showToast(
      "warning",
      "Sản phẩm chưa có giá",
      "Vui lòng xem chi tiết hoặc liên hệ cửa hàng."
    );
    return false;
  }

  if (getStock() <= 0) {
    showToast(
      "warning",
      "Tạm hết hàng",
      "Sản phẩm này hiện đã hết hàng."
    );
    return false;
  }

  return true;
};

const requireCustomerLoginForCart = () => {
  if (!hasToken()) {
    redirectToLogin(router.currentRoute.value.fullPath);
    return false;
  }

  if (!isCustomerLoggedIn()) {
    showToast(
      "warning",
      "Không thể mua hàng",
      "Chỉ tài khoản khách hàng mới được mua hàng."
    );
    return false;
  }

  return true;
};

const requireCustomerLoginForBuyNow = () => {
  if (!hasToken()) {
    redirectToLogin(getProductDetailPath());
    return false;
  }

  if (!isCustomerLoggedIn()) {
    showToast(
      "warning",
      "Không thể mua hàng",
      "Chỉ tài khoản khách hàng mới được mua hàng."
    );
    return false;
  }

  return true;
};

const requireCustomerLoginForFavorite = () => {
  if (!hasToken()) {
    redirectToLogin(router.currentRoute.value.fullPath);
    return false;
  }

  if (!isCustomerLoggedIn()) {
    showToast(
      "warning",
      "Không thể yêu thích",
      "Chỉ tài khoản khách hàng mới được thêm sản phẩm yêu thích."
    );
    return false;
  }

  return true;
};

const addProductToCart = async () => {
  await api.post("/v1/customer/cart/add", {
    productVariantId: getProductVariantId(),
    quantity: 1,
  });

  window.dispatchEvent(new Event("cart-updated"));
};

const handleAddToCart = async () => {
  if (!validateProductInfo()) {
    return;
  }

  if (!requireCustomerLoginForCart()) {
    return;
  }

  try {
    addCartLoading.value = true;

    await addProductToCart();

    showToast(
      "success",
      "Thêm thành công",
      "Đã thêm 1 sản phẩm vào giỏ.",
      true
    );
  } catch (error: any) {
    console.error("Lỗi khi gọi API thêm giỏ hàng:", error);

    showToast(
      "error",
      "Không thể thêm vào giỏ",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Vui lòng thử lại sau."
    );
  } finally {
    addCartLoading.value = false;
  }
};

const handleBuyNow = async () => {
  if (!validateProductInfo()) {
    return;
  }

  if (!requireCustomerLoginForBuyNow()) {
    return;
  }

  try {
    buyNowLoading.value = true;

    await addProductToCart();

    router.push({
      name: "Checkout",
    });
  } catch (error: any) {
    console.error("Lỗi khi mua ngay:", error);

    showToast(
      "error",
      "Không thể mua ngay",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Vui lòng thử lại sau."
    );
  } finally {
    buyNowLoading.value = false;
  }
};

const loadFavoriteStatus = async () => {
  const variantId = getProductVariantId();

  if (!variantId || !isCustomerLoggedIn()) {
    isFavorited.value = false;
    return;
  }

  try {
    const res = await favoriteService.checkFavorite(variantId);
    isFavorited.value = Boolean(res.data?.favorited);
  } catch (error) {
    console.error("Lỗi kiểm tra yêu thích:", error);
    isFavorited.value = false;
  }
};

const handleToggleFavorite = async () => {
  const variantId = getProductVariantId();

  if (!variantId || Number.isNaN(variantId)) {
    showToast(
      "warning",
      "Không xác định được biến thể",
      "Sản phẩm này chưa có biến thể hợp lệ để thêm yêu thích."
    );
    return;
  }

  if (!requireCustomerLoginForFavorite()) {
    return;
  }

  try {
    favoriteLoading.value = true;

    const res = await favoriteService.toggleFavorite(variantId);
    isFavorited.value = Boolean(res.data?.favorited);

    window.dispatchEvent(
      new CustomEvent("favorite-updated", {
        detail: {
          productVariantId: variantId,
          favorited: isFavorited.value,
        },
      })
    );

    showToast(
      isFavorited.value ? "success" : "warning",
      isFavorited.value ? "Đã thêm yêu thích" : "Đã bỏ yêu thích",
      res.data?.message ||
        (isFavorited.value
          ? "Sản phẩm đã được thêm vào danh sách yêu thích."
          : "Sản phẩm đã được bỏ khỏi danh sách yêu thích.")
    );
  } catch (error: any) {
    console.error("Lỗi xử lý yêu thích:", error);

    showToast(
      "error",
      "Không thể xử lý yêu thích",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Vui lòng thử lại sau."
    );
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

  if (!variantId || variantId !== getProductVariantId()) {
    return;
  }

  isFavorited.value = Boolean(customEvent.detail?.favorited);
};

onMounted(() => {
  window.addEventListener("favorite-updated", handleFavoriteUpdated);
  loadFavoriteStatus();
});

onBeforeUnmount(() => {
  window.removeEventListener("favorite-updated", handleFavoriteUpdated);

  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }
});

watch(
  () => getProductVariantId(),
  () => {
    loadFavoriteStatus();
  }
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
  height: 235px;
  padding: 26px 24px 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at center, rgba(189, 154, 95, 0.1), transparent 48%),
    #ffffff;
}

.product-bottle {
  width: 120px;
  height: 190px;
  display: flex;
  flex-direction: column;
  align-items: center;
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
  z-index: 99999;
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
</style>