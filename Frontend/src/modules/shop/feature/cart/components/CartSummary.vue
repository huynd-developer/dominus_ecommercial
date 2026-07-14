<template>
  <div class="cart-right">
    <h3 class="summary-title">Tóm tắt đơn hàng</h3>

    <div class="voucher-row-wrapper position-relative">
      <div class="voucher-row">
        <div class="voucher-input">
          <svg
            class="voucher-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect x="2" y="7" width="20" height="10" rx="2" ry="2" />
            <path d="M2 12a2 2 0 010-4m20 4a2 2 0 000-4M10 7v10m4-10v10" />
          </svg>

          <input
            type="text"
            v-model="voucherCode"
            placeholder="Nhập hoặc chọn mã..."
            @keyup.enter="handleApplyVoucher"
            @focus="openDropdown"
            @input="openDropdown"
            @blur="scheduleCloseDropdown"
            :disabled="isApplying || isVoucherApplied || !canCheckout"
            class="text-uppercase"
          />
        </div>

        <button
          v-if="isVoucherApplied"
          class="btn-cancel"
          type="button"
          @click="handleCancelVoucher"
        >
          Hủy bỏ
        </button>

        <button
          v-else
          class="btn-apply"
          type="button"
          @click="handleApplyVoucher"
          :disabled="isApplying || !voucherCode.trim() || !canCheckout"
        >
          <span v-if="isApplying" class="spinner-border spinner-border-sm"></span>
          <span v-else>Áp dụng</span>
        </button>
      </div>

      <div
        v-if="showDropdown && filteredVouchers.length > 0 && !isVoucherApplied && canCheckout"
        class="custom-voucher-dropdown"
      >
        <div
          v-for="v in filteredVouchers"
          :key="getVoucherCode(v)"
          :class="[
            'voucher-item',
            { 'voucher-disabled': !getVoucherEligibility(v).usable },
          ]"
          @mousedown.prevent="selectVoucher(v)"
        >
          <div class="voucher-top">
            <div class="voucher-code-badge">
              {{ getVoucherCode(v) }}
            </div>

            <span
              v-if="getVoucherEligibility(v).usable"
              class="voucher-status usable"
            >
              Có thể dùng
            </span>

            <span
              v-else
              class="voucher-status unusable"
            >
              Không đủ điều kiện
            </span>
          </div>

          <div class="voucher-desc">
            Giảm
            <strong class="text-danger">
              {{ formatVoucherDiscount(v) }}
            </strong>

            <span v-if="getMaxDiscount(v) > 0">
              · tối đa {{ formatCurrency(getMaxDiscount(v)) }}
            </span>
          </div>

          <div class="voucher-min">
            Đơn tối thiểu: {{ formatCurrency(getMinOrderValue(v)) }}
          </div>

          <div
            v-if="!getVoucherEligibility(v).usable"
            class="voucher-reason"
          >
            {{ getVoucherEligibility(v).reason }}
          </div>
        </div>
      </div>

      <div
        v-else-if="showDropdown && !isVoucherApplied && canCheckout"
        class="custom-voucher-dropdown"
      >
        <div class="voucher-empty">
          Không có voucher phù hợp
        </div>
      </div>
    </div>

    <p :class="['voucher-note', messageType]">
      {{ voucherMessage || "Nhập hoặc chọn mã giảm giá để nhận ưu đãi." }}
    </p>

    <div class="summary-box">
      <div class="summary-line">
        <span>Tạm tính</span>
        <span class="val">{{ formatCurrency(totalAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Giảm giá</span>
        <span class="val discount-val">- {{ formatCurrency(discountAmount) }}</span>
      </div>

      <div class="summary-line">
        <span>Phí vận chuyển</span>
        <span class="val free-ship">Không tính</span>
      </div>

      <div class="summary-line total-line">
        <span>Tổng thanh toán</span>
        <span class="total-val">{{ formatCurrency(finalTotal) }}</span>
      </div>
    </div>

    <div v-if="!canCheckout" class="checkout-warning">
      Có sản phẩm không khả dụng, hết hạn, chưa tới ngày bán hoặc vượt tồn kho.
      Vui lòng kiểm tra lại giỏ hàng.
    </div>

    <button
      class="btn-checkout"
      type="button"
      :disabled="!canCheckout"
      @click="$emit('checkout')"
    >
      ĐẶT HÀNG NGAY
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import api from "@/common/api";

const props = defineProps<{
  totalAmount: number;
  discountAmount: number;
  finalTotal: number;
  canCheckout: boolean;
}>();

const emit = defineEmits<{
  (e: "checkout"): void;
  (e: "apply-voucher", discount: number, code: string): void;
}>();

const voucherCode = ref("");
const isApplying = ref(false);
const isVoucherApplied = ref(false);
const voucherMessage = ref("");
const messageType = ref("");
const availableVouchers = ref<any[]>([]);
const showDropdown = ref(false);

let closeDropdownTimer: ReturnType<typeof setTimeout> | null = null;

const filteredVouchers = computed(() => {
  const keyword = voucherCode.value.trim().toLowerCase();

  const vouchers = availableVouchers.value.filter((voucher) => {
    const code = getVoucherCode(voucher).toLowerCase();

    if (!keyword) {
      return true;
    }

    return code.includes(keyword);
  });

  return vouchers.sort((a, b) => {
    const aUsable = getVoucherEligibility(a).usable ? 1 : 0;
    const bUsable = getVoucherEligibility(b).usable ? 1 : 0;

    return bUsable - aUsable;
  });
});

const getErrorMessage = (error: any) => {
  const data = error?.response?.data;

  if (!data) {
    return "Không thể áp dụng mã giảm giá này!";
  }

  if (typeof data === "string") {
    return data;
  }

  if (data.message) {
    return data.message;
  }

  if (data.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];

    if (firstError) {
      return String(firstError);
    }
  }

  return "Không thể áp dụng mã giảm giá này!";
};

const fetchAvailableVouchers = async () => {
  try {
    const res = await api.get("/v1/customer/vouchers");

    if (Array.isArray(res.data)) {
      availableVouchers.value = res.data;
      return;
    }

    if (Array.isArray(res.data?.content)) {
      availableVouchers.value = res.data.content;
      return;
    }

    if (Array.isArray(res.data?.data)) {
      availableVouchers.value = res.data.data;
      return;
    }

    availableVouchers.value = [];
  } catch (error) {
    console.error("Lỗi lấy danh sách Voucher:", error);
    availableVouchers.value = [];
  }
};

const openDropdown = () => {
  if (isVoucherApplied.value || !props.canCheckout) {
    return;
  }

  if (closeDropdownTimer) {
    clearTimeout(closeDropdownTimer);
    closeDropdownTimer = null;
  }

  showDropdown.value = true;
};

const scheduleCloseDropdown = () => {
  closeDropdownTimer = setTimeout(() => {
    showDropdown.value = false;
  }, 180);
};

const selectVoucher = async (voucher: any) => {
  const code = getVoucherCode(voucher);

  if (!code) {
    return;
  }

  const eligibility = getVoucherEligibility(voucher);

  if (!eligibility.usable) {
    voucherCode.value = code;
    voucherMessage.value = eligibility.reason;
    messageType.value = "text-danger";
    showDropdown.value = false;
    return;
  }

  voucherCode.value = code;
  showDropdown.value = false;

  await handleApplyVoucher();
};

const handleApplyVoucher = async () => {
  const cleanCode = voucherCode.value.trim().toUpperCase();

  if (!cleanCode) return;

  if (!props.canCheckout) {
    voucherMessage.value =
      "Không thể áp dụng mã khi giỏ hàng có sản phẩm không khả dụng.";
    messageType.value = "text-danger";
    return;
  }

  if (props.totalAmount <= 0) {
    voucherMessage.value = "Tổng tiền đơn hàng chưa hợp lệ.";
    messageType.value = "text-danger";
    return;
  }

  const selectedVoucher = availableVouchers.value.find(
    (voucher) => getVoucherCode(voucher).toUpperCase() === cleanCode
  );

  if (selectedVoucher) {
    const eligibility = getVoucherEligibility(selectedVoucher);

    if (!eligibility.usable) {
      voucherMessage.value = eligibility.reason;
      messageType.value = "text-danger";
      isVoucherApplied.value = false;
      emit("apply-voucher", 0, "");
      return;
    }
  }

  isApplying.value = true;
  voucherMessage.value = "";
  messageType.value = "";
  showDropdown.value = false;

  try {
    const res = await api.get("/v1/customer/vouchers/apply", {
      params: {
        code: cleanCode,
        orderTotal: props.totalAmount,
      },
    });

    const discount = Number(
      res.data?.discountAmount ??
        res.data?.discount ??
        res.data?.amount ??
        0
    );

    const safeDiscount = Math.min(
      Math.max(discount, 0),
      Number(props.totalAmount || 0)
    );

    const message = res.data?.message || "Áp dụng mã thành công!";

    voucherCode.value = cleanCode;
    voucherMessage.value = message;
    messageType.value = "text-success";
    isVoucherApplied.value = true;

    localStorage.setItem("applied_voucher", cleanCode);

    emit("apply-voucher", safeDiscount, cleanCode);
  } catch (error: any) {
    voucherMessage.value = getErrorMessage(error);
    messageType.value = "text-danger";
    isVoucherApplied.value = false;

    localStorage.removeItem("applied_voucher");

    emit("apply-voucher", 0, "");
  } finally {
    isApplying.value = false;
  }
};

const handleCancelVoucher = () => {
  isVoucherApplied.value = false;
  voucherCode.value = "";
  voucherMessage.value = "Đã hủy mã giảm giá.";
  messageType.value = "text-muted";

  localStorage.removeItem("applied_voucher");

  emit("apply-voucher", 0, "");
};

const getVoucherCode = (voucher: any) => {
  return String(
    voucher?.code ??
      voucher?.voucherCode ??
      voucher?.voucherName ??
      voucher?.name ??
      ""
  ).trim();
};

const getVoucherDiscountType = (voucher: any) => {
  return String(
    voucher?.discountType ??
      voucher?.type ??
      voucher?.voucherType ??
      ""
  )
    .toUpperCase()
    .trim();
};

const getVoucherDiscountValue = (voucher: any) => {
  return Number(
    voucher?.discountValue ??
      voucher?.value ??
      voucher?.discount ??
      voucher?.amount ??
      0
  );
};

const getMinOrderValue = (voucher: any) => {
  return Number(
    voucher?.minOrderValue ??
      voucher?.minimumOrderValue ??
      voucher?.minOrderAmount ??
      voucher?.conditionAmount ??
      0
  );
};

const getMaxDiscount = (voucher: any) => {
  return Number(
    voucher?.maxDiscountAmount ??
      voucher?.maximumDiscountAmount ??
      voucher?.maxDiscount ??
      0
  );
};

const getVoucherStartDate = (voucher: any) => {
  return voucher?.startDate ?? voucher?.validFrom ?? voucher?.fromDate ?? null;
};

const getVoucherEndDate = (voucher: any) => {
  return voucher?.endDate ?? voucher?.validTo ?? voucher?.toDate ?? null;
};

const isDateAfterNow = (value?: string | null) => {
  if (!value) return false;

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return false;
  }

  return date.getTime() > Date.now();
};

const isDateBeforeNow = (value?: string | null) => {
  if (!value) return false;

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return false;
  }

  return date.getTime() < Date.now();
};

const getRemainingQuantity = (voucher: any) => {
  if (voucher?.remainingQuantity != null) {
    return Number(voucher.remainingQuantity);
  }

  if (voucher?.remainingUsage != null) {
    return Number(voucher.remainingUsage);
  }

  if (voucher?.quantity != null && voucher?.usedQuantity != null) {
    return Number(voucher.quantity) - Number(voucher.usedQuantity);
  }

  if (voucher?.usageLimit != null && voucher?.usedCount != null) {
    return Number(voucher.usageLimit) - Number(voucher.usedCount);
  }

  return null;
};

const isVoucherStatusActive = (voucher: any) => {
  const status = voucher?.status;

  if (status == null) {
    return true;
  }

  if (typeof status === "number") {
    return status === 1;
  }

  const normalized = String(status).toUpperCase().trim();

  return ["1", "ACTIVE", "ENABLE", "ENABLED", "VALID", "AVAILABLE"].includes(
    normalized
  );
};

const getVoucherEligibility = (voucher: any) => {
  const code = getVoucherCode(voucher);

  if (!code) {
    return {
      usable: false,
      reason: "Voucher không hợp lệ.",
    };
  }

  if (!props.canCheckout) {
    return {
      usable: false,
      reason: "Giỏ hàng có sản phẩm không đủ điều kiện thanh toán.",
    };
  }

  if (props.totalAmount <= 0) {
    return {
      usable: false,
      reason: "Tổng tiền đơn hàng chưa hợp lệ.",
    };
  }

  if (voucher?.available === false || voucher?.usable === false) {
    return {
      usable: false,
      reason: voucher?.message || "Voucher hiện không khả dụng.",
    };
  }

  if (voucher?.isDeleted === true || voucher?.deleted === true) {
    return {
      usable: false,
      reason: "Voucher đã bị xóa hoặc không còn áp dụng.",
    };
  }

  if (!isVoucherStatusActive(voucher)) {
    return {
      usable: false,
      reason: "Voucher đang bị khóa hoặc ngừng hoạt động.",
    };
  }

  const startDate = getVoucherStartDate(voucher);

  if (isDateAfterNow(startDate)) {
    return {
      usable: false,
      reason: "Voucher chưa tới thời gian sử dụng.",
    };
  }

  const endDate = getVoucherEndDate(voucher);

  if (isDateBeforeNow(endDate)) {
    return {
      usable: false,
      reason: "Voucher đã hết hạn.",
    };
  }

  const remainingQuantity = getRemainingQuantity(voucher);

  if (remainingQuantity != null && remainingQuantity <= 0) {
    return {
      usable: false,
      reason: "Voucher đã hết lượt sử dụng.",
    };
  }

  const minOrderValue = getMinOrderValue(voucher);

  if (props.totalAmount < minOrderValue) {
    return {
      usable: false,
      reason: `Đơn hàng cần tối thiểu ${formatCurrency(minOrderValue)} để dùng voucher này.`,
    };
  }

  return {
    usable: true,
    reason: "",
  };
};

const formatVoucherDiscount = (voucher: any) => {
  const type = getVoucherDiscountType(voucher);
  const value = getVoucherDiscountValue(voucher);

  if (type === "PERCENT" || type === "PERCENTAGE") {
    return `${formatDiscount(value)}%`;
  }

  return formatCurrency(value);
};

const formatDiscount = (value?: number | null) => {
  const numberValue = Number(value || 0);

  if (Number.isInteger(numberValue)) {
    return String(numberValue);
  }

  return numberValue.toFixed(2).replace(/\.?0+$/, "");
};

const formatCurrency = (val?: number | null) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(val || 0));
};

watch(
  () => props.canCheckout,
  (value) => {
    if (!value && isVoucherApplied.value) {
      handleCancelVoucher();
      voucherMessage.value =
        "Đã hủy mã giảm giá vì giỏ hàng có sản phẩm không khả dụng.";
      messageType.value = "text-danger";
    }
  }
);

watch(
  () => props.totalAmount,
  () => {
    if (!isVoucherApplied.value || !voucherCode.value) {
      return;
    }

    const currentVoucher = availableVouchers.value.find(
      (voucher) =>
        getVoucherCode(voucher).toUpperCase() ===
        voucherCode.value.trim().toUpperCase()
    );

    if (!currentVoucher) {
      handleCancelVoucher();
      voucherMessage.value =
        "Đã hủy mã giảm giá vì không tìm thấy voucher đang áp dụng.";
      messageType.value = "text-danger";
      return;
    }

    const eligibility = getVoucherEligibility(currentVoucher);

    if (!eligibility.usable) {
      handleCancelVoucher();
      voucherMessage.value = eligibility.reason;
      messageType.value = "text-danger";
    }
  }
);

watch(
  () => props.discountAmount,
  (value) => {
    if (Number(value || 0) <= 0 && isVoucherApplied.value) {
      isVoucherApplied.value = false;
      voucherCode.value = "";
      voucherMessage.value = "Mã giảm giá đã được hủy.";
      messageType.value = "text-muted";
    }
  }
);

onMounted(async () => {
  await fetchAvailableVouchers();

  const savedVoucher = localStorage.getItem("applied_voucher");

  if (savedVoucher && props.totalAmount > 0 && props.canCheckout) {
    voucherCode.value = savedVoucher;
    await handleApplyVoucher();
  }
});
</script>

<style scoped>
.cart-right {
  flex: 1;
  background: white;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  position: sticky;
  top: 20px;
}

.summary-title {
  font-size: 18px;
  margin: 0 0 20px 0;
  color: #06132b;
}

.voucher-row-wrapper {
  position: relative;
  margin-bottom: 8px;
}

.voucher-row {
  display: flex;
  gap: 10px;
}

.voucher-input {
  flex: 1;
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 0 15px;
  background: #f8fafc;
  transition: all 0.2s;
}

.voucher-input:focus-within {
  border-color: #b78d52;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(183, 141, 82, 0.1);
}

.voucher-icon {
  width: 20px;
  height: 20px;
  color: #b78d52;
  margin-right: 10px;
}

.voucher-input input {
  flex: 1;
  border: none;
  outline: none;
  padding: 12px 0;
  font-size: 14px;
  color: #06132b;
  background: transparent;
  width: 100%;
}

.custom-voucher-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  width: calc(100% - 100px);
  background: white;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: 280px;
  overflow-y: auto;
}

.voucher-item {
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: 0.2s;
}

.voucher-item:last-child {
  border-bottom: none;
}

.voucher-item:hover:not(.voucher-disabled) {
  background: #f8fafc;
}

.voucher-disabled {
  opacity: 0.62;
  cursor: not-allowed;
  background: #f8fafc;
}

.voucher-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.voucher-code-badge {
  font-weight: 700;
  color: #06132b;
  font-size: 14px;
  margin-bottom: 2px;
  display: inline-block;
  padding: 2px 8px;
  background: rgba(183, 141, 82, 0.15);
  border-radius: 4px;
}

.voucher-status {
  font-size: 11px;
  font-weight: 800;
  border-radius: 999px;
  padding: 3px 8px;
  white-space: nowrap;
}

.voucher-status.usable {
  color: #166534;
  background: #dcfce7;
  border: 1px solid #bbf7d0;
}

.voucher-status.unusable {
  color: #991b1b;
  background: #fee2e2;
  border: 1px solid #fecaca;
}

.voucher-desc {
  font-size: 13px;
  color: #333;
  margin: 4px 0 2px;
}

.voucher-min {
  font-size: 12px;
  color: #64748b;
}

.voucher-reason {
  margin-top: 6px;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
}

.voucher-empty {
  padding: 14px;
  text-align: center;
  color: #64748b;
  font-size: 13px;
}

.btn-apply {
  background: #06132b;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
  min-width: 90px;
}

.btn-apply:hover:not(:disabled) {
  background: #b78d52;
}

.btn-apply:disabled,
.voucher-input input:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-cancel {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
  padding: 0 20px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
  min-width: 90px;
}

.btn-cancel:hover {
  background: #f87171;
  color: white;
  border-color: #ef4444;
}

.voucher-note {
  margin: 0 0 20px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
  font-weight: 500;
}

.text-success {
  color: #16a34a !important;
}

.text-danger {
  color: #dc2626 !important;
}

.text-muted {
  color: #94a3b8 !important;
}

.discount-val {
  color: #e53e3e !important;
}

.summary-box {
  background: #fafbfc;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 15px;
  color: #333;
}

.summary-line .val {
  font-weight: 500;
}

.free-ship {
  color: #38a169;
  font-weight: 700;
}

.total-line {
  border-top: 1px solid #eaeaea;
  padding-top: 15px;
  margin-top: 5px;
  font-weight: 700;
  font-size: 16px;
}

.total-val {
  font-size: 22px;
  color: #b78d52;
}

.checkout-warning {
  background: #fff7ed;
  color: #9a3412;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 14px;
}

.btn-checkout {
  width: 100%;
  padding: 16px;
  background: #06132b;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  font-size: 15px;
  cursor: pointer;
  transition: 0.2s;
}

.btn-checkout:hover:not(:disabled) {
  background: #0a1f44;
}

.btn-checkout:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}
</style>