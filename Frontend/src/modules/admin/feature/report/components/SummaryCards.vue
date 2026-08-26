<template>
  <div class="row g-3 mb-4">
    <div class="col-12 col-md-4">
      <div class="report-card card-revenue">
        <div class="report-card-label">Doanh thu thuần</div>

        <div class="report-card-value text-primary-gradient">
          {{ formatMoney(summary?.totalRevenue || 0) }}
        </div>

        <div class="report-card-desc revenue-desc">
          Doanh thu bán sau khi trừ các khoản hoàn tiền sản phẩm đã hoàn tất trong kỳ.
        </div>

        <div class="split-stats">
          <div class="stat-row">
            <span class="stat-label">🌐 Online:</span>
            <span class="stat-number">
              {{ formatMoney(summary?.onlineRevenue || 0) }}
            </span>
          </div>

          <div class="stat-row">
            <span class="stat-label">🏪 Tại quầy (POS):</span>
            <span class="stat-number">
              {{ formatMoney(summary?.offlineRevenue || 0) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="col-12 col-md-4">
      <div class="report-card card-orders">
        <div class="report-card-label">Số đơn hoàn thành</div>

        <div class="report-card-value">
          {{ formatNumber(summary?.totalOrders || 0) }}
          <span class="value-unit">đơn</span>
        </div>

        <div class="split-stats">
          <div class="stat-row">
            <span class="stat-label">🌐 Đơn Online:</span>
            <span class="stat-number">
              {{ formatNumber(summary?.onlineOrders || 0) }}
            </span>
          </div>

          <div class="stat-row">
            <span class="stat-label">🏪 Tại quầy (POS):</span>
            <span class="stat-number">
              {{ formatNumber(summary?.offlineOrders || 0) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="col-12 col-md-4">
      <div class="report-card card-products">
        <div class="report-card-label">Sản phẩm đã bán</div>

        <div class="report-card-value">
          {{ formatNumber(summary?.totalProductsSold || 0) }}
          <span class="value-unit">sản phẩm</span>
        </div>

        <div class="report-card-desc mt-3">
          Tổng số lượng sản phẩm thuộc các giao dịch bán đã hoàn thành trong kỳ;
          hàng trả sau đó không làm giảm chỉ số này.
        </div>
      </div>
    </div>

    <div v-if="summary" class="col-12">
      <div class="range-box d-flex align-items-center">
        <span class="range-icon me-2">📅</span>

        <span class="range-text">
          Dữ liệu báo cáo được tính từ
          <strong class="text-dark">{{ formatDate(summary.fromDate) }}</strong>
          đến
          <strong class="text-dark">{{ formatDate(summary.toDate) }}</strong>
        </span>
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
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid #f3f4f6;
  height: 100%;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.report-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.06);
}

.report-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
}

.card-revenue::before {
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
}

.card-orders::before {
  background: linear-gradient(90deg, #10b981, #3b82f6);
}

.card-products::before {
  background: linear-gradient(90deg, #f59e0b, #ef4444);
}

.report-card-label {
  font-size: 14.5px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.report-card-value {
  font-size: 32px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
}

.value-unit {
  font-size: 16px;
  color: #6b7280;
  font-weight: 500;
}

.text-primary-gradient {
  background: linear-gradient(90deg, #111827, #374151);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.report-card-desc {
  color: #6b7280;
  font-size: 13.5px;
}

.revenue-desc {
  margin-top: 2px;
  margin-bottom: 4px;
}

.split-stats {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px dashed #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 13.5px;
  color: #6b7280;
}

.stat-number {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  background: #f3f4f6;
  padding: 4px 10px;
  border-radius: 6px;
}

.range-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 20px;
}

.range-icon {
  font-size: 18px;
}

.range-text {
  font-size: 14.5px;
  color: #64748b;
}
</style>
