<template>
  <Teleport to="body">
    <div v-if="modelValue" class="review-backdrop">
      <div class="review-modal">
        <div class="review-header">
          <div>
            <h5 class="mb-1">Đánh giá sản phẩm</h5>
            <p class="mb-0 text-muted small">
              Chấm sao và chia sẻ cảm nhận của bạn
            </p>
          </div>

          <button
            type="button"
            class="btn-close"
            :disabled="loading"
            @click="close"
          ></button>
        </div>

        <div class="review-product">
          <img
            :src="item?.image || fallbackImage"
            alt="product"
            class="review-product-img"
          />

          <div class="flex-grow-1">
            <div class="fw-semibold">
              {{ item?.productName || "Sản phẩm" }}
            </div>

            <div class="text-muted small">
              {{ item?.brandName || "Không rõ thương hiệu" }}
            </div>

            <div v-if="item?.sku" class="text-muted small">
              SKU: {{ item.sku }}
            </div>
          </div>
        </div>

        <div class="review-body">
          <label class="form-label fw-semibold">Số sao đánh giá</label>

          <div class="star-row mb-3">
            <button
              v-for="star in 5"
              :key="star"
              type="button"
              class="star-btn"
              :class="{ active: star <= rating }"
              :disabled="loading"
              @click="rating = star"
            >
              <i
                class="bi"
                :class="star <= rating ? 'bi-star-fill' : 'bi-star'"
              ></i>
            </button>

            <span class="ms-2 text-muted small">
              {{ rating }}/5 sao
            </span>
          </div>

          <label class="form-label fw-semibold">Bình luận</label>

          <textarea
            v-model="comment"
            class="form-control review-textarea"
            rows="5"
            maxlength="1000"
            :disabled="loading"
            placeholder="Ví dụ: Mùi thơm, đóng gói cẩn thận, rất hài lòng..."
          ></textarea>

          <div class="text-end text-muted small mt-1">
            {{ comment.length }}/1000
          </div>

          <div v-if="errorMessage" class="alert alert-danger py-2 mt-3 mb-0">
            {{ errorMessage }}
          </div>
        </div>

        <div class="review-footer">
          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="loading"
            @click="close"
          >
            Hủy
          </button>

          <button
            type="button"
            class="btn btn-review-submit"
            :disabled="loading"
            @click="submit"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-2"
            ></span>
            Gửi đánh giá
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type { ReviewableOrderItemResponse } from "../types/profile.type";

const props = defineProps<{
  modelValue: boolean;
  item: ReviewableOrderItemResponse | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "submit", payload: { rating: number; comment: string | null }): void;
}>();

const rating = ref(5);
const comment = ref("");
const errorMessage = ref("");

const fallbackImage =
  "https://dummyimage.com/120x120/f4f4f4/999999&text=Perfume";

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      rating.value = 5;
      comment.value = "";
      errorMessage.value = "";
    }
  }
);

const close = () => {
  if (props.loading) return;
  emit("update:modelValue", false);
};

const submit = () => {
  errorMessage.value = "";

  if (!props.item) {
    errorMessage.value = "Không tìm thấy sản phẩm cần đánh giá";
    return;
  }

  if (rating.value < 1 || rating.value > 5) {
    errorMessage.value = "Số sao đánh giá phải từ 1 đến 5";
    return;
  }

  const normalizedComment = comment.value.trim();

  if (normalizedComment.length > 1000) {
    errorMessage.value = "Bình luận tối đa 1000 ký tự";
    return;
  }

  emit("submit", {
    rating: rating.value,
    comment: normalizedComment.length > 0 ? normalizedComment : null,
  });
};
</script>

<style scoped>
.review-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.review-modal {
  width: min(560px, 100%);
  background: #ffffff;
  border-radius: 22px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
  overflow: hidden;
}

.review-header,
.review-footer {
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-header {
  border-bottom: 1px solid #eeeeee;
}

.review-footer {
  border-top: 1px solid #eeeeee;
  gap: 12px;
}

.review-product {
  padding: 18px 22px;
  display: flex;
  gap: 14px;
  align-items: center;
  background: #faf8f4;
}

.review-product-img {
  width: 74px;
  height: 74px;
  border-radius: 16px;
  object-fit: cover;
  background: #f1f1f1;
  border: 1px solid #eeeeee;
}

.review-body {
  padding: 22px;
}

.star-row {
  display: flex;
  align-items: center;
}

.star-btn {
  border: none;
  background: transparent;
  color: #c9c9c9;
  font-size: 30px;
  padding: 0 4px;
  transition: 0.15s ease;
}

.star-btn.active {
  color: #bd9a5f;
}

.star-btn:hover:not(:disabled) {
  transform: translateY(-1px) scale(1.05);
}

.review-textarea {
  resize: none;
  border-radius: 14px;
}

.btn-review-submit {
  background: #1f2a44;
  color: #ffffff;
  border-radius: 999px;
  padding: 9px 18px;
}

.btn-review-submit:hover {
  background: #bd9a5f;
  color: #ffffff;
}
</style>