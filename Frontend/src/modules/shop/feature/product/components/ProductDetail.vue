<template>
  <div class="detail-view-container">
    <nav class="breadcrumb">
      <span class="back-link" @click="emit('back')">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          style="width: 14px; margin-right: 4px; vertical-align: middle;"
        >
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      </span>

      <span class="divider">/</span>
      Nước hoa
      <span class="divider">/</span>
      {{ product?.gender || "Sản phẩm" }}
      <span class="divider">/</span>
      {{ product?.brand?.name || product?.brand || "Thương hiệu" }}
      <span class="divider">/</span>
      <span class="active">
        {{ product?.name || "Đang cập nhật" }}
      </span>
    </nav>

    <div class="product-content" v-if="product">
      <div class="product-gallery">
        <div class="main-image-wrapper">
          <img
            :src="productImage"
            class="main-image"
            :alt="product?.name || 'Sản phẩm'"
          />

          <button class="btn-heart" type="button">
            ♡
          </button>
        </div>

        <div class="thumbnail-list">
          <div
            v-for="index in 4"
            :key="index"
            class="thumb"
            :class="{ active: index === 1 }"
          >
            <img :src="productImage" :alt="product?.name || 'Sản phẩm'" />
          </div>
        </div>
      </div>

      <div class="product-info">
        <div class="header-info">
          <div class="brand">
            {{ product?.brand?.name || product?.brand || "Đang cập nhật" }}
          </div>

          <div class="share">
            <svg
              class="icon-sm"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
            Chia sẻ
          </div>
        </div>

        <h1 class="title">
          {{ product?.name || "Tên sản phẩm đang cập nhật" }}
        </h1>

        <div class="rating">
          <span class="stars">
            <i
              v-for="star in 5"
              :key="star"
              class="bi"
              :class="star <= roundedAverage ? 'bi-star-fill' : 'bi-star'"
            ></i>
          </span>

          <span class="score">
            {{ averageRatingText }}
          </span>

          <span class="divider-line">|</span>

          <span class="reviews">
            {{ reviewCount }} đánh giá
          </span>
        </div>

        <div class="price-box">
          <span class="current-price">
            {{
              selectedVariant?.price != null && Number(selectedVariant.price) > 0
                ? formatCurrency(Number(selectedVariant.price))
                : "Liên hệ"
            }}
          </span>

          <span
            class="old-price"
            v-if="
              product?.oldPrice &&
              Number(product.oldPrice) > Number(selectedVariant?.price || 0)
            "
          >
            {{ formatCurrency(Number(product.oldPrice)) }}
          </span>
        </div>

        <div
          class="save-badge"
          v-if="
            product?.oldPrice &&
            Number(product.oldPrice) > Number(selectedVariant?.price || 0)
          "
        >
          Tiết kiệm
          {{
            formatCurrency(
              Number(product.oldPrice) - Number(selectedVariant?.price || 0)
            )
          }}
        </div>

        <div class="desc-divider"></div>

        <p class="desc">
          {{
            product?.description ||
            "Chưa có thông tin mô tả chi tiết cho sản phẩm này."
          }}
        </p>

        <div class="desc-divider"></div>

        <div class="variant-selection">
          <h4>Chọn dung tích</h4>

          <div
            class="capacity-options"
            v-if="product?.variants && product.variants.length > 0"
          >
            <button
              v-for="variant in product.variants"
              :key="variant.id"
              type="button"
              :class="['cap-btn', { active: selectedVariant?.id === variant.id }]"
              @click="selectVariant(variant)"
            >
              {{ variant.capacity || "N/A" }}

              <span
                v-if="selectedVariant?.id === variant.id"
                class="check-icon"
              >
                ✓
              </span>
            </button>
          </div>

          <div
            v-else
            style="color: #e53e3e; font-size: 14px; margin-bottom: 20px;"
          >
            Sản phẩm hiện chưa có dung tích nào
          </div>
        </div>

        <div class="quantity-section" v-if="selectedVariant">
          <h4>Số lượng</h4>

          <div class="stock-info">
            Tồn kho:
            <strong>{{ normalizeStock(selectedVariant) }}</strong>
          </div>

          <div class="qty-control">
            <button
              type="button"
              @click="decreaseQty"
              :disabled="quantity <= 1"
            >
              -
            </button>

            <input type="number" v-model="quantity" readonly />

            <button
              type="button"
              @click="increaseQty"
              :disabled="quantity >= normalizeStock(selectedVariant)"
            >
              +
            </button>
          </div>
        </div>

        <div class="actions">
          <button
            class="btn-add-cart"
            type="button"
            @click="addToCart"
            :disabled="
              isAdding ||
              !selectedVariant ||
              isVariantOutOfStock ||
              isVariantInvalidPrice
            "
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="btn-icon"
            >
              <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <path d="M16 10a4 4 0 01-8 0" />
            </svg>

            {{
              isVariantInvalidPrice
                ? "LIÊN HỆ ĐỂ MUA"
                : isVariantOutOfStock
                  ? "TẠM HẾT HÀNG"
                  : isAdding
                    ? "ĐANG THÊM..."
                    : "THÊM VÀO GIỎ HÀNG"
            }}
          </button>

          <button
            class="btn-buy-now"
            type="button"
            @click="buyNow"
            :disabled="
              isAdding ||
              !selectedVariant ||
              isVariantOutOfStock ||
              isVariantInvalidPrice
            "
          >
            {{
              isVariantInvalidPrice
                ? "LIÊN HỆ ĐỂ MUA"
                : isVariantOutOfStock
                  ? "TẠM HẾT HÀNG"
                  : "MUA NGAY"
            }}

            <svg
              v-if="
                selectedVariant &&
                !isVariantInvalidPrice &&
                !isVariantOutOfStock
              "
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="btn-icon-right"
            >
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>
        </div>

        <div class="policy-footer">
          <div class="policy-item">
            <svg
              class="icon-policy"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>

            <div>
              <strong>Cam kết chính hãng</strong><br />
              100% Authentic
            </div>
          </div>

          <div class="policy-item">
            <svg
              class="icon-policy"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <polyline points="1 4 1 10 7 10" />
              <polyline points="23 20 23 14 17 14" />
              <path
                d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"
              />
            </svg>

            <div>
              <strong>Đổi trả dễ dàng</strong><br />
              Trong 7 ngày
            </div>
          </div>

          <div class="policy-item">
            <svg
              class="icon-policy"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <rect x="1" y="3" width="15" height="13" />
              <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
              <circle cx="5.5" cy="18.5" r="2.5" />
              <circle cx="18.5" cy="18.5" r="2.5" />
            </svg>

            <div>
              <strong>Giao hàng miễn phí</strong><br />
              Đơn từ 1.000.000đ
            </div>
          </div>
        </div>
      </div>
    </div>

    <ProductReviews
      v-if="product?.id"
      :product-id="Number(product.id)"
      @summary-loaded="handleReviewSummaryLoaded"
    />

    <div class="luxury-toast" :class="{ show: showToast }">
      <div class="toast-content">
        <div class="icon-circle-toast">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>

        <div class="toast-text">
          <strong>Thêm thành công</strong>
          <span>Đã thêm {{ quantity }} sản phẩm vào giỏ.</span>
        </div>
      </div>

      <button class="toast-action" type="button" @click="goToCart">
        Xem giỏ hàng ➔
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";
import ProductReviews from "./ProductReviews.vue";
import type { ProductReviewSummaryResponse } from "../types/product-review.type";

const router = useRouter();

const props = defineProps<{
  product: any;
}>();

const emit = defineEmits<{
  (e: "back"): void;
  (e: "buy-now"): void;
}>();

const selectedVariant = ref<any>(null);
const quantity = ref<number>(1);
const showToast = ref(false);
const isAdding = ref(false);
const reviewSummary = ref<ProductReviewSummaryResponse | null>(null);

const productImage = computed(() => {
  return (
    props.product?.image ||
    props.product?.imageUrl ||
    "data:image/svg+xml;utf8," +
      encodeURIComponent(`
        <svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
          <rect width="100%" height="100%" fill="#f3f4f6"/>
          <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
            fill="#9ca3af" font-family="Arial" font-size="24">
            No Image
          </text>
        </svg>
      `)
  );
});

const averageRating = computed(() => {
  return Number(
    reviewSummary.value?.averageRating ??
      props.product?.averageRating ??
      props.product?.rating ??
      0
  );
});

const averageRatingText = computed(() => {
  return averageRating.value.toFixed(1);
});

const reviewCount = computed(() => {
  return Number(
    reviewSummary.value?.reviewCount ??
      props.product?.reviewCount ??
      0
  );
});

const roundedAverage = computed(() => {
  return Math.round(averageRating.value);
});

const normalizeStock = (variant: any) => {
  return Number(
    variant?.stock ??
      variant?.stockQuantity ??
      variant?.StockQuantity ??
      variant?.quantity ??
      variant?.availableQuantity ??
      0
  );
};

const isVariantOutOfStock = computed(() => {
  if (!selectedVariant.value) {
    return true;
  }

  return normalizeStock(selectedVariant.value) <= 0;
});

const isVariantInvalidPrice = computed(() => {
  if (!selectedVariant.value) {
    return true;
  }

  return Number(selectedVariant.value.price || 0) <= 0;
});

const formatCurrency = (value: number) => {
  if (value == null || Number.isNaN(Number(value))) {
    return "0 đ";
  }

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value));
};

watch(
  () => props.product,
  (newProduct) => {
    reviewSummary.value = null;

    if (
      newProduct &&
      Array.isArray(newProduct.variants) &&
      newProduct.variants.length > 0
    ) {
      selectedVariant.value = newProduct.variants[0];
      quantity.value = 1;
    } else {
      selectedVariant.value = null;
      quantity.value = 1;
    }
  },
  {
    immediate: true,
  }
);

const handleReviewSummaryLoaded = (summary: ProductReviewSummaryResponse) => {
  reviewSummary.value = summary;
};

const selectVariant = (variant: any) => {
  selectedVariant.value = variant;
  quantity.value = 1;
};

const decreaseQty = () => {
  if (quantity.value > 1) {
    quantity.value--;
  }
};

const increaseQty = () => {
  if (!selectedVariant.value) {
    return;
  }

  const stock = normalizeStock(selectedVariant.value);

  if (quantity.value < stock) {
    quantity.value++;
  }
};

const getVariantId = () => {
  return Number(
    selectedVariant.value?.id ??
      selectedVariant.value?.Id ??
      selectedVariant.value?.productVariantId ??
      0
  );
};

const showWarning = async (title: string, text: string) => {
  await Swal.fire({
    icon: "warning",
    title,
    text,
    confirmButtonText: "Đã hiểu",
    confirmButtonColor: "#bd9a5f",
  });
};

const showError = async (title: string, text: string) => {
  await Swal.fire({
    icon: "error",
    title,
    text,
    confirmButtonText: "Đóng",
    confirmButtonColor: "#bd9a5f",
  });
};

const askLogin = async () => {
  const result = await Swal.fire({
    icon: "info",
    title: "Bạn chưa đăng nhập",
    text: "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.",
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

const validateBeforeCartAction = async () => {
  if (!selectedVariant.value) {
    await showWarning(
      "Chưa chọn dung tích",
      "Vui lòng chọn dung tích trước khi mua hàng."
    );
    return false;
  }

  const variantId = getVariantId();

  if (!variantId || Number.isNaN(variantId)) {
    await showError(
      "Biến thể không hợp lệ",
      "Không xác định được biến thể sản phẩm. Vui lòng tải lại trang."
    );
    return false;
  }

  if (isVariantInvalidPrice.value) {
    await showWarning(
      "Sản phẩm chưa có giá",
      "Sản phẩm chưa có giá bán. Vui lòng liên hệ cửa hàng."
    );
    return false;
  }

  if (isVariantOutOfStock.value) {
    await showWarning(
      "Tạm hết hàng",
      "Sản phẩm này hiện đã hết hàng."
    );
    return false;
  }

  const token = localStorage.getItem("token");

  if (!token) {
    await askLogin();
    return false;
  }

  return true;
};

const addToCart = async () => {
  const valid = await validateBeforeCartAction();

  if (!valid) {
    return;
  }

  try {
    isAdding.value = true;

    await api.post("/v1/customer/cart/add", {
      productVariantId: getVariantId(),
      quantity: quantity.value,
    });

    showToast.value = true;

    window.setTimeout(() => {
      showToast.value = false;
    }, 3000);
  } catch (error: any) {
    console.error("Lỗi khi thêm vào giỏ hàng:", error);

    await showError(
      "Không thể thêm vào giỏ",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Không thể thêm vào giỏ. Vui lòng thử lại."
    );
  } finally {
    isAdding.value = false;
  }
};

const buyNow = async () => {
  const valid = await validateBeforeCartAction();

  if (!valid) {
    return;
  }

  try {
    isAdding.value = true;

    await api.post("/v1/customer/cart/add", {
      productVariantId: getVariantId(),
      quantity: quantity.value,
    });

    emit("buy-now");
  } catch (error: any) {
    console.error("Lỗi khi xử lý Mua ngay:", error);

    await showError(
      "Không thể mua ngay",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Không thể tiến hành mua ngay. Vui lòng thử lại."
    );
  } finally {
    isAdding.value = false;
  }
};

const goToCart = () => {
  router.push("/cart");
};
</script>

<style scoped>
.detail-view-container {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.breadcrumb {
  font-size: 13px;
  color: #718096;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  font-weight: 600;
  color: #0a142f;
  cursor: pointer;
  transition: 0.2s;
  display: flex;
  align-items: center;
}

.back-link:hover {
  color: #c69c6d;
}

.divider {
  color: #cbd5e0;
}

.breadcrumb .active {
  color: #c69c6d;
  font-weight: 500;
}

.product-content {
  display: flex;
  gap: 50px;
  background: #ffffff;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.product-gallery {
  flex: 1;
  max-width: 480px;
}

.main-image-wrapper {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  position: relative;
  margin-bottom: 15px;
}

.main-image {
  width: 100%;
  mix-blend-mode: multiply;
}

.btn-heart {
  position: absolute;
  top: 15px;
  right: 15px;
  background: #ffffff;
  border: 1px solid #eaeaea;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  font-size: 22px;
  cursor: pointer;
  color: #718096;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}

.btn-heart:hover {
  color: #e53e3e;
  border-color: #e53e3e;
}

.thumbnail-list {
  display: flex;
  gap: 15px;
}

.thumb {
  width: calc(25% - 11.25px);
  border-radius: 8px;
  border: 1px solid #eaeaea;
  cursor: pointer;
  padding: 5px;
  background: #f8f9fa;
  transition: 0.2s;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb img {
  max-width: 100%;
  max-height: 100%;
  mix-blend-mode: multiply;
}

.thumb.active {
  border: 2px solid #0a142f;
}

.product-info {
  flex: 1.2;
}

.header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.brand {
  color: #c69c6d;
  font-weight: 600;
  font-size: 16px;
}

.share {
  font-size: 13px;
  color: #718096;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-sm {
  width: 16px;
  height: 16px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 15px 0;
  color: #0a142f;
  letter-spacing: -0.5px;
}

.rating {
  font-size: 14px;
  color: #718096;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating .stars {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #ecc94b;
}

.rating .stars i {
  font-size: 14px;
}

.score {
  font-weight: bold;
  color: #0a142f;
}

.divider-line {
  color: #cbd5e0;
}

.price-box {
  margin-bottom: 5px;
  display: flex;
  align-items: baseline;
  gap: 15px;
}

.current-price {
  font-size: 28px;
  font-weight: bold;
  color: #0a142f;
}

.old-price {
  font-size: 16px;
  color: #a0aec0;
  text-decoration: line-through;
  border-left: 1px solid #cbd5e0;
  padding-left: 15px;
}

.save-badge {
  color: #c69c6d;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 25px;
}

.desc-divider {
  height: 1px;
  background: #eaeaea;
  margin: 25px 0;
}

.desc {
  color: #4a5568;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-line;
}

.variant-selection {
  margin-bottom: 30px;
}

.variant-selection h4,
.quantity-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #0a142f;
  margin: 0 0 15px 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.capacity-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.cap-btn {
  flex: 0 0 auto;
  min-width: 80px;
  padding: 10px 15px;
  border: 1px solid #cbd5e0;
  background: #ffffff;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
  transition: all 0.2s ease;
  text-align: center;
}

.cap-btn:hover {
  border-color: #c69c6d;
  color: #c69c6d;
}

.cap-btn.active {
  border-color: #0a142f;
  background: #0a142f;
  color: #ffffff;
}

.check-icon {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #c69c6d;
  color: #ffffff;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  font-weight: bold;
}

.stock-info {
  color: #718096;
  font-size: 13px;
  margin-bottom: 10px;
}

.stock-info strong {
  color: #0a142f;
}

.qty-control {
  display: inline-flex;
  border: 1px solid #cbd5e0;
  border-radius: 8px;
  margin-bottom: 40px;
  overflow: hidden;
}

.qty-control button {
  width: 45px;
  height: 45px;
  border: none;
  background: #ffffff;
  cursor: pointer;
  font-size: 18px;
  color: #4a5568;
}

.qty-control button:disabled {
  color: #cbd5e0;
  cursor: not-allowed;
}

.qty-control input {
  width: 60px;
  text-align: center;
  border: none;
  outline: none;
  font-weight: 600;
  font-size: 15px;
  border-left: 1px solid #cbd5e0;
  border-right: 1px solid #cbd5e0;
}

.actions {
  display: flex;
  gap: 15px;
  margin-bottom: 40px;
}

.btn-add-cart,
.btn-buy-now {
  flex: 1;
  padding: 16px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  border: none;
  transition: 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.btn-add-cart {
  background: #0a142f;
  color: #ffffff;
}

.btn-add-cart:hover:not(:disabled) {
  background: #13275a;
}

.btn-add-cart:disabled,
.btn-buy-now:disabled {
  background: #718096;
  cursor: not-allowed;
}

.btn-buy-now {
  background: #b78d52;
  color: #ffffff;
}

.btn-buy-now:hover:not(:disabled) {
  background: #c69c6d;
}

.btn-icon,
.btn-icon-right {
  width: 18px;
  height: 18px;
}

.policy-footer {
  display: flex;
  justify-content: space-between;
  border-top: 1px solid #eaeaea;
  padding-top: 25px;
}

.policy-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 12px;
  color: #718096;
  line-height: 1.5;
}

.icon-policy {
  width: 24px;
  height: 24px;
  color: #b78d52;
  flex-shrink: 0;
}

.policy-item strong {
  color: #4a5568;
  font-size: 13px;
  font-weight: 600;
}

.luxury-toast {
  position: fixed;
  bottom: 40px;
  right: 40px;
  background: #0a142f;
  color: #ffffff;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  min-width: 380px;
  transform: translateY(100px);
  opacity: 0;
  visibility: hidden;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 1000;
  border: 1px solid rgba(198, 156, 109, 0.3);
}

.luxury-toast.show {
  transform: translateY(0);
  opacity: 1;
  visibility: visible;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.icon-circle-toast {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(198, 156, 109, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c69c6d;
}

.icon-circle-toast svg {
  width: 20px;
  height: 20px;
}

.toast-text {
  display: flex;
  flex-direction: column;
}

.toast-text strong {
  font-size: 15px;
  margin-bottom: 2px;
  letter-spacing: 0.5px;
}

.toast-text span {
  font-size: 13px;
  color: #a0aec0;
}

.toast-action {
  background: transparent;
  border: none;
  color: #c69c6d;
  font-weight: bold;
  font-size: 13px;
  cursor: pointer;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding: 0;
  transition: 0.2s;
}

.toast-action:hover {
  color: #e8c499;
  text-decoration: underline;
}

@media (max-width: 992px) {
  .product-content {
    flex-direction: column;
    padding: 24px;
  }

  .product-gallery {
    max-width: 100%;
  }

  .actions,
  .policy-footer {
    flex-direction: column;
  }

  .luxury-toast {
    left: 16px;
    right: 16px;
    bottom: 20px;
    min-width: auto;
  }
}
</style>