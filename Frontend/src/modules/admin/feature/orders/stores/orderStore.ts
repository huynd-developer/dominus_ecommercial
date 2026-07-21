import { defineStore } from "pinia";
import { ref } from "vue";

import orderAdminService from "../service/orderAdminService";

import type { Order } from "../types/order";
import type { OrderDetail } from "../types/OrderDetail";

export const useOrderStore = defineStore("order-store", () => {

  const loading = ref(false);

  const orders = ref<Order[]>([]);

  const selectedOrder = ref<OrderDetail | null>(null);

  const totalElements = ref(0);

  const totalPages = ref(0);

  const currentPage = ref(0);

  const pageSize = ref(10);

  const keyword = ref("");

  const status = ref<number | undefined>();

  const orderType = ref("");

  async function loadOrders() {

    loading.value = true;

    try {

      const res = await orderAdminService.search({

        keyword: keyword.value || undefined,

        status: status.value,

        orderType: orderType.value || undefined,

        page: currentPage.value,

        size: pageSize.value,

      });

      orders.value = res.data.content;

      totalElements.value = res.data.totalElements;

      totalPages.value = res.data.totalPages;

    } finally {

      loading.value = false;

    }

  }

  async function loadDetail(id: number) {

    loading.value = true;

    try {

      const res = await orderAdminService.getDetail(id);

      selectedOrder.value = res.data;

    } finally {

      loading.value = false;

    }

  }

  async function updateStatus(id: number, newStatus: number) {

    loading.value = true;

    try {

      await orderAdminService.updateStatus(id, newStatus);

      await loadDetail(id);

      await loadOrders();

    } finally {

      loading.value = false;

    }

  }

  async function cancelOrder(id: number) {

    loading.value = true;

    try {

      await orderAdminService.cancel(id);

      await loadDetail(id);

      await loadOrders();

    } finally {

      loading.value = false;

    }

  }

  function changePage(page:number){

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

    loadOrders,

    loadDetail,

    updateStatus,

    cancelOrder,

    search,

    changePage,

  };

});