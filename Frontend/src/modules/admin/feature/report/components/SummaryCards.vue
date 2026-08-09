<template>
  <div class="row g-3 mb-4">
    <!-- Card Tổng Doanh Thu -->
    <div class="col-12 col-md-4">
      <div class="report-card card-revenue">
        <div class="report-card-label">Tổng doanh thu</div>
        <div class="report-card-value text-primary-gradient">
          {{ formatMoney(summary?.totalRevenue || 0) }}
        </div>
<<<<<<< HEAD
        <div class="report-card-desc d-flex align-items-center gap-2 mt-2">
          <div class="online-stat">
            <i class="bi bi-globe"></i> Online: <span class="fw-bold">{{ formatMoney(summary?.onlineRevenue || 0) }}</span>
          </div>
          <span class="text-muted">|</span>
          <div class="offline-stat">
            <i class="bi bi-shop"></i> Tại quầy: <span class="fw-bold">{{ formatMoney(summary?.offlineRevenue || 0) }}</span>
=======

        <div class="split-stats">
          <div class="stat-row">
            <span class="stat-label">🌐 Đơn Online:</span>
            <span class="stat-number">{{ formatMoney(summary?.onlineRevenue || 0) }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">🏪 Tại quầy (POS):</span>
            <span class="stat-number">{{ formatMoney(summary?.offlineRevenue || 0) }}</span>
>>>>>>> 9d167f0ed4c026eca4c2ba188b0c8fa4199145de
          </div>
        </div>
      </div>
    </div>

    <!-- Card Số Đơn Hoàn Thành -->
    <div class="col-12 col-md-4">
      <div class="report-card card-orders">
        <div class="report-card-label">Số đơn hoàn thành</div>
        <div class="report-card-value">
          {{ formatNumber(summary?.totalOrders || 0) }} <span style="font-size: 16px; color: #6b7280; font-weight: 500;">đơn</span>
        </div>
<<<<<<< HEAD
        <div class="report-card-desc d-flex align-items-center gap-2 mt-2">
          <div class="online-stat">
            <i class="bi bi-globe"></i> Online: <span class="fw-bold">{{ formatNumber(summary?.onlineOrders || 0) }}</span>
          </div>
          <span class="text-muted">|</span>
          <div class="offline-stat">
            <i class="bi bi-shop"></i> Tại quầy: <span class="fw-bold">{{ formatNumber(summary?.offlineOrders || 0) }}</span>
=======

        <div class="split-stats">
          <div class="stat-row">
            <span class="stat-label">🌐 Đơn Online:</span>
            <span class="stat-number">{{ formatNumber(summary?.onlineOrders || 0) }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">🏪 Tại quầy (POS):</span>
            <span class="stat-number">{{ formatNumber(summary?.offlineOrders || 0) }}</span>
>>>>>>> 9d167f0ed4c026eca4c2ba188b0c8fa4199145de
          </div>
        </div>
      </div>
    </div>

    <!-- Card Sản Phẩm Đã Bán -->
    <div class="col-12 col-md-4">
      <div class="report-card card-products">
        <div class="report-card-label">Sản phẩm đã bán</div>
        <div class="report-card-value">
          {{ formatNumber(summary?.totalProductsSold || 0) }} <span style="font-size: 16px; color: #6b7280; font-weight: 500;">sản phẩm</span>
        </div>
<<<<<<< HEAD
        <div class="report-card-desc mt-2">
          Tổng số lượng sản phẩm trong đơn hoàn thành
=======
        <div class="report-card-desc mt-3" style="font-size: 13.5px; color: #6b7280;">
           Tổng số lượng các sản phẩm nằm trong những đơn hàng đã hoàn thành thành công.
>>>>>>> 9d167f0ed4c026eca4c2ba188b0c8fa4199145de
        </div>
      </div>
    </div>

    <!-- Khoảng thời gian báo cáo -->
    <div v-if="summary" class="col-12">
      <div class="range-box d-flex align-items-center">
        <span class="range-icon me-2" style="font-size: 18px;">📅</span>
        <span style="font-size: 14.5px; color: #64748b;">
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

/* Các dải màu viền trên của thẻ */
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

.text-primary-gradient {
  background: linear-gradient(90deg, #111827, #374151);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.report-card-desc {
  margin-top: auto;
}

/* Phân chia thông tin Online / Offline */
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

.online-stat {
  color: #059669;
}

.offline-stat {
  color: #2563eb;
}

.range-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 20px;
}
</style>