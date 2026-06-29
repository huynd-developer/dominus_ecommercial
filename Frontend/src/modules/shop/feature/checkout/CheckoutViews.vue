<template>
  <div class="page-wrapper">
    <!-- NAVBAR TÔNG TỐI LUXURY (Icon viền tròn) -->
    <header class="dark-navbar">
      <div class="logo-section">
        <img src="@/assets/Logo.png" alt="Dominus" class="brand-logo" @click="$router.push('/')" />
      </div>

      <div class="nav-right">
        <!-- TÀI KHOẢN -->
        <div class="nav-item account-menu">
          <div class="nav-trigger">
            <div class="circle-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></div>
            <span class="nav-text">Tài khoản</span>
            <svg class="chevron-down" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>

          <!-- Dropdown Sổ xuống -->
          <div class="dropdown-content">
            <div class="dropdown-header">
              <div class="avatar">H</div>
              <div class="user-info">
                <span class="user-name">Hoàng Khách Hàng</span>
                <span class="rank-badge">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 3h12l4 6-10 13L2 9z"/></svg>
                  BRONZE RANK - 0 PTS
                </span>
              </div>
            </div>
            <ul class="dropdown-list">
              <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> Thông tin cá nhân</li>
              <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg> Sản phẩm yêu thích</li>
              <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg> Lịch sử đơn hàng</li>
              <li class="logout" @click="handleLogout"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg> Đăng xuất</li>
            </ul>
          </div>
        </div>

        <!-- GIỎ HÀNG -->
        <div class="nav-item cart-btn" @click="$router.push('/cart')">
          <div class="circle-icon cart-wrapper">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
            <span class="cart-badge">{{ cartItems.length || 0 }}</span>
          </div>
          <span class="nav-text">Giỏ hàng</span>
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

          <div class="vat-section-wrapper">
            <div class="vat-toggle-box">
              <div class="vat-toggle-info">
                <div class="vat-toggle-title">
                  <svg class="icon-receipt" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                  <strong>Yêu cầu xuất hóa đơn GTGT (VAT)</strong>
                </div>
                <span class="vat-sub">Hệ thống sẽ gửi hóa đơn điện tử qua email doanh nghiệp của bạn.</span>
              </div>
              <label class="switch">
                <input type="checkbox" v-model="orderForm.requireVat">
                <span class="slider round"></span>
              </label>
            </div>

            <div class="vat-form-box" v-if="orderForm.requireVat">
              <h4 class="vat-form-title">Thông tin doanh nghiệp</h4>
              
              <div class="form-row">
                <div class="form-group half">
                  <label>Mã số thuế *</label>
                  <div class="input-box">
                    <input type="text" v-model="orderForm.vatTaxCode" placeholder="Nhập mã số thuế công ty" />
                  </div>
                </div>
                <div class="form-group half">
                  <label>Email nhận hóa đơn *</label>
                  <div class="input-box">
                    <input type="email" v-model="orderForm.vatEmail" placeholder="accounting@company.com" />
                  </div>
                </div>
              </div>

              <div class="form-group">
                <label>Tên công ty / đơn vị *</label>
                <div class="input-box">
                  <input type="text" v-model="orderForm.vatCompanyName" placeholder="Công ty TNHH..." />
                </div>
              </div>

              <div class="form-group mb-0">
                <label>Địa chỉ công ty (Theo GPKD) *</label>
                <div class="input-box">
                  <input type="text" v-model="orderForm.vatCompanyAddress" placeholder="Số nhà, tên đường, quận/huyện, tỉnh/thành phố..." />
                </div>
              </div>
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
  paymentMethod: 'COD',
  
  requireVat: false,
  vatTaxCode: '',
  vatEmail: '',
  vatCompanyName: '',
  vatCompanyAddress: ''
});

const cartItems = ref<any[]>([]);
const isSubmitting = ref(false);

const getHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });
const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val).replace('VND', '₫');

const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));
const totalItems = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0));

const validateName = () => { orderForm.value.customerName = orderForm.value.customerName.replace(/[^\p{L}\s]/gu, '').replace(/^\s+/, ''); };
const validatePhone = () => { orderForm.value.customerPhone = orderForm.value.customerPhone.replace(/[^\d]/g, ''); };
const validateAddress = () => { orderForm.value.shippingAddress = orderForm.value.shippingAddress.replace(/[^\p{L}\d\s]/gu, '').replace(/^\s+/, ''); };
const validateNote = () => { orderForm.value.note = orderForm.value.note.replace(/[^\p{L}\d\s]/gu, '').replace(/^\s+/, ''); };

const handleLogout = () => {
  if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
    localStorage.removeItem("token");
    router.push("/login");
  }
};

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
    alert("Vui lòng điền thông tin giao hàng hợp lệ!"); return;
  }
  
  if (orderForm.value.requireVat) {
    if (!orderForm.value.vatTaxCode.trim() || !orderForm.value.vatEmail.trim() || !orderForm.value.vatCompanyName.trim() || !orderForm.value.vatCompanyAddress.trim()) {
      alert("Vui lòng điền đầy đủ thông tin xuất hóa đơn VAT!"); return;
    }
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

/* =========================================
   DARK NAVBAR LUXURY CSS
========================================== */
.dark-navbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 40px; height: 70px;
  background-color: #08101f; /* Navy đậm */
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.logo-section { display: flex; align-items: center; }
.brand-logo { height: 100px; object-fit: contain; cursor: pointer; transition: 0.2s; filter: brightness(0) invert(1) sepia(1) saturate(5) hue-rotate(3deg); }
.brand-logo:hover { opacity: 0.8; }

.nav-right { display: flex; align-items: center; gap: 30px; height: 100%; }
.nav-item { display: flex; align-items: center; gap: 8px; cursor: pointer; height: 100%; position: relative; }
.nav-trigger { display: flex; align-items: center; gap: 8px; height: 100%; }

/* Vòng tròn bọc Icon */
.circle-icon { width: 36px; height: 36px; border: 1px solid #718096; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: 0.3s; color: #a0aec0; }
.circle-icon svg { width: 18px; height: 18px; }
.nav-text { color: #fdfdfd; font-size: 14px; font-weight: 500; transition: 0.3s; }
.chevron-down { width: 14px; height: 14px; color: #a0aec0; transition: 0.3s; }

/* Hiệu ứng Hover nháy vàng */
.nav-item:hover .circle-icon { border-color: #b78d52; color: #b78d52; }
.nav-item:hover .nav-text { color: #b78d52; }
.nav-item:hover .chevron-down { color: #b78d52; }

/* Giỏ hàng Badge */
.cart-wrapper { position: relative; }
.cart-badge { position: absolute; top: -6px; right: -6px; background: #b78d52; color: white; font-size: 11px; font-weight: bold; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 2px solid #08101f; }

/* Dropdown Sổ xuống */
.dropdown-content { position: absolute; top: 100%; right: -20px; width: 260px; background: white; border-radius: 8px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); opacity: 0; visibility: hidden; transition: all 0.2s ease-in-out; transform: translateY(10px); z-index: 100; border: 1px solid #eaeaea;}
.account-menu:hover .dropdown-content { opacity: 1; visibility: visible; transform: translateY(0); }
.dropdown-content::before { content: ''; position: absolute; top: -6px; right: 50px; border-left: 6px solid transparent; border-right: 6px solid transparent; border-bottom: 6px solid white; }

.dropdown-header { display: flex; align-items: center; gap: 15px; padding: 20px; border-bottom: 1px solid #f0f0f0; }
.avatar { width: 45px; height: 45px; border-radius: 50%; background: #c19b6c; color: white; font-size: 20px; font-weight: bold; display: flex; align-items: center; justify-content: center; }
.user-info { display: flex; flex-direction: column; gap: 5px; }
.user-name { color: #111; font-weight: bold; font-size: 15px; }
.rank-badge { display: inline-flex; align-items: center; gap: 4px; border: 1px solid #b78d52; color: #b78d52; background: #faf7f2; padding: 3px 8px; border-radius: 20px; font-size: 10px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px;}
.rank-badge svg { width: 12px; height: 12px; }

.dropdown-list { list-style: none; margin: 0; padding: 10px 0; }
.dropdown-list li { display: flex; align-items: center; gap: 12px; padding: 12px 20px; color: #333; font-size: 14px; font-weight: 500; transition: 0.2s; }
.dropdown-list li svg { width: 18px; height: 18px; color: #a0aec0; }
.dropdown-list li:hover { background: #f8fafc; color: #b78d52; }
.dropdown-list li:hover svg { color: #b78d52; }
.dropdown-list li.logout { color: #dc3545; border-top: 1px solid #f0f0f0; margin-top: 5px; padding-top: 15px;}
.dropdown-list li.logout svg { color: #dc3545; }
.dropdown-list li.logout:hover { background: #fff5f5; }

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
.form-group.mb-0 { margin-bottom: 0; }
.form-group.half { flex: 1; }
.form-group label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.input-box { display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; background: white; transition: 0.2s; }
.input-box:focus-within { border-color: #06132b; }
.textarea-box { align-items: flex-start; padding-top: 12px; }
.input-icon { width: 18px; height: 18px; color: #a0aec0; margin-right: 10px; flex-shrink: 0;}
.input-box input { flex: 1; border: none; padding: 14px 0; outline: none; font-size: 14px; color: #333; }
.input-box textarea { flex: 1; border: none; padding: 0 0 14px 0; outline: none; font-size: 14px; color: #333; min-height: 80px; resize: none; font-family: inherit;}

/* VAT SECTION CSS */
.vat-section-wrapper { margin-top: 25px; margin-bottom: 20px; }
.vat-toggle-box { display: flex; justify-content: space-between; align-items: center; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px 20px; background: white; }
.vat-toggle-info { display: flex; flex-direction: column; gap: 4px; }
.vat-toggle-title { display: flex; align-items: center; gap: 8px; color: #1a202c; }
.icon-receipt { width: 18px; height: 18px; color: #4a5568; }
.vat-sub { font-size: 13px; color: #718096; }

/* Nút Toggle (Switch) */
.switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #cbd5e0; transition: .4s; }
.slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .4s; }
input:checked + .slider { background-color: #3182ce; }
input:checked + .slider:before { transform: translateX(20px); }
.slider.round { border-radius: 24px; }
.slider.round:before { border-radius: 50%; }

/* Khung form VAT màu xanh nhạt */
.vat-form-box { background: #ebf8ff; border: 1px solid #bee3f8; border-radius: 8px; padding: 20px; margin-top: 15px; }
.vat-form-title { margin: 0 0 15px 0; color: #2b6cb0; font-size: 15px; font-weight: 600; }
.vat-form-box .form-group label { color: #2d3748; }
.vat-form-box .input-box { border-color: #bee3f8; }
.vat-form-box .input-box:focus-within { border-color: #3182ce; box-shadow: 0 0 0 1px #3182ce; }

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