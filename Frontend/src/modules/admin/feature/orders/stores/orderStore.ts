import { defineStore } from "pinia";
import { ref } from "vue";
// 1. SỬA LẠI CÁCH IMPORT SERVICE (Có ngoặc nhọn)
import { orderService } from "../services/order.service";
// 2. SỬA LẠI IMPORT TYPE ĐÚNG VỚI FILE MỚI
import type { AdminOrderResponse } from "../types/order.type";

export const useOrderStore = defineStore("order-store", () => {
  const loading = ref(false);
  // 3. ĐỔI TYPE CHO CÁC BIẾN NÀY
  const orders = ref<AdminOrderResponse[]>([]);
  const selectedOrder = ref<AdminOrderResponse | null>(null);
  
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(10);
  
  const keyword = ref("");
  const status = ref<number | undefined>();
  const orderType = ref<string | undefined>(); 
  const dateRange = ref<any>(); 

  async function loadOrders() {
    loading.value = true;
    try {
      const payload: any = {
        keyword: keyword.value || undefined,
        status: status.value,
        orderType: orderType.value || undefined,
        page: currentPage.value,
        size: pageSize.value,
      };

      if (dateRange.value && dateRange.value.length === 2) {
         payload.startDate = dateRange.value[0].format('YYYY-MM-DD');
         payload.endDate = dateRange.value[1].format('YYYY-MM-DD');
      }

      // 4. SỬA LẠI TÊN HÀM (getOrders) VÀ CÁCH TRẢ DỮ LIỆU
      const res = await orderService.getOrders(payload);

      orders.value = res.content;
      totalElements.value = res.totalElements;
      totalPages.value = res.totalPages;
    } finally {
      loading.value = false;
    }
  }

  async function loadDetail(id: number) {
    loading.value = true;
    try {
      // 5. ĐỔI THÀNH getOrderDetail VÀ BỎ .data
      const res = await orderService.getOrderDetail(id);
      selectedOrder.value = res;
    } finally {
      loading.value = false;
    }
  }

  async function updateStatus(id: number, newStatus: number) {
    loading.value = true;
    try {
      // 6. ĐỔI THÀNH updateOrderStatus
      await orderService.updateOrderStatus(id, newStatus);
      await loadDetail(id);
      await loadOrders();
    } finally {
      loading.value = false;
    }
  }

  async function cancelOrder(id: number) {
    loading.value = true;
    try {
      // Do service ko có hàm cancel, ta dùng updateOrderStatus với status = 4 (Hủy)
      await orderService.updateOrderStatus(id, 4);
      await loadDetail(id);
      await loadOrders();
    } finally {
      loading.value = false;
    }
  }

  function changePage(page: number) {
    currentPage.value = page;
    loadOrders();
  }

  function search() {
    currentPage.value = 0;
    loadOrders();
  }

  return {
    loading,
    orders,
    selectedOrder,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    keyword,
    status,
    orderType,
    dateRange, 
    loadOrders,
    loadDetail,
    updateStatus,
    cancelOrder,
    search,
    changePage,
  };
});