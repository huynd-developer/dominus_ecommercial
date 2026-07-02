<template>
  <div class="page-wrapper">
    <ShopHeader />
    <main class="main-content full-width">
      <CheckoutForm :form="orderForm" />
      <CheckoutSummary 
        :cartItems="cartItems" 
        :totalItems="totalItems" 
        :totalAmount="totalAmount" 
        :shippingFee="shippingFee" 
        :finalTotal="finalTotal" 
        :isSubmitting="isSubmitting"
        @submit-order="handlePlaceOrder"
        @back="$router.push('/cart')"
      />
    </main>
    <ShopFooter />

    <Teleport to="body">
      <Transition name="fade-modal">
        <div v-if="showSuccessModal" class="premium-modal-overlay">
          <div class="premium-modal-content">
            <div class="success-icon-wrapper">
              <i class="bi bi-check-circle-fill"></i>
            </div>
            <h3 class="modal-title">Thanh toán thành công!</h3>
            <p class="modal-desc">
              Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang được xử lý.
            </p>
            <button type="button" class="btn-continue" @click="goToHome">
              Tiếp tục mua sắm <i class="bi bi-arrow-right ms-2"></i>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';
import CheckoutForm from '../components/CheckoutForm.vue';
import CheckoutSummary from '../components/CheckoutSummary.vue';

const router = useRouter();
const cartItems = ref<any[]>([]);
const isSubmitting = ref(false);

// Biến điều khiển Modal
const showSuccessModal = ref(false);

const orderForm = ref({
  customerName: '', customerPhone: '', shippingAddress: '', note: '', paymentMethod: 'COD',
  requireVat: false, vatTaxCode: '', vatEmail: '', vatCompanyName: '', vatCompanyAddress: ''
});

const getHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });
const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));
const totalItems = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0));
const shippingFee = computed(() => (cartItems.value.length === 0 || totalAmount.value >= 1000000) ? 0 : 30000);
const finalTotal = computed(() => totalAmount.value + shippingFee.value);

const loadCartSummary = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/v1/customer/cart/my-cart', { headers: getHeaders() });
    cartItems.value = res.data;
    if (cartItems.value.length === 0) { router.push('/'); } // Nếu trống thì về Home
  } catch (err) { console.error(err); }
};

const handlePlaceOrder = async () => {
  if (!orderForm.value.customerName.trim() || !orderForm.value.customerPhone.trim() || !orderForm.value.shippingAddress.trim()) {
    alert("Vui lòng điền thông tin giao hàng!"); return;
  }
  
  isSubmitting.value = true;
  try {
    const submitData = { ...orderForm.value };
    const res = await axios.post('http://localhost:8080/api/v1/orders/checkout', submitData, { headers: getHeaders() });
    
    if (submitData.paymentMethod === 'VNPAY' && res.data.paymentUrl) {
      window.location.href = res.data.paymentUrl;
    } else {
      // Thành công thì hiện Modal thay vì alert
      showSuccessModal.value = true;
    }
  } catch (err: any) { 
    alert(err.response?.data || "Lỗi khi đặt hàng!"); 
  } finally { 
    isSubmitting.value = false; 
  }
};

const goToHome = () => {
  showSuccessModal.value = false;
  router.push('/'); // Chuyển hướng về trang chủ
};

onMounted(loadCartSummary);
</script>

<style scoped>
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }
.main-content.full-width { max-width: 1400px; width: 100%; margin: 40px auto; padding: 0 20px; display: flex; gap: 30px; align-items: flex-start; }

/* MODAL STYLES */
.premium-modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 9999;
}
.premium-modal-content {
  background: #ffffff; border-top: 4px solid #b78d52;
  padding: 50px 40px; border-radius: 12px; text-align: center;
  max-width: 480px; width: 90%; box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}
.success-icon-wrapper i {
  font-size: 70px; color: #10b981; margin-bottom: 20px; display: block;
}
.modal-title {
  font-size: 26px; color: #06132b; font-weight: 700; margin-bottom: 15px;
}
.modal-desc {
  color: #64748b; font-size: 15px; line-height: 1.6; margin-bottom: 35px;
}
.btn-continue {
  background: #06132b; color: white; border: none;
  padding: 16px 30px; font-size: 15px; font-weight: 700;
  border-radius: 8px; cursor: pointer; transition: 0.3s; width: 100%;
}
.btn-continue:hover { background: #0a1f44; transform: translateY(-2px); }

/* Animation */
.fade-modal-enter-active, .fade-modal-leave-active { transition: all 0.3s ease; }
.fade-modal-enter-from, .fade-modal-leave-to { opacity: 0; transform: scale(0.9); }
</style>