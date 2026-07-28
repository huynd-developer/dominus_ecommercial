<template>
  <div class="card border-0 shadow-sm mb-3">
    <div class="card-body">
      <div class="row g-2 align-items-end">
        <div class="col-md-4">
          <label class="form-label fw-semibold">Tìm kiếm</label>
          <input
            v-model.trim="localKeyword"
            type="text"
            class="form-control"
            placeholder="Tên khách, SĐT, mã đơn..."
            @keyup.enter="emitSearch"
          />
        </div>

        <div class="col-md-3">
          <label class="form-label fw-semibold">Trạng thái</label>
          <select v-model="localStatus" class="form-select" @change="emitSearch">
            <option :value="null">Tất cả trạng thái</option>
            <option :value="0">Chờ xác nhận</option>
            <option :value="1">Đã xác nhận</option>
            <option :value="2">Đang giao hàng</option>
            <option :value="3">Hoàn thành</option>
            <option :value="4">Đã hủy</option>
            <option :value="5">Giao hàng thất bại</option>
            <option :value="6">Yêu cầu hoàn hàng</option>
            <option :value="7">Hoàn hàng hoàn tất</option>
          </select>
        </div>

        <div class="col-md-3">
          <label class="form-label fw-semibold">Loại đơn</label>
          <select v-model="localOrderType" class="form-select" @change="emitSearch">
            <option value="">Tất cả loại đơn</option>
            <option value="ONLINE">Online</option>
            <option value="IN_STORE">Tại quầy</option>
          </select>
        </div>

        <div class="col-md-2 d-flex gap-2">
          <button class="btn btn-primary w-100" @click="emitSearch">
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
}>();

const emit = defineEmits<{
  search: [
    payload: {
      keyword: string;
      status: number | null;
      orderType: string;
    }
  ];
}>();

const localKeyword = ref(props.keyword);
const localStatus = ref<number | null>(props.status);
const localOrderType = ref(props.orderType);

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

function emitSearch() {
  emit("search", {
    keyword: localKeyword.value,
    status: localStatus.value,
    orderType: localOrderType.value,
  });
}

function resetFilter() {
  localKeyword.value = "";
  localStatus.value = null;
  localOrderType.value = "";
  emitSearch();
}
</script>