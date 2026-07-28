<template>
  <div class="table-wrapper">
    <table class="table align-middle category-table">
      <thead>
        <tr>
          <th scope="col" width="50%">Tên danh mục</th>
          <th scope="col" class="text-center">Trạng thái (Ẩn/Hiện)</th>
          <th scope="col" class="text-center" width="140">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="category in categories" :key="category.id">
          <td class="category-name">{{ category.name }}</td>

          <td class="text-center">
            <div class="form-check form-switch d-inline-block custom-switch">
              <input
                class="form-check-input"
                type="checkbox"
                :checked="category.status === 1"
                :disabled="!canEdit"
                @change="emit('toggle-status', category)"
              />
            </div>
          </td>

          <td>
            <div class="actions">
              <button
                v-if="canEdit"
                @click="emit('edit', category)"
                class="icon-btn edit"
                title="Sửa"
              >
                <i class="bi bi-pencil"></i>
              </button>

              <button
                v-if="canDelete"
                @click="emit('delete', category.id)"
                class="icon-btn delete"
                title="Xóa"
              >
                <i class="bi bi-trash"></i>
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="categories.length === 0">
          <td colspan="3" class="empty">
            <i class="bi bi-folder-x"></i>
            <p>Không tìm thấy danh mục nào.</p>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Category } from "../types/category.type";

defineProps<{
  categories: Category[];
  canEdit: boolean;
  canDelete: boolean;
}>();

const emit = defineEmits(["edit", "delete", "toggle-status"]);
</script>

<style scoped>
.table-wrapper {
  overflow: auto;
}

.category-table {
  margin: 0;
}

.category-table thead {
  position: sticky;
  top: 0;
  background: white;
  z-index: 5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.category-table thead th {
  padding: 18px;
  border-bottom: 2px solid #edf2f7;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.category-table tbody td {
  padding: 18px;
  vertical-align: middle;
}

.category-table tbody tr {
  transition: 0.25s;
}

.category-table tbody tr:hover {
  background: #f8fafc;
}

.category-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}

/* Switch Toggle */
.custom-switch .form-check-input {
  width: 48px;
  height: 24px;
  cursor: pointer;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background-color: #e2e8f0;
  transition: 0.25s;
}

.custom-switch .form-check-input:checked {
  background-color: #2563eb;
  border-color: #2563eb;
}

.custom-switch .form-check-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Actions */
.actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 10px;
  transition: 0.25s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit {
  background: #eff6ff;
  color: #2563eb;
}

.edit:hover {
  background: #2563eb;
  color: white;
}

.delete {
  background: #fef2f2;
  color: #dc2626;
}

.delete:hover {
  background: #dc2626;
  color: white;
}

.empty {
  text-align: center;
  padding: 70px !important;
  color: #94a3b8;
}

.empty i {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}

.empty p {
  margin: 0;
  font-size: 15px;
}
</style>