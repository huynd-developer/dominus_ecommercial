<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="main-content full-width">
      <CartItemList 
        :cartItems="cartItems" 
        :isLoading="isLoading" 
        @update-qty="updateQty" 
        @remove-item="removeItem" 
      />
      <CartSummary 
        v-if="cartItems.length > 0" 
        :totalAmount="totalAmount" 
        :shippingFee="shippingFee" 
        :finalTotal="finalTotal" 
        @checkout="$router.push('/checkout')" 
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
import CartItemList from '../components/CartItemList.vue';
import CartSummary from '../components/CartSummary.vue';

const router = useRouter();
const cartItems = ref<any[]>([]); 
const isLoading = ref<boolean>(true);

// Lấy Token từ localStorage để gửi kèm API (Yêu cầu user đã đăng nhập)
const getHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });

// Tính toán tiền nong
const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));
const shippingFee = computed(() => (cartItems.value.length === 0 || totalAmount.value >= 1000000) ? 0 : 30000);
const finalTotal = computed(() => totalAmount.value + shippingFee.value);

// API: Lấy giỏ hàng
const loadCart = async () => { 
  try { 
    const res = await axios.get('http://localhost:8080/api/v1/customer/cart/my-cart', { headers: getHeaders() }); 
    cartItems.value = res.data; 
  } catch (err) { 
    console.error('Lỗi tải giỏ hàng:', err); 
  } finally { 
    isLoading.value = false; 
  } 
};

// API: Cập nhật số lượng
const updateQty = async (item: any, newQty: number) => { 
  if (newQty < 1 || newQty > item.stockQuantity) return; 
  try { 
    await axios.put(`http://localhost:8080/api/v1/customer/cart/update/${item.cartItemId}`, { quantity: newQty }, { headers: getHeaders() }); 
    item.quantity = newQty; 
  } catch (err) { 
    alert('Lỗi cập nhật số lượng! Kiểm tra lại backend.'); 
  } 
};

// API: Xóa sản phẩm
const removeItem = async (cartItemId: number) => { 
  if (!confirm('Xóa sản phẩm này khỏi giỏ hàng?')) return; 
  try { 
    await axios.delete(`http://localhost:8080/api/v1/customer/cart/remove/${cartItemId}`, { headers: getHeaders() }); 
    cartItems.value = cartItems.value.filter(i => i.cartItemId !== cartItemId); 
  } catch (err) { 
    alert('Không thể xóa sản phẩm! Kiểm tra lại backend.'); 
  } 
};

// Gọi API khi vừa vào trang
onMounted(() => {
  loadCart();
});
</script>

<style scoped>
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }
.main-content.full-width { max-width: 1400px; width: 100%; margin: 40px auto; padding: 0 20px; display: flex; gap: 30px; align-items: flex-start; }
</style>