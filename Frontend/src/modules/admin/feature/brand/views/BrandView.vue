<template>
  <div class="brand-workspace p-4">
    
    <div class="card border-0 shadow-sm mb-4 rounded-3">
      <div class="card-body d-flex justify-content-between align-items-center flex-wrap gap-3">
        <h1 class="h3 mb-0 fw-bold text-dark">Danh sách Thương hiệu</h1>
        <BrandFilter v-model="searchQuery" @add="openAdd" />
      </div>
    </div>

    <div class="card border-0 shadow-sm rounded-3 overflow-hidden">
      <div class="card-body p-0">
        <BrandTable 
          :brands="filteredBrands" 
          :loading="store.loading"
          @toggle="handleToggleStatus"
          @edit="openEdit"
          @delete="handleDelete"
        />
      </div>
    </div>

    <BrandFormModal 
      v-if="showModal"
      :is-editing="isEditing"
      :initial-data="activeData"
      @close="showModal = false"
      @save="onSave"
    />
    
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useBrand } from '../composables/useBrand';
import type { Brand } from '../types/brand.type';

import BrandFilter from '../components/BrandFilter.vue';
import BrandTable from '../components/BrandTable.vue';
import BrandFormModal from '../components/BrandFormModal.vue';

const { store, searchQuery, filteredBrands, handleToggleStatus, handleDelete, handleSave } = useBrand();

const showModal = ref(false);
const isEditing = ref(false);
const activeData = ref<Brand>({ name: '', description: '', status: 1 });

const openAdd = () => {
  isEditing.value = false;
  activeData.value = { name: '', description: '', status: 1 };
  showModal.value = true;
};

const openEdit = (brand: Brand) => {
  isEditing.value = true;
  activeData.value = { ...brand };
  showModal.value = true;
};

const onSave = async (data: Brand) => {
  await handleSave(data, isEditing.value);
  showModal.value = false;
};

onMounted(() => {
  store.fetchBrands();
});
</script>

<style scoped>
/* Khóa chặt không gian hiển thị bắt đầu từ sau Sidebar */
.brand-workspace {
  margin-left: 260px; /* Đẩy khoảng cách đúng bằng độ rộng Sidebar để né hoàn toàn */
  background-color: #f8f9fa; /* Nền xám nhạt chuẩn Admin làm nổi bật khung trắng */
  min-height: 100vh;
  min-width: 0;
}

/* Tự động tối ưu nếu màn hình nhỏ/máy tính bảng */
@media (max-width: 991.98px) {
  .brand-workspace {
    margin-left: 0;
    padding: 15px;
  }
}
</style>