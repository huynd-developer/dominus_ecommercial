<template>
  <div class="owner-report-page">
    <div class="page-header mb-4">
      <div>
        <h2 class="fw-bold mb-1">Báo cáo Owner</h2>
        <p class="text-muted mb-0">
          Thống kê doanh thu, đơn hàng và sản phẩm bán chạy
        </p>
      </div>

      <button
        class="btn btn-outline-dark"
        :disabled="reportStore.loading"
        @click="reportStore.fetchAll()"
      >
        Làm mới
      </button>
    </div>

    <ReportFilter />

    <div v-if="reportStore.loading" class="loading-box">
      <div class="spinner-border"></div>
      <p class="mt-3 mb-0">Đang tải dữ liệu báo cáo...</p>
    </div>

    <template v-else>
      <SummaryCards :summary="reportStore.summary" />

      <RevenueChart :items="reportStore.chartData" />

      <BestSellingTable :items="reportStore.bestSellingProducts" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import BestSellingTable from "../components/BestSellingTable.vue";
import ReportFilter from "../components/ReportFilter.vue";
import RevenueChart from "../components/RevenueChart.vue";
import SummaryCards from "../components/SummaryCards.vue";
import { useOwnerReportStore } from "../stores/ownerReport.store";

const reportStore = useOwnerReportStore();

onMounted(() => {
  reportStore.fetchAll();
});
</script>

<style scoped>
.owner-report-page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.loading-box {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>