<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0" style="background-color: #fff;">
      <thead class="text-secondary" style="border-bottom: 2px solid #f0f0f0;">
        <tr>
          <th scope="col" class="py-3 px-4" style="width: 100px;">Hình ảnh</th>
          <th scope="col" class="py-3">Tên thương hiệu</th>
          <th scope="col" class="py-3">Mô tả</th>
          <th scope="col" class="py-3 text-center" style="width: 150px;">Trạng thái</th>
          <th scope="col" class="py-3 text-center" style="width: 180px;">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="brand in brands" :key="brand.id">
          <td class="px-4 py-2">
            <img 
              v-if="brand.logoUrl" 
              :src="brand.logoUrl" 
              alt="Logo" 
              class="rounded border" 
              style="width: 60px; height: 60px; object-fit: cover;"
            >
            <div 
              v-else 
              class="rounded border bg-light d-flex align-items-center justify-content-center text-muted" 
              style="width: 60px; height: 60px; font-size: 12px;"
            >
              Trống
            </div>
          </td>

          <td class="py-3 fw-medium text-dark">{{ brand.name }}</td>
          
          <td class="py-3 text-muted text-truncate" style="max-width: 350px;" :title="brand.description">
            {{ brand.description || 'Chưa có mô tả' }}
          </td>

          <td class="py-3 text-center">
            <span 
              class="badge rounded-pill"
              :class="brand.status === 1 ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-secondary-subtle text-secondary border border-secondary-subtle'"
            >
              {{ brand.status === 1 ? 'Đang hoạt động' : 'Tạm ẩn' }}
            </span>
          </td>
          
          <td class="py-3 text-center">
            <button @click="emit('toggle-status', brand)" class="btn btn-sm btn-light border me-2 rounded-circle" :class="brand.status === 1 ? 'text-warning' : 'text-success'" :title="brand.status === 1 ? 'Tạm ẩn' : 'Hiển thị lại'">
              <i class="bi" :class="brand.status === 1 ? 'bi-eye-slash' : 'bi-eye'"></i>
            </button>
            <button @click="emit('edit', brand)" class="btn btn-sm btn-light text-primary border me-2 rounded-circle" title="Sửa">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button @click="emit('delete', brand.id)" class="btn btn-sm btn-light text-danger border rounded-circle" title="Đưa vào thùng rác">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        </tr>
        
        <tr v-if="brands.length === 0">
          <td colspan="5" class="text-center text-muted py-5">
            Không tìm thấy kết quả nào.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Brand } from '../types/brand.type';

defineProps<{ brands: Brand[] }>();
const emit = defineEmits(['edit', 'delete', 'toggle-status']);
</script>

<style scoped>
.table > :not(caption) > * > * { border-bottom-color: #f8f9fa; }
.bg-success-subtle { background-color: #d1e7dd !important; }
.bg-secondary-subtle { background-color: #e2e3e5 !important; }
</style>