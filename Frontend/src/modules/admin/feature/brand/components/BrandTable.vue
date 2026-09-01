<template>
  <div class="table-wrapper">
    <table class="table align-middle brand-table">
      <thead>
        <tr>
          <th scope="col" width="110">Ảnh Logo</th>
          <th scope="col">Tên thương hiệu</th>
          <th scope="col" class="text-center" width="180">
            Trạng thái (Ẩn/Hiện)
          </th>
          <th scope="col" class="text-center" width="140">Thao tác</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="brand in brands" :key="brand.id">
          <td>
            <div class="image-box">
              <img
                v-if="brand.logoUrl"
                :src="getImageUrl(brand.logoUrl)"
                loading="lazy"
                decoding="async"
                @error="onImageError"
              />

              <div v-else class="image-placeholder">
                <i class="bi bi-image"></i>
              </div>
            </div>
          </td>

          <td class="brand-name">
            {{ brand.name }}
          </td>

          <td class="text-center">
            <div class="form-check form-switch d-inline-block custom-switch">
              <input
                class="form-check-input"
                type="checkbox"
                :checked="brand.status === 1"
                @change="emit('toggle-status', brand)"
              />
            </div>
          </td>

          <td>
            <div class="actions">
              <button
                @click="emit('edit', brand)"
                class="icon-btn edit"
                title="Sửa"
              >
                <i class="bi bi-pencil"></i>
              </button>

              <button
                @click="emit('delete', brand.id)"
                class="icon-btn delete"
                title="Xóa"
              >
                <i class="bi bi-trash"></i>
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="brands.length === 0">
          <td colspan="4" class="empty">
            <i class="bi bi-star"></i>
            <p>Không tìm thấy thương hiệu nào.</p>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Brand } from "../types/brand.type";

defineProps<{ brands: Brand[] }>();

const emit = defineEmits([
  "edit",
  "delete",
  "toggle-status"
]);

const API_URL = import.meta.env.VITE_API_URL || "";

const getImageUrl = (url?: string) => {
  if (!url) return "";

  return url.startsWith("http")
    ? url
    : `${API_URL}${url}`;
};

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
      <rect width="100%" height="100%" fill="#f1f5f9"/>
      <text
        x="50%"
        y="50%"
        dominant-baseline="middle"
        text-anchor="middle"
        fill="#94a3b8"
        font-family="Arial"
        font-size="14"
      >
        Không có ảnh
      </text>
    </svg>
  `);

const onImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.src = FALLBACK_IMAGE;
};
</script>

<style scoped>
.table-wrapper {
  overflow: auto;
}

.brand-table {
  margin: 0;
}

.brand-table thead {
  position: sticky;
  top: 0;
  background: white;
  z-index: 5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.brand-table thead th {
  padding: 18px;
  border-bottom: 2px solid #edf2f7;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.brand-table tbody td {
  padding: 18px;
  vertical-align: middle;
}

.brand-table tbody tr {
  transition: 0.25s;
}

.brand-table tbody tr:hover {
  background: #f8fafc;
}

.brand-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}

.image-box {
  width: 55px;
  height: 55px;
}

.image-box img {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: contain;
  border: 1px solid #e2e8f0;
  transition: 0.25s;
  background: #f8fafc;
}

.image-box img:hover {
  transform: scale(1.08);
}

.image-placeholder {
  width: 55px;
  height: 55px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 20px;
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