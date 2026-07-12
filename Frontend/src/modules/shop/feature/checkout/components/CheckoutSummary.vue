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
          />
        </div>

        <div class="mini-info">
          <h4 class="item-name">
            {{ item.productName || item.sku || "Sản phẩm" }}
          </h4>

          <p class="item-variant">
            Dung tích:
            <strong style="color: #b78d52;">
              {{ formatCapacity(item) }}
            </strong>
          </p>

          <div class="item-meta">
            <span>Đơn giá:</span>
            <strong>{{ formatCurrency(getItemPrice(item)) }}</strong>
          </div>

          <div class="quantity-control">
            <button
              type="button"
              class="qty-btn"
              :disabled="isSubmitting || isUpdating(item) || Number(item.quantity || 0) <= 1"
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

    <div class="summary-preview">
      <div class="summary-line">
        <span>Tạm tính</span>
        <span>{{ formatCurrency(totalAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Giảm giá</span>
        <span>{{ formatCurrency(discountAmount) }}</span>
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
const props = defineProps<{
  cartItems: any[];
  totalItems: number;
  totalAmount: number;
  discountAmount: number;
  finalTotal: number;
  isSubmitting: boolean;
  updatingItemKey?: string | number | null;
}>();

const emit = defineEmits<{
  (e: "submit-order"): void;
  (e: "back"): void;
  (e: "update-quantity", item: any, quantity: number): void;
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

const getItemPrice = (item: any) => {
  return Number(item?.price ?? item?.finalPrice ?? item?.originalPrice ?? 0);
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
    return true;
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

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
};
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
  align-items: center;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.item-meta strong {
  color: #06132b;
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
}
</style>