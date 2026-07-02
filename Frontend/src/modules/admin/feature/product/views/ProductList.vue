<template>
  <div class="container-fluid">

    <!-- ================= HEADER ================= -->

    <div class="card shadow-sm">

      <div
        class="card-header bg-primary text-white d-flex justify-content-between align-items-center">

        <h5 class="mb-0">
          Danh sách sản phẩm
        </h5>

        <RouterLink
          class="btn btn-light"
          to="/admin/products/create">

          <i class="bi bi-plus-circle me-1"></i>

          Thêm sản phẩm

        </RouterLink>

      </div>

      <!-- ================= SEARCH ================= -->

      <div class="card-body border-bottom">

        <div class="row">

          <div class="col-md-4">

            <input
              v-model="store.keyword"
              class="form-control"
              placeholder="Tìm theo tên sản phẩm..."
              @keyup.enter="search"/>

          </div>

          <div class="col-md-2">

            <button
              class="btn btn-primary w-100"
              @click="search">

              Tìm kiếm

            </button>

          </div>

        </div>

      </div>

      <!-- ================= TABLE ================= -->

      <div class="table-responsive">

        <table class="table table-hover align-middle mb-0">

          <thead class="table-light">

          <tr>

            <th width="80">
              Ảnh
            </th>

            <th>Tên</th>

            <th>Thương hiệu</th>

            <th>Danh mục</th>

            <th>Nồng độ</th>

            <th>Giới tính</th>

            <th>Loại</th>

            <th>Trạng thái</th>

            <th width="150">
              Thao tác
            </th>

          </tr>

          </thead>

          <tbody>

          <tr
            v-if="store.loading">

            <td
              colspan="9"
              class="text-center">

              Đang tải dữ liệu...

            </td>

          </tr>

          <tr
            v-else-if="store.products.length==0">

            <td
              colspan="9"
              class="text-center">

              Không có dữ liệu

            </td>

          </tr>

          <tr
            v-for="item in store.products"
            :key="item.id">

            <!-- IMAGE -->

            <td>

              <img
                :src="getImage(item)"
                class="rounded border"
                style="
                  width:70px;
                  height:70px;
                  object-fit:cover;
                ">

            </td>

            <!-- NAME -->

            <td>

              {{ item.name }}

            </td>

            <!-- BRAND -->

            <td>

              {{ item.brandName }}

            </td>

            <!-- CATEGORY -->

            <td>

              {{ item.categoryName }}

            </td>

            <!-- CONCENTRATION -->

            <td>

              {{ item.concentrationName }}

            </td>

            <!-- GENDER -->

            <td>

              {{ genderText(item.gender) }}

            </td>

            <!-- NICHE -->

            <td>

              <span
                class="badge"
                :class="item.isNiche
                    ?'bg-dark'
                    :'bg-secondary'">

                {{ item.isNiche
                  ? 'Niche'
                  : 'Designer' }}

              </span>

            </td>

            <!-- STATUS -->

            <td>

              <span
                class="badge"
                :class="item.status==1
                    ?'bg-success'
                    :'bg-danger'">

                {{ item.status==1
                  ?'Hoạt động'
                  :'Ngừng' }}

              </span>

            </td>

            <!-- ACTION -->

            <td>

              <RouterLink
                class="btn btn-warning btn-sm me-2"
                :to="`/admin/products/${item.id}/edit`">

                Sửa

              </RouterLink>

              <button
                class="btn btn-danger btn-sm"
                @click="remove(item.id)">

                Xóa

              </button>

            </td>

          </tr>

          </tbody>

        </table>

      </div>

      <!-- ================= PAGINATION ================= -->

      <div
        class="card-footer d-flex justify-content-between align-items-center">

        <div>

          Tổng:

          <strong>

            {{ store.page.totalElements || 0 }}

          </strong>

          sản phẩm

        </div>

        <nav>

          <ul class="pagination mb-0">

            <li
              class="page-item"
              :class="{
                disabled:store.currentPage==0
              }">

              <button
                class="page-link"
                @click="prevPage">

                «

              </button>

            </li>

            <li
              class="page-item disabled">

              <span class="page-link">

                {{ store.currentPage+1 }}

                /

                {{ store.page.totalPages || 1 }}

              </span>

            </li>

            <li
              class="page-item"
              :class="{
                disabled:
                store.currentPage>=store.page.totalPages-1
              }">

              <button
                class="page-link"
                @click="nextPage">

                »

              </button>

            </li>

          </ul>

        </nav>

      </div>

    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";

import { useProductStore } from "@/modules/admin/feature/product/store/productStore";

const router = useRouter();

const store = useProductStore();

/* =========================
   LOAD
========================= */

onMounted(async () => {
    await store.fetchProducts();
});

/* =========================
   SEARCH
========================= */

async function search() {

    store.currentPage = 0;

    await store.fetchProducts();

}

/* =========================
   PAGING
========================= */

async function prevPage() {

    if (store.currentPage <= 0) {

        return;

    }

    store.currentPage--;

    await store.fetchProducts();

}

async function nextPage() {

    if (store.currentPage >= store.page.totalPages - 1) {

        return;

    }

    store.currentPage++;

    await store.fetchProducts();

}

/* =========================
   DELETE
========================= */

async function remove(id?: number) {

    if (id == null) {
        return;
    }

    if (!confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) {

        return;

    }

    try {

        await store.delete(id);

        alert("Xóa thành công.");

    } catch (e: any) {

        console.error(e);

        alert(
            e?.response?.data?.message
            ||
            "Không thể xóa sản phẩm."
        );

    }

}

/* =========================
   IMAGE
========================= */

function getImage(item: any): string {

    if (!item.images || item.images.length === 0) {

        return "/images/no-image.png";

    }

    const primary = item.images.find(
        (x: any) => x.isPrimary
    );

    return primary
        ? primary.imageUrl
        : item.images[0].imageUrl;

}

/* =========================
   GENDER
========================= */

function genderText(gender: number): string {

    switch (gender) {

        case 0:
            return "Nam";

        case 1:
            return "Nữ";

        default:
            return "Unisex";

    }

}

/* =========================
   FORMAT
========================= */

function formatStatus(status: number) {

    return status === 1
        ? "Hoạt động"
        : "Ngừng";

}
</script>

<style scoped lang="scss">

/* =========================
   CARD WRAPPER
========================= */
.card {
  border-radius: 12px;
  border: none;
  overflow: hidden;
}

/* HEADER */
.card-header {
  padding: 14px 18px;
  font-weight: 600;

  h5 {
    font-size: 16px;
    letter-spacing: 0.3px;
  }

  .btn {
    border-radius: 8px;
    font-weight: 500;
  }
}

/* =========================
   SEARCH AREA
========================= */
.card-body {
  background: #fafafa;

  input.form-control {
    border-radius: 10px;
    padding: 10px 14px;
    transition: all 0.2s ease;

    &:focus {
      box-shadow: 0 0 0 0.15rem rgba(13, 110, 253, 0.15);
      border-color: #0d6efd;
    }
  }

  .btn {
    border-radius: 10px;
    font-weight: 500;
  }
}

/* =========================
   TABLE
========================= */
.table {
  margin-bottom: 0;

  thead {
    background: #f1f3f5;

    th {
      font-size: 13px;
      font-weight: 600;
      color: #495057;
      border-bottom: 1px solid #e9ecef;
      white-space: nowrap;
    }
  }

  tbody {
    tr {
      transition: all 0.15s ease;

      &:hover {
        background: #f8f9fa;
      }
    }

    td {
      font-size: 14px;
      vertical-align: middle;
    }
  }
}

/* =========================
   IMAGE
========================= */
img {
  border-radius: 10px;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }
}

/* =========================
   BADGES
========================= */
.badge {
  padding: 6px 10px;
  font-size: 12px;
  border-radius: 8px;
  font-weight: 500;
}

/* =========================
   BUTTONS
========================= */
.btn-sm {
  border-radius: 8px;
  padding: 5px 10px;
  font-size: 13px;
}

/* hover button delete */
.btn-danger {
  transition: 0.2s;

  &:hover {
    background: #bb2d3b;
    transform: translateY(-1px);
  }
}

.btn-warning {
  transition: 0.2s;

  &:hover {
    transform: translateY(-1px);
  }
}

/* =========================
   PAGINATION
========================= */
.card-footer {
  background: #fff;
  border-top: 1px solid #eee;

  .page-link {
    border-radius: 8px !important;
    margin: 0 3px;
  }

  .page-item.disabled .page-link {
    opacity: 0.6;
  }
}

/* =========================
   EMPTY / LOADING
========================= */
.text-center {
  color: #6c757d;
}

/* spinner spacing */
.spinner-border {
  width: 2rem;
  height: 2rem;
}

</style>