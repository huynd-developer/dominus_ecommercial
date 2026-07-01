<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0" style="background-color: #fff;">
      <thead class="text-secondary" style="border-bottom: 2px solid #f0f0f0;">
        <tr>
          <th scope="col" class="py-3 px-4">Tên thương hiệu</th>
          <th scope="col" class="py-3">Mô tả</th>
          <th scope="col" class="py-3 text-center" style="width: 150px;">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="brand in brands" :key="brand.id">
          <td class="px-4 py-3 fw-medium text-dark">{{ brand.name }}</td>
          
          <td class="py-3 text-muted text-truncate" style="max-width: 350px;" :title="brand.description">
            {{ brand.description || 'Chưa có mô tả' }}
          </td>
          
          <td class="py-3 text-center">
            <button @click="emit('edit', brand)" class="btn btn-sm btn-light text-primary border me-2 rounded-circle" title="Sửa">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button @click="emit('delete', brand.id)" class="btn btn-sm btn-light text-danger border rounded-circle" title="Xóa">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        </tr>
        
        <tr v-if="brands.length === 0">
          <td colspan="3" class="text-center text-muted py-5">
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

// Đã bỏ 'toggle-status' ra khỏi danh sách emit vì không dùng tới nữa
const emit = defineEmits(['edit', 'delete']);
</script>

<style scoped>
.table > :not(caption) > * > * {
  border-bottom-color: #f8f9fa;
}
</style>