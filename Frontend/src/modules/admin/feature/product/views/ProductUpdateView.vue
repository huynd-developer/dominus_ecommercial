<template>
  <div class="container-fluid py-4">

    <!-- Breadcrumb -->
    <nav class="mb-3">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <a href="#" @click.prevent="router.push('/admin')">Dashboard</a>
        </li>
        <li class="breadcrumb-item">
          <a href="#" @click.prevent="router.push('/admin/products')">
            Sản phẩm
          </a>
        </li>
        <li class="breadcrumb-item active">
          Cập nhật
        </li>
      </ol>
    </nav>

    <!-- Header -->
    <div class="card border-0 shadow-sm rounded-4 mb-4">

      <div class="card-body">

        <div class="d-flex justify-content-between align-items-center">

          <div>

            <h2 class="fw-bold mb-1">
              <i class="bi bi-pencil-square text-primary me-2"></i>
              Cập nhật sản phẩm
            </h2>

            <p class="text-secondary mb-0">
              Chỉnh sửa thông tin sản phẩm trong hệ thống
            </p>

          </div>

          <button
              class="btn btn-outline-secondary px-4"
              @click="back"
          >
            <i class="bi bi-arrow-left me-2"></i>
            Quay lại
          </button>

        </div>

      </div>

    </div>

    <!-- Loading -->
    <div
        v-if="loading"
        class="card border-0 shadow rounded-4"
    >
      <div class="card-body text-center py-5">

        <div
            class="spinner-border text-primary"
            style="width:3rem;height:3rem"
        ></div>

        <h5 class="mt-3">
          Đang tải dữ liệu...
        </h5>

      </div>
    </div>

    <!-- Form -->
    <transition name="fade">

      <div
          v-if="product"
          class="card border-0 shadow rounded-4"
      >

        <div class="card-body">

          <ProductForm
              :product="product"
              @submit="update"
          />

        </div>

      </div>

    </transition>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

import ProductForm from "../components/ProductForm.vue";
import { useProductStore } from "../store/productStore";

const store = useProductStore();

const router = useRouter();
const route = useRoute();

const product = ref();
const loading = ref(true);

onMounted(async () => {

  try {

    product.value = await store.getById(
      Number(route.params.id)
    );

  } catch (e) {

    console.error(e);

  } finally {

    loading.value = false;

  }

});

const update = async (formData: FormData) => {

  try {

    await store.update(
      Number(route.params.id),
      formData
    );

    alert("Cập nhật thành công");

    router.push("/admin/products");

  } catch (e) {

    console.error(e);

    alert("Có lỗi xảy ra");

  }

};

const back = () => {

  router.back();

};
</script>

<style scoped>

.container-fluid{
    background:#f8fafc;
    min-height:100vh;
}

.card{
    border-radius:20px;
}

.btn{
    border-radius:12px;
}

.fade-enter-active,
.fade-leave-active{
    transition:.35s;
}

.fade-enter-from{
    opacity:0;
    transform:translateY(20px);
}

.fade-leave-to{
    opacity:0;
}

</style>