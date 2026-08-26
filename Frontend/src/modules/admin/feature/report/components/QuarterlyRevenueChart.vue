<template>
  <div class="card border-0 shadow-sm mb-4 quarterly-card">
    <div class="card-body">
      <div class="chart-header">
        <div>
          <h5 class="fw-bold mb-1">Biểu đồ doanh thu thuần theo quý</h5>
          <p class="text-muted mb-0">
            Doanh thu bán trừ các khoản hoàn tiền sản phẩm đã hoàn tất trong từng quý
          </p>
        </div>

        <div class="chart-summary">
          <div class="summary-item">
            <span>Tổng doanh thu thuần</span>
            <strong>{{ formatCurrency(totalRevenue) }}</strong>
          </div>

          <div class="summary-item">
            <span>Quý cao nhất</span>
            <strong>{{ bestQuarterLabel }}</strong>
          </div>
        </div>
      </div>

      <div v-if="chartItems.length === 0" class="empty-box">
        Chưa có dữ liệu doanh thu theo quý.
      </div>

      <div v-else class="chart-wrapper">
        <div class="y-axis">
          <span v-for="label in yAxisLabels" :key="label">
            {{ label }}
          </span>
        </div>

        <div class="plot-area">
          <div class="grid-line line-100"></div>
          <div class="grid-line line-75"></div>
          <div class="grid-line line-50"></div>
          <div class="grid-line line-25"></div>
          <div class="grid-line line-0"></div>

          <div class="bars">
            <div
              v-for="item in chartItems"
              :key="item.label"
              class="bar-item"
            >
              <div class="bar-tooltip">
                <strong>{{ item.label }}</strong>
                <span>{{ formatCurrency(item.revenue) }}</span>
                <small>{{ item.totalOrders }} đơn hoàn thành</small>
              </div>

              <div class="bar-track">
                <div
                  class="bar-fill"
                  :class="{ 'is-empty': item.revenue <= 0 }"
                  :style="{ height: `${item.heightPercent}%` }"
                ></div>
              </div>

              <div class="bar-label">
                {{ item.label }}
              </div>

              <div class="bar-value">
                {{ formatCompactCurrency(item.revenue) }}
              </div>

              <div class="bar-orders">
                {{ item.totalOrders }} đơn
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="chartItems.length > 0" class="chart-note">
        Giao dịch bán được ghi nhận theo thời điểm hoàn thành đơn; khoản hoàn tiền
        được ghi nhận theo thời điểm hoàn tiền hoàn tất.
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { RevenueChartResponse } from "../types/report.type";

const props = defineProps<{
  items: RevenueChartResponse[];
}>();

const safeItems = computed(() => {
  if (!Array.isArray(props.items)) {
    return [];
  }

  return props.items;
});

const totalRevenue = computed(() => {
  return safeItems.value.reduce((total, item) => {
    return total + Number(item.revenue || 0);
  }, 0);
});

const maxRevenue = computed(() => {
  const values = safeItems.value.map((item) => Number(item.revenue || 0));
  return Math.max(...values, 0);
});

const chartItems = computed(() => {
  return safeItems.value.map((item) => {
    const revenue = Number(item.revenue || 0);
    const totalOrders = Number(item.totalOrders || 0);

    let heightPercent = 0;

    if (maxRevenue.value > 0 && revenue > 0) {
      heightPercent = (revenue / maxRevenue.value) * 100;
    }

    if (revenue > 0 && heightPercent < 6) {
      heightPercent = 6;
    }

    return {
      label: String(item.label || "N/A"),
      revenue,
      totalOrders,
      heightPercent,
    };
  });
});

const bestQuarterLabel = computed(() => {
  const items = chartItems.value;

  if (items.length === 0) {
    return "-";
  }

  let bestItem = items[0];

  if (!bestItem) {
    return "-";
  }

  for (const item of items) {
    if (item.revenue > bestItem.revenue) {
      bestItem = item;
    }
  }

  if (bestItem.revenue <= 0) {
    return "-";
  }

  return bestItem.label;
});

const yAxisLabels = computed(() => {
  const max = maxRevenue.value;

  if (max <= 0) {
    return ["0", "0", "0", "0", "0"];
  }

  return [
    formatCompactCurrency(max),
    formatCompactCurrency(max * 0.75),
    formatCompactCurrency(max * 0.5),
    formatCompactCurrency(max * 0.25),
    "0đ",
  ];
});

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
};

const formatCompactCurrency = (value: number) => {
  const numberValue = Number(value || 0);
  const sign = numberValue < 0 ? "-" : "";
  const absoluteValue = Math.abs(numberValue);

  if (absoluteValue >= 1_000_000_000) {
    return `${sign}${Math.round(absoluteValue / 1_000_000_000)} tỷ`;
  }

  if (absoluteValue >= 1_000_000) {
    return `${sign}${Math.round(absoluteValue / 1_000_000)} triệu`;
  }

  if (absoluteValue >= 1_000) {
    return `${sign}${Math.round(absoluteValue / 1_000)} nghìn`;
  }

  return `${Math.round(numberValue)}đ`;
};
</script>

<style scoped>
.quarterly-card {
  border-radius: 18px;
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.chart-summary {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.summary-item {
  min-width: 150px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.summary-item span {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.summary-item strong {
  display: block;
  font-size: 15px;
  color: #0f172a;
}

.empty-box {
  min-height: 220px;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}

.chart-wrapper {
  display: flex;
  min-height: 320px;
  gap: 14px;
}

.y-axis {
  width: 74px;
  height: 240px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-top: 2px;
  padding-bottom: 2px;
  color: #94a3b8;
  font-size: 12px;
  text-align: right;
}

.plot-area {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 280px;
  padding-top: 4px;
}

.grid-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: #e5e7eb;
}

.line-100 {
  top: 0;
}

.line-75 {
  top: 60px;
}

.line-50 {
  top: 120px;
}

.line-25 {
  top: 180px;
}

.line-0 {
  top: 240px;
  background: #cbd5e1;
}

.bars {
  position: relative;
  z-index: 2;
  height: 280px;
  display: flex;
  align-items: flex-start;
  justify-content: space-around;
  gap: 24px;
}

.bar-item {
  position: relative;
  flex: 1;
  min-width: 110px;
  max-width: 180px;
  height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar-track {
  width: 64px;
  height: 240px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar-fill {
  width: 64px;
  border-radius: 14px 14px 4px 4px;
  background: linear-gradient(180deg, #111827 0%, #334155 100%);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.2);
  transition: height 0.25s ease, transform 0.2s ease;
}

.bar-fill.is-empty {
  height: 0 !important;
  box-shadow: none;
}

.bar-item:hover .bar-fill {
  transform: translateY(-2px);
}

.bar-label {
  margin-top: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.bar-value {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  text-align: center;
}

.bar-orders {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.bar-tooltip {
  position: absolute;
  bottom: 58px;
  left: 50%;
  transform: translateX(-50%) translateY(6px);
  min-width: 170px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #0f172a;
  color: #ffffff;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.2);
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: all 0.18s ease;
  text-align: center;
  z-index: 5;
}

.bar-tooltip strong,
.bar-tooltip span,
.bar-tooltip small {
  display: block;
}

.bar-tooltip strong {
  font-size: 13px;
  margin-bottom: 4px;
}

.bar-tooltip span {
  font-size: 13px;
  font-weight: 700;
}

.bar-tooltip small {
  margin-top: 2px;
  font-size: 12px;
  color: #cbd5e1;
}

.bar-item:hover .bar-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
}

.chart-note {
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 768px) {
  .chart-header {
    flex-direction: column;
  }

  .chart-summary {
    width: 100%;
    justify-content: stretch;
  }

  .summary-item {
    flex: 1;
    min-width: 0;
  }

  .chart-wrapper {
    overflow-x: auto;
  }

  .plot-area {
    min-width: 560px;
  }
}
</style>
