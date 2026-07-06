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
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>

            <input
              type="text"
              v-model="form.customerName"
              @input="validateName"
              maxlength="100"
              placeholder="Ví dụ: Nguyễn Văn An"
              autocomplete="name"
            />
          </div>
          <small class="field-hint">Từ 2 đến 100 ký tự, chỉ nhập chữ và khoảng trắng.</small>
        </div>

        <div class="form-group half">
          <label>Số điện thoại *</label>
          <div class="input-box">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/>
            </svg>

            <input
              type="tel"
              v-model="form.customerPhone"
              @input="validatePhone"
              maxlength="10"
              placeholder="Ví dụ: 0987654321"
              autocomplete="tel"
            />
          </div>
          <small class="field-hint">Đúng 10 số và bắt đầu bằng 0.</small>
        </div>
      </div>

      <div class="form-group">
        <label>Địa chỉ nhận hàng *</label>
        <div class="input-box textarea-box">
          <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>

          <textarea
            v-model="form.shippingAddress"
            @input="validateAddress"
            maxlength="500"
            placeholder="Số nhà, ngõ, đường, phường/xã, quận/huyện, tỉnh/thành phố..."
            autocomplete="street-address"
          ></textarea>
        </div>
        <small class="field-hint">Từ 5 đến 500 ký tự. Có thể nhập dấu phẩy, dấu chấm, gạch ngang, dấu /.</small>
      </div>

      <div class="form-group">
        <label>Ghi chú đơn hàng</label>
        <div class="input-box textarea-box">
          <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
          </svg>

          <textarea
            v-model="form.note"
            @input="validateNote"
            maxlength="255"
            placeholder="Ví dụ: Giao hàng trong giờ hành chính..."
          ></textarea>
        </div>
        <small class="field-hint">Không bắt buộc, tối đa 255 ký tự.</small>
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
          <input type="radio" name="payment" value="COD" v-model="form.paymentMethod" />
          <span class="custom-radio"></span>
        </div>

        <div class="option-info">
          <strong>Thanh toán khi nhận hàng (COD)</strong>
          <span>Thanh toán bằng tiền mặt khi shipper giao tới</span>
        </div>

        <svg class="option-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="2" y="6" width="20" height="12" rx="2"/>
          <circle cx="12" cy="12" r="2"/>
          <path d="M6 12h.01M18 12h.01"/>
        </svg>
      </label>

      <label class="payment-option">
        <div class="radio-wrapper">
          <input type="radio" name="payment" value="VNPAY" v-model="form.paymentMethod" />
          <span class="custom-radio"></span>
        </div>

        <div class="option-info">
          <strong>Thanh toán trực tuyến qua VNPay</strong>
          <span>Thanh toán an toàn qua thẻ ATM/Internet Banking</span>
        </div>

        <span class="vnpay-text">
          <span class="red">VN</span><span class="blue">PAY</span>
        </span>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  form: {
    customerName: string;
    customerPhone: string;
    shippingAddress: string;
    note: string;
    paymentMethod: string;
  };
}>();

const collapseSpaces = (value: string) => {
  return String(value || "").replace(/\s{2,}/g, " ");
};

const validateName = () => {
  props.form.customerName = collapseSpaces(
    props.form.customerName.replace(/[^\p{L}\s]/gu, "")
  ).replace(/^\s+/, "");
};

const validatePhone = () => {
  props.form.customerPhone = String(props.form.customerPhone || "")
    .replace(/[^\d]/g, "")
    .slice(0, 10);
};

const validateAddress = () => {
  props.form.shippingAddress = collapseSpaces(
    String(props.form.shippingAddress || "")
      .replace(/[^\p{L}\d\s,./#()\-]/gu, "")
      .replace(/^\s+/, "")
  );
};

const validateNote = () => {
  props.form.note = collapseSpaces(
    String(props.form.note || "").replace(/^\s+/, "")
  ).slice(0, 255);
};
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
.form-group.half { flex: 1; }
.form-group label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.field-hint { display: block; margin-top: 6px; color: #718096; font-size: 12px; }
.input-box { display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; background: white; transition: 0.2s; }
.input-box:focus-within { border-color: #06132b; }
.textarea-box { align-items: flex-start; padding-top: 12px; }
.input-icon { width: 18px; height: 18px; color: #a0aec0; margin-right: 10px; flex-shrink: 0; }
.input-box input { flex: 1; border: none; padding: 14px 0; outline: none; font-size: 14px; color: #333; }
.input-box textarea { flex: 1; border: none; padding: 0 0 14px 0; outline: none; font-size: 14px; color: #333; min-height: 80px; resize: none; font-family: inherit; }
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
.vnpay-text { font-weight: 800; font-size: 18px; letter-spacing: -0.5px; }
.vnpay-text .red { color: #e53e3e; }
.vnpay-text .blue { color: #3182ce; }

@media (max-width: 768px) {
  .checkout-left { padding: 24px; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>