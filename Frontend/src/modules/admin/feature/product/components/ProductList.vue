<script setup lang="ts">
import { ref, computed } from "vue";
import Swal from "sweetalert2";

import type { Product, ProductVariant } from "../types/product.type";

const props = defineProps<{
  paginatedData: Product[];
}>();

const emit = defineEmits<{
  (e: "edit", product: Product): void;
  (e: "clone", product: Product): void;
  (e: "stop-selling", id: number): void;
  (e: "start-selling", id: number): void;
  (e: "toggle-variant-status", product: Product, variant: ProductVariant): void;
  (e: "delete", id: number): void;
}>();

const API_URL = import.meta.env.VITE_API_URL || "";

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
  return new Intl.NumberFormat("en-US").format(price) + " VNĐ";
};

const calculateTotalStock = (variants?: ProductVariant[]) =>
  variants?.reduce((sum, item) => sum + Number(item.totalQuantity ?? 0), 0) ?? 0;

const calculateTotalSellableStock = (variants?: ProductVariant[]) =>
  variants?.reduce((sum, item) => sum + Number(item.sellableQuantity ?? 0), 0) ?? 0;

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

const handleDelete = (product: Product) => {
  Swal.fire({
    title: "Xác nhận xóa?",
    text: `Bạn có chắc chắn muốn xóa sản phẩm "${product.name}" không? Thao tác này không thể hoàn tác!`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#64748b",
    confirmButtonText: "Xóa sản phẩm",
    cancelButtonText: "Hủy",
  }).then((result) => {
    if (result.isConfirmed) {
      emit("delete", product.id);
    }
  });
};

const rows = computed(() =>
  props.paginatedData.map((product) => {
    const stock = calculateTotalStock(product.variants);
    const sellableStock = calculateTotalSellableStock(product.variants);

    return {
      ...product,
      stock,
      sellableStock,
      stockClass: getStockClass(stock),
      sellableStockClass: getStockClass(sellableStock),
    };
  }),
);
</script>

<template>
  <div class="table-wrapper custom-scrollbar">
    <table class="table align-middle product-table">
      <thead>
        <tr>
          <th width="75">Ảnh</th>
          <th>Sản phẩm</th>
          <th>Thương hiệu</th>
          <th>Danh mục</th>
          <th>Nồng độ</th>
          <th class="text-center">Tồn thực tế</th>
          <th class="text-center">Có thể bán</th>
          <th class="text-center">Trạng thái</th>
          <th width="180" class="text-center">Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="rows.length === 0">
          <td colspan="9" class="empty">
            <i class="bi bi-box-seam"></i>
            <p>Chưa có sản phẩm</p>
          </td>
        </tr>

        <template v-for="product in rows" :key="product.id">
          <tr 
            :class="[
              { 'row-expanded': expandedRowIds.includes(product.id) }, 
              product.status !== 1 ? 'row-inactive' : ''
            ]"
          >
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
                <div
                  class="expand-trigger"
                  @click="toggleRow(product.id)"
                >
                  <span>{{ product.variants?.length || 0 }} biến thể</span>
                  <i
                    class="bi"
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
              <span
                class="stock-badge"
                :class="product.stockClass"
                title="Tổng số lượng thực tế đang có trong các lô kho"
              >
                {{ product.stock }}
              </span>
            </td>
            <td class="text-center">
              <span
                class="stock-badge"
                :class="product.sellableStockClass"
                title="Số lượng còn có thể bán, không tính hàng hết hạn"
              >
                {{ product.sellableStock }}
              </span>
            </td>
            <td class="text-center">
              <!-- TRẠNG THÁI SẢN PHẨM CHÍNH -->
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
                  class="icon-btn clone"
                  title="Nhân bản sản phẩm"
                  @click="emit('clone', product)"
                >
                  <i class="bi bi-files"></i>
                </button>

                <button
                  class="icon-btn edit"
                  title="Chỉnh sửa"
                  @click="emit('edit', product)"
                >
                  <i class="bi bi-pencil"></i>
                </button>
                
                <button
                  v-if="product.status === 1"
                  class="icon-btn toggle-status-off"
                  title="Ngừng bán"
                  @click="emit('stop-selling', product.id)"
                >
                  <i class="bi bi-eye-slash"></i>
                </button>
                <button
                  v-else
                  class="icon-btn toggle-status-on"
                  title="Mở bán"
                  @click="emit('start-selling', product.id)"
                >
                  <i class="bi bi-eye"></i>
                </button>

                <button
                  class="icon-btn delete"
                  title="Xóa sản phẩm"
                  @click="handleDelete(product)"
                >
                  <i class="bi bi-trash"></i>
                </button>
              </div>
            </td>
          </tr>

          <!-- Dòng phụ hiển thị chi tiết biến thể -->
          <tr v-if="expandedRowIds.includes(product.id)" class="variant-row">
            <td class="p-0 border-0 bg-transparent"></td>
            <td colspan="8" class="p-0 border-0">
              <div class="variant-container slide-down">
                <div class="variant-arrow"></div>
                <div class="px-4 py-2 bg-light border-bottom fw-bold text-dark d-flex align-items-center gap-2" style="font-size: 13px;">
                  <i class="bi bi-list-nested text-primary"></i> Chi tiết các biến thể sản phẩm
                </div>
                <table class="table variant-table m-0">
                  <thead>
                    <tr>
                      <th width="120">Dung tích</th>
                      <th width="200">Loại chai</th>
                      <th class="text-end" width="150">Giá bán</th>
                      <th class="text-center" width="110">Tồn thực tế</th>
                      <th class="text-center" width="110">Có thể bán</th>
                      <th class="text-center" width="150">Trạng thái biến thể</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="v in product.variants" :key="v.id" :class="{'opacity-50': v.status !== 1}">
                      <td class="fw-semibold text-dark">
                        {{ v.capacityName }} ml
                      </td>
                      <td>{{ v.bottleTypeName }}</td>
                      <td class="text-end text-danger fw-bold">
                        {{ formatPrice(v.price) }}
                      </td>
                      <td class="text-center">
                        <span
                          class="badge px-3 py-1.5"
                          :class="
                            Number(v.totalQuantity ?? 0) > 0 ? 'bg-success' : 'bg-danger'
                          "
                          title="Tổng số lượng thực tế đang có trong các lô kho"
                        >
                          {{ v.totalQuantity ?? 0 }}
                        </span>
                      </td>
                      <td class="text-center">
                        <span
                          class="badge px-3 py-1.5"
                          :class="
                            Number(v.sellableQuantity ?? 0) > 0 ? 'bg-success' : 'bg-danger'
                          "
                          title="Số lượng còn có thể bán, không tính hàng hết hạn"
                        >
                          {{ v.sellableQuantity ?? 0 }}
                        </span>
                      </td>
                      <td class="text-center">
                        <!-- TRẠNG THÁI BIẾN THỂ -->
                        <span
                          class="status"
                          :class="[
                            v.status === 1 ? 'active' : 'inactive',
                            'clickable-status'
                          ]"
                          title="Bấm để chuyển đổi nhanh trạng thái biến thể"
                          @click="emit('toggle-variant-status', product, v)"
                        >
                          {{ v.status === 1 ? "Đang bán" : "Ngừng bán" }}
                          <i class="bi bi-arrow-repeat ms-1 fs-7"></i>
                        </span>
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
.table-wrapper { overflow: auto; }
.product-table { margin: 0; border-collapse: separate; border-spacing: 0; }
.product-table thead { position: sticky; top: 0; background: white; z-index: 5; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }

.product-table thead th { padding: 14px 12px; border-bottom: 2px solid #edf2f7; font-size: 13.5px; color: #64748b; font-weight: 600; }
.product-table tbody td { padding: 14px 12px; vertical-align: middle; border-bottom: 1px solid #f1f5f9; font-size: 14px; }

.product-table tbody tr { transition: all 0.25s ease; }
.product-table tbody tr:hover:not(.variant-row) { background: #f8fafc; }
.row-expanded td { border-bottom-color: transparent !important; background: #f8fafc; }
.row-inactive > td { background-color: #f8fafc !important; }
.row-inactive .image-box img { filter: grayscale(100%); opacity: 0.6; }

.image-box { width: 55px; height: 55px; }
.image-box img { width: 100%; height: 100%; border-radius: 10px; object-fit: cover; border: 1px solid #e2e8f0; transition: 0.25s; }
.image-box img:hover { transform: scale(1.08); }
.image-placeholder { width: 55px; height: 55px; border-radius: 10px; display: flex; align-items: center; justify-content: center; background: #f1f5f9; color: #94a3b8; font-size: 20px; }
.product-name { font-weight: 600; color: #1e293b; font-size: 14.5px; }

.expand-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12.5px;
  padding: 4px 10px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
  margin-top: 5px;
  user-select: none;
}
.expand-trigger:hover {
  background: #e2e8f0;
  color: #1e293b;
}
.expand-trigger i {
  font-size: 11px;
  transition: transform 0.3s ease;
}

.variant-row { background: #f8fafc; }

.variant-container {
  max-width: 900px;
  margin: 6px 0 16px 5px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}
.variant-arrow { position: absolute; top: -8px; left: 35px; width: 14px; height: 14px; background: #ffffff; border-top: 1px solid #cbd5e1; border-left: 1px solid #cbd5e1; transform: rotate(45deg); }

.variant-table th { background: #f8fafc; font-size: 12.5px; padding: 10px 14px; color: #475569; border-bottom: 1px solid #e2e8f0; }
.variant-table td { padding: 10px 14px; font-size: 13px; border-bottom: 1px solid #f1f5f9; }
.variant-table tr:last-child td { border-bottom: none; }
.variant-table tr:hover td { background: #f8fbff; }

.opacity-50 { opacity: 0.5; }

.stock-badge { padding: 4px 10px; border-radius: 999px; font-weight: 700; font-size: 12px; }
.stock-badge.success { background: #dcfce7; color: #15803d; }
.stock-badge.warning { background: #fef3c7; color: #b45309; }
.stock-badge.danger { background: #fee2e2; color: #dc2626; }

.status { display: inline-flex; align-items: center; justify-content: center; min-width: 90px; padding: 4px 10px; border-radius: 999px; font-weight: 600; font-size: 12px; }
.clickable-status { cursor: pointer; transition: 0.2s; }
.clickable-status:hover { opacity: 0.85; transform: scale(1.03); box-shadow: 0 2px 6px rgba(0,0,0,0.1); }

.active { background: #dcfce7; color: #15803d; }
.inactive { background: #f3f4f6; color: #475569; }

.actions { display: flex; justify-content: center; gap: 5px; }
.icon-btn { width: 34px; height: 34px; border: none; border-radius: 9px; transition: 0.25s; display: flex; align-items: center; justify-content: center; font-size: 14px; cursor: pointer; }
.icon-btn:disabled { cursor: not-allowed; }

.clone { background: #f3e8ff; color: #9333ea; }
.clone:hover:not(:disabled) { background: #9333ea; color: white; transform: translateY(-2px); }
.edit { background: #eff6ff; color: #2563eb; }
.edit:hover:not(:disabled) { background: #2563eb; color: white; transform: translateY(-2px); }
.toggle-status-off { background: #fffbeb; color: #d97706; }
.toggle-status-off:hover:not(:disabled) { background: #d97706; color: white; transform: translateY(-2px); }
.toggle-status-on { background: #dcfce7; color: #15803d; }
.toggle-status-on:hover:not(:disabled) { background: #15803d; color: white; transform: translateY(-2px); }
.delete { background: #fef2f2; color: #dc2626; }
.delete:hover:not(:disabled) { background: #dc2626; color: white; transform: translateY(-2px); }

.empty { text-align: center; padding: 60px !important; color: #94a3b8; }
.empty i { font-size: 42px; display: block; margin-bottom: 8px; }

.slide-down { animation: slideDown 0.25s ease-out forwards; transform-origin: top; }
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
.custom-scrollbar::-webkit-scrollbar { height: 6px; width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 8px; }
</style>