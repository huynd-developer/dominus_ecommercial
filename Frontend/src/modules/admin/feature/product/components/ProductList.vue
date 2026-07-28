<script setup lang="ts">
import { ref, computed } from "vue";

import type { Product, ProductVariant } from "../types/product.type";

const props = defineProps<{
  paginatedData: Product[];
}>();

const emit = defineEmits<{
  (e: "edit", product: Product): void;
  (e: "stop-selling", id: number): void;
}>();

const API_URL = import.meta.env.VITE_API_URL || "";

// Biến quản lý các dòng đang được mở (xổ biến thể)
const expandedRowIds = ref<number[]>([]);

const toggleRow = (id: number) => {
  const index = expandedRowIds.value.indexOf(id);
  if (index === -1) {
    expandedRowIds.value.push(id);
  } else {
    expandedRowIds.value.splice(index, 1);
  }
};

const formatPrice = (price: number) => {
  // Dùng en-US để lấy dấu phẩy phân cách hàng nghìn, sau đó ghép thêm chữ VNĐ
  return new Intl.NumberFormat("en-US").format(price) + " VNĐ";
};

const calculateTotalStock = (variants?: ProductVariant[]) =>
  variants?.reduce((sum, item) => sum + (item.stockQuantity || 0), 0) ?? 0;

const getStockClass = (stock: number) => {
  if (stock === 0) return "danger";
  if (stock < 10) return "warning";
  return "success";
};

const getImageUrl = (url?: string) => {
  if (!url) return "";
  return url.startsWith("http") ? url : `${API_URL}${url}`;
};

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
  <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
    <rect width="100%" height="100%" fill="#f1f5f9"/>
    <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#94a3b8" font-family="Arial" font-size="14">Không có ảnh</text>
  </svg>
`);

const onImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.src = FALLBACK_IMAGE;
};

const rows = computed(() =>
  props.paginatedData.map((product) => {
    const stock = calculateTotalStock(product.variants);
    return {
      ...product,
      stock,
      stockClass: getStockClass(stock),
    };
  }),
);
</script>

<template>
  <div class="table-wrapper custom-scrollbar">
    <table class="table align-middle product-table">
      <thead>
        <tr>
          <th width="90">Ảnh</th>
          <th>Sản phẩm</th>
          <th>Thương hiệu</th>
          <th>Danh mục</th>
          <th>Nồng độ</th>
          <th class="text-center">Tồn kho</th>
          <th class="text-center">Trạng thái</th>
          <th width="140" class="text-center">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="rows.length === 0">
          <td colspan="8" class="empty">
            <i class="bi bi-box-seam"></i>
            <p>Chưa có sản phẩm</p>
          </td>
        </tr>

        <!-- Bọc trong template để render 2 dòng (dòng chính + dòng biến thể) -->
        <template v-for="product in rows" :key="product.id">
          <!-- Dòng chính -->
          <tr :class="{ 'row-expanded': expandedRowIds.includes(product.id) }">
            <td>
              <div class="image-box">
                <img
                  v-if="product.primaryImageUrl"
                  :src="getImageUrl(product.primaryImageUrl)"
                  loading="lazy"
                  decoding="async"
                  @error="onImageError"
                />
                <div v-else class="image-placeholder">
                  <i class="bi bi-image"></i>
                </div>
              </div>
            </td>
            <td>
              <div class="product-info">
                <div class="product-name">{{ product.name }}</div>
                <!-- Nút toggle xổ biến thể -->
                <div
                  class="product-sub expand-trigger"
                  @click="toggleRow(product.id)"
                >
                  <span class="badge-variant"
                    >{{ product.variants?.length || 0 }} biến thể</span
                  >
                  <i
                    class="bi ms-1"
                    :class="
                      expandedRowIds.includes(product.id)
                        ? 'bi-chevron-up text-primary'
                        : 'bi-chevron-down'
                    "
                  ></i>
                </div>
              </div>
            </td>
            <td>{{ product.brandName }}</td>
            <td>{{ product.categoryName }}</td>
            <td>{{ product.concentrationName }}</td>
            <td class="text-center">
              <span class="stock-badge" :class="product.stockClass">
                {{ product.stock }}
              </span>
            </td>
            <td class="text-center">
              <span
                class="status"
                :class="product.status === 1 ? 'active' : 'inactive'"
              >
                {{ product.status === 1 ? "Đang bán" : "Ngừng bán" }}
              </span>
            </td>
            <td>
              <div class="actions">
                <button
                  class="icon-btn edit"
                  title="Chỉnh sửa"
                  @click="emit('edit', product)"
                >
                  <i class="bi bi-pencil"></i>
                </button>
                <button
                  v-if="product.status === 1"
                  class="icon-btn delete"
                  title="Ngừng bán"
                  @click="emit('stop-selling', product.id)"
                >
                  <i class="bi bi-eye-slash"></i>
                </button>
              </div>
            </td>
          </tr>

          <!-- Dòng phụ xổ xuống chi tiết biến thể -->
          <tr v-if="expandedRowIds.includes(product.id)" class="variant-row">
            <td colspan="8" class="p-0 border-0">
              <div class="variant-container slide-down">
                <div class="variant-arrow"></div>
                <table class="table variant-table m-0">
                  <thead>
                    <tr>
                      <th>Dung tích</th>
                      <th>Loại chai</th>
                      <th class="text-end">Giá bán</th>
                      <th class="text-center">Tồn kho</th>
                      <th class="text-center">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    <!-- Tìm đoạn này trong phần <tbody> của bảng variant-table và thay thế -->
                    <tr v-for="v in product.variants" :key="v.id">

                      <!-- Sửa 2 dòng này: Chỉ gọi thẳng capacityName và bottleTypeName -->
                      <td class="fw-medium text-dark">
                        {{ v.capacityName }} ml
                      </td>
                      <td>{{ v.bottleTypeName }}</td>

                      <td class="text-end text-danger fw-bold">
                        {{ formatPrice(v.price) }}
                      </td>
                      <td class="text-center">
                        <span
                          class="badge"
                          :class="
                            v.stockQuantity > 0 ? 'bg-success' : 'bg-danger'
                          "
                        >
                          {{ v.stockQuantity }}
                        </span>
                      </td>
                      <td class="text-center">
                        <span
                          class="status-dot"
                          :class="
                            v.status === 1 ? 'bg-success' : 'bg-secondary'
                          "
                        ></span>
                        <small class="ms-1 text-muted">{{
                          v.status === 1 ? "Mở bán" : "Tắt"
                        }}</small>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrapper {
  overflow: auto;
}

.product-table {
  margin: 0;
  border-collapse: separate;
  border-spacing: 0;
}

.product-table thead {
  position: sticky;
  top: 0;
  background: white;
  z-index: 5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.product-table thead th {
  padding: 18px;
  border-bottom: 2px solid #edf2f7;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.product-table tbody td {
  padding: 18px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
}

.product-table tbody tr {
  transition: all 0.25s ease;
}

.product-table tbody tr:hover:not(.variant-row) {
  background: #f8fafc;
}

/* Row khi đang mở */
.row-expanded td {
  border-bottom-color: transparent !important;
  background: #f8fafc;
}

.image-box {
  width: 65px;
  height: 65px;
}

.image-box img {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
  transition: 0.25s;
}

.image-box img:hover {
  transform: scale(1.08);
}

.image-placeholder {
  width: 65px;
  height: 65px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 24px;
}

.product-name {
  font-weight: 600;
  color: #1e293b;
}

/* Phần trigger xổ xuống */
.product-sub {
  margin-top: 6px;
}

.expand-trigger {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  padding: 4px 8px 4px 0;
  border-radius: 6px;
  transition: 0.2s;
}

.expand-trigger:hover .badge-variant {
  background: #dbeafe;
  color: #1d4ed8;
}

.expand-trigger i {
  font-size: 12px;
  color: #94a3b8;
  transition: transform 0.3s ease;
}

.badge-variant {
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 500;
  transition: 0.2s;
}

/* Bảng phụ (Variant) */
.variant-row {
  background: #f8fafc;
}

.variant-container {
  margin: 0 40px 20px 90px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  position: relative;
  overflow: hidden;
}

.variant-arrow {
  position: absolute;
  top: -8px;
  left: 60px;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  border-left: 1px solid #e2e8f0;
  transform: rotate(45deg);
}

.variant-table th {
  background: #f8fafc;
  font-size: 13px;
  padding: 12px 16px;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
}

.variant-table td {
  padding: 12px 16px;
  font-size: 13px;
  border-bottom: 1px solid #f1f5f9;
}

.variant-table tr:last-child td {
  border-bottom: none;
}

.variant-table tr:hover td {
  background: #fdfefe;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.stock-badge {
  padding: 6px 14px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
}

.stock-badge.success {
  background: #dcfce7;
  color: #15803d;
}
.stock-badge.warning {
  background: #fef3c7;
  color: #b45309;
}
.stock-badge.danger {
  background: #fee2e2;
  color: #dc2626;
}

.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 90px;
  padding: 6px 14px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 13px;
}

.active {
  background: #dcfce7;
  color: #15803d;
}
.inactive {
  background: #fee2e2;
  color: #dc2626;
}

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
  transform: translateY(-2px);
}

.delete {
  background: #fef2f2;
  color: #dc2626;
}
.delete:hover {
  background: #dc2626;
  color: white;
  transform: translateY(-2px);
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

/* Animations */
.slide-down {
  animation: slideDown 0.3s ease-out forwards;
  transform-origin: top;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.custom-scrollbar::-webkit-scrollbar {
  height: 8px;
  width: 8px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
