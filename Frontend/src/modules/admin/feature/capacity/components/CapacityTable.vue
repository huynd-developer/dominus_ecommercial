<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0" style="background-color: #fff;">
      <thead class="text-secondary" style="border-bottom: 2px solid #f0f0f0;">
        <tr>
          <th scope="col" class="py-3 px-4">Dung tích (ml)</th>
          <th scope="col" class="py-3 text-center" style="width: 150px;">Trạng thái</th>
          <th scope="col" class="py-3 text-center" style="width: 180px;">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="capacity in capacities" :key="capacity.id">
          <td class="px-4 py-3 fw-medium text-dark">{{ capacity.value }} ml</td>

          <td class="py-3 text-center">
            <span 
              class="badge rounded-pill"
              :class="capacity.status === 1 ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-secondary-subtle text-secondary border border-secondary-subtle'"
            >
              {{ capacity.status === 1 ? 'Đang hoạt động' : 'Tạm ẩn' }}
            </span>
          </td>
          
          <td class="py-3 text-center">
            <button @click="emit('toggle-status', capacity)" class="btn btn-sm btn-light border me-2 rounded-circle" :class="capacity.status === 1 ? 'text-warning' : 'text-success'" :title="capacity.status === 1 ? 'Tạm ẩn' : 'Hiển thị lại'">
              <i class="bi" :class="capacity.status === 1 ? 'bi-eye-slash' : 'bi-eye'"></i>
            </button>
            <button @click="emit('edit', capacity)" class="btn btn-sm btn-light text-primary border me-2 rounded-circle" title="Sửa">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button @click="emit('delete', capacity.id)" class="btn btn-sm btn-light text-danger border rounded-circle" title="Đưa vào thùng rác">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        </tr>
        
        <tr v-if="capacities.length === 0">
          <td colspan="3" class="text-center text-muted py-5">
            Không tìm thấy kết quả nào.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Capacity } from '../types/capacity.type';

defineProps<{ capacities: Capacity[] }>();
const emit = defineEmits(['edit', 'delete', 'toggle-status']);
</script>

<style scoped>
.table > :not(caption) > * > * { border-bottom-color: #f8f9fa; }
.bg-success-subtle { background-color: #d1e7dd !important; }
.bg-secondary-subtle { background-color: #e2e3e5 !important; }
</style>