<template>
  <div class="row g-3 mb-4">
    <div class="col-12 col-md-4">
      <div class="report-card">
        <div class="report-card-label">Tổng doanh thu</div>
        <div class="report-card-value">
          {{ formatMoney(summary?.totalRevenue || 0) }}
        </div>
        <div class="report-card-desc">
          Chỉ tính đơn hoàn thành Status = 3
        </div>
      </div>
    </div>

    <div class="col-12 col-md-4">
      <div class="report-card">
        <div class="report-card-label">Số đơn đã bán</div>
        <div class="report-card-value">
          {{ summary?.totalOrders || 0 }}
        </div>
        <div class="report-card-desc">
          Tổng số đơn hoàn thành
        </div>
      </div>
    </div>

    <div class="col-12 col-md-4">
      <div class="report-card">
        <div class="report-card-label">Sản phẩm đã bán</div>
        <div class="report-card-value">
          {{ summary?.totalProductsSold || 0 }}
        </div>
        <div class="report-card-desc">
          Tổng quantity trong OrderItem
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ReportSummaryResponse } from "../types/report.type";

defineProps<{
  summary: ReportSummaryResponse | null;
}>();

const formatMoney = (value: number) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};
</script>

<style scoped>
.report-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 22px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  border: 1px solid #f1f1f1;
}

.report-card-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 10px;
}

.report-card-value {
  font-size: 28px;
  font-weight: 800;
  color: #111827;
}

.report-card-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 8px;
}
</style>