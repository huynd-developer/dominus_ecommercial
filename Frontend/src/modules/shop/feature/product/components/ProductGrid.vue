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
            <span class="stars">★★★★★</span>
            <span class="score">{{ item?.rating || "5.0" }}</span>
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
            <button
              type="button"
              class="btn-buy-now-small"
              :disabled="isBuyNowDisabled(item) || isBuyNowLoading(item)"
              @click.stop="handleBuyNow(item)"
            >
              <span
                v-if="isBuyNowLoading(item)"
                class="spinner-border spinner-border-sm me-1"
              ></span>

              <span v-else>
                Mua ngay
              </span>
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
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
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
const buyNowLoadingMap = ref<Record<number, boolean>>({});

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

const getProductImage = (item: any) => {
  return (
    item?.imageUrl ||
    item?.image ||
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

const isBuyNowLoading = (item: any) => {
  const variantId = getPrimaryVariantId(item);

  if (!variantId) {
    return false;
  }

  return Boolean(buyNowLoadingMap.value[variantId]);
};

const setFavoriteLoading = (variantId: number, value: boolean) => {
  favoriteLoadingMap.value = {
    ...favoriteLoadingMap.value,
    [variantId]: value,
  };
};

const setBuyNowLoading = (variantId: number, value: boolean) => {
  buyNowLoadingMap.value = {
    ...buyNowLoadingMap.value,
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

const handleBuyNow = async (item: any) => {
  const variantId = getPrimaryVariantId(item);

  if (!variantId || Number.isNaN(variantId)) {
    await Swal.fire({
      icon: "warning",
      title: "Không xác định được biến thể",
      text: "Sản phẩm này chưa có biến thể hợp lệ để mua ngay.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (getVariantPrice(item) <= 0) {
    await Swal.fire({
      icon: "warning",
      title: "Sản phẩm chưa có giá",
      text: "Sản phẩm chưa có giá bán. Vui lòng xem chi tiết hoặc liên hệ cửa hàng.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (getVariantStock(item) <= 0) {
    await Swal.fire({
      icon: "warning",
      title: "Tạm hết hàng",
      text: "Sản phẩm này hiện đã hết hàng.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để mua sản phẩm.");
    return;
  }

  if (!isCustomerLoggedIn()) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể mua hàng",
      text: "Chỉ tài khoản khách hàng mới được mua hàng.",
      confirmButtonColor: "#bd9a5f",
    });
    return;
  }

  try {
    setBuyNowLoading(variantId, true);

    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: 1,
    });

    window.dispatchEvent(new Event("cart-updated"));

    router.push({
      name: "Checkout",
    });
  } catch (error: any) {
    console.error("Lỗi mua ngay:", error);

    await Swal.fire({
      icon: "error",
      title: "Không thể mua ngay",
      text:
        error?.response?.data?.message ||
        error?.response?.data ||
        "Không thể thêm sản phẩm vào giỏ. Vui lòng thử lại.",
      confirmButtonColor: "#bd9a5f",
    });
  } finally {
    setBuyNowLoading(variantId, false);
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
});

onBeforeUnmount(() => {
  window.removeEventListener("favorite-updated", handleFavoriteUpdated);
});

watch(
  () => props.productList,
  () => {
    loadMyFavorites();
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
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.product-card.luxury-card:hover {
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
}

.card-img-wrapper {
  position: relative;
  background: #f8f9fa;
  padding: 40px 20px;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-img-wrapper img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  mix-blend-mode: multiply;
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
</style>