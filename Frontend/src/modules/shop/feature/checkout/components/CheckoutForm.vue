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
          <span>
            Bạn có thể thay đổi số điện thoại hoặc địa chỉ nhận hàng cho riêng đơn này.
          </span>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group half">
          <label>Họ và tên <span class="text-danger">*</span></label>

          <div class="input-box">
            <svg
              class="input-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
              <circle cx="12" cy="7" r="4" />
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

          <small class="field-hint">
            Từ 2 đến 100 ký tự, chỉ nhập chữ và khoảng trắng. Không nhập số hoặc ký tự đặc biệt.
          </small>
        </div>

        <div class="form-group half">
          <label>Số điện thoại <span class="text-danger">*</span></label>

          <div class="input-box">
            <svg
              class="input-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"
              />
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

          <small class="field-hint">
            Đúng 10 chữ số, bắt đầu bằng 0. Không nhập chữ hoặc ký tự đặc biệt.
          </small>
        </div>
      </div>

      <div class="address-box">
        <div class="address-title">
          <strong>Địa chỉ nhận hàng <span class="text-danger">*</span></strong>
          <span>
            Có thể dùng địa chỉ tài khoản hoặc đổi địa chỉ nhận hàng riêng cho đơn này.
          </span>
        </div>

        <div v-if="showSavedAddressCard" class="saved-address-card">
          <div class="saved-address-content">
            <span>Địa chỉ đang dùng cho đơn hàng</span>
            <strong>{{ form.shippingAddress }}</strong>
          </div>

          <button
            type="button"
            class="btn-change-address"
            @click="startEditAddress"
          >
            Thay đổi địa chỉ
          </button>
        </div>

        <div v-if="showAddressEditor" class="address-editor">
          <div v-if="hasAccountAddress" class="account-address-box">
            <div>
              <span>Địa chỉ trong tài khoản</span>
              <strong>{{ form.profileAddress }}</strong>
            </div>

            <button
              type="button"
              class="btn-use-profile-address"
              @click="useProfileAddress"
            >
              Dùng địa chỉ này
            </button>
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label>Tỉnh / Thành phố <span class="text-danger">*</span></label>

              <div class="select-box">
                <select
                  v-model="selectedProvinceCode"
                  @change="handleProvinceChange"
                  :disabled="loadingProvinces"
                >
                  <option value="">
                    {{ loadingProvinces ? "Đang tải tỉnh/thành..." : "Chọn tỉnh/thành phố" }}
                  </option>

                  <option
                    v-for="province in provinces"
                    :key="province.code"
                    :value="province.code"
                  >
                    {{ province.name }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-group half">
              <label>Phường / Xã / Đặc khu <span class="text-danger">*</span></label>

              <div class="select-box">
                <select
                  v-model="selectedWardCode"
                  @change="syncFullAddress"
                  :disabled="!selectedProvinceCode || loadingWards"
                >
                  <option value="">
                    {{
                      !selectedProvinceCode
                        ? "Chọn tỉnh/thành trước"
                        : loadingWards
                          ? "Đang tải phường/xã..."
                          : "Chọn phường/xã/đặc khu"
                    }}
                  </option>

                  <option
                    v-for="ward in wards"
                    :key="ward.code"
                    :value="ward.code"
                  >
                    {{ ward.name }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <div v-if="addressLoadError" class="address-error">
            {{ addressLoadError }}
          </div>

          <div class="form-group">
            <label>Địa chỉ cụ thể <span class="text-danger">*</span></label>

            <div class="input-box textarea-box">
              <svg
                class="input-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>

              <textarea
                v-model="specificAddress"
                @input="validateSpecificAddress"
                maxlength="255"
                placeholder="Ví dụ: Số 12/5, ngõ 36-A, đường Trần Phú"
                autocomplete="street-address"
              ></textarea>
            </div>

            <small class="field-hint">
              Nhập số nhà, ngõ, đường, tòa nhà. Không nhập ký tự lạ như @ $ %.
            </small>
          </div>

          <div class="address-editor-actions">
            <button
              v-if="hasSavedAddress"
              type="button"
              class="btn-cancel-address"
              @click="cancelEditAddress"
            >
              Hủy thay đổi
            </button>

            <button
              type="button"
              class="btn-save-address"
              @click="finishEditAddress"
            >
              Dùng địa chỉ mới
            </button>
          </div>

          <div class="full-address-preview" v-if="form.shippingAddress">
            <span>Địa chỉ đầy đủ sẽ lưu vào đơn hàng:</span>
            <strong>{{ form.shippingAddress }}</strong>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label>Ghi chú đơn hàng</label>

        <div class="input-box textarea-box">
          <svg
            class="input-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>

          <textarea
            v-model="form.note"
            @input="validateNote"
            maxlength="255"
            placeholder="Ví dụ: Giao hàng trong giờ hành chính..."
          ></textarea>
        </div>

        <small class="field-hint">
          Không bắt buộc, tối đa 255 ký tự.
        </small>
      </div>

      <div class="vat-section-wrapper">
        <div class="vat-toggle-box">
          <div class="vat-toggle-info">
            <div class="vat-toggle-title">
              <svg
                class="icon-receipt"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>

              <strong>Yêu cầu xuất hóa đơn GTGT (VAT)</strong>
            </div>

            <span class="vat-sub">
              Chỉ bật khi khách cần hóa đơn điện tử cho doanh nghiệp.
            </span>
          </div>

          <label class="switch">
            <input
              type="checkbox"
              v-model="form.requireVat"
              @change="handleVatToggle"
            />
            <span class="slider round"></span>
          </label>
        </div>

        <div class="vat-form-box" v-if="form.requireVat">
          <h4 class="vat-form-title">Thông tin xuất hóa đơn</h4>

          <div class="form-row">
            <div class="form-group half">
              <label>Mã số thuế <span class="text-danger">*</span></label>

              <div class="input-box">
                <input
                  type="text"
                  v-model="form.vatTaxCode"
                  @input="validateVatTaxCode"
                  maxlength="14"
                  placeholder="Ví dụ: 0101234567"
                />
              </div>
            </div>

            <div class="form-group half">
              <label>Email nhận hóa đơn <span class="text-danger">*</span></label>

              <div class="input-box">
                <input
                  type="email"
                  v-model="form.vatEmail"
                  @input="validateVatEmail"
                  maxlength="255"
                  placeholder="accounting@company.com"
                />
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>Tên công ty / đơn vị <span class="text-danger">*</span></label>

            <div class="input-box">
              <input
                type="text"
                v-model="form.vatCompanyName"
                @input="validateVatCompanyName"
                maxlength="255"
                placeholder="Ví dụ: Công ty TNHH ABC"
              />
            </div>
          </div>

          <div class="form-group mb-0">
            <label>Địa chỉ công ty <span class="text-danger">*</span></label>

            <div class="input-box">
              <input
                type="text"
                v-model="form.vatCompanyAddress"
                @input="validateVatCompanyAddress"
                maxlength="500"
                placeholder="Địa chỉ theo giấy đăng ký kinh doanh"
              />
            </div>
          </div>

          <div class="vat-warning">
            Lưu ý: FE đang có form VAT. Muốn lưu thật vào đơn hàng thì BE cần thêm field VAT trong
            OrderRequest, Entity Orders và database.
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
          <input
            type="radio"
            name="payment"
            value="COD"
            v-model="form.paymentMethod"
          />
          <span class="custom-radio"></span>
        </div>

        <div class="option-info">
          <strong>Thanh toán khi nhận hàng (COD)</strong>
          <span>Thanh toán bằng tiền mặt khi shipper giao tới</span>
        </div>

        <svg
          class="option-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        >
          <rect x="2" y="6" width="20" height="12" rx="2" />
          <circle cx="12" cy="12" r="2" />
          <path d="M6 12h.01M18 12h.01" />
        </svg>
      </label>

      <!-- ĐÃ SỬA THÀNH VIETQR -->
      <label class="payment-option">
        <div class="radio-wrapper">
          <input
            type="radio"
            name="payment"
            value="VIETQR"
            v-model="form.paymentMethod"
          />
          <span class="custom-radio"></span>
        </div>

        <div class="option-info">
          <strong>Chuyển khoản VietQR</strong>
          <span>Quét mã QR qua ứng dụng ngân hàng (Miễn phí)</span>
        </div>

        <img src="https://vietqr.net/portal/v1.0.0/assets/img/logo-vietqr.png" alt="VietQR" style="height: 24px; object-fit: contain;" />
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

interface Ward {
  code: number | string;
  name: string;
  division_type?: string;
  codename?: string;
  province_code?: number | string;
}

interface Province {
  code: number | string;
  name: string;
  division_type?: string;
  codename?: string;
  wards?: Ward[] | null;
}

const ADDRESS_API_BASE = "https://provinces.open-api.vn/api/v2";

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
    profileAddress?: string;

    requireVat?: boolean;
    vatTaxCode?: string;
    vatEmail?: string;
    vatCompanyName?: string;
    vatCompanyAddress?: string;
  };
}>();

const provinces = ref<Province[]>([]);
const wards = ref<Ward[]>([]);

const selectedProvinceCode = ref<string>("");
const selectedWardCode = ref<string>("");
const specificAddress = ref("");
const isEditingAddress = ref(false);
const addressBeforeEdit = ref("");

const loadingProvinces = ref(false);
const loadingWards = ref(false);
const addressLoadError = ref("");

const selectedProvince = computed(() => {
  return provinces.value.find(
    (item) => String(item.code) === String(selectedProvinceCode.value)
  );
});

const selectedWard = computed(() => {
  return wards.value.find(
    (item) => String(item.code) === String(selectedWardCode.value)
  );
});

const hasSavedAddress = computed(() => {
  return String(props.form.shippingAddress || "").trim().length >= 5;
});

const hasAccountAddress = computed(() => {
  return String(props.form.profileAddress || "").trim().length >= 5;
});

const showAddressEditor = computed(() => {
  return isEditingAddress.value || !hasSavedAddress.value;
});

const showSavedAddressCard = computed(() => {
  return hasSavedAddress.value && !showAddressEditor.value;
});

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
    division_type: item.division_type,
    codename: item.codename,
    wards: Array.isArray(item.wards) ? item.wards.map(normalizeWard) : null,
  };
};

const normalizeWard = (item: any): Ward => {
  return {
    code: item.code ?? item.ward_code ?? item.wardCode ?? item.id,
    name: item.name ?? item.ward_name ?? item.wardName ?? "",
    division_type: item.division_type,
    codename: item.codename,
    province_code: item.province_code ?? item.provinceCode,
  };
};

const syncFullAddress = () => {
  const provinceName = selectedProvince.value?.name || "";
  const wardName = selectedWard.value?.name || "";
  const addressDetail = specificAddress.value || "";

  props.form.provinceName = provinceName;
  props.form.wardName = wardName;
  props.form.specificAddress = addressDetail;

  const parts = [addressDetail, wardName, provinceName]
    .map((item) => String(item || "").trim())
    .filter(Boolean);

  props.form.shippingAddress = parts.join(", ");
};

/**
 * Không tự xóa ký tự sai ở FE.
 * FE chỉ giới hạn độ dài để tránh nhập quá dài.
 * Sai format sẽ được báo ở CheckoutView trước khi gọi BE.
 */
const validateName = () => {
  props.form.customerName = String(props.form.customerName || "").slice(0, 100);
};

const validatePhone = () => {
  props.form.customerPhone = String(props.form.customerPhone || "").slice(0, 10);
};

const validateSpecificAddress = () => {
  specificAddress.value = String(specificAddress.value || "").slice(0, 255);
  syncFullAddress();
};

const validateNote = () => {
  props.form.note = String(props.form.note || "").slice(0, 255);
};

const validateVatTaxCode = () => {
  props.form.vatTaxCode = String(props.form.vatTaxCode || "").slice(0, 14);
};

const validateVatEmail = () => {
  props.form.vatEmail = String(props.form.vatEmail || "").slice(0, 255);
};

const validateVatCompanyName = () => {
  props.form.vatCompanyName = String(props.form.vatCompanyName || "").slice(0, 255);
};

const validateVatCompanyAddress = () => {
  props.form.vatCompanyAddress = String(props.form.vatCompanyAddress || "").slice(0, 500);
};

const handleProvinceChange = async () => {
  selectedWardCode.value = "";
  wards.value = [];
  syncFullAddress();

  if (!selectedProvinceCode.value) {
    return;
  }

  await loadWardsByProvince(selectedProvinceCode.value);
};

const startEditAddress = () => {
  addressBeforeEdit.value = props.form.shippingAddress || "";
  isEditingAddress.value = true;

  selectedProvinceCode.value = "";
  selectedWardCode.value = "";
  wards.value = [];

  specificAddress.value =
    props.form.provinceName || props.form.wardName
      ? props.form.specificAddress || ""
      : "";
};

const cancelEditAddress = () => {
  if (addressBeforeEdit.value) {
    props.form.shippingAddress = addressBeforeEdit.value;
  }

  selectedProvinceCode.value = "";
  selectedWardCode.value = "";
  wards.value = [];
  specificAddress.value = "";
  props.form.provinceName = "";
  props.form.wardName = "";
  props.form.specificAddress = "";
  isEditingAddress.value = false;
};

const useProfileAddress = () => {
  if (!hasAccountAddress.value) {
    return;
  }

  props.form.shippingAddress = String(props.form.profileAddress || "").trim();
  props.form.provinceName = "";
  props.form.wardName = "";
  props.form.specificAddress = "";

  selectedProvinceCode.value = "";
  selectedWardCode.value = "";
  wards.value = [];
  specificAddress.value = "";
  isEditingAddress.value = false;
};

const finishEditAddress = () => {
  syncFullAddress();

  if (props.form.shippingAddress && props.form.shippingAddress.trim().length >= 5) {
    isEditingAddress.value = false;
  }
};

const handleVatToggle = () => {
  if (!props.form.requireVat) {
    props.form.vatTaxCode = "";
    props.form.vatEmail = "";
    props.form.vatCompanyName = "";
    props.form.vatCompanyAddress = "";
  }
};

const loadProvinces = async () => {
  try {
    loadingProvinces.value = true;
    addressLoadError.value = "";

    const response = await fetch(`${ADDRESS_API_BASE}/`);
    const data = await response.json();

    provinces.value = extractArray(data)
      .map(normalizeProvince)
      .filter((item: Province) => item.code && item.name);
  } catch (error) {
    console.error("Không tải được danh sách tỉnh/thành mới:", error);
    addressLoadError.value =
      "Không tải được dữ liệu tỉnh/thành. Vui lòng thử lại hoặc nhập lại sau.";
    provinces.value = [];
  } finally {
    loadingProvinces.value = false;
  }
};

const loadWardsByProvince = async (provinceCode: string) => {
  try {
    loadingWards.value = true;
    addressLoadError.value = "";

    const provinceWithWards = selectedProvince.value;

    if (
      provinceWithWards &&
      Array.isArray(provinceWithWards.wards) &&
      provinceWithWards.wards.length > 0
    ) {
      wards.value = provinceWithWards.wards
        .map(normalizeWard)
        .filter((item: Ward) => item.code && item.name);
      return;
    }

    let wardData: Ward[] = [];

    try {
      const provinceResponse = await fetch(
        `${ADDRESS_API_BASE}/p/${provinceCode}?depth=2`
      );

      if (provinceResponse.ok) {
        const provinceDetail = await provinceResponse.json();

        wardData = extractArray(provinceDetail?.wards || provinceDetail)
          .map(normalizeWard)
          .filter((item: Ward) => item.code && item.name);
      }
    } catch (error) {
      wardData = [];
    }

    if (wardData.length === 0) {
      const wardResponse = await fetch(
        `${ADDRESS_API_BASE}/w/?province_code=${provinceCode}`
      );

      if (wardResponse.ok) {
        const data = await wardResponse.json();

        wardData = extractArray(data)
          .map(normalizeWard)
          .filter((item: Ward) => item.code && item.name);
      }
    }

    wards.value = wardData;
  } catch (error) {
    console.error("Không tải được danh sách phường/xã mới:", error);
    addressLoadError.value =
      "Không tải được dữ liệu phường/xã. Vui lòng chọn lại tỉnh/thành hoặc thử lại sau.";
    wards.value = [];
  } finally {
    loadingWards.value = false;
  }
};

watch(
  () => props.form.shippingAddress,
  (newValue, oldValue) => {
    if (!oldValue && newValue && String(newValue).trim().length >= 5) {
      isEditingAddress.value = false;
    }
  }
);

onMounted(async () => {
  props.form.requireVat = Boolean(props.form.requireVat);
  specificAddress.value = props.form.specificAddress || "";
  isEditingAddress.value = !hasSavedAddress.value;

  await loadProvinces();

  if (!hasSavedAddress.value) {
    syncFullAddress();
  }
});
</script>

<style scoped>
.checkout-left {
  flex: 2;
  background: white;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
}

.step-section {
  margin-bottom: 10px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 25px;
}

.step-num {
  background: #06132b;
  color: white;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
}

.step-header h2 {
  font-size: 20px;
  color: #06132b;
  margin: 0;
  position: relative;
}

.step-header h2::after {
  content: "";
  position: absolute;
  bottom: -6px;
  left: 0;
  width: 30px;
  height: 2px;
  background: #b78d52;
}

.profile-filled-box {
  display: flex;
  padding: 14px 16px;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  border-radius: 10px;
  margin-bottom: 22px;
}

.profile-filled-box strong {
  display: block;
  color: #166534;
  font-size: 14px;
  margin-bottom: 4px;
}

.profile-filled-box span {
  color: #15803d;
  font-size: 13px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-group {
  margin-bottom: 20px;
  width: 100%;
}

.form-group.half {
  flex: 1;
}

.form-group.mb-0 {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.field-hint {
  display: block;
  margin-top: 6px;
  color: #718096;
  font-size: 12px;
}

.input-box,
.select-box {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 0 15px;
  background: white;
  transition: 0.2s;
}

.input-box:focus-within,
.select-box:focus-within {
  border-color: #06132b;
}

.select-box select {
  width: 100%;
  border: none;
  outline: none;
  padding: 14px 0;
  font-size: 14px;
  background: transparent;
  color: #333;
  cursor: pointer;
}

.select-box select:disabled {
  color: #a0aec0;
  cursor: not-allowed;
}

.textarea-box {
  align-items: flex-start;
  padding-top: 12px;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: #a0aec0;
  margin-right: 10px;
  flex-shrink: 0;
}

.input-box input {
  flex: 1;
  border: none;
  padding: 14px 0;
  outline: none;
  font-size: 14px;
  color: #333;
}

.input-box textarea {
  flex: 1;
  border: none;
  padding: 0 0 14px 0;
  outline: none;
  font-size: 14px;
  color: #333;
  min-height: 80px;
  resize: none;
  font-family: inherit;
}

.address-box {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  background: #fcfcfd;
}

.address-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 18px;
}

.address-title strong {
  color: #06132b;
  font-size: 15px;
}

.address-title span {
  color: #718096;
  font-size: 13px;
}

.saved-address-card,
.account-address-box {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.saved-address-content,
.account-address-box div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.saved-address-content span,
.account-address-box span {
  color: #718096;
  font-size: 12px;
}

.saved-address-content strong,
.account-address-box strong {
  color: #06132b;
  font-size: 14px;
  line-height: 1.5;
}

.btn-change-address,
.btn-use-profile-address,
.btn-save-address,
.btn-cancel-address {
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}

.btn-change-address,
.btn-save-address {
  border: none;
  background: #06132b;
  color: #ffffff;
}

.btn-change-address:hover,
.btn-save-address:hover {
  background: #b78d52;
}

.btn-use-profile-address {
  border: 1px solid #b78d52;
  background: #ffffff;
  color: #b78d52;
}

.btn-use-profile-address:hover {
  background: #b78d52;
  color: #ffffff;
}

.btn-cancel-address {
  border: 1px solid #cbd5e0;
  background: #ffffff;
  color: #4a5568;
}

.btn-cancel-address:hover {
  background: #f8fafc;
}

.address-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: -4px;
  margin-bottom: 12px;
}

.address-error {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #fff1f2;
  color: #b91c1c;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  font-size: 13px;
}

.full-address-preview {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e0;
  color: #4a5568;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.full-address-preview strong {
  color: #06132b;
  line-height: 1.5;
}

.vat-section-wrapper {
  margin-top: 25px;
  margin-bottom: 20px;
}

.vat-toggle-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 15px 20px;
  background: white;
}

.vat-toggle-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vat-toggle-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1a202c;
}

.icon-receipt {
  width: 18px;
  height: 18px;
  color: #4a5568;
}

.vat-sub {
  font-size: 13px;
  color: #718096;
}

.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  flex-shrink: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e0;
  transition: 0.4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.4s;
}

input:checked + .slider {
  background-color: #3182ce;
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.slider.round {
  border-radius: 24px;
}

.slider.round:before {
  border-radius: 50%;
}

.vat-form-box {
  background: #ebf8ff;
  border: 1px solid #bee3f8;
  border-radius: 8px;
  padding: 20px;
  margin-top: 15px;
}

.vat-form-title {
  margin: 0 0 16px;
  color: #1a365d;
  font-size: 16px;
  font-weight: 800;
}

.vat-warning {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
}

.divider {
  height: 1px;
  background: #edf2f7;
  margin: 30px 0;
}

.payment-option {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 14px;
  transition: 0.2s;
}

.payment-option:hover {
  border-color: #b78d52;
  background: #fffaf0;
}

.radio-wrapper {
  position: relative;
}

.radio-wrapper input {
  opacity: 0;
  position: absolute;
}

.custom-radio {
  width: 20px;
  height: 20px;
  display: block;
  border: 2px solid #cbd5e0;
  border-radius: 50%;
  position: relative;
}

.radio-wrapper input:checked + .custom-radio {
  border-color: #b78d52;
}

.radio-wrapper input:checked + .custom-radio::after {
  content: "";
  width: 10px;
  height: 10px;
  background: #b78d52;
  border-radius: 50%;
  position: absolute;
  inset: 3px;
}

.option-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.option-info strong {
  color: #06132b;
  font-size: 14px;
}

.option-info span {
  color: #718096;
  font-size: 13px;
}

.option-icon {
  width: 30px;
  height: 30px;
  color: #4a5568;
}

.vnpay-text {
  font-weight: 900;
  font-size: 18px;
}

.vnpay-text .red {
  color: #e11d48;
}

.vnpay-text .blue {
  color: #2563eb;
}

@media (max-width: 768px) {
  .checkout-left {
    padding: 24px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .saved-address-card,
  .account-address-box,
  .vat-toggle-box {
    flex-direction: column;
  }
}
</style>