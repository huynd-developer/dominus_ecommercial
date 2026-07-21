<template>

  <div>

    <OrderFilter />

    <OrderTable
        @detail="showDetail"
    />

    <OrderDetailModal
        :open="openDetail"
        @close="openDetail=false"
    />

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

async function showDetail(id:number){

    await store.loadDetail(id);

    openDetail.value=true;

}

onMounted(() => {

  store.loadOrders();

});
</script>