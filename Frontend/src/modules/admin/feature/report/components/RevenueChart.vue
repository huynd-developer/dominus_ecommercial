<template>
  <div class="card border-0 shadow-sm mb-4">
    <div class="card-header bg-white border-0 py-3">
      <h5 class="mb-0 fw-bold">Biểu đồ doanh thu</h5>
    </div>

    <div class="card-body">
      <div v-if="items.length === 0" class="empty-box">
        Chưa có dữ liệu doanh thu
      </div>

      <canvas v-show="items.length > 0" ref="canvasRef"></canvas>
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

const formatMoney = (value: number) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};

const renderChart = async () => {
  await nextTick();

  if (!canvasRef.value) return;

  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }

  chartInstance = new Chart(canvasRef.value, {
    type: "line",
    data: {
      labels: props.items.map((item) => item.label),
      datasets: [
        {
          label: "Doanh thu",
          data: props.items.map((item) => Number(item.revenue || 0)),
          tension: 0.35,
          fill: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        tooltip: {
          callbacks: {
            label(context) {
              return `Doanh thu: ${formatMoney(Number(context.raw || 0))}`;
            },
          },
        },
      },
      scales: {
        y: {
          ticks: {
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
  }
});
</script>

<style scoped>
.empty-box {
  padding: 50px;
  text-align: center;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 14px;
}
</style>