<template>
  <div class="container-fluid">

    <div class="card shadow-sm">

      <!-- HEADER -->
      <div class="card-header bg-primary text-white">
        <h5 class="mb-0">
          {{ isEdit ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm' }}
        </h5>
      </div>

      <div class="card-body">

        <form @submit.prevent="submit">

          <div class="row g-3">

            <!-- NAME -->
            <div class="col-md-6">
              <label class="form-label">Tên sản phẩm</label>
              <input v-model="form.name" class="form-control" required />
            </div>

            <!-- CODE -->
            <div class="col-md-6">
              <label class="form-label">Mã sản phẩm</label>
              <input v-model="form.code" class="form-control" required />
            </div>

            <!-- BRAND -->
            <div class="col-md-4">
              <label class="form-label">Thương hiệu</label>
              <input v-model="form.brandName" class="form-control" />
            </div>

            <!-- CATEGORY -->
            <div class="col-md-4">
              <label class="form-label">Danh mục</label>
              <input v-model="form.categoryName" class="form-control" />
            </div>

            <!-- CONCENTRATION -->
            <div class="col-md-4">
              <label class="form-label">Nồng độ</label>
              <input v-model="form.concentrationName" class="form-control" />
            </div>

            <!-- PRICE -->
            <div class="col-md-4">
              <label class="form-label">Giá</label>
              <input v-model.number="form.gia" type="number" class="form-control" />
            </div>

            <!-- SIZE -->
            <div class="col-md-4">
              <label class="form-label">Size</label>
              <input v-model="form.size" class="form-control" />
            </div>

            <!-- STOCK -->
            <div class="col-md-4">
              <label class="form-label">Số lượng</label>
              <input v-model.number="form.so_luong" type="number" class="form-control" />
            </div>

            <!-- STATUS -->
            <div class="col-md-6">
              <label class="form-label">Trạng thái</label>

              <select v-model.number="form.tinh_trang" class="form-select">
                <option :value="1">Còn hàng</option>
                <option :value="0">Hết hàng</option>
              </select>
            </div>

            <!-- NICHE -->
            <div class="col-md-6">
              <label class="form-label">Loại sản phẩm</label>

              <div class="mt-2">
                <label class="me-3">
                  <input type="radio" v-model="form.isNiche" :value="true" />
                  Niche
                </label>

                <label>
                  <input type="radio" v-model="form.isNiche" :value="false" />
                  Designer
                </label>
              </div>
            </div>

            <!-- IMAGE -->
            <div class="col-md-12">
              <label class="form-label">Ảnh sản phẩm</label>

              <input type="file" class="form-control" @change="handleFile" />

              <div v-if="preview" class="mt-3">
                <img :src="preview" class="img-thumbnail" style="width:120px;height:120px;object-fit:cover;" />
              </div>
            </div>

          </div>

          <!-- BUTTONS -->
          <div class="mt-4 d-flex gap-2">
            <button class="btn btn-primary">
              {{ isEdit ? 'Cập nhật' : 'Thêm mới' }}
            </button>

            <RouterLink to="/admin/products" class="btn btn-secondary">
              Hủy
            </RouterLink>
          </div>

        </form>

      </div>

    </div>

  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useProductStore } from "@/modules/admin/feature/product/store/productStore";

const store = useProductStore();
const route = useRoute();
const router = useRouter();

const isEdit = computed(() => !!route.params.id);

const form = reactive<any>({
  id: null,
  name: "",
  code: "",
  brandName: "",
  categoryName: "",
  concentrationName: "",
  gia: 0,
  size: "",
  so_luong: 0,
  tinh_trang: 1,
  isNiche: false,
  avatar_url: ""
});

const preview = ref<string>("");

onMounted(async () => {
  if (isEdit.value) {
    const id = Number(route.params.id);
    const data = await store.getById(id);

    Object.assign(form, data);
    preview.value = data.avatar_url;
  }
});

/* FILE */
function handleFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;

  preview.value = URL.createObjectURL(file);

  // nếu backend upload file
  form.avatar_url = file.name; // hoặc upload API sau
}

/* SUBMIT */
async function submit() {
  try {
    if (isEdit.value) {
      await store.update(form.id, form);
      alert("Cập nhật thành công");
    } else {
      await store.create(form);
      alert("Thêm mới thành công");
    }

    router.push("/admin/products");

  } catch (e) {
    console.error(e);
    alert("Có lỗi xảy ra");
  }
}
</script>

<style scoped lang="scss">

.card {
  border-radius: 12px;
  border: none;
}

.card-header {
  font-weight: 600;
}

.form-label {
  font-weight: 500;
  font-size: 14px;
}

.form-control,
.form-select {
  border-radius: 10px;
  padding: 10px;
}

.form-control:focus,
.form-select:focus {
  box-shadow: 0 0 0 0.15rem rgba(13,110,253,.15);
}

.img-thumbnail {
  border-radius: 10px;
}

.btn {
  border-radius: 10px;
  padding: 8px 16px;
}

</style>