<template>
    <div class="card border-0 shadow-sm rounded-4">

        <!-- Header -->

        <div
            class="card-header bg-white border-0 py-3 px-4 d-flex justify-content-between align-items-center flex-wrap"
        >

        </div>

        <!-- Table -->

        <div class="table-responsive">

            <table class="table align-middle table-hover mb-0">

                <thead>

                <tr>

                    <th width="90">Ảnh</th>

                    <th width="260">Tên sản phẩm</th>

                    <th>Thương hiệu</th>

                    <th>Danh mục</th>

                    <th>Nồng độ</th>

                    <th class="text-center">
                        Giới tính
                    </th>

                    <th class="text-center">
                        Loại
                    </th>

                    <th class="text-center">
                        Trạng thái
                    </th>

                    <th class="text-center" width="90">
                        Thao tác
                    </th>

                </tr>

                </thead>

                <tbody>

                <!-- Skeleton -->

                <template v-if="loading">

                    <tr
                        v-for="i in 8"
                        :key="i"
                    >

                        <td>

                            <div class="skeleton image"></div>

                        </td>

                        <td>

                            <div class="skeleton title"></div>

                            <div class="skeleton text mt-2"></div>

                        </td>

                        <td>

                            <div class="skeleton text"></div>

                        </td>

                        <td>

                            <div class="skeleton text"></div>

                        </td>

                        <td>

                            <div class="skeleton text"></div>

                        </td>

                        <td>

                            <div class="skeleton badge"></div>

                        </td>

                        <td>

                            <div class="skeleton badge"></div>

                        </td>

                        <td>

                            <div class="skeleton badge"></div>

                        </td>

                        <td>

                            <div class="skeleton button"></div>

                        </td>

                    </tr>

                </template>

                <!-- Empty -->

                <tr
                    v-else-if="products.length===0"
                >

                    <td
                        colspan="9"
                        class="py-5"
                    >

                        <div class="empty-state">

                            <i
                                class="bi bi-box display-1 text-secondary"
                            ></i>

                            <h4 class="mt-4">

                                Chưa có sản phẩm

                            </h4>

                            <p class="text-muted">

                                Hãy thêm sản phẩm đầu tiên.

                            </p>

                        </div>

                    </td>

                </tr>

                <!-- Data -->

                <tr
                    v-else
                    v-for="item in products"
                    :key="item.id"
                >

                    <!-- Image -->

                    <td>

                        <img

                            v-if="item.images?.length"

                            :src="item.images?.[0]?.imageUrl"

                            class="product-image"

                        >

                        <img

                            v-else

                            src="https://placehold.co/80x80?text=No"

                            class="product-image"

                        >

                    </td>

                    <!-- Name -->

                    <td>

                        <div class="fw-bold">

                            {{ item.name }}

                        </div>

                        <small class="text-muted">

                            ID :
                            {{ item.id }}

                        </small>

                    </td>

                    <td>

                        {{ item.brandName }}

                    </td>

                    <td>

                        {{ item.categoryName }}

                    </td>

                    <td>

                        {{ item.concentrationName }}

                    </td>

                    <!-- Gender -->

                    <td class="text-center">

                        <span
                            class="badge rounded-pill"
                            :class="item.gender==1
                            ?'bg-primary'
                            :'bg-danger'"
                        >

                            <i
                                class="bi"
                                :class="item.gender==1
                                ?'bi-gender-male'
                                :'bi-gender-female'"
                            ></i>

                            {{ item.gender==1?"Nam":"Nữ" }}

                        </span>

                    </td>

                    <!-- Type -->

                    <td class="text-center">

                        <span
                            class="badge rounded-pill"
                            :class="item.isNiche
                            ?'bg-dark'
                            :'bg-secondary'"
                        >

                            {{ item.isNiche
                                ?'Niche'
                                :'Designer'
                            }}

                        </span>

                    </td>

                    <!-- Status -->

                    <td class="text-center">

                        <span
                            class="badge rounded-pill"
                            :class="item.status==1
                            ?'bg-success'
                            :'bg-warning text-dark'"
                        >

                            <i
                                class="bi bi-circle-fill me-1"
                            ></i>

                            {{ item.status==1
                                ?"Hoạt động"
                                :"Ngừng"
                            }}

                        </span>

                    </td>

                    <!-- Action -->

                    <td class="text-center">

                        <div class="dropdown">

                            <button

                                class="btn btn-light btn-sm shadow-sm"

                                data-bs-toggle="dropdown"

                            >

                                <i class="bi bi-three-dots"></i>

                            </button>

                            <ul
                                class="dropdown-menu dropdown-menu-end"
                            >

                                <li>

                                    <button
                                        class="dropdown-item"
                                        @click="$emit('edit',item.id)"
                                    >

                                        <i
                                            class="bi bi-pencil-square me-2 text-primary"
                                        ></i>

                                        Chỉnh sửa

                                    </button>

                                </li>

                                <li>

                                    <button
                                        class="dropdown-item text-danger"
                                        @click="$emit('delete',item.id)"
                                    >

                                        <i
                                            class="bi bi-trash me-2"
                                        ></i>

                                        Xóa

                                    </button>

                                </li>

                            </ul>

                        </div>

                    </td>

                </tr>

                </tbody>

            </table>

        </div>

    </div>

</template>

<script setup lang="ts">

import type { Product } from "../types/Product";

defineProps<{

    products: Product[];

    loading: boolean;

}>();

defineEmits([

    "edit",

    "delete"

]);

</script>

<style scoped>

/* ===========================
   CARD
=========================== */

.card{
    border-radius:20px;
    overflow:hidden;
    transition:.3s;
}

.card:hover{

    box-shadow:0 8px 30px rgba(0,0,0,.08);

}

/* ===========================
   TABLE
=========================== */

.table{

    margin-bottom:0;

}

.table thead th{

    position:sticky;

    top:0;

    z-index:10;

    background:#fff;

    font-size:14px;

    font-weight:700;

    color:#495057;

    white-space:nowrap;

    border-bottom:2px solid #edf2f7;

    padding:18px 14px;

}

.table tbody td{

    padding:16px 14px;

    vertical-align:middle;

    border-color:#f3f3f3;

}

.table tbody tr{

    transition:all .25s ease;

}

.table tbody tr:hover{

    background:#f8fbff;

    transform:translateY(-2px);

    box-shadow:0 5px 20px rgba(0,0,0,.04);

}

/* ===========================
   IMAGE
=========================== */

.product-image{

    width:72px;

    height:72px;

    border-radius:18px;

    object-fit:cover;

    border:1px solid #ececec;

    transition:.3s;

    cursor:pointer;

    box-shadow:0 3px 12px rgba(0,0,0,.08);

}

.product-image:hover{

    transform:scale(1.12);

    box-shadow:0 10px 25px rgba(0,0,0,.15);

}

/* ===========================
   BADGE
=========================== */

.badge{

    padding:8px 14px;

    font-size:12px;

    font-weight:600;

    letter-spacing:.3px;

}

/* ===========================
   BUTTON
=========================== */

.btn{

    transition:.25s;

}

.btn:hover{

    transform:translateY(-2px);

}

.btn-light{

    border-radius:10px;

    border:1px solid #e8e8e8;

}

.dropdown-menu{

    border:none;

    border-radius:14px;

    box-shadow:0 10px 35px rgba(0,0,0,.15);

    overflow:hidden;

}

.dropdown-item{

    padding:12px 18px;

    transition:.2s;

}

.dropdown-item:hover{

    background:#f8f9fa;

}

/* ===========================
   EMPTY
=========================== */

.empty-state{

    display:flex;

    flex-direction:column;

    justify-content:center;

    align-items:center;

    padding:70px 20px;

}

.empty-state i{

    opacity:.3;

}

.empty-state h4{

    margin-top:20px;

    font-weight:700;

}

.empty-state p{

    color:#6c757d;

}

/* ===========================
   SKELETON
=========================== */

.skeleton{

    position:relative;

    overflow:hidden;

    background:#ececec;

    border-radius:8px;

}

.skeleton::after{

    content:"";

    position:absolute;

    inset:0;

    transform:translateX(-100%);

    background:linear-gradient(

        90deg,

        transparent,

        rgba(255,255,255,.7),

        transparent

    );

    animation:skeleton 1.2s infinite;

}

.skeleton.image{

    width:70px;

    height:70px;

    border-radius:16px;

}

.skeleton.title{

    width:220px;

    height:18px;

}

.skeleton.text{

    width:120px;

    height:14px;

}

.skeleton.badge{

    width:80px;

    height:28px;

    border-radius:50px;

}

.skeleton.button{

    width:40px;

    height:40px;

    border-radius:10px;

}

@keyframes skeleton{

    100%{

        transform:translateX(100%);

    }

}

/* ===========================
   SCROLLBAR
=========================== */

.table-responsive::-webkit-scrollbar{

    height:8px;

}

.table-responsive::-webkit-scrollbar-thumb{

    background:#ced4da;

    border-radius:20px;

}

/* ===========================
   RESPONSIVE
=========================== */

@media(max-width:1200px){

    .table{

        min-width:1100px;

    }

}

@media(max-width:768px){

    .product-image{

        width:60px;

        height:60px;

    }

    .badge{

        font-size:11px;

        padding:6px 10px;

    }

}

/* ===========================
   NAME
=========================== */

.fw-bold{

    color:#212529;

}

small{

    font-size:12px;

}

/* ===========================
   HOVER ICON
=========================== */

.bi{

    transition:.2s;

}

.dropdown-item:hover .bi{

    transform:scale(1.15);

}

</style>