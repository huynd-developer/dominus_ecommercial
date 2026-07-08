<template>
  <div class="cart-left">
    <div class="card-header">
      <div class="header-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z" />
          <line x1="3" y1="6" x2="21" y2="6" />
          <path d="M16 10a4 4 0 01-8 0" />
        </svg>
      </div>

      <h2>Giỏ hàng của bạn</h2>
    </div>

    <div v-if="isLoading" class="loading">
      Đang tải dữ liệu...
    </div>

    <div v-else-if="cartItems.length === 0" class="empty">
      Giỏ hàng trống.
      <router-link to="/products">Tiếp tục mua sắm ➔</router-link>
    </div>

    <div v-else class="item-list">
      <div
        v-for="item in cartItems"
        :key="item.cartItemId"
        :class="['cart-item', { 'item-unavailable': !isItemAvailable(item) }]"
      >
        <img
          :src="getItemImage(item)"
          class="item-img"
          :alt="item.productName || item.sku || 'Sản phẩm'"
          @error="handleImageError"
        />

        <div class="item-info">
          <div class="name-row">
            <h4 class="item-name">
              {{ item.productName || item.sku || "Sản phẩm" }}
            </h4>

            <span v-if="item.sku" class="sku-badge">
              {{ item.sku }}
            </span>
          </div>

          <p class="item-variant">
            Dung tích:
            <strong>{{ item.capacity || "-" }}</strong>
          </p>

          <p class="stock-line">
            Tồn kho:
            <strong>{{ Number(item.stockQuantity || 0) }}</strong>
          </p>

          <p v-if="!isItemAvailable(item)" class="unavailable-text">
            {{ item.unavailableReason || "Sản phẩm hiện không khả dụng" }}
          </p>

          <div class="qty-wrapper">
            <button
              type="button"
              @click="$emit('update-qty', item, Number(item.quantity || 0) - 1)"
              :disabled="isUpdating || Number(item.quantity || 0) <= 1"
            >
              −
            </button>

            <input
              type="text"
              :value="Number(item.quantity || 0)"
              readonly
            />

            <button
              type="button"
              @click="$emit('update-qty', item, Number(item.quantity || 0) + 1)"
              :disabled="
                isUpdating ||
                !isItemAvailable(item) ||
                Number(item.quantity || 0) >= Number(item.stockQuantity || 0)
              "
            >
              +
            </button>
          </div>
        </div>

        <div class="item-action">
          <span class="price">
            {{ formatCurrency(getLineTotal(item)) }}
          </span>

          <button
            class="btn-delete"
            type="button"
            @click="$emit('remove-item', item.cartItemId)"
            title="Xóa sản phẩm"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
              <line x1="10" y1="11" x2="10" y2="17" />
              <line x1="14" y1="11" x2="14" y2="17" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  cartItems: any[];
  isLoading: boolean;
  isUpdating?: boolean;
}>();

defineEmits<{
  (e: "update-qty", item: any, quantity: number): void;
  (e: "remove-item", cartItemId: number): void;
}>();

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
      <rect width="100%" height="100%" fill="#f3f4f6"/>
      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
        fill="#9ca3af" font-family="Arial" font-size="20">
        No Image
      </text>
    </svg>
  `);

const getItemImage = (item: any) => {
  return item?.image || item?.imageUrl || item?.thumbnailUrl || FALLBACK_IMAGE;
};

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement;
  target.src = FALLBACK_IMAGE;
};

const isItemAvailable = (item: any) => {
  return item?.available !== false && Number(item?.stockQuantity || 0) > 0;
};

const getLineTotal = (item: any) => {
  return Number(item?.price || 0) * Number(item?.quantity || 0);
};

const formatCurrency = (val: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  })
    .format(Number(val || 0))
    .replace("₫", "₫");
};
</script>

<style scoped>
.cart-left {
  flex: 2;
  background: white;
  border: 1px solid #eaeaea;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 30px;
}

.header-icon {
  background: #06132b;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon svg {
  width: 18px;
  height: 18px;
}

.card-header h2 {
  font-size: 22px;
  color: #06132b;
  margin: 0;
  position: relative;
  font-weight: 700;
}

.card-header h2::after {
  content: "";
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 50px;
  height: 3px;
  background: #b78d52;
  border-radius: 2px;
}

.loading {
  padding: 30px 0;
  color: #718096;
}

.empty {
  margin-top: 20px;
  color: #718096;
  font-size: 16px;
}

.empty a {
  color: #b78d52;
  font-weight: bold;
  text-decoration: none;
  margin-left: 5px;
}

.empty a:hover {
  text-decoration: underline;
  color: #8e6c3a;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 30px 0;
  border-bottom: 1px solid #f0f0f0;
}

.cart-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-unavailable {
  opacity: 0.72;
}

.item-img {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #eaeaea;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);
  background: #f8fafc;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.name-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-name {
  font-size: 19px;
  font-weight: 700;
  margin: 0;
  color: #0a142f;
  line-height: 1.35;
}

.sku-badge {
  width: fit-content;
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
}

.item-variant {
  font-size: 15px;
  color: #718096;
  margin: 0;
}

.item-variant strong {
  color: #b78d52;
  font-weight: 700;
  font-size: 16px;
  background: rgba(183, 141, 82, 0.1);
  padding: 4px 10px;
  border-radius: 6px;
}

.stock-line {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.stock-line strong {
  color: #06132b;
}

.unavailable-text {
  margin: 0;
  color: #dc2626;
  background: #fee2e2;
  border: 1px solid #fecaca;
  padding: 8px 10px;
  border-radius: 8px;
  width: fit-content;
  font-size: 13px;
}

.qty-wrapper {
  display: inline-flex;
  border: 1px solid #cbd5e0;
  border-radius: 8px;
  overflow: hidden;
  width: fit-content;
  margin-top: 5px;
}

.qty-wrapper button {
  width: 45px;
  height: 45px;
  background: #f8f9fa;
  border: none;
  cursor: pointer;
  color: #0a142f;
  font-size: 20px;
  font-weight: bold;
  transition: 0.2s;
}

.qty-wrapper button:hover:not(:disabled) {
  background: #e2e8f0;
}

.qty-wrapper button:disabled {
  color: #cbd5e0;
  cursor: not-allowed;
  background: #f8f9fa;
}

.qty-wrapper input {
  width: 55px;
  text-align: center;
  border: none;
  font-size: 16px;
  font-weight: 600;
  outline: none;
  border-left: 1px solid #cbd5e0;
  border-right: 1px solid #cbd5e0;
  color: #0a142f;
}

.item-action {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  min-height: 130px;
}

.price {
  font-weight: 800;
  font-size: 22px;
  color: #e53e3e;
}

.btn-delete {
  background: #fff5f5;
  border: 1px solid #fed7d7;
  cursor: pointer;
  color: #e53e3e;
  display: flex;
  padding: 12px;
  border-radius: 8px;
  transition: 0.2s;
}

.btn-delete svg {
  width: 22px;
  height: 22px;
}

.btn-delete:hover {
  background: #e53e3e;
  color: white;
  border-color: #e53e3e;
  box-shadow: 0 4px 10px rgba(229, 62, 62, 0.2);
}

@media (max-width: 768px) {
  .cart-left {
    padding: 24px;
  }

  .cart-item {
    align-items: flex-start;
    gap: 16px;
  }

  .item-img {
    width: 90px;
    height: 90px;
  }

  .item-action {
    min-height: auto;
  }

  .price {
    font-size: 17px;
  }
}
</style>