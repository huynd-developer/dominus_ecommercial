<template>
  <div class="row g-3 mb-3">
    <div class="col-md-6">
      <input
        v-model.trim="keywordModel"
        type="text"
        class="form-control"
        placeholder="Tìm theo tên, email, SĐT, mã NV, CCCD, chức vụ..."
        @keyup.enter="$emit('search')"
      />
    </div>

    <div class="col-md-3">
      <select v-model="statusModel" class="form-select" @change="$emit('search')">
        <option :value="null">Tất cả trạng thái</option>
        <option :value="1">Đang hoạt động</option>
        <option :value="0">Đã khóa</option>
      </select>
    </div>

    <div class="col-md-3 d-flex gap-2">
      <button class="btn btn-outline-primary w-100" @click="$emit('search')">
        Tìm kiếm
      </button>

      <button class="btn btn-outline-secondary" @click="$emit('reset')">
        Reset
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { EmployeeStatus } from "../types/employee.type";

const props = defineProps<{
  keyword: string;
  status: EmployeeStatus | null;
}>();

const emit = defineEmits<{
  (e: "update:keyword", value: string): void;
  (e: "update:status", value: EmployeeStatus | null): void;
  (e: "search"): void;
  (e: "reset"): void;
}>();

const keywordModel = computed({
  get: () => props.keyword,
  set: (value: string) => emit("update:keyword", value),
});

const statusModel = computed({
  get: () => props.status,
  set: (value: EmployeeStatus | null) => emit("update:status", value),
});
</script>