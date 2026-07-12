<template>
  <div class="cart-right">
    <h3 class="summary-title">Tóm tắt đơn hàng</h3>

    <div class="voucher-row disabled-voucher">
      <div class="voucher-input">
        <svg class="voucher-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="7" width="20" height="10" rx="2" ry="2" />
          <path d="M2 12a2 2 0 010-4m20 4a2 2 0 000-4M10 7v10m4-10v10" />
        </svg>

        <input
          type="text"
          placeholder="Voucher chưa áp dụng ở bước này"
          disabled
        />
      </div>

      <button class="btn-apply" type="button" disabled>
        Áp dụng
      </button>
    </div>

    <p class="voucher-note">
      Voucher sẽ chỉ hoạt động khi BE checkout có xử lý mã giảm giá.
    </p>

    <div class="summary-box">
      <div class="summary-line">
        <span>Tạm tính</span>
        <span class="val">{{ formatCurrency(totalAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Giảm giá</span>
        <span class="val">{{ formatCurrency(discountAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Phí vận chuyển</span>
        <span class="val free-ship">Không tính</span>
      </div>

      <div class="summary-line total-line">
        <span>Tổng thanh toán</span>
        <span class="total-val">{{ formatCurrency(finalTotal) }}</span>
      </div>
    </div>

    <div v-if="!canCheckout" class="checkout-warning">
      Có sản phẩm không khả dụng hoặc vượt tồn kho. Vui lòng kiểm tra lại giỏ hàng.
    </div>

    <button
      class="btn-checkout"
      type="button"
      :disabled="!canCheckout"
      @click="$emit('checkout')"
    >
      ĐẶT HÀNG NGAY
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  totalAmount: number;
  discountAmount: number;
  finalTotal: number;
  canCheckout: boolean;
}>();

defineEmits<{
  (e: "checkout"): void;
}>();

const formatCurrency = (val: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(val || 0));
};
</script>

<style scoped>
.cart-right {
  flex: 1;
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
}

.voucher-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.voucher-input {
  flex: 1;
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 0 15px;
  background: #f8fafc;
}

.voucher-icon {
  width: 20px;
  height: 20px;
  color: #b78d52;
  margin-right: 10px;
}

.voucher-input input {
  flex: 1;
  border: none;
  outline: none;
  padding: 12px 0;
  font-size: 14px;
  color: #64748b;
  background: transparent;
}

.btn-apply {
  background: #06132b;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
}

.btn-apply:disabled,
.voucher-input input:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.voucher-note {
  margin: 0 0 20px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}

.summary-box {
  background: #fafbfc;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 15px;
  color: #333;
}

.summary-line .val {
  font-weight: 500;
}

.free-ship {
  color: #38a169;
  font-weight: 700;
}

.total-line {
  border-top: 1px solid #eaeaea;
  padding-top: 15px;
  margin-top: 5px;
  font-weight: 700;
  font-size: 16px;
}

.total-val {
  font-size: 22px;
  color: #b78d52;
}

.checkout-warning {
  background: #fff7ed;
  color: #9a3412;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 14px;
}

.btn-checkout {
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
}

.btn-checkout:hover:not(:disabled) {
  background: #0a1f44;
}

.btn-checkout:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}
</style>