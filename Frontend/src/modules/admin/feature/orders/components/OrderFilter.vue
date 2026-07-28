<template>
  <div class="card border-0 shadow-sm mb-3 order-search-card">
    <div class="card-body">
      <div class="row g-2 align-items-end">
        <div class="col-lg-6 col-md-12">
          <label class="form-label fw-semibold">Tìm kiếm</label>
          <input
            v-model.trim="localKeyword"
            type="text"
            class="form-control"
            placeholder="Mã đơn, tên khách, SĐT, SKU, sản phẩm..."
            @keyup.enter="emitSearch"
          />
        </div>

        <div class="col-lg-2 col-md-4">
          <label class="form-label fw-semibold">Từ ngày</label>
          <input
            v-model="localFromDate"
            type="date"
            class="form-control"
          />
        </div>

        <div class="col-lg-2 col-md-4">
          <label class="form-label fw-semibold">Đến ngày</label>
          <input
            v-model="localToDate"
            type="date"
            class="form-control"
          />
        </div>

        <div class="col-lg-2 col-md-4 d-flex gap-2">
          <button class="btn btn-primary flex-fill" @click="emitSearch">
            Tìm
          </button>

          <button class="btn btn-outline-secondary" @click="resetFilter">
            Xóa
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";

const props = defineProps<{
  keyword: string;
  status: number | null;
  orderType: string;
  fromDate: string;
  toDate: string;
}>();

const emit = defineEmits<{
  search: [
    payload: {
      keyword: string;
      status: number | null;
      orderType: string;
      fromDate: string;
      toDate: string;
    }
  ];
}>();

const localKeyword = ref(props.keyword);
const localStatus = ref<number | null>(props.status);
const localOrderType = ref(props.orderType);
const localFromDate = ref(props.fromDate);
const localToDate = ref(props.toDate);

watch(
  () => props.keyword,
  (value) => {
    localKeyword.value = value;
  }
);

watch(
  () => props.status,
  (value) => {
    localStatus.value = value;
  }
);

watch(
  () => props.orderType,
  (value) => {
    localOrderType.value = value;
  }
);

watch(
  () => props.fromDate,
  (value) => {
    localFromDate.value = value;
  }
);

watch(
  () => props.toDate,
  (value) => {
    localToDate.value = value;
  }
);

function emitSearch() {
  emit("search", {
    keyword: localKeyword.value,
    status: localStatus.value,
    orderType: localOrderType.value,
    fromDate: localFromDate.value,
    toDate: localToDate.value,
  });
}

function resetFilter() {
  localKeyword.value = "";
  localStatus.value = null;
  localOrderType.value = "";
  localFromDate.value = "";
  localToDate.value = "";

  emitSearch();
}
</script>

<style scoped>
.order-search-card {
  border-radius: 14px;
}

.form-label {
  color: #111827;
  font-size: 13px;
}

.form-control {
  min-height: 40px;
  border-radius: 10px;
}

.btn {
  min-height: 40px;
  border-radius: 10px;
  font-weight: 700;
}
</style>