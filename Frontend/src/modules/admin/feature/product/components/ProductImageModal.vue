<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/common/store/app.store'

import { productService } from '../services/productService'
import type { Product } from '../types/product.type'

import ImageUploader from '@/common/components/ImageUploader.vue'

const props = defineProps<{
  productSelected: Product | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'refresh'): void
}>()

const appStore = useAppStore()

const selectedFile = ref<File | null>(null)
const isUploading = ref(false)

const closeImageModal = () => {
  selectedFile.value = null
  emit('close')
}

const uploadImage = async () => {
  if (!selectedFile.value) {
    appStore.notifyError('Vui lòng chọn ảnh')
    return
  }

  if (!props.productSelected?.id) {
    appStore.notifyError('Không tìm thấy sản phẩm')
    return
  }

  try {
    isUploading.value = true
    appStore.startLoading()

    await productService.uploadImage(
      props.productSelected.id,
      selectedFile.value
    )

    appStore.notifySuccess('Upload ảnh thành công')

    emit('refresh')
    closeImageModal()
  } catch (error: any) {
    appStore.notifyError(
      error?.response?.data?.message ||
      'Không thể upload ảnh'
    )
  } finally {
    isUploading.value = false
    appStore.stopLoading()
  }
}
</script>

<template>
  <div>
    <div
      class="offcanvas-backdrop fade show"
      @click="closeImageModal"
    ></div>

    <div
      class="offcanvas offcanvas-end show shadow-lg border-0"
      tabindex="-1"
      style="visibility: visible; width: 450px"
    >
      <div
        class="offcanvas-header border-bottom px-4 py-3 bg-light"
      >
        <h5 class="offcanvas-title">
          Upload ảnh sản phẩm
        </h5>

        <button
          type="button"
          class="btn-close"
          @click="closeImageModal"
        ></button>
      </div>

      <div class="offcanvas-body d-flex flex-column">
        <div class="mb-3">
          <label class="form-label fw-semibold">
            Sản phẩm
          </label>

          <div class="text-muted">
            {{ productSelected?.name }}
          </div>
        </div>

        <div class="mb-4">
          <ImageUploader
            ratio="1/1"
            @update:file="(file: File | null) => selectedFile = file"
          />
        </div>

        <div class="mt-auto d-flex gap-2">
          <button
            type="button"
            class="btn btn-outline-secondary flex-fill"
            @click="closeImageModal"
          >
            Hủy
          </button>

          <button
            type="button"
            class="btn btn-primary flex-fill"
            :disabled="!selectedFile || isUploading"
            @click="uploadImage"
          >
            <span
              v-if="isUploading"
              class="spinner-border spinner-border-sm me-2"
            ></span>

            Upload ảnh
          </button>
        </div>
      </div>
    </div>
  </div>
</template>