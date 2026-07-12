<template>
  <div class="payment-return-wrapper">
    <OrderResultCard
      v-if="status === 'processing'"
      mode="processing"
      title="Đang kiểm tra giao dịch..."
      message="Vui lòng chờ trong giây lát. Hệ thống đang xác minh kết quả thanh toán với máy chủ."
    />

    <OrderResultCard
      v-else-if="status === 'success'"
      mode="success"
      title="Thanh toán thành công!"
      message="Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đã được ghi nhận và đang chờ cửa hàng xử lý."
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

const route = useRoute();
const router = useRouter();

const status = ref<PaymentReturnStatus>("processing");
const serverMessage = ref("");

const resultDetails = ref<
  {
    label: string;
    value: string | number;
  }[]
>([]);

const responseCode = computed(() => {
  return String(route.query.vnp_ResponseCode || "");
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
    return "Bạn đã hủy giao dịch VNPay. Đơn hàng đã được hủy và hệ thống đã xử lý hoàn lại tồn kho.";
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

const verifyPaymentReturn = async () => {
  try {
    status.value = "processing";

    const params = normalizeQueryParams();

    const res = await api.get("/v1/orders/payment/vnpay-return", {
      params,
    });

    const data = res.data || {};

    serverMessage.value = data.message || "";

    resultDetails.value = [
      {
        label: "Mã đơn hàng",
        value: data.orderId ? `#${data.orderId}` : "-",
      },
      {
        label: "Phương thức",
        value: data.paymentMethod || "VNPAY",
      },
      {
        label: "Tổng thanh toán",
        value: formatCurrency(Number(data.finalAmount || 0)),
      },
    ];

    status.value = data.success ? "success" : "failed";
  } catch (error: any) {
    console.error("Lỗi xác minh thanh toán VNPay:", error);

    serverMessage.value =
      error?.response?.data?.message ||
      "Không thể xác minh giao dịch VNPay. Vui lòng liên hệ cửa hàng để được hỗ trợ.";

    resultDetails.value = [
      {
        label: "Mã phản hồi",
        value: responseCode.value || "-",
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