<template>
  <div class="payment-return-wrapper">
    <!-- Màn hình Thành công -->
    <div class="message-box" v-if="status === 'success'">
      <div class="icon success">✓</div>
      <h2>Thanh toán thành công!</h2>
      <p>Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang được xử lý.</p>
      <button @click="$router.push('/product')">Tiếp tục mua sắm ➔</button>
    </div>

    <!-- Màn hình Hủy / Thất bại -->
    <div class="message-box" v-else-if="status === 'cancel'">
      <div class="icon error">✕</div>
      <h2>Thanh toán bị hủy</h2>
      <p>Bạn đã hủy giao dịch hoặc có lỗi xảy ra trong quá trình thanh toán.</p>
      <button class="btn-outline" @click="$router.push('/cart')">Quay lại Giỏ hàng</button>
    </div>

    <!-- Màn hình chờ Load -->
    <div class="message-box" v-else>
      <h2>Đang kiểm tra giao dịch...</h2>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const status = ref('processing');

onMounted(() => {
  // Lấy mã phản hồi từ URL do VNPay trả về
  const responseCode = route.query.vnp_ResponseCode;
  
  if (responseCode === '00') {
    // 00 là mã thành công của VNPay
    status.value = 'success';
  } else {
    // Các mã khác (như 24 là Khách hàng hủy) đều coi là thất bại
    status.value = 'cancel';
  }
});
</script>

<style scoped>
.payment-return-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafbfc;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.message-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
  text-align: center;
  max-width: 420px;
  width: 100%;
  border-top: 4px solid #b78d52;
}

.icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  margin: 0 auto 25px;
  font-weight: bold;
}

.icon.success { background: #e6ffed; color: #38a169; }
.icon.error { background: #fee2e2; color: #e53e3e; border-top: none; }
.message-box:has(.error) { border-top-color: #e53e3e; }

h2 { color: #06132b; margin: 0 0 10px 0; font-size: 24px; }
p { color: #718096; font-size: 14px; margin-bottom: 30px; line-height: 1.5; }

button {
  width: 100%;
  padding: 14px;
  background: #06132b;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
  font-size: 15px;
}
button:hover { background: #b78d52; }

.btn-outline {
  background: transparent;
  color: #06132b;
  border: 1px solid #06132b;
}
.btn-outline:hover {
  background: #f8fafc;
  color: #b78d52;
  border-color: #b78d52;
}
</style>