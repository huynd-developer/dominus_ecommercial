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

      <!-- KHỐI ĐỊA CHỈ ĐƯỢC THIẾT KẾ LẠI CHUẨN SHOPEE/LAZADA -->
      <div class="address-box">
        <div class="d-flex justify-content-between align-items-end mb-3">
          <div class="address-title mb-0">
            <strong>Địa chỉ nhận hàng <span class="text-danger">*</span></strong>
          </div>
          <button v-if="!isAddingNewAddress" type="button" class="btn-add-address" @click="isAddingNewAddress = true">
            <i class="bi bi-plus-circle me-1"></i> Thêm địa chỉ mới
          </button>
        </div>

        <div v-if="addressLoadError" class="address-error mb-3">
          {{ addressLoadError }}
        </div>

        <!-- DANH SÁCH ĐỊA CHỈ -->
        <div v-if="!isAddingNewAddress" class="address-list">
          <label v-for="(addr, index) in parsedProfileAddresses" :key="index" class="address-card" :class="{ active: selectedProfileAddressIndex === String(index) }">
            <input type="radio" name="selectedAddress" v-model="selectedProfileAddressIndex" :value="String(index)" @change="applySelectedProfileAddress" class="d-none">
            
            <div class="address-card-inner">
              <div class="d-flex justify-content-between">
                <div class="d-flex align-items-center mb-1">
                  <i class="bi bi-geo-alt-fill text-danger me-2"></i>
                  <span class="fw-bold text-dark">{{ addr.name || form.customerName || 'Người nhận' }}</span>
                  <span class="text-muted mx-2">|</span>
                  <span class="text-muted">{{ addr.phone || form.customerPhone || 'SĐT' }}</span>
                </div>
                
                <div class="d-flex gap-2">
                  <span v-if="index === 0 && !addr.isNew" class="badge bg-danger bg-opacity-10 text-danger border border-danger">Mặc định</span>
                  <span v-if="addr.isNew" class="badge bg-success bg-opacity-10 text-success border border-success">Mới thêm</span>
                </div>
              </div>
              <div class="text-secondary small ms-4" style="line-height: 1.5;">
                {{ addr.fullAddress }}
              </div>
            </div>
          </label>
        </div>

        <!-- FORM THÊM ĐỊA CHỈ MỚI -->
        <div v-if="isAddingNewAddress" class="address-editor">
          <h6 class="mb-3 fw-bold" style="color: #06132b;">Thêm địa chỉ giao hàng mới</h6>
          
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
              <label>Phường / Xã / Đặc khu <span class="text-danger">*</span></label>
              <div class="select-box">
                <select v-model="selectedWardCode" @change="syncFullAddress" :disabled="!selectedProvinceCode || loadingWards">
                  <option value="">{{ !selectedProvinceCode ? "Chọn tỉnh/thành trước" : loadingWards ? "Đang tải phường / xã..." : "Chọn phường/xã/đặc khu" }}</option>
                  <option v-for="ward in wards" :key="ward.code" :value="ward.code">{{ ward.name }}</option>
                </select>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>Địa chỉ cụ thể <span class="text-danger">*</span></label>
            <div class="input-box textarea-box">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              <textarea v-model="specificAddress" @input="validateSpecificAddress" :maxlength="MAX_SHIPPING_ADDRESS_LENGTH"
                placeholder="Ví dụ: Số 12/5, ngõ 36-A, đường Trần Phú" autocomplete="street-address"></textarea>
            </div>
            <small class="field-hint">Nhập số nhà, ngõ, đường, tòa nhà. Tổng địa chỉ gửi hệ thống tối đa 200 ký tự.</small>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-2">
            <button v-if="parsedProfileAddresses.length > 0" type="button" class="btn btn-outline-secondary px-4 py-2 fw-bold rounded-3" @click="cancelAddNewAddress">
              Hủy
            </button>
            <button type="button" class="btn btn-primary px-4 py-2 fw-bold rounded-3" style="background-color: #06132b; border-color: #06132b;" @click="saveNewAddress">
              Lưu địa chỉ
            </button>
          </div>
        </div>
      </div>
      <!-- END KHỐI ĐỊA CHỈ -->

      <div class="form-group">
        <label>Ghi chú đơn hàng</label>
        <div class="input-box textarea-box">
          <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
          <textarea v-model="form.note" @input="validateNote" maxlength="255"
            placeholder="Ví dụ: Giao hàng trong giờ hành chính..."></textarea>
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
import { computed, onMounted, ref } from "vue";
import { useRouter, onBeforeRouteLeave } from "vue-router"; 

interface Ward { code: number | string; name: string; }
interface Province { code: number | string; name: string; wards?: Ward[] | null; }

const props = defineProps<{
  form: {
    customerName: string; customerPhone: string; shippingAddress: string; note: string; paymentMethod: string;
    provinceName?: string; wardName?: string; specificAddress?: string;
    profileLoaded?: boolean; profileAddress?: string;
  };
}>();

const provinces = ref<Province[]>([]);
const wards = ref<Ward[]>([]);

const selectedProvinceCode = ref<string>("");
const selectedWardCode = ref<string>("");
const specificAddress = ref("");

const loadingProvinces = ref(false);
const loadingWards = ref(false);
const addressLoadError = ref("");

const parsedProfileAddresses = ref<any[]>([]);
const selectedProfileAddressIndex = ref<string>("");
const isAddingNewAddress = ref(false);

const MIN_SHIPPING_ADDRESS_LENGTH = 5;
const MAX_SHIPPING_ADDRESS_LENGTH = 200;

const selectedProvince = computed(() => provinces.value.find((item) => String(item.code) === String(selectedProvinceCode.value)));
const selectedWard = computed(() => wards.value.find((item) => String(item.code) === String(selectedWardCode.value)));

const extractArray = (data: any) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.results)) return data.results;
  if (Array.isArray(data?.wards)) return data.wards;
  return [];
};

const normalizeProvince = (item: any): Province => {
  return {
    code: item.code ?? item.province_code ?? item.provinceCode ?? item.id,
    name: item.name ?? item.province_name ?? item.provinceName ?? "",
    wards: Array.isArray(item.wards) ? item.wards.map(normalizeWard) : null,
  };
};

const normalizeWard = (item: any): Ward => {
  return {
    code: item.code ?? item.ward_code ?? item.wardCode ?? item.id,
    name: item.name ?? item.ward_name ?? item.wardName ?? "",
  };
};

const collapseSpaces = (value: string) => String(value || "").replace(/\s{2,}/g, " ");
const cleanText = (value: string) => collapseSpaces(String(value || "").replace(/^\s+/, ""));
const cleanAddressText = (value: string) => cleanText(value).replace(/[^\p{L}\d\s,./#()\-]/gu, "");

const buildFullAddress = (addressDetail: string, wardName: string, provinceName: string) => {
  return [addressDetail, wardName, provinceName]
    .map((item) => String(item || "").trim())
    .filter(Boolean)
    .join(", ");
};

const getMaxSpecificAddressLength = (wardName: string, provinceName: string) => {
  const locationAddress = buildFullAddress("", wardName, provinceName);
  const separatorLength = locationAddress ? 2 : 0;

  return Math.max(MAX_SHIPPING_ADDRESS_LENGTH - locationAddress.length - separatorLength, 0);
};

const isShippingAddressValid = (address: string) => {
  const length = String(address || "").trim().length;

  return length >= MIN_SHIPPING_ADDRESS_LENGTH && length <= MAX_SHIPPING_ADDRESS_LENGTH;
};

const validateShippingAddressLength = (address: string) => {
  if (!isShippingAddressValid(address)) {
    addressLoadError.value = `Địa chỉ nhận hàng phải từ ${MIN_SHIPPING_ADDRESS_LENGTH} đến ${MAX_SHIPPING_ADDRESS_LENGTH} ký tự.`;
    return false;
  }

  return true;
};

const syncFullAddress = () => {
  const provinceName = selectedProvince.value?.name || "";
  const wardName = selectedWard.value?.name || "";
  const maxSpecificAddressLength = getMaxSpecificAddressLength(wardName, provinceName);
  const addressDetail = cleanAddressText(specificAddress.value).slice(0, maxSpecificAddressLength);

  specificAddress.value = addressDetail;

  props.form.provinceName = provinceName;
  props.form.wardName = wardName;
  props.form.specificAddress = addressDetail;
  props.form.shippingAddress = buildFullAddress(addressDetail, wardName, provinceName);
};

const validateName = () => { props.form.customerName = cleanText(String(props.form.customerName || "").replace(/[^\p{L}\s]/gu, "")).slice(0, 100); };
const validatePhone = () => { props.form.customerPhone = String(props.form.customerPhone || "").replace(/[^\d]/g, "").slice(0, 10); };
const validateSpecificAddress = () => { syncFullAddress(); };
const validateNote = () => { props.form.note = cleanText(props.form.note).slice(0, 255); };

// XỬ LÝ CHỌN ĐỊA CHỈ TỪ DANH SÁCH
const applySelectedProfileAddress = () => {
  if (selectedProfileAddressIndex.value === "") return;

  const addr = parsedProfileAddresses.value[Number(selectedProfileAddressIndex.value)];
  if (addr) {
    const fullAddress = String(addr.fullAddress || "").trim();

    if (!validateShippingAddressLength(fullAddress)) return;

    addressLoadError.value = "";

    if (addr.name) props.form.customerName = addr.name;
    if (addr.phone) props.form.customerPhone = addr.phone;
    props.form.shippingAddress = fullAddress;
    
    // Xóa form nhập tay & xóa state Validation để form check coi đây là địa chỉ "đã lưu"
    selectedProvinceCode.value = "";
    selectedWardCode.value = "";
    specificAddress.value = "";
    wards.value = [];
    
    props.form.provinceName = "";
    props.form.wardName = "";
    props.form.specificAddress = "";
  }
};

const handleProvinceChange = async () => {
  selectedWardCode.value = "";
  wards.value = [];
  syncFullAddress();
  if (selectedProvinceCode.value) await loadWardsByProvince(selectedProvinceCode.value);
};

// HỦY BỎ THÊM MỚI
const cancelAddNewAddress = () => {
  isAddingNewAddress.value = false;
  addressLoadError.value = "";
  if (parsedProfileAddresses.value.length > 0) {
    if (!selectedProfileAddressIndex.value) {
      selectedProfileAddressIndex.value = "0";
    }
    applySelectedProfileAddress();
  }
};

// LƯU ĐỊA CHỈ VÀO DANH SÁCH CHỌN
const saveNewAddress = () => {
  if (!selectedProvinceCode.value || !selectedWardCode.value || !specificAddress.value) {
    addressLoadError.value = "Vui lòng chọn đầy đủ Tỉnh/Thành, Phường/Xã và Địa chỉ cụ thể.";
    return;
  }

  syncFullAddress(); 

  if (!validateShippingAddressLength(props.form.shippingAddress)) return;

  addressLoadError.value = "";
  
  const newAddr = {
    name: props.form.customerName,
    phone: props.form.customerPhone,
    fullAddress: props.form.shippingAddress,
    isNew: true
  };
  
  // Đẩy địa chỉ vừa nhập vào list để chọn luôn
  parsedProfileAddresses.value.push(newAddr);
  selectedProfileAddressIndex.value = String(parsedProfileAddresses.value.length - 1);
  isAddingNewAddress.value = false;
  
  // Xóa các biến nhập tay để trick cho CheckoutView hiểu đây là địa chỉ được chọn từ danh sách 
  props.form.provinceName = "";
  props.form.wardName = "";
  props.form.specificAddress = "";
};

const loadProvinces = async () => {
  try {
    loadingProvinces.value = true;
    addressLoadError.value = "";
    const response = await fetch(`https://provinces.open-api.vn/api/p/`);
    const data = await response.json();
    provinces.value = extractArray(data).map(normalizeProvince).filter((item: Province) => item.code && item.name);
  } catch (error) {
    addressLoadError.value = "Không tải được dữ liệu tỉnh/thành. Vui lòng thử lại sau.";
    provinces.value = [];
  } finally {
    loadingProvinces.value = false;
  }
};

const loadWardsByProvince = async (provinceCode: string) => {
  try {
    loadingWards.value = true;
    addressLoadError.value = "";
    wards.value = [];

    const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=3`);
    const data = await response.json();

    let allWards: any[] = [];
    if (data && data.districts && Array.isArray(data.districts)) {
      data.districts.forEach((district: any) => {
        if (district.wards && Array.isArray(district.wards)) {
          district.wards.forEach((ward: any) => {
            allWards.push({
              code: ward.code,
              name: `${ward.name}, ${district.name}`
            });
          });
        }
      });
    }
    wards.value = allWards;
  } catch (error) {
    addressLoadError.value = "Không tải được dữ liệu phường/xã.";
    wards.value = [];
  } finally {
    loadingWards.value = false;
  }
};

onMounted(async () => {
  specificAddress.value = props.form.specificAddress || "";

  if (props.form.shippingAddress && props.form.shippingAddress.startsWith('[')) {
    props.form.shippingAddress = "";
  }

  // Load danh sách địa chỉ từ Profile Backend bắn về
  try {
    if (props.form.profileAddress && props.form.profileAddress.startsWith('[')) {
      parsedProfileAddresses.value = JSON.parse(props.form.profileAddress);
    } else if (props.form.profileAddress) {
      parsedProfileAddresses.value = [{ fullAddress: props.form.profileAddress }];
    }
  } catch (e) {
    console.error("Lỗi parse địa chỉ:", e);
  }

  await loadProvinces();

  // FIX TRIỆT ĐỂ LỖI MẤT ĐỊA CHỈ:
  if (props.form.shippingAddress) {
    // Đi tìm xem địa chỉ draft có trong list DB trả về không
    const matchIdx = parsedProfileAddresses.value.findIndex(
      (a) => a.fullAddress === props.form.shippingAddress
    );
    
    if (matchIdx !== -1) {
      selectedProfileAddressIndex.value = String(matchIdx);
      isAddingNewAddress.value = false;
    } else {
      // Nếu không có (do vừa nhập tay xong), TỰ ĐỘNG ĐẨY VÀO LIST TRÊN GIAO DIỆN luôn
      parsedProfileAddresses.value.push({
        name: props.form.customerName,
        phone: props.form.customerPhone,
        fullAddress: props.form.shippingAddress,
        isNew: true
      });
      selectedProfileAddressIndex.value = String(parsedProfileAddresses.value.length - 1);
      isAddingNewAddress.value = false;
    }
  } else if (parsedProfileAddresses.value.length > 0) {
    // Lần đầu vào trang, chọn mặc định cái đầu tiên
    selectedProfileAddressIndex.value = "0";
    applySelectedProfileAddress();
    isAddingNewAddress.value = false;
  } else {
    // Chưa có địa chỉ nào bao giờ thì mở form thêm mới
    isAddingNewAddress.value = true;
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
.form-group.mb-0 { margin-bottom: 0; }
.form-group label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; font-weight: 500; }
.field-hint { display: block; margin-top: 6px; color: #718096; font-size: 12px; }
.input-box, .select-box { display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; background: white; transition: 0.2s; }
.input-box:focus-within, .select-box:focus-within { border-color: #06132b; }
.select-box select { width: 100%; border: none; outline: none; padding: 14px 0; font-size: 14px; background: transparent; color: #333; cursor: pointer; }
.select-box select:disabled { color: #a0aec0; cursor: not-allowed; }
.textarea-box { align-items: flex-start; padding-top: 12px; }
.input-icon { width: 18px; height: 18px; color: #a0aec0; margin-right: 10px; flex-shrink: 0; }
.input-box input { flex: 1; border: none; padding: 14px 0; outline: none; font-size: 14px; color: #333; }
.input-box textarea { flex: 1; border: none; padding: 0 0 14px 0; outline: none; font-size: 14px; color: #333; min-height: 80px; resize: none; font-family: inherit; }

.address-box { border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px; margin-bottom: 20px; background: #fcfcfd; }
.address-title { display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px; }
.address-title strong { color: #06132b; font-size: 15px; }

/* CSS MỚI CHO LIST ĐỊA CHỈ & THÊM MỚI */
.btn-add-address {
  background: transparent;
  border: none;
  color: #bd9a5f;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-add-address:hover {
  color: #06132b;
  text-decoration: underline;
}
.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.address-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #ffffff;
}
.address-card:hover {
  border-color: #bd9a5f;
}
.address-card.active {
  border-color: #bd9a5f;
  background: #fefaf4;
  box-shadow: 0 0 0 1px #bd9a5f;
}
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

.option-icon {
  width: 32px !important;
  height: 32px !important;
  min-width: 32px !important;
  color: #b78d52;
  flex-shrink: 0 !important;
}

@media (max-width: 768px) {
  .checkout-left { padding: 24px; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>