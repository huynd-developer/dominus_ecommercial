<!-- <script setup lang="ts">
import { ref } from 'vue';
import { cartStore } from '@/store/cartStore';
import { useRouter } from 'vue-router';

const router = useRouter();
const quantity = ref(1);

// Dữ liệu sản phẩm mẫu (tương ứng với Product Interface trong cartStore của m)
const product = { id: 1, name: 'Bleu de Chanel Parfum', price: 4250000, image: 'https://placehold.co/400' };

const addToCart = () => {
  cartStore.addToCart(product, quantity.value);
  alert("Đã thêm vào giỏ!");
  router.push('/cart'); // Thêm xong chuyển sang giỏ hàng
};
</script>

<template>
  <div class="container py-5">
    <div class="row">
      <div class="col-md-6"><img :src="product.image" class="img-fluid" /></div>
      <div class="col-md-6">
        <h1 class="display-5">{{ product.name }}</h1>
        <h3 class="text-danger">{{ product.price.toLocaleString() }}đ</h3>
        <div class="my-4">
          <input type="number" v-model="quantity" class="form-control w-25 mb-3" min="1" />
          <button class="btn btn-dark btn-lg" @click="addToCart">THÊM VÀO GIỎ HÀNG</button>
        </div>
      </div>
    </div>
  </div>
</template> -->