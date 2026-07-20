<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Swal from 'sweetalert2'

import { useProductStore } from '../stores/productStore'
import { productService } from '../services/productService'

import type { Product } from '../types/product.type'

import ProductList from '../components/ProductList.vue'
import ProductModal from '../components/ProductModal.vue'

const store = useProductStore()

const router = useRouter()
const route = useRoute()

const searchQuery = ref('')

const showModal = ref(false)

const selectedProduct = ref<Product | null>(null)

const currentPage = ref(
  Number(route.query.page) || 1
)

const pageSize = ref(10)

onMounted(async () => {
  await store.fetchDropdowns()
  await store.fetchProducts()
})

const filteredData = computed(() => {
  if (!searchQuery.value) {
    return store.products
  }

  const keyword =
    searchQuery.value.toLowerCase()

  return store.products.filter(
    (item) =>
      item.name?.toLowerCase().includes(keyword) ||
      item.brandName
        ?.toLowerCase()
        .includes(keyword) ||
      item.categoryName
        ?.toLowerCase()
        .includes(keyword) ||
      item.concentrationName
        ?.toLowerCase()
        .includes(keyword)
  )
})

const totalPages = computed(() => {
  return (
    Math.ceil(
      filteredData.value.length /
        pageSize.value
    ) || 1
  )
})

const paginatedData = computed(() => {
  const start =
    (currentPage.value - 1) *
    pageSize.value

  return filteredData.value.slice(
    start,
    start + pageSize.value
  )
})

watch(currentPage, (newPage) => {
  router.replace({
    query: {
      ...route.query,
      page: newPage
    }
  })
})

watch(searchQuery, () => {
  currentPage.value = 1
})

const openAddModal = () => {
  selectedProduct.value = null
  showModal.value = true
}

const openEditModal = (
  item: Product
) => {
  selectedProduct.value = item
  showModal.value = true
}

const closeAllModals = () => {
  showModal.value = false
}

const stopSelling = async (
  id: number
) => {
  const result = await Swal.fire({
    title: 'Ẩn sản phẩm?',
    text: 'Sản phẩm sẽ được chuyển sang trạng thái ngừng kinh doanh.',
    icon: 'warning',

    showCancelButton: true,

    confirmButtonColor: '#f59e0b',

    confirmButtonText: 'Ẩn sản phẩm',

    cancelButtonText: 'Hủy'
  })

  if (!result.isConfirmed) {
    return
  }

  try {
    await productService.deleteProduct(id)

    Swal.fire({
      icon: 'success',
      title: 'Thành công',
      text: 'Đã ẩn sản phẩm',
      timer: 1200,
      showConfirmButton: false
    })

    await store.fetchProducts()
  } catch (error) {
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể thực hiện thao tác'
    })
  }
}
</script>

<template>
  <div class="product-page">

    <!-- Header -->
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-box-seam me-2"></i>
          Quản lý nước hoa
        </h3>

        <!-- <div class="text-muted small">
          Tổng cộng
          <b>{{ filteredData.length }}</b>
          sản phẩm
        </div> -->
      </div>

      <button
        class="btn btn-primary px-4"
        @click="openAddModal"
      >
        <i class="bi bi-plus-circle me-2"></i>
        Thêm sản phẩm
      </button>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">

      <div class="search-box">

        <i class="bi bi-search"></i>

        <input
          v-model="searchQuery"
          placeholder="Tìm theo tên, thương hiệu..."
        />

      </div>

      <div class="toolbar-right">

        <select
          v-model="pageSize"
          class="form-select"
          style="width:100px"
        >
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>

        <button
          class="btn btn-light"
          @click="store.fetchProducts"
        >
          <i class="bi bi-arrow-clockwise"></i>
        </button>

      </div>

    </div>

    <!-- Table -->
    <div class="table-wrapper">

      <ProductList
        :paginated-data="paginatedData"
        :brand-list="store.brandList"
        @edit="openEditModal"
        @stop-selling="stopSelling"
      />

    </div>

    <!-- Footer -->
    <div class="footer">

      <div class="text-muted">

        Hiển thị

        <b>{{ paginatedData.length }}</b>

        /

        <b>{{ filteredData.length }}</b>

      </div>

      <div class="pagination">

        <button
          class="btn btn-light"
          :disabled="currentPage==1"
          @click="currentPage--"
        >
          ←
        </button>

        <span>

          {{ currentPage }}

          /

          {{ totalPages }}

        </span>

        <button
          class="btn btn-light"
          :disabled="currentPage==totalPages"
          @click="currentPage++"
        >
          →
        </button>

      </div>

    </div>

    <Teleport to="body">
      <ProductModal
        v-if="showModal"
        :product-selected="selectedProduct"
        :brand-list="store.brandList"
        :category-list="store.categoryList"
        :concentration-list="store.concentrationList"
        :fragrance-family-list="store.fragranceFamilyList"
        :capacity-list="store.capacityList"
        :bottle-type-list="store.bottleTypeList"
        @close="closeAllModals"
        @refresh="store.fetchProducts"
      />
    </Teleport>

  </div>
</template>

<style scoped>

.product-page{
    display:flex;
    flex-direction:column;
    background:white;
    border-radius:20px;
    box-shadow:0 10px 30px rgba(0,0,0,.05);
    overflow:hidden;
}

.page-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    padding:24px 30px;
    border-bottom:1px solid #eef2f7;
}

.page-title{
    margin:0;
    font-size:26px;
    font-weight:700;
}

.toolbar{

    display:flex;
    justify-content:space-between;
    align-items:center;

    padding:18px 30px;

    background:#fafafa;

}

.search-box{

    width:420px;

    position:relative;

}

.search-box i{

    position:absolute;

    top:50%;
    left:16px;

    transform:translateY(-50%);

    color:#94a3b8;

}

.search-box input{

    width:100%;

    padding:12px 18px 12px 45px;

    border-radius:999px;

    border:1px solid #e2e8f0;

    transition:.25s;

}

.search-box input:focus{

    outline:none;

    border-color:#3b82f6;

    box-shadow:0 0 0 4px rgba(59,130,246,.15);

}

.toolbar-right{

    display:flex;
    gap:10px;
    align-items:center;

}

.table-wrapper{

    padding:20px 24px;

}

.footer{

    display:flex;

    justify-content:space-between;

    align-items:center;

    padding:18px 28px;

    border-top:1px solid #eee;

    background:#fafafa;

}

.pagination{

    display:flex;

    align-items:center;

    gap:12px;

}

.btn{

    border-radius:12px;

}

.btn-primary{

    border:none;

    background:#2563eb;

}

.btn-primary:hover{

    background:#1d4ed8;

}

</style>