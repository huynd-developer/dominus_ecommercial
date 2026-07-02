<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter v-if="!isShowingDetail" @filter-change="handleFilterChange" />

      <main class="product-main">
        <div v-if="isLoading" style="padding: 50px; text-align: center; color: #666;">
          Đang tải danh sách sản phẩm...
        </div>
        <ProductGrid v-else-if="!isShowingDetail" :product-list="filteredProductList" @open-detail="handleOpenDetail" />
        <!-- GỌI COMPONENT CHI TIẾT -->
        <ProductDetail v-else :product="activeProduct" @back="handleBackToList" @buy-now="handleBuyNow" />
      </main>
    </div>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import SidebarFilter from '@/modules/shop/feature/product/components/SidebarFilter.vue';
import ProductGrid from '@/modules/shop/feature/product/components/ProductGrid.vue';
import ProductDetail from '@/modules/shop/feature/product/components/ProductDetail.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';

const router = useRouter();

const isShowingDetail = ref(false);
const activeProduct = ref<any>(null);
const productList = ref<any[]>([]);
const isLoading = ref(false);

const activeFilters = ref<any>({
  genders: [],
  fragranceFamilies: [],
  capacities: [],
  bottleTypes: [],
  concentrations: []
});

const handleFilterChange = (filters: any) => {
  activeFilters.value = filters;
};

// HÀM "MÓC" DUNG TÍCH THÔNG MINH TỪ API
const extractCapacity = (v: any) => {
  if (!v) return 'N/A';
  let val = null;
  // Bắt mọi trường hợp BE có thể trả về (Object, field rời, số Float)
  if (v.capacity && v.capacity.value) val = v.capacity.value;
  else if (v.capacityValue) val = v.capacityValue;
  else if (v.volume) val = v.volume;
  else if (v.capacity != null && typeof v.capacity !== 'object') val = v.capacity;

  if (val != null) {
    // Ép kiểu parseFloat để nếu BE trả 50.0 thì nó biến thành 50ml
    return `${parseFloat(val)}ml`;
  }
  return v.capacity?.name || 'N/A';
};

// 1. FETCH DANH SÁCH SẢN PHẨM & MAP DỮ LIỆU
const fetchProducts = async () => {
  try {
    isLoading.value = true;
    const res = await axios.get('http://localhost:8080/api/products');
    const rawData = res.data.data?.content || res.data.data || [];
    
    productList.value = rawData.map((item: any) => {
      // Dịch giới tính[cite: 15]
      let genderStr = '';
      if (item.gender === 1) genderStr = 'Nam';
      else if (item.gender === 2) genderStr = 'Nữ';
      else if (item.gender === 0) genderStr = 'Unisex';
      else genderStr = typeof item.gender === 'object' ? item.gender?.name : (item.gender || '');

      const rawVariants = item.variants || item.productVariants || item.productVariantList || [];
      const mappedVariants = rawVariants.map((v: any) => ({
        ...v, 
        capacity: extractCapacity(v), 
        price: v.price || 0 
      }));

      let mappedScents: string[] = [];
      if (item.fragranceFamily) mappedScents.push(item.fragranceFamily.name || item.fragranceFamily.Name || '');
      else if (Array.isArray(item.fragranceFamilies)) mappedScents = item.fragranceFamilies.map((f: any) => typeof f === 'object' ? f.name : f);

      return {
        ...item,
        id: item.id || item.Id,
        name: item.name || item.Name || 'Sản phẩm',
        image: item.imageUrl || item.ImageUrl || item.image || 'https://via.placeholder.com/300x300?text=No+Image',
        brand: typeof item.brand === 'object' ? item.brand?.name : (item.brandName || item.brand || 'Premium'),
        price: mappedVariants.length > 0 ? mappedVariants[0].price : (item.price || 0),
        variants: mappedVariants,
        scents: mappedScents.filter(Boolean),
        gender: genderStr,
        // Bổ sung map Nồng độ và Loại chai để bộ lọc có dữ liệu so sánh
        concentration: typeof item.concentration === 'object' ? item.concentration?.name : (item.concentrationName || item.concentration || ''),
        bottleType: typeof item.bottleType === 'object' ? item.bottleType?.name : (item.bottleTypeName || item.bottleType || '')
      };
    });
  } catch (error) {
    console.error("Lỗi fetch API List:", error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => fetchProducts());

// 2. BỘ LỌC (ĐÃ BỔ SUNG FULL 5 LOẠI LỌC)
const filteredProductList = computed(() => {
  if (!productList.value) return [];
  return productList.value.filter((product: any) => {
    
    // 1. Lọc Giới tính[cite: 15]
    if (activeFilters.value.genders?.length > 0) {
      const filters = activeFilters.value.genders.map((g:string)=>g.toLowerCase());
      const pGender = (product.gender || '').toLowerCase();
      if (!filters.includes(pGender)) return false;
    }

    // 2. Lọc Nhóm hương[cite: 15]
    if (activeFilters.value.fragranceFamilies?.length > 0) {
      const filters = activeFilters.value.fragranceFamilies.map((f:string)=>f.toLowerCase());
      const pScents = product.scents.map((s:string)=>s.toLowerCase());
      if (!filters.some((f: string) => pScents.includes(f))) return false;
    }

    // 3. Lọc Nồng độ (Bổ sung mới)
    if (activeFilters.value.concentrations?.length > 0) {
      const filters = activeFilters.value.concentrations.map((c:string)=>c.toLowerCase());
      const pConc = (product.concentration || '').toLowerCase();
      if (!filters.includes(pConc)) return false;
    }

    // 4. Lọc Loại chai (Bổ sung mới)
    if (activeFilters.value.bottleTypes?.length > 0) {
      const filters = activeFilters.value.bottleTypes.map((b:string)=>b.toLowerCase());
      const pBottle = (product.bottleType || '').toLowerCase();
      // Nếu sản phẩm không khớp loại chai, và các variants cũng không khớp thì loại
      let hasMatch = filters.includes(pBottle);
      if (!hasMatch && product.variants) {
         const vBottles = product.variants.map((v:any)=>(typeof v.bottleType === 'object' ? v.bottleType?.name : (v.bottleType || '')).toLowerCase());
         if (filters.some((f: string) => vBottles.includes(f))) hasMatch = true;
      }
      if (!hasMatch) return false;
    }

    // 5. Lọc Dung tích[cite: 15]
    if (activeFilters.value.capacities?.length > 0) {
      if (product.variants && product.variants.length > 0) {
        // Xóa chữ 'ml' khi so sánh để tránh lệch format
        const filters = activeFilters.value.capacities.map((c:string)=>c.toLowerCase().replace('ml','').trim());
        const pCaps = product.variants.map((v:any)=>(v.capacity || '').toLowerCase().replace('ml','').trim());
        if (!filters.some((c: string) => pCaps.includes(c))) return false;
      } else {
        return false; 
      }
    }

    return true; 
  });
});

// 3. MỞ CHI TIẾT & GỌI API VARIANTS
const handleOpenDetail = async (item: any) => {
  activeProduct.value = { ...item };
  isShowingDetail.value = true;
  window.scrollTo({ top: 0, behavior: 'smooth' });

  try {
    const variantRes = await axios.get(`http://localhost:8080/api/products/${item.id}/variants`);
    const rawVariants = variantRes.data.data || [];

    if (rawVariants.length > 0) {
      // 1. Map dữ liệu trước
      let mappedVariants = rawVariants.map((v: any) => ({
        ...v, 
        capacity: extractCapacity(v),
        price: v.price || 0 
      }));

      // 2. SẮP XẾP TỪ BÉ ĐẾN LỚN
      mappedVariants.sort((a: any, b: any) => {
        // Cắt chữ 'ml' đi, biến thành số để so sánh (VD: "30ml" -> 30)
        const valA = parseFloat(a.capacity.replace('ml', '')) || 0;
        const valB = parseFloat(b.capacity.replace('ml', '')) || 0;
        return valA - valB; 
      });

      // 3. Gán lại vào activeProduct
      activeProduct.value.variants = mappedVariants;
      activeProduct.value.price = activeProduct.value.variants[0].price;
    }
  } catch (error) {
    console.error("Lỗi fetch API Variants:", error);
  }
};

const handleBackToList = () => {
  isShowingDetail.value = false;
  activeProduct.value = null;
};

const handleBuyNow = () => router.push('/checkout');
</script>

<style scoped>
.page-wrapper { font-family: 'Inter', sans-serif; background-color: #ffffff; min-height: 100vh; }
.product-layout { display: flex; flex-direction: row; gap: 40px; max-width: 1320px; margin: 40px auto; padding: 0 20px; }
.product-main { flex: 1; min-width: 0; }
:deep(.sidebar-filter) { width: 280px; flex-shrink: 0; }
@media (max-width: 991px) { .product-layout { flex-direction: column; } :deep(.sidebar-filter) { width: 100%; } }
</style>