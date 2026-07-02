<template>
  <div class="container-fluid py-4">

    <!-- Breadcrumb -->
    <!-- <nav class="mb-3">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <a href="#" @click.prevent="router.push('/admin')">
            Dashboard
          </a>
        </li>

        <li class="breadcrumb-item">
          <a href="#" @click.prevent="router.push('/admin/products')">
            Sản phẩm
          </a>
        </li>

        <li class="breadcrumb-item active">
          Thêm mới
        </li>
      </ol>
    </nav> -->

    <!-- Header -->
    <!-- <div class="card border-0 shadow-sm rounded-4 mb-4">

      <div class="card-body">

        <div class="d-flex justify-content-between align-items-center flex-wrap">

          <div>

            <h2 class="fw-bold mb-1">
              <i class="bi bi-plus-circle-fill text-success me-2"></i>
              Thêm sản phẩm
            </h2>

            <p class="text-muted mb-0">
              Tạo sản phẩm mới cho hệ thống
            </p>

          </div>

          <button
              class="btn btn-outline-secondary px-4 mt-3 mt-md-0"
              @click="back"
          >
            <i class="bi bi-arrow-left me-2"></i>
            Quay lại
          </button>

        </div>

      </div>

    </div> -->

    <!-- Form -->
    <transition name="fade">

      <div class="card border-0 shadow rounded-4">

        <div class="card-body p-4">

          <ProductForm
              :loading="loading"
              @submit="save"
          />

        </div>

      </div>

    </transition>

  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";

import ProductForm from "../components/ProductForm.vue";
import { useProductStore } from "../store/productStore";

const router = useRouter();
const store = useProductStore();

const loading = ref(false);

const save = async (formData: FormData) => {

  loading.value = true;

  try {

    await store.create(formData);

    alert("Thêm sản phẩm thành công!");

    router.push("/admin/products");

  } catch (e) {

    console.error(e);

    alert("Có lỗi xảy ra!");

  } finally {

    loading.value = false;

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
    transition:.3s;
}

.btn:hover{
    transform:translateY(-2px);
}

.fade-enter-active,
.fade-leave-active{
    transition:.35s;
}

.fade-enter-from{
    opacity:0;
    transform:translateY(15px);
}

.fade-leave-to{
    opacity:0;
}

</style>