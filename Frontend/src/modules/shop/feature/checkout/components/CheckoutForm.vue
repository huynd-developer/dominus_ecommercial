<template>
  <div class="checkout-left">
    <div class="step-section">
      <div class="step-header">
        <span class="step-num">1</span>
        <h2>Thông tin nhận hàng</h2>
      </div>

      <div v-if="form.profileLoaded" class="profile-filled-box">
        <div>
          <strong>Thông tin tài khoản đã được tự động điền</strong>
          <span>Bạn có thể thay đổi số điện thoại hoặc địa chỉ nhận hàng cho đơn này.</span>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group half">
          <label>Họ và tên <span class="text-danger">*</span></label>
          <div class="input-box">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <input type="text" v-model="form.customerName" @input="validateName" maxlength="100"
              placeholder="Ví dụ: Nguyễn Văn An" autocomplete="name" />
          </div>
          <small class="field-hint">Từ 2 đến 100 ký tự, chỉ nhập chữ và khoảng trắng.</small>
        </div>

        <div class="form-group half">
          <label>Số điện thoại <span class="text-danger">*</span></label>
          <div class="input-box">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path
                d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z" />
            </svg>
            <input type="tel" v-model="form.customerPhone" @input="validatePhone" maxlength="10"
              placeholder="Ví dụ: 0987654321" autocomplete="tel" />
          </div>
          <small class="field-hint">Đúng 10 số và bắt đầu bằng 0.</small>
        </div>
      </div>

      <!-- KHỐI ĐỊA CHỈ NHẬN HÀNG (DÙNG BẢNG CUSTOMER_ADDRESS) -->
      <div class="address-box">
        <div class="d-flex justify-content-between align-items-end mb-3">
          <div class="address-title mb-0">
            <strong>Địa chỉ nhận hàng <span class="text-danger">*</span></strong>
          </div>
          <button v-if="!isAddingNewAddress" type="button" class="btn-add-address" @click="openAddMode">
            <i class="bi bi-plus-circle me-1"></i> Thêm địa chỉ mới
          </button>
        </div>

        <div v-if="addressLoadError" class="address-error mb-3">
          {{ addressLoadError }}
        </div>

        <!-- DANH SÁCH ĐỊA CHỈ ĐÃ LƯU -->
        <div v-if="!isAddingNewAddress" class="address-list">
          <div v-if="addressList.length === 0" class="text-muted text-center py-3 border rounded bg-light small">
            Bạn chưa lưu địa chỉ nào. Vui lòng thêm địa chỉ mới.
          </div>
          <label v-for="addr in addressList" :key="addr.id" class="address-card" :class="{ active: selectedAddressId === addr.id }">
            <input type="radio" name="selectedAddress" :value="addr.id" v-model="selectedAddressId" @change="applySelectedAddress(addr)" class="d-none">
            
            <div class="address-card-inner">
              <div class="d-flex justify-content-between align-items-start w-100">
                <div class="flex-grow-1">
                  <div class="d-flex align-items-center mb-1">
                    <i class="bi bi-geo-alt-fill text-danger me-2"></i>
                    <span class="fw-bold text-dark">{{ addr.recipientName || form.customerName || 'Người nhận' }}</span>
                    <span class="text-muted mx-2">|</span>
                    <span class="text-muted">{{ addr.phone || form.customerPhone || 'SĐT' }}</span>
                  </div>
                  
                  <div class="text-secondary small ms-4 mb-2" style="line-height: 1.5;">
                    {{ addr.fullAddress }}
                  </div>
                  <div class="d-flex gap-2 ms-4">
                    <span v-if="addr.isDefault" class="badge bg-danger bg-opacity-10 text-danger border border-danger">Mặc định</span>
                  </div>
                </div>

                <!-- NÚT SỬA VÀ XÓA -->
                <div class="d-flex flex-column gap-2 ms-3">
                  <button type="button" class="btn btn-sm btn-outline-primary px-3" @click.stop="editAddress(addr)">Sửa</button>
                  <button type="button" class="btn btn-sm btn-outline-danger px-3" @click.stop="removeAddress(addr.id)">Xóa</button>
                </div>
              </div>
            </div>
          </label>
        </div>

        <!-- FORM THÊM / SỬA ĐỊA CHỈ MỚI -->
        <div v-if="isAddingNewAddress" class="address-editor">
          <h6 class="mb-3 fw-bold" style="color: #06132b;">{{ editingAddressId ? 'Sửa địa chỉ giao hàng' : 'Thêm địa chỉ giao hàng mới' }}</h6>
          
          <div class="form-row">
            <div class="form-group half">
              <label>Tỉnh / Thành phố <span class="text-danger">*</span></label>
              <div class="select-box">
                <select v-model="selectedProvinceCode" @change="handleProvinceChange" :disabled="loadingProvinces">
                  <option value="">{{ loadingProvinces ? "Đang tải tỉnh/thành..." : "Chọn tỉnh/thành phố" }}</option>
                  <option v-for="province in provinces" :key="province.code" :value="province.code">{{ province.name }}</option>
                </select>
              </div>
            </div>

            <div class="form-group half">
              <label>Phường / Xã <span class="text-danger">*</span></label>
              <div class="select-box">
                <select v-model="selectedWardCode" @change="syncFullAddress" :disabled="!selectedProvinceCode || loadingWards">
                  <option value="">{{ !selectedProvinceCode ? "Chọn tỉnh/thành trước" : loadingWards ? "Đang tải phường / xã..." : "Chọn phường/xã" }}</option>
                  <option v-for="ward in wards" :key="ward.code" :value="ward.code">{{ ward.name }}</option>
                </select>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>Địa chỉ cụ thể <span class="text-danger">*</span></label>
            <div class="input-box textarea-box">
              <textarea v-model="specificAddress" @input="validateSpecificAddress" maxlength="255"
                placeholder="Ví dụ: Số 12/5, ngõ 36-A, đường Trần Phú"></textarea>
            </div>
            <small class="field-hint">Nhập số nhà, ngõ, đường, tòa nhà.</small>
          </div>

          <div class="form-check mb-3">
            <input class="form-check-input" type="checkbox" v-model="isDefaultAddress" id="chkDefault" />
            <label class="form-check-label" for="chkDefault">Đặt làm địa chỉ mặc định</label>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-2">
            <button v-if="addressList.length > 0" type="button" class="btn btn-outline-secondary px-4 py-2 fw-bold rounded-3" @click="cancelAddMode">
              Hủy
            </button>
            <button type="button" class="btn btn-primary px-4 py-2 fw-bold rounded-3" style="background-color: #06132b;" @click="saveAddress">
              Lưu địa chỉ
            </button>
          </div>
        </div>
      </div>
      <!-- END KHỐI ĐỊA CHỈ -->

      <div class="form-group">
        <label>Ghi chú đơn hàng</label>
        <div class="input-box textarea-box">
          <textarea v-model="form.note" maxlength="255" placeholder="Ví dụ: Giao hàng trong giờ hành chính..."></textarea>
        </div>
        <small class="field-hint">Không bắt buộc, tối đa 255 ký tự.</small>
      </div>

    </div>

    <div class="divider"></div>

    <!-- BƯỚC 2: PHƯƠNG THỨC THANH TOÁN -->
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
          <rect x="2" y="6" width="20" height="12" rx="2" />
          <circle cx="12" cy="12" r="2" />
          <path d="M6 12h.01M18 12h.01" />
        </svg>
      </label>

      <label class="payment-option">
        <div class="radio-wrapper">
          <input type="radio" name="payment" value="VIETQR" v-model="form.paymentMethod" />
          <span class="custom-radio"></span>
        </div>
        <div class="option-info">
          <strong>Chuyển khoản VietQR</strong>
          <span>Quét mã QR qua ứng dụng ngân hàng (Miễn phí)</span>
        </div>
        <svg class="option-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 7V4h3M17 4h3v3M4 17v3h3M17 20h3v-3" />
          <rect x="7" y="7" width="4" height="4" rx="0.5" />
          <rect x="13" y="7" width="4" height="4" rx="0.5" />
          <rect x="7" y="13" width="4" height="4" rx="0.5" />
          <path d="M13 13h2v2h-2zM15 15h2v2h-2z" fill="currentColor"/>
        </svg>
      </label>

      <label class="payment-option">
        <div class="radio-wrapper">
          <input type="radio" name="payment" value="VNPAY" v-model="form.paymentMethod" />
          <span class="custom-radio"></span>
        </div>
        <div class="option-info">
          <strong>Thanh toán qua VNPay</strong>
          <span>Thanh toán an toàn bằng thẻ ATM/Nội địa hoặc Internet Banking</span>
        </div>
        <svg class="option-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="5" width="20" height="14" rx="2" />
          <path d="M2 10h20" />
          <path d="M6 15h2.01M10 15h.01" />
        </svg>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import Swal from "sweetalert2";
import customerAddressService from "@/modules/customerAddress/services/customerAddress.service";

interface Ward { code: number | string; name: string; }
interface Province { code: number | string; name: string; wards?: Ward[] | null; }

const props = defineProps<{
  form: {
    customerName: string; 
    customerPhone: string; 
    shippingAddress: string; 
    note: string; 
    paymentMethod: string;
    provinceName?: string; 
    wardName?: string; 
    specificAddress?: string;
    profileLoaded?: boolean; 
    customerId?: number | null;
  };
}>();

const provinces = ref<Province[]>([]);
const wards = ref<Ward[]>([]);
const selectedProvinceCode = ref<string>("");
const selectedWardCode = ref<string>("");
const specificAddress = ref("");
const isDefaultAddress = ref(false);

const loadingProvinces = ref(false);
const loadingWards = ref(false);
const addressLoadError = ref("");

const addressList = ref<any[]>([]);
const selectedAddressId = ref<number | null>(null);
const isAddingNewAddress = ref(false);
const editingAddressId = ref<number | null>(null);

const validCustomerId = computed(() => props.form.customerId ? Number(props.form.customerId) : 0);

const selectedProvince = computed(() => provinces.value.find((item) => String(item.code) === String(selectedProvinceCode.value)));
const selectedWard = computed(() => wards.value.find((item) => String(item.code) === String(selectedWardCode.value)));

const syncFullAddress = () => {
  const provinceName = selectedProvince.value?.name || "";
  const wardName = selectedWard.value?.name || "";
  const addressDetail = specificAddress.value || "";

  props.form.provinceName = provinceName;
  props.form.wardName = wardName;
  props.form.specificAddress = addressDetail;

  const parts = [addressDetail, wardName, provinceName].map((item) => String(item || "").trim()).filter(Boolean);
  props.form.shippingAddress = parts.join(", ");
};

const validateName = () => { props.form.customerName = String(props.form.customerName || "").slice(0, 100); };
const validatePhone = () => { props.form.customerPhone = String(props.form.customerPhone || "").replace(/[^\d]/g, "").slice(0, 10); };
const validateSpecificAddress = () => { specificAddress.value = String(specificAddress.value || "").slice(0, 255); syncFullAddress(); };

const applySelectedAddress = (addr: any) => {
  if (!addr) return;
  if (addr.recipientName) props.form.customerName = addr.recipientName;
  if (addr.phone) props.form.customerPhone = addr.phone;
  props.form.shippingAddress = addr.fullAddress || "";
};

const handleProvinceChange = async () => {
  selectedWardCode.value = "";
  wards.value = [];
  syncFullAddress();
  if (selectedProvinceCode.value) await loadWardsByProvince(selectedProvinceCode.value);
};

const openAddMode = () => {
  editingAddressId.value = null;
  selectedProvinceCode.value = "";
  selectedWardCode.value = "";
  specificAddress.value = "";
  isDefaultAddress.value = false;
  isAddingNewAddress.value = true;
};

const editAddress = async (addr: any) => {
  editingAddressId.value = addr.id;
  selectedProvinceCode.value = addr.provinceCode ? String(addr.provinceCode) : "";
  if (selectedProvinceCode.value) {
    await loadWardsByProvince(selectedProvinceCode.value);
  }
  selectedWardCode.value = addr.wardCode ? String(addr.wardCode) : "";
  specificAddress.value = addr.specificAddress || "";
  isDefaultAddress.value = Boolean(addr.isDefault);
  isAddingNewAddress.value = true;
};

const removeAddress = async (id: number) => {
  const result = await Swal.fire({ title: "Xóa địa chỉ?", icon: "warning", showCancelButton: true, confirmButtonText: "Xác nhận", cancelButtonText: "Hủy" });
  if (result.isConfirmed) {
    try {
      await customerAddressService.deleteAddress(validCustomerId.value, id);
      addressList.value = addressList.value.filter(a => a.id !== id);
      if (selectedAddressId.value === id && addressList.value.length > 0) {
        selectedAddressId.value = addressList.value[0].id;
        applySelectedAddress(addressList.value[0]);
      } else if (addressList.value.length === 0) {
        isAddingNewAddress.value = true;
      }
    } catch (e) {
      console.error(e);
    }
  }
};

const cancelAddMode = () => {
  isAddingNewAddress.value = false;
  editingAddressId.value = null;
  addressLoadError.value = "";
  if (addressList.value.length > 0 && !selectedAddressId.value) {
    selectedAddressId.value = addressList.value[0].id;
    applySelectedAddress(addressList.value[0]);
  }
};

const saveAddress = async () => {
  if (!selectedProvinceCode.value || !selectedWardCode.value || !specificAddress.value.trim()) {
    addressLoadError.value = "Vui lòng chọn đầy đủ Tỉnh/Thành, Phường/Xã và Địa chỉ cụ thể.";
    return;
  }
  addressLoadError.value = "";
  syncFullAddress();

  const payload = {
    customerId: validCustomerId.value,
    recipientName: props.form.customerName,
    phone: props.form.customerPhone,
    provinceCode: selectedProvinceCode.value,
    provinceName: selectedProvince.value?.name || "",
    wardCode: selectedWardCode.value,
    wardName: selectedWard.value?.name || "",
    specificAddress: specificAddress.value.trim(),
    fullAddress: props.form.shippingAddress,
    isDefault: isDefaultAddress.value
  };

  try {
    if (editingAddressId.value) {
      await customerAddressService.updateAddress(validCustomerId.value, editingAddressId.value, payload);
    } else {
      await customerAddressService.addAddress(validCustomerId.value, payload);
    }

    const res = await customerAddressService.getAddresses(validCustomerId.value);
    addressList.value = Array.isArray(res.data) ? res.data : [];
    
    if (addressList.value.length > 0) {
      const target = editingAddressId.value 
        ? addressList.value.find(a => a.id === editingAddressId.value) 
        : (addressList.value.find(a => a.isDefault) || addressList.value[addressList.value.length - 1]);
      
      if (target) {
        selectedAddressId.value = target.id;
        applySelectedAddress(target);
      }
    }

    isAddingNewAddress.value = false;
    editingAddressId.value = null;
    Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Đã lưu địa chỉ!', showConfirmButton: false, timer: 1500 });
  } catch (error: any) {
    addressLoadError.value = "Lỗi đồng bộ dữ liệu địa chỉ!";
  }
};

const loadProvinces = async () => {
  try {
    loadingProvinces.value = true;
    const response = await fetch(`https://provinces.open-api.vn/api/p/`);
    const data = await response.json();
    provinces.value = data.map((item: any) => ({ code: item.code, name: item.name }));
  } catch (error) {
    provinces.value = [];
  } finally {
    loadingProvinces.value = false;
  }
};

const loadWardsByProvince = async (provinceCode: string) => {
  try {
    loadingWards.value = true;
    wards.value = [];
    const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=3`);
    const data = await response.json();
    let allWards: any[] = [];
    if (data && data.districts) {
      data.districts.forEach((d: any) => {
        if (d.wards) {
          d.wards.forEach((w: any) => {
            allWards.push({ code: w.code, name: `${w.name}, ${d.name}` });
          });
        }
      });
    }
    wards.value = allWards;
  } catch (error) {
    wards.value = [];
  } finally {
    loadingWards.value = false;
  }
};

const fetchCustomerAddresses = async () => {
  if (!validCustomerId.value) return;
  try {
    const res = await customerAddressService.getAddresses(validCustomerId.value);
    addressList.value = Array.isArray(res.data) ? res.data : [];
    if (addressList.value.length > 0) {
      const def = addressList.value.find(a => a.isDefault) || addressList.value[0];
      selectedAddressId.value = def.id;
      applySelectedAddress(def);
      isAddingNewAddress.value = false;
    } else {
      isAddingNewAddress.value = true;
    }
  } catch (e) {
    isAddingNewAddress.value = true;
  }
};

watch(() => props.form.customerId, async (newId) => {
  if (newId) {
    await fetchCustomerAddresses();
  }
}, { immediate: true });

onMounted(async () => {
  await loadProvinces();
  if (validCustomerId.value) {
    await fetchCustomerAddresses();
  }
});
</script>

<style scoped>
.checkout-left { flex: 2; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 40px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03); }
.step-section { margin-bottom: 10px; }
.step-header { display: flex; align-items: center; gap: 12px; margin-bottom: 25px; }
.step-num { background: #06132b; color: white; width: 26px; height: 26px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; }
.step-header h2 { font-size: 20px; color: #06132b; margin: 0; position: relative; }
.step-header h2::after { content: ""; position: absolute; bottom: -6px; left: 0; width: 30px; height: 2px; background: #b78d52; }
.profile-filled-box { display: flex; padding: 14px 16px; border: 1px solid #bbf7d0; background: #f0fdf4; border-radius: 10px; margin-bottom: 22px; }
.profile-filled-box strong { display: block; color: #166534; font-size: 14px; margin-bottom: 4px; }
.profile-filled-box span { color: #15803d; font-size: 13px; }
.form-row { display: flex; gap: 20px; }
.form-group { margin-bottom: 20px; width: 100%; }
.form-group.half { flex: 1; }
.form-group label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.field-hint { display: block; margin-top: 6px; color: #718096; font-size: 12px; }
.input-box, .select-box { display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; background: white; transition: 0.2s; }
.input-box:focus-within, .select-box:focus-within { border-color: #06132b; }
.select-box select { width: 100%; border: none; outline: none; padding: 14px 0; font-size: 14px; background: transparent; cursor: pointer; }
.textarea-box { align-items: flex-start; padding-top: 12px; }
.input-icon { width: 18px; height: 18px; color: #a0aec0; margin-right: 10px; flex-shrink: 0; }
.input-box input, .textarea-box textarea { flex: 1; border: none; outline: none; font-size: 14px; color: #333; }
.textarea-box textarea { min-height: 80px; resize: none; font-family: inherit; width: 100%; padding: 0; }

.address-box { border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px; margin-bottom: 20px; background: #fcfcfd; }
.address-title { display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px; }
.address-title strong { color: #06132b; font-size: 15px; }

.btn-add-address { background: transparent; border: none; color: #bd9a5f; font-weight: 700; font-size: 14px; cursor: pointer; transition: 0.2s; }
.btn-add-address:hover { color: #06132b; text-decoration: underline; }
.address-list { display: flex; flex-direction: column; gap: 12px; }
.address-card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px; cursor: pointer; transition: all 0.2s ease; background: #ffffff; display: block; margin-bottom: 0; }
.address-card:hover { border-color: #bd9a5f; }
.address-card.active { border-color: #bd9a5f; background: #fefaf4; box-shadow: 0 0 0 1px #bd9a5f; }
.address-error { padding: 10px 12px; background: #fff1f2; color: #b91c1c; border: 1px solid #fecdd3; border-radius: 8px; font-size: 13px; }

.divider { height: 1px; background: #f0f0f0; margin: 30px 0; }

.payment-option { display: flex; align-items: center; gap: 15px; padding: 20px; border: 1px solid #ddd; border-radius: 8px; margin-bottom: 15px; cursor: pointer; transition: 0.2s; }
.payment-option:hover, .payment-option:has(input:checked) { border-color: #b78d52; background: #fdfaf6; }
.radio-wrapper { position: relative; width: 20px; height: 20px; flex-shrink: 0; }
.radio-wrapper input { opacity: 0; position: absolute; cursor: pointer; }
.custom-radio { position: absolute; top: 0; left: 0; height: 20px; width: 20px; background: #fff; border: 2px solid #ddd; border-radius: 50%; }
.radio-wrapper input:checked ~ .custom-radio { border-color: #06132b; }
.custom-radio:after { content: ""; position: absolute; display: none; top: 4px; left: 4px; width: 8px; height: 8px; border-radius: 50%; background: #06132b; }
.radio-wrapper input:checked ~ .custom-radio:after { display: block; }
.option-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.option-info strong { color: #333; font-size: 15px; }
.option-info span { color: #777; font-size: 13px; }

.option-icon { width: 32px !important; height: 32px !important; min-width: 32px !important; color: #b78d52; flex-shrink: 0 !important; }

@media (max-width: 768px) {
  .checkout-left { padding: 24px; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>