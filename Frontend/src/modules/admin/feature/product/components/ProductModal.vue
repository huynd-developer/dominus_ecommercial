<template>
  <div>

    <!-- Overlay -->

    <div
      class="offcanvas-backdrop fade show"
      @click="closeModal"
    ></div>

    <!-- Drawer -->

    <div
      class="offcanvas offcanvas-end show border-0 shadow-lg"
      tabindex="-1"
      style="visibility:visible;width:1100px;max-width:100vw"
    >

      <!-- HEADER -->

      <div
        class="offcanvas-header border-bottom bg-white px-4 py-3"
      >

        <div>

          <h4 class="fw-bold mb-1">

            {{
              isEdit
                ? "Cập nhật sản phẩm"
                : "Thêm sản phẩm"
            }}

          </h4>

          <div class="text-secondary small">

            {{
              isEdit
                ? "Chỉnh sửa thông tin sản phẩm"
                : "Nhập đầy đủ thông tin sản phẩm mới"
            }}

          </div>

        </div>

       <button
  class="btn-close"
  @click="closeModal"
></button>

      </div>

      <!-- BODY -->

      <div
        class="offcanvas-body p-0 custom-scrollbar d-flex flex-column"
      >

        <form
          class="d-flex flex-column h-100"
          @submit.prevent="saveData"
        >

          <div class="flex-grow-1 p-4">

            <!-- =============================== -->
            <!-- THÔNG TIN CƠ BẢN -->
            <!-- =============================== -->

            <div
              class="card border-0 shadow-sm rounded-4 mb-4"
            >

              <div class="card-body p-4">

                <h5 class="fw-bold mb-4">

                  Thông tin cơ bản

                </h5>

                <div class="row g-3">

                  <!-- NAME -->

                  <div class="col-12">

                    <label class="form-label fw-semibold">

                      Tên sản phẩm

                      <span class="text-danger">*</span>

                    </label>

                    <input
                      v-model.trim="formData.name"
                      class="form-control"
                      placeholder="Nhập tên sản phẩm..."
                    />

                  </div>

                  <!-- BRAND -->

                  <div class="col-md-4">

                    <label class="form-label">

                      Thương hiệu

                    </label>

                    <select
                      v-model.number="formData.brandId"
                      class="form-select"
                    >

                      <option :value="0">

                        Chọn thương hiệu

                      </option>

                      <option
                        v-for="brand in brandList"
                        :key="brand.id"
                        :value="brand.id"
                      >

                        {{ brand.brandName ?? brand.name }}

                      </option>

                    </select>

                  </div>

                  <!-- CATEGORY -->

                  <div class="col-md-4">

                    <label class="form-label">

                      Danh mục

                    </label>

                    <select
                      v-model.number="formData.categoryId"
                      class="form-select"
                    >

                      <option :value="0">

                        Chọn danh mục

                      </option>

                      <option
                        v-for="category in categoryList"
                        :key="category.id"
                        :value="category.id"
                      >

                        {{ category.categoryName ?? category.name }}

                      </option>

                    </select>

                  </div>

                  <!-- CONCENTRATION -->

                  <div class="col-md-4">

                    <label class="form-label">

                      Nồng độ

                    </label>

                    <select
                      v-model.number="formData.concentrationId"
                      class="form-select"
                    >

                      <option :value="0">

                        Chọn nồng độ

                      </option>

                      <option
                        v-for="item in concentrationList"
                        :key="item.id"
                        :value="item.id"
                      >

                        {{ item.concentrationName ?? item.name }}

                      </option>

                    </select>

                  </div>

                  <!-- GENDER -->

                  <div class="col-md-4">

                    <label class="form-label">

                      Giới tính

                    </label>

                    <select
                      v-model.number="formData.gender"
                      class="form-select"
                    >

                      <option :value="1">

                        Nam

                      </option>

                      <option :value="2">

                        Nữ

                      </option>

                      <option :value="3">

                        Unisex

                      </option>

                    </select>

                  </div>

                  <!-- STATUS -->

                  <div class="col-md-4">

                    <label class="form-label">

                      Trạng thái

                    </label>

                    <select
                      v-model.number="formData.status"
                      class="form-select"
                    >

                      <option :value="1">

                        Đang bán

                      </option>

                      <option :value="0">

                        Ngừng bán

                      </option>

                    </select>

                  </div>

                  <!-- NICHE -->

                  <div
                    class="col-md-4 d-flex align-items-end"
                  >

                    <div class="form-check form-switch">

                      <input
                        id="isNiche"
                        v-model="formData.isNiche"
                        class="form-check-input"
                        type="checkbox"
                      >

                      <label
                        for="isNiche"
                        class="form-check-label"
                      >

                        Nước hoa Niche

                      </label>

                    </div>

                  </div>

                  <!-- DESCRIPTION -->

                  <div class="col-12">

                    <label class="form-label">

                      Mô tả

                    </label>

                    <textarea
                      v-model="formData.description"
                      class="form-control"
                      rows="4"
                      placeholder="Nhập mô tả..."
                    ></textarea>

                  </div>

                </div>

              </div>

            </div>
            <!-- ================================= -->
<!-- NHÓM HƯƠNG -->
<!-- ================================= -->

<div
  class="card border-0 shadow-sm rounded-4 mb-4"
>

  <div class="card-body p-4">

    <div
      class="d-flex justify-content-between align-items-center mb-4"
    >

      <h5 class="fw-bold mb-0">

        Nhóm hương

      </h5>

      <span
        class="badge bg-light text-dark"
      >

        {{ formData.fragranceFamilyIds?.length ?? 0 }}
        đã chọn

      </span>

    </div>

    <div class="row">

      <div class="col-12">

        <select
          v-model="formData.fragranceFamilyIds"
          multiple
          class="form-select"
          style="height:220px"
        >

          <option
            v-for="item in fragranceFamilyList"
            :key="item.id"
            :value="item.id"
          >

            {{ item.familyName ?? item.name }}

          </option>

        </select>

      </div>

    </div>

    <small class="text-secondary">

      Giữ Ctrl để chọn nhiều nhóm hương.

    </small>

  </div>

</div>

<!-- ================================= -->
<!-- BIẾN THỂ -->
<!-- ================================= -->

<div
  class="card border-0 shadow-sm rounded-4"
>

  <div class="card-body p-4">

    <div
      class="d-flex justify-content-between align-items-center mb-4"
    >

      <h5 class="fw-bold mb-0">

        Danh sách biến thể

      </h5>

      <button
        type="button"
        class="btn btn-dark rounded-pill"
        @click="addVariant"
      >

        <i class="bi bi-plus-circle me-2"></i>

        Thêm biến thể

      </button>

    </div>

    <div class="table-responsive">

      <table
        class="table align-middle"
      >

        <thead>

        <tr>

          <th>Dung tích</th>

          <th>Loại chai</th>

          <th>SKU</th>

          <th width="150">

            Giá

          </th>

          <th width="140">

            Tồn

          </th>

          <th width="150">

            Trạng thái

          </th>

          <th width="70">

          </th>

        </tr>

        </thead>

        <tbody>

        <tr
          v-for="(variant,index) in formData.variants"
          :key="index"
        >

          <!-- Capacity -->

          <td>

            <select
              v-model.number="variant.capacityId"
              class="form-select form-select-sm"
            >

              <option :value="0">

                Chọn dung tích

              </option>

              <option
                v-for="item in capacityList"
                :key="item.id"
                :value="item.id"
              >

                {{ item.value }} ml

              </option>

            </select>

          </td>

          <!-- Bottle -->

          <td>

            <select
              v-model.number="variant.bottleTypeId"
              class="form-select form-select-sm"
            >

              <option :value="0">

                Chọn loại chai

              </option>

              <option
                v-for="item in bottleTypeList"
                :key="item.id"
                :value="item.id"
              >

                {{ item.bottleTypeName ?? item.name }}

              </option>

            </select>

          </td>

          <!-- SKU -->

          <td>

            <input
              v-model.trim="variant.sku"
              class="form-control form-control-sm"
              placeholder="SKU"
            >

          </td>

          <!-- PRICE -->

          <td>

            <input
              v-model.number="variant.price"
              type="number"
              min="0"
              class="form-control form-control-sm text-end"
            >

          </td>

          <!-- STOCK -->

          <td>

            <input
              v-model.number="variant.stockQuantity"
              type="number"
              min="0"
              class="form-control form-control-sm text-end"
            >

          </td>

          <!-- STATUS -->

          <td>

            <select
              v-model.number="variant.status"
              class="form-select form-select-sm"
            >

              <option :value="1">

                Đang bán

              </option>

              <option :value="0">

                Ngừng bán

              </option>

            </select>

          </td>

          <!-- DELETE -->

          <td class="text-end">

            <button
              v-if="formData.variants.length>1"
              type="button"
              class="btn btn-outline-danger btn-sm rounded-circle"
              @click="removeVariant(index)"
            >

              <i class="bi bi-trash"></i>

            </button>

          </td>

        </tr>

        </tbody>

      </table>

    </div>

  </div>

</div>
          </div>

          <!-- ========================= -->
          <!-- FOOTER -->
          <!-- ========================= -->

          <div
            class="sticky-bottom bg-white border-top px-4 py-3 d-flex justify-content-end gap-3"
          >
            <button
              type="button"
              class="btn btn-outline-secondary rounded-pill px-4"
              @click="emit('close')"
            >
              <i class="bi bi-x-circle me-2"></i>
              Hủy
            </button>

            <button
             
              type="submit"
              class="btn btn-dark rounded-pill px-5"
            >
              <i
                class="bi"
                :class="
                  isEdit
                    ? 'bi-pencil-square'
                    : 'bi-plus-circle'
                "
              ></i>

              {{
                isEdit
                  ? ' Cập nhật sản phẩm'
                  : ' Thêm sản phẩm'
              }}
            </button>
          </div>

        </form>

      </div>

    </div>

  </div>

</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import Swal from "sweetalert2";

import { productService } from "../services/productService";
import { useAppStore } from "@/common/store/app.store";

import type {
  Product,
  ProductRequestDTO,
  ProductVariant,
  Brand,
  Category,
  Concentration,
  Capacity,
  BottleType,
  FragranceFamily
} from "../types/product.type";

const props = defineProps<{
  productSelected: Product | null;

  brandList: Brand[];

  categoryList: Category[];

  concentrationList: Concentration[];

  fragranceFamilyList: FragranceFamily[];

  capacityList: Capacity[];

  bottleTypeList: BottleType[];
}>();

const emit = defineEmits<{
  (e: "close"): void;

  (e: "refresh"): void;
}>();

const appStore = useAppStore();

const isEdit = ref(false);

const formData = ref<ProductRequestDTO>({
  name: "",

  description: "",

  brandId: 0,

  categoryId: 0,

  concentrationId: 0,

  gender: 3,

  isNiche: false,

  status: 1,

  fragranceFamilyIds: [],

  variants: []
});

const resetForm = () => {

  formData.value = {

    name: "",

    description: "",

    brandId: 0,

    categoryId: 0,

    concentrationId: 0,

    gender: 3,

    isNiche: false,

    status: 1,

    fragranceFamilyIds: [],

    variants: []

  };

  addVariant();

};

const addVariant = () => {

  formData.value.variants.push({

    capacityId: 0,

    bottleTypeId: 0,

    sku: "",

    price: 0,

    stockQuantity: 0,

    status: 1

  });

};

const fillForm = (product: Product) => {

  formData.value = {

    name: product.name,

    description: product.description ?? "",

    brandId: product.brandId,

    categoryId: product.categoryId,

    concentrationId: product.concentrationId,

    gender: product.gender ?? 3,

    isNiche: product.isNiche ?? false,

status: product.status ?? 1,

    fragranceFamilyIds:
      product.fragranceFamilies?.map(
        item => item.id
      ) ?? [],

    variants:
      product.variants?.map(v => ({

        id: v.id,

        capacityId: v.capacityId,

        bottleTypeId: v.bottleTypeId,

        sku: v.sku,

        price: v.price,

        stockQuantity: v.stockQuantity,

        status: v.status ?? 1

      })) ?? []

  };

  if (formData.value.variants.length === 0) {

    addVariant();

  }

};

watch(
  () => props.productSelected,
  (product) => {
    console.log("product =", product)

    if (product) {
      isEdit.value = true
      fillForm(product)
    } else {
      isEdit.value = false
      resetForm()
    }

    console.log("isEdit =", isEdit.value)
  },
  {
    immediate: true
  }
)

const removeVariant = (
  index: number
) => {

  formData.value.variants.splice(
    index,
    1
  );

  if (
    formData.value.variants.length === 0
  ) {

    addVariant();

  }

};

const checkDuplicateSku = () => {

  const skuList = formData.value.variants.map(
    v => v.sku.trim().toLowerCase()
  );

  return new Set(skuList).size !== skuList.length;

};

const validateForm = () => {

  if (!formData.value.name.trim()) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Vui lòng nhập tên sản phẩm",
      "warning"
    );

    return false;

  }

  if (formData.value.brandId === 0) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Chưa chọn thương hiệu",
      "warning"
    );

    return false;

  }

  if (formData.value.categoryId === 0) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Chưa chọn danh mục",
      "warning"
    );

    return false;

  }

  if (formData.value.concentrationId === 0) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Chưa chọn nồng độ",
      "warning"
    );

    return false;

  }

  if (checkDuplicateSku()) {

    Swal.fire(
      "SKU bị trùng",
      "Các biến thể không được trùng SKU",
      "warning"
    );

    return false;

  }

  return true;

};

const saveData = async () => {

  if (!validateForm()) return;

  try {

    appStore.startLoading();

    // Chuẩn hóa dữ liệu trước khi gửi API
    const payload: ProductRequestDTO = {
      ...formData.value,

      variants: formData.value.variants.map(v => ({
        id: v.id,

        capacityId: Number(v.capacityId),

        bottleTypeId: Number(v.bottleTypeId),

        sku: v.sku.trim(),

        price: Number(v.price),

        stockQuantity: Number(v.stockQuantity),

        status: Number(v.status)
      })),

      fragranceFamilyIds:
        formData.value.fragranceFamilyIds.map(Number)
    };

    if (isEdit.value && props.productSelected) {

      await productService.updateProduct(
        props.productSelected.id,
        payload
      );

      appStore.notifySuccess(
        "Cập nhật sản phẩm thành công"
      );

    } else {

      await productService.createProduct(
        payload
      );

      appStore.notifySuccess(
        "Thêm sản phẩm thành công"
      );

    }

    emit("refresh");

    emit("close");

  } catch (e: any) {

    console.error(e);

    Swal.fire({

      icon: "error",

      title: "Lỗi",

      text:
        e?.response?.data?.message ??
        "Không thể lưu sản phẩm"

    });

  } finally {

    appStore.stopLoading();

  }

};

const closeModal = () => {

  resetForm();

  emit("close");

};

</script>

<style scoped>

/* ============================
   OFFCANVAS
============================ */

.offcanvas{
    width:1100px;
    max-width:100vw;
    transition:.3s;
    background:#f8fafc;
}

.offcanvas-body{
    display:flex;
    flex-direction:column;
    height:100%;
    overflow:hidden;
}

form{
    display:flex;
    flex-direction:column;
    height:100%;
}

.flex-grow-1{
    overflow-y:auto;
}

/* ============================
   SCROLLBAR
============================ */

.custom-scrollbar{
    overflow-y:auto;
    overflow-x:hidden;
}

.custom-scrollbar::-webkit-scrollbar{
    width:7px;
}

.custom-scrollbar::-webkit-scrollbar-thumb{
    background:#cbd5e1;
    border-radius:30px;
}

.custom-scrollbar::-webkit-scrollbar-track{
    background:transparent;
}

/* ============================
   CARD
============================ */

.card{
    border:none;
    border-radius:18px;
    box-shadow:
        0 4px 18px rgba(15,23,42,.05);
}

.card-body{
    padding:28px;
}

/* ============================
   INPUT
============================ */

.form-control,
.form-select{

    min-height:46px;

    border-radius:12px;

    border:1px solid #e2e8f0;

    transition:.2s;

    background:#fff;

}

textarea.form-control{

    min-height:120px;

    resize:none;

}

.form-control:focus,
.form-select:focus{

    border-color:#111827;

    box-shadow:0 0 0 .15rem rgba(17,24,39,.08);

}

/* ============================
   LABEL
============================ */

.form-label{

    font-size:14px;

    font-weight:600;

    color:#475569;

}

/* ============================
   TABLE
============================ */

.table{

    margin-bottom:0;

}

.table thead{

    background:#f8fafc;

}

.table thead th{

    color:#64748b;

    font-size:13px;

    font-weight:700;

    border-bottom:1px solid #e5e7eb;

}

.table td{

    vertical-align:middle;

    padding-top:14px;

    padding-bottom:14px;

}

/* ============================
   BUTTON
============================ */

.btn{

    border-radius:12px;

}

.btn-dark{

    background:#111827;

    border:none;

}

.btn-dark:hover{

    background:#000;

}

.btn-outline-secondary{

    border:1px solid #d1d5db;

}

.btn-outline-danger{

    width:36px;

    height:36px;

    padding:0;

    display:flex;

    justify-content:center;

    align-items:center;

}

/* ============================
   SWITCH
============================ */

.form-check-input{

    width:48px;

    height:24px;

}

.form-check-input:checked{

    background:#111827;

    border-color:#111827;

}

/* ============================
   BADGE
============================ */

.badge{

    font-size:12px;

    padding:8px 12px;

    border-radius:999px;

}

/* ============================
   MULTIPLE SELECT
============================ */

select[multiple]{

    min-height:220px;

}

select[multiple] option{

    padding:8px;

}

/* ============================
   FOOTER
============================ */

.sticky-bottom{

    position:relative;

    bottom:0;

    z-index:100;

    background:#fff;

    border-top:1px solid #e5e7eb;

}

/* ============================
   HEADER
============================ */

.offcanvas-header{

    background:#fff;

    border-bottom:1px solid #e5e7eb;

}

/* ============================
   TITLE
============================ */

h4,
h5,
h6{

    color:#0f172a;

}

/* ============================
   ANIMATION
============================ */

.card{

    animation:fadeIn .25s ease;

}

@keyframes fadeIn{

    from{

        opacity:0;

        transform:translateY(10px);

    }

    to{

        opacity:1;

        transform:translateY(0);

    }

}

</style>