<script setup lang="ts">
import { ref, watch } from 'vue';
import { orderAdminService } from '../service/orderAdminService';
import type { OrderDetailAdminResponse } from '../types';

const props = defineProps<{ orderId: number | null; isOpen: boolean }>();
const emit = defineEmits(['close', 'refresh']);

const order = ref<OrderDetailAdminResponse | null>(null);
const loading = ref(false);

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: 'Chờ xác nhận', color: 'bg-amber-100 text-amber-800' },
  1: { text: 'Đã xác nhận', color: 'bg-blue-100 text-blue-800' },
  2: { text: 'Đang giao hàng', color: 'bg-indigo-100 text-indigo-800' },
  3: { text: 'Hoàn thành', color: 'bg-green-100 text-green-800' },
  4: { text: 'Đã hủy', color: 'bg-red-100 text-red-800' }
};

const fetchDetail = async () => {
  if (!props.orderId) return;
  loading.value = true;
  try {
    order.value = await orderAdminService.getOrderDetail(props.orderId);
  } catch (error: any) {
    alert(error.response?.data?.error || 'Không thể tải chi tiết đơn hàng');
  } finally {
    loading.value = false;
  }
};

watch(() => props.isOpen, (newVal) => { if (newVal) fetchDetail(); });

const handleNextStatus = async () => {
  if (!order.value || !confirm('Bạn có chắc chắn muốn chuyển trạng thái đơn hàng này?')) return;
  try {
    await orderAdminService.nextStatus(order.value.id);
    alert('Cập nhật trạng thái thành công!');
    fetchDetail();
    emit('refresh');
  } catch (error: any) {
    alert(error.response?.data?.error || 'Thao tác thất bại');
  }
};

const handleCancelOrder = async () => {
  if (!order.value || !confirm('Bạn có chắc chắn muốn HỦY đơn hàng này và HOÀN tồn kho?')) return;
  try {
    await orderAdminService.cancelOrder(order.value.id);
    alert('Đã hủy đơn hàng thành công!');
    fetchDetail();
    emit('refresh');
  } catch (error: any) {
    alert(error.response?.data?.error || 'Không thể hủy đơn');
  }
};

const formatPrice = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto p-6">
      <div class="flex justify-between items-center border-b pb-3 mb-4">
        <h3 class="text-xl font-bold text-gray-800">Chi tiết đơn hàng #{{ orderId }}</h3>
        <button @click="emit('close')" class="text-gray-500 hover:text-gray-700 text-2xl">&times;</button>
      </div>

      <div v-if="loading" class="text-center py-10 text-gray-500">Đang tải dữ liệu...</div>

      <div v-else-if="order">
        <!-- Thông tin tổng quan & Người nhận -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6 bg-gray-50 p-4 rounded-md">
          <div>
            <p class="mb-2"><strong>Người nhận:</strong> {{ order.customerName || 'N/A' }}</p>
            <p class="mb-2"><strong>Số điện thoại:</strong> {{ order.customerPhone || 'N/A' }}</p>
            <p><strong>Địa chỉ giao hàng:</strong> {{ order.shippingAddress || 'Mua tại cửa hàng' }}</p>
          </div>
          <div>
            <p class="mb-2"><strong>Hình thức:</strong> 
              <span :class="order.orderType === 'ONLINE' ? 'text-blue-600 font-semibold' : 'text-green-600 font-semibold'"> {{ order.orderType }}</span>
            </p>
            <p class="mb-2"><strong>Thanh toán:</strong> {{ order.paymentMethod }}</p>
            <p class="flex items-center gap-2"><strong>Trạng thái:</strong> 
              <span :class="['px-2.5 py-0.5 rounded-full text-xs font-medium', statusMap[order.status]?.color]">{{ statusMap[order.status]?.text }}</span>
            </p>
          </div>
        </div>

        <!-- Danh sách sản phẩm (OrderItem) -->
        <h4 class="font-bold text-gray-700 mb-3">Danh sách sản phẩm</h4>
        <div class="border rounded-md overflow-hidden mb-6">
          <table class="w-full text-left border-collapse">
            <thead class="bg-gray-100 text-gray-600 text-sm">
              <tr>
                <th class="p-3">Sản phẩm</th>
                <th class="p-3 text-center">Số lượng</th>
                <th class="p-3 text-right">Giá gốc</th>
                <th class="p-3 text-right">Giảm giá</th>
                <th class="p-3 text-right">Thành tiền</th>
              </tr>
            </thead>
            <tbody class="divide-y text-sm">
              <tr v-for="item in order.items" :key="item.id">
                <td class="p-3 flex items-center gap-3">
                  <img :src="item.image || 'https://via.placeholder.com/50'" class="w-12 h-12 object-cover rounded" />
                  <div>
                    <div class="font-medium text-gray-800">{{ item.productName }}</div>
                    <div class="text-xs text-gray-500">Phân loại: {{ item.capacity }} ml | Đóng chai: {{ item.bottleType }}</div>
                  </div>
                </td>
                <td class="p-3 text-center">{{ item.quantity }}</td>
                <td class="p-3 text-right text-gray-400 line-through">{{ formatPrice(item.originalPrice) }}</td>
                <td class="p-3 text-right text-red-500">-{{ formatPrice(item.discountAmount) }}</td>
                <td class="p-3 text-right font-medium text-gray-900">{{ formatPrice(item.finalPrice * item.quantity) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Khối tiền tệ (Amounts) -->
        <div class="w-full md:w-1/2 ml-auto space-y-2 border-t pt-4 mb-6 text-sm">
          <div class="flex justify-between text-gray-600"><span>Tổng tiền hàng (TotalAmount):</span><span>{{ formatPrice(order.totalAmount) }}</span></div>
          <div class="flex justify-between text-red-500"><span>Giảm giá Voucher (DiscountAmount):</span><span>-{{ formatPrice(order.discountAmount) }}</span></div>
          <div class="flex justify-between text-base font-bold text-gray-900 border-t pt-2"><span>Thực nhận (FinalAmount):</span><span class="text-xl text-emerald-600">{{ formatPrice(order.finalAmount) }}</span></div>
        </div>

        <!-- Nút xử lý chuyển trạng thái / Hủy đơn -->
        <div class="flex justify-end gap-3 border-t pt-4">
          <button v-if="order.status < 3" @click="handleCancelOrder" class="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition">Hủy đơn hàng</button>
          <button v-if="order.status < 3" @click="handleNextStatus" class="px-4 py-2 bg-emerald-600 text-white rounded-md hover:bg-emerald-700 transition">
            {{ order.status === 0 ? 'Xác nhận đơn' : order.status === 1 ? 'Giao hàng' : 'Hoàn thành đơn' }}
          </button>
          <button @click="emit('close')" class="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition">Đóng</button>
        </div>
      </div>
    </div>
  </div>
</template>