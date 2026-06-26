<script setup lang="ts">
const formData = defineModel<{
  fullName: string;
  phone: string;
  address: string;
  paymentMethod: string;
}>({ required: true });
</script>

<template>
  <div class="d-flex flex-column gap-4">
    <div class="card shadow-sm border-light">
      <div class="card-body p-4">
        <h5 class="card-title fw-bold mb-4 d-flex align-items-center gap-2">
          <span class="badge bg-dark rounded-circle d-flex align-items-center justify-center p-0 text-center" style="width: 24px; height: 24px; font-size: 12px; line-height: 24px;">1</span>
          Thông tin nhận hàng
        </h5>
        
        <div class="d-flex flex-column gap-3">
          <div>
            <label class="form-label small text-muted fw-semibold">Họ và tên</label>
            <input type="text" v-model="formData.fullName" class="form-control form-control-lg fs-6" placeholder="Nhập họ và tên người nhận" />
          </div>
          <div>
            <label class="form-label small text-muted fw-semibold">Số điện thoại</label>
            <input type="text" v-model="formData.phone" class="form-control form-control-lg fs-6" placeholder="Nhập số điện thoại liên hệ" />
          </div>
          <div>
            <label class="form-label small text-muted fw-semibold">Địa chỉ nhận hàng</label>
            <textarea v-model="formData.address" class="form-control form-control-lg fs-6" rows="3" placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành phố..."></textarea>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm border-light">
      <div class="card-body p-4">
        <h5 class="card-title fw-bold mb-4 d-flex align-items-center gap-2">
          <span class="badge bg-dark rounded-circle d-flex align-items-center justify-center p-0 text-center" style="width: 24px; height: 24px; font-size: 12px; line-height: 24px;">2</span>
          Phương thức thanh toán
        </h5>

        <div class="d-flex flex-column gap-2">
          <div class="form-check p-3 border rounded border-2 position-relative cursor-pointer transition" 
               :class="formData.paymentMethod === 'COD' ? 'border-dark bg-light' : 'border-light'">
            <input class="form-check-input ms-0 me-3 position-absolute top-50 translate-middle-y" style="left: 15px;"
                   type="radio" v-model="formData.paymentMethod" id="payCod" value="COD" />
            <label class="form-check-label w-full cursor-pointer" for="payCod" style="padding-left: 25px;">
              <span class="d-block fw-bold text-dark">Thanh toán khi nhận hàng (COD)</span>
              <span class="text-muted small">Thanh toán bằng tiền mặt trực tiếp cho shipper khi nhận nước hoa</span>
            </label>
          </div>

          <div class="form-check p-3 border rounded border-2 position-relative cursor-pointer transition" 
               :class="formData.paymentMethod === 'VNPAY' ? 'border-dark bg-light' : 'border-light'">
            <input class="form-check-input ms-0 me-3 position-absolute top-50 translate-middle-y" style="left: 15px;"
                   type="radio" v-model="formData.paymentMethod" id="payVnpay" value="VNPAY" />
            <label class="form-check-label w-full cursor-pointer d-flex justify-content-between align-items-center" for="payVnpay" style="padding-left: 25px;">
              <div>
                <span class="d-block fw-bold text-dark">Thanh toán trực tuyến qua VNPay</span>
                <span class="text-muted small">Thanh toán quét mã QR an toàn, nhanh chóng qua ứng dụng Ngân hàng</span>
              </div>
              <span class="badge bg-danger p-2 text-uppercase fw-bold text-white" style="font-size: 10px;">VNPay</span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>