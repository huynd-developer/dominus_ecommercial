```vue
<script setup lang="ts">
import { computed } from 'vue'

import type {
  Product,
  ProductVariant
} from '../types/product.type'

const props = defineProps<{
  paginatedData: Product[]
}>()

const emit = defineEmits<{
  (e: 'edit', product: Product): void
  (e: 'stop-selling', id: number): void
}>()

const API_URL =
  import.meta.env.VITE_API_URL || ''

const calculateTotalStock = (
  variants?: ProductVariant[]
) =>
  variants?.reduce(
    (sum, item) =>
      sum + (item.stockQuantity || 0),
    0
  ) ?? 0

const getStockClass = (
  stock: number
) => {
  if (stock === 0) {
    return 'danger'
  }

  if (stock < 10) {
    return 'warning'
  }

  return 'success'
}

const getImageUrl = (
  url?: string
) => {
  if (!url) {
    return ''
  }

  return url.startsWith('http')
    ? url
    : `${API_URL}${url}`
}

const onImageError = (
  event: Event
) => {
  const img =
    event.target as HTMLImageElement

  img.src =
    'https://placehold.co/200x200?text=No+Image'
}

const rows = computed(() =>
  props.paginatedData.map(
    (product) => {
      const stock =
        calculateTotalStock(
          product.variants
        )

      return {
        ...product,
        stock,
        stockClass:
          getStockClass(stock)
      }
    }
  )
)
</script>

<template>
  <div class="table-wrapper">

    <table
      class="table align-middle product-table"
    >

      <thead>

        <tr>
          <th width="90">
            Ảnh
          </th>

          <th>
            Sản phẩm
          </th>

          <th>
            Thương hiệu
          </th>

          <th>
            Danh mục
          </th>

          <th>
            Nồng độ
          </th>

          <th class="text-center">
            Tồn kho
          </th>

          <th class="text-center">
            Trạng thái
          </th>

          <th
            width="140"
            class="text-center"
          >
            Thao tác
          </th>
        </tr>

      </thead>

      <tbody>

        <tr
          v-if="rows.length === 0"
        >
          <td
            colspan="8"
            class="empty"
          >
            <i
              class="bi bi-box-seam"
            ></i>

            <p>
              Chưa có sản phẩm
            </p>
          </td>
        </tr>

        <tr
          v-for="product in rows"
          :key="product.id"
        >

          <td>

            <div class="image-box">

              <img
                v-if="
                  product.primaryImageUrl
                "
                :src="
                  getImageUrl(
                    product.primaryImageUrl
                  )
                "
                loading="lazy"
                decoding="async"
                @error="
                  onImageError
                "
              >

              <div
                v-else
                class="image-placeholder"
              >
                <i
                  class="bi bi-image"
                ></i>
              </div>

            </div>

          </td>

          <td>

            <div
              class="product-info"
            >

              <div
                class="product-name"
              >
                {{ product.name }}
              </div>

              <div
                class="product-sub"
              >
                {{
                  product.variants
                    ?.length || 0
                }}
                biến thể
              </div>

            </div>

          </td>

          <td>
            {{
              product.brandName
            }}
          </td>

          <td>
            {{
              product.categoryName
            }}
          </td>

          <td>
            {{
              product.concentrationName
            }}
          </td>

          <td
            class="text-center"
          >

            <span
              class="stock-badge"
              :class="
                product.stockClass
              "
            >
              {{
                product.stock
              }}
            </span>

          </td>

          <td
            class="text-center"
          >

            <span
              class="status"
              :class="
                product.status === 1
                  ? 'active'
                  : 'inactive'
              "
            >
              {{
                product.status === 1
                  ? 'Đang bán'
                  : 'Ngừng bán'
              }}
            </span>

          </td>

          <td>

            <div
              class="actions"
            >

              <button
                class="icon-btn edit"
                @click="
                  emit(
                    'edit',
                    product
                  )
                "
              >
                <i
                  class="bi bi-pencil"
                ></i>
              </button>

              <button
                v-if="
                  product.status === 1
                "
                class="icon-btn delete"
                @click="
                  emit(
                    'stop-selling',
                    product.id
                  )
                "
              >
                <i
                  class="bi bi-eye-slash"
                ></i>
              </button>

            </div>

          </td>

        </tr>

      </tbody>

    </table>

  </div>
</template>

<style scoped>
.table-wrapper{
    overflow:auto;
}

.product-table{
    margin:0;
}

.product-table thead{
    position:sticky;
    top:0;
    background:white;
    z-index:5;
    box-shadow:0 2px 8px rgba(0,0,0,.05);
}

.product-table thead th{
    padding:18px;
    border-bottom:2px solid #edf2f7;
    font-size:14px;
    color:#64748b;
    font-weight:600;
}

.product-table tbody td{
    padding:18px;
    vertical-align:middle;
}

.product-table tbody tr{
    transition:.25s;
}

.product-table tbody tr:hover{
    background:#f8fafc;
}

.image-box{
    width:65px;
    height:65px;
}

.image-box img{
    width:100%;
    height:100%;
    border-radius:12px;
    object-fit:cover;
    border:1px solid #e2e8f0;
    transition:.25s;
}

.image-box img:hover{
    transform:scale(1.08);
}

.image-placeholder{
    width:65px;
    height:65px;
    border-radius:12px;
    display:flex;
    align-items:center;
    justify-content:center;
    background:#f1f5f9;
    color:#94a3b8;
    font-size:24px;
}

.product-name{
    font-weight:600;
    color:#1e293b;
}

.product-sub{
    color:#94a3b8;
    font-size:13px;
    margin-top:4px;
}

.stock-badge{
    padding:6px 14px;
    border-radius:999px;
    font-weight:700;
    font-size:13px;
}

.stock-badge.success{
    background:#dcfce7;
    color:#15803d;
}

.stock-badge.warning{
    background:#fef3c7;
    color:#b45309;
}

.stock-badge.danger{
    background:#fee2e2;
    color:#dc2626;
}

.status{
    display:inline-flex;
    align-items:center;
    justify-content:center;
    min-width:90px;
    padding:6px 14px;
    border-radius:999px;
    font-weight:600;
    font-size:13px;
}

.active{
    background:#dcfce7;
    color:#15803d;
}

.inactive{
    background:#fee2e2;
    color:#dc2626;
}

.actions{
    display:flex;
    justify-content:center;
    gap:8px;
}

.icon-btn{
    width:38px;
    height:38px;
    border:none;
    border-radius:10px;
    transition:.25s;
}

.edit{
    background:#eff6ff;
    color:#2563eb;
}

.edit:hover{
    background:#2563eb;
    color:white;
}

.delete{
    background:#fef2f2;
    color:#dc2626;
}

.delete:hover{
    background:#dc2626;
    color:white;
}

.empty{
    text-align:center;
    padding:70px !important;
    color:#94a3b8;
}

.empty i{
    font-size:48px;
    display:block;
    margin-bottom:10px;
}
</style>
```
