<script setup lang="ts">
import { useCartStore } from '@/store/cartStore';
import { storeToRefs } from 'pinia'; // Dùng cái này để giữ tính phản ứng
import { useRouter } from 'vue-router';

const router = useRouter();
const cart = useCartStore(); // Khởi tạo store

// Lấy ra các biến reactive từ store
const { items, cartTotal } = storeToRefs(cart);

// Gọi action từ store
const updateItemQty = (item: any, change: number) => {
  // item.id ở đây là CartItemId (do m để trong store), 
  // item.stockQuantity là tồn kho lấy từ DB
  cart.updateQuantity(item.id, item.quantity, change, item.stockQuantity);
};

// Trong script setup
const removeItem = (productId: number | string) => {
  cart.removeItem(productId); // Gọi đúng hàm removeItem của store
};

const goToCheckout = () => {
  router.push('/checkout');
};
</script>

<template>
  <div class="container py-5">
    <h2 class="fw-bold mb-4">Giỏ hàng của bạn</h2>
    
    <div v-if="items.length === 0" class="alert alert-info">
      Giỏ hàng trống. <router-link to="/">Tiếp tục mua sắm</router-link>
    </div>

    <div v-else class="row">
      <div class="col-lg-8">
        <div v-for="item in items" :key="item.product.id" class="card mb-3 p-3">
          <div class="row align-items-center">
            <div class="col-2"><img :src="item.product.image || 'https://placehold.co/100'" class="img-fluid rounded" /></div>
            <div class="col-4"><h5>{{ item.product.name }}</h5></div>
            <div class="col-3">
              <div class="btn-group">
                <button class="btn btn-outline-secondary" @click="updateItemQty(item, -1)">-</button>
                <span class="btn btn-light">{{ item.quantity }}</span>
                <button class="btn btn-outline-secondary" @click="updateItemQty(item, 1)">+</button>
              </div>
            </div>
            <div class="col-2 fw-bold">{{ (item.product.price * item.quantity).toLocaleString() }}đ</div>
            <button class="btn btn-danger" @click="removeItem(item.product.id)">X</button>
          </div>
        </div>
      </div>
      
      <div class="col-lg-4">
        <div class="card p-4">
          <h4>Tổng cộng: {{ cartTotal.toLocaleString() }}đ</h4>
          <button class="btn btn-dark w-100 mt-3" @click="goToCheckout">TIẾN HÀNH THANH TOÁN</button>
        </div>
      </div>
    </div>
  </div>
</template>