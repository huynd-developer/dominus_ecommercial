<template>
  <div class="product-modal">
    <!-- Overlay -->
    <div class="modal-overlay" @click="closeModal"></div>

    <!-- Drawer -->
    <div class="product-drawer">
      <!-- HEADER -->
      <div class="drawer-header">
        <div class="drawer-title">
          <div class="drawer-icon">
            <i
              class="bi"
              :class="isEdit ? 'bi-pencil-square' : 'bi-plus-circle'"
            ></i>
          </div>
          <div>
            <h3>{{ isEdit ? "Cập nhật sản phẩm" : "Thêm sản phẩm" }}</h3>
            <p>
              {{
                isEdit
                  ? "Chỉnh sửa thông tin sản phẩm"
                  : "Tạo sản phẩm mới trong hệ thống"
              }}
            </p>
          </div>
        </div>
        <button class="btn-close-modal" @click="closeModal">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <!-- BODY -->
      <div class="drawer-body custom-scrollbar">
        <form class="product-form" @submit.prevent="saveData">
          <div class="form-content">
            <!-- 1. THÔNG TIN CƠ BẢN -->
            <section class="content-card">
              <div class="section-header">
                <div>
                  <h4>Thông tin cơ bản</h4>
                  <span>Các thông tin chính của sản phẩm</span>
                </div>
              </div>
              <div class="section-body">
                <div
                  class="alert alert-warning py-2 mb-4"
                  v-if="isEdit && formData.status === 1"
                >
                  <i class="bi bi-exclamation-triangle me-2"></i>
                  Sản phẩm đang ở trạng thái <b>Đang bán</b> nên không thể sửa
                  thông tin cơ bản. Vui lòng chuyển trạng thái sang
                  <b>Ngừng bán</b> để chỉnh sửa.
                </div>

                <div class="row g-4">
                  <!-- Trạng thái -->
                  <div class="col-lg-4">
                    <label class="form-label"
                      >Trạng thái <span class="text-danger">*</span></label
                    >
                    <select
                      v-model.number="formData.status"
                      class="form-select bg-white"
                    >
                      <option :value="1">Đang bán</option>
                      <option :value="0">Ngừng bán</option>
                    </select>
                  </div>

                  <div class="col-12 mt-3">
                    <label class="form-label"
                      >Tên sản phẩm <span class="text-danger">*</span></label
                    >
                    <input
                      v-model.trim="formData.name"
                      class="form-control"
                      placeholder="Ví dụ: Dior Sauvage Eau De Parfum"
                      :disabled="isEdit && formData.status === 1"
                    />
                  </div>

                  <div class="col-lg-4">
                    <label class="form-label"
                      >Thương hiệu <span class="text-danger">*</span></label
                    >
                    <select
                      v-model.number="formData.brandId"
                      class="form-select"
                      :disabled="isEdit && formData.status === 1"
                    >
                      <option :value="0">Chọn thương hiệu</option>
                      <option
                        v-for="brand in brandList"
                        :key="brand.id"
                        :value="brand.id"
                      >
                        {{ brand.brandName ?? brand.name }}
                      </option>
                    </select>
                  </div>

                  <div class="col-lg-4">
                    <label class="form-label"
                      >Danh mục <span class="text-danger">*</span></label
                    >
                    <select
                      v-model.number="formData.categoryId"
                      class="form-select"
                      :disabled="isEdit && formData.status === 1"
                    >
                      <option :value="0">Chọn danh mục</option>
                      <option
                        v-for="category in categoryList"
                        :key="category.id"
                        :value="category.id"
                      >
                        {{ category.categoryName ?? category.name }}
                      </option>
                    </select>
                  </div>

                  <div class="col-lg-4">
                    <label class="form-label"
                      >Nồng độ <span class="text-danger">*</span></label
                    >
                    <select
                      v-model.number="formData.concentrationId"
                      class="form-select"
                      :disabled="isEdit && formData.status === 1"
                    >
                      <option :value="0">Chọn nồng độ</option>
                      <option
                        v-for="item in concentrationList"
                        :key="item.id"
                        :value="item.id"
                      >
                        {{ item.concentrationName ?? item.name }}
                      </option>
                    </select>
                  </div>

                  <div class="col-lg-4">
                    <label class="form-label"
                      >Giới tính <span class="text-danger">*</span></label
                    >
                    <select
                      v-model.number="formData.gender"
                      class="form-select"
                      :disabled="isEdit && formData.status === 1"
                    >
                      <option :value="1">Nam</option>
                      <option :value="2">Nữ</option>
                      <option :value="0">Unisex</option>
                    </select>
                  </div>

                  <div class="col-lg-4 d-flex align-items-end mb-2">
                    <div class="form-check form-switch fs-6 mb-2">
                      <input
                        id="isNiche"
                        v-model="formData.isNiche"
                        class="form-check-input"
                        type="checkbox"
                        :disabled="isEdit && formData.status === 1"
                      />
                      <label for="isNiche" class="form-check-label ms-2"
                        >Nước hoa Niche</label
                      >
                    </div>
                  </div>

                  <div class="col-12">
                    <label class="form-label">Mô tả sản phẩm</label>
                    <textarea
                      v-model="formData.description"
                      class="form-control"
                      rows="4"
                      placeholder="Nhập mô tả chi tiết về sản phẩm..."
                      :disabled="isEdit && formData.status === 1"
                    ></textarea>
                  </div>
                </div>
              </div>
            </section>

            <!-- 2. NHÓM HƯƠNG -->
            <section
              class="content-card"
              :class="{
                'opacity-50 pointer-events-none':
                  isEdit && formData.status === 1,
              }"
            >
              <div class="section-header">
                <div>
                  <h4>Nhóm hương <span class="text-danger">*</span></h4>
                  <span>Chọn các nhóm hương đặc trưng của sản phẩm</span>
                </div>
                <div class="section-badge">
                  {{ formData.fragranceFamilyIds.length }} nhóm hương
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
                        active: formData.fragranceFamilyIds.includes(item.id),
                      }"
                    >
                      <input
                        type="checkbox"
                        :value="item.id"
                        v-model="formData.fragranceFamilyIds"
                        class="d-none"
                        :disabled="isEdit && formData.status === 1"
                      />
                      <div
                        class="d-flex justify-content-between align-items-center"
                      >
                        <div class="fw-medium text-truncate pe-2">
                          {{ item.name }}
                        </div>
                        <div class="check-icon">
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
              </div>
            </section>

            <!-- 3. DANH SÁCH BIẾN THỂ -->
            <section class="content-card">
              <div class="section-header">
                <div>
                  <h4>Danh sách biến thể</h4>
                  <span>Quản lý dung tích, loại chai, giá bán và tồn kho</span>
                </div>
                <button
                  type="button"
                  class="btn btn-primary rounded-pill px-4"
                  @click="addVariant"
                >
                  <i class="bi bi-plus-lg me-2"></i> Thêm biến thể
                </button>
              </div>
              <div class="section-body p-0">
                <div class="variant-table m-3 mt-0">
                  <table class="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th width="150">
                          Dung tích <span class="text-danger">*</span>
                        </th>
                        <th width="180">
                          Loại chai <span class="text-danger">*</span>
                        </th>
                        <th width="180">
                          Giá bán <span class="text-danger">*</span>
                        </th>
                        <th width="120">
                          Tồn kho <span class="text-danger">*</span>
                        </th>
                        <th width="160">
                          NSX <span class="text-danger">*</span>
                        </th>
                        <th width="160">
                          HSD <span class="text-danger">*</span>
                        </th>
                        <th width="140">Trạng thái</th>
                        <th width="60" class="text-center">
                          <i class="bi bi-gear"></i>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="(variant, index) in formData.variants"
                        :key="index"
                      >
                        <td>
                          <select
                            v-model.number="variant.capacityId"
                            class="form-select"
                          >
                            <option :value="0">Chọn</option>
                            <option
                              v-for="item in capacityList"
                              :key="item.id"
                              :value="item.id"
                            >
                              {{ item.value }} ml
                            </option>
                          </select>
                        </td>
                        <td>
                          <select
                            v-model.number="variant.bottleTypeId"
                            class="form-select"
                          >
                            <option :value="0">Chọn</option>
                            <option
                              v-for="item in bottleTypeList"
                              :key="item.id"
                              :value="item.id"
                              :disabled="isVariantDuplicate(variant.capacityId, item.id, index)"
                            >
                              {{ item.bottleTypeName ?? item.name }}
                            </option>
                          </select>
                        </td>
                        <td>
                          <div class="input-group">
                            <span class="input-group-text">₫</span>
                            <input
                              :value="formatDisplayPrice(variant.price)"
                              @input="onPriceInput(index, $event)"
                              type="text"
                              class="form-control text-end"
                              placeholder="0"
                            />
                          </div>
                        </td>
                        <!-- CẬP NHẬT PHẦN INPUT TỒN KHO Ở ĐÂY -->
                        <td>
                          <input
                            :value="variant.stockQuantity"
                            @input="onStockInput(index, $event)"
                            type="text"
                            class="form-control text-center"
                            placeholder="0"
                          />
                        </td>
                        <td>
                          <input
                            v-model="variant.manufacturingDate"
                            type="date"
                            class="form-control"
                            :max="today"
                          />
                        </td>
                        <td>
                          <input
                            v-model="variant.expirationDate"
                            type="date"
                            class="form-control"
                            :min="today"
                          />
                        </td>
                        <td>
                          <select
                            v-model.number="variant.status"
                            class="form-select"
                          >
                            <option :value="1">Đang bán</option>
                            <option :value="0">Ngừng bán</option>
                          </select>
                        </td>
                        <td class="text-center">
                          <button
                            v-if="formData.variants.length > 1"
                            type="button"
                            class="btn btn-outline-danger rounded-circle btn-icon mx-auto"
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

            <!-- 4. HÌNH ẢNH SẢN PHẨM -->
            <section class="content-card mb-5">
              <div class="section-header">
                <div>
                  <h4>Hình ảnh sản phẩm</h4>
                  <span>Chọn ảnh hiển thị của sản phẩm (Tối đa 6 ảnh, mỗi ảnh không quá 10MB)</span>
                </div>
                <div class="section-badge">{{ imageList.length }}/6 ảnh</div>
              </div>
              <div class="section-body">
                <label class="upload-area" :class="{ 'opacity-50 pointer-events-none': imageList.length >= 6 }">
                  <input
                    type="file"
                    multiple
                    accept="image/jpeg, image/png, image/webp, image/gif, image/jpg"
                    hidden
                    :disabled="imageList.length >= 6"
                    @change="handleImages"
                  />
                  <i class="bi bi-cloud-arrow-up"></i>
                  <h6>{{ imageList.length >= 6 ? 'Đã đạt giới hạn 6 ảnh' : 'Kéo ảnh vào đây' }}</h6>
                  <p v-if="imageList.length < 6">
                    hoặc nhấn để chọn ảnh (chỉ chấp nhận file ảnh JPG, PNG,
                    WEBP...)
                  </p>
                </label>

                <transition-group
                  name="image-list"
                  tag="div"
                  class="row g-4 mt-1"
                >
                  <div
                    class="col-xl-3 col-lg-4 col-md-6 image-item"
                    v-for="(img, index) in imageList"
                    :key="img.preview"
                  >
                    <div class="image-card">
                      <div class="image-wrapper">
                        <img :src="img.preview" />
                        <div class="image-overlay">
                          <button
                            type="button"
                            class="btn btn-light btn-sm"
                            @click="setPrimaryImage(index)"
                          >
                            <i class="bi bi-star-fill text-warning"></i>
                          </button>
                          <button
                            type="button"
                            class="btn btn-danger btn-sm"
                            @click="removeImage(index)"
                          >
                            <i class="bi bi-trash"></i>
                          </button>
                        </div>
                      </div>
                      <div class="image-footer">
                        <button
                          v-if="img.isPrimary"
                          type="button"
                          class="btn btn-success w-100 rounded-pill"
                          style="pointer-events: none; opacity: 1"
                        >
                          Ảnh chính
                        </button>
                        <button
                          v-else
                          type="button"
                          class="btn btn-outline-success w-100 rounded-pill"
                          @click="setPrimaryImage(index)"
                        >
                          Đặt làm ảnh chính
                        </button>
                      </div>
                    </div>
                  </div>
                </transition-group>
              </div>
            </section>
          </div>

          <!-- FOOTER -->
          <div class="drawer-footer">
            <button
              type="button"
              class="btn btn-light px-4"
              @click="closeModal"
            >
              <i class="bi bi-x-circle me-2"></i> Hủy
            </button>
            <button
              type="submit"
              class="btn btn-primary px-5"
              :disabled="appStore.globalLoading || isCloningImages"
            >
              <span
                v-if="appStore.globalLoading || isCloningImages"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              <i
                v-else
                class="bi"
                :class="isEdit ? 'bi-pencil-square' : 'bi-plus-circle'"
              ></i>
              {{
                isCloningImages
                  ? " Đang xử lý ảnh..."
                  : isEdit
                    ? " Cập nhật"
                    : " Thêm sản phẩm"
              }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
import Swal from "sweetalert2";
import { productService } from "../services/productService";
import { useAppStore } from "@/common/store/app.store";
import { useProductStore } from "../stores/productStore"; 

import type {
  Product,
  Brand,
  Category,
  Concentration,
  Capacity,
  BottleType,
  FragranceFamily,
} from "../types/product.type";

const props = defineProps<{
  productSelected: Product | null;
  isClone?: boolean;
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
const productStore = useProductStore();
const isEdit = ref(false);
const API_URL = import.meta.env.VITE_API_URL || "";
const today = computed(() => new Date().toISOString().split("T")[0]);
const isCloningImages = ref(false); 

interface ProductImageItem {
  id?: number;
  file?: File;
  preview: string;
  isPrimary: boolean;
}

const imageList = ref<ProductImageItem[]>([]);
const deletedImageIds = ref<number[]>([]);

const formData = ref<any>({
  name: "",
  description: "",
  brandId: 0,
  categoryId: 0,
  concentrationId: 0,
  gender: 0,
  isNiche: false,
  status: 1,
  fragranceFamilyIds: [],
  variants: [],
});

const resetForm = () => {
  formData.value = {
    name: "",
    description: "",
    brandId: 0,
    categoryId: 0,
    concentrationId: 0,
    gender: 0,
    isNiche: false,
    status: 1,
    fragranceFamilyIds: [],
    variants: [],
  };
  imageList.value = [];
  deletedImageIds.value = [];

  const defaultCapacities = [10, 50, 100];
  defaultCapacities.forEach((targetVal) => {
    const foundCap = props.capacityList?.find(
      (c: any) => Number(c.value) === targetVal || Number(c.name) === targetVal,
    );
    formData.value.variants.push({
      capacityId: foundCap ? foundCap.id : 0,
      bottleTypeId: props.bottleTypeList?.[0]?.id ?? 0,
      price: 100,
      stockQuantity: 10,
      manufacturingDate: today.value,
      expirationDate: new Date(
        new Date().setFullYear(new Date().getFullYear() + 3),
      )
        .toISOString()
        .split("T")[0],
      status: 1,
    });
  });

  if (formData.value.variants.length === 0) {
    addVariant();
  }
};

const addVariant = () => {
  formData.value.variants.push({
    capacityId: 0,
    bottleTypeId: 0,
    price: 0,
    stockQuantity: 0,
    manufacturingDate: "",
    expirationDate: "",
    status: 1,
  });
};

const isVariantDuplicate = (
  capacityId: number | string,
  bottleTypeId: number | string,
  currentIndex: number | string,
) => {
  const currentCapId = Number(capacityId || 0);
  const currentBotId = Number(bottleTypeId || 0);
  const currentIndexNumber = Number(currentIndex);

  if (!currentCapId || !currentBotId || Number.isNaN(currentIndexNumber)) {
    return false;
  }

  // Chặn nếu có dòng khác trùng cả Dung tích VÀ Loại chai
  return formData.value.variants.some((variant: any, index: number) => {
    return (
      index !== currentIndexNumber &&
      Number(variant?.capacityId || 0) === currentCapId &&
      Number(variant?.bottleTypeId || 0) === currentBotId
    );
  });
};

const formatDisplayPrice = (price?: number) => {
  if (!price) return "";
  return new Intl.NumberFormat("en-US").format(price);
};

const onPriceInput = (index: any, event: Event) => {
  const input = event.target as HTMLInputElement;
  const rawValue = input.value.replace(/\D/g, "");
  const numericValue = rawValue ? Number(rawValue) : 0;

  formData.value.variants[index].price = numericValue;
  input.value = rawValue
    ? new Intl.NumberFormat("en-US").format(numericValue)
    : "";
};

// Hàm xử lý chặn nhập tồn kho
const onStockInput = (index: any, event: Event) => {
  const input = event.target as HTMLInputElement;
  const rawValue = input.value.replace(/\D/g, ""); // Xóa bỏ chữ, dấu thập phân, dấu âm
  let numericValue = rawValue ? parseInt(rawValue, 10) : 0;

  // Giới hạn max là 1000
  if (numericValue > 1000) {
    numericValue = 1000;
  }

  formData.value.variants[index].stockQuantity = numericValue;
  input.value = rawValue ? String(numericValue) : "";
};

const formatDateForInput = (dateString?: string) => {
  if (!dateString) return "";
  return dateString.substring(0, 10);
};

const fillForm = async (product: Product, isClone = false) => {
  let newName = product.name;
  if (isClone && !newName.endsWith("(Bản sao)")) {
    newName = `${newName} (Bản sao)`;
  }

  formData.value = {
    name: newName,
    description: product.description ?? "",
    brandId: product.brandId,
    categoryId: product.categoryId,
    concentrationId: product.concentrationId,
    gender: product.gender ?? 0,
    isNiche: product.isNiche ?? false,
    status: isClone ? 1 : (product.status ?? 1),
    fragranceFamilyIds: product.fragranceFamilies?.map((item) => item.id) ?? [],
    variants:
      product.variants?.map((v) => ({
        id: isClone ? undefined : v.id,
        capacityId: v.capacityId,
        bottleTypeId: v.bottleTypeId,
        price: v.price,
        stockQuantity: v.stockQuantity,
        manufacturingDate: formatDateForInput(v.manufacturingDate),
        expirationDate: formatDateForInput(v.expirationDate),
        status: v.status ?? 1,
        sku: isClone ? undefined : (v as any).sku,
      })) ?? [],
  };

  if (formData.value.variants.length === 0) addVariant();

  const initialImages = (product.images || []).map((img) => {
    const url = img.imageUrl
      ? img.imageUrl.startsWith("http")
        ? img.imageUrl
        : `${API_URL}${img.imageUrl}`
      : "";
    return {
      id: isClone ? undefined : img.id,
      preview: url,
      isPrimary: img.isPrimary,
      file: undefined,
    };
  });

  imageList.value = initialImages.sort((a, b) =>
    a.isPrimary === b.isPrimary ? 0 : a.isPrimary ? -1 : 1
  ).slice(0, 6);

  if (isClone) {
    isCloningImages.value = true; 
    
    Promise.all(
      imageList.value.map(async (img, idx) => {
        if (img.preview) {
          try {
            const res = await fetch(img.preview);
            if (res.ok) {
              const blob = await res.blob();
              const fileType = blob.type || "image/jpeg";
              img.file = new File([blob], `clone_${Date.now()}_${idx}.jpg`, { type: fileType });
            }
          } catch (error) {
            console.error("Lỗi tải ngầm ảnh clone:", error);
          }
        }
      })
    ).then(() => {
      imageList.value = imageList.value.filter(img => img.file);
      isCloningImages.value = false;
    });
  }
};

watch(
  () => props.productSelected,
  async (product) => {
    if (product) {
      isEdit.value = !props.isClone;
      await fillForm(product, props.isClone);
    } else {
      isEdit.value = false;
      resetForm();
    }
  },
  { immediate: true },
);

const removeVariant = (index: any) => {
  formData.value.variants.splice(index, 1);
  if (formData.value.variants.length === 0) addVariant();
};

const handleImages = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;

  const validTypes = [
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp",
    "image/gif",
  ];
  
  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  let hasInvalidType = false;
  let hasOversizedFile = false;
  let skippedLimit = 0;

  Array.from(files).forEach((file) => {
    if (imageList.value.length >= 6) {
      skippedLimit++;
      return;
    }

    if (!validTypes.includes(file.type.toLowerCase())) {
      hasInvalidType = true;
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      hasOversizedFile = true;
      return;
    }

    imageList.value.push({
      file,
      preview: URL.createObjectURL(file),
      isPrimary: imageList.value.length === 0,
    });
  });

  input.value = "";

  if (skippedLimit > 0 || hasInvalidType || hasOversizedFile) {
    let errorMsg = "";
    if (skippedLimit > 0) errorMsg += `<p>• Chỉ được tải lên tối đa 6 ảnh.</p>`;
    if (hasOversizedFile) errorMsg += `<p>• Một số ảnh vượt quá dung lượng 10MB.</p>`;
    if (hasInvalidType) errorMsg += `<p>• Một số file sai định dạng hình ảnh.</p>`;
    
    Swal.fire({
      title: "Lỗi tải ảnh",
      html: `<div style="text-align: left;">${errorMsg}</div>`,
      icon: "warning",
    });
  }
};

const setPrimaryImage = (index: number) => {
  if (index === 0) return;
  const selectedImage = imageList.value[index];
  if (!selectedImage) return;

  imageList.value.forEach((img) => (img.isPrimary = false));
  selectedImage.isPrimary = true;

  imageList.value.splice(index, 1);
  imageList.value.unshift(selectedImage);
};

const removeImage = (index: number) => {
  const image = imageList.value[index];
  if (image?.id && !props.isClone) deletedImageIds.value.push(image.id);
  imageList.value.splice(index, 1);

  if (imageList.value.length > 0 && !imageList.value.some((x) => x.isPrimary)) {
    imageList.value[0]!.isPrimary = true;
  }
};

const validateForm = () => {
  const name = formData.value.name.trim();
  if (!name) {
    Swal.fire("Thiếu dữ liệu", "Vui lòng nhập Tên sản phẩm.", "warning");
    return false;
  }

  const isNameChanged = isEdit.value ? name.toLowerCase() !== props.productSelected?.name.toLowerCase() : true;
  
  if (isNameChanged) {
    const isDuplicate = productStore.products.some(
      (p) => p.name.toLowerCase() === name.toLowerCase()
    );
    if (isDuplicate) {
      Swal.fire("Trùng lặp", `Tên sản phẩm "${name}" đã tồn tại trong hệ thống. Vui lòng chọn tên khác!`, "error");
      return false;
    }
  }

  if (formData.value.brandId === 0) {
    Swal.fire("Thiếu dữ liệu", "Vui lòng chọn Thương hiệu.", "warning");
    return false;
  }
  if (formData.value.categoryId === 0) {
    Swal.fire("Thiếu dữ liệu", "Vui lòng chọn Danh mục.", "warning");
    return false;
  }
  if (formData.value.concentrationId === 0) {
    Swal.fire("Thiếu dữ liệu", "Vui lòng chọn Nồng độ.", "warning");
    return false;
  }
  if (formData.value.fragranceFamilyIds.length === 0) {
    Swal.fire(
      "Thiếu dữ liệu",
      "Vui lòng chọn ít nhất 1 Nhóm hương.",
      "warning",
    );
    return false;
  }
  if (formData.value.variants.length === 0) {
    Swal.fire(
      "Thiếu dữ liệu",
      "Phải có ít nhất 1 biến thể sản phẩm.",
      "warning",
    );
    return false;
  }

  const variantPairSet = new Set<string>();
  for (let i = 0; i < formData.value.variants.length; i++) {
    const variant = formData.value.variants[i];
    if (variant.capacityId === 0 || variant.bottleTypeId === 0) {
      Swal.fire(
        "Thiếu dữ liệu",
        `Biến thể dòng ${i + 1}: Vui lòng chọn đầy đủ Dung tích và Loại chai.`,
        "warning",
      );
      return false;
    }
    const capacityId = Number(variant.capacityId || 0);
    const bottleTypeId = Number(variant.bottleTypeId || 0);
    const pairKey = `${capacityId}-${bottleTypeId}`;

    if (variantPairSet.has(pairKey)) {
      Swal.fire(
        "Trùng biến thể",
        `Biến thể dòng ${i + 1}: Không được phép có 2 biến thể trùng cả Dung tích và Loại chai giống nhau!`,
        "error",
      );
      return false;
    }
    variantPairSet.add(pairKey);

    if (variant.price <= 0 || isNaN(variant.price)) {
      Swal.fire(
        "Lỗi dữ liệu",
        `Biến thể dòng ${i + 1}: Giá bán phải lớn hơn 0.`,
        "warning",
      );
      return false;
    }
  }
  return true;
};

const saveData = async () => {
  if (!validateForm()) return;

  try {
    const payload = {
      name: formData.value.name.trim(),
      description: formData.value.description
        ? formData.value.description.trim()
        : "",
      brandId: Number(formData.value.brandId),
      categoryId: Number(formData.value.categoryId),
      concentrationId: Number(formData.value.concentrationId),
      gender: Number(formData.value.gender ?? 3),
      isNiche: Boolean(formData.value.isNiche),
      status: Number(formData.value.status ?? 1),
      variants: (formData.value.variants || []).map((v: any) => ({
        id: props.isClone ? undefined : v.id ? Number(v.id) : undefined,
        capacityId: Number(v.capacityId),
        bottleTypeId: Number(v.bottleTypeId),
        price: Number(v.price),
        stockQuantity: Number(v.stockQuantity),
        manufacturingDate: v.manufacturingDate
          ? String(v.manufacturingDate).substring(0, 10)
          : "",
        expirationDate: v.expirationDate
          ? String(v.expirationDate).substring(0, 10)
          : "",
        status: Number(v.status ?? 1),
        sku: v.sku ? String(v.sku).trim() : undefined,
      })),
      fragranceFamilyIds: [
        ...new Set((formData.value.fragranceFamilyIds || []).map(Number)),
      ],
    } as any;

    appStore.startLoading();

    if (isEdit.value && props.productSelected && !props.isClone) {
      const confirmResult = await Swal.fire({
        title: "Xác nhận cập nhật?",
        text: "Bạn có chắc chắn muốn lưu các thay đổi không?",
        icon: "question",
        showCancelButton: true,
        confirmButtonColor: "#2563eb",
        cancelButtonColor: "#ef4444",
        confirmButtonText: "Đồng ý",
        cancelButtonText: "Hủy",
      });

      if (!confirmResult.isConfirmed) {
        appStore.stopLoading();
        return;
      }

      await productService.updateProduct(props.productSelected.id, payload);

      for (const imageId of deletedImageIds.value) {
        await productService.deleteImage(imageId);
      }

      for (const image of imageList.value) {
        if (!image.file) continue;
        await productService.uploadImage(props.productSelected.id, image.file);
      }

      const primaryImage = imageList.value.find((img) => img.isPrimary);
      if (primaryImage?.id) {
        await productService.setPrimaryImage(
          props.productSelected.id,
          primaryImage.id,
        );
      }

      emit("refresh");
      emit("close");
      Swal.fire({
        toast: true,
        position: "top-end",
        icon: "success",
        title: "Cập nhật sản phẩm thành công",
        showConfirmButton: false,
        timer: 2000,
      });
    } else {
      // Thêm mới hoặc Nhân bản sản phẩm
      const created = await productService.createProduct(payload);

      for (const image of imageList.value) {
        if (image.file) {
          await productService.uploadImage(created.id, image.file);
        }
      }

      const uploadedImages = await productService.getImagesByProduct(
        created.id,
      );
      const primaryIndex = imageList.value.findIndex((img) => img.isPrimary);

      if (primaryIndex >= 0 && uploadedImages?.[primaryIndex]?.id) {
        await productService.setPrimaryImage(
          created.id,
          uploadedImages[primaryIndex].id,
        );
      }

      emit("refresh");
      emit("close");
      Swal.fire({
        toast: true,
        position: "top-end",
        icon: "success",
        title: props.isClone
          ? "Nhân bản sản phẩm thành công"
          : "Thêm sản phẩm thành công",
        showConfirmButton: false,
        timer: 2000,
      });
    }
  } catch (e: any) {
    console.error(e);
    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: e?.response?.data?.message ?? "Không thể lưu sản phẩm",
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
.product-modal {
  position: fixed;
  inset: 0;
  z-index: 1055;
}
.modal-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(3px);
  animation: fadeOverlay 0.25s;
}
.product-drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: min(1550px, 96vw);
  height: 100%;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  border-radius: 24px 0 0 24px;
  overflow: hidden;
  box-shadow: -12px 0 50px rgba(15, 23, 42, 0.18);
  animation: drawerIn 0.25s ease;
}

/* HEADER */
.drawer-header {
  height: 88px;
  flex-shrink: 0;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  border-bottom: 1px solid #e2e8f0;
}
.drawer-title {
  display: flex;
  align-items: center;
  gap: 18px;
}
.drawer-icon {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.25);
}
.drawer-title h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}
.drawer-title p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 14px;
}
.btn-close-modal {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: #f1f5f9;
  transition: 0.25s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-close-modal:hover {
  background: #ef4444;
  color: #fff;
}

/* BODY & CARDS */
.drawer-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.product-form {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.form-content {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}
.content-card {
  background: #fff;
  border-radius: 22px;
  margin-bottom: 24px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  border: 1px solid #eef2f7;
  position: relative;
  transition: 0.25s;
}
.content-card:hover {
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.07);
}
.section-header {
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eef2f7;
}
.section-header h4 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  position: relative;
  padding-left: 14px;
}
.section-header h4::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 24px;
  border-radius: 999px;
  background: linear-gradient(180deg, #2563eb, #60a5fa);
}
.section-header span {
  color: #94a3b8;
  font-size: 14px;
}
.section-body {
  padding: 28px;
}
.section-badge {
  background: #eff6ff;
  color: #2563eb;
  padding: 8px 16px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 13px;
}

/* FORM CONTROLS */
.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}
.form-control,
.form-select {
  min-height: 48px;
  border: 1px solid #dbe4ee;
  border-radius: 14px;
  background: #fff;
  transition: all 0.25s ease;
  font-size: 14px;
  color: #0f172a;
  padding-left: 16px;
}
.form-control:hover:not(:disabled),
.form-select:hover:not(:disabled) {
  border-color: #94a3b8;
}
.form-control:focus,
.form-select:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}
textarea.form-control {
  resize: none;
  min-height: 130px;
  padding-top: 14px;
}
.form-control:disabled,
.form-select:disabled {
  background-color: #f1f5f9;
  cursor: not-allowed;
  opacity: 0.7;
}
.pointer-events-none {
  pointer-events: none;
}
.opacity-50 {
  opacity: 0.6;
}
.input-group {
  border-radius: 14px;
  overflow: hidden;
}
.input-group-text {
  background: #f8fafc;
  border: 1px solid #dbe4ee;
  border-right: none;
  color: #64748b;
  font-weight: 600;
  min-width: 46px;
  justify-content: center;
}
.input-group .form-control {
  border-left: none;
}
.form-switch {
  display: flex;
  align-items: center;
  gap: 12px;
}
.form-check-input {
  width: 50px;
  height: 26px;
  cursor: pointer;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background-color: #e2e8f0;
  transition: 0.25s;
}
.form-check-input:checked {
  background-color: #2563eb;
  border-color: #2563eb;
}
.form-check-label {
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

/* NHÓM HƯƠNG */
.fragrance-card {
  display: block;
  padding: 12px 20px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  cursor: pointer;
  transition: 0.25s;
  background: white;
  will-change: transform;
}
.fragrance-card:hover {
  border-color: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
}
.fragrance-card.active {
  border-color: #2563eb;
  background: #eff6ff;
}
.check-icon {
  font-size: 20px;
  color: #2563eb;
  line-height: 1;
}
.selected-list {
  border-top: 1px dashed #dbe4ee;
  padding-top: 20px;
}

/* BẢNG BIẾN THỂ */
.variant-table {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  overflow: auto;
  background: #fff;
}
.variant-table table {
  margin: 0;
  min-width: 1200px;
}
.variant-table thead {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #f8fafc;
}
.variant-table thead th {
  padding: 16px 14px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
  vertical-align: middle;
}
.variant-table tbody td {
  padding: 14px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
  background: white;
}
.variant-table tbody tr {
  transition: 0.2s;
  animation: variantFade 0.25s ease;
}
.variant-table tbody tr:hover td {
  background: #f8fbff;
  transform: scale(1.001);
}
.variant-table .form-control,
.variant-table .form-select {
  min-height: 42px;
  font-size: 13px;
  border-radius: 12px;
}
.variant-table input[type="text"],
.variant-table input[type="number"] {
  font-weight: 600;
}
.btn-icon {
  width: 38px;
  height: 38px;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 10px;
  transition: 0.25s;
}
.btn-icon:hover {
  transform: scale(1.08);
}
.variant-table::-webkit-scrollbar {
  height: 9px;
}
.variant-table::-webkit-scrollbar-track {
  background: #f8fafc;
}
.variant-table::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

/* HÌNH ẢNH */
.upload-area {
  width: 100%;
  min-height: 200px;
  border: 2px dashed #cbd5e1;
  border-radius: 20px;
  background: #fbfdff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: 0.25s;
  text-align: center;
  margin-bottom: 28px;
}
.upload-area:hover {
  border-color: #2563eb;
  background: #eff6ff;
}
.upload-area i {
  font-size: 48px;
  color: #2563eb;
  margin-bottom: 12px;
}
.upload-area h6 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}
.upload-area p {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 14px;
}
.image-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  overflow: hidden;
  transition: 0.25s;
  height: 100%;
}
.image-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 35px rgba(15, 23, 42, 0.08);
}
.image-wrapper {
  position: relative;
  height: 220px;
  overflow: hidden;
  background: #f8fafc;
}
.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: 0.35s;
}
.image-card:hover img {
  transform: scale(1.05);
}
.image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.3);
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  opacity: 0;
  transition: 0.25s;
}
.image-card:hover .image-overlay {
  opacity: 1;
}
.image-overlay .btn {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(4px);
}
.image-footer {
  padding: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-top: 1px solid #f1f5f9;
}
.image-footer .btn {
  font-weight: 600;
  font-size: 13.5px;
  padding: 10px 0;
}
.image-list-move,
.image-list-enter-active,
.image-list-leave-active {
  transition: all 0.4s ease;
}
.image-list-enter-from,
.image-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
.image-list-leave-active {
  position: absolute;
}

/* FOOTER ACTIONS */
.drawer-footer {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(14px);
  border-top: 1px solid #e2e8f0;
  padding: 18px 32px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  position: sticky;
  bottom: 0;
  z-index: 20;
  box-shadow: 0 -8px 20px rgba(15, 23, 42, 0.04);
}
.drawer-footer .btn {
  min-width: 140px;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 14px;
  transition: 0.25s;
}
.drawer-footer .btn-primary {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border: none;
}
.drawer-footer .btn-primary:hover {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  transform: translateY(-1px);
}
.drawer-footer .btn-light {
  background: #fff;
  border: 1px solid #dbe4ee;
}
.drawer-footer .btn-light:hover {
  background: #f8fafc;
}

/* Utils & Animations */
.custom-scrollbar {
  scroll-behavior: smooth;
}
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
button:focus,
input:focus,
select:focus,
textarea:focus {
  outline: none;
}
@keyframes drawerIn {
  from {
    transform: translateX(80px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
@keyframes fadeOverlay {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes variantFade {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Reponsive */
@media (max-width: 1400px) {
  .product-drawer {
    width: 100vw;
    border-radius: 0;
  }
}
@media (max-width: 992px) {
  .drawer-header {
    padding: 20px;
    height: auto;
  }
  .form-content {
    padding: 20px;
  }
  .drawer-footer {
    padding: 18px 20px;
  }
}
@media (max-width: 768px) {
  .drawer-title {
    gap: 12px;
  }
  .drawer-icon {
    width: 48px;
    height: 48px;
    font-size: 20px;
  }
  .drawer-footer {
    flex-direction: column-reverse;
  }
  .drawer-footer .btn {
    width: 100%;
  }
  .section-body {
    padding: 20px;
  }
}
</style>