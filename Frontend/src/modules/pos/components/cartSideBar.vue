<template>
  <div class="cart-premium-panel rounded-3 d-flex flex-column h-100 p-3 select-none">
    <div class="d-flex align-items-center justify-content-between border-bottom border-dark-custom pb-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <div class="icon-wrap rounded p-2 d-flex align-items-center justify-center">
          <i class="bi bi-cart3 text-gold fs-5"></i>
        </div>
        <div>
          <h5 class="mb-0 text-light fw-bold font-sm tracking-wide">Giỏ hàng</h5>
          <span class="text-muted-custom font-xs">{{ posStore.cart.length }} dòng mặt hàng</span>
        </div>
      </div>
    </div>

    <div v-if="posStore.cart.length === 0" class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-center py-5 empty-state-box">
      <div class="mb-3 empty-icon-wrap">
        <i class="bi bi-bag-x display-4 text-muted-custom opacity-25"></i>
      </div>
      <p class="text-light opacity-70 mb-1 fw-bold font-sm">Giỏ hàng hiện tại đang trống</p>
      <p class="text-muted-custom font-xs mb-0">Vui lòng chọn sản phẩm từ danh sách bên trái</p>
    </div>

    <el-scrollbar v-else class="flex-grow-1 mb-2 pe-1">
      <div 
        v-for="item in posStore.cart" 
        :key="item.id" 
        class="cart-item-row d-flex align-items-center justify-content-between p-2.5 rounded-3 mb-2 transition-all"
      >
        <div class="overflow-hidden flex-grow-1 pe-2">
          <div class="fw-bold text-truncate font-sm text-light-custom" :title="item.name">
            {{ item.name }}
          </div>
          <div class="text-gold font-xs fw-bold mt-1">
            {{ formatPrice(item.price) }} ₫
          </div>
        </div>

        <div class="d-flex align-items-center gap-2 shrink-0">
          <div class="quantity-controller-box d-flex align-items-center rounded-2">
            <button class="qty-btn" type="button" @click.stop="posStore.decreaseQuantity(item.id)">
              <i class="bi bi-dash-lg font-xs"></i>
            </button>
            <span class="qty-number px-1 font-xs fw-black">{{ item.quantity }}</span>
            <button class="qty-btn" type="button" @click.stop="posStore.addToCart(item)">
              <i class="bi bi-plus-lg font-xs"></i>
            </button>
          </div>
          
          <button class="btn-delete-item p-1.5 rounded-2 d-flex align-items-center justify-content-center" type="button" @click.stop="posStore.removeItem(item.id)">
            <i class="bi bi-trash3 text-danger font-xs"></i>
          </button>
        </div>
      </div>
    </el-scrollbar>

    <div class="checkout-section pt-3 border-top border-dark-custom mt-auto">
      <div class="d-flex justify-content-between mb-2 text-muted-custom font-xs">
        <span>Tạm tính tổng đơn</span>
        <span class="text-light fw-medium">{{ formatPrice(posStore.cartTotal) }} ₫</span>
      </div>

      <div class="promo-code-container d-flex align-items-center gap-2 mb-3">
        <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
          <i class="bi bi-ticket-perforated text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
          <input 
            type="text" 
            v-model="promoCode" 
            placeholder="Nhập mã giảm giá..." 
            class="promo-input w-100 rounded-3 font-sm text-light transition-all"
            :disabled="posStore.cart.length === 0"
          />
        </div>
        <button 
          type="button" 
          @click="handleApplyPromo"
          class="apply-promo-btn py-2 px-3 rounded-3 font-sm fw-bold transition-all shrink-0"
          :disabled="!promoCode.trim() || posStore.cart.length === 0"
        >
          Áp dụng
        </button>
      </div>
      
      <div class="d-flex justify-content-between align-items-center mb-3">
        <span class="text-light font-sm fw-bold">Khách cần thanh toán</span>
        <h4 class="text-gold mb-0 fw-black tracking-wide fs-4">
          {{ formatPrice(posStore.cartTotal) }} ₫
        </h4>
      </div>

      <div class="d-flex flex-column gap-2 mb-3">
        <button 
          type="button"
          class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all"
          :class="{ 'active': activePayment === 'cash' }"
          @click="activePayment = 'cash'"
        >
          <i class="bi bi-cash-coin text-warning fs-5"></i> Tiền mặt
        </button>
        
        <button 
          type="button"
          class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all"
          :class="{ 'active': activePayment === 'transfer' }"
          @click="activePayment = 'transfer'"
        >
          <i class="bi bi-qr-code-scan text-info fs-5"></i> Chuyển khoản ngân hàng
        </button>
      </div>

      <button 
        class="submit-pay-btn w-100 py-3 rounded-3 text-dark fw-black font-sm tracking-wider transition-all" 
        :disabled="posStore.cart.length === 0"
      >
        XÁC NHẬN THANH TOÁN
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const activePayment = ref('cash')
const promoCode = ref('')

const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val || 0)

const handleApplyPromo = () => {
  if (!promoCode.value.trim()) return
  // Viết thêm logic xử lý API hoặc đẩy mã vào posStore tại đây
  console.log('Đang áp dụng mã giảm giá:', promoCode.value)
}
</script>

<style scoped>
/* PALETTE MÀU DARK PREMIUM */
.cart-premium-panel { 
  background-color: #0b1120; 
  border: 1px solid #1d2740; 
  height: 100%; 
}
.text-gold { color: #f3c63f !important; }
.text-light-custom { color: #f1f5f9; }
.text-muted-custom { color: #64748b; }
.border-dark-custom { border-color: #1d2740 !important; }
.icon-wrap { background-color: rgba(243, 198, 63, 0.06); border: 1px solid rgba(243, 198, 63, 0.15); }

/* Typography chỉn chu */
.font-sm { font-size: 0.875rem; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900; }

/* DANH SÁCH MÓN */
.cart-item-row { 
  background-color: #131c31; 
  border: 1px solid #222f4f; 
}
.cart-item-row:hover {
  border-color: #2e3f66;
  background-color: #17223b;
}

/* CỤM ĐIỀU KHIỂN TĂNG GIẢM SỐ LƯỢNG */
.quantity-controller-box { 
  background-color: #070c18; 
  border: 1px solid #222f4f; 
  overflow: hidden; 
  padding: 2px;
}
.qty-btn { 
  background: transparent; 
  border: none; 
  color: #94a3b8; 
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer; 
  transition: all 0.15s ease; 
}
.qty-btn:hover { 
  background-color: #222f4f; 
  color: #fff; 
}
.qty-number { 
  color: #f8fafc; 
  min-width: 22px; 
  text-align: center; 
}

/* NÚT THÙNG RÁC */
.btn-delete-item { 
  background: rgba(239, 68, 68, 0.04); 
  border: 1px solid rgba(239, 68, 68, 0.15); 
  cursor: pointer; 
  transition: all 0.15s; 
}
.btn-delete-item:hover { 
  background-color: rgba(239, 68, 68, 0.15); 
  border-color: rgba(239, 68, 68, 0.3);
}

/* ĐƯỢC THÊM: STYLE KHU VỰC Ô NHẬP VOUCHER */
.promo-input {
  background-color: #131c31;
  border: 1px solid #222f4f;
  padding: 10px 12px 10px 40px; /* Thụt lề trái để nhường chỗ cho icon */
  outline: none;
}
.promo-input:focus {
  border-color: #384f80;
  background-color: #17223b;
}
.promo-input:disabled {
  background-color: #0f1626;
  border-color: #1d2740;
  color: #475569;
  cursor: not-allowed;
}

/* Nút áp dụng mã */
.apply-promo-btn {
  background-color: #1e293b;
  border: 1px solid #334155;
  color: #f1f5f9;
  padding-top: 10px;
  padding-bottom: 10px;
  cursor: pointer;
}
.apply-promo-btn:hover:not(:disabled) {
  background-color: #f3c63f;
  border-color: #f3c63f;
  color: #0b1120;
}
.apply-promo-btn:disabled {
  background-color: #131c31;
  border-color: #1d2740;
  color: #475569;
  cursor: not-allowed;
}

/* PHƯƠNG THỨC THANH TOÁN TO RÕ RÀNG */
.payment-method-btn { 
  background-color: #131c31; 
  border: 1px solid #222f4f; 
  color: #94a3b8; 
  cursor: pointer; 
}
.payment-method-btn:hover { 
  border-color: #384f80; 
  color: #f1f5f9; 
}
.payment-method-btn.active {
  border-color: #f3c63f;
  background-color: rgba(243, 198, 63, 0.05);
  color: #f3c63f;
  box-shadow: 0 0 12px rgba(243, 198, 63, 0.1);
}

/* NÚT XÁC NHẬN CHỐT ĐƠN */
.submit-pay-btn { 
  background-image: linear-gradient(to right, #f3c63f, #e0aa14);
  border: none; 
  cursor: pointer; 
  box-shadow: 0 4px 12px rgba(243, 198, 63, 0.15);
}
.submit-pay-btn:hover:not(:disabled) { 
  filter: brightness(1.1);
  box-shadow: 0 6px 16px rgba(243, 198, 63, 0.25);
}
.submit-pay-btn:active:not(:disabled) {
  transform: scale(0.99);
}
.submit-pay-btn:disabled { 
  background-image: none !important;
  background-color: #1e2538 !important; 
  color: #475569 !important; 
  box-shadow: none !important;
  cursor: not-allowed; 
}

/* Hiệu ứng chuyển động mượt */
.transition-all {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>