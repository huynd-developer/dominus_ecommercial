<script setup lang="ts">
import { ref, onMounted } from 'vue';
// Sửa lại cách gọi store
import { cartStore } from '../../store/cartStore';

import CheckoutForm from '../checkout/CheckoutForm.vue';
import CartSummary from '../checkout/CartSummary.vue';

const checkoutData = ref({
  fullName: '',
  phone: '',
  address: '',
  paymentMethod: 'COD'
});

const processOrder = (summaryData: any) => {
  if (!checkoutData.value.fullName || !checkoutData.value.phone || !checkoutData.value.address) {
    alert("Vui lòng điền đầy đủ thông tin nhận hàng!");
    return;
  }

  // Sửa lại thành cartStore.items.length
  if (cartStore.items.length === 0) {
    alert("Giỏ hàng của bạn đang trống!");
    return;
  }

  const finalPayload = {
    ...checkoutData.value,
    ...summaryData
  };

  console.log("Dữ liệu gửi xuống Backend API Đặt Hàng:", finalPayload);
  alert("Gửi đơn hàng thành công! Kiểm tra log console.");
};
</script>

<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center border-b pb-3 mb-4">
      <h1 class="h2 font-serif text-uppercase tracking-wide text-dark m-0" style="font-family: Georgia, serif; color: #85586F !important;">
        LUXE PARFUM
      </h1>
      <div class="text-muted small d-none d-sm-block">
        <span class="me-3"><i class="bi bi-patch-check-fill text-success"></i> Sản phẩm chính hãng</span>
        <span><i class="bi bi-shield-lock-fill text-success"></i> Thanh toán bảo mật 100%</span>
      </div>
    </div>

    <div class="row g-4">
      <div class="col-lg-7">
        <CheckoutForm v-model="checkoutData" />
      </div>

      <div class="col-lg-5">
        <CartSummary @submit-order="processOrder" />
      </div>
    </div>
  </div>
</template>