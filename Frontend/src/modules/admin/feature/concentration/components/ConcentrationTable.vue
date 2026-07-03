<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0" style="background-color: #fff;">
      <thead class="text-secondary" style="border-bottom: 2px solid #f0f0f0;">
        <tr>
          <th scope="col" class="py-3 px-4">Tên nồng độ</th>
          <th scope="col" class="py-3 text-center" style="width: 180px;">Trạng thái</th>
          <th scope="col" class="py-3 text-center" style="width: 180px;">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in concentrations" :key="item.id">
          <td class="px-4 py-3 fw-medium text-dark">{{ item.name }}</td>

          <td class="py-3 text-center">
            <span 
              class="badge rounded-pill"
              :class="item.status === 1 ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-secondary-subtle text-secondary border border-secondary-subtle'"
            >
              {{ item.status === 1 ? 'Đang hoạt động' : 'Tạm ẩn' }}
            </span>
          </td>
          
          <td class="py-3 text-center">
            <button @click="emit('toggle-status', item)" class="btn btn-sm btn-light border me-2 rounded-circle" :class="item.status === 1 ? 'text-warning' : 'text-success'" :title="item.status === 1 ? 'Tạm ẩn' : 'Hiển thị lại'">
              <i class="bi" :class="item.status === 1 ? 'bi-eye-slash' : 'bi-eye'"></i>
            </button>
            <button @click="emit('edit', item)" class="btn btn-sm btn-light text-primary border me-2 rounded-circle" title="Sửa">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button @click="emit('delete', item.id)" class="btn btn-sm btn-light text-danger border rounded-circle" title="Đưa vào thùng rác">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        </tr>
        
        <tr v-if="concentrations.length === 0">
          <td colspan="3" class="text-center text-muted py-5">
            Không tìm thấy kết quả nào.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Concentration } from '../types/concentration.type';

defineProps<{ concentrations: Concentration[] }>();
const emit = defineEmits(['edit', 'delete', 'toggle-status']);
</script>

<style scoped>
.table > :not(caption) > * > * { border-bottom-color: #f8f9fa; }
.bg-success-subtle { background-color: #d1e7dd !important; }
.bg-secondary-subtle { background-color: #e2e3e5 !important; }
</style>