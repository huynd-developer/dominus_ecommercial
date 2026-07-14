<template>
  <div class="checkout-right">
    <h3 class="summary-title">Đơn hàng của bạn ({{ totalItems }})</h3>

    <div v-if="cartItems.length === 0" class="empty-cart">
      Giỏ hàng đang trống
    </div>

    <div v-else class="mini-cart-items">
      <div
        class="mini-item"
        v-for="item in cartItems"
        :key="getItemKey(item)"
      >
        <div class="mini-img-wrapper">
          <div class="mini-qty">{{ item.quantity }}</div>

          <img
            :src="getItemImage(item)"
            class="mini-img"
            :alt="item.productName || item.sku || 'Sản phẩm'"
            @error="handleImageError"
          />
        </div>

        <div class="mini-info">
          <h4 class="item-name">
            {{ item.productName || item.sku || "Sản phẩm" }}
          </h4>

          <p class="item-variant">
            Dung tích:
            <strong style="color: #b78d52">
              {{ formatCapacity(item) }}
            </strong>
          </p>

          <p class="item-variant">
            Loại chai:
            <strong style="color: #b78d52">
              {{ item.bottleType || item.bottleTypeName || "-" }}
            </strong>
          </p>

          <div class="item-meta">
            <span>Đơn giá:</span>

            <div class="price-stack">
              <span
                v-if="hasPromotion(item)"
                class="old-unit-price"
              >
                {{ formatCurrency(getOriginalPrice(item)) }}
              </span>

              <strong>{{ formatCurrency(getItemPrice(item)) }}</strong>

              <span
                v-if="hasPromotion(item)"
                class="sale-chip"
              >
                -{{ formatDiscount(item.discountPercent) }}%
              </span>
            </div>
          </div>

          <div class="quantity-control">
            <button
              type="button"
              class="qty-btn"
              :disabled="
                isSubmitting ||
                isUpdating(item) ||
                Number(item.quantity || 0) <= 1
              "
              @click="emitUpdateQuantity(item, Number(item.quantity || 0) - 1)"
            >
              -
            </button>

            <input
              type="number"
              :value="Number(item.quantity || 0)"
              readonly
            />

            <button
              type="button"
              class="qty-btn"
              :disabled="isSubmitting || isUpdating(item) || !canIncrease(item)"
              @click="emitUpdateQuantity(item, Number(item.quantity || 0) + 1)"
            >
              +
            </button>

            <span v-if="isUpdating(item)" class="qty-loading">
              Đang cập nhật...
            </span>
          </div>

          <div
            v-if="getStock(item) > 0"
            class="stock-hint"
          >
            Còn {{ getStock(item) }} sản phẩm
          </div>
        </div>

        <div class="mini-total">
          {{ formatCurrency(getItemPrice(item) * Number(item.quantity || 0)) }}
        </div>
      </div>
    </div>

    <div class="voucher-section">
      <div class="voucher-title-row">
        <div>
          <h4>Mã giảm giá</h4>
          <p>Nhập mã hoặc chọn voucher đang khả dụng</p>
        </div>

        <button
          type="button"
          class="btn-refresh-voucher"
          :disabled="voucherLoading || isSubmitting"
          @click="fetchAvailableVouchers"
          title="Làm mới voucher"
        >
          <span
            v-if="voucherLoading"
            class="spinner-border spinner-border-sm"
          ></span>
          <span v-else>↻</span>
        </button>
      </div>

      <div class="voucher-control">
        <div class="voucher-input-wrapper">
          <svg
            class="voucher-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect x="2" y="7" width="20" height="10" rx="2" ry="2" />
            <path d="M2 12a2 2 0 010-4m20 4a2 2 0 000-4M10 7v10m4-10v10" />
          </svg>

          <input
            v-model="voucherCode"
            type="text"
            placeholder="Nhập hoặc chọn mã..."
            class="text-uppercase"
            :disabled="
              isSubmitting ||
              Boolean(updatingItemKey) ||
              isApplyingVoucher ||
              isVoucherApplied ||
              cartItems.length === 0 ||
              totalAmount <= 0
            "
            @focus="openVoucherDropdown"
            @input="openVoucherDropdown"
            @blur="scheduleCloseDropdown"
            @keyup.enter="handleApplyVoucher"
          />

          <button
            type="button"
            class="btn-toggle-dropdown"
            :disabled="
              isSubmitting ||
              Boolean(updatingItemKey) ||
              isApplyingVoucher ||
              isVoucherApplied ||
              cartItems.length === 0 ||
              totalAmount <= 0
            "
            @mousedown.prevent
            @click="toggleVoucherDropdown"
          >
            ▾
          </button>
        </div>

        <button
          v-if="isVoucherApplied || discountAmount > 0"
          type="button"
          class="btn-cancel-voucher"
          :disabled="isSubmitting || Boolean(updatingItemKey)"
          @click="handleCancelVoucher"
        >
          Hủy
        </button>

        <button
          v-else
          type="button"
          class="btn-apply-voucher"
          :disabled="
            isSubmitting ||
            Boolean(updatingItemKey) ||
            isApplyingVoucher ||
            !voucherCode.trim() ||
            cartItems.length === 0 ||
            totalAmount <= 0
          "
          @click="handleApplyVoucher"
        >
          <span
            v-if="isApplyingVoucher"
            class="spinner-border spinner-border-sm"
          ></span>
          <span v-else>Áp dụng</span>
        </button>
      </div>

      <div
        v-if="showVoucherDropdown && !isVoucherApplied"
        class="voucher-dropdown"
      >
        <div v-if="voucherLoading" class="voucher-dropdown-empty">
          Đang tải voucher...
        </div>

        <div v-else-if="filteredVouchers.length === 0" class="voucher-dropdown-empty">
          Không có voucher phù hợp
        </div>

        <button
          v-else
          v-for="voucher in filteredVouchers"
          :key="getVoucherCode(voucher)"
          type="button"
          class="voucher-option"
          :class="{ disabled: !canUseVoucherLocally(voucher) }"
          @mousedown.prevent="selectVoucher(voucher)"
        >
          <div class="voucher-option-left">
            <div class="voucher-code">
              {{ getVoucherCode(voucher) }}
            </div>

            <div class="voucher-desc">
              Giảm
              <strong>{{ formatVoucherDiscount(voucher) }}</strong>
              <span v-if="getMaxDiscount(voucher) > 0">
                · tối đa {{ formatCurrency(getMaxDiscount(voucher)) }}
              </span>
            </div>

            <div class="voucher-min">
              Đơn tối thiểu {{ formatCurrency(getMinOrderValue(voucher)) }}
            </div>
          </div>

          <div class="voucher-option-right">
            <span v-if="canUseVoucherLocally(voucher)" class="voucher-use">
              Chọn
            </span>

            <span v-else class="voucher-disabled-text">
              Chưa đủ
            </span>
          </div>
        </button>
      </div>

      <p
        class="voucher-message"
        :class="voucherMessageClass"
      >
        {{ voucherMessage || "Chọn voucher để giảm giá cho đơn hàng." }}
      </p>
    </div>

    <div class="summary-preview">
      <div class="summary-line">
        <span>Tạm tính</span>
        <span>{{ formatCurrency(totalAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Giảm giá</span>
        <span class="discount-value">-{{ formatCurrency(discountAmount) }}</span>
      </div>

      <div class="summary-line total-line">
        <span>Tổng thanh toán</span>
        <span class="total-val">{{ formatCurrency(finalTotal) }}</span>
      </div>
    </div>

    <button
      class="btn-submit"
      type="button"
      @click="$emit('submit-order')"
      :disabled="isSubmitting || cartItems.length === 0 || Boolean(updatingItemKey)"
    >
      {{
        isSubmitting
          ? "ĐANG XỬ LÝ..."
          : updatingItemKey
            ? "ĐANG CẬP NHẬT GIỎ..."
            : "XÁC NHẬN ĐẶT HÀNG"
      }}
    </button>

    <button class="btn-back" type="button" @click="$emit('back')">
      Quay lại giỏ hàng
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import api from "@/common/api";

const props = withDefaults(
  defineProps<{
    cartItems: any[];
    totalItems: number;
    totalAmount: number;
    discountAmount: number;
    finalTotal: number;
    isSubmitting: boolean;
    updatingItemKey?: string | number | null;
    selectedVoucherCode?: string | null;
  }>(),
  {
    updatingItemKey: null,
    selectedVoucherCode: "",
  }
);

const emit = defineEmits<{
  (e: "submit-order"): void;
  (e: "back"): void;
  (e: "update-quantity", item: any, quantity: number): void;
  (e: "apply-voucher", discountAmount: number, voucherCode: string): void;
  (e: "cancel-voucher"): void;
}>();

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="150" height="150">
      <rect width="100%" height="100%" fill="#f3f4f6"/>
      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
        fill="#9ca3af" font-family="Arial" font-size="16">
        No Image
      </text>
    </svg>
  `);

const availableVouchers = ref<any[]>([]);
const voucherCode = ref("");
const voucherMessage = ref("");
const voucherMessageClass = ref("");
const showVoucherDropdown = ref(false);
const voucherLoading = ref(false);
const isApplyingVoucher = ref(false);
const isVoucherApplied = ref(false);
const vouchersLoaded = ref(false);

let closeDropdownTimer: ReturnType<typeof setTimeout> | null = null;

const filteredVouchers = computed(() => {
  const keyword = voucherCode.value.trim().toLowerCase();

  return availableVouchers.value
    .filter((voucher) => {
      const code = getVoucherCode(voucher).toLowerCase();

      if (!keyword) {
        return true;
      }

      return code.includes(keyword);
    })
    .slice(0, 10);
});

const getItemKey = (item: any) => {
  return (
    item?.cartItemId ||
    item?.id ||
    item?.productVariantId ||
    item?.variantId ||
    item?.sku
  );
};

const getItemImage = (item: any) => {
  return item?.image || item?.imageUrl || item?.thumbnailUrl || FALLBACK_IMAGE;
};

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement;
  target.src = FALLBACK_IMAGE;
};

const getItemPrice = (item: any) => {
  return Number(item?.price ?? item?.salePrice ?? item?.finalPrice ?? item?.originalPrice ?? 0);
};

const getOriginalPrice = (item: any) => {
  return Number(item?.originalPrice ?? item?.oldPrice ?? item?.price ?? 0);
};

const hasPromotion = (item: any) => {
  const originalPrice = getOriginalPrice(item);
  const salePrice = getItemPrice(item);
  const discountPercent = Number(item?.discountPercent || 0);

  return Boolean(item?.hasPromotion || item?.isFlashSale) &&
    discountPercent > 0 &&
    originalPrice > salePrice;
};

const getStock = (item: any) => {
  return Number(
    item?.stockQuantity ??
      item?.stock ??
      item?.availableQuantity ??
      item?.maxQuantity ??
      0
  );
};

const formatCapacity = (item: any) => {
  const value = item?.capacity || item?.capacityValue || item?.volume || "";

  if (value === null || value === undefined || value === "") {
    return "-";
  }

  const text = String(value);

  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const canIncrease = (item: any) => {
  const stock = getStock(item);
  const quantity = Number(item?.quantity || 0);

  if (stock <= 0) {
    return false;
  }

  return quantity < stock;
};

const isUpdating = (item: any) => {
  return String(props.updatingItemKey || "") === String(getItemKey(item));
};

const emitUpdateQuantity = (item: any, quantity: number) => {
  if (quantity < 1) {
    return;
  }

  emit("update-quantity", item, quantity);
};

const fetchAvailableVouchers = async () => {
  try {
    voucherLoading.value = true;

    const res = await api.get("/v1/customer/vouchers");

    if (Array.isArray(res.data)) {
      availableVouchers.value = res.data;
    } else if (Array.isArray(res.data?.content)) {
      availableVouchers.value = res.data.content;
    } else if (Array.isArray(res.data?.data)) {
      availableVouchers.value = res.data.data;
    } else {
      availableVouchers.value = [];
    }

    vouchersLoaded.value = true;
  } catch (error) {
    console.error("Không tải được voucher:", error);
    availableVouchers.value = [];
    vouchersLoaded.value = true;
  } finally {
    voucherLoading.value = false;
  }
};

const openVoucherDropdown = async () => {
  if (isVoucherApplied.value) {
    return;
  }

  if (closeDropdownTimer) {
    clearTimeout(closeDropdownTimer);
    closeDropdownTimer = null;
  }

  showVoucherDropdown.value = true;

  if (!vouchersLoaded.value) {
    await fetchAvailableVouchers();
  }
};

const toggleVoucherDropdown = async () => {
  if (isVoucherApplied.value) {
    return;
  }

  showVoucherDropdown.value = !showVoucherDropdown.value;

  if (showVoucherDropdown.value && !vouchersLoaded.value) {
    await fetchAvailableVouchers();
  }
};

const scheduleCloseDropdown = () => {
  closeDropdownTimer = setTimeout(() => {
    showVoucherDropdown.value = false;
  }, 180);
};

const selectVoucher = async (voucher: any) => {
  const code = getVoucherCode(voucher);

  if (!code) {
    return;
  }

  voucherCode.value = code;
  showVoucherDropdown.value = false;

  await handleApplyVoucher();
};

const handleApplyVoucher = async () => {
  const cleanCode = voucherCode.value.trim().toUpperCase();

  if (!cleanCode) {
    voucherMessage.value = "Vui lòng nhập hoặc chọn mã giảm giá.";
    voucherMessageClass.value = "text-danger";
    return;
  }

  if (props.cartItems.length === 0 || props.totalAmount <= 0) {
    voucherMessage.value = "Giỏ hàng chưa có sản phẩm để áp dụng voucher.";
    voucherMessageClass.value = "text-danger";
    return;
  }

  try {
    isApplyingVoucher.value = true;
    voucherMessage.value = "";
    voucherMessageClass.value = "";
    showVoucherDropdown.value = false;

    const res = await api.get("/v1/customer/vouchers/apply", {
      params: {
        code: cleanCode,
        orderTotal: props.totalAmount,
      },
    });

    const discount = Number(
      res.data?.discountAmount ??
        res.data?.discount ??
        res.data?.amount ??
        0
    );

    voucherCode.value = cleanCode;
    isVoucherApplied.value = true;
    voucherMessage.value = res.data?.message || "Áp dụng voucher thành công.";
    voucherMessageClass.value = "text-success";

    localStorage.setItem("applied_voucher", cleanCode);

    emit("apply-voucher", discount, cleanCode);
  } catch (error: any) {
    console.error("Không áp dụng được voucher:", error);

    isVoucherApplied.value = false;
    voucherMessage.value = getErrorMessage(
      error,
      "Không thể áp dụng mã giảm giá này."
    );
    voucherMessageClass.value = "text-danger";

    emit("apply-voucher", 0, "");
  } finally {
    isApplyingVoucher.value = false;
  }
};

const handleCancelVoucher = () => {
  isVoucherApplied.value = false;
  voucherCode.value = "";
  voucherMessage.value = "Đã hủy voucher.";
  voucherMessageClass.value = "text-muted";

  localStorage.removeItem("applied_voucher");

  emit("cancel-voucher");
  emit("apply-voucher", 0, "");
};

const getVoucherCode = (voucher: any) => {
  return String(voucher?.code ?? voucher?.voucherCode ?? voucher?.name ?? "").trim();
};

const getVoucherDiscountType = (voucher: any) => {
  return String(voucher?.discountType ?? voucher?.type ?? "").toUpperCase();
};

const getVoucherDiscountValue = (voucher: any) => {
  return Number(voucher?.discountValue ?? voucher?.value ?? voucher?.discount ?? 0);
};

const getMinOrderValue = (voucher: any) => {
  return Number(voucher?.minOrderValue ?? voucher?.minimumOrderValue ?? voucher?.minOrderAmount ?? 0);
};

const getMaxDiscount = (voucher: any) => {
  return Number(voucher?.maxDiscountAmount ?? voucher?.maximumDiscountAmount ?? 0);
};

const canUseVoucherLocally = (voucher: any) => {
  return props.totalAmount >= getMinOrderValue(voucher);
};

const formatVoucherDiscount = (voucher: any) => {
  const type = getVoucherDiscountType(voucher);
  const value = getVoucherDiscountValue(voucher);

  if (type === "PERCENT" || type === "PERCENTAGE") {
    return `${formatDiscount(value)}%`;
  }

  return formatCurrency(value);
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
};

const formatDiscount = (value?: number | null) => {
  const numberValue = Number(value || 0);

  if (Number.isInteger(numberValue)) {
    return String(numberValue);
  }

  return numberValue.toFixed(2).replace(/\.?0+$/, "");
};

const getErrorMessage = (error: any, fallback: string) => {
  const data = error?.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  if (data?.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];

    if (firstError) {
      return String(firstError);
    }
  }

  return error?.message || fallback;
};

watch(
  () => props.selectedVoucherCode,
  (value) => {
    const code = String(value || "").trim();

    if (code && !voucherCode.value) {
      voucherCode.value = code;
      isVoucherApplied.value = Number(props.discountAmount || 0) > 0;
    }
  },
  { immediate: true }
);

watch(
  () => props.totalAmount,
  (newValue, oldValue) => {
    if (
      oldValue !== undefined &&
      Number(newValue || 0) !== Number(oldValue || 0) &&
      isVoucherApplied.value
    ) {
      handleCancelVoucher();
      voucherMessage.value =
        "Đã hủy voucher vì giỏ hàng thay đổi. Vui lòng áp dụng lại.";
      voucherMessageClass.value = "text-danger";
    }
  }
);

watch(
  () => props.discountAmount,
  (value) => {
    if (Number(value || 0) <= 0 && isVoucherApplied.value && !isApplyingVoucher.value) {
      isVoucherApplied.value = false;
    }
  }
);

onMounted(() => {
  fetchAvailableVouchers();

  const savedVoucher = localStorage.getItem("applied_voucher");

  if (savedVoucher && !voucherCode.value) {
    voucherCode.value = savedVoucher;
  }
});
</script>

<style scoped>
.checkout-right {
  flex: 1.2;
  background: white;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  position: sticky;
  top: 20px;
}

.summary-title {
  font-size: 18px;
  margin: 0 0 20px 0;
  color: #06132b;
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
}

.empty-cart {
  padding: 30px 10px;
  text-align: center;
  color: #718096;
  background: #f8fafc;
  border-radius: 8px;
}

.mini-cart-items {
  max-height: 320px;
  overflow-y: auto;
  margin-bottom: 20px;
  padding-right: 5px;
}

.mini-item {
  display: grid;
  grid-template-columns: 58px 1fr auto;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
  position: relative;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.mini-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.mini-img-wrapper {
  position: relative;
  width: 58px;
  height: 58px;
}

.mini-qty {
  position: absolute;
  top: -6px;
  left: -6px;
  background: #06132b;
  color: white;
  min-width: 21px;
  height: 21px;
  padding: 0 5px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  z-index: 2;
}

.mini-img {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  border: 1px solid #eaeaea;
  object-fit: cover;
  background: #f8fafc;
}

.mini-info {
  min-width: 0;
}

.item-name {
  font-size: 13px;
  font-weight: 700;
  color: #06132b;
  margin: 0 0 4px;
  line-height: 1.4;
}

.item-variant {
  margin: 0 0 5px;
  font-size: 12px;
  color: #718096;
}

.item-meta {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.item-meta strong {
  color: #06132b;
}

.price-stack {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.old-unit-price {
  color: #94a3b8;
  text-decoration: line-through;
}

.sale-chip {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 999px;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 800;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 6px;
}

.qty-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #d7dde8;
  background: #ffffff;
  color: #06132b;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition: 0.2s;
}

.qty-btn:first-child {
  border-radius: 7px 0 0 7px;
}

.qty-btn:nth-child(3) {
  border-radius: 0 7px 7px 0;
}

.qty-btn:hover:not(:disabled) {
  background: #06132b;
  border-color: #06132b;
  color: #ffffff;
}

.qty-btn:disabled {
  color: #cbd5e0;
  cursor: not-allowed;
  background: #f8fafc;
}

.quantity-control input {
  width: 42px;
  height: 30px;
  border: 1px solid #d7dde8;
  border-left: none;
  border-right: none;
  text-align: center;
  outline: none;
  font-size: 13px;
  font-weight: 800;
  color: #06132b;
  background: #ffffff;
}

.qty-loading {
  margin-left: 8px;
  font-size: 11px;
  color: #b78d52;
  font-weight: 700;
}

.stock-hint {
  margin-top: 5px;
  color: #718096;
  font-size: 11px;
}

.mini-total {
  color: #06132b;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  padding-top: 2px;
}

.voucher-section {
  position: relative;
  margin: 20px 0 0;
  padding: 16px;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  background: #f8fafc;
}

.voucher-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.voucher-title-row h4 {
  margin: 0;
  color: #06132b;
  font-size: 14px;
  font-weight: 800;
}

.voucher-title-row p {
  margin: 3px 0 0;
  color: #718096;
  font-size: 12px;
}

.btn-refresh-voucher {
  width: 32px;
  height: 32px;
  border: 1px solid #d7dde8;
  border-radius: 999px;
  background: #ffffff;
  color: #06132b;
  font-weight: 900;
  cursor: pointer;
}

.btn-refresh-voucher:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.voucher-control {
  display: flex;
  gap: 8px;
}

.voucher-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1px solid #d7dde8;
  border-radius: 10px;
  overflow: hidden;
}

.voucher-icon {
  width: 18px;
  height: 18px;
  color: #b78d52;
  margin-left: 12px;
  flex-shrink: 0;
}

.voucher-input-wrapper input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  padding: 11px 8px;
  color: #06132b;
  font-weight: 700;
  background: transparent;
}

.voucher-input-wrapper input:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-toggle-dropdown {
  width: 38px;
  align-self: stretch;
  border: none;
  border-left: 1px solid #eef2f7;
  background: #ffffff;
  color: #06132b;
  cursor: pointer;
}

.btn-toggle-dropdown:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-apply-voucher,
.btn-cancel-voucher {
  min-width: 86px;
  border: none;
  border-radius: 10px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.btn-apply-voucher {
  background: #06132b;
  color: #ffffff;
}

.btn-apply-voucher:hover:not(:disabled) {
  background: #0a1f44;
}

.btn-cancel-voucher {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.btn-cancel-voucher:hover:not(:disabled) {
  background: #dc2626;
  color: #ffffff;
}

.btn-apply-voucher:disabled,
.btn-cancel-voucher:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.voucher-dropdown {
  position: absolute;
  top: 116px;
  left: 16px;
  right: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.16);
  max-height: 300px;
  overflow-y: auto;
  z-index: 30;
}

.voucher-dropdown-empty {
  padding: 16px;
  color: #718096;
  font-size: 13px;
  text-align: center;
}

.voucher-option {
  width: 100%;
  border: none;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.voucher-option:last-child {
  border-bottom: none;
}

.voucher-option:hover:not(.disabled) {
  background: #fffaf0;
}

.voucher-option.disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.voucher-code {
  display: inline-flex;
  width: fit-content;
  background: rgba(183, 141, 82, 0.14);
  color: #7c541e;
  border: 1px solid rgba(183, 141, 82, 0.25);
  border-radius: 6px;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 900;
}

.voucher-desc {
  margin-top: 6px;
  color: #334155;
  font-size: 12px;
}

.voucher-desc strong {
  color: #dc2626;
}

.voucher-min {
  margin-top: 3px;
  color: #718096;
  font-size: 11px;
}

.voucher-option-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.voucher-use {
  color: #16a34a;
  font-size: 12px;
  font-weight: 900;
}

.voucher-disabled-text {
  color: #dc2626;
  font-size: 12px;
  font-weight: 900;
}

.voucher-message {
  margin: 9px 0 0;
  min-height: 18px;
  font-size: 12px;
  font-weight: 600;
  color: #718096;
}

.text-success {
  color: #16a34a !important;
}

.text-danger {
  color: #dc2626 !important;
}

.text-muted {
  color: #718096 !important;
}

.summary-preview {
  margin-top: 20px;
  padding: 20px 0;
  border-top: 1px dashed #cbd5e0;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  color: #4a5568;
}

.discount-value {
  color: #dc2626;
  font-weight: 800;
}

.total-line {
  border-top: 1px solid #e2e8f0;
  padding-top: 15px;
  margin-top: 5px;
  font-weight: bold;
  font-size: 16px;
  color: #06132b;
}

.total-val {
  font-size: 22px;
  color: #b78d52;
}

.btn-submit {
  width: 100%;
  padding: 16px;
  background: #06132b;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  font-size: 15px;
  cursor: pointer;
  transition: 0.2s;
  margin-top: 10px;
}

.btn-submit:hover:not(:disabled) {
  background: #0a1f44;
}

.btn-submit:disabled {
  background: #a0aec0;
  cursor: not-allowed;
}

.btn-back {
  width: 100%;
  background: transparent;
  border: none;
  color: #666;
  font-size: 14px;
  text-decoration: underline;
  cursor: pointer;
  padding: 15px 0;
  margin-top: 5px;
}

.btn-back:hover {
  color: #06132b;
}

@media (max-width: 768px) {
  .checkout-right {
    position: static;
    width: 100%;
  }

  .mini-item {
    grid-template-columns: 58px 1fr;
  }

  .mini-total {
    grid-column: 2;
  }

  .voucher-control {
    flex-direction: column;
  }

  .btn-apply-voucher,
  .btn-cancel-voucher {
    min-height: 42px;
  }

  .voucher-dropdown {
    top: 156px;
  }
}
</style>