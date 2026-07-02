<template>
  <div class="page-wrapper">
    <ShopHeader />
    <main class="single-product-main">
      <div v-if="isLoading" style="padding: 100px; text-align: center; color: #666;">
        Đang tải thông tin sản phẩm...
      </div>
      <div v-else-if="!product" style="padding: 100px; text-align: center; color: #e53e3e;">
        Không tìm thấy sản phẩm!
      </div>
      <!-- Gọi giao diện Chi tiết -->
      <ProductDetail v-else :product="product" @back="goBack" @buy-now="handleBuyNow" />
    </main>
    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import ProductDetail from '@/modules/shop/feature/product/components/ProductDetail.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';

const route = useRoute();
const router = useRouter();

const product = ref<any>(null);
const isLoading = ref(true);

const extractCapacity = (v: any) => {
  if (!v) return 'N/A';
  let val = null;
  if (v.capacity && v.capacity.value) val = v.capacity.value;
  else if (v.capacityValue) val = v.capacityValue;
  else if (v.volume) val = v.volume;
  else if (v.capacity != null && typeof v.capacity !== 'object') val = v.capacity;
  if (val != null) return `${parseFloat(val)}ml`;
  return v.capacity?.name || 'N/A';
};

onMounted(async () => {
  // Lấy ID từ URL (vd: /product/4 -> id = 4)
  const productId = route.params.id; 
  
  try {
    // 1. Gọi API lấy thông tin cơ bản
    const res = await axios.get(`http://localhost:8080/api/products/${productId}`);
    const p = res.data.data;

    // 2. Gọi API lấy dung tích (Variants)
    const varRes = await axios.get(`http://localhost:8080/api/products/${productId}/variants`);
    const rawVariants = varRes.data.data || [];
    
    let mappedVariants = [];
    if (rawVariants.length > 0) {
      mappedVariants = rawVariants.map((v: any) => ({
        ...v, 
        capacity: extractCapacity(v),
        price: v.price || 0 
      }));
      // Sắp xếp dung tích từ bé đến lớn
      mappedVariants.sort((a: any, b: any) => {
        const valA = parseFloat(a.capacity.replace('ml', '')) || 0;
        const valB = parseFloat(b.capacity.replace('ml', '')) || 0;
        return valA - valB; 
      });
    }

    // 3. Gộp lại
    product.value = {
      ...p,
      id: p.id,
      name: p.name,
      brand: typeof p.brand === 'object' ? p.brand?.name : (p.brand || 'Premium'),
      image: p.imageUrl || p.image || 'https://via.placeholder.com/300x300?text=No+Image',
      description: p.description,
      rating: p.rating || 5.0,
      variants: mappedVariants,
      price: mappedVariants.length > 0 ? mappedVariants[0].price : (p.price || 0)
    };
  } catch (error) {
    console.error("Lỗi tải chi tiết sản phẩm:", error);
  } finally {
    isLoading.value = false;
  }
});

const goBack = () => {
  router.push({ name: 'ProductList' }); 
};
const handleBuyNow = () => router.push('/checkout');
</script>

<style scoped>
.page-wrapper { font-family: 'Inter', sans-serif; background-color: #ffffff; min-height: 100vh; }
.single-product-main { max-width: 1320px; margin: 40px auto; padding: 0 20px; min-height: 50vh; }
</style>