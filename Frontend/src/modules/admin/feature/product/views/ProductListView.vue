<template>
  <div class="container-fluid py-4">

    <!-- HEADER -->
    <div class="d-flex justify-content-between align-items-center mb-4">

      <div>
        <h2 class="fw-bold mb-1">
          <i class="bi bi-box-seam me-2 text-primary"></i>
          Quản lý sản phẩm
        </h2>
        
      </div>

      <button
          class="btn btn-primary px-4"
          @click="router.push('/admin/products/create')"
      >
        <i class="bi bi-plus-circle me-2"></i>
        Thêm sản phẩm
      </button>

    </div>

    <!-- SEARCH -->
    <div class="card border-0 shadow-sm rounded-4">

      <div class="card-body">

        <ProductSearch
            @search="search"
        />

      </div>

    </div>

    <!-- STATISTIC -->

    <div class="row mt-4">

      <div class="col-lg-3">

        <div class="card border-0 shadow-sm rounded-4">

          <div class="card-body">

            <h6 class="text-secondary">
              Tổng sản phẩm
            </h6>

            <h2 class="fw-bold text-primary">
              {{ store.page.totalElements }}
            </h2>

          </div>

        </div>

      </div>

      <div class="col-lg-3">

        <div class="card border-0 shadow-sm rounded-4">

          <div class="card-body">

            <h6 class="text-secondary">
              Trang hiện tại
            </h6>

            <h2 class="fw-bold text-success">
              {{ store.currentPage + 1 }}
            </h2>

          </div>

        </div>

      </div>

      <div class="col-lg-3">

        <div class="card border-0 shadow-sm rounded-4">

          <div class="card-body">

            <h6 class="text-secondary">
              Kích thước trang
            </h6>

            <h2 class="fw-bold text-warning">
              {{ store.page.size }}
            </h2>

          </div>

        </div>

      </div>

      <div class="col-lg-3">

        <div class="card border-0 shadow-sm rounded-4">

          <div class="card-body">

            <h6 class="text-secondary">
              Tổng trang
            </h6>

            <h2 class="fw-bold text-danger">
              {{ store.page.totalPages }}
            </h2>

          </div>

        </div>

      </div>

    </div>

    <!-- TABLE -->

    <div class="card border-0 shadow mt-4 rounded-4">

      <div
          class="card-header bg-white border-0 py-3"
      >

        <h5 class="fw-bold mb-0">

          Danh sách sản phẩm

        </h5>

      </div>

      <div class="card-body">

        <ProductTable
            :products="store.products"
            :loading="store.loading"
            @edit="edit"
            @delete="remove"
        />

        <div class="mt-4 d-flex justify-content-center">

          <ProductPagination
              :page="store.page"
              @change="changePage"
          />

        </div>

      </div>

    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useProductStore } from "../store/productStore";

import ProductSearch from "../components/ProductSearch.vue";
import ProductTable from "../components/ProductTable.vue";
import ProductPagination from "../components/ProductPagination.vue";

const router = useRouter();
const store = useProductStore();

onMounted(async () => {
  await store.fetchProducts();
});

const search = async (keyword: string) => {
  store.keyword = keyword;
  store.currentPage = 0;
  await store.fetchProducts();
};

const changePage = async (page: number) => {
  store.currentPage = page;
  await store.fetchProducts();
};

const edit = (id: number) => {
  router.push(`/admin/products/${id}`);
};

const remove = async (id: number) => {
  if (!confirm("Bạn có chắc muốn xóa?")) return;

  try {
    await store.delete(id);
    alert("Xóa thành công");
  } catch (e) {
    alert("Xóa thất bại");
  }
};
</script>

<style scoped>

.card{
    transition:.3s;
}

.card:hover{
    transform:translateY(-2px);
}

.btn-primary{
    border-radius:12px;
}

.rounded-4{
    border-radius:18px!important;
}

</style>