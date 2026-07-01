<template>
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
            <input type="text" v-model="form.customerName" @input="validateName" maxlength="50" placeholder="Họ và tên" />
          </div>
        </div>
        <div class="form-group half">
          <label>Số điện thoại *</label>
          <div class="input-box">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>
            <input type="text" v-model="form.customerPhone" @input="validatePhone" maxlength="50" placeholder="Số điện thoại" />
          </div>
        </div>
      </div>
      
      <div class="form-group">
        <label>Địa chỉ nhận hàng *</label>
        <div class="input-box textarea-box">
          <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
          <textarea v-model="form.shippingAddress" @input="validateAddress" maxlength="50" placeholder="Số nhà, tên đường, phường/xã..."></textarea>
        </div>
      </div>

      <div class="form-group">
        <label>Ghi chú đơn hàng (Tùy chọn)</label>
        <div class="input-box textarea-box">
          <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          <textarea v-model="form.note" @input="validateNote" maxlength="50" placeholder="Giao hàng trong giờ hành chính..."></textarea>
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
            <input type="checkbox" v-model="form.requireVat">
            <span class="slider round"></span>
          </label>
        </div>

        <div class="vat-form-box" v-if="form.requireVat">
          <h4 class="vat-form-title">Thông tin doanh nghiệp</h4>
          <div class="form-row">
            <div class="form-group half"><label>Mã số thuế *</label><div class="input-box"><input type="text" v-model="form.vatTaxCode" placeholder="Nhập mã số thuế" /></div></div>
            <div class="form-group half"><label>Email nhận hóa đơn *</label><div class="input-box"><input type="email" v-model="form.vatEmail" placeholder="accounting@company.com" /></div></div>
          </div>
          <div class="form-group"><label>Tên công ty / đơn vị *</label><div class="input-box"><input type="text" v-model="form.vatCompanyName" placeholder="Công ty TNHH..." /></div></div>
          <div class="form-group mb-0"><label>Địa chỉ công ty (Theo GPKD) *</label><div class="input-box"><input type="text" v-model="form.vatCompanyAddress" placeholder="Số nhà, tên đường..." /></div></div>
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
        <div class="radio-wrapper"><input type="radio" name="payment" value="COD" v-model="form.paymentMethod" /><span class="custom-radio"></span></div>
        <div class="option-info"><strong>Thanh toán khi nhận hàng (COD)</strong><span>Thanh toán bằng tiền mặt khi shipper giao tới</span></div>
        <svg class="option-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/><path d="M6 12h.01M18 12h.01"/></svg>
      </label>
      <label class="payment-option">
        <div class="radio-wrapper"><input type="radio" name="payment" value="VNPAY" v-model="form.paymentMethod" /><span class="custom-radio"></span></div>
        <div class="option-info"><strong>Thanh toán trực tuyến qua VNPay</strong><span>Thanh toán an toàn qua thẻ ATM/Internet Banking</span></div>
        <span class="vnpay-text"><span class="red">VN</span><span class="blue">PAY</span></span>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ form: any }>();

const validateName = () => { props.form.customerName = props.form.customerName.replace(/[^\p{L}\s]/gu, '').replace(/^\s+/, ''); };
const validatePhone = () => { props.form.customerPhone = props.form.customerPhone.replace(/[^\d]/g, ''); };
const validateAddress = () => { props.form.shippingAddress = props.form.shippingAddress.replace(/[^\p{L}\d\s]/gu, '').replace(/^\s+/, ''); };
const validateNote = () => { props.form.note = props.form.note.replace(/[^\p{L}\d\s]/gu, '').replace(/^\s+/, ''); };
</script>

<style scoped>
.checkout-left { flex: 2; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 40px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
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
.vat-section-wrapper { margin-top: 25px; margin-bottom: 20px; }
.vat-toggle-box { display: flex; justify-content: space-between; align-items: center; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px 20px; background: white; }
.vat-toggle-info { display: flex; flex-direction: column; gap: 4px; }
.vat-toggle-title { display: flex; align-items: center; gap: 8px; color: #1a202c; }
.icon-receipt { width: 18px; height: 18px; color: #4a5568; }
.vat-sub { font-size: 13px; color: #718096; }
.switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #cbd5e0; transition: .4s; }
.slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .4s; }
input:checked + .slider { background-color: #3182ce; }
input:checked + .slider:before { transform: translateX(20px); }
.slider.round { border-radius: 24px; }
.slider.round:before { border-radius: 50%; }
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
</style>