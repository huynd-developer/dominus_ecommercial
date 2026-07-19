<template>
  <header
    class="pos-held-header d-flex align-items-stretch justify-content-between gap-3 mb-2 select-none"
  >
    <section class="held-processing-panel flex-grow-1 min-w-0">
      <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
        <div class="d-flex align-items-center gap-2 min-w-0">
          <i class="bi bi-clock-history text-gold"></i>

          <div class="min-w-0">
            <div class="held-title text-truncate">
              Đơn hàng đang xử lý ({{ posStore.heldOrders.length }})
            </div>

            <div class="held-subtitle text-truncate">
              Phiếu treo được hiển thị tại đây để mở lại nhanh.
            </div>
          </div>
        </div>

        <div class="d-flex align-items-center gap-2 shrink-0">
          <button
            type="button"
            class="btn-header-action"
            :disabled="posStore.isLoading"
            title="Tải lại phiếu treo"
            @click="posStore.fetchHeldOrders()"
          >
            <i
              class="bi bi-arrow-clockwise"
              :class="{ spin: posStore.isLoading }"
            ></i>
            Làm mới
          </button>
        </div>
      </div>

      <div
        v-if="posStore.heldOrders.length === 0"
        class="held-empty-card d-flex align-items-center justify-content-center"
      >
        <i class="bi bi-inbox me-2"></i>
        Chưa có phiếu treo.
      </div>

      <div v-else class="held-card-list custom-scrollbar">
        <div
          v-for="held in visibleHeldOrders"
          :key="held.orderId"
          class="held-card"
          :class="{
            active:
              Number(posStore.activeHeldOrderId || 0) === Number(held.orderId),
            disabled: posStore.isLoading || posStore.cashPaid > 0,
          }"
          :title="`Bấm để mở phiếu #${held.orderId}`"
          @click="openHeldOrderFromHeader(held.orderId)"
        >
          <div class="held-card-main">
            <div class="held-card-info min-w-0">
              <div class="held-code text-truncate">
                #{{ held.orderId }} -
                {{ held.customerName || "Khách tại quầy" }}
              </div>

              <div class="held-phone text-truncate">
                {{ held.customerPhone || "Không có SĐT" }}
              </div>

              <div class="held-cashier text-truncate">
                NV: {{ held.cashierName || "Không rõ" }}
              </div>

              <div class="held-money">
                {{ formatPrice(held.finalAmount) }} ₫
              </div>
            </div>

            <div class="held-card-actions shrink-0">
              <button
                type="button"
                class="btn-held-action btn-transfer-mini"
                title="Chuyển phiếu cho nhân viên khác"
                :disabled="posStore.isLoading || posStore.cashPaid > 0"
                @click.stop="transferHeldOrderFromHeader(held.orderId)"
              >
                <i class="bi bi-arrow-left-right"></i>
                Chuyển
              </button>

              <button
                type="button"
                class="btn-held-action btn-cancel-mini"
                title="Hủy phiếu treo"
                :disabled="posStore.isLoading || posStore.cashPaid > 0"
                @click.stop="cancelHeldOrderFromHeader(held.orderId)"
              >
                <i class="bi bi-x-circle"></i>
                Hủy
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="posStore.cashPaid > 0" class="header-warning mt-2">
        <i class="bi bi-lock-fill me-1"></i>
        Đơn đã nhận tiền mặt một phần, không được mở/chuyển sang phiếu khác.
      </div>
    </section>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { usePosStore } from "../stores/posStore";

const posStore = usePosStore();

const visibleHeldOrders = computed(() => {
  return (posStore.heldOrders || []).slice(0, 5);
});

const formatPrice = (val?: number | null) => {
  return new Intl.NumberFormat("vi-VN").format(Number(val || 0));
};

const focusProductSearch = () => {
  window.dispatchEvent(new CustomEvent("pos-focus-product-search"));
};

const openHeldOrderFromHeader = async (orderId: number) => {
  if (!orderId) {
    posStore.errorMsg = "Mã phiếu treo không hợp lệ.";
    return;
  }

  if (posStore.cashPaid > 0) {
    posStore.errorMsg =
      "Đơn đã nhận tiền mặt một phần, không được mở phiếu treo khác.";
    return;
  }

  await posStore.openHeldOrder(orderId);
};

const transferHeldOrderFromHeader = (orderId: number) => {
  if (!orderId) {
    posStore.errorMsg = "Mã phiếu treo không hợp lệ.";
    return;
  }

  if (posStore.cashPaid > 0) {
    posStore.errorMsg =
      "Đơn đã nhận tiền mặt một phần, không được chuyển phiếu.";
    return;
  }

  window.dispatchEvent(
    new CustomEvent("pos-transfer-held-order", {
      detail: {
        orderId,
      },
    })
  );
};

const cancelHeldOrderFromHeader = (orderId: number) => {
  if (!orderId) {
    posStore.errorMsg = "Mã phiếu treo không hợp lệ.";
    return;
  }

  if (posStore.cashPaid > 0) {
    posStore.errorMsg = "Đơn đã nhận tiền mặt một phần, không được hủy phiếu.";
    return;
  }

  window.dispatchEvent(
    new CustomEvent("pos-cancel-held-order", {
      detail: {
        orderId,
      },
    })
  );
};

const handleGlobalKeydown = (e: KeyboardEvent) => {
  if (e.key === "F4") {
    e.preventDefault();

    if (posStore.cashPaid > 0) {
      posStore.errorMsg =
        "Đơn đã nhận tiền mặt một phần, không được quét/thêm sản phẩm.";
      return;
    }

    focusProductSearch();
  }
};

onMounted(async () => {
  window.addEventListener("keydown", handleGlobalKeydown);
  await posStore.fetchHeldOrders();
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleGlobalKeydown);
});
</script>

<style scoped>
.pos-held-header {
  width: 100%;
  min-height: 104px;
  flex-shrink: 0;
}

.held-processing-panel {
  background: rgba(15, 23, 42, 0.78);
  border: 1px solid #1d2740;
  border-radius: 14px;
  padding: 10px 12px;
  min-width: 0;
}

.text-gold {
  color: #f3c63f !important;
}

.held-title {
  color: #f8fafc;
  font-weight: 900;
  font-size: 0.86rem;
  line-height: 1.2;
}

.held-subtitle {
  color: #64748b;
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1.2;
}

.btn-header-action {
  height: 30px;
  border-radius: 9px;
  border: 1px solid #263654;
  background: #10182a;
  color: #9fb0d3;
  padding: 0 10px;
  font-size: 0.7rem;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.btn-header-action:hover:not(:disabled) {
  border-color: #f3c63f;
  color: #f3c63f;
}

.btn-header-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.held-empty-card {
  height: 54px;
  border: 1px dashed #263654;
  border-radius: 12px;
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 800;
  background: rgba(15, 23, 42, 0.35);
}

.held-card-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 2px;
}

/* CARD PHIẾU TREO */
.held-card {
  width: 280px;
  min-width: 280px;
  border-radius: 12px;
  border: 1px solid #263654;
  background: #10182a;
  color: #dbeafe;
  padding: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.held-card:hover,
.held-card.active {
  border-color: rgba(243, 198, 63, 0.72);
  background: rgba(243, 198, 63, 0.08);
}

.held-card.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.held-card-main {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.held-card-info {
  flex: 1;
  min-width: 0;
}

.held-code {
  color: #93c5fd;
  font-size: 0.72rem;
  font-weight: 900;
  line-height: 1.15;
}

.held-phone,
.held-cashier {
  color: #64748b;
  font-size: 0.66rem;
  font-weight: 700;
  line-height: 1.2;
  margin-top: 2px;
}

.held-money {
  color: #22c55e;
  font-size: 0.76rem;
  font-weight: 900;
  margin-top: 5px;
  text-align: left;
}

/* NÚT CHUYỂN / HỦY BÊN PHẢI */
.held-card-actions {
  display: flex;
  flex-direction: column;
  gap: 5px;
  justify-content: center;
  align-items: stretch;
  flex-shrink: 0;
}

.btn-held-action {
  height: 24px;
  min-width: 70px;
  padding: 0 7px;
  border-radius: 7px;
  font-size: 0.62rem;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  border: 1px solid #263654;
  background: rgba(15, 23, 42, 0.88);
  transition: all 0.15s ease;
}

.btn-held-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-transfer-mini {
  color: #f3c63f;
  border-color: rgba(243, 198, 63, 0.35);
}

.btn-cancel-mini {
  color: #fca5a5;
  border-color: rgba(239, 68, 68, 0.35);
}

.btn-transfer-mini:hover:not(:disabled) {
  background: rgba(243, 198, 63, 0.14);
  border-color: rgba(243, 198, 63, 0.72);
}

.btn-cancel-mini:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.16);
  border-color: rgba(239, 68, 68, 0.72);
}

.header-warning,
.header-editing {
  font-size: 0.7rem;
  font-weight: 800;
  line-height: 1.2;
}

.header-warning {
  color: #fca5a5;
}

.header-editing {
  color: #f3c63f;
}

.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(243, 198, 63, 0.55) rgba(15, 23, 42, 0.45);
}

.custom-scrollbar::-webkit-scrollbar {
  height: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.72);
  border-radius: 999px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(243, 198, 63, 0.72);
  border-radius: 999px;
}

.spin {
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
