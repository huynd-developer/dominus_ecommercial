<template>
  <div class="payment-result-container">
    <div
      id="invoice-print-area"
      class="invoice-card k80-print-area"
      v-if="!isProcessing && isSuccess"
    >
      <div class="header">
        <div class="logo">
          <img
            src="/src/assets/logo.png"
            alt="Logo Scent Haven"
            width="65"
            height="65"
            style="object-fit: contain"
          />
        </div>

        <p>Địa chỉ: 123 Đường ABC, Quận Hoàn Kiếm, Hà Nội</p>
        <p>Hotline: 0987.654.321</p>
      </div>

      <div class="divider"></div>

      <h2 class="invoice-title">HÓA ĐƠN THANH TOÁN</h2>

      <div class="info-section">
        <div class="info-row">
          <span class="label">Số HD:</span>
          <span class="value font-bold">{{ orderId }}</span>
        </div>

        <div class="info-row">
          <span class="label">Thời gian:</span>
          <span class="value">{{ formatDateTime(orderTime) }}</span>
        </div>

        <div class="info-row">
          <span class="label">Khách hàng:</span>
          <span class="value">{{ customerName || "Khách mua tại quầy" }}</span>
        </div>

        <div class="info-row" v-if="customerPhone">
          <span class="label">SĐT:</span>
          <span class="value">{{ customerPhone }}</span>
        </div>

        <div class="info-row" v-if="customerEmail">
          <span class="label">Email:</span>
          <span class="value">{{ customerEmail }}</span>
        </div>

        <div class="info-row">
          <span class="label">Phương thức:</span>
          <span class="value font-bold">{{ paymentMethodText }}</span>
        </div>

        <div class="info-row" v-if="vietQrContent">
          <span class="label">Nội dung CK:</span>
          <span class="value">{{ vietQrContent }}</span>
        </div>

        <div class="info-row" v-if="transactionNo">
          <span class="label">Mã GD VNPay:</span>
          <span class="value">{{ transactionNo }}</span>
        </div>
      </div>

      <table class="invoice-table">
        <thead>
          <tr>
            <th align="left">TÊN SẢN PHẨM</th>
            <th align="center" style="width: 40px">SL</th>
            <th align="right" style="width: 100px">THÀNH TIỀN</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="(item, index) in orderItems" :key="item.sku || index">
            <td align="left">
              <span class="product-name">{{ item.productName }}</span>

              <span class="product-sub" v-if="item.variantName">
                {{ item.variantName }}
              </span>

              <span class="product-sub" v-if="item.sku">
                SKU: {{ item.sku }}
              </span>

              <span class="product-sub">
                {{ formatCurrency(item.price) }}
              </span>
            </td>

            <td align="center">{{ item.quantity }}</td>

            <td align="right">
              {{ formatCurrency(item.lineTotal || item.price * item.quantity) }}
            </td>
          </tr>

          <tr v-if="orderItems.length === 0">
            <td align="left">
              <span class="product-name">Đơn hàng sản phẩm tổng hợp</span>

              <span class="product-sub" v-if="orderInfo">
                Nội dung: {{ orderInfo }}
              </span>
            </td>

            <td align="center">1</td>
            <td align="right">{{ formatCurrency(amount) }}</td>
          </tr>
        </tbody>
      </table>

      <div class="divider"></div>

      <div class="price-details">
        <div class="info-row">
          <span>Tổng cộng:</span>
          <span>{{ formatCurrency(totalAmount) }}</span>
        </div>

        <div class="info-row" v-if="discountAmount > 0">
          <span>Giảm giá:</span>
          <span>-{{ formatCurrency(discountAmount) }}</span>
        </div>

        <div class="info-row font-bold">
          <span>Khách cần thanh toán:</span>
          <span>{{ formatCurrency(amount) }}</span>
        </div>

        <div class="info-row" v-if="cashGiven > 0">
          <span>Tiền mặt:</span>
          <span>{{ formatCurrency(cashGiven) }}</span>
        </div>

        <div class="info-row" v-if="transferAmount > 0">
          <span>{{ transferLabel }}:</span>
          <span>{{ formatCurrency(transferAmount) }}</span>
        </div>

        <div class="info-row" v-if="changeAmount > 0">
          <span>Tiền thừa:</span>
          <span>{{ formatCurrency(changeAmount) }}</span>
        </div>

        <div class="info-row" v-if="paidAmount > 0">
          <span>Đã thanh toán:</span>
          <span>{{ formatCurrency(paidAmount) }}</span>
        </div>

        <div class="info-row" v-if="remainingAmount > 0">
          <span>Còn thiếu:</span>
          <span>{{ formatCurrency(remainingAmount) }}</span>
        </div>
      </div>

      <div class="footer">
        <p>Cảm ơn quý khách và hẹn gặp lại!</p>
        <p class="powered">Powered by Scent Haven POS</p>
      </div>
    </div>

    <div class="status-card no-print" v-if="!isProcessing">
      <div v-if="isSuccess">
        <div class="success-icon">
          <svg
            width="40"
            height="40"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
          >
            <path
              d="M20 6L9 17L4 12"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </div>

        <h3>Thanh toán thành công!</h3>
        <p>
          Hệ thống đã lưu hóa đơn thành công. Bạn có thể in hóa đơn khi cần.
        </p>

        <div class="d-flex gap-2 justify-content-center flex-wrap">
          <button @click="printInvoice" class="btn-print">
            <i class="bi bi-printer"></i>
            In hóa đơn
          </button>

          <button @click="goBackToPos" class="btn-back">Quay lại POS</button>
        </div>
      </div>

      <div v-else>
        <div class="error-icon">❌</div>

        <h3 style="color: #ef4444">Thanh toán thất bại</h3>

        <p>
          {{
            errorMessage || "Giao dịch lỗi hoặc chữ ký xác thực không hợp lệ."
          }}
        </p>

        <button @click="goBackToPos" class="btn-back">Quay lại POS</button>
      </div>
    </div>

    <div class="status-card no-print" v-if="isProcessing">
      <p>Đang xử lý hóa đơn...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/common/api";

const route = useRoute();
const router = useRouter();

const getQueryValue = (value: unknown): string => {
  if (Array.isArray(value)) {
    return String(value[0] || "");
  }

  return String(value || "");
};

const toMoneyNumber = (value: unknown): number => {
  if (value === null || value === undefined || value === "") {
    return 0;
  }

  const numberValue = Number(value);

  if (Number.isNaN(numberValue)) {
    return 0;
  }

  return numberValue;
};

const parseVnpayQueryAmount = (value: unknown): number => {
  const numberValue = toMoneyNumber(value);

  if (numberValue <= 0) {
    return 0;
  }

  return numberValue / 100;
};

const normalizePossibleRawVnpayAmount = (
  value: unknown,
  compareTotal = 0
): number => {
  const numberValue = toMoneyNumber(value);
  const totalValue = toMoneyNumber(compareTotal);

  if (numberValue <= 0) {
    return 0;
  }

  const queryVnpAmount = toMoneyNumber(getQueryValue(route.query.vnp_Amount));

  if (queryVnpAmount > 0 && numberValue === queryVnpAmount) {
    return numberValue / 100;
  }

  if (totalValue > 0 && numberValue === totalValue * 100) {
    return numberValue / 100;
  }

  return numberValue;
};

const orderId = ref(
  getQueryValue(route.query.vnp_TxnRef) ||
    getQueryValue(route.query.orderId) ||
    ""
);

const paymentMethod = ref(
  getQueryValue(route.query.paymentMethod) ||
    (route.query.vnp_SecureHash ? "VNPAY" : "CASH")
);

const transferProvider = ref(getQueryValue(route.query.transferProvider));

const bankCode = ref(getQueryValue(route.query.vnp_BankCode));

const transactionNo = ref(
  getQueryValue(route.query.vnp_BankTranNo) ||
    getQueryValue(route.query.vnp_TransactionNo)
);

const orderInfo = ref(getQueryValue(route.query.vnp_OrderInfo));
const vietQrContent = ref("");

const amount = ref(
  route.query.vnp_Amount
    ? parseVnpayQueryAmount(getQueryValue(route.query.vnp_Amount))
    : 0
);

const totalAmount = ref(amount.value);
const discountAmount = ref(0);
const cashGiven = ref(0);
const transferAmount = ref(0);
const paidAmount = ref(0);
const remainingAmount = ref(0);
const changeAmount = ref(0);

const isProcessing = ref(true);
const isSuccess = ref(false);
const errorMessage = ref("");

const customerName = ref("");
const customerPhone = ref("");
const customerEmail = ref("");
const orderTime = ref("");
const orderItems = ref<any[]>([]);

const normalizedPaymentMethod = computed(() => {
  return String(paymentMethod.value || "").toUpperCase();
});

const normalizedTransferProvider = computed(() => {
  return String(transferProvider.value || "").toUpperCase();
});

const isVnpayPayment = computed(() => {
  return normalizedPaymentMethod.value === "VNPAY";
});

const isVietQrPayment = computed(() => {
  return normalizedPaymentMethod.value === "VIETQR";
});

const isMixedPayment = computed(() => {
  return normalizedPaymentMethod.value === "MIXED";
});

const paymentMethodText = computed(() => {
  if (normalizedPaymentMethod.value === "CASH") {
    return "Tiền mặt";
  }

  if (normalizedPaymentMethod.value === "VNPAY") {
    return bankCode.value ? `VNPay (${bankCode.value})` : "VNPay";
  }

  if (normalizedPaymentMethod.value === "VIETQR") {
    return "VietQR / Chuyển khoản";
  }

  if (normalizedPaymentMethod.value === "MIXED") {
    if (normalizedTransferProvider.value === "VNPAY") {
      return bankCode.value
        ? `Tiền mặt + VNPay (${bankCode.value})`
        : "Tiền mặt + VNPay";
    }

    return "Tiền mặt + VietQR";
  }

  return normalizedPaymentMethod.value || "Không rõ";
});

const transferLabel = computed(() => {
  if (isVnpayPayment.value) {
    return bankCode.value ? `VNPay (${bankCode.value})` : "VNPay";
  }

  if (isVietQrPayment.value) {
    return "VietQR";
  }

  if (isMixedPayment.value) {
    return normalizedTransferProvider.value === "VNPAY" ? "VNPay" : "VietQR";
  }

  return "Chuyển khoản";
});

const formatCurrency = (value: unknown): string => {
  const numberValue = toMoneyNumber(value);

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  })
    .format(numberValue)
    .replace("₫", "đ");
};

const formatDateTime = (dateString: string): string => {
  if (!dateString) {
    return new Date().toLocaleString("vi-VN");
  }

  const date = new Date(dateString);

  if (Number.isNaN(date.getTime())) {
    return new Date().toLocaleString("vi-VN");
  }

  return date.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const normalizeItems = (items: any[] = []) => {
  return items.map((item) => {
    const quantity = Number(item.quantity || item.qty || 1);

    const price = toMoneyNumber(
      item.price || item.unitPrice || item.salePrice || item.product?.price || 0
    );

    const lineTotal = toMoneyNumber(
      item.lineTotal || item.finalPrice || item.total || price * quantity
    );

    return {
      sku: item.sku || item.productSku || item.product?.sku || "",
      productName:
        item.productName || item.name || item.product?.name || "Sản phẩm",
      variantName:
        item.variantName ||
        [item.capacityLabel, item.bottleTypeName].filter(Boolean).join(" - ") ||
        item.capacity ||
        item.size ||
        item.variant?.name ||
        "",
      price,
      quantity,
      lineTotal,
    };
  });
};

const applyInvoiceData = (data: any = {}) => {
  orderId.value =
    data.orderId ||
    data.id ||
    data.code ||
    data.orderCode ||
    data.invoiceCode ||
    orderId.value ||
    "HD" + Date.now();

  paymentMethod.value = data.paymentMethod || paymentMethod.value;

  transferProvider.value =
    data.transferProvider || data.provider || transferProvider.value || "";

  vietQrContent.value = data.vietQrContent || data.vietQrTransferContent || "";

  customerName.value =
    data.customerName ||
    data.customer?.name ||
    customerName.value ||
    "Khách mua tại quầy";

  customerPhone.value =
    data.customerPhone || data.customer?.phone || customerPhone.value || "";

  customerEmail.value =
    data.customerEmail || data.customer?.email || customerEmail.value || "";

  orderTime.value =
    data.orderTime ||
    data.createdAt ||
    data.createdDate ||
    orderTime.value ||
    new Date().toISOString();

  const items =
    data.items || data.orderItems || data.details || data.orderDetails || [];

  orderItems.value = normalizeItems(items);

  const calculatedTotal = orderItems.value.reduce((sum, item) => {
    return sum + (item.lineTotal || item.price * item.quantity);
  }, 0);

  totalAmount.value = toMoneyNumber(
    data.totalAmount ??
      data.subTotal ??
      data.total ??
      calculatedTotal ??
      totalAmount.value ??
      0
  );

  const finalAmountFromBackend =
    data.finalAmount ?? data.payAmount ?? data.totalPay ?? null;

  if (finalAmountFromBackend !== null && finalAmountFromBackend !== undefined) {
    amount.value = toMoneyNumber(finalAmountFromBackend);
  } else {
    const rawAmount = data.amount ?? amount.value ?? totalAmount.value ?? 0;

    amount.value =
      normalizedPaymentMethod.value === "VNPAY" ||
      normalizedPaymentMethod.value === "MIXED"
        ? normalizePossibleRawVnpayAmount(rawAmount, totalAmount.value)
        : toMoneyNumber(rawAmount);
  }

  if (amount.value <= 0 && totalAmount.value > 0) {
    amount.value = totalAmount.value;
  }

  discountAmount.value = toMoneyNumber(
    data.discountAmount ?? Math.max(totalAmount.value - amount.value, 0)
  );

  cashGiven.value = toMoneyNumber(data.cashGiven ?? cashGiven.value ?? 0);

  transferAmount.value = toMoneyNumber(
    data.transferAmount ??
      data.vnpayAmount ??
      data.vnpayPaidAmount ??
      transferAmount.value ??
      0
  );

  if (
    transferAmount.value <= 0 &&
    (normalizedPaymentMethod.value === "VNPAY" ||
      normalizedPaymentMethod.value === "VIETQR")
  ) {
    transferAmount.value = amount.value;
  }

  paidAmount.value = toMoneyNumber(
    data.paidAmount ??
      (["CASH", "VIETQR", "MIXED"].includes(normalizedPaymentMethod.value)
        ? amount.value
        : 0)
  );

  if (
    paidAmount.value <= 0 &&
    ["VNPAY", "VIETQR", "MIXED"].includes(normalizedPaymentMethod.value) &&
    (data.success === true || data.status === "COMPLETED")
  ) {
    paidAmount.value = amount.value;
  }

  remainingAmount.value = toMoneyNumber(data.remainingAmount ?? 0);

  changeAmount.value = toMoneyNumber(
    data.changeAmount ?? Math.max(cashGiven.value - amount.value, 0)
  );
};

const loadLocalPosInvoice = async (): Promise<boolean> => {
  const cachedInvoiceRaw = sessionStorage.getItem("pos_latest_invoice");

  if (cachedInvoiceRaw) {
    try {
      const cachedInvoice = JSON.parse(cachedInvoiceRaw);
      applyInvoiceData(cachedInvoice);
      return true;
    } catch (error) {
      console.warn(
        "Không đọc được dữ liệu hóa đơn trong sessionStorage:",
        error
      );
    }
  }

  if (orderId.value) {
    const response = await api.get(`/admin/pos/order/${orderId.value}`);
    applyInvoiceData(response.data);
    return true;
  }

  return false;
};

const loadVnpayInvoice = async (): Promise<boolean> => {
  paymentMethod.value = "VNPAY";

  const response = await api.get("/vnpay/return", {
    params: route.query,
  });

  if (response.status !== 200) {
    errorMessage.value = "Không xác thực được giao dịch VNPay.";
    return false;
  }

  const backendData =
    response.data && typeof response.data === "object" ? response.data : {};

  applyInvoiceData({
    ...backendData,
    orderId: backendData.orderId || backendData.id || route.query.vnp_TxnRef,
    paymentMethod: backendData.paymentMethod || paymentMethod.value,
    transferProvider: backendData.transferProvider || transferProvider.value,
    finalAmount:
      backendData.finalAmount ??
      backendData.payAmount ??
      backendData.totalPay ??
      amount.value,
  });

  errorMessage.value =
    backendData.message ||
    (backendData.success === true ? "" : "Thanh toán VNPay chưa hoàn tất.");

  return backendData.success === true;
};

const printInvoice = () => {
  window.print();
};

const goBackToPos = () => {
  router.push("/admin/pos");
};

onMounted(async () => {
  try {
    const isVnpayReturn =
      Object.keys(route.query).length > 0 && !!route.query.vnp_SecureHash;

    if (isVnpayReturn) {
      isSuccess.value = await loadVnpayInvoice();
    } else {
      isSuccess.value = await loadLocalPosInvoice();
    }

    if (isSuccess.value) {
      sessionStorage.removeItem("pos_pending_checkout_draft");
    }
  } catch (error) {
    console.error("Lỗi xử lý hóa đơn:", error);
    errorMessage.value = "Có lỗi khi xử lý hóa đơn.";
    isSuccess.value = false;
  } finally {
    isProcessing.value = false;
  }
});
</script>

<style scoped>
.payment-result-container {
  background-color: #0f172a;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  font-family: Arial, sans-serif;
  color: #333;
}

.invoice-card {
  background: #ffffff;
  padding: 30px;
  width: 100%;
  max-width: 450px;
  border-radius: 4px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
}

.header .logo img {
  width: 150px !important;
  height: 150px !important;
  margin-bottom: 1px;
}

.header {
  text-align: center;
}

.header p {
  font-size: 12px;
  color: #666;
  margin: 3px 0;
}

.divider {
  border-top: 1px dashed #ccc;
  margin: 15px 0;
}

.invoice-title {
  text-align: center;
  font-size: 16px;
  margin: 10px 0 20px 0;
  font-weight: bold;
}

.info-section {
  font-size: 13px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.label {
  color: #666;
}

.font-bold {
  font-weight: bold;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.invoice-table th {
  border-bottom: 1px solid #333;
  padding-bottom: 8px;
  font-weight: bold;
}

.invoice-table td {
  padding: 10px 0;
  vertical-align: top;
}

.product-name {
  display: block;
  font-weight: bold;
}

.product-sub {
  display: block;
  font-size: 11px;
  color: #777;
  margin-top: 2px;
}

.price-details {
  font-size: 14px;
  margin-top: 10px;
}

.footer {
  text-align: center;
  margin-top: 30px;
  font-size: 12px;
  font-style: italic;
}

.footer p {
  margin: 4px 0;
}

.powered {
  font-size: 10px;
  color: #999;
}

.status-card {
  margin-top: 25px;
  text-align: center;
  background: rgba(30, 41, 59, 0.7);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #334155;
  width: 100%;
  max-width: 450px;
  box-sizing: border-box;
}

.status-card h3 {
  color: #4ade80;
  margin: 10px 0;
}

.status-card p {
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 15px;
}

.success-icon,
.error-icon {
  width: 50px;
  height: 50px;
  background: rgba(74, 222, 128, 0.1);
  color: #4ade80;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.error-icon {
  background: rgba(239, 68, 68, 0.1);
  font-size: 24px;
}

.btn-back,
.btn-print {
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  font-size: 13px;
}

.btn-back {
  background: #3b82f6;
}

.btn-back:hover {
  background: #2563eb;
}

.btn-print {
  background: #16a34a;
}

.btn-print:hover {
  background: #15803d;
}
</style>

<style>
@media print {
  html,
  body {
    background: #ffffff !important;
    color: #000000 !important;
    padding: 0 !important;
    margin: 0 !important;
  }

  body > div:not(#app),
  #app > div:not(.payment-result-container),
  header,
  nav,
  aside,
  .sidebar,
  [class*="sidebar"],
  [class*="header"]:not(.header),
  .v-navigation-drawer,
  .no-print,
  .status-card,
  .btn-back,
  .btn-print,
  .success-icon,
  button,
  div[class*="arrow"],
  div[id*="arrow"],
  i,
  .v-btn,
  .v-main__wrap > :not(.payment-result-container) {
    display: none !important;
    opacity: 0 !important;
    visibility: hidden !important;
    height: 0 !important;
    padding: 0 !important;
    margin: 0 !important;
  }

  .payment-result-container {
    background: #ffffff !important;
    padding: 0 !important;
    margin: 0 !important;
    min-height: auto !important;
    display: block !important;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
  }

  .invoice-card {
    box-shadow: none !important;
    border: none !important;
    padding: 4mm !important;
    margin: 0 auto !important;
    width: 80mm !important;
    max-width: 80mm !important;
    background: #ffffff !important;
  }

  @page {
    size: 80mm auto;
    margin: 0mm;
  }
}
</style>
