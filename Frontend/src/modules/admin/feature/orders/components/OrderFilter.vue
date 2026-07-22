<template>
  <a-card :bordered="false" class="mb-3">
    <a-row :gutter="16">

      <a-col :span="8">
        <a-input
          v-model:value="store.keyword"
          placeholder="Tìm theo tên hoặc số điện thoại..."
          allow-clear
          @pressEnter="search"
        />
      </a-col>

      <a-col :span="5">
        <a-select
          v-model:value="store.status"
          style="width:100%"
          placeholder="Trạng thái"
          allow-clear
        >
          <a-select-option :value="0">Chờ xác nhận</a-select-option>
          <a-select-option :value="1">Đã xác nhận</a-select-option>
          <a-select-option :value="2">Đang giao</a-select-option>
          <a-select-option :value="3">Hoàn thành</a-select-option>
          <a-select-option :value="4">Đã hủy</a-select-option>
          <a-select-option :value="5">Giao thất bại</a-select-option>
          <a-select-option :value="6">Yêu cầu hoàn</a-select-option>
          <a-select-option :value="7">Hoàn tất hoàn hàng</a-select-option>
        </a-select>
      </a-col>

      <a-col :span="5">
        <a-select
          v-model:value="store.orderType"
          style="width:100%"
          placeholder="Loại đơn"
          allow-clear
        >
          <a-select-option value="ONLINE">
            Online
          </a-select-option>

          <a-select-option value="OFFLINE">
            Tại quầy
          </a-select-option>
        </a-select>
      </a-col>

      <a-col :span="6">

        <a-space>

          <a-button
              type="primary"
              @click="search">

            Tìm kiếm

          </a-button>

          <a-button
              @click="reset">

            Làm mới

          </a-button>

        </a-space>

      </a-col>

    </a-row>
  </a-card>
</template>

<script setup lang="ts">

import {useOrderStore} from "../stores/orderStore";

const store = useOrderStore();

function search(){

  store.search();

}

function reset(){

  store.keyword="";

  store.status=undefined;

  store.orderType="";

  store.search();

}

</script>