<template>
  <div class="cart-right">
    <h3 class="summary-title">Tóm tắt đơn hàng</h3>
    <div class="voucher-row">
      <div class="voucher-input">
        <svg class="voucher-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="10" rx="2" ry="2"/><path d="M2 12a2 2 0 010-4m20 4a2 2 0 000-4M10 7v10m4-10v10"/></svg>
        <input type="text" placeholder="Nhập mã Voucher" />
      </div>
      <button class="btn-apply">Áp dụng</button>
    </div>

    <div class="summary-box">
      <div class="summary-line"><span>Tạm tính</span><span class="val">{{ formatCurrency(totalAmount) }}</span></div>
      <div class="summary-line">
        <span>Phí vận chuyển</span>
        <span class="val" :class="{ 'free-ship': shippingFee === 0 }">
          {{ shippingFee === 0 ? 'Miễn phí' : formatCurrency(shippingFee) }}
        </span>
      </div>
      <div class="summary-line total-line"><span>Tổng thanh toán</span><span class="total-val">{{ formatCurrency(finalTotal) }}</span></div>
    </div>

    <button class="btn-checkout" @click="$emit('checkout')">ĐẶT HÀNG NGAY</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ totalAmount: number, shippingFee: number, finalTotal: number }>();
defineEmits(['checkout']);
const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val).replace('VND', '₫');
</script>

<style scoped>
.cart-right { flex: 1; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 30px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); position: sticky; top: 20px; }
.summary-title { font-size: 18px; margin: 0 0 20px 0; color: #06132b; }
.voucher-row { display: flex; gap: 10px; margin-bottom: 25px; }
.voucher-input { flex: 1; display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; }
.voucher-icon { width: 20px; height: 20px; color: #b78d52; margin-right: 10px; }
.voucher-input input { flex: 1; border: none; outline: none; padding: 12px 0; font-size: 14px; color: #333;}
.btn-apply { background: #06132b; color: white; border: none; padding: 0 20px; border-radius: 6px; font-weight: 500; cursor: pointer; }
.summary-box { background: #fafbfc; border: 1px solid #f0f0f0; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
.summary-line { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 15px; color: #333; }
.summary-line .val { font-weight: 500; }
.free-ship { color: #38a169; font-weight: 500; }
.total-line { border-top: 1px solid #eaeaea; padding-top: 15px; margin-top: 5px; font-weight: 700; font-size: 16px; }
.total-val { font-size: 22px; color: #b78d52; }
.btn-checkout { width: 100%; padding: 16px; background: #06132b; color: white; border: none; border-radius: 6px; font-weight: bold; font-size: 15px; cursor: pointer; transition: 0.2s;}
.btn-checkout:hover { background: #0a1f44; }
</style>