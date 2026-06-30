<script setup lang="ts">
import type { Brand } from '../types/brand.type';

defineProps<{ brands: Brand[] }>();

const emit = defineEmits<{
  (e: 'toggle', brand: Brand): void;
  (e: 'edit', brand: Brand): void;
  (e: 'delete', id: number | undefined): void;
}>();
</script>

<template>
  <div class="p-0 flex-grow-1 overflow-auto custom-scrollbar">
    <table class="table align-middle mb-0 table-hover border-0">
      
      <thead class="bg-white text-muted small fw-medium tracking-wider text-uppercase" style="position: sticky; top: 0; z-index: 1">
        <tr>
          <th class="ps-4 py-3 border-bottom font-weight-normal" style="width: 10%">Hình ảnh</th>
          <th class="py-3 border-bottom font-weight-normal" style="width: 25%">Tên thương hiệu</th>
          <th class="py-3 border-bottom font-weight-normal" style="width: 35%">Mô tả (Description)</th>
          <th class="py-3 border-bottom font-weight-normal text-center" style="width: 15%">Trạng thái</th>
          <th class="text-end pe-4 py-3 border-bottom font-weight-normal" style="width: 15%">Thao tác</th>
        </tr>
      </thead>
      
      <tbody>
        <tr v-if="brands.length === 0">
          <td colspan="5" class="text-center py-5 text-muted fw-medium">
            Chưa có dữ liệu thương hiệu.
          </td>
        </tr>
        
        <tr v-for="brand in brands" :key="brand.id">
          
          <td class="ps-4 py-3 border-bottom border-light">
            <div class="bg-light text-muted d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; border-radius: 12px; border: 1px dashed #cbd5e1;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <circle cx="8.5" cy="8.5" r="1.5"></circle>
                <polyline points="21 15 16 10 5 21"></polyline>
              </svg>
            </div>
          </td>
          
          <td class="py-3 border-bottom border-light text-dark fw-bold">
            {{ brand.name }}
            <div class="text-muted small fw-normal">ID: #{{ brand.id }}</div>
          </td>
          
          <td class="py-3 border-bottom border-light text-muted">
            {{ brand.description || 'Chưa có mô tả' }}
          </td>
          
          <td class="py-3 border-bottom border-light text-center">
            <div class="form-check form-switch d-inline-block m-0">
              <input 
                class="form-check-input custom-switch" 
                type="checkbox" 
                role="switch"
                :checked="brand.status === 1"
                @change="emit('toggle', brand)"
              />
            </div>
          </td>
          
          <td class="text-end pe-4 py-3 border-bottom border-light">
            <button @click="emit('edit', brand)" class="btn btn-sm btn-light rounded-circle p-2 mx-1 text-primary" title="Sửa">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <button @click="emit('delete', brand.id)" class="btn btn-sm btn-light rounded-circle p-2 mx-1 text-danger" title="Xóa">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          </td>
        </tr>
      </tbody>
      
    </table>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }

.btn-light { 
  background-color: #f8fafc; 
  border: 1px solid transparent; 
  transition: all 0.2s; 
}
.btn-light:hover { 
  background-color: #fff; 
  border-color: #e2e8f0; 
  box-shadow: 0 2px 4px rgba(0,0,0,0.05); 
  transform: translateY(-1px); 
}

.custom-switch { cursor: pointer; }
.custom-switch:focus { box-shadow: none; }
</style>