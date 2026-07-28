<template>
  <a-card :bordered="false" class="shadow-sm rounded-3">
    <a-row :gutter="16">
      <!-- Mở rộng tìm kiếm -->
      <a-col :span="8">
        <a-input
          v-model:value="store.keyword"
          placeholder="Mã đơn, Tên khách hàng hoặc Số điện thoại..."
          allow-clear
          @pressEnter="search"
        >
          <template #prefix>
            <i class="bi bi-search text-muted"></i>
          </template>
        </a-input>
      </a-col>

      <!-- Bộ lọc Ngày tháng -->
      <a-col :span="6">
        <a-range-picker
          v-model:value="store.dateRange"
          format="DD/MM/YYYY"
          :placeholder="['Từ ngày', 'Đến ngày']"
          style="width: 100%"
        />
      </a-col>

      <!-- Loại đơn (Chỉ giữ lại Online / Tại quầy) -->
      <a-col :span="5">
        <a-select
          v-model:value="store.orderType"
          style="width: 100%"
          placeholder="Loại đơn hàng"
          allow-clear
        >
          <a-select-option value="ONLINE">Online (Giao hàng)</a-select-option>
          <!-- ĐÃ SỬA: Đổi OFFLINE thành IN_STORE để khớp với dữ liệu trong Database -->
          <a-select-option value="IN_STORE">Tại quầy</a-select-option>
        </a-select>
      </a-col>

      <a-col :span="5">
        <a-space>
          <a-button type="primary" @click="search">
            <i class="bi bi-funnel me-1"></i> Lọc
          </a-button>
          <a-button @click="reset">
            <i class="bi bi-arrow-clockwise me-1"></i> Làm mới
          </a-button>
        </a-space>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { useOrderStore } from "../stores/orderStore";

const store = useOrderStore();

function search() {
  store.search();
}

function reset() {
  store.keyword = "";
  store.orderType = undefined;
  store.dateRange = undefined; // Reset biến ngày tháng
  store.search();
}
</script>