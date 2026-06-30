<template>
  <div class="row">

    <!-- ẢNH ĐẠI DIỆN -->
    <div class="col-lg-4">

      <label class="form-label fw-bold mb-3">
        Ảnh đại diện
      </label>

      <div
          class="upload-box"
          @click="primaryInput?.click()"
      >

        <template v-if="!primaryPreview">

          <i class="bi bi-cloud-arrow-up upload-icon"></i>

          <h6 class="mt-3">
            Chọn ảnh đại diện
          </h6>

          <small class="text-muted">
            PNG, JPG, WEBP
          </small>

        </template>

        <img
            v-else
            :src="primaryPreview"
            class="preview-large"
        >

      </div>

      <input
          ref="primaryInput"
          hidden
          type="file"
          accept="image/*"
          @change="changePrimary"
      >

    </div>

    <!-- ẢNH PHỤ -->

    <div class="col-lg-8">

      <label class="form-label fw-bold mb-3">

        Ảnh chi tiết

      </label>

      <div
          class="upload-box small"
          @click="imagesInput?.click()"
      >

        <i class="bi bi-images fs-1"></i>

        <div class="mt-2">

          Chọn nhiều ảnh

        </div>

      </div>

      <input
          ref="imagesInput"
          hidden
          type="file"
          multiple
          accept="image/*"
          @change="changeImages"
      >

      <div class="row mt-4">

        <div
            class="col-md-3 mb-3"
            v-for="(img,index) in imagePreviews"
            :key="index"
        >

          <div class="preview-card">

            <img
                :src="img"
                class="preview-small"
            >

            <button
                class="btn btn-danger btn-sm remove-btn"
                @click="remove(index)"
            >
              <i class="bi bi-x"></i>
            </button>

          </div>

        </div>

      </div>

    </div>

  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const emit = defineEmits(["primary","images"]);

const primaryInput = ref<HTMLInputElement>();

const imagesInput = ref<HTMLInputElement>();

const primaryPreview = ref("");

const imagePreviews = ref<string[]>([]);

const imageFiles = ref<File[]>([]);

const changePrimary = (e:any)=>{

    const file = e.target.files[0];

    if(!file) return;

    primaryPreview.value = URL.createObjectURL(file);

    emit("primary",file);

}

const changeImages=(e:any)=>{

    imageFiles.value = [...e.target.files];

    imagePreviews.value = imageFiles.value.map(f=>URL.createObjectURL(f));

    emit("images",imageFiles.value);

}

const remove=(index:number)=>{

    imageFiles.value.splice(index,1);

    imagePreviews.value.splice(index,1);

    emit("images",imageFiles.value);

}
</script>

<style scoped>

.upload-box{

    height:260px;

    border:2px dashed #d6d6d6;

    border-radius:20px;

    display:flex;

    flex-direction:column;

    justify-content:center;

    align-items:center;

    cursor:pointer;

    transition:.3s;

    background:#fafafa;

}

.upload-box:hover{

    border-color:#0d6efd;

    background:#f3f8ff;

}

.small{

    height:130px;

}

.upload-icon{

    font-size:55px;

    color:#0d6efd;

}

.preview-large{

    width:100%;

    height:100%;

    object-fit:cover;

    border-radius:18px;

}

.preview-card{

    position:relative;

}

.preview-small{

    width:100%;

    height:140px;

    object-fit:cover;

    border-radius:15px;

}

.remove-btn{

    position:absolute;

    top:8px;

    right:8px;

    border-radius:50%;

    width:30px;

    height:30px;

    padding:0;

}

</style>