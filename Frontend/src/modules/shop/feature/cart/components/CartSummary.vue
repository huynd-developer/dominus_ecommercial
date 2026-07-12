<template>
  <div class="cart-right">
    <h3 class="summary-title">Tóm tắt đơn hàng</h3>

    <div class="voucher-row">
      <div class="voucher-input">
        <svg class="voucher-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="7" width="20" height="10" rx="2" ry="2" />
          <path d="M2 12a2 2 0 010-4m20 4a2 2 0 000-4M10 7v10m4-10v10" />
        </svg>

        <input
          type="text"
          v-model="voucherCode"
          placeholder="Nhập mã giảm giá..."
          @keyup.enter="handleApplyVoucher"
          :disabled="isApplying"
          class="text-uppercase"
        />
      </div>

      <button 
        class="btn-apply" 
        type="button" 
        @click="handleApplyVoucher" 
        :disabled="isApplying || !voucherCode.trim()"
      >
        <span v-if="isApplying" class="spinner-border spinner-border-sm"></span>
        <span v-else>Áp dụng</span>
      </button>
    </div>

    <!-- Thông báo kết quả áp dụng Voucher -->
    <p :class="['voucher-note', messageType]">
      {{ voucherMessage || 'Nhập mã giảm giá để nhận ưu đãi.' }}
    </p>

    <div class="summary-box">
      <div class="summary-line">
        <span>Tạm tính</span>
        <span class="val">{{ formatCurrency(totalAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Giảm giá</span>
        <span class="val discount-val">- {{ formatCurrency(discountAmount) }}</span>
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
import { ref } from 'vue';
import axios from 'axios';

const props = defineProps<{
  totalAmount: number;
  discountAmount: number;
  finalTotal: number;
  canCheckout: boolean;
}>();

// Thêm sự kiện apply-voucher để ném tiền giảm giá ra ngoài cho thằng cha xử lý tính tổng
const emit = defineEmits<{
  (e: "checkout"): void;
  (e: "apply-voucher", discount: number, code: string): void;
}>();

const voucherCode = ref('');
const isApplying = ref(false);
const voucherMessage = ref('');
const messageType = ref('');

// Logic gọi API kiểm tra mã Voucher
const handleApplyVoucher = async () => {
  if (!voucherCode.value.trim()) return;

  isApplying.value = true;
  voucherMessage.value = '';
  messageType.value = '';

  try {
    const token = localStorage.getItem('token');
    
    // Gọi API apply voucher của backend
    const res = await axios.get('http://localhost:8080/api/v1/customer/vouchers/apply', {
      params: {
        code: voucherCode.value.trim(),
        orderTotal: props.totalAmount // Truyền tổng tiền sang để backend check Đơn tối thiểu
      },
      headers: { Authorization: `Bearer ${token}` }
    });

    const { discountAmount, message } = res.data;

    voucherMessage.value = message || 'Áp dụng mã thành công!';
    messageType.value = 'text-success';

    // Bắn sự kiện ra ngoài báo cho CartView biết là được giảm bao nhiêu tiền
    emit('apply-voucher', discountAmount, voucherCode.value.trim());

  } catch (error: any) {
    voucherMessage.value = error.response?.data || 'Không thể áp dụng mã giảm giá này!';
    messageType.value = 'text-danger';

    // Nếu lỗi (mã sai, chưa đủ điều kiện) thì reset tiền giảm về 0
    emit('apply-voucher', 0, '');
  } finally {
    isApplying.value = false;
  }
};

const formatCurrency = (val: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(val || 0));
};
</script>

<style scoped>
/* Giữ nguyên các class cũ của m */
.cart-right { flex: 1; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 30px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03); position: sticky; top: 20px; }
.summary-title { font-size: 18px; margin: 0 0 20px 0; color: #06132b; }
.voucher-row { display: flex; gap: 10px; margin-bottom: 8px; }
.voucher-input { flex: 1; display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; background: #f8fafc; transition: all 0.2s; }
.voucher-input:focus-within { border-color: #b78d52; background: #fff; box-shadow: 0 0 0 3px rgba(183, 141, 82, 0.1); }
.voucher-icon { width: 20px; height: 20px; color: #b78d52; margin-right: 10px; }
.voucher-input input { flex: 1; border: none; outline: none; padding: 12px 0; font-size: 14px; color: #06132b; background: transparent; }
.btn-apply { background: #06132b; color: white; border: none; padding: 0 20px; border-radius: 6px; font-weight: 500; cursor: pointer; transition: 0.2s; min-width: 90px; }
.btn-apply:hover:not(:disabled) { background: #b78d52; }
.btn-apply:disabled, .voucher-input input:disabled { cursor: not-allowed; opacity: 0.7; }
.voucher-note { margin: 0 0 20px; font-size: 12px; color: #94a3b8; line-height: 1.4; font-weight: 500; }

/* 2 class mới t thêm để đổi màu chữ trạng thái */
.text-success { color: #16a34a !important; }
.text-danger { color: #dc2626 !important; }
.discount-val { color: #e53e3e !important; }

.summary-box { background: #fafbfc; border: 1px solid #f0f0f0; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
.summary-line { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 15px; color: #333; }
.summary-line .val { font-weight: 500; }
.free-ship { color: #38a169; font-weight: 700; }
.total-line { border-top: 1px solid #eaeaea; padding-top: 15px; margin-top: 5px; font-weight: 700; font-size: 16px; }
.total-val { font-size: 22px; color: #b78d52; }
.checkout-warning { background: #fff7ed; color: #9a3412; border: 1px solid #fed7aa; border-radius: 8px; padding: 10px 12px; font-size: 13px; line-height: 1.5; margin-bottom: 14px; }
.btn-checkout { width: 100%; padding: 16px; background: #06132b; color: white; border: none; border-radius: 6px; font-weight: bold; font-size: 15px; cursor: pointer; transition: 0.2s; }
.btn-checkout:hover:not(:disabled) { background: #0a1f44; }
.btn-checkout:disabled { background: #94a3b8; cursor: not-allowed; }
</style>