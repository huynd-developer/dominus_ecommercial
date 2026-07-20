<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { orderAdminService } from '../service/orderAdminService';
import type { OrderAdminResponse } from '../types';
import OrderDetailModal from '../components/OrderDetailModal.vue';

const orders = ref<OrderAdminResponse[]>([]);
const totalElements = ref(0);
const totalPages = ref(0);

// Khởi tạo các bộ lọc (Filters)
const filters = ref({ keyword: '', status: '', orderType: '', page: 0, size: 10 });

const selectedOrderId = ref<number | null>(null);
const isModalOpen = ref(false);

const fetchOrders = async () => {
  try {
    const data = await orderAdminService.getOrders(filters.value);
    orders.value = data.content;
    totalElements.value = data.totalElements;
    totalPages.value = data.totalPages;
  } catch (error) {
    console.error('Lỗi tải danh sách đơn hàng:', error);
  }
};

const handleSearch = () => {
  filters.value.page = 0; // Reset về trang đầu tiên khi tìm kiếm
  fetchOrders();
};

const changePage = (newPage: number) => {
  if (newPage >= 0 && newPage < totalPages.value) {
    filters.value.page = newPage;
    fetchOrders();
  }
};

const openDetail = (id: number) => {
  selectedOrderId.value = id;
  isModalOpen.value = true;
};

onMounted(() => { fetchOrders(); });

const formatPrice = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
const formatDate = (dateStr: string) => new Date(dateStr).toLocaleString('vi-VN');
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">Quản Lý Đơn Hàng (Admin)</h2>

    <!-- Bộ lọc tìm kiếm (Filters Bar) -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 bg-white p-4 rounded-md shadow-sm border mb-6">
      <input v-model="filters.keyword" @keyup.enter="handleSearch" type="text" placeholder="Tìm tên, SĐT, Mã đơn hàng..." class="border p-2 rounded-md text-sm outline-none focus:border-blue-500" />
      
      <select v-model="filters.orderType" @change="handleSearch" class="border p-2 rounded-md text-sm outline-none">
        <option value="">Tất cả nguồn đơn</option>
        <option value="ONLINE">ONLINE</option>
        <option value="IN_STORE">IN_STORE</option>
      </select>

      <select v-model="filters.status" @change="handleSearch" class="border p-2 rounded-md text-sm outline-none">
        <option value="">Tất cả trạng thái</option>
        <option value="0">Chờ xác nhận</option>
        <option value="1">Đã xác nhận</option>
        <option value="2">Đang giao hàng</option>
        <option value="3">Hoàn thành</option>
        <option value="4">Đã hủy</option>
      </select>

      <button @click="handleSearch" class="bg-blue-600 text-white font-medium rounded-md text-sm hover:bg-blue-700 transition py-2">Tìm kiếm</button>
    </div>

    <!-- Bảng danh sách dữ liệu -->
    <div class="bg-white rounded-md shadow-sm border overflow-hidden">
      <table class="w-full text-left border-collapse">
        <thead class="bg-gray-50 text-gray-600 text-sm border-b">
          <tr>
            <th class="p-4">Mã Đơn</th>
            <th class="p-4">Khách hàng</th>
            <th class="p-4">Số điện thoại</th>
            <th class="p-4 text-center">Nguồn đơn</th>
            <th class="p-4 text-right">Tổng tiền cuối</th>
            <th class="p-4 text-center">Ngày tạo</th>
            <th class="p-4 text-center">Hành động</th>
          </tr>
        </thead>
        <tbody class="divide-y text-sm text-gray-700">
          <tr v-for="order in orders" :key="order.id" class="hover:bg-gray-50 transition">
            <td class="p-4 font-semibold text-blue-600">#{{ order.id }}</td>
            <td class="p-4 font-medium">{{ order.customerName || 'N/A' }}</td>
            <td class="p-4">{{ order.customerPhone || 'N/A' }}</td>
            <td class="p-4 text-center">
              <span :class="['px-2 py-1 rounded text-xs font-semibold', order.orderType === 'ONLINE' ? 'bg-sky-50 text-sky-700' : 'bg-emerald-50 text-emerald-700']">{{ order.orderType }}</span>
            </td>
            <td class="p-4 text-right font-semibold text-gray-900">{{ formatPrice(order.finalAmount) }}</td>
            <td class="p-4 text-center text-gray-500">{{ formatDate(order.createdAt) }}</td>
            <td class="p-4 text-center">
              <button @click="openDetail(order.id)" class="px-3 py-1.5 bg-gray-800 text-white rounded text-xs hover:bg-gray-900 transition">Xem chi tiết</button>
            </td>
          </tr>
          <tr v-if="orders.length === 0">
            <td colspan="7" class="text-center py-10 text-gray-400">Không tìm thấy đơn hàng nào phù hợp.</td>
          </tr>
        </tbody>
      </table>

      <!-- Thanh phân trang (Pagination) -->
      <div class="flex justify-between items-center p-4 border-t bg-gray-50 text-sm text-gray-600">
        <div>Tổng số: <strong>{{ totalElements }}</strong> đơn hàng</div>
        <div class="flex items-center gap-2">
          <button @click="changePage(filters.page - 1)" :disabled="filters.page === 0" class="px-3 py-1 border bg-white rounded-md disabled:opacity-50">&lt;</button>
          <span>Trang {{ filters.page + 1 }} / {{ totalPages || 1 }}</span>
          <button @click="changePage(filters.page + 1)" :disabled="filters.page >= totalPages - 1" class="px-3 py-1 border bg-white rounded-md disabled:opacity-50">&gt;</button>
        </div>
      </div>
    </div>

    <!-- Modal chi tiết đơn hàng tích hợp -->
    <OrderDetailModal :order-id="selectedOrderId" :is-open="isModalOpen" @close="isModalOpen = false" @refresh="fetchOrders" />
  </div>
</template>