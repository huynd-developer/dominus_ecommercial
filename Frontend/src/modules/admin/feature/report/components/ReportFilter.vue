<template>
  <div class="card border-0 shadow-sm mb-4">
    <div class="card-body">
      <div class="row g-3 align-items-end">
        <div class="col-12 col-md-3">
          <label class="form-label fw-semibold">Mốc thời gian</label>
          <select
            v-model="reportStore.filter.filterType"
            class="form-select"
            :disabled="reportStore.loading"
            @change="handleFilterTypeChange"
          >
            <option value="DAY">Hôm nay</option>
            <option value="WEEK">Tuần này</option>
            <option value="MONTH">Tháng này</option>
            <option value="YEAR">Năm nay</option>
            <option value="CUSTOM">Tùy chọn ngày</option>
          </select>
        </div>

        <div
          v-if="reportStore.filter.filterType === 'CUSTOM'"
          class="col-12 col-md-3"
        >
          <label class="form-label fw-semibold">Từ ngày</label>
          <input
            v-model="reportStore.filter.fromDate"
            type="date"
            class="form-control"
            :max="todayText"
            :disabled="reportStore.loading"
          />
        </div>

        <div
          v-if="reportStore.filter.filterType === 'CUSTOM'"
          class="col-12 col-md-3"
        >
          <label class="form-label fw-semibold">Đến ngày</label>
          <input
            v-model="reportStore.filter.toDate"
            type="date"
            class="form-control"
            :min="reportStore.filter.fromDate || undefined"
            :max="todayText"
            :disabled="reportStore.loading"
          />
        </div>

        <div class="col-12 col-md-2">
          <label class="form-label fw-semibold">Top sản phẩm</label>
          <input
            v-model="reportStore.filter.limit"
            type="text"
            inputmode="numeric"
            maxlength="2"
            class="form-control"
            placeholder="10"
            :disabled="reportStore.loading"
            @input="handleLimitInput"
            @blur="handleLimitBlur"
            @keyup.enter="reportStore.fetchAll()"
          />
        </div>

        <div class="col-12 col-md-1 d-grid">
          <button
            class="btn btn-dark"
            :disabled="reportStore.loading"
            @click="reportStore.fetchAll()"
          >
            <span
              v-if="reportStore.loading"
              class="spinner-border spinner-border-sm"
            ></span>
            <span v-else>Lọc</span>
          </button>
        </div>
      </div>

      <div class="form-text mt-3">
        Doanh thu chỉ ghi nhận từ các đơn đã hoàn thành.
      </div>

      <div v-if="reportStore.error" class="alert alert-danger mt-3 mb-0">
        {{ reportStore.error }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useOwnerReportStore } from "../stores/ownerReport.store";

const reportStore = useOwnerReportStore();

const todayText = computed(() => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
});

const handleFilterTypeChange = () => {
  reportStore.resetCustomDateIfNeeded();
};

const handleLimitInput = () => {
  reportStore.filter.limit = String(reportStore.filter.limit || "")
    .replace(/[^\d]/g, "")
    .slice(0, 2);
};

const handleLimitBlur = () => {
  if (!reportStore.filter.limit) {
    reportStore.filter.limit = "10";
  }
};
</script>