<template>
  <div class="cart-premium-panel rounded-3 d-flex flex-column h-100 p-3 select-none">
    <div class="d-flex align-items-center justify-content-between border-bottom border-dark-custom pb-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <div class="icon-wrap rounded p-2 d-flex align-items-center justify-center">
          <i class="bi bi-cart3 text-gold fs-5"></i>
        </div>
        <div>
          <h5 class="mb-0 text-light fw-bold font-sm tracking-wide">Giỏ hàng</h5>
          <span class="text-muted-custom font-xs">{{ posStore.cart.length }} dòng mặt hàng</span>
        </div>
      </div>
    </div>

    <!-- KHU VỰC TÌM KHÁCH HÀNG THEO SĐT — GỌI API THẬT -->
    <div class="customer-section mb-3 shrink-0">
      <div v-if="!posStore.selectedCustomer" class="d-flex align-items-center gap-2">
        <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
          <i class="bi bi-person text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
          <input
            type="text"
            v-model="posStore.customerPhoneInput"
            placeholder="SĐT khách hàng (bỏ trống = khách vãng lai)"
            class="promo-input w-100 rounded-3 font-sm text-light transition-all"
            @keydown.enter="handleSearchCustomer"
          />
        </div>
        <button
          type="button"
          @click="handleSearchCustomer"
          :disabled="posStore.isSearchingCustomer"
          class="apply-promo-btn py-2 px-3 rounded-3 font-sm fw-bold transition-all shrink-0"
        >
          <i v-if="posStore.isSearchingCustomer" class="bi bi-arrow-repeat"></i>
          <span v-else>Tìm</span>
        </button>
      </div>

      <div v-else class="customer-found-pill d-flex align-items-center justify-content-between rounded-3 px-3 py-2">
        <div class="d-flex flex-column">
          <span class="text-light font-sm fw-bold">{{ posStore.selectedCustomer.name }}</span>
          <span class="text-muted-custom font-xs">
            {{ posStore.selectedCustomer.phone }} · {{ posStore.selectedCustomer.loyaltyPoints }} điểm tích lũy
          </span>
        </div>
        <button type="button" class="btn-clear-customer border-0 bg-transparent" @click="posStore.clearSelectedCustomer()">
          <i class="bi bi-x-circle text-muted-custom"></i>
        </button>
      </div>

      <p v-if="posStore.customerLookupMessage && !posStore.selectedCustomer" class="font-xs mb-0 mt-1 text-muted-custom">
        {{ posStore.customerLookupMessage }}
      </p>
    </div>

    <div v-if="posStore.cart.length === 0" class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-center py-5 empty-state-box">
      <div class="mb-3 empty-icon-wrap">
        <i class="bi bi-bag-x display-4 text-muted-custom opacity-25"></i>
      </div>
      <p class="text-light opacity-70 mb-1 fw-bold font-sm">Giỏ hàng hiện tại đang trống</p>
      <p class="text-muted-custom font-xs mb-0">Vui lòng chọn sản phẩm từ danh sách bên trái</p>
    </div>

    <el-scrollbar v-else class="flex-grow-1 mb-2 pe-1">
      <div 
        v-for="item in posStore.cart" 
        :key="item.id" 
        class="cart-item-row d-flex align-items-center justify-content-between p-2.5 rounded-3 mb-2 transition-all"
      >
        <div class="overflow-hidden flex-grow-1 pe-2">
          <div class="fw-bold text-truncate font-sm text-light-custom" :title="item.name">
            {{ item.name }}
          </div>
          <div class="text-gold font-xs fw-bold mt-1">
            {{ formatPrice(item.price) }} ₫
          </div>
        </div>

        <div class="d-flex align-items-center gap-2 shrink-0">
          <div class="quantity-controller-box d-flex align-items-center rounded-2">
            <button class="qty-btn" type="button" @click.stop="posStore.decreaseQuantity(item.id)">
              <i class="bi bi-dash-lg font-xs"></i>
            </button>
            <span class="qty-number px-1 font-xs fw-black">{{ item.quantity }}</span>
            <button class="qty-btn" type="button" @click.stop="posStore.increaseQuantity(item.id)">
              <i class="bi bi-plus-lg font-xs"></i>
            </button>
          </div>
          
          <button class="btn-delete-item p-1.5 rounded-2 d-flex align-items-center justify-content-center" type="button" @click.stop="posStore.removeItem(item.id)">
            <i class="bi bi-trash3 text-danger font-xs"></i>
          </button>
        </div>
      </div>
    </el-scrollbar>

    <div class="checkout-section pt-3 border-top border-dark-custom mt-auto">
      <div class="d-flex justify-content-between mb-2 text-muted-custom font-xs">
        <span>Tạm tính tổng đơn</span>
        <span class="text-light fw-medium">{{ formatPrice(posStore.cartTotal) }} ₫</span>
      </div>

      <!-- Hiển thị số tiền giảm thực tế SAU KHI checkout thành công, vì BE mới là nơi tính đúng -->
      <div v-if="posStore.lastOrder && posStore.lastOrder.discountAmount > 0" class="d-flex justify-content-between mb-2 text-success font-xs">
        <span>Giảm giá ({{ posStore.appliedVoucherCode }})</span>
        <span class="fw-medium">-{{ formatPrice(posStore.lastOrder.discountAmount) }} ₫</span>
      </div>

      <div class="promo-code-container d-flex align-items-center gap-2 mb-3">
        <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
          <i class="bi bi-ticket-perforated text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
          <input 
            type="text" 
            v-model="posStore.voucherCode" 
            placeholder="Nhập mã giảm giá..." 
            class="promo-input w-100 rounded-3 font-sm text-light transition-all"
            :disabled="posStore.cart.length === 0"
          />
        </div>
        <!--
          GIẢI THÍCH NGHIỆP VỤ: BE chỉ validate voucher thật tại lúc gọi /checkout
          (xem PosServiceImpl mục 3.5) — không có endpoint riêng để "preview" số tiền
          giảm trước. Nên nút "Áp dụng" ở đây CHỈ lưu mã vào store để gửi kèm khi
          bấm XÁC NHẬN THANH TOÁN, không gọi API riêng nào cả.
        -->
        <button 
          type="button" 
          @click="handleApplyPromo"
          class="apply-promo-btn py-2 px-3 rounded-3 font-sm fw-bold transition-all shrink-0"
          :disabled="!posStore.voucherCode.trim() || posStore.cart.length === 0"
        >
          Áp dụng
        </button>
      </div>
      
      <div class="d-flex justify-content-between align-items-center mb-3">
        <span class="text-light font-sm fw-bold">Khách cần thanh toán</span>
        <h4 class="text-gold mb-0 fw-black tracking-wide fs-4">
          {{ formatPrice(posStore.cartTotal) }} ₫
        </h4>
      </div>

      <div class="d-flex flex-column gap-2 mb-3">
        <button 
          type="button"
          class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all"
          :class="{ 'active': activePayment === 'cash' }"
          @click="activePayment = 'cash'"
        >
          <i class="bi bi-cash-coin text-warning fs-5"></i> Tiền mặt
        </button>
        
        <button 
          type="button"
          class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all"
          :class="{ 'active': activePayment === 'transfer' }"
          @click="activePayment = 'transfer'"
        >
          <i class="bi bi-qr-code-scan text-info fs-5"></i> Chuyển khoản ngân hàng
        </button>
      </div>

      <p v-if="posStore.checkoutError" class="text-danger font-xs mb-2">
        <i class="bi bi-exclamation-circle"></i> {{ posStore.checkoutError }}
      </p>

      <p v-if="vnpayPendingMessage" class="text-info font-xs mb-2">
        <i class="bi bi-info-circle"></i> {{ vnpayPendingMessage }}
      </p>

      <button 
        class="submit-pay-btn w-100 py-3 rounded-3 text-dark fw-black font-sm tracking-wider transition-all" 
        :disabled="posStore.cart.length === 0 || posStore.isCheckingOut"
        @click="handleSubmitPayment"
      >
        <i v-if="posStore.isCheckingOut" class="bi bi-arrow-repeat"></i>
        <span v-else>XÁC NHẬN THANH TOÁN</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const activePayment = ref('cash')
const vnpayPendingMessage = ref('')

const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val || 0)

const handleApplyPromo = () => {
  const code = posStore.voucherCode.trim()
  if (!code) return
  posStore.setVoucherCode(code)
}

const handleSearchCustomer = () => {
  posStore.searchCustomerByPhone()
}

const handleSubmitPayment = async () => {
  vnpayPendingMessage.value = ''
  const method = activePayment.value === 'cash' ? 'CASH' : 'VNPAY'

  const result = await posStore.checkout(method)

  if (!result.success) return

  if (method === 'CASH') {
    printInvoice(result.order)
    return
  }

  if (result.order?.vnpayPaymentUrl) {
    window.open(result.order.vnpayPaymentUrl, '_blank')
    vnpayPendingMessage.value = `Đơn #${result.order.orderId} đang chờ khách thanh toán qua VNPay. Kho và voucher đã được giữ.`
  }
}

// SỬA: map đúng theo PosOrderResponse / InvoiceItem trả về từ BE
// (trước đây dùng nhầm it.name / it.price / order.subTotal / order.total -> không tồn tại trong DTO thật)
const printInvoice = (order) => {
  if (!order) return
  const win = window.open('', '_blank', 'width=380,height=600')
  if (!win) return

  const itemsHtml = (order.items || []).map(it => `
    <tr>
      <td>
        ${it.productName}
        ${it.capacityLabel ? `<br/><span style="color:#666;font-size:10px">${it.capacityLabel} · ${it.sku}</span>` : ''}
      </td>
      <td style="text-align:center">${it.quantity}</td>
      <td style="text-align:right">${formatPrice(it.unitPrice)}</td>
      <td style="text-align:right">${formatPrice(it.lineTotal)}</td>
    </tr>
  `).join('')

  const createdAtStr = order.createdAt
    ? new Date(order.createdAt).toLocaleString('vi-VN')
    : new Date().toLocaleString('vi-VN')

  win.document.write(`
    <html>
      <head>
        <title>Hóa đơn #${order.orderId}</title>
        <style>
          body { font-family: monospace; font-size: 12px; padding: 16px; }
          h2 { text-align: center; margin: 0 0 4px; }
          .sub { text-align: center; color: #555; margin-bottom: 4px; }
          table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }
          td { padding: 4px 0; vertical-align: top; }
          .line { border-top: 1px dashed #000; margin: 6px 0; }
          .total-row { font-weight: bold; font-size: 14px; }
          .info-row { display: flex; justify-content: space-between; margin-bottom: 2px; }
        </style>
      </head>
      <body>
        <h2>DOMINUS POS</h2>
        <div class="sub">Hóa đơn bán hàng #${order.orderId}</div>
        <div class="sub">${createdAtStr}</div>
        <div class="line"></div>

        <div class="info-row"><span>Khách hàng</span><span>${order.customerName || 'Khách vãng lai'}</span></div>
        ${order.customerPhone ? `<div class="info-row"><span>SĐT</span><span>${order.customerPhone}</span></div>` : ''}
        <div class="info-row"><span>Thu ngân</span><span>${order.cashierName || '-'}</span></div>
        <div class="info-row"><span>Thanh toán</span><span>${order.paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản'}</span></div>

        <div class="line"></div>
        <table>${itemsHtml}</table>
        <div class="line"></div>

        <table>
          <tr><td>Tạm tính</td><td style="text-align:right">${formatPrice(order.totalAmount)}</td></tr>
          ${order.discountAmount > 0 ? `<tr><td>Giảm giá</td><td style="text-align:right">-${formatPrice(order.discountAmount)}</td></tr>` : ''}
          <tr class="total-row"><td>TỔNG CỘNG</td><td style="text-align:right">${formatPrice(order.finalAmount)}</td></tr>
        </table>
        <div class="line"></div>
        <p style="text-align:center">Cảm ơn quý khách!</p>
        <script>window.onload = () => { window.print(); }<\/script>
      </body>
    </html>
  `)
  win.document.close()
}
</script>

<style scoped>
/* PALETTE MÀU DARK PREMIUM */
.cart-premium-panel { 
  background-color: #0b1120; 
  border: 1px solid #1d2740; 
  height: 100%; 
}
.text-gold { color: #f3c63f !important; }
.text-light-custom { color: #f1f5f9; }
.text-muted-custom { color: #64748b; }
.border-dark-custom { border-color: #1d2740 !important; }
.icon-wrap { background-color: rgba(243, 198, 63, 0.06); border: 1px solid rgba(243, 198, 63, 0.15); }

/* Typography chỉn chu */
.font-sm { font-size: 0.875rem; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900; }

/* KHÁCH HÀNG ĐÃ TÌM ĐƯỢC */
.customer-found-pill {
  background-color: #131c31;
  border: 1px solid #222f4f;
}
.btn-clear-customer {
  cursor: pointer;
}
.btn-clear-customer:hover i {
  color: #ef4444 !important;
}

/* DANH SÁCH MÓN */
.cart-item-row { 
  background-color: #131c31; 
  border: 1px solid #222f4f; 
}
.cart-item-row:hover {
  border-color: #2e3f66;
  background-color: #17223b;
}

/* CỤM ĐIỀU KHIỂN TĂNG GIẢM SỐ LƯỢNG */
.quantity-controller-box { 
  background-color: #070c18; 
  border: 1px solid #222f4f; 
  overflow: hidden; 
  padding: 2px;
}
.qty-btn { 
  background: transparent; 
  border: none; 
  color: #94a3b8; 
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer; 
  transition: all 0.15s ease; 
}
.qty-btn:hover { 
  background-color: #222f4f; 
  color: #fff; 
}
.qty-number { 
  color: #f8fafc; 
  min-width: 22px; 
  text-align: center; 
}

/* NÚT THÙNG RÁC */
.btn-delete-item { 
  background: rgba(239, 68, 68, 0.04); 
  border: 1px solid rgba(239, 68, 68, 0.15); 
  cursor: pointer; 
  transition: all 0.15s; 
}
.btn-delete-item:hover { 
  background-color: rgba(239, 68, 68, 0.15); 
  border-color: rgba(239, 68, 68, 0.3);
}

/* ĐƯỢC THÊM: STYLE KHU VỰC Ô NHẬP VOUCHER */
.promo-input {
  background-color: #131c31;
  border: 1px solid #222f4f;
  padding: 10px 12px 10px 40px; /* Thụt lề trái để nhường chỗ cho icon */
  outline: none;
}
.promo-input:focus {
  border-color: #384f80;
  background-color: #17223b;
}
.promo-input:disabled {
  background-color: #0f1626;
  border-color: #1d2740;
  color: #475569;
  cursor: not-allowed;
}

/* Nút áp dụng mã */
.apply-promo-btn {
  background-color: #1e293b;
  border: 1px solid #334155;
  color: #f1f5f9;
  padding-top: 10px;
  padding-bottom: 10px;
  cursor: pointer;
}
.apply-promo-btn:hover:not(:disabled) {
  background-color: #f3c63f;
  border-color: #f3c63f;
  color: #0b1120;
}
.apply-promo-btn:disabled {
  background-color: #131c31;
  border-color: #1d2740;
  color: #475569;
  cursor: not-allowed;
}

/* PHƯƠNG THỨC THANH TOÁN TO RÕ RÀNG */
.payment-method-btn { 
  background-color: #131c31; 
  border: 1px solid #222f4f; 
  color: #94a3b8; 
  cursor: pointer; 
}
.payment-method-btn:hover { 
  border-color: #384f80; 
  color: #f1f5f9; 
}
.payment-method-btn.active {
  border-color: #f3c63f;
  background-color: rgba(243, 198, 63, 0.05);
  color: #f3c63f;
  box-shadow: 0 0 12px rgba(243, 198, 63, 0.1);
}

/* NÚT XÁC NHẬN CHỐT ĐƠN */
.submit-pay-btn { 
  background-image: linear-gradient(to right, #f3c63f, #e0aa14);
  border: none; 
  cursor: pointer; 
  box-shadow: 0 4px 12px rgba(243, 198, 63, 0.15);
}
.submit-pay-btn:hover:not(:disabled) { 
  filter: brightness(1.1);
  box-shadow: 0 6px 16px rgba(243, 198, 63, 0.25);
}
.submit-pay-btn:active:not(:disabled) {
  transform: scale(0.99);
}
.submit-pay-btn:disabled { 
  background-image: none !important;
  background-color: #1e2538 !important; 
  color: #475569 !important; 
  box-shadow: none !important;
  cursor: not-allowed; 
}

/* Hiệu ứng chuyển động mượt */
.transition-all {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>