<template>
  <div class="pos-view-wrapper d-flex flex-column vh-100 p-3 overflow-hidden">
    <PosHeader />

    <div class="row flex-grow-1 g-3 mt-1 overflow-hidden min-h-0">
      <div class="col-12 col-xl-8 h-100 overflow-hidden">
        <ProductGrid />
      </div>

      <div class="col-12 col-xl-4 h-100 overflow-hidden">
        <CartSideBar />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/common/api";
import { usePosStore } from "../stores/posStore";
import PosHeader from "../components/posHeader.vue";
import ProductGrid from "../components/productGrid.vue";
import CartSideBar from "../components/cartSideBar.vue";

const route = useRoute();
const router = useRouter();
const posStore = usePosStore();

const isVnpayReturnQuery = () => {
  return Boolean(route.query?.vnp_SecureHash);
};

const normalizeVnpayAmount = (value: unknown) => {
  const numberValue = Number(value || 0);

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return 0;
  }

  return numberValue / 100;
};

const getVnpayQueryValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return String(value[0] || "");
  }

  return String(value || "");
};

const clearVnpayPendingLocalState = () => {
  posStore.vnpayUrl = "";
  posStore.vietQrImageUrl = "";
  posStore.vietQrContent = "";
  posStore.pendingVietQrOrderId = null;
  posStore.pendingVietQrAmount = 0;
  posStore.activePendingPaymentOrderId = null;
  posStore.activePendingPaymentTransferProvider = "";
  posStore.activeHeldOrderId = null;
  posStore.activeHeldOrderCashierName = "";

  sessionStorage.removeItem("pos_pending_checkout_draft");
};

const setLatestCompletedInvoice = (invoice: any) => {
  const safeInvoice = invoice || {};

  posStore.lastCompletedOrder = safeInvoice;
  posStore.showPaymentSuccess = true;

  sessionStorage.setItem("pos_latest_invoice", JSON.stringify(safeInvoice));
};

const buildFallbackVnpayInvoice = (backendData: any) => {
  const queryAmount = normalizeVnpayAmount(route.query.vnp_Amount);

  return {
    ...backendData,
    orderId:
      backendData?.orderId ||
      backendData?.id ||
      getVnpayQueryValue(route.query.vnp_TxnRef),
    paymentMethod: backendData?.paymentMethod || "VNPAY",
    transferProvider: backendData?.transferProvider || "VNPAY",
    transactionNo:
      backendData?.transactionNo ||
      getVnpayQueryValue(route.query.vnp_BankTranNo) ||
      getVnpayQueryValue(route.query.vnp_TransactionNo),
    bankCode: backendData?.bankCode || getVnpayQueryValue(route.query.vnp_BankCode),
    orderInfo:
      backendData?.orderInfo || getVnpayQueryValue(route.query.vnp_OrderInfo),
    amount: backendData?.amount || queryAmount,
    finalAmount: backendData?.finalAmount || queryAmount,
    transferAmount: backendData?.transferAmount || queryAmount,
    paidAmount: backendData?.paidAmount || backendData?.finalAmount || queryAmount,
    remainingAmount: 0,
  };
};

const handleVnpayReturnAtPos = async () => {
  try {
    posStore.isLoading = true;
    posStore.errorMsg = "";

    const response = await api.get("/vnpay/return", {
      params: route.query,
    });

    const backendData =
      response.data && typeof response.data === "object" ? response.data : {};

    if (backendData.success === true) {
      const invoice = buildFallbackVnpayInvoice(backendData);

      clearVnpayPendingLocalState();
      setLatestCompletedInvoice(invoice);

      posStore.cart = [];
      posStore.customer = null;
      posStore.customerSavedKey = "";
      posStore.voucherCode = "";
      posStore.discountAmount = 0;
      posStore.cashPaid = 0;
      posStore.paymentMethod = "CASH";
      posStore.transferProvider = "";
      posStore.lastOrderId = invoice.orderId || posStore.lastOrderId;
      posStore.availableVouchers = [];

      await posStore.fetchProducts();
      await posStore.fetchHeldOrders();

      await router.replace({ path: "/admin/pos" });
      return true;
    }

    /*
     * VNPay trả về nhưng chưa thành công hoặc bị hủy.
     * Không xóa draft ở đây để nhân viên còn đổi phương thức/thanh toán lại.
     */
    posStore.errorMsg =
      backendData.message ||
      "Thanh toán VNPay chưa hoàn tất. Có thể đổi phương thức hoặc thanh toán lại.";

    posStore.restorePendingCheckoutDraft();
    await posStore.fetchHeldOrders();
    await router.replace({ path: "/admin/pos" });
    return false;
  } catch (error: any) {
    posStore.errorMsg =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      "Không thể xử lý kết quả thanh toán VNPay. Vui lòng kiểm tra lại.";

    posStore.restorePendingCheckoutDraft();
    await posStore.fetchHeldOrders();
    await router.replace({ path: "/admin/pos" });
    return false;
  } finally {
    posStore.isLoading = false;
  }
};

onMounted(async () => {
  /*
   * Không được xóa pos_pending_checkout_draft ở đây.
   * Draft này dùng để khôi phục hóa đơn đang chờ thanh toán VNPay/VietQR
   * khi người dùng back lại trang POS.
   */

  await posStore.fetchPosFilters();
  await posStore.fetchProducts();

  /*
   * Nếu VNPay redirect về /admin/pos?vnp_*, phải xử lý kết quả VNPay trước.
   * Không được restore draft trước, vì restore sẽ giữ đơn ở trạng thái đang chờ
   * thanh toán và làm popup thanh toán thành công không hiển thị.
   */
  if (isVnpayReturnQuery()) {
    await handleVnpayReturnAtPos();
    return;
  }

  /*
   * Phải restore sau khi fetchProducts()
   * để giỏ hàng còn map được lại thông tin sản phẩm mới nhất.
   */
  posStore.restorePendingCheckoutDraft();

  await posStore.fetchHeldOrders();
});
</script>

<style scoped>
.pos-view-wrapper {
  background-color: #070c18;
}

.min-h-0 {
  min-height: 0;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #070c18;
}

::-webkit-scrollbar-thumb {
  background: #1a233a;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #2d3d63;
}
</style>
