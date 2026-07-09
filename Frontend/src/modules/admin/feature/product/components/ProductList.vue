<script setup lang="ts">
import type { Product, ProductVariant } from '../types/product.type'

defineProps<{
  paginatedData: Product[]
}>()

const emit = defineEmits<{
  (e: 'edit', product: Product): void
  (e: 'upload-image', product: Product): void
  (e: 'stop-selling', id: number): void
}>()

const calculateTotalStock = (
  variants?: ProductVariant[]
) => {
  if (!variants?.length) return 0

  return variants.reduce(
    (total, item) => total + (item.stockQuantity || 0),
    0
  )
}

const getImageUrl = (url?: string) => {
  if (!url) return ''

  return url.startsWith('http')
    ? url
    : `http://localhost:8080${url}`
}
</script>

<template>
  <div class="p-0 flex-grow-1 overflow-auto custom-scrollbar">
    <table class="table align-middle mb-0 table-hover border-0">
      <thead
        class="bg-white text-muted small"
        style="position: sticky; top: 0; z-index: 1"
      >
        <tr>
          <th width="90" class="ps-4">Ảnh</th>
          <th>ID</th>
          <th>Tên sản phẩm</th>
          <th>Thương hiệu</th>
          <th>Danh mục</th>
          <th>Nồng độ</th>
          <th class="text-center">Tồn kho</th>
          <th>Trạng thái</th>
          <th class="text-end pe-4">Thao tác</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="paginatedData.length === 0">
          <td colspan="9" class="text-center py-5">
            Chưa có sản phẩm nào
          </td>
        </tr>

        <tr
          v-for="product in paginatedData"
          :key="product.id"
        >
          <!-- Ảnh -->
          <td class="ps-4">
            <img
              v-if="product.primaryImageUrl"
              :src="getImageUrl(product.primaryImageUrl)"
              alt=""
              style="
                width: 60px;
                height: 60px;
                object-fit: cover;
                border-radius: 10px;
              "
            />

            <div
              v-else
              class="bg-light d-flex justify-content-center align-items-center"
              style="
                width: 60px;
                height: 60px;
                border-radius: 10px;
              "
            >
              No Image
            </div>
          </td>

          <!-- ID -->
          <td>
            #{{ product.id }}
          </td>

          <!-- Tên -->
          <td class="fw-semibold">
            {{ product.name }}
          </td>

          <!-- Brand -->
          <td>
            {{ product.brandName }}
          </td>

          <!-- Category -->
          <td>
            {{ product.categoryName }}
          </td>

          <!-- Concentration -->
          <td>
            {{ product.concentrationName }}
          </td>

          <!-- Tồn kho -->
          <td class="text-center fw-bold">
            {{ calculateTotalStock(product.variants) }}
          </td>

          <!-- Status -->
          <td>
            <span
              v-if="product.status === 1"
              class="badge bg-success"
            >
              Đang bán
            </span>

            <span
              v-else
              class="badge bg-danger"
            >
              Ngừng bán
            </span>
          </td>

          <!-- Action -->
          <td class="text-end pe-4">
            <button
              class="btn btn-sm btn-outline-primary me-2"
              @click="emit('upload-image', product)"
            >
              Ảnh
            </button>

            <button
              class="btn btn-sm btn-outline-warning me-2"
              @click="emit('edit', product)"
            >
              Sửa
            </button>

            <button
              v-if="product.status === 1"
              class="btn btn-sm btn-outline-danger"
              @click="emit('stop-selling', product.id)"
            >
              Ẩn
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 20px;
}
</style>