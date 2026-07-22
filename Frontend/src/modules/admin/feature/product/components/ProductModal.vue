<template>
  <div class="product-modal">

    <!-- Overlay -->
    <div
      class="modal-overlay"
      @click="closeModal"
    ></div>

    <!-- Drawer -->
    <div
      class="product-drawer"
    >

      <!-- ========================= -->
      <!-- HEADER -->
      <!-- ========================= -->

      <div class="drawer-header">

        <div class="drawer-title">

          <div class="drawer-icon">

            <i
              class="bi"
              :class="
                isEdit
                  ? 'bi-pencil-square'
                  : 'bi-plus-circle'
              "
            ></i>

          </div>

          <div>

            <h3>

              {{
                isEdit
                  ? 'Cập nhật sản phẩm'
                  : 'Thêm sản phẩm'
              }}

            </h3>

            <p>

              {{
                isEdit
                  ? 'Chỉnh sửa thông tin sản phẩm'
                  : 'Tạo sản phẩm mới trong hệ thống'
              }}

            </p>

          </div>

        </div>

        <button
          class="btn-close-modal"
          @click="closeModal"
        >

          <i class="bi bi-x-lg"></i>

        </button>

      </div>

      <!-- ========================= -->
      <!-- BODY -->
      <!-- ========================= -->

      <div
        class="drawer-body custom-scrollbar"
      >

        <form
          class="product-form"
          @submit.prevent="saveData"
        >

          <div class="form-content">

            <!-- ======================================= -->
            <!-- THÔNG TIN CƠ BẢN -->
            <!-- ======================================= -->

            <section class="content-card">

              <div class="section-header">

                <div>

                  <h4>

                    Thông tin cơ bản

                  </h4>

                  <span>

                    Các thông tin chính của sản phẩm

                  </span>

                </div>

              </div>

              <div class="section-body">

                <div class="row g-4">

                  <!-- NAME -->

                  <div class="col-12">

                    <label class="form-label">

                      Tên sản phẩm

                      <span class="text-danger">*</span>

                    </label>

                    <input
                      v-model.trim="formData.name"
                      class="form-control"
                      placeholder="Ví dụ: Dior Sauvage Eau De Parfum"
                    >

                  </div>

                  <!-- BRAND -->

                  <div class="col-lg-4">

                    <label class="form-label">

                      Thương hiệu

                      <span class="text-danger">*</span>

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

                  <div class="col-lg-4">

                    <label class="form-label">

                      Danh mục

                      <span class="text-danger">*</span>

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

                  <div class="col-lg-4">

                    <label class="form-label">

                      Nồng độ

                      <span class="text-danger">*</span>

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

                  <div class="col-lg-4">

                    <label class="form-label">

                      Giới tính

                      <span class="text-danger">*</span>

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

                  <div class="col-lg-4">

                    <label class="form-label">

                      Trạng thái

                      <span class="text-danger">*</span>

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
                    class="col-lg-4 d-flex align-items-end"
                  >

                    <div
                      class="form-check form-switch fs-6"
                    >

                      <input
                        id="isNiche"
                        v-model="formData.isNiche"
                        class="form-check-input"
                        type="checkbox"
                      >

                      <label
                        for="isNiche"
                        class="form-check-label ms-2"
                      >

                        Nước hoa Niche

                      </label>

                    </div>

                  </div>

                  <!-- DESCRIPTION -->

                  <div class="col-12">

                    <label class="form-label">

                      Mô tả sản phẩm

                    </label>

                    <textarea
                      v-model="formData.description"
                      class="form-control"
                      rows="5"
                      placeholder="Nhập mô tả chi tiết về sản phẩm..."
                    ></textarea>

                  </div>

                </div>

              </div>

            </section>

            <!-- ======================================= -->
            <!-- NHÓM HƯƠNG -->
            <!-- PHẦN 1.2 sẽ bắt đầu từ đây -->
                        <!-- ======================================= -->
            <!-- NHÓM HƯƠNG -->
            <!-- ======================================= -->

            <section class="content-card">

              <div class="section-header">

                <div>

                  <h4>

                    Nhóm hương

                    <span class="text-danger">*</span>

                  </h4>

                  <span>

                    Chọn các nhóm hương đặc trưng của sản phẩm

                  </span>

                </div>

                <div class="section-badge">

                  {{ formData.fragranceFamilyIds.length }}

                  nhóm hương

                </div>

              </div>

              <div class="section-body">

                <div class="row g-3">

                  <div
                    class="col-xl-3 col-lg-4 col-md-6"
                    v-for="item in fragranceFamilyList"
                    :key="item.id"
                  >

                    <label
                      class="fragrance-card"
                      :class="{
                        active:
                          formData.fragranceFamilyIds.includes(
                            item.id
                          )
                      }"
                    >

                      <input
                        type="checkbox"
                        :value="item.id"
                        v-model="formData.fragranceFamilyIds"
                        class="d-none"
                      >

                      <div
                        class="d-flex justify-content-between align-items-center"
                      >

                        <div>

                          <div class="fw-semibold">

                            {{ item.name }}

                          </div>

                        </div>

                        <div
                          class="check-icon"
                        >

                          <i
                            class="bi"
                            :class="
                              formData.fragranceFamilyIds.includes(item.id)
                                ? 'bi-check-circle-fill'
                                : 'bi-circle'
                            "
                          ></i>

                        </div>

                      </div>

                    </label>

                  </div>

                </div>

                <div
                  v-if="
                    formData.fragranceFamilyIds.length
                  "
                  class="selected-list mt-4"
                >

                  <div class="small text-muted mb-2">

                    Đã chọn

                  </div>

                  <div class="d-flex flex-wrap gap-2">

                    <span
                      v-for="item in fragranceFamilyList.filter(
                        x =>
                          formData.fragranceFamilyIds.includes(
                            x.id
                          )
                      )"
                      :key="item.id"
                      class="badge rounded-pill bg-primary"
                    >

                      {{ item.name }}

                    </span>

                  </div>

                </div>

              </div>

            </section>

            <!-- ======================================= -->
            <!-- BIẾN THỂ -->
            <!-- PHẦN 1.3 -->
            <!-- ======================================= -->

<!-- ======================================= -->
<!-- BIẾN THỂ -->
<!-- ======================================= -->

<section class="content-card">

  <div class="section-header">

    <div>

      <h4>

        Danh sách biến thể

      </h4>

      <span>

        Quản lý dung tích, loại chai, giá bán và tồn kho

      </span>

    </div>

    <button
      type="button"
      class="btn btn-primary rounded-pill px-4"
      @click="addVariant"
    >

      <i class="bi bi-plus-lg me-2"></i>

      Thêm biến thể

    </button>

  </div>

  <div class="section-body">

    <div class="variant-table">

      <table class="table align-middle mb-0">

        <thead>

        <tr>

          <th width="170">

            Dung tích

            <span class="text-danger">*</span>

          </th>

          <th width="180">

            Loại chai

            <span class="text-danger">*</span>

          </th>

          <th width="170">

            SKU

          </th>

          <th width="150">

            Giá bán

            <span class="text-danger">*</span>

          </th>

          <th width="130">

            Tồn kho

            <span class="text-danger">*</span>

          </th>

          <th width="170">

            NSX

            <span class="text-danger">*</span>

          </th>

          <th width="170">

            HSD

            <span class="text-danger">*</span>

          </th>

          <th width="150">

            Trạng thái

          </th>

          <th width="70"></th>

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
              class="form-select"
            >

              <option :value="0">

                Chọn

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
              class="form-select"
            >

              <option :value="0">

                Chọn

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

          <!-- <td>

            <input
              :value="
                isEdit
                  ? (variant.sku || '')
                  : 'Tự động sinh'
              "
              class="form-control bg-light text-center"
              readonly
            >

          </td> -->

          <!-- Price -->

          <td>

            <div class="input-group">

              <span class="input-group-text">

                ₫

              </span>

              <input
                v-model.number="variant.price"
                type="number"
                min="0"
                class="form-control text-end"
              >

            </div>

          </td>

          <!-- Stock -->

          <td>

            <input
              v-model.number="variant.stockQuantity"
              type="number"
              min="0"
              class="form-control text-center"
            >

          </td>

          <!-- NSX -->

          <td>

            <input
              v-model="variant.manufacturingDate"
              type="date"
              class="form-control"
            >

          </td>

          <!-- HSD -->

          <td>

            <input
              v-model="variant.expirationDate"
              type="date"
              class="form-control"
            >

          </td>

          <!-- Status -->

          <td>

            <select
              v-model.number="variant.status"
              class="form-select"
            >

              <option :value="1">

                Đang bán

              </option>

              <option :value="0">

                Ngừng bán

              </option>

            </select>

          </td>

          <!-- Delete -->

          <td class="text-center">

            <button
              v-if="formData.variants.length>1"
              type="button"
              class="btn btn-outline-danger rounded-circle btn-icon"
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

</section>

<!-- ======================================= -->
<!-- HÌNH ẢNH -->
<!-- PHẦN 1.4 -->
<!-- ======================================= -->

<!-- ======================================= -->
<!-- HÌNH ẢNH -->
<!-- ======================================= -->

<section class="content-card">

  <div class="section-header">

    <div>

      <h4>

        Hình ảnh sản phẩm

      </h4>

      <span>

        Chọn ảnh hiển thị của sản phẩm

      </span>

    </div>

    <div class="section-badge">

      {{ imageList.length }}

      ảnh

    </div>

  </div>

  <div class="section-body">

    <!-- Upload -->

    <label
      class="upload-area"
    >

      <input
        type="file"
        multiple
        accept="image/*"
        hidden
        @change="handleImages"
      >

      <i
        class="bi bi-cloud-arrow-up"
      ></i>

      <h6>

        Kéo ảnh vào đây

      </h6>

      <p>

        hoặc nhấn để chọn ảnh

      </p>

    </label>

    <!-- Preview -->

    <div
      class="row g-4 mt-1"
    >

      <div
        class="col-xl-3 col-lg-4 col-md-6"
        v-for="(img,index) in imageList"
        :key="index"
      >

        <div class="image-card">

          <div class="image-wrapper">

            <img
              :src="img.preview"
            >

            <div
              class="image-overlay"
            >

              <button
                type="button"
                class="btn btn-light btn-sm"
                @click="
                  setPrimaryImage(index)
                "
              >

                <i
                  class="bi bi-star-fill"
                ></i>

              </button>

              <button
                type="button"
                class="btn btn-danger btn-sm"
                @click="
                  removeImage(index)
                "
              >

                <i
                  class="bi bi-trash"
                ></i>

              </button>

            </div>

          </div>

          <div
            class="image-footer"
          >

            <span
              v-if="img.isPrimary"
              class="badge bg-success"
            >

              Ảnh chính

            </span>

            <button
              v-else
              type="button"
              class="btn btn-sm btn-outline-success"
              @click="
                setPrimaryImage(index)
              "
            >

              Đặt làm ảnh chính

            </button>

          </div>

        </div>

      </div>

    </div>

  </div>

</section>

</div>

<!-- ================================ -->
<!-- FOOTER -->
<!-- ================================ -->

<div
  class="drawer-footer"
>

  <button
    type="button"
    class="btn btn-light px-4"
    @click="closeModal"
  >

    <i
      class="bi bi-x-circle me-2"
    ></i>

    Hủy

  </button>

  <button
    type="submit"
    class="btn btn-primary px-5"
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

        ? ' Cập nhật'

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
import { ref, watch } from "vue";
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

interface ProductImageItem {
  id?: number
  file?: File
  preview: string
  isPrimary: boolean
}

const imageList = ref<ProductImageItem[]>([])

const deletedImageIds = ref<number[]>([])

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

  imageList.value = [];

  deletedImageIds.value = [];

  addVariant();

};

const addVariant = () => {

  formData.value.variants.push({
  capacityId: 0,
  bottleTypeId: 0,
  price: 0,
  stockQuantity: 0,
  manufacturingDate: "",
  expirationDate: "",
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
    manufacturingDate: v.manufacturingDate ?? "",
    expirationDate: v.expirationDate ?? "",
    status: v.status ?? 1
  })) ?? []

  };

  if (formData.value.variants.length === 0) {

    addVariant();

  }

  imageList.value =
  product.images?.map(img => ({

    id: img.id,

   preview:
  img.imageUrl
    ? (
        img.imageUrl.startsWith("http")
          ? img.imageUrl
          : `http://localhost:8080${img.imageUrl}`
      )
    : "",

    isPrimary:
      img.isPrimary

  })) ?? []

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

const handleImages = (
  event: Event
) => {

  const files =
    (event.target as HTMLInputElement)
      .files

  if (!files) return

  Array.from(files).forEach(file => {

    imageList.value.push({

      file,

      preview:
        URL.createObjectURL(file),

      isPrimary:
        imageList.value.length === 0

    })

  })

}

const setPrimaryImage = (
  index: number
) => {

  imageList.value.forEach(img => {
    img.isPrimary = false
  })

  const image = imageList.value[index]

  if (image) {
    image.isPrimary = true
  }
}

const removeImage = (
  index: number
) => {

  const image =
    imageList.value[index]

  if (image?.id) {

    deletedImageIds.value.push(
      image.id
    )

  }

  imageList.value.splice(
    index,
    1
  )

  if (
    imageList.value.length > 0 &&
    !imageList.value.some(
      x => x.isPrimary
    )
  ) {

    imageList.value[0]!.isPrimary = true

  }

}


const validateForm = () => {

  if(imageList.value.length === 0){

  Swal.fire(
    "Thiếu ảnh",
    "Vui lòng chọn ít nhất 1 ảnh",
    "warning"
  )

  return false
}

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

    for (const variant of formData.value.variants) {

  if (variant.capacityId === 0) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Chưa chọn dung tích",
      "warning"
    )

    return false
  }

  if (variant.bottleTypeId === 0) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Chưa chọn loại chai",
      "warning"
    )

    return false
  }

  if (variant.price <= 0) {

    Swal.fire(
      "Thiếu dữ liệu",
      "Giá bán phải lớn hơn 0",
      "warning"
    )

    return false
  }
}

    Swal.fire(
      "Thiếu dữ liệu",
      "Chưa chọn nồng độ",
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
  price: Number(v.price),
  stockQuantity: Number(v.stockQuantity),
  manufacturingDate: v.manufacturingDate,
  expirationDate: v.expirationDate,
  status: Number(v.status)
})),

      fragranceFamilyIds:
        formData.value.fragranceFamilyIds.map(Number)
    };

    if (
  isEdit.value &&
  props.productSelected
) {

  await productService.updateProduct(
    props.productSelected.id,
    payload
  )

  // XÓA ẢNH

  for (
    const imageId of
    deletedImageIds.value
  ) {

    await productService.deleteImage(
      imageId
    )

  }

  // UPLOAD ẢNH MỚI

  for (
    const image of imageList.value
  ) {

    if (!image.file) continue

    await productService.uploadImage(
  props.productSelected.id,
  image.file
)

  }

  // CẬP NHẬT ẢNH CHÍNH

  const primaryImage =
    imageList.value.find(
      img => img.isPrimary
    )

  if (
    primaryImage?.id
  ) {

    await productService.setPrimaryImage(
      props.productSelected.id,
      primaryImage.id
    )

  }

  appStore.notifySuccess(
    "Cập nhật sản phẩm thành công"
  )

} else {

      const created =
  await productService.createProduct(
    payload
  )

// upload ảnh

for (const image of imageList.value) {

  if (!image.file) continue

  await productService.uploadImage(
    created.id,
    image.file
  )

}

// lấy lại danh sách ảnh sau upload

const uploadedImages =
  await productService.getImagesByProduct(
    created.id
  )

// tìm vị trí ảnh được chọn là ảnh chính

const primaryIndex =
  imageList.value.findIndex(
    img => img.isPrimary
  )

// set ảnh chính

if (
  primaryIndex >= 0 &&
  uploadedImages?.[primaryIndex]?.id
) {

  await productService.setPrimaryImage(
    created.id,
    uploadedImages[primaryIndex].id
  )

}

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
/* =========================================================
   PRODUCT MODAL
========================================================= */

.product-modal{
    position:fixed;
    inset:0;
    z-index:1055;
}

/* =========================================================
   OVERLAY
========================================================= */

.modal-overlay{

    position:absolute;

    inset:0;

    background:rgba(15,23,42,.45);

    backdrop-filter:blur(3px);

    animation:fadeOverlay .25s;

}

/* =========================================================
   DRAWER
========================================================= */

.product-drawer{

    position:absolute;

    top:0;

    right:0;

    width:min(1550px,96vw);

    height:100%;

    background:#f8fafc;

    display:flex;

    flex-direction:column;

    border-radius:24px 0 0 24px;

    overflow:hidden;

    box-shadow:
        -12px 0 50px rgba(15,23,42,.18);

    animation:drawerIn .25s ease;

}

/* =========================================================
   HEADER
========================================================= */

.drawer-header{

    height:88px;

    flex-shrink:0;

    background:#fff;

    display:flex;

    justify-content:space-between;

    align-items:center;

    padding:0 32px;

    border-bottom:1px solid #e2e8f0;

}

.drawer-title{

    display:flex;

    align-items:center;

    gap:18px;

}

.drawer-icon{

    width:58px;

    height:58px;

    border-radius:18px;

    background:linear-gradient(
        135deg,
        #2563eb,
        #3b82f6
    );

    color:#fff;

    display:flex;

    justify-content:center;

    align-items:center;

    font-size:24px;

    box-shadow:
        0 10px 24px rgba(37,99,235,.25);

}

.drawer-title h3{

    margin:0;

    font-size:24px;

    font-weight:700;

    color:#0f172a;

}

.drawer-title p{

    margin:4px 0 0;

    color:#94a3b8;

    font-size:14px;

}

.btn-close-modal{

    width:44px;

    height:44px;

    border:none;

    border-radius:12px;

    background:#f1f5f9;

    transition:.25s;

    display:flex;

    align-items:center;

    justify-content:center;

}

.btn-close-modal:hover{

    background:#ef4444;

    color:#fff;

}

/* =========================================================
   BODY
========================================================= */

.drawer-body{

    flex:1;

    overflow:hidden;

    display:flex;

    flex-direction:column;

}

.product-form{

    height:100%;

    display:flex;

    flex-direction:column;

}

.form-content{

    flex:1;

    overflow-y:auto;

    padding:28px;

}

/* =========================================================
   CARD
========================================================= */

.content-card{

    background:#fff;

    border-radius:22px;

    margin-bottom:24px;

    overflow:hidden;

    box-shadow:
        0 10px 30px rgba(15,23,42,.05);

    border:1px solid #eef2f7;

}

.section-header{

    padding:24px 28px;

    display:flex;

    justify-content:space-between;

    align-items:center;

    border-bottom:1px solid #eef2f7;

}

.section-header h4{

    margin:0;

    font-size:20px;

    font-weight:700;

    color:#0f172a;

}

.section-header span{

    color:#94a3b8;

    font-size:14px;

}

.section-body{

    padding:28px;

}

.section-badge{

    background:#eff6ff;

    color:#2563eb;

    padding:8px 16px;

    border-radius:999px;

    font-weight:600;

    font-size:13px;

}

/* =========================================================
   SCROLLBAR
========================================================= */

.custom-scrollbar{

    overflow-y:auto;

}

.custom-scrollbar::-webkit-scrollbar{

    width:8px;

}

.custom-scrollbar::-webkit-scrollbar-track{

    background:transparent;

}

.custom-scrollbar::-webkit-scrollbar-thumb{

    background:#cbd5e1;

    border-radius:999px;

}

.custom-scrollbar::-webkit-scrollbar-thumb:hover{

    background:#94a3b8;

}

/* =========================================================
   ANIMATION
========================================================= */

@keyframes drawerIn{

    from{

        transform:translateX(80px);

        opacity:0;

    }

    to{

        transform:translateX(0);

        opacity:1;

    }

}

@keyframes fadeOverlay{

    from{

        opacity:0;

    }

    to{

        opacity:1;

    }

}
/* =========================================================
   FORM
========================================================= */

.form-label{

    display:flex;

    align-items:center;

    gap:6px;

    margin-bottom:10px;

    font-size:14px;

    font-weight:600;

    color:#334155;

}

.form-control,
.form-select{

    min-height:50px;

    border:1px solid #dbe4ee;

    border-radius:14px;

    background:#fff;

    transition:all .25s ease;

    font-size:14px;

    color:#0f172a;

    padding-left:16px;

}

.form-control:hover,
.form-select:hover{

    border-color:#94a3b8;

}

.form-control:focus,
.form-select:focus{

    border-color:#2563eb;

    box-shadow:
        0 0 0 4px rgba(37,99,235,.12);

}

textarea.form-control{

    resize:none;

    min-height:130px;

    padding-top:14px;

}

.form-control::placeholder{

    color:#94a3b8;

}

/* =========================================================
   INPUT GROUP
========================================================= */

.input-group{

    border-radius:14px;

    overflow:hidden;

}

.input-group-text{

    background:#f8fafc;

    border:1px solid #dbe4ee;

    border-right:none;

    color:#64748b;

    font-weight:600;

    min-width:46px;

    justify-content:center;

}

.input-group .form-control{

    border-left:none;

}

/* =========================================================
   SWITCH
========================================================= */

.form-check{

    margin-bottom:0;

}

.form-switch{

    display:flex;

    align-items:center;

    gap:12px;

}

.form-check-input{

    width:54px;

    height:28px;

    cursor:pointer;

    border-radius:999px;

    border:1px solid #cbd5e1;

    background-color:#e2e8f0;

    transition:.25s;

}

.form-check-input:checked{

    background-color:#2563eb;

    border-color:#2563eb;

}

.form-check-label{

    font-weight:600;

    color:#334155;

    cursor:pointer;

}

/* =========================================================
   SELECT
========================================================= */

.form-select{

    cursor:pointer;

    background-position:
        right .8rem center;

}

/* =========================================================
   NUMBER INPUT
========================================================= */

input[type="number"]{

    text-align:right;

}

input[type="date"]{

    cursor:pointer;

}

/* =========================================================
   SKU
========================================================= */

.form-control.bg-light{

    background:#f8fafc !important;

    color:#64748b;

    font-weight:600;

    text-align:center;

}

/* =========================================================
   BUTTON
========================================================= */

.btn{

    border-radius:14px;

    transition:.25s;

    font-weight:600;

}

.btn-primary{

    background:#2563eb;

    border:none;

}

.btn-primary:hover{

    background:#1d4ed8;

    transform:translateY(-1px);

}

.btn-light{

    background:#fff;

    border:1px solid #dbe4ee;

}

.btn-light:hover{

    background:#f8fafc;

}

.btn-outline-success{

    border:1px solid #22c55e;

    color:#16a34a;

}

.btn-outline-success:hover{

    background:#22c55e;

    color:white;

}

.btn-outline-danger{

    border:1px solid #ef4444;

    color:#ef4444;

}

.btn-outline-danger:hover{

    background:#ef4444;

    color:white;

}

/* =========================================================
   BADGE
========================================================= */

.badge{

    border-radius:999px;

    padding:7px 14px;

    font-weight:600;

    font-size:12px;

}

/* =========================================================
   CHECKBOX CARD
========================================================= */

.fragrance-card{

    display:block;

    padding:18px;

    border:1px solid #e2e8f0;

    border-radius:16px;

    cursor:pointer;

    transition:.25s;

    background:white;

    height:100%;

}

.fragrance-card:hover{

    border-color:#2563eb;

    transform:translateY(-2px);

    box-shadow:
        0 8px 24px rgba(37,99,235,.08);

}

.fragrance-card.active{

    border-color:#2563eb;

    background:#eff6ff;

}

.check-icon{

    font-size:22px;

    color:#2563eb;

}

.selected-list{

    border-top:1px dashed #dbe4ee;

    padding-top:20px;

}
/* =========================================================
   VARIANT TABLE
========================================================= */

.variant-table{

    border:1px solid #e2e8f0;

    border-radius:18px;

    overflow:auto;

    background:#fff;

}

.variant-table table{

    margin:0;

    min-width:1450px;

}

.variant-table thead{

    position:sticky;

    top:0;

    z-index:10;

    background:#f8fafc;

}

.variant-table thead th{

    padding:16px 18px;

    font-size:13px;

    font-weight:700;

    color:#64748b;

    border-bottom:1px solid #e2e8f0;

    white-space:nowrap;

    vertical-align:middle;

    background:#f8fafc;

}

.variant-table tbody td{

    padding:16px;

    vertical-align:middle;

    border-bottom:1px solid #f1f5f9;

    background:white;

}

.variant-table tbody tr{

    transition:.2s;

}

.variant-table tbody tr:hover td{

    background:#f8fbff;

}

/* =========================================================
   VARIANT INPUT
========================================================= */

.variant-table .form-control,
.variant-table .form-select{

    min-height:42px;

    font-size:13px;

    border-radius:12px;

}

.variant-table .form-control{

    padding-left:12px;

}

.variant-table input[type="number"]{

    text-align:right;

    font-weight:600;

}

.variant-table input[type="date"]{

    text-align:center;

}

.variant-table .form-select{

    padding-left:12px;

}

/* =========================================================
   SKU
========================================================= */

.variant-table input[readonly]{

    background:#f8fafc !important;

    color:#64748b;

    font-weight:600;

    text-align:center;

    cursor:not-allowed;

}

/* =========================================================
   PRICE
========================================================= */

.variant-table .input-group{

    border-radius:12px;

    overflow:hidden;

}

.variant-table .input-group-text{

    min-width:42px;

    background:#f8fafc;

    color:#64748b;

    border-color:#dbe4ee;

}

/* =========================================================
   DELETE BUTTON
========================================================= */

.btn-icon{

    width:40px;

    height:40px;

    padding:0;

    display:flex;

    justify-content:center;

    align-items:center;

    border-radius:12px;

    transition:.25s;

}

.btn-icon:hover{

    transform:scale(1.08);

}

/* =========================================================
   EMPTY
========================================================= */

.variant-empty{

    padding:50px;

    text-align:center;

    color:#94a3b8;

}

.variant-empty i{

    font-size:40px;

    margin-bottom:10px;

    display:block;

}

/* =========================================================
   TABLE SCROLLBAR
========================================================= */

.variant-table::-webkit-scrollbar{

    height:9px;

}

.variant-table::-webkit-scrollbar-track{

    background:#f8fafc;

}

.variant-table::-webkit-scrollbar-thumb{

    background:#cbd5e1;

    border-radius:999px;

}

.variant-table::-webkit-scrollbar-thumb:hover{

    background:#94a3b8;

}

/* =========================================================
   HEADER ACTION
========================================================= */

.section-header .btn-primary{

    min-width:170px;

    height:46px;

    font-size:14px;

}

/* =========================================================
   TABLE ANIMATION
========================================================= */

.variant-table tbody tr{

    animation:variantFade .25s ease;

}

@keyframes variantFade{

    from{

        opacity:0;

        transform:translateY(10px);

    }

    to{

        opacity:1;

        transform:translateY(0);

    }

}

/* =========================================================
   IMAGE UPLOAD
========================================================= */

.upload-area{

    width:100%;

    min-height:220px;

    border:2px dashed #cbd5e1;

    border-radius:20px;

    background:#fbfdff;

    display:flex;

    flex-direction:column;

    justify-content:center;

    align-items:center;

    cursor:pointer;

    transition:.25s;

    text-align:center;

    margin-bottom:28px;

}

.upload-area:hover{

    border-color:#2563eb;

    background:#eff6ff;

}

.upload-area i{

    font-size:56px;

    color:#2563eb;

    margin-bottom:16px;

}

.upload-area h6{

    margin:0;

    font-size:18px;

    font-weight:700;

    color:#0f172a;

}

.upload-area p{

    margin-top:8px;

    color:#94a3b8;

    font-size:14px;

}

/* =========================================================
   IMAGE CARD
========================================================= */

.image-card{

    background:#fff;

    border:1px solid #e2e8f0;

    border-radius:18px;

    overflow:hidden;

    transition:.25s;

    height:100%;

}

.image-card:hover{

    transform:translateY(-4px);

    box-shadow:
        0 14px 35px rgba(15,23,42,.08);

}

/* =========================================================
   IMAGE
========================================================= */

.image-wrapper{

    position:relative;

    height:240px;

    overflow:hidden;

    background:#f8fafc;

}

.image-wrapper img{

    width:100%;

    height:100%;

    object-fit:cover;

    transition:.35s;

}

.image-card:hover img{

    transform:scale(1.08);

}

/* =========================================================
   OVERLAY
========================================================= */

.image-overlay{

    position:absolute;

    inset:0;

    background:rgba(15,23,42,.45);

    display:flex;

    justify-content:center;

    align-items:center;

    gap:12px;

    opacity:0;

    transition:.25s;

}

.image-card:hover .image-overlay{

    opacity:1;

}

.image-overlay .btn{

    width:42px;

    height:42px;

    border-radius:12px;

    padding:0;

    display:flex;

    justify-content:center;

    align-items:center;

    backdrop-filter:blur(6px);

}

.image-overlay .btn-light{

    background:white;

    border:none;

}

.image-overlay .btn-danger{

    border:none;

}

/* =========================================================
   IMAGE FOOTER
========================================================= */

.image-footer{

    padding:16px;

    display:flex;

    justify-content:center;

    align-items:center;

    min-height:68px;

    border-top:1px solid #f1f5f9;

}

.image-footer .btn{

    width:100%;

}

/* =========================================================
   PRIMARY BADGE
========================================================= */

.image-footer .badge{

    width:100%;

    padding:10px;

    font-size:13px;

    background:#16a34a !important;

}

/* =========================================================
   IMAGE COUNT
========================================================= */

.section-badge{

    display:flex;

    align-items:center;

    gap:8px;

}

/* =========================================================
   EMPTY IMAGE
========================================================= */

.no-image{

    height:220px;

    display:flex;

    justify-content:center;

    align-items:center;

    color:#94a3b8;

    font-size:42px;

    background:#f8fafc;

}

/* =========================================================
   IMAGE GRID
========================================================= */

.image-card{

    animation:imageFade .3s ease;

}

@keyframes imageFade{

    from{

        opacity:0;

        transform:translateY(12px);

    }

    to{

        opacity:1;

        transform:translateY(0);

    }

}

/* =========================================================
   FOOTER
========================================================= */

.drawer-footer{

    background:#fff;

    border-top:1px solid #e2e8f0;

    padding:18px 32px;

    display:flex;

    justify-content:flex-end;

    align-items:center;

    gap:14px;

    position:sticky;

    bottom:0;

    z-index:20;

    box-shadow:0 -8px 20px rgba(15,23,42,.04);

}

.drawer-footer .btn{

    min-width:150px;

    height:48px;

    font-size:15px;

    font-weight:600;

}

.drawer-footer .btn-primary{

    background:linear-gradient(
        135deg,
        #2563eb,
        #3b82f6
    );

}

.drawer-footer .btn-primary:hover{

    background:linear-gradient(
        135deg,
        #1d4ed8,
        #2563eb
    );

}

/* =========================================================
   CARD HOVER
========================================================= */

.content-card{

    transition:.25s;

}

.content-card:hover{

    box-shadow:

        0 18px 40px rgba(15,23,42,.07);

}

/* =========================================================
   TABLE
========================================================= */

.table{

    margin-bottom:0;

}

.table>:not(caption)>*>*{

    border-bottom-width:1px;

}

/* =========================================================
   BUTTON ICON
========================================================= */

.btn i{

    margin-right:6px;

}

.btn-icon i{

    margin:0;

}

/* =========================================================
   BADGE
========================================================= */

.badge{

    letter-spacing:.2px;

}

/* =========================================================
   FOCUS
========================================================= */

button:focus,

input:focus,

select:focus,

textarea:focus{

    outline:none;

}

/* =========================================================
   DISABLED
========================================================= */

button:disabled{

    opacity:.55;

    cursor:not-allowed;

}

.form-control:disabled,

.form-select:disabled{

    background:#f8fafc;

}

/* =========================================================
   RESPONSIVE
========================================================= */

@media (max-width:1400px){

.product-drawer{

    width:100vw;

    border-radius:0;

}

}

@media (max-width:992px){

.drawer-header{

    padding:20px;

    height:auto;

}

.drawer-title h3{

    font-size:20px;

}

.form-content{

    padding:20px;

}

.section-header{

    flex-direction:column;

    align-items:flex-start;

    gap:16px;

}

.drawer-footer{

    padding:18px 20px;

}

}

@media (max-width:768px){

.drawer-title{

    gap:12px;

}

.drawer-icon{

    width:48px;

    height:48px;

    font-size:20px;

}

.drawer-footer{

    flex-direction:column-reverse;

}

.drawer-footer .btn{

    width:100%;

}

.section-body{

    padding:20px;

}

.upload-area{

    min-height:180px;

}

.image-wrapper{

    height:200px;

}

}

@media (max-width:576px){

.form-content{

    padding:14px;

}

.section-body{

    padding:16px;

}

.section-header{

    padding:18px;

}

.drawer-header{

    padding:16px;

}

.drawer-title h3{

    font-size:18px;

}

.drawer-title p{

    font-size:13px;

}

}

/* =========================================================
   LOADING
========================================================= */

.loading{

    pointer-events:none;

    opacity:.65;

}

/* =========================================================
   TRANSITION
========================================================= */

.fade-enter-active,

.fade-leave-active{

    transition:.25s;

}

.fade-enter-from,

.fade-leave-to{

    opacity:0;

}

.slide-enter-active{

    transition:.25s;

}

.slide-enter-from{

    transform:translateY(20px);

    opacity:0;

}

/* =========================================================
   GLASS EFFECT
========================================================= */

.drawer-header{

    backdrop-filter:blur(14px);

    background:rgba(255,255,255,.92);

}

.drawer-footer{

    backdrop-filter:blur(14px);

    background:rgba(255,255,255,.92);

}

/* =========================================================
   CARD BORDER GRADIENT
========================================================= */

.content-card{

    position:relative;

}

.content-card::before{

    content:'';

    position:absolute;

    inset:0;

    border-radius:22px;

    padding:1px;

    background:

        linear-gradient(

            180deg,

            rgba(255,255,255,.8),

            rgba(226,232,240,.8)

        );

    -webkit-mask:

        linear-gradient(#fff 0 0) content-box,

        linear-gradient(#fff 0 0);

    -webkit-mask-composite:xor;

    mask-composite:exclude;

    pointer-events:none;

}

/* =========================================================
   SECTION TITLE
========================================================= */

.section-header h4{

    position:relative;

    padding-left:14px;

}

.section-header h4::before{

    content:'';

    position:absolute;

    left:0;

    top:50%;

    transform:translateY(-50%);

    width:5px;

    height:24px;

    border-radius:999px;

    background:linear-gradient(

        180deg,

        #2563eb,

        #60a5fa

    );

}

/* =========================================================
   PRIMARY GRADIENT BUTTON
========================================================= */

.btn-primary{

    background:

        linear-gradient(

            135deg,

            #2563eb,

            #3b82f6

        );

}

.btn-primary:hover{

    background:

        linear-gradient(

            135deg,

            #1d4ed8,

            #2563eb

        );

}

/* =========================================================
   INPUT ANIMATION
========================================================= */

.form-control,

.form-select{

    transition:

        border-color .25s,

        box-shadow .25s,

        transform .2s;

}

.form-control:focus,

.form-select:focus{

    transform:translateY(-1px);

}

/* =========================================================
   IMAGE SHADOW
========================================================= */

.image-wrapper img{

    box-shadow:

        0 6px 16px rgba(15,23,42,.06);

}

/* =========================================================
   HOVER LIFT
========================================================= */

.image-card,

.fragrance-card,

.content-card{

    will-change:transform;

}

/* =========================================================
   VARIANT ROW
========================================================= */

.variant-table tbody tr{

    transition:

        background .2s,

        transform .2s;

}

.variant-table tbody tr:hover{

    transform:scale(1.002);

}

/* =========================================================
   SMOOTH SCROLL
========================================================= */

.custom-scrollbar{

    scroll-behavior:smooth;

}
</style>