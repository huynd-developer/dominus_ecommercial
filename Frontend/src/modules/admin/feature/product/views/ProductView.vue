<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Swal from 'sweetalert2'

import { useProductStore } from '../stores/productStore'
import { productService } from '../services/productService'

import type { Product } from '../types/product.type'

import ProductList from '../components/ProductList.vue'
import ProductModal from '../components/ProductModal.vue'
import ProductImageModal from '../components/ProductImageModal.vue'

const store = useProductStore()

const router = useRouter()
const route = useRoute()

const searchQuery = ref('')

const showModal = ref(false)

const showImageModal = ref(false)

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

const openImageModal = (
  item: Product
) => {
  selectedProduct.value = item
  showImageModal.value = true
}

const closeAllModals = () => {
  showModal.value = false
  showImageModal.value = false
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
  <div
    class="bg-white h-100 d-flex flex-column overflow-hidden rounded-4 shadow-sm border-0"
  >
    <div
      class="p-4 border-bottom border-light d-flex justify-content-between align-items-center bg-light bg-opacity-50"
    >
      <h5 class="fw-semibold mb-0">
        Quản lý Nước Hoa
      </h5>

      <div class="d-flex gap-3">
        <div class="position-relative">
          <input
            v-model="searchQuery"
            type="text"
            class="form-control rounded-pill pe-5 bg-white border-light shadow-sm"
            placeholder="Tìm kiếm nước hoa..."
          />
        </div>

        <button
  @click="openAddModal"
  class="btn btn-dark rounded-pill px-4 fw-medium shadow-sm"
>
  + Thêm mới
</button>
      </div>
    </div>

    <ProductList
      :paginated-data="
        paginatedData
      "
      :brand-list="
        store.brandList
      "
      @edit="openEditModal"
      @stop-selling="
        stopSelling
      "
      @upload-image="
        openImageModal
      "
    />

    <div
      class="p-3 border-top border-light d-flex justify-content-between align-items-center bg-light bg-opacity-50"
    >
      <span
        class="small text-muted fw-medium"
      >
        Hiển thị
        {{ paginatedData.length }}
        /
        {{
          filteredData.length
        }}
        sản phẩm
      </span>

      <div
        class="d-flex gap-2 align-items-center"
      >
        <button
          class="btn btn-sm btn-white rounded-pill px-3 shadow-sm"
          :disabled="
            currentPage === 1
          "
          @click="currentPage--"
        >
          Trước
        </button>

        <span
          class="small mx-2 fw-medium"
        >
          Trang
          {{ currentPage }}
          /
          {{ totalPages }}
        </span>

        <button
          class="btn btn-sm btn-white rounded-pill px-3 shadow-sm"
          :disabled="
            currentPage ===
            totalPages
          "
          @click="currentPage++"
        >
          Sau
        </button>
      </div>
    </div>

    <Teleport to="body">
      <ProductModal
        v-if="showModal"
        :product-selected="
          selectedProduct
        "
        :brand-list="
          store.brandList
        "
        :category-list="
          store.categoryList
        "
        :concentration-list="
          store.concentrationList
        "
        :fragrance-family-list="
          store.fragranceFamilyList
        "
        :capacity-list="
          store.capacityList
        "
        :bottle-type-list="
          store.bottleTypeList
        "
        @close="
          closeAllModals
        "
        @refresh="
          store.fetchProducts
        "
      />

      <ProductImageModal
        v-if="showImageModal"
        :product-selected="
          selectedProduct
        "
        @close="
          closeAllModals
        "
        @refresh="
          store.fetchProducts
        "
      />
    </Teleport>
  </div>
</template>

<style scoped>
.btn-white {
  background-color: #fff;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.btn-white:hover {
  background-color: #f8fafc;
}
</style>