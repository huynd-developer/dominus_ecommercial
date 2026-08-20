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

const getBackendOrderStatus = (data: any): number | null => {
  const value =
    data?.status ??
    data?.orderStatus ??
    data?.order?.status ??
    null;

  if (value === null || value === undefined || value === "") {
    return null;
  }

  const numberValue = Number(value);

  return Number.isFinite(numberValue)
    ? numberValue
    : null;
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
    bankCode:
      backendData?.bankCode ||
      getVnpayQueryValue(route.query.vnp_BankCode),
    orderInfo:
      backendData?.orderInfo ||
      getVnpayQueryValue(route.query.vnp_OrderInfo),
    amount: backendData?.amount || queryAmount,
    finalAmount: backendData?.finalAmount || queryAmount,
    transferAmount: backendData?.transferAmount || queryAmount,
    paidAmount:
      backendData?.paidAmount ||
      backendData?.finalAmount ||
      queryAmount,
    remainingAmount: 0,
  };
};

const syncRecoveredCartWithLatestProducts = () => {
  if (!Array.isArray(posStore.cart) || posStore.cart.length === 0) {
    return;
  }

  posStore.cart = posStore.cart.map((item: any) => {
    const currentProduct = item?.product;

    if (!currentProduct) {
      return item;
    }

    const latestProduct = posStore.allProducts.find((product: any) => {
      if (
        Number(product?.id || 0) > 0 &&
        Number(product?.id || 0) === Number(currentProduct?.id || 0)
      ) {
        return true;
      }

      return (
        String(product?.sku || "").trim().toLowerCase() ===
        String(currentProduct?.sku || "").trim().toLowerCase()
      );
    });

    if (!latestProduct) {
      return item;
    }

    /*
     * Chỉ refresh thông tin SKU/tồn hiển thị từ InventoryLot.
     * Giữ nguyên quantity mà thu ngân đã nhập, BE sẽ re-check khi checkout.
     */
    return {
      ...item,
      product: {
        ...latestProduct,
      },
    };
  });
};

const recoverCancelledVnpayOrderAsNewSale = async () => {
  /*
   * Draft được lưu trước khi redirect sang VNPay.
   * Đọc lại trước để giữ cart/customer/voucher cho giao dịch mới.
   */
  posStore.restorePendingCheckoutDraft();

  const hadPartialCash = Number(posStore.cashPaid || 0) > 0;

  /*
   * BE đã:
   * - hủy order thanh toán cũ
   * - RETURN_IN đúng InventoryLot đã SALE_OUT
   *
   * FE phải cắt liên kết tới order cũ, không được retry chính order CANCELLED.
   */
  posStore.vnpayUrl = "";
  posStore.vietQrImageUrl = "";
  posStore.vietQrContent = "";
  posStore.pendingVietQrOrderId = null;
  posStore.pendingVietQrAmount = 0;
  posStore.activePendingPaymentOrderId = null;
  posStore.activePendingPaymentTransferProvider = "";
  posStore.activeHeldOrderId = null;
  posStore.activeHeldOrderCashierName = "";
  posStore.lastOrderId = null;

  posStore.paymentMethod = "CASH";
  posStore.transferProvider = "";
  posStore.cashPaid = 0;

  sessionStorage.removeItem("pos_pending_checkout_draft");

  /*
   * Callback failure vừa RETURN_IN InventoryLot ở BE.
   * Refresh sản phẩm rồi đồng bộ cart để sellableQuantity phản ánh tồn thật.
   */
  await posStore.fetchProducts();
  syncRecoveredCartWithLatestProducts();

  if (posStore.cart.length > 0) {
    await posStore.fetchAvailableVouchers();
  }

  await posStore.fetchHeldOrders();

  posStore.errorMsg = hadPartialCash
    ? "VNPay thất bại. Đơn cũ đã hủy và kho đã hoàn đúng lô. Hãy hoàn lại phần tiền mặt đã nhận cho khách trước khi tạo giao dịch mới."
    : "VNPay thất bại. Đơn cũ đã hủy và kho đã hoàn đúng lô. Giỏ hàng đã được khôi phục để tạo giao dịch mới.";
};

const handleVnpayReturnAtPos = async () => {
  try {
    posStore.isLoading = true;
    posStore.errorMsg = "";

    const response = await api.get("/vnpay/return", {
      params: route.query,
    });

    const backendData =
      response.data && typeof response.data === "object"
        ? response.data
        : {};

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

    const backendStatus = getBackendOrderStatus(backendData);

    /*
     * Theo BE mới:
     * - VNPay fail/cancel hợp lệ => order status = 4
     * - POS đã SALE_OUT trước gateway
     * - BE đã RETURN_IN đúng lot SALE_OUT trước khi trả response
     *
     * Vì vậy không được restore draft thành PENDING_PAYMENT của order cũ.
     */
    if (backendStatus === 4) {
      await recoverCancelledVnpayOrderAsNewSale();
      await router.replace({ path: "/admin/pos" });
      return false;
    }

    /*
     * Chưa có trạng thái hủy chắc chắn:
     * giữ nguyên draft để không làm mất payment intent trong trường hợp
     * callback/IPN vẫn đang xử lý.
     */
    posStore.errorMsg =
      backendData.message ||
      "Thanh toán VNPay chưa hoàn tất. Hệ thống giữ nguyên giao dịch đang chờ để tránh xử lý trùng.";

    posStore.restorePendingCheckoutDraft();
    await posStore.fetchProducts();
    await posStore.fetchHeldOrders();
    await router.replace({ path: "/admin/pos" });
    return false;
  } catch (error: any) {
    /*
     * Network/signature/server error => chưa thể khẳng định payment thất bại.
     * Không xóa pending order, không tự hoàn/đổi trạng thái ở FE.
     */
    posStore.errorMsg =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      "Không thể xác minh kết quả VNPay. Hệ thống giữ nguyên giao dịch đang chờ để tránh xử lý trùng.";

    posStore.restorePendingCheckoutDraft();

    /*
     * Refresh tồn hiển thị: nếu BE/IPN đã xử lý thì danh sách sẽ phản ánh
     * đúng InventoryLot hiện tại; nếu chưa xử lý thì vẫn giữ trạng thái đang chờ.
     */
    await posStore.fetchProducts();
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
