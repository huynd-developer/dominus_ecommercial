<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0" style="background-color: #fff;">
      <thead class="text-secondary" style="border-bottom: 2px solid #f0f0f0;">
        <tr>
          <th scope="col" class="py-3 px-4">Tên danh mục</th>
          <th scope="col" class="py-3">Trạng thái (Ẩn/Hiện)</th>
          <th scope="col" class="py-3 text-center" style="width: 150px;">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="category in categories" :key="category.id">
          <td class="px-4 py-3 fw-medium">{{ category.name }}</td>
          
          <td class="py-3">
            <div class="form-check form-switch">
              <input 
                class="form-check-input fs-5" 
                type="checkbox" 
                :checked="category.status === 1"
                @change="emit('toggle-status', category)"
                style="cursor: pointer;"
              >
            </div>
          </td>
          
          <td class="py-3 text-center">
            <button @click="emit('edit', category)" class="btn btn-sm btn-light text-primary border me-2 rounded-circle" title="Sửa">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button @click="emit('delete', category.id)" class="btn btn-sm btn-light text-danger border rounded-circle" title="Xóa">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        </tr>
        
        <tr v-if="categories.length === 0">
          <td colspan="3" class="text-center text-muted py-5">
            Không tìm thấy kết quả nào.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Category } from '../types/category.type';

defineProps<{ categories: Category[] }>();

// Bắn sự kiện lên Component Cha (CategoryView) để nó xử lý logic
const emit = defineEmits(['edit', 'delete', 'toggle-status']);
</script>

<style scoped>
/* Custom nhẹ lại cái table cho giống ảnh */
.table > :not(caption) > * > * {
  border-bottom-color: #f8f9fa;
}
</style>