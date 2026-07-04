<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter @filter-change="handleFilterChange" />

      <main class="product-main">
        <div v-if="isLoading" style="padding: 50px; text-align: center; color: #666;">
          Đang tải danh sách sản phẩm...
        </div>

        <div v-else>
          <!-- ÉP VUE VẼ LẠI COMPONENT MỖI KHI ĐỔI TRANG BẰNG :key -->
          <ProductGrid 
            :key="`grid-${currentPage}`"
            :product-list="paginatedProductList" 
            @open-detail="handleOpenDetail" 
          />

          <!-- 2. GIAO DIỆN PHÂN TRANG (PAGINATION) -->
          <div class="pagination-wrapper mt-5 mb-5 d-flex justify-content-center" v-if="totalPages > 1">
            <ul class="aura-pagination d-flex gap-2 list-unstyled mb-0">
              <!-- Nút Previous -->
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <button type="button" class="page-link" @click.prevent="changePage(currentPage - 1)" :disabled="currentPage === 1">
                  <i class="bi bi-chevron-left" style="font-size: 12px;"></i>
                </button>
              </li>

              <!-- Danh sách các trang (1, 2, 3...) -->
              <li class="page-item" v-for="page in totalPages" :key="page" :class="{ active: currentPage === page }">
                <button type="button" class="page-link" @click.prevent="changePage(page)">
                  {{ page }}
                </button>
              </li>

              <!-- Nút Next -->
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <button type="button" class="page-link" @click.prevent="changePage(currentPage + 1)" :disabled="currentPage === totalPages">
                  <i class="bi bi-chevron-right" style="font-size: 12px;"></i>
                </button>
              </li>
            </ul>
          </div>
        </div>
      </main>
    </div>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import SidebarFilter from '@/modules/shop/feature/product/components/SidebarFilter.vue';
import ProductGrid from '@/modules/shop/feature/product/components/ProductGrid.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';

const router = useRouter();
const route = useRoute();

const productList = ref<any[]>([]);
const isLoading = ref(false);

// === STATE CHO PHÂN TRANG ===
const currentPage = ref(1);
const itemsPerPage = ref(6); // Số sản phẩm trên 1 trang (M có thể đổi thành 12, 16 tùy ý)

const activeFilters = ref<any>({
  genders: [],
  fragranceFamilies: [],
  capacities: [],
  bottleTypes: [],
  concentrations: []
});

const handleFilterChange = (filters: any) => {
  activeFilters.value = filters;
  currentPage.value = 1; // RESET về trang 1 khi người dùng đổi bộ lọc
};

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

const fetchProducts = async () => {
  try {
    isLoading.value = true;
    const keyword = route.query.keyword || ''; 
    const res = await axios.get('http://localhost:8080/api/products', {
      params: { 
        keyword: keyword,
        size: 100 // Ép Backend trả về nhiều để Front-end tự phân trang
      } 
    });
    
    const rawData = res.data.data?.content || res.data.data || [];
    
    productList.value = rawData.map((item: any) => {
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

onMounted(() => {
  fetchProducts();
});

watch(() => route.query.keyword, () => {
  currentPage.value = 1; 
  fetchProducts();
});

// BỘ LỌC GỐC
const filteredProductList = computed(() => {
  if (!productList.value) return [];
  return productList.value.filter((product: any) => {
    if (activeFilters.value.genders?.length > 0) {
      const filters = activeFilters.value.genders.map((g:string)=>g.toLowerCase());
      const pGender = (product.gender || '').toLowerCase();
      if (!filters.includes(pGender)) return false;
    }
    if (activeFilters.value.fragranceFamilies?.length > 0) {
      const filters = activeFilters.value.fragranceFamilies.map((f:string)=>f.toLowerCase());
      const pScents = product.scents.map((s:string)=>s.toLowerCase());
      if (!filters.some((f: string) => pScents.includes(f))) return false;
    }
    if (activeFilters.value.concentrations?.length > 0) {
      const filters = activeFilters.value.concentrations.map((c:string)=>c.toLowerCase());
      const pConc = (product.concentration || '').toLowerCase();
      if (!filters.includes(pConc)) return false;
    }
    if (activeFilters.value.bottleTypes?.length > 0) {
      const filters = activeFilters.value.bottleTypes.map((b:string)=>b.toLowerCase());
      const pBottle = (product.bottleType || '').toLowerCase();
      let hasMatch = filters.includes(pBottle);
      if (!hasMatch && product.variants) {
         const vBottles = product.variants.map((v:any)=>(typeof v.bottleType === 'object' ? v.bottleType?.name : (v.bottleType || '')).toLowerCase());
         if (filters.some((f: string) => vBottles.includes(f))) hasMatch = true;
      }
      if (!hasMatch) return false;
    }
    if (activeFilters.value.capacities?.length > 0) {
      if (product.variants && product.variants.length > 0) {
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

// === LOGIC TÍNH TOÁN PHÂN TRANG ===
const totalPages = computed(() => {
  return Math.ceil(filteredProductList.value.length / itemsPerPage.value);
});

const paginatedProductList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return filteredProductList.value.slice(start, end);
});

// Hàm chuyển trang
const changePage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const handleOpenDetail = (item: any) => {
  router.push({ name: 'ProductDetail', params: { id: item.id } }); 
};
</script>

<style scoped>
.page-wrapper { font-family: 'Inter', sans-serif; background-color: #ffffff; min-height: 100vh; }
.product-layout { display: flex; flex-direction: row; gap: 40px; max-width: 1320px; margin: 40px auto; padding: 0 20px; }
.product-main { flex: 1; min-width: 0; }
:deep(.sidebar-filter) { width: 280px; flex-shrink: 0; }
@media (max-width: 991px) { .product-layout { flex-direction: column; } :deep(.sidebar-filter) { width: 100%; } }

/* === CSS CHO NÚT PHÂN TRANG === */
.aura-pagination .page-link {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid #e0e0e0;
  background-color: #ffffff;
  color: #333333;
  font-weight: 500;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s ease;
  padding: 0;
}

.aura-pagination .page-link:hover:not([disabled]) {
  background-color: #f5f5f5;
  border-color: #cccccc;
}

.aura-pagination .page-item.active .page-link {
  background-color: #07172f;
  color: #ffffff;
  border-color: #07172f;
  box-shadow: 0 4px 10px rgba(7, 23, 47, 0.2);
}

.aura-pagination .page-link[disabled] {
  background-color: #f9f9f9;
  color: #bbbbbb;
  cursor: not-allowed;
  border-color: #eeeeee;
}
</style>