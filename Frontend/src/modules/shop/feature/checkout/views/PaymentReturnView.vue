<template>
  <div class="payment-return-wrapper">
    <OrderResultCard
      v-if="status === 'processing'"
      mode="processing"
      title="Đang kiểm tra giao dịch..."
      message="Vui lòng chờ trong giây lát. Hệ thống đang xác minh kết quả thanh toán với máy chủ."
      status-text="Đang xử lý"
    />

    <OrderResultCard
      v-else-if="status === 'success'"
      mode="success"
      title="Thanh toán thành công!"
      :message="successMessage"
      :status-text="orderStatusText || 'Thanh toán thành công'"
      :details="resultDetails"
      primary-text="Tiếp tục mua sắm"
      secondary-text="Xem lịch sử đơn hàng"
      primary-icon="bi bi-arrow-right ms-2"
      @primary="goToHome"
      @secondary="goToOrders"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/common/api";
import OrderResultCard from "../components/OrderResultCard.vue";

type PaymentReturnStatus = "processing" | "success" | "failed";

interface ResultDetail {
  label: string;
  value: string | number;
  money?: boolean;
}

interface PendingVnpayOutcome {
  orderId?: string | number | null;
  status?: number | null;
  success: boolean;
  message?: string;
  processedAt: string;
}

const route = useRoute();
const router = useRouter();

const status = ref<PaymentReturnStatus>("processing");
const serverMessage = ref("");
const orderStatus = ref<number | null>(null);
const resultDetails = ref<ResultDetail[]>([]);

const PENDING_VNPAY_OUTCOME_KEY = "pending_vnpay_outcome";

const responseCode = computed(() =>
  String(route.query.vnp_ResponseCode || "")
);

const transactionStatus = computed(() =>
  String(route.query.vnp_TransactionStatus || "")
);

const orderStatusText = computed(() => {
  if (orderStatus.value === null || orderStatus.value === undefined) {
    return "";
  }

  return getStatusText(orderStatus.value);
});

const successMessage = computed(() => {
  return (
    serverMessage.value ||
    "Cảm ơn bạn đã mua sắm tại Dominus. Thanh toán đã được xác minh và đơn hàng đang được cửa hàng xử lý."
  );
});

const normalizeQueryParams = () => {
  const params: Record<string, string> = {};

  Object.entries(route.query).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      params[key] = value[0] ? String(value[0]) : "";
      return;
    }

    params[key] = value ? String(value) : "";
  });

  return params;
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
};

const formatPaymentMethod = (value: string | null | undefined) => {
  const normalized = String(value || "").toUpperCase();

  if (normalized === "COD") return "Thanh toán khi nhận hàng";
  if (normalized === "VNPAY") return "VNPay";
  if (normalized === "CASH") return "Tiền mặt";
  if (normalized === "BANK_TRANSFER") return "Chuyển khoản";
  if (normalized === "TRANSFER") return "Chuyển khoản";
  if (normalized === "MIXED") return "Tiền mặt + chuyển khoản";

  return value || "-";
};

const getStatusText = (value: number) => {
  switch (Number(value)) {
    case 0:
      return "Chờ xác nhận";
    case 1:
      return "Đã xác nhận";
    case 2:
      return "Đang giao hàng";
    case 3:
      return "Hoàn thành";
    case 4:
      return "Đã hủy";
    case 5:
      return "Giao hàng thất bại";
    case 6:
      return "Yêu cầu hoàn hàng / đổi trả";
    case 7:
      return "Hoàn hàng / đổi trả hoàn tất";
    case 8:
      return "Đã hủy / Chờ hoàn tiền";
    default:
      return "Không xác định";
  }
};

const getNumber = (value: unknown) => {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const getNullableStatus = (data: any) => {
  const value =
    data?.orderStatus ??
    data?.status ??
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

const getFallbackOrderId = () => {
  const pendingOrder = sessionStorage.getItem("pending_vnpay_order");

  if (pendingOrder) {
    return pendingOrder;
  }

  const txnRef = String(route.query.vnp_TxnRef || "");

  if (!txnRef) {
    return null;
  }

  const [orderId] = txnRef.split("_");

  return orderId || txnRef;
};

const savePendingVnpayOutcome = (
  data: any,
  success: boolean
) => {
  const outcome: PendingVnpayOutcome = {
    orderId:
      data?.orderId ??
      data?.id ??
      getFallbackOrderId(),
    status: getNullableStatus(data),
    success,
    message:
      data?.message ||
      data?.error ||
      "",
    processedAt: new Date().toISOString(),
  };

  sessionStorage.setItem(
    PENDING_VNPAY_OUTCOME_KEY,
    JSON.stringify(outcome)
  );
};

const clearPendingVnpayState = () => {
  sessionStorage.removeItem("pending_vnpay_order");
  sessionStorage.removeItem("pending_vnpay_cart");
  sessionStorage.removeItem("pending_vnpay_form");
  sessionStorage.removeItem("pending_vnpay_voucher");
  sessionStorage.removeItem(PENDING_VNPAY_OUTCOME_KEY);
};

const isSuccessResponse = (data: any) => {
  /*
   * Chỉ BE /api/vnpay/return được quyền kết luận payment success.
   * Không dùng vnp_ResponseCode trên URL để tự kết luận vì BE còn phải:
   * - verify chữ ký/amount
   * - xử lý InventoryLot/StockMovement
   * - chống callback trùng
   */
  return data?.success === true;
};

const buildDetails = (data: any) => {
  const currentStatus = getNullableStatus(data);
  const query = route.query;

  const fallbackOrderId =
    data?.orderId ||
    data?.id ||
    getFallbackOrderId();

  const rawVnpAmount = query.vnp_Amount
    ? Number(query.vnp_Amount) / 100
    : 0;

  const fallbackBankCode = query.vnp_BankCode
    ? String(query.vnp_BankCode)
    : null;

  const fallbackTransactionNo = query.vnp_TransactionNo
    ? String(query.vnp_TransactionNo)
    : null;

  const orderIdVal =
    data?.orderId ||
    data?.orderCode ||
    fallbackOrderId;

  const totalAmountVal =
    getNumber(data?.totalAmount) ||
    rawVnpAmount;

  const finalAmountVal =
    getNumber(data?.finalAmount) ||
    rawVnpAmount;

  const discountAmountVal = getNumber(data?.discountAmount);

  // Giữ nguyên cách hiển thị phí ship hiện tại.
  const shippingFeeVal = getNumber(data?.shippingFee ?? 30000);

  const details: ResultDetail[] = [
    {
      label: "Mã đơn hàng",
      value: orderIdVal
        ? String(orderIdVal).startsWith("#")
          ? orderIdVal
          : `#${orderIdVal}`
        : "-",
    },
    {
      label: "Trạng thái đơn",
      value:
        data?.statusText ||
        data?.orderStatusText ||
        (
          currentStatus !== null
            ? getStatusText(currentStatus)
            : "Đã thanh toán"
        ),
    },
    {
      label: "Phương thức",
      value: formatPaymentMethod(data?.paymentMethod || "VNPAY"),
    },
    {
      label: "Tạm tính",
      value: formatCurrency(totalAmountVal),
      money: true,
    },
  ];

  if (discountAmountVal > 0) {
    details.push({
      label: "Giảm giá",
      value: `-${formatCurrency(discountAmountVal)}`,
      money: true,
    });
  }

  details.push({
    label: "Phí vận chuyển",
    value: formatCurrency(shippingFeeVal),
    money: true,
  });

  details.push({
    label: "Tổng thanh toán",
    value: formatCurrency(finalAmountVal),
    money: true,
  });

  if (
    data?.paidAmount !== undefined &&
    data?.paidAmount !== null
  ) {
    details.push({
      label: "Đã thanh toán",
      value: formatCurrency(getNumber(data.paidAmount)),
      money: true,
    });
  }

  if (responseCode.value) {
    details.push({
      label: "Mã phản hồi VNPay",
      value: responseCode.value,
    });
  }

  if (transactionStatus.value) {
    details.push({
      label: "Trạng thái giao dịch",
      value: transactionStatus.value,
    });
  }

  const transNo =
    data?.transactionNo ||
    fallbackTransactionNo;

  if (transNo) {
    details.push({
      label: "Mã giao dịch",
      value: transNo,
    });
  }

  const bank =
    data?.bankCode ||
    fallbackBankCode;

  if (bank) {
    details.push({
      label: "Ngân hàng",
      value: bank,
    });
  }

  return details;
};

const verifyPaymentReturn = async () => {
  try {
    status.value = "processing";

    const params = normalizeQueryParams();

    /*
     * Dùng duy nhất VNPayController mới.
     * Không fallback sang endpoint OrderService cũ vì endpoint cũ chỉ đánh dấu
     * payment reported và không phải flow InventoryLot/FEFO hiện tại.
     */
    const res = await api.get("/vnpay/return", {
      params,
    });

    const rawPayload = res.data || {};
    const data =
      rawPayload.data ||
      rawPayload.result ||
      rawPayload;

    if (isSuccessResponse(data)) {
      serverMessage.value = data.message || "";
      orderStatus.value = getNullableStatus(data);
      resultDetails.value = buildDetails(data);

      localStorage.removeItem("applied_voucher");
      clearPendingVnpayState();

      status.value = "success";
      return;
    }

    /*
     * BE đã trả response thất bại có trạng thái order.
     * Ghi outcome để CheckoutView chỉ restore cart nếu order thực sự CANCELLED.
     */
    savePendingVnpayOutcome(data, false);

    await router.replace("/checkout");
  } catch (error: any) {
    /*
     * Network/server/signature error không đồng nghĩa payment thất bại.
     * Ghi outcome UNKNOWN (status null nếu BE không trả) và KHÔNG tự add cart.
     * CheckoutView sẽ giữ an toàn cho tới khi có kết quả chắc chắn.
     */
    const errorData =
      error?.response?.data &&
      typeof error.response.data === "object"
        ? error.response.data
        : {
            message:
              error?.response?.data?.message ||
              error?.message ||
              "Không thể xác minh kết quả VNPay.",
          };

    savePendingVnpayOutcome(errorData, false);

    await router.replace("/checkout");
  }
};

const goToHome = () => router.push("/");

const goToOrders = () =>
  router.push({
    path: "/customer/profile",
    query: {
      tab: "orders",
    },
  });

onMounted(() => {
  verifyPaymentReturn();
});
</script>

<style scoped>
.payment-return-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  padding: 24px;
}
</style>
