<template>
  <div class="page-wrapper">
    <header class="lp-header">
      <div class="logo-section">
        <img src="@/assets/Logo.png" alt="Dominus" class="brand-logo" @click="$router.push('/')" />
      </div>
      <div class="header-features">
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg><p>Sản phẩm<br>chính hãng</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="8" y="10" width="8" height="12" rx="2"/><path d="M10 10V6a2 2 0 014 0v4"/><path d="M9 2h6v2H9z"/></svg><p>Hương thơm<br>chuẩn Pháp</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg><p>Đóng gói<br>sang trọng</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 18v-6a9 9 0 0118 0v6"/><path d="M21 19a2 2 0 01-2 2h-1v-6h3v4zM3 19a2 2 0 002 2h1v-6H3v4z"/></svg><p>Tư vấn tận tâm<br>24/7</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg><p>Thanh toán bảo mật<br>100% an toàn</p></div>
        
        <div class="header-actions">
          <div class="nav-cart" @click="$router.push('/cart')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
            <span class="cart-badge" v-if="cartItems.length > 0">{{ cartItems.length }}</span>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content full-width">
      
      <div class="checkout-left">
        <div class="step-section">
          <div class="step-header">
            <span class="step-num">1</span>
            <h2>Thông tin nhận hàng</h2>
          </div>
          
          <div class="form-row">
            <div class="form-group half">
              <label>Họ và tên *</label>
              <div class="input-box">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                <input type="text" v-model="orderForm.customerName" @input="validateName" maxlength="50" placeholder="Họ và tên" />
              </div>
            </div>
            
            <div class="form-group half">
              <label>Số điện thoại *</label>
              <div class="input-box">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>
                <input type="text" v-model="orderForm.customerPhone" @input="validatePhone" maxlength="50" placeholder="Số điện thoại" />
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label>Địa chỉ nhận hàng *</label>
            <div class="input-box textarea-box">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <textarea v-model="orderForm.shippingAddress" @input="validateAddress" maxlength="50" placeholder="Số nhà, tên đường, phường/xã..."></textarea>
            </div>
          </div>

          <div class="form-group">
            <label>Ghi chú đơn hàng (Tùy chọn)</label>
            <div class="input-box textarea-box">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              <textarea v-model="orderForm.note" @input="validateNote" maxlength="50" placeholder="Giao hàng trong giờ hành chính..."></textarea>
            </div>
          </div>
        </div>

        <div class="divider"></div>

        <div class="step-section">
          <div class="step-header">
            <span class="step-num">2</span>
            <h2>Phương thức thanh toán</h2>
          </div>

          <label class="payment-option">
            <div class="radio-wrapper">
              <input type="radio" name="payment" value="COD" v-model="orderForm.paymentMethod" />
              <span class="custom-radio"></span>
            </div>
            <div class="option-info">
              <strong>Thanh toán khi nhận hàng (COD)</strong>
              <span>Thanh toán bằng tiền mặt khi shipper giao tới</span>
            </div>
            <svg class="option-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/><path d="M6 12h.01M18 12h.01"/></svg>
          </label>

          <label class="payment-option">
            <div class="radio-wrapper">
              <input type="radio" name="payment" value="VNPAY" v-model="orderForm.paymentMethod" />
              <span class="custom-radio"></span>
            </div>
            <div class="option-info">
              <strong>Thanh toán trực tuyến qua VNPay</strong>
              <span>Thanh toán an toàn qua thẻ ATM/Internet Banking</span>
            </div>
            <span class="vnpay-text"><span class="red">VN</span><span class="blue">PAY</span></span>
          </label>
        </div>
      </div>

      <div class="checkout-right">
        <h3 class="summary-title">Đơn hàng của bạn ({{ totalItems }})</h3>
        
        <div class="mini-cart-items">
          <div class="mini-item" v-for="item in cartItems" :key="item.cartItemId">
            <div class="mini-qty">{{ item.quantity }}</div>
            <img src="https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=150" class="mini-img" />
            <div class="mini-info">
              <div class="mini-name">{{ item.sku }}</div>
              <div class="mini-price">{{ formatCurrency(item.price * item.quantity) }}</div>
            </div>
          </div>
        </div>

        <div class="summary-preview">
           <div class="summary-line"><span>Tạm tính</span><span>{{ formatCurrency(totalAmount) }}</span></div>
           <div class="summary-line"><span class="free-ship">Phí vận chuyển</span><span class="free-ship">Miễn phí</span></div>
           <div class="summary-line total-line"><span>Tổng thanh toán</span><span class="total-val">{{ formatCurrency(totalAmount) }}</span></div>
        </div>

        <button class="btn-submit" @click="handlePlaceOrder" :disabled="isSubmitting">
          {{ isSubmitting ? 'ĐANG XỬ LÝ...' : 'XÁC NHẬN ĐẶT HÀNG' }}
        </button>
        <button class="btn-back" @click="$router.push('/cart')">Quay lại giỏ hàng</button>
      </div>

    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

const orderForm = ref({
  customerName: '',
  customerPhone: '',
  shippingAddress: '',
  note: '', 
  paymentMethod: 'COD'
});

const cartItems = ref<any[]>([]);
const isSubmitting = ref(false);

const getHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });
const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val).replace('VND', '₫');

const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));
const totalItems = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0));

// Logic Validate
const validateName = () => { orderForm.value.customerName = orderForm.value.customerName.replace(/[^\p{L}\s]/gu, '').replace(/^\s+/, ''); };
const validatePhone = () => { orderForm.value.customerPhone = orderForm.value.customerPhone.replace(/[^\d]/g, ''); };
const validateAddress = () => { orderForm.value.shippingAddress = orderForm.value.shippingAddress.replace(/[^\p{L}\d\s]/gu, '').replace(/^\s+/, ''); };
const validateNote = () => { orderForm.value.note = orderForm.value.note.replace(/[^\p{L}\d\s]/gu, '').replace(/^\s+/, ''); };

const loadCartSummary = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/v1/customer/cart/my-cart', { headers: getHeaders() });
    cartItems.value = res.data;
    if (cartItems.value.length === 0) {
      alert("Giỏ hàng trống, vui lòng chọn sản phẩm!");
      router.push('/product');
    }
  } catch (err) { console.error("Lỗi lấy giỏ hàng", err); }
};

const handlePlaceOrder = async () => {
  if (!orderForm.value.customerName.trim() || !orderForm.value.customerPhone.trim() || !orderForm.value.shippingAddress.trim()) {
    alert("Vui lòng điền thông tin hợp lệ!"); return;
  }

  isSubmitting.value = true;
  try {
    const submitData = { ...orderForm.value, customerName: orderForm.value.customerName.trim(), customerPhone: orderForm.value.customerPhone.trim(), shippingAddress: orderForm.value.shippingAddress.trim(), note: orderForm.value.note.trim() };
    const res = await axios.post('http://localhost:8080/api/v1/orders/checkout', submitData, { headers: getHeaders() });
    
    if (submitData.paymentMethod === 'VNPAY' && res.data.paymentUrl) {
      window.location.href = res.data.paymentUrl;
    } else {
      alert("🎉 " + res.data.message); router.push('/product'); 
    }
  } catch (err: any) { alert(err.response?.data || "Lỗi khi đặt hàng!"); } finally { isSubmitting.value = false; }
};

onMounted(loadCartSummary);
</script>

<style scoped>
* { box-sizing: border-box; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }

/* Navbar */
.lp-header { display: flex; justify-content: space-between; align-items: center; padding: 15px 40px; background: white; border-bottom: 1px solid #eee; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
.logo-section { display: flex; align-items: center; }
.brand-logo { height: 45px; object-fit: contain; cursor: pointer; transition: 0.2s; }
.brand-logo:hover { opacity: 0.8; }
.header-features { display: flex; align-items: center; gap: 30px; }
.feature-item { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #06132b; font-weight: 500;}
.icon-hdr { width: 22px; height: 22px; color: #b78d52; }
.feature-item p { margin: 0; line-height: 1.4; }

/* Nút Giỏ hàng trên Navbar */
.header-actions { padding-left: 30px; border-left: 1px solid #eaeaea; display: flex; align-items: center; }
.nav-cart { position: relative; cursor: pointer; color: #06132b; transition: 0.2s; }
.nav-cart:hover { color: #b78d52; }
.nav-cart svg { width: 28px; height: 28px; }
.cart-badge { position: absolute; top: -6px; right: -8px; background: #b78d52; color: white; font-size: 11px; font-weight: bold; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }

/* Full Width Split Layout */
.main-content.full-width { max-width: 1400px; width: 100%; margin: 40px auto; padding: 0 20px; display: flex; gap: 30px; align-items: flex-start; }
.checkout-left { flex: 2; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 40px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.checkout-right { flex: 1.2; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 30px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); position: sticky; top: 20px; }

/* Left Column */
.step-section { margin-bottom: 10px; }
.step-header { display: flex; align-items: center; gap: 12px; margin-bottom: 25px; }
.step-num { background: #06132b; color: white; width: 26px; height: 26px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; }
.step-header h2 { font-size: 20px; color: #06132b; margin: 0; position: relative; }
.step-header h2::after { content: ''; position: absolute; bottom: -6px; left: 0; width: 30px; height: 2px; background: #b78d52; }

.form-row { display: flex; gap: 20px; }
.form-group { margin-bottom: 20px; width: 100%; }
.form-group.half { flex: 1; }
.form-group label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.input-box { display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; background: white; transition: 0.2s; }
.input-box:focus-within { border-color: #06132b; }
.textarea-box { align-items: flex-start; padding-top: 12px; }
.input-icon { width: 18px; height: 18px; color: #a0aec0; margin-right: 10px; flex-shrink: 0;}
.input-box input { flex: 1; border: none; padding: 14px 0; outline: none; font-size: 14px; color: #333; }
.input-box textarea { flex: 1; border: none; padding: 0 0 14px 0; outline: none; font-size: 14px; color: #333; min-height: 80px; resize: none; font-family: inherit;}

.divider { height: 1px; background: #f0f0f0; margin: 30px 0; }

.payment-option { display: flex; align-items: center; gap: 15px; padding: 20px; border: 1px solid #ddd; border-radius: 8px; margin-bottom: 15px; cursor: pointer; transition: 0.2s; }
.payment-option:hover, .payment-option:has(input:checked) { border-color: #b78d52; background: #fdfaf6; }
.radio-wrapper { position: relative; width: 20px; height: 20px; }
.radio-wrapper input { opacity: 0; position: absolute; cursor: pointer; }
.custom-radio { position: absolute; top: 0; left: 0; height: 20px; width: 20px; background: #fff; border: 2px solid #ddd; border-radius: 50%; }
.radio-wrapper input:checked ~ .custom-radio { border-color: #06132b; }
.custom-radio:after { content: ""; position: absolute; display: none; top: 4px; left: 4px; width: 8px; height: 8px; border-radius: 50%; background: #06132b; }
.radio-wrapper input:checked ~ .custom-radio:after { display: block; }

.option-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.option-info strong { color: #333; font-size: 15px; }
.option-info span { color: #777; font-size: 13px; }
.option-icon { width: 32px; height: 32px; color: #b78d52; }
.vnpay-text { font-weight: 800; font-size: 18px; letter-spacing: -0.5px;}
.vnpay-text .red { color: #e53e3e; }
.vnpay-text .blue { color: #3182ce; }

/* Right Column */
.summary-title { font-size: 18px; margin: 0 0 20px 0; color: #06132b; border-bottom: 1px solid #eee; padding-bottom: 15px; }
.mini-cart-items { max-height: 250px; overflow-y: auto; margin-bottom: 20px; padding-right: 5px; }
.mini-item { display: flex; align-items: center; gap: 12px; margin-bottom: 15px; position: relative; }
.mini-qty { position: absolute; top: -5px; left: -5px; background: #666; color: white; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; z-index: 2; }
.mini-img { width: 50px; height: 50px; border-radius: 6px; border: 1px solid #eaeaea; object-fit: cover; }
.mini-info { flex: 1; }
.mini-name { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 4px; }
.mini-price { font-size: 13px; color: #666; }

.summary-preview { margin-top: 20px; padding: 20px 0; border-top: 1px dashed #cbd5e0; }
.summary-line { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; color: #4a5568; }
.free-ship { color: #38a169; font-weight: 500; }
.total-line { border-top: 1px solid #e2e8f0; padding-top: 15px; margin-top: 5px; font-weight: bold; font-size: 16px; color: #06132b; }
.total-val { font-size: 22px; color: #b78d52; }

.btn-submit { width: 100%; padding: 16px; background: #06132b; color: white; border: none; border-radius: 6px; font-weight: bold; font-size: 15px; cursor: pointer; transition: 0.2s; margin-top: 10px;}
.btn-submit:hover:not(:disabled) { background: #0a1f44; }
.btn-submit:disabled { background: #a0aec0; cursor: not-allowed; }
.btn-back { width: 100%; background: transparent; border: none; color: #666; font-size: 14px; text-decoration: underline; cursor: pointer; padding: 15px 0; margin-top: 5px;}
.btn-back:hover { color: #06132b; }
</style>