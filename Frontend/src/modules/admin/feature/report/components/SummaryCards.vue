<template>
  <div class="row g-3 mb-4">
    <div class="col-12 col-md-4">
      <div class="report-card">
        <div class="report-card-label">Tổng doanh thu</div>
        <div class="report-card-value">
          {{ formatMoney(summary?.totalRevenue || 0) }}
        </div>
        <div class="report-card-desc">
          Doanh thu từ đơn đã hoàn thành
        </div>
      </div>
    </div>

    <div class="col-12 col-md-4">
      <div class="report-card">
        <div class="report-card-label">Số đơn hoàn thành</div>
        <div class="report-card-value">
          {{ formatNumber(summary?.totalOrders || 0) }}
        </div>
        <div class="report-card-desc">
          Không tính đơn chờ xác nhận, đang giao hoặc đã hủy
        </div>
      </div>
    </div>

    <div class="col-12 col-md-4">
      <div class="report-card">
        <div class="report-card-label">Sản phẩm đã bán</div>
        <div class="report-card-value">
          {{ formatNumber(summary?.totalProductsSold || 0) }}
        </div>
        <div class="report-card-desc">
          Tổng số lượng sản phẩm trong đơn hoàn thành
        </div>
      </div>
    </div>

    <div v-if="summary" class="col-12">
      <div class="range-box">
        Khoảng báo cáo:
        <strong>{{ formatDate(summary.fromDate) }}</strong>
        <span> đến </span>
        <strong>{{ formatDate(summary.toDate) }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ReportSummaryResponse } from "../types/report.type";

defineProps<{
  summary: ReportSummaryResponse | null;
}>();

const toNumber = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const formatMoney = (value: unknown) => {
  return toNumber(value).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  });
};

const formatNumber = (value: unknown) => {
  return toNumber(value).toLocaleString("vi-VN");
};

const formatDate = (value?: string | null) => {
  if (!value) return "-";

  const parts = value.split("-");

  if (parts.length !== 3) {
    return value;
  }

  const [year, month, day] = parts;

  return `${day}/${month}/${year}`;
};
</script>

<style scoped>
.report-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 22px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  border: 1px solid #f1f1f1;
  height: 100%;
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

.range-box {
  background: #fff;
  border: 1px solid #f1f1f1;
  border-radius: 14px;
  padding: 14px 18px;
  color: #6b7280;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.range-box strong {
  color: #111827;
}
</style>