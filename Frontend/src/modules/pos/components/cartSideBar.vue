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
      <button v-if="posStore.cart.length > 0" class="btn btn-sm btn-outline-danger font-xs" @click="posStore.startNewOrder()">
        <i class="bi bi-trash"></i> Hủy đơn
      </button>
    </div>

    <div class="customer-section mb-3 shrink-0">
      <div v-if="!posStore.customer" class="d-flex align-items-center gap-2">
        <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
          <i class="bi bi-person text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
          <input
            type="text"
            v-model="customerPhoneInput"
            placeholder="SĐT khách hàng (bỏ trống = vãng lai)"
            class="promo-input w-100 rounded-3 font-sm text-light transition-all"
            @keydown.enter="handleSearchCustomer"
          />
        </div>
        <button
          type="button"
          @click="handleSearchCustomer"
          :disabled="posStore.isLoading"
          class="apply-promo-btn py-2 px-3 rounded-3 font-sm fw-bold transition-all shrink-0"
        >
          <i v-if="posStore.isLoading" class="bi bi-arrow-repeat spin"></i>
          <span v-else>Tìm</span>
        </button>
      </div>

      <div v-else class="customer-found-pill d-flex align-items-center justify-content-between rounded-3 px-3 py-2">
        <div class="d-flex flex-column">
          <span class="text-light font-sm fw-bold">{{ posStore.customer.name }}</span>
          <span class="text-muted-custom font-xs">
            {{ posStore.customer.phone }} · {{ posStore.customer.loyaltyPoints }} điểm tích lũy
          </span>
        </div>
        <button type="button" class="btn-clear-customer border-0 bg-transparent" @click="posStore.customer = null">
          <i class="bi bi-x-circle text-muted-custom"></i>
        </button>
      </div>
    </div>

    <div v-if="posStore.cart.length === 0" class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-center py-5 empty-state-box">
      <div class="mb-3 empty-icon-wrap">
        <i class="bi bi-bag-x display-4 text-muted-custom opacity-25"></i>
      </div>
      <p class="text-light opacity-70 mb-1 fw-bold font-sm">Giỏ hàng hiện tại đang trống</p>
      <p class="text-muted-custom font-xs mb-0">Vui lòng chọn sản phẩm từ danh sách hoặc quét mã vạch</p>
    </div>

    <div v-else class="flex-grow-1 overflow-y-auto mb-2 pe-1 cart-scroll">
      <div 
        v-for="item in posStore.cart" 
        :key="item.product.sku" 
        class="cart-item-row d-flex align-items-center justify-content-between p-2.5 rounded-3 mb-2 transition-all"
      >
        <div class="overflow-hidden flex-grow-1 pe-2">
          <div class="fw-bold text-truncate font-sm text-light-custom" :title="item.product.name">
            {{ item.product.name }}
          </div>
          <div class="text-gold font-xs fw-bold mt-1">
            {{ formatPrice(item.product.price) }} ₫
          </div>
        </div>

        <div class="d-flex align-items-center gap-2 shrink-0">
          <div class="quantity-controller-box d-flex align-items-center rounded-2">
            <button class="qty-btn" type="button" @click.stop="posStore.updateQuantity(item.product.sku, item.quantity - 1)">
              <i class="bi bi-dash-lg font-xs"></i>
            </button>
            <span class="qty-number px-1 font-xs fw-black">{{ item.quantity }}</span>
            <button class="qty-btn" type="button" @click.stop="posStore.updateQuantity(item.product.sku, item.quantity + 1)">
              <i class="bi bi-plus-lg font-xs"></i>
            </button>
          </div>
          
          <button class="btn-delete-item p-1.5 rounded-2 d-flex align-items-center justify-content-center" type="button" @click.stop="posStore.removeFromCart(item.product.sku)">
            <i class="bi bi-trash3 text-danger font-xs"></i>
          </button>
        </div>
      </div>
    </div>

    <div class="checkout-section pt-3 border-top border-dark-custom mt-auto">
      <div class="d-flex justify-content-between mb-2 text-muted-custom font-xs">
        <span>Tạm tính tổng đơn</span>
        <span class="text-light fw-medium">{{ formatPrice(posStore.totalAmount) }} ₫</span>
      </div>

      <div class="promo-code-container d-flex align-items-center gap-2 mb-3">
        <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
          <i class="bi bi-ticket-perforated text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
          <input 
            type="text" 
            v-model="posStore.voucherCode" 
            placeholder="Mã giảm giá (nếu có)..." 
            class="promo-input w-100 rounded-3 font-sm text-light transition-all"
            :disabled="posStore.cart.length === 0"
          />
        </div>
      </div>
      
      <div class="d-flex justify-content-between align-items-center mb-3">
        <span class="text-light font-sm fw-bold">Khách cần thanh toán</span>
        <h4 class="text-gold mb-0 fw-black tracking-wide fs-4">
          {{ formatPrice(posStore.finalAmount) }} ₫
        </h4>
      </div>

      <div class="d-flex flex-column gap-2 mb-3">
        <button 
          type="button"
          class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all"
          :class="{ 'active': posStore.paymentMethod === 'CASH' }"
          @click="posStore.paymentMethod = 'CASH'"
        >
          <i class="bi bi-cash-coin text-warning fs-5"></i> Tiền mặt
        </button>
        
        <button 
          type="button"
          class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all"
          :class="{ 'active': posStore.paymentMethod === 'VNPAY' }"
          @click="posStore.paymentMethod = 'VNPAY'"
        >
          <i class="bi bi-qr-code-scan text-info fs-5"></i> VNPay (Mã QR)
        </button>
      </div>

      <p v-if="posStore.errorMsg" class="text-danger font-xs mb-2 text-center fw-bold">
        <i class="bi bi-exclamation-circle"></i> {{ posStore.errorMsg }}
      </p>

      <button 
        class="submit-pay-btn w-100 py-3 rounded-3 text-dark fw-black font-sm tracking-wider transition-all" 
        :disabled="posStore.cart.length === 0 || posStore.isLoading"
        @click="posStore.processCheckout"
      >
        <span v-if="posStore.isLoading"><i class="bi bi-arrow-repeat spin"></i> ĐANG XỬ LÝ...</span>
        <span v-else>XÁC NHẬN THANH TOÁN</span>
      </button>

      <button 
        v-if="posStore.vnpayUrl"
        class="w-100 py-2 mt-2 rounded-3 text-light border border-secondary bg-transparent transition-all btn-refresh" 
        @click="posStore.startNewOrder()"
      >
        <i class="bi bi-arrow-clockwise"></i> LÀM MỚI (ĐÓN KHÁCH MỚI)
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const customerPhoneInput = ref('')

const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val || 0)

const handleSearchCustomer = () => {
  if (customerPhoneInput.value) {
    posStore.searchCustomer(customerPhoneInput.value)
  }
}
</script>

<style scoped>
/* Giữ nguyên CSS cũ của mày, thêm class scroll cho giỏ hàng */
.cart-premium-panel { background-color: #0b1120; border: 1px solid #1d2740; height: 100%; }
.text-gold { color: #f3c63f !important; }
.text-light-custom { color: #f1f5f9; }
.text-muted-custom { color: #64748b; }
.border-dark-custom { border-color: #1d2740 !important; }
.icon-wrap { background-color: rgba(243, 198, 63, 0.06); border: 1px solid rgba(243, 198, 63, 0.15); }
.font-sm { font-size: 0.875rem; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900; }
.customer-found-pill { background-color: #131c31; border: 1px solid #222f4f; }
.btn-clear-customer { cursor: pointer; }
.btn-clear-customer:hover i { color: #ef4444 !important; }
.cart-item-row { background-color: #131c31; border: 1px solid #222f4f; }
.cart-item-row:hover { border-color: #2e3f66; background-color: #17223b; }
.quantity-controller-box { background-color: #070c18; border: 1px solid #222f4f; overflow: hidden; padding: 2px; }
.qty-btn { background: transparent; border: none; color: #94a3b8; width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; border-radius: 4px; cursor: pointer; transition: all 0.15s ease; }
.qty-btn:hover { background-color: #222f4f; color: #fff; }
.qty-number { color: #f8fafc; min-width: 22px; text-align: center; }
.btn-delete-item { background: rgba(239, 68, 68, 0.04); border: 1px solid rgba(239, 68, 68, 0.15); cursor: pointer; transition: all 0.15s; }
.btn-delete-item:hover { background-color: rgba(239, 68, 68, 0.15); border-color: rgba(239, 68, 68, 0.3); }
.promo-input { background-color: #131c31; border: 1px solid #222f4f; padding: 10px 12px 10px 40px; outline: none; }
.promo-input:focus { border-color: #384f80; background-color: #17223b; }
.promo-input:disabled { background-color: #0f1626; border-color: #1d2740; color: #475569; cursor: not-allowed; }
.apply-promo-btn { background-color: #1e293b; border: 1px solid #334155; color: #f1f5f9; padding-top: 10px; padding-bottom: 10px; cursor: pointer; }
.apply-promo-btn:hover:not(:disabled) { background-color: #f3c63f; border-color: #f3c63f; color: #0b1120; }
.payment-method-btn { background-color: #131c31; border: 1px solid #222f4f; color: #94a3b8; cursor: pointer; }
.payment-method-btn:hover { border-color: #384f80; color: #f1f5f9; }
.payment-method-btn.active { border-color: #f3c63f; background-color: rgba(243, 198, 63, 0.05); color: #f3c63f; box-shadow: 0 0 12px rgba(243, 198, 63, 0.1); }
.submit-pay-btn { background-image: linear-gradient(to right, #f3c63f, #e0aa14); border: none; cursor: pointer; box-shadow: 0 4px 12px rgba(243, 198, 63, 0.15); }
.submit-pay-btn:hover:not(:disabled) { filter: brightness(1.1); box-shadow: 0 6px 16px rgba(243, 198, 63, 0.25); }
.submit-pay-btn:disabled { background-image: none !important; background-color: #1e2538 !important; color: #475569 !important; box-shadow: none !important; cursor: not-allowed; }
.btn-refresh:hover { background-color: rgba(255, 255, 255, 0.1); }
.transition-all { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.spin { animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* Tùy chỉnh thanh cuộn giỏ hàng */
.cart-scroll::-webkit-scrollbar { width: 4px; }
.cart-scroll::-webkit-scrollbar-track { background: transparent; }
.cart-scroll::-webkit-scrollbar-thumb { background: #1d2740; border-radius: 4px; }
.cart-scroll::-webkit-scrollbar-thumb:hover { background: #384f80; }
</style>