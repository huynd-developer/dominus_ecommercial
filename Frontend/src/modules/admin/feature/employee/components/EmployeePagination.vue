<template>
  <div class="d-flex justify-content-between align-items-center mt-3">
    <small class="text-muted">
      Tổng: {{ safeTotalElements }} nhân viên
    </small>

    <div class="d-flex align-items-center gap-2">
      <button
        class="btn btn-sm btn-outline-secondary"
        :disabled="safePage <= 0"
        @click="$emit('change-page', safePage - 1)"
      >
        Trước
      </button>

      <span class="small">
        Trang {{ safePage + 1 }} / {{ safeTotalPages }}
      </span>

      <button
        class="btn btn-sm btn-outline-secondary"
        :disabled="safePage + 1 >= safeTotalPages"
        @click="$emit('change-page', safePage + 1)"
      >
        Sau
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    page?: number;
    totalPages?: number;
    totalElements?: number;
  }>(),
  {
    page: 0,
    totalPages: 1,
    totalElements: 0,
  }
);

defineEmits<{
  (e: "change-page", page: number): void;
}>();

const safePage = computed(() => Number(props.page) || 0);
const safeTotalPages = computed(() => Math.max(Number(props.totalPages) || 1, 1));
const safeTotalElements = computed(() => Number(props.totalElements) || 0);
</script>