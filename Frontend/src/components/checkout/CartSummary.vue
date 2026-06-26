<script setup lang="ts">
import { ref, computed } from 'vue';
// Import đúng 2 biến m đã export ra từ file store của m
import { cartStore, cartTotal } from '../../store/cartStore';

const emit = defineEmits(['submit-order']);

const voucherCode = ref('');
const shippingFee = ref(30000);

// Tính tổng thanh toán (Lấy cartTotal có sẵn của m + phí ship)
const totalPayment = computed(() => cartTotal.value + shippingFee.value);

// Hàm tăng giảm số lượng (Tác động thẳng vào biến quantity vì nó là reactive)
const updateItemQty = (item: any, change: number) => {
  const newQty = item.quantity + change;
  if (newQty >= 1) {
    item.quantity = newQty;
  }
};

const handlePlaceOrder = () => {
  emit('submit-order', {
    voucher: voucherCode.value,
    total: totalPayment.value,
    subTotal: cartTotal.value,
    shippingFee: shippingFee.value
  });
};
</script>

<template>
  <div class="card shadow-sm border-light bg-white sticky-top" style="top: 20px; z-index: 10;">
    <div class="card-body p-4">
      <h5 class="card-title fw-bold mb-4 d-flex align-items-center gap-2">
        <i class="bi bi-bag-heart-fill text-dark"></i> Giỏ hàng của bạn
      </h5>

      <div class="overflow-auto mb-4" style="max-height: 350px;">
        <div v-if="cartStore.items.length === 0" class="text-center text-muted py-4">
          <i class="bi bi-cart-x fs-2 d-block mb-2"></i> Giỏ hàng của bạn chưa có gì.
        </div>

        <div v-for="item in cartStore.items" :key="item.product.id" class="d-flex align-items-center gap-3 border-bottom pb-3 mb-3">
          <img :src="item.product.image || 'https://placehold.co/100'" class="rounded border object-fit-cover" style="width: 65px; height: 65px;" />
          <div class="flex-grow-1">
            <h6 class="fw-bold mb-0 text-dark text-truncate" style="max-width: 220px;">{{ item.product.name }}</h6>
            
            <div class="d-flex justify-content-between align-items-center mt-2">
              <div class="btn-group btn-group-sm border rounded bg-light" role="group">
                <button type="button" @click="updateItemQty(item, -1)" class="btn btn-light px-2 py-0 border-0">-</button>
                <span class="px-3 bg-white text-dark d-flex align-items-center small fw-semibold">{{ item.quantity }}</span>
                <button type="button" @click="updateItemQty(item, 1)" class="btn btn-light px-2 py-0 border-0">+</button>
              </div>
              
              <div class="text-end">
                <span class="fw-bold d-block text-dark small">{{ (item.product.price * item.quantity).toLocaleString('vi-VN') }}đ</span>
                <button type="button" @click="cartStore.removeItem(item.product.id)" class="btn btn-link text-danger p-0 m-0 border-0 text-decoration-none small" style="font-size: 12px;">
                  <i class="bi bi-trash3 small"></i> Xóa
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-group mb-4">
        <input type="text" v-model="voucherCode" class="form-control" placeholder="Mã Voucher ưu đãi" />
        <button class="btn btn-dark fw-bold px-3" type="button">Áp dụng</button>
      </div>

      <div class="border-top pt-3 small text-secondary d-flex flex-column gap-2">
        <div class="d-flex justify-content-between">
          <span>Tạm tính hàng hóa:</span>
          <span class="fw-semibold text-dark">{{ cartTotal.toLocaleString('vi-VN') }}đ</span>
        </div>
        <div class="d-flex justify-content-between">
          <span>Phí vận chuyển:</span>
          <span class="fw-semibold text-dark">{{ shippingFee.toLocaleString('vi-VN') }}đ</span>
        </div>
        <div class="d-flex justify-content-between align-items-center border-top pt-3 mt-2 text-dark">
          <span class="fw-bold fs-6">Tổng tiền thanh toán:</span>
          <span class="fw-bold fs-4" style="color: #A0522D;">{{ totalPayment.toLocaleString('vi-VN') }}đ</span>
        </div>
      </div>

      <button type="button" @click="handlePlaceOrder" class="btn btn-dark w-100 btn-lg py-3 fw-bold mt-4 shadow-sm text-uppercase tracking-wider">
        Tiến hành Đặt hàng
      </button>
    </div>
  </div>
</template>