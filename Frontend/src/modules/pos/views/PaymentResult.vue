<template>
  <div class="payment-result-container">
    <div id="invoice-print-area" class="invoice-card k80-print-area">
      <div class="header">
        <div class="logo">
          <img
            src="src/assets/logo.png"
            alt="Logo Scent Haven"
            width="45"
            height="45"
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
          <span class="value">{{ new Date().toLocaleString("vi-VN") }}</span>
        </div>
        <div class="info-row">
          <span class="label">Phương thức:</span>
          <span class="value">VNPay ({{ bankCode }} - ATM)</span>
        </div>
        <div class="info-row">
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
          <tr>
            <td align="left">
              <span class="product-name">DIOR SAUVAGE PARFUM</span>
              <span class="product-sub">Mã đơn: #{{ orderInfo }}</span>
            </td>
            <td align="center">1</td>
            <td align="right">{{ formatCurrency(amount) }}</td>
          </tr>
        </tbody>
      </table>

      <div class="divider"></div>

      <div class="total-section">
        <span>Tổng thanh toán:</span>
        <span class="total-amount">{{ formatCurrency(amount) }}</span>
      </div>

      <div class="footer">
        <p>Cảm ơn quý khách và hẹn gặp lại!</p>
        <p class="powered">Powered by Scent Haven POS</p>
      </div>
    </div>

    <div class="status-card no-print">
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
      <h3>Thanh Toán Thành Công!</h3>
      <p>Hệ thống đang xuất lệnh in hóa đơn máy in K80...</p>
      <button @click="router.push('/admin/pos')" class="btn-back">
        Quay lại trang bán hàng POS
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const orderId = ref(route.query.vnp_TxnRef || "25132675");
const bankCode = ref(route.query.vnp_BankCode || "NCB");
const transactionNo = ref(route.query.vnp_BankTranNo || "VNP15607475");
const orderInfo = ref(route.query.vnp_OrderInfo || "Thanh toan don hang 25");
const amount = ref(
  route.query.vnp_Amount ? parseInt(route.query.vnp_Amount) / 100 : 3500000
);

const formatCurrency = (value) => {
  if (!value && value !== 0) return "0 đ";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  })
    .format(value)
    .replace("₫", "đ");
};

onMounted(() => {
  setTimeout(() => {
    window.print();
  }, 1000);
});
</script>

<style scoped>
/* MÀN HÌNH KẾT QUẢ VNPAY TRÊN WEB (Giữ nguyên cấu trúc của bạn) */
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
.header {
  text-align: center;
}
.header h1 {
  font-size: 20px;
  margin: 10px 0 5px 0;
  letter-spacing: 1px;
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
.total-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: bold;
  margin-top: 10px;
}
.total-amount {
  font-size: 18px;
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
.success-icon {
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
.btn-back {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  font-size: 13px;
}
.btn-back:hover {
  background: #2563eb;
}
</style>
