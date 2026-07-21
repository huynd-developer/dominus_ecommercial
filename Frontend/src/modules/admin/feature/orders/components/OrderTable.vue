<template>
  <a-card :bordered="false">

    <a-table
        :columns="columns"
        :data-source="store.orders"
        :loading="store.loading"
        :pagination="false"
        row-key="id">

        <template #bodyCell="{ column, record }">

            <!-- Loại đơn -->
            <template v-if="column.key==='orderType'">

                <a-tag
                    :color="record.orderType==='ONLINE' ? 'blue':'green'">

                    {{ record.orderType==='ONLINE'
                    ? 'Online'
                    : 'Tại quầy'
                    }}

                </a-tag>

            </template>

            <!-- Tiền -->
            <template v-if="column.key==='finalAmount'">

                {{ money(record.finalAmount) }}

            </template>

            <!-- Trạng thái -->
            <template v-if="column.key==='status'">

                <OrderStatusTag
                        :status="record.status"/>

            </template>

            <!-- Ngày -->
            <template v-if="column.key==='createdAt'">

                {{ formatDate(record.createdAt) }}

            </template>

            <!-- Thao tác -->
            <template v-if="column.key==='action'">

                <a-space>

                    <a-button
                            type="primary"
                            size="small"
                            @click="detail(record.id)">

                        Chi tiết

                    </a-button>

                </a-space>

            </template>

        </template>

    </a-table>

    <div class="mt-3 text-end">

        <a-pagination

                :current="store.currentPage+1"

                :pageSize="store.pageSize"

                :total="store.totalElements"

                show-size-changer

                @change="changePage"

                @showSizeChange="changeSize"

        />

    </div>

  </a-card>
</template>

<script setup lang="ts">


import {useOrderStore} from "../stores/orderStore";

import OrderStatusTag from "./OrderStatusTag.vue";

const emit = defineEmits<{
  (e: "detail", id: number): void;
}>();

const store = useOrderStore();

const columns=[

    {
        title:"Mã đơn",
        dataIndex:"id",
        key:"id"
    },

    {
        title:"Khách hàng",
        dataIndex:"customerName",
        key:"customerName"
    },

    {
        title:"SĐT",
        dataIndex:"customerPhone",
        key:"customerPhone"
    },

    {
        title:"Loại đơn",
        key:"orderType"
    },

    {
        title:"Thanh toán",
        dataIndex:"paymentMethod",
        key:"paymentMethod"
    },

    {
        title:"Tổng tiền",
        key:"finalAmount"
    },

    {
        title:"Trạng thái",
        key:"status"
    },

    {
        title:"Ngày tạo",
        key:"createdAt"
    },

    {
        title:"",
        key:"action"
    }

]

function money(value:number){

    return new Intl.NumberFormat(
        "vi-VN",
        {
            style:"currency",
            currency:"VND"
        }
    ).format(value);

}

function formatDate(date:string){

    return new Date(date)
        .toLocaleString("vi-VN");

}

function detail(id: number) {
    emit("detail", id);
}

function changePage(page:number){

    store.changePage(page - 1);

}

function changeSize(
    page:number,
    size:number
){

    store.currentPage=0;

    store.pageSize=size;

    store.loadOrders();

}

</script>