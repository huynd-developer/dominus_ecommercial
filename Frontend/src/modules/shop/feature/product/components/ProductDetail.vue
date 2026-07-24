<template>
  <div class="detail-view-container">
    <nav class="breadcrumb">
      <span class="back-link" @click="emit('back')">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          style="width: 14px; margin-right: 4px; vertical-align: middle"
        >
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      </span>

      <span class="divider">/</span>
      Nước hoa
      <span class="divider">/</span>
      {{ genderText }}
      <span class="divider">/</span>
      {{ brandText }}
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

          <button
            class="btn-heart"
            type="button"
            :class="{ active: isFavorited }"
            :disabled="isFavoriteLoading || !selectedVariant"
            @click="toggleFavorite"
            :title="isFavorited ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
          >
            <i
              v-if="isFavoriteLoading"
              class="spinner-border spinner-border-sm"
            ></i>

            <i
              v-else
              class="bi"
              :class="isFavorited ? 'bi-heart-fill' : 'bi-heart'"
            ></i>
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
            {{ brandText }}
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
              selectedDisplayPrice > 0
                ? formatCurrency(selectedDisplayPrice)
                : "Liên hệ"
            }}
          </span>

          <span class="old-price" v-if="selectedHasFlashSale">
            {{ formatCurrency(selectedOriginalPrice) }}
          </span>

          <span v-if="selectedHasFlashSale" class="flash-sale-badge">
            -{{ formatDiscount(selectedDiscountPercent) }}%
          </span>
        </div>

        <div class="save-badge" v-if="selectedHasFlashSale">
          Flash Sale đang diễn ra - tiết kiệm
          {{ formatCurrency(selectedOriginalPrice - selectedDisplayPrice) }}
        </div>

        <div class="desc-divider"></div>

        <p class="desc">
          {{
            product?.description ||
            "Chưa có thông tin mô tả chi tiết cho sản phẩm này."
          }}
        </p>

        <div class="product-specs">
          <h4>Thông tin sản phẩm</h4>

          <div class="spec-grid">
            <div class="spec-item">
              <span>Thương hiệu</span>
              <strong>{{ brandText }}</strong>
            </div>

            <div class="spec-item">
              <span>Giới tính</span>
              <strong>{{ genderText }}</strong>
            </div>

            <div class="spec-item">
              <span>Nồng độ</span>
              <strong>{{ concentrationText }}</strong>
            </div>

            <div class="spec-item">
              <span>Nhóm hương</span>
              <strong>{{ fragranceFamilyText }}</strong>
            </div>

            <div class="spec-item">
              <span>Dung tích đang chọn</span>
              <strong>{{ selectedCapacityText }}</strong>
            </div>

            <div class="spec-item">
              <span>Loại chai</span>
              <strong>{{ bottleTypeText }}</strong>
            </div>

            <div class="spec-item">
              <span>Tình trạng</span>
              <strong :class="isVariantOutOfStock ? 'text-danger' : 'text-success'">
                {{ stockStatusText }}
              </strong>
            </div>
          </div>
        </div>

        <div class="desc-divider"></div>

        <div class="variant-selection">
          <h4>Chọn dung tích</h4>

          <div
            class="capacity-options"
            v-if="product?.variants && product.variants.length > 0"
          >
            <button
              v-for="variant in product.variants"
              :key="getVariantIdFromVariant(variant)"
              type="button"
              :class="[
                'cap-btn',
                {
                  active:
                    getVariantIdFromVariant(selectedVariant) ===
                    getVariantIdFromVariant(variant),
                },
              ]"
              @click="selectVariant(variant)"
            >
             <span>{{ getCapacityText(variant) }}</span>

              <span
                v-if="isVariantFlashSale(variant)"
                class="variant-sale-chip"
              >
                -{{ formatDiscount(variant.discountPercent) }}%
              </span>

              <span
                v-if="
                  getVariantIdFromVariant(selectedVariant) ===
                  getVariantIdFromVariant(variant)
                "
                class="check-icon"
              >
                ✓
              </span>
            </button>
          </div>

          <div
            v-else
            style="color: #e53e3e; font-size: 14px; margin-bottom: 20px"
          >
            Sản phẩm hiện chưa có dung tích nào
          </div>
        </div>

        <div class="stock-status mb-3" v-if="selectedVariant">
          <span
            v-if="normalizeStock(selectedVariant) > 0"
            style="color: #2e7d32; font-size: 14px; font-weight: 500"
          >
            <i class="bi bi-box-seam me-1"></i>
            Kho còn: {{ normalizeStock(selectedVariant) }} sản phẩm
          </span>

          <span
            v-else
            style="color: #d32f2f; font-size: 14px; font-weight: 500"
          >
            <i class="bi bi-x-circle me-1"></i>
            Đã hết hàng
          </span>
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
          <span>Đã thêm {{ lastAddedQuantity }} sản phẩm vào giỏ.</span>
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
import { favoriteService } from "../services/favorite.service";

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
const lastAddedQuantity = ref<number>(1);
const showToast = ref(false);
const isAdding = ref(false);
const reviewSummary = ref<ProductReviewSummaryResponse | null>(null);

const isFavorited = ref(false);
const isFavoriteLoading = ref(false);

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

const brandText = computed(() => {
  if (typeof props.product?.brand === "object") {
    return props.product?.brand?.name || "Đang cập nhật";
  }

  return props.product?.brandName || props.product?.brand || "Đang cập nhật";
});

const genderText = computed(() => {
  const gender = props.product?.gender;

  if (gender === 1 || gender === "1") return "Nam";
  if (gender === 2 || gender === "2") return "Nữ";
  if (gender === 0 || gender === "0") return "Unisex";

  if (typeof gender === "object") {
    return gender?.name || "Đang cập nhật";
  }

  return gender || "Đang cập nhật";
});

const concentrationText = computed(() => {
  if (typeof props.product?.concentration === "object") {
    return props.product?.concentration?.name || "Đang cập nhật";
  }

  return (
    props.product?.concentrationName ||
    props.product?.concentration ||
    "Đang cập nhật"
  );
});

const fragranceFamilyText = computed(() => {
  if (Array.isArray(props.product?.scents) && props.product.scents.length > 0) {
    return props.product.scents.join(", ");
  }

  if (Array.isArray(props.product?.fragranceFamilies)) {
    const names = props.product.fragranceFamilies
      .map((item: any) => (typeof item === "object" ? item?.name : item))
      .filter(Boolean);

    if (names.length > 0) {
      return names.join(", ");
    }
  }

  if (typeof props.product?.fragranceFamily === "object") {
    return props.product?.fragranceFamily?.name || "Đang cập nhật";
  }

  return (
    props.product?.fragranceFamilyName ||
    props.product?.fragranceFamily ||
    "Đang cập nhật"
  );
});

const selectedCapacityText = computed(() => {
  if (!selectedVariant.value) {
    return "Chưa chọn";
  }

  return getCapacityText(selectedVariant.value);
});

const bottleTypeText = computed(() => {
  if (!selectedVariant.value) {
    return "Đang cập nhật";
  }

  return getBottleTypeText(selectedVariant.value);
});

const selectedSkuText = computed(() => {
  return (
    selectedVariant.value?.sku ||
    selectedVariant.value?.SKU ||
    "Đang cập nhật"
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
    reviewSummary.value?.reviewCount ?? props.product?.reviewCount ?? 0
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

const getVariantIdFromVariant = (variant: any) => {
  return Number(
    variant?.productVariantId ??
      variant?.variantId ??
      variant?.id ??
      variant?.Id ??
      0
  );
};

const isEmptyDisplayValue = (value: any) => {
  if (value === null || value === undefined) {
    return true;
  }

  const text = String(value).trim();

  return (
    text === "" ||
    text.toUpperCase() === "N/A" ||
    text.toUpperCase() === "NULL" ||
    text.toUpperCase() === "UNDEFINED" ||
    text === "-"
  );
};

const formatCapacityNumber = (value: any) => {
  const text = String(value || "").trim();

  if (!text) {
    return "";
  }

  const lowerText = text.toLowerCase().replace(/\s+/g, "");

  if (lowerText.endsWith("ml")) {
    const numberPart = lowerText.replace("ml", "");
    const numberValue = Number(numberPart);

    if (Number.isFinite(numberValue)) {
      return `${Number.isInteger(numberValue) ? numberValue : numberValue.toString()}ml`;
    }

    return text;
  }

  const numberValue = Number(text);

  if (Number.isFinite(numberValue)) {
    return `${Number.isInteger(numberValue) ? numberValue : numberValue.toString()}ml`;
  }

  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const getCapacityText = (variant: any) => {
  const candidates = [
    variant?.capacityName,
    variant?.capacityText,
    variant?.capacityValue,
    variant?.volume,
    variant?.volumeValue,
    variant?.capacity?.name,
    variant?.capacity?.value,
    variant?.capacity,
  ];

  for (const value of candidates) {
    if (!isEmptyDisplayValue(value)) {
      return formatCapacityNumber(value);
    }
  }

  const sku = String(variant?.sku || variant?.SKU || "").toUpperCase();
  const match = sku.match(/-(\d+(?:\.\d+)?)-/);

  if (match?.[1]) {
    return formatCapacityNumber(match[1]);
  }

  return "Đang cập nhật";
};

const getBottleTypeText = (variant: any) => {
  const candidates = [
    variant?.bottleTypeName,
    variant?.bottleTypeText,
    variant?.variantBottleType,
    variant?.bottleName,
    variant?.bottleType?.name,
    variant?.bottleType,
    props.product?.bottleTypeName,
    props.product?.bottleType,
  ];

  for (const value of candidates) {
    if (!isEmptyDisplayValue(value)) {
      return String(value).trim();
    }
  }

  const sku = String(variant?.sku || variant?.SKU || "").toUpperCase();

  if (sku.includes("FULL")) {
    return "Chai gốc Fullbox";
  }

  if (sku.includes("CHIET") || sku.includes("DECANT")) {
    return "Chai chiết";
  }

  return "Đang cập nhật";
};

const getVariantId = () => {
  return getVariantIdFromVariant(selectedVariant.value);
};

const isVariantFlashSale = (variant: any) => {
  const originalPrice = Number(
    variant?.originalPrice ?? variant?.oldPrice ?? variant?.price ?? 0
  );

  const salePrice = Number(variant?.salePrice ?? variant?.price ?? 0);

  const discountPercent = Number(variant?.discountPercent ?? 0);

  return (
    Boolean(variant?.isFlashSale || variant?.hasPromotion) &&
    discountPercent > 0 &&
    originalPrice > salePrice
  );
};

const selectedOriginalPrice = computed(() => {
  if (!selectedVariant.value) {
    return 0;
  }

  return Number(
    selectedVariant.value.originalPrice ??
      selectedVariant.value.oldPrice ??
      selectedVariant.value.price ??
      0
  );
});

const selectedDisplayPrice = computed(() => {
  if (!selectedVariant.value) {
    return 0;
  }

  if (isVariantFlashSale(selectedVariant.value)) {
    return Number(
      selectedVariant.value.salePrice ??
        selectedVariant.value.price ??
        0
    );
  }

  return Number(selectedVariant.value.price ?? 0);
});

const selectedDiscountPercent = computed(() => {
  return Number(selectedVariant.value?.discountPercent ?? 0);
});

const selectedHasFlashSale = computed(() => {
  return isVariantFlashSale(selectedVariant.value);
});

const isVariantOutOfStock = computed(() => {
  if (!selectedVariant.value) {
    return true;
  }

  return normalizeStock(selectedVariant.value) <= 0;
});

const stockStatusText = computed(() => {
  if (!selectedVariant.value) {
    return "Chưa chọn dung tích";
  }

  return isVariantOutOfStock.value ? "Hết hàng" : "Còn hàng";
});

const isVariantInvalidPrice = computed(() => {
  if (!selectedVariant.value) {
    return true;
  }

  return selectedDisplayPrice.value <= 0;
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

const formatDiscount = (value?: number | null) => {
  const numberValue = Number(value || 0);

  if (Number.isInteger(numberValue)) {
    return String(numberValue);
  }

  return numberValue.toFixed(2).replace(/\.?0+$/, "");
};

const getErrorMessage = (error: any) => {
  const data = error?.response?.data;

  if (!data) {
    return "Có lỗi xảy ra, vui lòng thử lại.";
  }

  if (typeof data === "string") {
    return data;
  }

  if (data.message) {
    return data.message;
  }

  if (data.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];

    if (firstError) {
      return String(firstError);
    }
  }

  return "Có lỗi xảy ra, vui lòng thử lại.";
};

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

const showWarning = async (title: string, text: string) => {
  await Swal.fire({
    icon: "warning",
    title,
    text,
    confirmButtonText: "Đã hiểu",
    confirmButtonColor: "#bd9a5f",
  });
};

const showWarningHtml = async (title: string, html: string) => {
  await Swal.fire({
    icon: "warning",
    title,
    html,
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

const loadFavoriteStatus = async () => {
  const variantId = getVariantId();

  if (!variantId || Number.isNaN(variantId)) {
    isFavorited.value = false;
    return;
  }

  if (!isCustomerLoggedIn()) {
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

const toggleFavorite = async () => {
  const variantId = getVariantId();

  if (!variantId || Number.isNaN(variantId)) {
    await showWarning(
      "Chưa chọn dung tích",
      "Vui lòng chọn dung tích trước khi thêm yêu thích."
    );
    return;
  }

  if (!hasToken()) {
    await askLogin(
      "Vui lòng đăng nhập để thêm sản phẩm vào danh sách yêu thích."
    );
    return;
  }

  if (!isCustomerLoggedIn()) {
    await showWarning(
      "Không thể sử dụng chức năng này",
      "Chỉ tài khoản khách hàng mới được thêm sản phẩm yêu thích."
    );
    return;
  }

  try {
    isFavoriteLoading.value = true;

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

    await Swal.fire({
      toast: true,
      position: "top-end",
      icon: isFavorited.value ? "success" : "info",
      title:
        res.data?.message ||
        (isFavorited.value ? "Đã thêm vào yêu thích" : "Đã bỏ yêu thích"),
      showConfirmButton: false,
      timer: 1600,
      timerProgressBar: true,
    });
  } catch (error: any) {
    console.error("Lỗi yêu thích sản phẩm:", error);

    await showError(
      "Không thể xử lý yêu thích",
      getErrorMessage(error)
    );
  } finally {
    isFavoriteLoading.value = false;
  }
};

const getCurrentCartQuantity = async (productVariantId: number) => {
  try {
    const res = await api.get("/v1/customer/cart/my-cart");

    const items = Array.isArray(res.data) ? res.data : [];

    const cartItem = items.find(
      (item: any) =>
        Number(item.productVariantId) === Number(productVariantId)
    );

    return Number(cartItem?.quantity || 0);
  } catch (error) {
    console.error("Không kiểm tra được giỏ hàng hiện tại:", error);
    return 0;
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
    await showWarning("Tạm hết hàng", "Sản phẩm này hiện đã hết hàng.");
    return false;
  }

  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
    return false;
  }

  if (!isCustomerLoggedIn()) {
    await showWarning(
      "Không thể mua hàng",
      "Chỉ tài khoản khách hàng mới được thêm sản phẩm vào giỏ hàng."
    );
    return false;
  }

  const stockQuantity = normalizeStock(selectedVariant.value);
  const quantityToAdd = Number(quantity.value || 1);

  if (quantityToAdd <= 0) {
    await showWarning(
      "Số lượng không hợp lệ",
      "Số lượng phải lớn hơn 0."
    );
    return false;
  }

  if (quantityToAdd > stockQuantity) {
    await showWarning(
      "Vượt quá tồn kho",
      `Sản phẩm chỉ còn ${stockQuantity} trong kho.`
    );
    return false;
  }

  const currentCartQuantity = await getCurrentCartQuantity(variantId);
  const totalAfterAdd = currentCartQuantity + quantityToAdd;

  if (totalAfterAdd > stockQuantity) {
    await showWarningHtml(
      "Vượt quá tồn kho",
      `
        Sản phẩm này chỉ còn <b>${stockQuantity}</b> trong kho.<br/>
        Trong giỏ hàng của bạn hiện đã có <b>${currentCartQuantity}</b> sản phẩm.<br/>
        Bạn chỉ có thể thêm tối đa <b>${Math.max(
          stockQuantity - currentCartQuantity,
          0
        )}</b> sản phẩm nữa.
      `
    );

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

    const variantId = getVariantId();
    const quantityToAdd = Number(quantity.value || 1);

    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: quantityToAdd,
    });

    lastAddedQuantity.value = quantityToAdd;

    window.dispatchEvent(new Event("cart-updated"));

    showToast.value = true;

    window.setTimeout(() => {
      showToast.value = false;
    }, 3000);
  } catch (error: any) {
    console.error("Lỗi khi thêm vào giỏ hàng:", error);

    await showError(
      "Không thể thêm vào giỏ",
      getErrorMessage(error)
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

    const variantId = getVariantId();
    const quantityToAdd = Number(quantity.value || 1);

    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: quantityToAdd,
    });

    lastAddedQuantity.value = quantityToAdd;

    window.dispatchEvent(new Event("cart-updated"));

    emit("buy-now");
  } catch (error: any) {
    console.error("Lỗi khi xử lý Mua ngay:", error);

    await showError(
      "Không thể mua ngay",
      getErrorMessage(error)
    );
  } finally {
    isAdding.value = false;
  }
};

const goToCart = () => {
  router.push("/cart");
};

watch(
  () => props.product,
  (newProduct) => {
    reviewSummary.value = null;
    isFavorited.value = false;

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

watch(
  () => getVariantId(),
  () => {
    loadFavoriteStatus();
  },
  {
    immediate: true,
  }
);
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

.btn-heart.active {
  color: #e53e3e;
  border-color: #e53e3e;
  background: #fff5f5;
}

.btn-heart:disabled {
  opacity: 0.65;
  cursor: not-allowed;
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
  flex-wrap: wrap;
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

.flash-sale-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #dc2626;
  color: #ffffff;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 13px;
  font-weight: 800;
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

.product-specs {
  margin: 24px 0;
  padding: 20px;
  border: 1px solid #eaeaea;
  border-radius: 14px;
  background: #fafafa;
}

.product-specs h4 {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 700;
  color: #0a142f;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.spec-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px 20px;
}

.spec-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.spec-item span {
  font-size: 12px;
  color: #718096;
}

.spec-item strong {
  font-size: 14px;
  color: #0a142f;
  word-break: break-word;
}

.text-danger {
  color: #dc2626 !important;
}

.text-success {
  color: #16a34a !important;
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

.variant-sale-chip {
  display: block;
  margin-top: 4px;
  font-size: 10px;
  font-weight: 800;
  color: #dc2626;
}

.cap-btn.active .variant-sale-chip {
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

@media (max-width: 576px) {
  .spec-grid {
    grid-template-columns: 1fr;
  }

  .title {
    font-size: 26px;
  }

  .current-price {
    font-size: 24px;
  }
}
</style>