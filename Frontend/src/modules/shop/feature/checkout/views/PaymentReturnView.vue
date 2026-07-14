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

    <OrderResultCard
      v-else
      mode="failed"
      :title="failedTitle"
      :message="failedMessage"
      :status-text="orderStatusText || 'Thanh toán thất bại'"
      :details="resultDetails"
      primary-text="Tiếp tục mua sắm"
      secondary-text="Về trang chủ"
      @primary="goToProducts"
      @secondary="goToHome"
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

const route = useRoute();
const router = useRouter();

const status = ref<PaymentReturnStatus>("processing");
const serverMessage = ref("");
const orderStatus = ref<number | null>(null);
const resultDetails = ref<ResultDetail[]>([]);

const responseCode = computed(() => {
  return String(route.query.vnp_ResponseCode || "");
});

const transactionStatus = computed(() => {
  return String(route.query.vnp_TransactionStatus || "");
});

const orderStatusText = computed(() => {
  if (orderStatus.value === null || orderStatus.value === undefined) {
    return "";
  }

  return getStatusText(orderStatus.value);
});

const successMessage = computed(() => {
  return (
    serverMessage.value ||
    "Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đã được xác nhận thanh toán và đang chờ cửa hàng xử lý."
  );
});

const failedTitle = computed(() => {
  if (responseCode.value === "24") {
    return "Thanh toán đã bị hủy";
  }

  return "Thanh toán không thành công";
});

const failedMessage = computed(() => {
  if (serverMessage.value) {
    return serverMessage.value;
  }

  if (responseCode.value === "24") {
    return "Bạn đã hủy giao dịch VNPay. Đơn hàng đã được hủy và hệ thống đã hoàn lại tồn kho.";
  }

  return "Giao dịch chưa hoàn tất hoặc có lỗi xảy ra trong quá trình thanh toán.";
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

  return Number.isFinite(numberValue) ? numberValue : null;
};

const isSuccessResponse = (data: any) => {
  if (data?.success === true) {
    return true;
  }

  const currentStatus = getNullableStatus(data);

  /**
   * Online VNPay đúng nghiệp vụ: thanh toán xong -> 1 Đã xác nhận.
   * POS / flow cũ có thể thanh toán xong -> 3 Hoàn thành.
   */
  if (currentStatus === 1 || currentStatus === 3) {
    return true;
  }

  return false;
};

const buildDetails = (data: any) => {
  const currentStatus = getNullableStatus(data);

  const details: ResultDetail[] = [
    {
      label: "Mã đơn hàng",
      value: data?.orderId ? `#${data.orderId}` : "-",
    },
    {
      label: "Trạng thái đơn",
      value:
        data?.statusText ||
        data?.orderStatusText ||
        (currentStatus !== null ? getStatusText(currentStatus) : "-"),
    },
    {
      label: "Phương thức",
      value: formatPaymentMethod(data?.paymentMethod || "VNPAY"),
    },
    {
      label: "Tạm tính",
      value: formatCurrency(getNumber(data?.totalAmount)),
      money: true,
    },
    {
      label: "Giảm giá",
      value: `-${formatCurrency(getNumber(data?.discountAmount))}`,
      money: true,
    },
    {
      label: "Tổng thanh toán",
      value: formatCurrency(getNumber(data?.finalAmount)),
      money: true,
    },
  ];

  if (data?.paidAmount !== undefined && data?.paidAmount !== null) {
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

  if (data?.transactionNo) {
    details.push({
      label: "Mã giao dịch",
      value: data.transactionNo,
    });
  }

  if (data?.bankCode) {
    details.push({
      label: "Ngân hàng",
      value: data.bankCode,
    });
  }

  return details;
};

const getErrorMessage = (error: any) => {
  const data = error?.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  if (error?.message) {
    return error.message;
  }

  return "Không thể xác minh giao dịch VNPay. Vui lòng liên hệ cửa hàng để được hỗ trợ.";
};

const callOnlineReturnApi = async (params: Record<string, string>) => {
  return api.get("/v1/orders/payment/vnpay-return", {
    params,
  });
};

const callLegacyReturnApi = async (params: Record<string, string>) => {
  /**
   * api thường đã có baseURL = /api
   * nên endpoint này sẽ thành /api/vnpay/return
   */
  return api.get("/vnpay/return", {
    params,
  });
};

const shouldFallbackToLegacyEndpoint = (error: any) => {
  const statusCode = Number(error?.response?.status || 0);

  return statusCode === 404 || statusCode === 405;
};

const verifyPaymentReturn = async () => {
  try {
    status.value = "processing";

    const params = normalizeQueryParams();

    let res;

    try {
      /**
       * Ưu tiên flow online mới.
       */
      res = await callOnlineReturnApi(params);
    } catch (error: any) {
      /**
       * Không phá flow cũ:
       * nếu endpoint online không tồn tại thì fallback sang /api/vnpay/return.
       */
      if (!shouldFallbackToLegacyEndpoint(error)) {
        throw error;
      }

      res = await callLegacyReturnApi(params);
    }

    const data = res.data || {};

    serverMessage.value = data.message || "";
    orderStatus.value = getNullableStatus(data);
    resultDetails.value = buildDetails(data);

    if (isSuccessResponse(data)) {
      localStorage.removeItem("applied_voucher");
      status.value = "success";
      return;
    }

    status.value = "failed";
  } catch (error: any) {
    console.error("Lỗi xác minh thanh toán VNPay:", error);

    const data = error?.response?.data || {};

    serverMessage.value = getErrorMessage(error);
    orderStatus.value = getNullableStatus(data);

    resultDetails.value = [
      {
        label: "Mã phản hồi VNPay",
        value: responseCode.value || "-",
      },
      {
        label: "Trạng thái giao dịch",
        value: transactionStatus.value || "-",
      },
    ];

    status.value = "failed";
  }
};

const goToHome = () => {
  router.push("/");
};

const goToProducts = () => {
  router.push("/products");
};

const goToOrders = () => {
  router.push({
    path: "/customer/profile",
    query: {
      tab: "orders",
    },
  });
};

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