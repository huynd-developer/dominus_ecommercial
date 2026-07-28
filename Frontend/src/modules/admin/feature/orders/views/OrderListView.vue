<template>
  <div class="order-page">
    <div class="page-header mb-4">
      <h3 class="fw-bold mb-0 text-dark">
        <i class="bi bi-receipt-cutoff me-2"></i> Quản lý đơn hàng
      </h3>
    </div>

    <!-- Component Lọc -->
    <OrderFilter />

    <!-- Tabs Trạng thái (Thay thế cho Dropdown) -->
    <a-card :bordered="false" class="shadow-sm rounded-3 mt-3">
      <a-tabs v-model:activeKey="activeTab" @change="onTabChange" type="card">
        <a-tab-pane key="ALL" tab="Tất cả"></a-tab-pane>
        <a-tab-pane key="0" tab="Chờ xác nhận"></a-tab-pane>
        <a-tab-pane key="1" tab="Đã xác nhận"></a-tab-pane>
        <a-tab-pane key="2" tab="Đang giao"></a-tab-pane>
        <a-tab-pane key="3" tab="Hoàn thành"></a-tab-pane>
        <a-tab-pane key="5" tab="Giao thất bại"></a-tab-pane>
        <a-tab-pane key="6" tab="Yêu cầu hoàn"></a-tab-pane>
        <a-tab-pane key="7" tab="Đã hoàn hàng"></a-tab-pane>
        <a-tab-pane key="4" tab="Đã hủy"></a-tab-pane>
      </a-tabs>

      <!-- Bảng dữ liệu -->
      <OrderTable @detail="showDetail" />
    </a-card>

    <!-- Modal chi tiết -->
    <OrderDetailModal :open="openDetail" @close="openDetail=false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import OrderFilter from "../components/OrderFilter.vue";
import OrderTable from "../components/OrderTable.vue";
import OrderDetailModal from "../components/OrderDetailModal.vue";
import { useOrderStore } from "../stores/orderStore";

const store = useOrderStore();
const openDetail = ref(false);
const activeTab = ref("ALL");

async function showDetail(id: number) {
  await store.loadDetail(id);
  openDetail.value = true;
}

// Khi chuyển Tab, set lại trạng thái cho Store và tìm kiếm
function onTabChange(key: string) {
  store.status = key === "ALL" ? undefined : Number(key);
  store.currentPage = 0;
  store.search();
}

onMounted(() => {
  store.loadOrders();
});
</script>