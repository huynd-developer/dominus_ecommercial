<template>
  <a-modal
    :open="open"
    title="Chi tiết đơn hàng"
    width="1100px"
    :footer="null"
    @cancel="close"
  >
    <div v-if="order">

      <!-- Thông tin đơn -->
      <a-descriptions bordered :column="2">

        <a-descriptions-item label="Mã đơn">
          #{{ order.id }}
        </a-descriptions-item>

        <a-descriptions-item label="Trạng thái">
          <OrderStatusTag :status="order.status"/>
        </a-descriptions-item>

        <a-descriptions-item label="Khách hàng">
          {{ order.customerName }}
        </a-descriptions-item>

        <a-descriptions-item label="Số điện thoại">
          {{ order.customerPhone }}
        </a-descriptions-item>

        <a-descriptions-item label="Loại đơn">
          {{ order.orderType }}
        </a-descriptions-item>

        <a-descriptions-item label="Thanh toán">
          {{ order.paymentMethod }}
        </a-descriptions-item>

        <a-descriptions-item :span="2" label="Địa chỉ">

          {{ order.shippingAddress || "-" }}

        </a-descriptions-item>

      </a-descriptions>

      <br>

      <!-- Danh sách sản phẩm -->

      <a-table

          :pagination="false"

          :data-source="order.items"

          row-key="id"

          bordered

      >

        <a-table-column
            title="Ảnh">

          <template #default="{record}">

            <img

                :src="record.image"

                width="60"

            >

          </template>

        </a-table-column>

        <a-table-column
            title="Sản phẩm">

          <template #default="{record}">

            <div>

              <b>

                {{record.productName}}

              </b>

              <br>

              SKU:

              {{record.sku}}

              <br>

              {{record.capacityName}}

              -

              {{record.bottleTypeName}}

            </div>

          </template>

        </a-table-column>

        <a-table-column
            title="SL"
            dataIndex="quantity"/>

        <a-table-column
            title="Đơn giá">

          <template #default="{record}">

            {{money(record.finalPrice)}}

          </template>

        </a-table-column>

        <a-table-column
            title="Thành tiền">

          <template #default="{record}">

            {{money(record.lineTotal)}}

          </template>

        </a-table-column>

      </a-table>

      <br>

      <!-- Tổng tiền -->

      <a-descriptions bordered>

        <a-descriptions-item label="Tạm tính">

          {{money(order.totalAmount)}}

        </a-descriptions-item>

        <a-descriptions-item label="Giảm giá">

          {{money(order.discountAmount)}}

        </a-descriptions-item>

        <a-descriptions-item label="Thanh toán">

          <b style="color:red">

            {{money(order.finalAmount)}}

          </b>

        </a-descriptions-item>

      </a-descriptions>

      <br>

      <!-- Nút thao tác -->

      <a-space>

        <a-button

            v-if="order.status===0"

            type="primary"

            @click="update(1)"

        >

          Xác nhận

        </a-button>

        <a-button

            v-if="order.status===0"

            danger

            @click="cancel"

        >

          Hủy đơn

        </a-button>

        <a-button

            v-if="order.status===1"

            type="primary"

            @click="update(2)"

        >

          Chuyển giao hàng

        </a-button>

        <a-button

            v-if="order.status===2"

            type="primary"

            @click="update(3)"

        >

          Hoàn thành

        </a-button>

        <a-button

            v-if="order.status===2"

            danger

            @click="update(5)"

        >

          Giao thất bại

        </a-button>

        <a-button

            v-if="order.status===3"

            @click="update(6)"

        >

          Yêu cầu hoàn

        </a-button>

        <a-button

            v-if="order.status===6"

            danger

            @click="update(7)"

        >

          Hoàn tất hoàn

        </a-button>

      </a-space>

    </div>

  </a-modal>
</template>

<script setup lang="ts">

import {computed} from "vue";

import Swal from "sweetalert2";

import {useOrderStore} from "../stores/orderStore";

import OrderStatusTag from "./OrderStatusTag.vue";

const props=defineProps<{

  open:boolean

}>();

const emit=defineEmits([

    "close"

]);

const store=useOrderStore();

const order=computed(

    ()=>store.selectedOrder

);

function close(){

  emit("close");

}

function money(value:number){

  return new Intl.NumberFormat(

      "vi-VN",

      {

        style:"currency",

        currency:"VND"

      }

  ).format(value);

}

async function update(status:number){

  if(!order.value) return;

  try{

    await store.updateStatus(
      order.value.id,
      status
    );

    await Swal.fire({
      icon:"success",
      title:"Thành công",
      text:"Cập nhật trạng thái thành công",
      timer:1500,
      showConfirmButton:false
    });

  }catch(e:any){

    Swal.fire({
      icon:"error",
      title:"Lỗi",
      text:e.response?.data?.message || "Có lỗi xảy ra"
    });

  }

}

async function cancel(){

  if(!order.value) return;

  const result = await Swal.fire({
    title:"Xác nhận hủy đơn?",
    icon:"warning",
    showCancelButton:true,
    confirmButtonText:"Hủy đơn",
    cancelButtonText:"Đóng"
  });

  if(!result.isConfirmed) return;

  try{

    await store.cancelOrder(order.value.id);

    Swal.fire({
      icon:"success",
      title:"Đã hủy đơn",
      timer:1500,
      showConfirmButton:false
    });

  }catch(e:any){

    Swal.fire({
      icon:"error",
      title:"Lỗi",
      text:e.response?.data?.message || "Có lỗi xảy ra"
    });

  }

}

</script>