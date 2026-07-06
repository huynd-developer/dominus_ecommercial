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
        :key="item.cartItemId || item.id || item.productVariantId"
      >
        <div class="mini-qty">{{ item.quantity }}</div>

        <img
          :src="getItemImage(item)"
          class="mini-img"
          :alt="item.productName || item.sku || 'Sản phẩm'"
        />

        <div class="mini-info">
          <h4 class="item-name">{{ item.productName || item.sku || "Sản phẩm" }}</h4>

          <p class="item-variant">
            Dung tích:
            <strong style="color: #b78d52;">
              {{ item.capacity || item.capacityValue || "-" }}
            </strong>
          </p>

          <div class="mini-price">
            {{ formatCurrency(getItemPrice(item) * Number(item.quantity || 0)) }}
          </div>
        </div>
      </div>
    </div>

    <div class="summary-preview">
      <div class="summary-line">
        <span>Tạm tính</span>
        <span>{{ formatCurrency(totalAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Phí vận chuyển</span>
        <span :class="{ 'free-ship': shippingFee === 0 }">
          {{ shippingFee === 0 ? "Miễn phí" : formatCurrency(shippingFee) }}
        </span>
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
      :disabled="isSubmitting || cartItems.length === 0"
    >
      {{ isSubmitting ? "ĐANG XỬ LÝ..." : "XÁC NHẬN ĐẶT HÀNG" }}
    </button>

    <button class="btn-back" type="button" @click="$emit('back')">
      Quay lại giỏ hàng
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  cartItems: any[];
  totalItems: number;
  totalAmount: number;
  shippingFee: number;
  finalTotal: number;
  isSubmitting: boolean;
}>();

defineEmits<{
  (e: "submit-order"): void;
  (e: "back"): void;
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

const getItemImage = (item: any) => {
  return item?.image || item?.imageUrl || item?.thumbnailUrl || FALLBACK_IMAGE;
};

const getItemPrice = (item: any) => {
  return Number(item?.price ?? item?.finalPrice ?? item?.originalPrice ?? 0);
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  })
    .format(Number(value || 0))
    .replace("₫", "₫");
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
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 20px;
  padding-right: 5px;
}

.mini-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
  position: relative;
}

.mini-qty {
  position: absolute;
  top: -5px;
  left: -5px;
  background: #666;
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  z-index: 2;
}

.mini-img {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  border: 1px solid #eaeaea;
  object-fit: cover;
}

.mini-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px;
  line-height: 1.4;
}

.item-variant {
  margin: 0 0 4px;
  font-size: 12px;
  color: #718096;
}

.mini-price {
  font-size: 13px;
  color: #666;
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

.free-ship {
  color: #38a169;
  font-weight: 500;
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
</style>