<template>
<form @submit.prevent="submit">

<div class="container-fluid py-4">

    <!-- Header -->

    <div class="d-flex justify-content-between align-items-center mb-4">

        <div>

            <h2 class="fw-bold mb-1">

                <i class="bi bi-box-seam-fill text-primary me-2"></i>

                {{ product ? "Cập nhật sản phẩm" : "Thêm sản phẩm" }}

            </h2>

            <p class="text-muted mb-0">

                Quản lý thông tin sản phẩm

            </p>

        </div>

        <button
    class="btn btn-outline-secondary px-4"
    type="button"
    @click="router.back()"
>
    <i class="bi bi-arrow-left me-2"></i>
    Quay lại
</button>

    </div>

    <div class="row">

        <!-- LEFT -->

        <div class="col-lg-8">

            <!-- =========================== -->
            <!-- THÔNG TIN -->
            <!-- =========================== -->

            <div class="card border-0 shadow-sm rounded-4 mb-4">

                <div class="card-header bg-white py-3">

                    <h5 class="fw-bold mb-0">

                        <i class="bi bi-info-circle text-primary me-2"></i>

                        Thông tin sản phẩm

                    </h5>

                </div>

                <div class="card-body p-4">

                    <div class="row">

                        <!-- Name -->

                        <div class="col-12 mb-4">

                            <label class="form-label fw-semibold">

                                Tên sản phẩm

                            </label>

                            <input

                                v-model="form.name"

                                class="form-control form-control-lg"

                                placeholder="Ví dụ: Dior Sauvage EDP"

                            >

                        </div>

                        <!-- Brand -->

                        <div class="col-md-6 mb-4">

                            <label class="form-label fw-semibold">

                                Brand

                            </label>

                            <select

                                v-model="form.brandId"

                                class="form-select form-select-lg"

                            >

                                <option

                                    v-for="item in brands"

                                    :key="item.id"

                                    :value="item.id"

                                >

                                    {{ item.name }}

                                </option>

                            </select>

                        </div>

                        <!-- Category -->

                        <div class="col-md-6 mb-4">

                            <label class="form-label fw-semibold">

                                Category

                            </label>

                            <select

                                v-model="form.categoryId"

                                class="form-select form-select-lg"

                            >

                                <option

                                    v-for="item in categories"

                                    :key="item.id"

                                    :value="item.id"

                                >

                                    {{ item.name }}

                                </option>

                            </select>

                        </div>

                        <!-- Concentration -->

                        <div class="col-md-6 mb-4">

                            <label class="form-label fw-semibold">

                                Concentration

                            </label>

                            <select

                                v-model="form.concentrationId"

                                class="form-select form-select-lg"

                            >

                                <option

                                    v-for="item in concentrations"

                                    :key="item.id"

                                    :value="item.id"

                                >

                                    {{ item.name }}

                                </option>

                            </select>

                        </div>

                        <!-- Gender -->

                        <div class="col-md-6 mb-4">

                            <label class="form-label fw-semibold">

                                Giới tính

                            </label>

                            <select

                                v-model="form.gender"

                                class="form-select form-select-lg"

                            >

                                <option :value="1">

                                     Nam

                                </option>

                                <option :value="2">

                                     Nữ

                                </option>

                            </select>

                        </div>

                        <!-- Description -->

                        <div class="col-12">

                            <label class="form-label fw-semibold">

                                Mô tả

                            </label>

                            <textarea

                                v-model="form.description"

                                class="form-control"

                                rows="7"

                                placeholder="Nhập mô tả sản phẩm..."

                            />

                        </div>

                    </div>

                </div>

            </div>
                        <!-- ========================= -->
            <!-- CẤU HÌNH -->
            <!-- ========================= -->

            <div class="card border-0 shadow-sm rounded-4 mb-4">

                <div class="card-header bg-white py-3">

                    <h5 class="fw-bold mb-0">

                        <!-- <i class="bi bi-sliders text-warning me-2"></i> -->

                        Cấu hình sản phẩm

                    </h5>

                </div>

                <div class="card-body">

                    <div class="row">

                        <div class="col-md-6 mb-4">

                            <label class="form-label fw-semibold">

                                Loại

                            </label>

                            <select
                                v-model="form.isNiche"
                                class="form-select form-select-lg"
                            >

                                <option :value="false">

                                    Designer

                                </option>

                                <option :value="true">

                                    Niche

                                </option>

                            </select>

                        </div>

                        <div class="col-md-6 mb-4">

                            <label class="form-label fw-semibold">

                                Trạng thái

                            </label>

                            <select
                                v-model="form.status"
                                class="form-select form-select-lg"
                            >

                                <option :value="1">

                                    Hoạt động

                                </option>

                                <option :value="0">

                                    Ngừng hoạt động

                                </option>

                            </select>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        <!-- RIGHT -->

        <div class="col-lg-4">

            <!-- ========================= -->
            <!-- ẢNH ĐẠI DIỆN -->
            <!-- ========================= -->

            <div class="card border-0 shadow-sm rounded-4 mb-4">

                <div class="card-header bg-white py-3">

                    <h5 class="fw-bold mb-0">

                        <i class="bi bi-image text-danger me-2"></i>

                        Ảnh đại diện

                    </h5>

                </div>

                <div class="card-body">

                    <input

                        type="file"

                        class="form-control"

                        accept="image/*"

                        @change="choosePrimary"

                    >

                    <div
                        v-if="primaryPreview"
                        class="mt-4 text-center"
                    >

                        <img

                            :src="primaryPreview"

                            class="img-fluid rounded-4 shadow-sm preview-image"

                        >

                    </div>

                    <div
                        v-else
                        class="upload-placeholder mt-4"
                    >

                        <i class="bi bi-cloud-upload fs-1 text-secondary"></i>

                        <p class="text-muted mt-3 mb-0">

                            Chưa chọn ảnh đại diện

                        </p>

                    </div>

                </div>

            </div>

            <!-- ========================= -->
            <!-- ẢNH CHI TIẾT -->
            <!-- ========================= -->

            <div class="card border-0 shadow-sm rounded-4">

                <div class="card-header bg-white py-3">

                    <div class="d-flex justify-content-between">

                        <h5 class="fw-bold mb-0">

                            <i class="bi bi-images text-success me-2"></i>

                            Thư viện ảnh

                        </h5>

                        <span class="badge bg-primary">

                            {{ imagePreviews.length }}

                        </span>

                    </div>

                </div>

                <div class="card-body">

                    <input

                        type="file"

                        class="form-control"

                        multiple

                        accept="image/*"

                        @change="chooseImages"

                    >

                    <div
                        class="row mt-4"
                        v-if="imagePreviews.length"
                    >

                        <div

                            class="col-6 mb-3"

                            v-for="(img,index) in imagePreviews"

                            :key="index"

                        >

                            <div class="gallery-item">

                                <img

                                    :src="img"

                                    class="img-fluid rounded-3"

                                >

                            </div>

                        </div>

                    </div>

                    <div
                        v-else
                        class="upload-placeholder mt-4"
                    >

                        <i class="bi bi-images fs-1 text-secondary"></i>

                        <p class="text-muted mt-3 mb-0">

                            Chưa có ảnh chi tiết

                        </p>

                    </div>

                </div>

            </div>

        </div>

    </div>

    <!-- Footer -->

    <div class="sticky-bottom bg-white py-3 mt-4 border-top">

        <div class="d-flex justify-content-end gap-3">

            <button

                class="btn btn-outline-secondary btn-lg px-4"

                type="reset"

            >

                <i class="bi bi-arrow-counterclockwise me-2"></i>

                Làm mới

            </button>

            <button

                class="btn btn-primary btn-lg px-5"

                type="submit"

            >

                <i class="bi bi-check-circle-fill me-2"></i>

                Lưu sản phẩm

            </button>

        </div>

    </div>

</div>

</form>
</template>
<script setup lang="ts">
import {
    reactive,
    ref,
    onMounted
} from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

/**
 * Props
 */
const props = defineProps<{
    product?: any
}>();

/**
 * Emit
 */
const emit = defineEmits([
    "submit"
]);

/* ===========================
   DỮ LIỆU GIẢ
=========================== */

const brands = [

    {
        id: 1,
        name: "Chanel"
    },

    {
        id: 2,
        name: "Dior"
    },

    {
        id: 3,
        name: "Gucci"
    },

    {
        id: 4,
        name: "Versace"
    },

    {
        id: 5,
        name: "Tom Ford"
    }

] as const;

const categories = [

    {
        id: 1,
        name: "Nước hoa Nam"
    },

    {
        id: 2,
        name: "Nước hoa Nữ"
    },

    {
        id: 3,
        name: "Unisex"
    }

] as const;

const concentrations = [

    {
        id: 1,
        name: "EDT"
    },

    {
        id: 2,
        name: "EDP"
    },

    {
        id: 3,
        name: "Parfum"
    },

    {
        id: 4,
        name: "Extrait"
    }

] as const;

/* ===========================
   FORM
=========================== */

const form = reactive({

    name: "",

    brandId: brands[0].id,

    categoryId: categories[0].id,

    concentrationId: concentrations[0].id,

    description: "",

    gender: 1,

    isNiche: false,

    status: 1

});

/* ===========================
   IMAGE
=========================== */

const primaryImage = ref<File | null>(null);

const images = ref<File[]>([]);

const primaryPreview = ref("");

const imagePreviews = ref<string[]>([]);

/* ===========================
   LOADING
=========================== */

const saving = ref(false);

/* ===========================
   LOAD UPDATE
=========================== */

onMounted(() => {

    if (!props.product) return;

    Object.assign(

        form,

        props.product

    );

    if (!props.product.images) return;

    const primary = props.product.images.find(

        (i: any) => i.isPrimary

    );

    if (primary) {

        primaryPreview.value =

            primary.imageUrl;

    }

    imagePreviews.value =

        props.product.images

            .filter(

                (i: any) => !i.isPrimary

            )

            .map(

                (i: any) => i.imageUrl

            );

});

/* ===========================
   VALIDATE
=========================== */

const validate = () => {

    if (!form.name.trim()) {

        alert(

            "Vui lòng nhập tên sản phẩm"

        );

        return false;

    }

    if (form.name.length < 3) {

        alert(

            "Tên quá ngắn"

        );

        return false;

    }

    return true;

};
/* ===========================
   ẢNH ĐẠI DIỆN
=========================== */

const choosePrimary = (e: Event) => {

    const target = e.target as HTMLInputElement;

    if (!target.files?.length) return;

    const file = target.files.item(0);

    if (!file) return;

    primaryImage.value = file;

    primaryPreview.value = URL.createObjectURL(file);

};

/* ===========================
   ẢNH CHI TIẾT
=========================== */

const chooseImages = (e: Event) => {

    const target = e.target as HTMLInputElement;

    if (!target.files?.length) return;

    images.value = Array.from(target.files);

    imagePreviews.value = images.value.map(file =>
        URL.createObjectURL(file)
    );

};

/* ===========================
   XÓA 1 ẢNH PREVIEW
=========================== */

const removeImage = (index: number) => {

    images.value.splice(index, 1);

    imagePreviews.value.splice(index, 1);

};

/* ===========================
   RESET FORM
=========================== */

const resetForm = () => {

    form.name = "";

    form.brandId = brands[0].id;

    form.categoryId = categories[0].id;

    form.concentrationId = concentrations[0].id;

    form.description = "";

    form.gender = 1;

    form.isNiche = false;

    form.status = 1;

    primaryImage.value = null;

    images.value = [];

    primaryPreview.value = "";

    imagePreviews.value = [];

};

/* ===========================
   SUBMIT
=========================== */

const submit = async () => {

    if (!validate()) return;

    saving.value = true;

    try {

        const fd = new FormData();

        fd.append("name", form.name);

        fd.append(
            "brandId",
            String(form.brandId)
        );

        fd.append(
            "categoryId",
            String(form.categoryId)
        );

        fd.append(
            "concentrationId",
            String(form.concentrationId)
        );

        fd.append(
            "description",
            form.description
        );

        fd.append(
            "gender",
            String(form.gender)
        );

        fd.append(
            "isNiche",
            String(form.isNiche)
        );

        fd.append(
            "status",
            String(form.status)
        );

        if (primaryImage.value) {

            fd.append(
                "primaryImage",
                primaryImage.value
            );

        }

        images.value.forEach(file => {

            fd.append(
                "images",
                file
            );

        });

        emit(
            "submit",
            fd
        );

    } finally {

        saving.value = false;

    }

};

</script>
<style scoped>

/* ==========================
   PAGE
========================== */

.container-fluid{

    max-width:1600px;

}

/* ==========================
   CARD
========================== */

.card{

    border:none;

    border-radius:18px;

    overflow:hidden;

    transition:.3s;

}

.card:hover{

    transform:translateY(-2px);

    box-shadow:0 12px 35px rgba(0,0,0,.08);

}

.card-header{

    background:#fff;

    border-bottom:1px solid #f1f1f1;

    padding:18px 24px;

}

.card-body{

    padding:24px;

}

/* ==========================
   TITLE
========================== */

h2{

    font-weight:700;

}

h5{

    font-weight:700;

}

/* ==========================
   FORM
========================== */

.form-label{

    font-weight:600;

    color:#495057;

}

.form-control,

.form-select{

    border-radius:12px;

    min-height:48px;

    transition:.25s;

}

.form-control:focus,

.form-select:focus{

    border-color:#4f8ef7;

    box-shadow:0 0 0 .2rem rgba(79,142,247,.15);

}

textarea{

    min-height:180px;

    resize:vertical;

}

/* ==========================
   BUTTON
========================== */

.btn{

    border-radius:12px;

    transition:.25s;

}

.btn:hover{

    transform:translateY(-2px);

}

.btn-primary{

    padding-left:28px;

    padding-right:28px;

}

/* ==========================
   PREVIEW
========================== */

.preview-image{

    width:100%;

    max-height:260px;

    object-fit:cover;

    border-radius:18px;

    transition:.3s;

    cursor:pointer;

}

.preview-image:hover{

    transform:scale(1.03);

}

/* ==========================
   GALLERY
========================== */

.gallery-item{

    position:relative;

    overflow:hidden;

    border-radius:14px;

    border:1px solid #ececec;

    background:#fff;

}

.gallery-item img{

    width:100%;

    height:130px;

    object-fit:cover;

    transition:.3s;

}

.gallery-item:hover img{

    transform:scale(1.08);

}

/* ==========================
   REMOVE BUTTON
========================== */

.remove-btn{

    position:absolute;

    top:8px;

    right:8px;

    width:30px;

    height:30px;

    border-radius:50%;

    display:flex;

    align-items:center;

    justify-content:center;

    opacity:0;

    transition:.2s;

}

.gallery-item:hover .remove-btn{

    opacity:1;

}

/* ==========================
   EMPTY UPLOAD
========================== */

.upload-placeholder{

    border:2px dashed #d9d9d9;

    border-radius:16px;

    padding:45px 20px;

    text-align:center;

    transition:.25s;

    background:#fafafa;

}

.upload-placeholder:hover{

    border-color:#0d6efd;

    background:#f8fbff;

}

/* ==========================
   FOOTER
========================== */

.sticky-bottom{

    position:sticky;

    bottom:0;

    z-index:20;

    background:#fff;

    border-top:1px solid #ececec;

}

/* ==========================
   ICON
========================== */

.bi{

    transition:.2s;

}

.card-header i{

    font-size:18px;

}

/* ==========================
   SCROLL
========================== */

::-webkit-scrollbar{

    width:8px;

}

::-webkit-scrollbar-thumb{

    background:#d2d2d2;

    border-radius:20px;

}

/* ==========================
   RESPONSIVE
========================== */

@media(max-width:992px){

    .sticky-bottom{

        position:static;

    }

    .preview-image{

        max-height:220px;

    }

}

@media(max-width:768px){

    .card-body{

        padding:18px;

    }

    .btn{

        width:100%;

    }

    .sticky-bottom .d-flex{

        flex-direction:column;

    }

}

</style>