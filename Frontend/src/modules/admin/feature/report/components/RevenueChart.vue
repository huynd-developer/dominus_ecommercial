<template>
  <div class="card border-0 shadow-sm mb-4">
    <div class="card-header bg-white border-0 py-3">
      <div class="d-flex align-items-center justify-content-between gap-3">
        <div>
          <h5 class="mb-1 fw-bold">Biểu đồ doanh thu</h5>
          <div class="text-muted small">
            Doanh thu ghi nhận từ đơn đã hoàn thành
          </div>
        </div>

        <span v-if="items.length > 0" class="badge bg-dark-subtle text-dark">
          {{ items.length }} mốc
        </span>
      </div>
    </div>

    <div class="card-body">
      <div v-if="items.length === 0" class="empty-box">
        Chưa có dữ liệu doanh thu
      </div>

      <div v-else class="chart-wrapper">
        <canvas ref="canvasRef"></canvas>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Chart } from "chart.js/auto";
import { nextTick, onBeforeUnmount, ref, watch } from "vue";
import type { RevenueChartResponse } from "../types/report.type";

const props = defineProps<{
  items: RevenueChartResponse[];
}>();

const canvasRef = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart | null = null;

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

const renderChart = async () => {
  await nextTick();

  if (!canvasRef.value) return;

  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }

  if (!props.items || props.items.length === 0) {
    return;
  }

  chartInstance = new Chart(canvasRef.value, {
    type: "line",
    data: {
      labels: props.items.map((item) => item.label),
      datasets: [
        {
          label: "Doanh thu",
          data: props.items.map((item) => toNumber(item.revenue)),
          tension: 0.35,
          fill: false,
          pointRadius: 3,
          pointHoverRadius: 5,
        },
        {
          label: "Số đơn",
          data: props.items.map((item) => toNumber(item.totalOrders)),
          tension: 0.35,
          fill: false,
          yAxisID: "orders",
          pointRadius: 3,
          pointHoverRadius: 5,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: "index",
        intersect: false,
      },
      plugins: {
        tooltip: {
          callbacks: {
            label(context) {
              if (context.dataset.yAxisID === "orders") {
                return `Số đơn: ${formatNumber(context.raw)}`;
              }

              return `Doanh thu: ${formatMoney(context.raw)}`;
            },
          },
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback(value) {
              return Number(value).toLocaleString("vi-VN");
            },
          },
        },
        orders: {
          beginAtZero: true,
          position: "right",
          grid: {
            drawOnChartArea: false,
          },
          ticks: {
            precision: 0,
            callback(value) {
              return Number(value).toLocaleString("vi-VN");
            },
          },
        },
      },
    },
  });
};

watch(
  () => props.items,
  () => {
    renderChart();
  },
  {
    deep: true,
    immediate: true,
  }
);

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }
});
</script>

<style scoped>
.chart-wrapper {
  width: 100%;
  height: 360px;
}

.empty-box {
  padding: 50px;
  text-align: center;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 14px;
}
</style>