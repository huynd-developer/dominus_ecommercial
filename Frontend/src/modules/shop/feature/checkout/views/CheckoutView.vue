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
    if (cartItems.value.length === 0) { alert("Giỏ hàng trống!"); router.push('/product'); }
  } catch (err) { console.error(err); }
};

const handlePlaceOrder = async () => {
  if (!orderForm.value.customerName.trim() || !orderForm.value.customerPhone.trim() || !orderForm.value.shippingAddress.trim()) {
    alert("Vui lòng điền thông tin giao hàng hợp lệ!"); return;
  }
  if (orderForm.value.requireVat && (!orderForm.value.vatTaxCode.trim() || !orderForm.value.vatEmail.trim() || !orderForm.value.vatCompanyName.trim() || !orderForm.value.vatCompanyAddress.trim())) {
    alert("Vui lòng điền đầy đủ thông tin xuất hóa đơn VAT!"); return;
  }

  isSubmitting.value = true;
  try {
    const submitData = { ...orderForm.value, customerName: orderForm.value.customerName.trim(), customerPhone: orderForm.value.customerPhone.trim(), shippingAddress: orderForm.value.shippingAddress.trim(), note: orderForm.value.note.trim() };
    const res = await axios.post('http://localhost:8080/api/v1/orders/checkout', submitData, { headers: getHeaders() });
    if (submitData.paymentMethod === 'VNPAY' && res.data.paymentUrl) window.location.href = res.data.paymentUrl;
    else { alert("🎉 " + res.data.message); router.push('/product'); }
  } catch (err: any) { alert(err.response?.data || "Lỗi khi đặt hàng!"); } finally { isSubmitting.value = false; }
};

onMounted(loadCartSummary);
</script>

<style scoped>
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }
.main-content.full-width { max-width: 1400px; width: 100%; margin: 40px auto; padding: 0 20px; display: flex; gap: 30px; align-items: flex-start; }
</style>