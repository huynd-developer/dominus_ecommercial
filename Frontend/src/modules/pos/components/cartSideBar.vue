<template>
  <div class="cart-premium-panel rounded-3 d-flex flex-column h-100 p-3 select-none position-relative">
    <div class="pos-ui-container d-flex flex-column h-100">
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
        <button v-if="posStore.cart.length > 0" class="btn btn-sm btn-outline-danger font-xs" @click="posStore.startNewOrder()">
          <i class="bi bi-trash"></i> Hủy đơn
        </button>
      </div>

      <div class="customer-section mb-3 shrink-0">
        <div v-if="!posStore.customer" class="d-flex align-items-center gap-2">
          <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
            <i class="bi bi-person text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
            <input
              type="text"
              v-model="customerPhoneInput"
              placeholder="SĐT khách (bỏ trống = vãng lai)"
              class="promo-input w-100 rounded-3 font-sm text-light transition-all"
              @keydown.enter="handleSearchCustomer"
            />
          </div>
          <button
            type="button"
            @click="handleSearchCustomer"
            :disabled="posStore.isLoading"
            class="apply-promo-btn py-2 px-3 rounded-3 font-sm fw-bold transition-all shrink-0"
          >
            <i v-if="posStore.isLoading" class="bi bi-arrow-repeat spin"></i>
            <span v-else>Tìm</span>
          </button>
        </div>

        <div v-else class="customer-found-pill d-flex align-items-center justify-content-between rounded-3 px-3 py-2">
          <div class="d-flex flex-column">
            <span class="text-light font-sm fw-bold">{{ posStore.customer.name }}</span>
            <span class="text-muted-custom font-xs">
              {{ posStore.customer.phone }} · {{ posStore.customer.loyaltyPoints }} điểm
            </span>
          </div>
          <button type="button" class="btn-clear-customer border-0 bg-transparent" @click="posStore.customer = null">
            <i class="bi bi-x-circle text-muted-custom"></i>
          </button>
        </div>
      </div>

      <div v-if="posStore.cart.length === 0" class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-center py-5 empty-state-box">
        <div class="mb-3 empty-icon-wrap">
          <i class="bi bi-bag-x display-4 text-muted-custom opacity-25"></i>
        </div>
        <p class="text-light opacity-70 mb-1 fw-bold font-sm">Giỏ hàng hiện tại đang trống</p>
        <p class="text-muted-custom font-xs mb-0">Vui lòng chọn sản phẩm từ danh sách hoặc quét mã vạch</p>
      </div>

      <div v-else class="flex-grow-1 overflow-y-auto mb-2 pe-1 cart-scroll">
        <div v-for="item in posStore.cart" :key="item.product.sku" class="cart-item-row d-flex align-items-center justify-content-between p-2.5 rounded-3 mb-2 transition-all">
          <div class="overflow-hidden flex-grow-1 pe-2">
            <div class="fw-bold text-truncate font-sm text-light-custom" :title="item.product.name">
              {{ item.product.name }}
            </div>
            <div class="text-gold font-xs fw-bold mt-1">
              {{ formatPrice(item.product.price) }} ₫
            </div>
          </div>

          <div class="d-flex align-items-center gap-2 shrink-0">
            <div class="quantity-controller-box d-flex align-items-center rounded-2">
              <button class="qty-btn" type="button" @click.stop="posStore.updateQuantity(item.product.sku, item.quantity - 1)">
                <i class="bi bi-dash-lg font-xs"></i>
              </button>
              <span class="qty-number px-1 font-xs fw-black">{{ item.quantity }}</span>
              <button class="qty-btn" type="button" @click.stop="posStore.updateQuantity(item.product.sku, item.quantity + 1)">
                <i class="bi bi-plus-lg font-xs"></i>
              </button>
            </div>
            <button class="btn-delete-item p-1.5 rounded-2 d-flex align-items-center justify-content-center" type="button" @click.stop="posStore.removeFromCart(item.product.sku)">
              <i class="bi bi-trash3 text-danger font-xs"></i>
            </button>
          </div>
        </div>
      </div>

      <div class="checkout-section pt-3 border-top border-dark-custom mt-auto">
        <div class="d-flex justify-content-between mb-2 text-muted-custom font-xs">
          <span>Tạm tính tổng đơn</span>
          <span class="text-light fw-medium">{{ formatPrice(posStore.totalAmount) }} ₫</span>
        </div>

        <div class="promo-code-container d-flex align-items-center gap-2 mb-3">
          <div class="input-wrapper flex-grow-1 position-relative d-flex align-items-center">
            <i class="bi bi-ticket-perforated text-muted-custom position-absolute start-0 ms-3 font-sm"></i>
            <input type="text" v-model="posStore.voucherCode" placeholder="Mã giảm giá..." class="promo-input w-100 rounded-3 font-sm text-light transition-all" :disabled="posStore.cart.length === 0" />
          </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mb-3">
          <span class="text-light font-sm fw-bold">Khách cần thanh toán</span>
          <h4 class="text-gold mb-0 fw-black tracking-wide fs-4">{{ formatPrice(posStore.finalAmount) }} ₫</h4>
        </div>

        <div class="d-flex flex-column gap-2 mb-3">
          <button type="button" class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all" :class="{ active: posStore.paymentMethod === 'CASH' }" @click="posStore.paymentMethod = 'CASH'">
            <i class="bi bi-cash-coin text-warning fs-5"></i> Tiền mặt
          </button>
          <button type="button" class="payment-method-btn w-100 py-2.5 rounded-3 d-flex align-items-center justify-content-center gap-2 font-sm fw-bold transition-all" :class="{ active: posStore.paymentMethod === 'VNPAY' }" @click="posStore.paymentMethod = 'VNPAY'">
            <i class="bi bi-qr-code-scan text-info fs-5"></i> VNPay (Mã QR)
          </button>
        </div>

        <p v-if="posStore.errorMsg" class="text-danger font-xs mb-2 text-center fw-bold"><i class="bi bi-exclamation-circle"></i> {{ posStore.errorMsg }}</p>

        <button class="submit-pay-btn w-100 py-3 rounded-3 text-dark fw-black font-sm tracking-wider transition-all" :disabled="posStore.cart.length === 0 || posStore.isLoading" @click="handleCheckoutAction">
          <span v-if="posStore.isLoading"><i class="bi bi-arrow-repeat spin"></i> ĐANG XỬ LÝ...</span>
          <span v-else>XÁC NHẬN THANH TOÁN</span>
        </button>

        <button v-if="posStore.vnpayUrl" class="w-100 py-2 mt-2 rounded-3 text-light border border-secondary bg-transparent transition-all btn-refresh" @click="posStore.startNewOrder()">
          <i class="bi bi-arrow-clockwise"></i> LÀM MỚI (ĐÓN KHÁCH MỚI)
        </button>
      </div>
    </div>

    <div v-if="showCashModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Thanh Toán Tiền Mặt</h3>
          <button class="close-btn" @click="showCashModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="payment-row">
            <span>Khách cần thanh toán:</span>
            <span class="text-highlight">{{ formatPrice(posStore.finalAmount) }} ₫</span>
          </div>
          <div class="payment-row input-row">
            <span>Tiền khách đưa:</span>
            <div class="position-relative w-100 d-flex align-items-center">
              <input type="text" v-model="displayCash" @input="handleInputCash" placeholder="Nhập số tiền..." class="cash-input pe-4 text-end" />
              <span class="position-absolute text-muted-custom fw-bold" style="right: 12px">₫</span>
            </div>
            <div class="d-flex gap-2 mt-2">
              <button @click="setExactAmount" class="btn-quick-cash">Khách đưa đủ</button>
              <button @click="addAmount(500000)" class="btn-quick-cash">+500.000</button>
              <button @click="addAmount(100000)" class="btn-quick-cash">+100.000</button>
            </div>
          </div>
          <div class="payment-row border-top border-dark-custom pt-3 mt-1">
            <span>Tiền thừa trả khách:</span>
            <span :class="changeAmount >= 0 ? 'text-success' : 'text-danger'">
              {{ changeAmount >= 0 ? formatPrice(changeAmount) + " ₫" : "Khách đưa chưa đủ" }}
            </span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showCashModal = false">Hủy bỏ</button>
          <button class="btn-confirm" :disabled="changeAmount < 0" @click="processCashPayment">Hoàn tất & In Bill</button>
        </div>
      </div>
    </div>

   
    <Teleport to="body">
      <div id="print-bill-section" class="print-only-bill k80-print-area">
        <div class="bill-header">
          <div class="logo-wrapper">
            <img v-if="shopLogoUrl" :src="shopLogoUrl" alt="Scent Haven" class="logo-img" />
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" class="shop-logo">
              <path d="M50,15 L60,15 L62,22 L38,22 L40,15 Z" fill="none" stroke="#000" stroke-width="2" stroke-linejoin="round"/>
              <circle cx="50" cy="11" r="3" fill="#000"/>
              <path d="M30,28 L70,28 L75,75 C75,80 70,85 65,85 L35,85 C30,85 25,80 25,75 Z" fill="none" stroke="#000" stroke-width="2.5" stroke-linejoin="round"/>
              <rect x="38" y="48" width="24" height="24" rx="2" fill="none" stroke="#000" stroke-width="1.5" stroke-dasharray="2,2"/>
              <text x="50" y="61" font-family="'Playfair Display', serif" font-size="7" font-weight="bold" fill="#000" text-anchor="middle" letter-spacing="1">SCENT</text>
              <text x="50" y="68" font-family="'Playfair Display', serif" font-size="6" fill="#000" text-anchor="middle" letter-spacing="0.5">HAVEN</text>
            </svg>
          </div>
          <h1 class="store-name">SCENT HAVEN</h1>
          <p class="store-info">Địa chỉ: 123 Đường ABC, Quận Hoàn Kiếm, Hà Nội</p>
          <p class="store-info">Hotline: 0987.654.321</p>
          <div class="divider-dashed"></div>
          <h2 class="bill-title">HÓA ĐƠN THANH TOÁN</h2>
        </div>

        <div class="bill-meta">
          <div class="meta-row">
            <span>Số HĐ:</span>
            <strong>{{ transactionId }}</strong>
          </div>
          <div class="meta-row">
            <span>Thời gian:</span>
            <span>{{ new Date().toLocaleString('vi-VN') }}</span>
          </div>
          <div class="meta-row">
            <span>Khách hàng:</span>
            <span>{{ posStore.customer ? posStore.customer.name : 'Khách vãng lai' }}</span>
          </div>
          <div class="meta-row">
            <span>Phương thức:</span>
            <span>Tiền mặt</span>
          </div>
        </div>

        <table class="bill-table">
          <thead>
            <tr>
              <th class="text-left">Tên sản phẩm</th>
              <th class="text-center" width="12%">SL</th>
              <th class="text-right" width="35%">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in posStore.cart" :key="item.product.sku">
              <td class="text-left product-name">
                <strong>{{ item.product.name }}</strong>
                <div style="font-size: 10px; color: #555;">{{ formatPrice(item.product.price) }}</div>
              </td>
              <td class="text-center">{{ item.quantity }}</td>
              <td class="text-right">{{ formatPrice(item.product.price * item.quantity) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="divider-dashed"></div>

        <div class="bill-summary">
          <div class="total-payable">
            <span class="total-label">Tổng cộng:</span>
            <span class="total-amount">{{ formatPrice(posStore.totalAmount) }} ₫</span>
          </div>
          <div class="total-payable" v-if="posStore.finalAmount < posStore.totalAmount">
            <span class="total-label" style="font-weight: normal; font-size: 12px;">Giảm giá:</span>
            <span class="total-amount" style="font-weight: normal; font-size: 12px;">-{{ formatPrice(posStore.totalAmount - posStore.finalAmount) }} ₫</span>
          </div>
          <div class="total-payable" style="margin-top: 5px;">
            <span class="total-label">Khách thanh toán:</span>
            <span class="total-amount">{{ formatPrice(posStore.finalAmount) }} ₫</span>
          </div>
          <div class="total-payable" style="margin-top: 5px; font-weight: normal; font-size: 12px;">
            <span>Tiền khách đưa:</span>
            <span>{{ displayCash }} ₫</span>
          </div>
          <div class="total-payable" style="margin-top: 5px; font-weight: normal; font-size: 12px;">
            <span>Tiền thừa:</span>
            <span>{{ formatPrice(changeAmount) }} ₫</span>
          </div>
        </div>

        <div class="divider-dashed"></div>

        <div class="bill-footer">
          <p class="thank-you">Cảm ơn quý khách và hẹn gặp lại!</p>
          <p class="software-credit">Powered by Scent Haven POS</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { usePosStore } from "../stores/posStore";

const posStore = usePosStore();
const router = useRouter();
const customerPhoneInput = ref("");

const shopLogoUrl = ref('');

const transactionId = ref('TM' + Math.floor(Math.random() * 1000000));
const formatPrice = (val) => new Intl.NumberFormat("vi-VN").format(val || 0);

const handleSearchCustomer = () => {
  if (customerPhoneInput.value) {
    posStore.searchCustomer(customerPhoneInput.value);
  }
};

const showCashModal = ref(false);
const displayCash = ref("");

const rawCashGiven = computed(() => {
  const raw = displayCash.value.replace(/\D/g, "");
  return raw ? parseInt(raw, 10) : 0;
});

const changeAmount = computed(() => {
  return rawCashGiven.value - posStore.finalAmount;
});

const handleInputCash = (e) => {
  let val = e.target.value.replace(/\D/g, "");
  if (!val) {
    displayCash.value = "";
    return;
  }
  displayCash.value = new Intl.NumberFormat("vi-VN").format(val);
};

const setExactAmount = () => {
  displayCash.value = new Intl.NumberFormat("vi-VN").format(posStore.finalAmount);
};

const addAmount = (amount) => {
  const current = rawCashGiven.value;
  displayCash.value = new Intl.NumberFormat("vi-VN").format(current + amount);
};

const handleCheckoutAction = () => {
  if (posStore.paymentMethod === "CASH") {
    displayCash.value = new Intl.NumberFormat("vi-VN").format(posStore.finalAmount);
    transactionId.value = 'TM' + Math.floor(Math.random() * 1000000);
    showCashModal.value = true;
  } else {
    posStore.processCheckout();
  }
};

const processCashPayment = () => {
  if (changeAmount.value < 0) return; 

  window.onafterprint = () => {
    posStore.startNewOrder();
    showCashModal.value = false;
    displayCash.value = "";
    customerPhoneInput.value = "";
    window.onafterprint = null; 
  };

  window.print();
};
</script>

<style>
@media print {
  /* Ẩn triệt để mọi thứ trong toàn bộ hệ thống web */
  body * {
    visibility: hidden !important;
  }

  /* Chỉ hiển thị duy nhất vùng chứa Class chuẩn K80 */
  .k80-print-area, .k80-print-area * {
    visibility: visible !important;
  }

  /* Định vị vùng in đè lên góc trái màn hình giấy in nhiệt */
  .k80-print-area {
    display: block !important;
    position: absolute !important;
    left: 0 !important;
    top: 0 !important;
    width: 80mm !important; 
    padding: 2mm !important;
    margin: 0 !important;
    background-color: #fff !important;
    color: #000 !important;
    box-shadow: none !important;
  }

  @page {
    margin: 0 !important;
    size: 80mm auto !important;
  }
}
</style>

<style scoped>
/* GIAO DIỆN MÀN HÌNH CHÍNH (Giữ nguyên gốc của bạn) */
.cart-premium-panel { background-color: #0b1120; border: 1px solid #1d2740; height: 100%; }
.text-gold { color: #f3c63f !important; }
.text-light-custom { color: #f1f5f9; }
.text-muted-custom { color: #64748b; }
.border-dark-custom { border-color: #1d2740 !important; }
.icon-wrap { background-color: rgba(243, 198, 63, 0.06); border: 1px solid rgba(243, 198, 63, 0.15); }
.font-sm { font-size: 0.875rem; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900; }
.customer-found-pill { background-color: #131c31; border: 1px solid #222f4f; }
.btn-clear-customer { cursor: pointer; }
.btn-clear-customer:hover i { color: #ef4444 !important; }
.cart-item-row { background-color: #131c31; border: 1px solid #222f4f; }
.cart-item-row:hover { border-color: #2e3f66; background-color: #17223b; }
.quantity-controller-box { background-color: #070c18; border: 1px solid #222f4f; overflow: hidden; padding: 2px; }
.qty-btn { background: transparent; border: none; color: #94a3b8; width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; border-radius: 4px; cursor: pointer; transition: all 0.15s ease; }
.qty-btn:hover { background-color: #222f4f; color: #fff; }
.qty-number { color: #f8fafc; min-width: 22px; text-align: center; }
.btn-delete-item { background: rgba(239, 68, 68, 0.04); border: 1px solid rgba(239, 68, 68, 0.15); cursor: pointer; transition: all 0.15s; }
.btn-delete-item:hover { background-color: rgba(239, 68, 68, 0.15); border-color: rgba(239, 68, 68, 0.3); }
.promo-input { background-color: #131c31; border: 1px solid #222f4f; padding: 10px 12px 10px 40px; outline: none; }
.promo-input:focus { border-color: #384f80; background-color: #17223b; }
.promo-input:disabled { background-color: #0f1626; border-color: #1d2740; color: #475569; cursor: not-allowed; }
.apply-promo-btn { background-color: #1e293b; border: 1px solid #334155; color: #f1f5f9; padding-top: 10px; padding-bottom: 10px; cursor: pointer; }
.apply-promo-btn:hover:not(:disabled) { background-color: #f3c63f; border-color: #f3c63f; color: #0b1120; }
.payment-method-btn { background-color: #131c31; border: 1px solid #222f4f; color: #94a3b8; cursor: pointer; }
.payment-method-btn:hover { border-color: #384f80; color: #f1f5f9; }
.payment-method-btn.active { border-color: #f3c63f; background-color: rgba(243, 198, 63, 0.05); color: #f3c63f; box-shadow: 0 0 12px rgba(243, 198, 63, 0.1); }
.submit-pay-btn { background-image: linear-gradient(to right, #f3c63f, #e0aa14); border: none; cursor: pointer; box-shadow: 0 4px 12px rgba(243, 198, 63, 0.15); }
.submit-pay-btn:hover:not(:disabled) { filter: brightness(1.1); box-shadow: 0 6px 16px rgba(243, 198, 63, 0.25); }
.submit-pay-btn:disabled { background-image: none !important; background-color: #1e2538 !important; color: #475569 !important; box-shadow: none !important; cursor: not-allowed; }
.btn-refresh:hover { background-color: rgba(255, 255, 255, 0.1); }
.transition-all { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.spin { animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { 100% { transform: rotate(360deg); } }
.cart-scroll::-webkit-scrollbar { width: 4px; }
.cart-scroll::-webkit-scrollbar-track { background: transparent; }
.cart-scroll::-webkit-scrollbar-thumb { background: #1d2740; border-radius: 4px; }
.cart-scroll::-webkit-scrollbar-thumb:hover { background: #384f80; }

/* MODAL */
.modal-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(11, 17, 32, 0.85); display: flex; justify-content: center; align-items: center; z-index: 9999; backdrop-filter: blur(4px); border-radius: inherit; }
.modal-content { background: #1e293b; border: 1px solid #334155; border-radius: 12px; width: 90%; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); color: #f8fafc; overflow: hidden; }
.modal-header { padding: 16px 20px; border-bottom: 1px solid #334155; display: flex; justify-content: space-between; align-items: center; }
.modal-header h3 { margin: 0; font-size: 16px; color: #f1f5f9; font-weight: bold; }
.close-btn { background: none; border: none; color: #94a3b8; font-size: 20px; cursor: pointer; }
.close-btn:hover { color: #ef4444; }
.modal-body { padding: 20px; }
.payment-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: 14px; }
.input-row { flex-direction: column; align-items: flex-start; gap: 8px; }
.cash-input { width: 100%; padding: 12px; border-radius: 6px; border: 1px solid #475569; background: #0f172a; color: #fbbf24; font-size: 18px; font-weight: bold; outline: none; }
.cash-input:focus { border-color: #3b82f6; }
.btn-quick-cash { padding: 6px 12px; background: #0f172a; color: #94a3b8; border: 1px solid #334155; border-radius: 6px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.btn-quick-cash:hover { background: #3b82f6; color: white; border-color: #3b82f6; }
.text-highlight { color: #fbbf24; font-size: 18px; font-weight: bold; }
.text-success { color: #4ade80; font-size: 16px; font-weight: bold; }
.text-danger { color: #ef4444; font-size: 14px; font-weight: bold; font-style: italic; }
.modal-footer { padding: 16px 20px; background: #0f172a; display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 16px; background: transparent; color: #94a3b8; border: 1px solid #475569; border-radius: 6px; cursor: pointer; }
.btn-cancel:hover { background: #334155; }
.btn-confirm { padding: 8px 16px; background: #3b82f6; color: white; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; }
.btn-confirm:disabled { background: #475569; color: #94a3b8; cursor: not-allowed; }
.btn-confirm:not(:disabled):hover { background: #2563eb; }

/*
  Ẩn khối bill khi không in. Vue vẫn gắn attribute scoped (data-v-xxxx) lên
  phần tử này dù đã bị Teleport ra <body>, nên rule "scoped" này vẫn áp dụng
  đúng, không bị lộ ra ngoài các phần tử khác của trang.
*/
.print-only-bill { display: none; }

/*
  FIX (bill không cân giữa / không có định dạng khi in):
  Các class bill-header, logo-wrapper, bill-meta, bill-table, bill-summary,
  bill-footer... được dùng trong template nhưng TRƯỚC ĐÂY KHÔNG HỀ có CSS
  nào định nghĩa cho chúng cả -> khi in, trình duyệt chỉ áp style mặc định
  (canh trái, không có bảng, cỡ chữ tuỳ tiện) -> logo/tiêu đề bị lệch trái
  y hệt ảnh chụp màn hình bạn gửi. Bổ sung đầy đủ CSS bên dưới để hoá đơn
  có bố cục chuẩn khổ giấy K80, logo và tiêu đề canh giữa.
*/
.k80-print-area { font-family: 'Courier New', Courier, monospace; color: #000; }

.bill-header { text-align: center; }
.logo-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 4px;
}
.shop-logo { width: 46px; height: 46px; display: block; }
.logo-img {
  width: 46px;
  height: 46px;
  object-fit: contain;
  display: block;
  margin: 0 auto;
}
.store-name { font-size: 15px; font-weight: 900; letter-spacing: 1px; margin: 4px 0 2px; text-align: center; }
.store-info { font-size: 10px; margin: 1px 0; text-align: center; }
.divider-dashed { border-top: 1px dashed #000; margin: 8px 0; }
.bill-title { font-size: 13px; font-weight: bold; text-align: center; margin: 6px 0 2px; }

.bill-meta { font-size: 11px; margin-bottom: 8px; }
.meta-row { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 2px; }
.meta-row strong { font-weight: bold; }

.bill-table { width: 100%; border-collapse: collapse; font-size: 11px; margin-bottom: 4px; }
.bill-table th { border-bottom: 1px solid #000; padding-bottom: 4px; font-size: 10px; font-weight: bold; }
.bill-table td { padding: 4px 0; vertical-align: top; }
.bill-table .text-left { text-align: left; }
.bill-table .text-center { text-align: center; }
.bill-table .text-right { text-align: right; }
.product-name strong { font-size: 11px; }

.bill-summary { font-size: 11px; }
.total-payable { display: flex; justify-content: space-between; gap: 8px; }
.total-label { font-weight: bold; font-size: 13px; }
.total-amount { font-weight: bold; font-size: 13px; }

.bill-footer { text-align: center; margin-top: 10px; }
.thank-you { font-size: 11px; font-style: italic; margin-bottom: 2px; }
.software-credit { font-size: 9px; color: #555; }
</style>