<template>
  <Teleport to="body">
    <div v-if="modelValue && order" class="return-overlay">
      <div class="return-modal">
        <button type="button" class="return-close" @click="closeModal">
          <i class="bi bi-x-lg"></i>
        </button>

        <template v-if="step === 1">
          <h4 class="return-title">Tình huống bạn đang gặp?</h4>

          <div class="return-option-list">
            <button
              v-for="item in returnTypeOptions"
              :key="item.value"
              type="button"
              class="return-type-card"
              :class="{ selected: form.returnType === item.value }"
              @click="chooseReturnType(item.value)"
            >
              <div class="return-type-icon">
                <i :class="item.icon"></i>
              </div>

              <div>
                <h5>{{ item.title }}</h5>
                <p>{{ item.description }}</p>
              </div>
            </button>
          </div>
        </template>

        <template v-else>
          <div class="return-form-header">
            <div>
              <h4 class="return-title mb-1">Tình huống bạn đang gặp?</h4>
              <p class="return-subtitle mb-0">
                {{ selectedReturnTypeText }}
              </p>
            </div>

            <button type="button" class="change-type-btn" @click="step = 1">
              Thay đổi
            </button>
          </div>

          <div class="return-section">
            <h5>Chọn sản phẩm cần trả hàng và hoàn tiền</h5>

            <div class="return-item-select-list">
              <div
                v-for="item in order.items"
                :key="`return-item-${item.orderItemId}`"
                class="return-select-item"
                :class="{ selected: isReturnItemSelected(item) }"
              >
                <label class="return-select-main">
                  <input
                    type="checkbox"
                    class="return-select-checkbox"
                    :checked="isReturnItemSelected(item)"
                    @change="toggleReturnItem(item)"
                  />

                  <img
                    :src="getItemImage(item)"
                    class="return-select-img"
                    alt="Sản phẩm"
                    @error="handleImageError"
                  />

                  <span class="return-select-info">
                    <strong>{{ item.productName || "Sản phẩm" }}</strong>

                    <span class="return-variant-line">
                      <template v-if="item.capacity || item.bottleType">
                        {{ item.capacity || "-" }}
                        <template v-if="item.bottleType"> · {{ item.bottleType }}</template>
                      </template>
                      <template v-else>{{ item.sku || "-" }}</template>
                    </span>

                    <span class="return-sku-line" v-if="item.sku">
                      SKU: {{ item.sku }}
                    </span>

                    <span class="return-price-inline">
                      <span
                        v-if="hasItemDiscount(item)"
                        class="return-original-price"
                      >
                        {{ formatMoney(getItemOriginalPrice(item)) }}
                      </span>

                      <strong class="return-final-price">
                        {{ formatMoney(getItemFinalPrice(item)) }}
                      </strong>
                    </span>
                  </span>
                </label>

                <div class="return-item-right">
                  <span class="return-order-qty">
                    x{{ getOrderItemQuantity(item) }}
                  </span>

                  <div
                    v-if="isReturnItemSelected(item)"
                    class="return-selected-actions"
                  >
                    <div class="return-quantity-box">
                      <button
                        type="button"
                        :disabled="getReturnItemQuantity(item) <= 1"
                        @click="decreaseReturnQuantity(item)"
                      >
                        -
                      </button>

                      <input
                        type="number"
                        min="1"
                        :max="getMaxReturnQuantity(item)"
                        :value="getReturnItemQuantity(item)"
                        @input="changeReturnQuantity(item, $event)"
                      />

                      <button
                        type="button"
                        :disabled="getReturnItemQuantity(item) >= getMaxReturnQuantity(item)"
                        @click="increaseReturnQuantity(item)"
                      >
                        +
                      </button>

                    </div>

                    <div class="return-line-refund">
                      <span>Hoàn tiền</span>
                      <strong>{{ formatMoney(getReturnItemRefundAmount(item)) }}</strong>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="return-item-note">
              Chọn đúng sản phẩm và số lượng cần hoàn. Số tiền hoàn dự kiến đã
              trừ phần giảm giá/voucher phân bổ theo tỷ lệ đơn hàng.
            </div>

            <div class="form-row">
              <label>
                <span class="required">*</span>
                Lý do
              </label>

              <div class="reason-select" :class="{ open: reasonDropdownOpen }">
                <button
                  type="button"
                  class="reason-select-toggle"
                  @click="toggleReasonDropdown"
                >
                  <span
                    v-if="selectedReasonLabel"
                    class="reason-value-text"
                    :title="selectedReasonLabel"
                  >
                    {{ selectedReasonLabel }}
                  </span>

                  <span v-else class="reason-placeholder">Chọn Lý Do</span>

                  <i
                    class="bi bi-chevron-down reason-chevron"
                    :class="{ rotated: reasonDropdownOpen }"
                  ></i>
                </button>

                <div
                  v-if="reasonDropdownOpen"
                  class="reason-dropdown"
                  :class="{ 'has-submenu': hoveredReasonChildren.length > 0 }"
                  @mouseleave="hoveredReasonValue = ''"
                >
                  <div class="reason-main-list">
                    <button
                      v-for="reason in currentReasonOptions"
                      :key="reason.value"
                      type="button"
                      class="reason-option"
                      :class="{
                        selected: isReasonSelected(reason),
                        hovered: hoveredReasonValue === reason.value,
                        'has-children': hasReasonChildren(reason),
                      }"
                      @mouseenter="hoverReason(reason)"
                      @click="handleReasonOptionClick(reason)"
                    >
                      <span class="reason-row-head">
                        <span class="reason-label">{{ reason.label }}</span>

                        <i
                          v-if="hasReasonChildren(reason)"
                          class="bi bi-chevron-right reason-child-arrow"
                        ></i>
                      </span>

                      <span
                        v-if="reason.description"
                        class="reason-option-description"
                      >
                        {{ reason.description }}
                      </span>
                    </button>
                  </div>

                  <div
                    v-if="hoveredReasonChildren.length > 0"
                    class="reason-submenu-panel"
                  >
                    <button
                      v-for="child in hoveredReasonChildren"
                      :key="child.value"
                      type="button"
                      class="reason-sub-option"
                      :class="{ selected: form.reason === child.value }"
                      @mouseenter="
                        hoveredReasonValue = hoveredReasonParentValue
                      "
                      @click="chooseReason(child.value)"
                    >
                      {{ child.label }}
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="selectedReasonDescription" class="reason-description">
                {{ selectedReasonDescription }}
              </div>
            </div>

            <div class="form-row">
              <label>Mô tả</label>

              <textarea
                v-model.trim="form.description"
                class="return-textarea"
                maxlength="2000"
                placeholder="Chi tiết vấn đề bạn gặp phải"
              ></textarea>

              <div class="char-count">{{ form.description.length }}/2000</div>
            </div>

            <div class="form-row">
              <label class="evidence-title">
                <span v-if="reasonNeedsEvidence" class="required">*</span>
                Đăng tải hình ảnh hoặc video:
                <button
                  type="button"
                  class="evidence-example-btn"
                  @click="showEvidenceExample"
                >
                </button>
              </label>

              <div v-if="reasonNeedsEvidence" class="evidence-required-guide">
                1. {{ evidenceGuideText }}
              </div>

              <div class="evidence-preview-grid">
                <div
                  v-for="(file, index) in imageFiles"
                  :key="`image-preview-${file.name}-${file.lastModified}-${index}`"
                  class="evidence-preview-card"
                >
                  <img
                    :src="imagePreviewUrls[index]"
                    class="evidence-preview-media"
                    alt="Ảnh bằng chứng"
                  />

                  <div class="evidence-preview-actions">
                    <button
                      type="button"
                      title="Sửa ảnh"
                      @click="replaceImageFile(index)"
                    >
                      <i class="bi bi-pencil"></i>
                    </button>

                    <button
                      type="button"
                      title="Xóa ảnh"
                      @click="removeImageFile(index)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>
                  </div>
                </div>

                <div
                  v-for="(file, index) in videoFiles"
                  :key="`video-preview-${file.name}-${file.lastModified}-${index}`"
                  class="evidence-preview-card video"
                >
                  <video
                    :src="videoPreviewUrls[index]"
                    class="evidence-preview-media"
                    muted
                    playsinline
                    controls
                  ></video>

                  <div class="evidence-preview-actions">
                    <button
                      type="button"
                      title="Sửa video"
                      @click="replaceVideoFile(index)"
                    >
                      <i class="bi bi-pencil"></i>
                    </button>

                    <button
                      type="button"
                      title="Xóa video"
                      @click="removeVideoFile(index)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>
                  </div>
                </div>

                <button
                  v-if="imageFiles.length < MAX_IMAGE_COUNT"
                  type="button"
                  class="evidence-upload-card evidence-upload-card-square"
                  @click="openImagePicker"
                >
                  <i class="bi bi-camera"></i>
                  <strong>Thêm Hình ảnh</strong>
                  <span>({{ imageFiles.length }}/{{ MAX_IMAGE_COUNT }})</span>
                  <small>Tổng ảnh tối đa: 10MB</small>
                </button>

                <button
                  v-if="videoFiles.length < MAX_VIDEO_COUNT"
                  type="button"
                  class="evidence-upload-card evidence-upload-card-square"
                  @click="openVideoPicker"
                >
                  <i class="bi bi-camera-video"></i>
                  <strong>Thêm video</strong>
                  <span>({{ videoFiles.length }}/{{ MAX_VIDEO_COUNT }})</span>
                  <small>Tối đa: 10MB</small>
                </button>
              </div>

              <input
                ref="imageInputRef"
                type="file"
                class="file-hidden-input"
                multiple
                accept="image/png,image/jpeg,image/jpg,image/webp"
                @change="handleImageFilesChange"
              />

              <input
                ref="videoInputRef"
                type="file"
                class="file-hidden-input"
                accept="video/mp4,video/quicktime,video/webm"
                @change="handleVideoFileChange"
              />

              <input
                ref="replaceImageInputRef"
                type="file"
                class="file-hidden-input"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                @change="handleReplaceImageFileChange"
              />

              <input
                ref="replaceVideoInputRef"
                type="file"
                class="file-hidden-input"
                accept="video/mp4,video/quicktime,video/webm"
                @change="handleReplaceVideoFileChange"
              />

              <div class="file-note">
                Tối đa {{ MAX_IMAGE_COUNT }} hình ảnh với tổng dung lượng 10MB
                và {{ MAX_VIDEO_COUNT }} video, mỗi video tối đa 10MB. Hãy tải lên
                bằng chứng liên quan đến vấn đề sản phẩm.
              </div>

              <div v-if="reasonNeedsEvidence" class="file-required-note">
                Lý do này cần ít nhất 1 ảnh hoặc 1 video bằng chứng để cửa hàng kiểm tra.
              </div>
            </div>
          </div>

          <div class="return-section">
            <h5>Phương án hoàn tiền</h5>

            <div class="refund-method-grid">
              <button
                type="button"
                class="refund-method-card"
                :class="{ selected: form.refundMethod === 'BANK_TRANSFER' }"
                @click="form.refundMethod = 'BANK_TRANSFER'"
              >
                <i class="bi bi-bank"></i>

                <div>
                  <strong>Chuyển khoản ngân hàng</strong>
                  <span>Nhận tiền hoàn qua tài khoản ngân hàng</span>
                </div>
              </button>

              <button
                type="button"
                class="refund-method-card"
                :class="{ selected: form.refundMethod === 'STORE' }"
                @click="form.refundMethod = 'STORE'"
              >
                <i class="bi bi-shop"></i>

                <div>
                  <strong>Nhận hoàn tại cửa hàng</strong>
                  <span>Đến cửa hàng để được xử lý hoàn tiền/trả hàng</span>
                </div>
              </button>
            </div>

            <div
              v-if="form.refundMethod === 'BANK_TRANSFER'"
              class="bank-info-box"
            >
              <div class="form-row">
                <label>
                  <span class="required">*</span>
                  Ngân hàng
                </label>

                <div class="bank-select" :class="{ open: bankDropdownOpen }">
                  <button
                    type="button"
                    class="bank-select-toggle"
                    :disabled="bankLoading"
                    @click="toggleBankDropdown"
                  >
                    <span v-if="selectedBank" class="bank-select-value">
                      <img
                        v-if="selectedBank.logo"
                        :src="selectedBank.logo"
                        class="bank-logo"
                        :alt="selectedBank.displayName"
                      />

                      <span class="bank-select-text">
                        <strong>{{ selectedBank.displayName }}</strong>
                        <small v-if="selectedBank.fullName">
                          {{ selectedBank.fullName }}
                        </small>
                      </span>
                    </span>

                    <span v-else class="bank-placeholder">
                      {{ bankLoading ? "Đang tải ngân hàng..." : "Chọn ngân hàng" }}
                    </span>

                    <i
                      class="bi bi-chevron-down bank-chevron"
                      :class="{ rotated: bankDropdownOpen }"
                    ></i>
                  </button>

                  <div v-if="bankDropdownOpen" class="bank-dropdown">
                    <button
                      v-if="bankLoadError"
                      type="button"
                      class="bank-option bank-option-error"
                      @click="fetchSupportedBanks(true)"
                    >
                      <span>{{ bankLoadError }}</span>
                      <strong>Thử lại</strong>
                    </button>

                    <button
                      v-else-if="!bankLoading && supportedBanks.length === 0"
                      type="button"
                      class="bank-option bank-option-error"
                      @click="fetchSupportedBanks(true)"
                    >
                      <span>Không có dữ liệu ngân hàng VietQR</span>
                      <strong>Tải lại</strong>
                    </button>

                    <div v-else-if="bankLoading" class="bank-loading">
                      Đang tải danh sách ngân hàng từ VietQR...
                    </div>

                    <template v-else>
                      <button
                        v-for="bank in supportedBanks"
                        :key="bank.id || bank.code || bank.value"
                        type="button"
                        class="bank-option"
                        :class="{ selected: form.bankName === bank.value }"
                        @click="chooseBank(bank)"
                      >
                        <img
                          v-if="bank.logo"
                          :src="bank.logo"
                          class="bank-logo"
                          :alt="bank.displayName"
                        />

                        <span class="bank-option-text">
                          <strong>{{ bank.displayName }}</strong>
                          <small v-if="bank.fullName">{{ bank.fullName }}</small>
                        </span>
                      </button>
                    </template>
                  </div>
                </div>
              </div>

              <div class="form-row">
                <label>
                  <span class="required">*</span>
                  Số tài khoản
                </label>

                <input
                  v-model.trim="form.bankAccountNumber"
                  type="text"
                  class="return-input"
                  maxlength="30"
                  inputmode="numeric"
                  autocomplete="off"
                  placeholder="Nhập số tài khoản nhận hoàn"
                  @blur="normalizeBankFields"
                />
              </div>

              <div class="form-row">
                <label>
                  <span class="required">*</span>
                  Chủ tài khoản
                </label>

                <input
                  v-model.trim="form.bankAccountHolder"
                  type="text"
                  class="return-input"
                  maxlength="100"
                  autocomplete="off"
                  placeholder="Nhập tên chủ tài khoản"
                  @blur="normalizeBankFields"
                />
              </div>
            </div>

            <div v-else class="store-refund-box">
              <i class="bi bi-info-circle"></i>
              <span>
                Bạn sẽ mang sản phẩm đến cửa hàng. Nhân viên sẽ kiểm tra và xử
                lý hoàn tiền trực tiếp tại quầy.
              </span>
            </div>
          </div>

          <div class="return-refund-section">
            <div class="refund-card">
              <div class="refund-card-header">
                <div>
                  <h5>Thông tin hoàn tiền</h5>
                  <p>Hệ thống sẽ gửi yêu cầu hoàn hàng để cửa hàng kiểm tra.</p>
                </div>

                <span class="refund-method-pill">
                  {{ refundMethodText }}
                </span>
              </div>

              <div class="refund-total-row">
                <span>Số tiền hoàn dự kiến</span>
                <strong>{{ formatMoney(refundAmount) }}</strong>
              </div>

              <div class="refund-summary-box">
                <div class="refund-line">
                  <span>Tiền hàng đã chọn</span>
                  <strong>{{ formatMoney(selectedReturnSubtotal) }}</strong>
                </div>

                <div
                  v-if="orderDiscountAmount > 0"
                  class="refund-line discount"
                >
                  <span>Giảm giá/voucher phân bổ</span>
                  <strong>-{{ formatMoney(selectedVoucherDiscount) }}</strong>
                </div>

                <div v-if="returnShippingFee > 0" class="refund-line shipping">
                  <span>Phí vận chuyển được hoàn</span>
                  <strong>+{{ formatMoney(returnShippingFee) }}</strong>
                </div>

                <div class="refund-line receive">
                  <span>Số tiền hoàn nhận được</span>
                  <strong>{{ formatMoney(refundAmount) }}</strong>
                </div>
              </div>

              <div class="refund-action-row">
                <button
                  type="button"
                  class="return-submit-btn"
                  :disabled="loading"
                  @click="submit"
                >
                  <span
                    v-if="loading"
                    class="spinner-border spinner-border-sm me-2"
                  ></span>
                  Hoàn thành
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import type {
  CustomerOrderResponse,
  RefundMethod,
  ReturnRequestItemPayload,
  ReturnRequestSubmitPayload,
  ReturnType,
} from "../types/profile.type";

const props = defineProps<{
  modelValue: boolean;
  order: CustomerOrderResponse | null;
  loading?: boolean;
  defaultEmail?: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "submit", payload: ReturnRequestSubmitPayload): void;
}>();

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
      <rect width="100%" height="100%" fill="#f3f4f6"/>
      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
        fill="#9ca3af" font-family="Arial" font-size="20">
        Không có ảnh
      </text>
    </svg>
  `);

const step = ref<1 | 2>(1);
const imageFiles = ref<File[]>([]);
const videoFiles = ref<File[]>([]);
const imageInputRef = ref<HTMLInputElement | null>(null);
const videoInputRef = ref<HTMLInputElement | null>(null);
const replaceImageInputRef = ref<HTMLInputElement | null>(null);
const replaceVideoInputRef = ref<HTMLInputElement | null>(null);
const editingImageIndex = ref<number | null>(null);
const editingVideoIndex = ref<number | null>(null);
const imagePreviewUrls = ref<string[]>([]);
const videoPreviewUrls = ref<string[]>([]);
const reasonDropdownOpen = ref(false);
const hoveredReasonValue = ref("");

const MAX_IMAGE_COUNT = 6;
const MAX_VIDEO_COUNT = 1;
const MAX_TOTAL_IMAGE_SIZE = 10 * 1024 * 1024;
const MAX_VIDEO_SIZE = 10 * 1024 * 1024;

const MIN_BANK_NAME_LENGTH = 2;
const MAX_BANK_NAME_LENGTH = 100;
const MIN_BANK_ACCOUNT_HOLDER_LENGTH = 2;
const MAX_BANK_ACCOUNT_HOLDER_LENGTH = 100;

const BANK_NAME_PATTERN = /^[\p{L}0-9\s.()\-/&]+$/u;
const BANK_ACCOUNT_HOLDER_PATTERN = /^[\p{L}\s'.-]+$/u;

const EVIDENCE_REQUIRED_REASONS = new Set([
  "Thiếu hàng",
  "Người bán gửi sai hàng",
  "Hàng bể vỡ",
  "Hàng bể vỡ - Thùng hàng không nguyên vẹn",
  "Hàng bể vỡ - Hàng trầy/xước/nứt",
  "Hàng bể vỡ - Rò rỉ chất lỏng",
  "Hàng bể vỡ - Hàng bể/vỡ vụn",
  "Hàng bể vỡ - Khác",
  "Hàng lỗi, không hoạt động",
  "Hàng hết hạn sử dụng",
  "Khác với mô tả",
  "Hàng đã qua sử dụng",
  "Hàng giả, nhái",
  "Thùng hàng rỗng",
]);
const VIETQR_BANKS_API_URL = "https://api.vietqr.io/v2/banks";

type VietQrBankOption = {
  id: number | string | null;
  name: string;
  shortName: string;
  code: string;
  bin: string;
  logo: string;
  value: string;
  displayName: string;
  fullName: string;
};

const supportedBanks = ref<VietQrBankOption[]>([]);
const bankLoading = ref(false);
const bankLoadError = ref("");
const bankDropdownOpen = ref(false);
const ALLOWED_IMAGE_EXTENSIONS = new Set(["jpg", "jpeg", "png", "webp"]);
const ALLOWED_VIDEO_EXTENSIONS = new Set(["mp4", "mov", "webm"]);

const files = computed(() => [...imageFiles.value, ...videoFiles.value]);

const selectedBank = computed(() => {
  const bankName = normalizeSpaces(form.bankName);

  if (!bankName) {
    return null;
  }

  return (
    supportedBanks.value.find((bank) => isSameBankValue(bank, bankName)) || null
  );
});

type ReasonOption = {
  value: string;
  label: string;
  description?: string;
  children?: ReasonOption[];
};
const form = reactive<{
  returnType: ReturnType | "";
  reason: string;
  description: string;
  email: string;
  refundMethod: RefundMethod;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
}>({
  returnType: "",
  reason: "",
  description: "",
  email: "",
  refundMethod: "BANK_TRANSFER",
  bankName: "",
  bankAccountNumber: "",
  bankAccountHolder: "",
});

const returnItemQuantities = reactive<Record<number, number>>({});

const returnTypeOptions: {
  value: ReturnType;
  title: string;
  description: string;
  icon: string;
}[] = [
  {
    value: "RECEIVED_WITH_PROBLEM",
    title:
      "Đã nhận hàng nhưng hàng có vấn đề (bể vỡ, sai mẫu, hàng lỗi, khác mô tả...)",
    description: "Miễn phí hoàn về nếu yêu cầu được chấp nhận.",
    icon: "bi bi-box-seam",
  },
  {
    value: "NOT_RECEIVED_OR_MISSING",
    title: "Chưa nhận hàng hoặc nhận thiếu hàng",
    description:
      "Dùng khi đơn giao thiếu sản phẩm hoặc bạn chưa nhận được hàng.",
    icon: "bi bi-box2-heart",
  },
];

const receivedProblemReasonOptions: ReasonOption[] = [
  {
    value: "Thiếu hàng",
    label: "Thiếu hàng",
    description: "",
  },
  {
    value: "Người bán gửi sai hàng",
    label: "Người bán gửi sai hàng",
    description: "",
  },
  {
    value: "Hàng bể vỡ",
    label: "Hàng bể vỡ",
    description: "Sản phẩm đã nhận bị trầy/xước/bể vỡ",
    children: [
      {
        value: "Hàng bể vỡ - Thùng hàng không nguyên vẹn",
        label: "Thùng hàng không nguyên vẹn",
      },
      {
        value: "Hàng bể vỡ - Hàng trầy/xước/nứt",
        label: "Hàng trầy/xước/nứt",
      },
      {
        value: "Hàng bể vỡ - Rò rỉ chất lỏng",
        label: "Rò rỉ chất lỏng",
      },
      {
        value: "Hàng bể vỡ - Hàng bể/vỡ vụn",
        label: "Hàng bể/vỡ vụn",
      },
      {
        value: "Hàng bể vỡ - Khác",
        label: "Khác",
      },
    ],
  },
  {
    value: "Hàng lỗi, không hoạt động",
    label: "Hàng lỗi, không hoạt động",
    description: "",
  },
  {
    value: "Hàng hết hạn sử dụng",
    label: "Hàng hết hạn sử dụng",
    description: "",
  },
  {
    value: "Khác với mô tả",
    label: "Khác với mô tả",
    description: "",
  },
  {
    value: "Hàng đã qua sử dụng",
    label: "Hàng đã qua sử dụng",
    description: "",
  },
  {
    value: "Hàng giả, nhái",
    label: "Hàng giả, nhái",
    description:
      "Ngoại quan, bao bì, logo, kết cấu, màu sắc hoặc mùi hương sản phẩm khác biệt so với hàng chính hãng.",
  },
];

const missingReasonOptions: ReasonOption[] = [
  {
    value: "Chưa nhận được hàng",
    label: "Chưa nhận được hàng",
    description: "",
  },
  {
    value: "Thiếu hàng",
    label: "Thiếu hàng",
    description: "",
  },
  {
    value: "Thùng hàng rỗng",
    label: "Thùng hàng rỗng",
    description: "",
  },
];

const currentReasonOptions = computed<ReasonOption[]>(() => {
  if (form.returnType === "NOT_RECEIVED_OR_MISSING") {
    return missingReasonOptions;
  }

  return receivedProblemReasonOptions;
});

const flatReasonOptions = computed<ReasonOption[]>(() => {
  return currentReasonOptions.value.flatMap((reason) => {
    if (hasReasonChildren(reason)) {
      return [reason, ...(reason.children || [])];
    }

    return [reason];
  });
});

const selectedReason = computed(() => {
  return flatReasonOptions.value.find((item) => item.value === form.reason);
});

const selectedReasonLabel = computed(() => {
  return selectedReason.value?.label || "";
});

const selectedReasonDescription = computed(() => {
  return selectedReason.value?.description || "";
});

const reasonNeedsEvidence = computed(() => {
  return EVIDENCE_REQUIRED_REASONS.has(form.reason);
});

const evidenceGuideText = computed(() => {
  if (form.reason === "Người bán gửi sai hàng") {
    return "Thấy rõ sản phẩm nhận được không phải sản phẩm người mua đã đặt";
  }

  if (form.reason === "Thiếu hàng") {
    return "Thấy rõ sản phẩm đã nhận, kiện hàng hoặc phần sản phẩm bị thiếu";
  }

  if (form.reason === "Thùng hàng rỗng") {
    return "Thấy rõ kiện hàng rỗng, bao bì và thông tin đơn hàng";
  }

  if (form.reason.includes("Hàng bể vỡ")) {
    return "Thấy rõ tình trạng bể vỡ, trầy xước, nứt hoặc rò rỉ của sản phẩm";
  }

  if (form.reason === "Hàng giả, nhái") {
    return "Thấy rõ bao bì, logo, tem nhãn hoặc dấu hiệu nghi ngờ hàng giả";
  }

  if (form.reason === "Hàng hết hạn sử dụng") {
    return "Thấy rõ hạn sử dụng hoặc thông tin lô sản xuất trên sản phẩm";
  }

  return "Thấy rõ tình trạng sản phẩm hoặc vấn đề cần cửa hàng kiểm tra";
});

const reasonNeedsDescription = computed(() => {
  return form.reason === "Hàng bể vỡ - Khác";
});

const hoveredReason = computed(() => {
  return currentReasonOptions.value.find(
    (item) => item.value === hoveredReasonValue.value
  );
});

const hoveredReasonChildren = computed<ReasonOption[]>(() => {
  return hoveredReason.value?.children || [];
});

const hoveredReasonParentValue = computed(() => {
  return hoveredReason.value?.value || "";
});

const selectedReturnTypeText = computed(() => {
  return (
    returnTypeOptions.find((item) => item.value === form.returnType)?.title ||
    ""
  );
});

const refundMethodText = computed(() => {
  return form.refundMethod === "BANK_TRANSFER"
    ? "Chuyển khoản ngân hàng"
    : "Nhận hoàn tại cửa hàng";
});

const selectedReturnItems = computed<ReturnRequestItemPayload[]>(() => {
  if (!props.order?.items?.length) {
    return [];
  }

  return props.order.items
    .filter((item) => isReturnItemSelected(item))
    .map((item) => ({
      orderItemId: item.orderItemId,
      quantity: getReturnItemQuantity(item),
    }));
});

const orderDiscountAmount = computed(() => {
  return Math.max(0, Number(props.order?.discountAmount || 0));
});

const orderDiscountBaseAmount = computed(() => {
  if (!props.order?.items?.length) {
    return 0;
  }

  return props.order.items.reduce((total, item) => {
    return total + getOrderItemLineAmountBeforeOrderDiscount(item);
  }, 0);
});

const selectedReturnSubtotal = computed(() => {
  if (!props.order?.items?.length) {
    return 0;
  }

  return roundMoneyAmount(
    props.order.items.reduce((total, item) => {
      return total + getReturnItemBaseAmount(item);
    }, 0)
  );
});

const selectedProductRefundAmount = computed(() => {
  if (!props.order?.items?.length) {
    return 0;
  }

  return roundMoneyAmount(
    props.order.items.reduce((total, item) => {
      return total + getReturnItemRefundAmount(item);
    }, 0)
  );
});

const orderShippingFee = computed(() => {
  return getFirstPositiveMoneyByKeys(props.order || {}, [
    "shippingFee",
    "shippingfee",
    "shippingFeeAmount",
    "shipFee",
    "deliveryFee",
    "shippingAmount",
  ]);
});

const isFullOrderReturn = computed(() => {
  if (!props.order?.items?.length) {
    return false;
  }

  return props.order.items.every((item) => {
    return (
      isReturnItemSelected(item) &&
      getReturnItemQuantity(item) === getOrderItemQuantity(item)
    );
  });
});

const shouldRefundShippingFee = computed(() => {
  return isFullOrderReturn.value && Boolean(selectedReason.value);
});

const returnShippingFee = computed(() => {
  return shouldRefundShippingFee.value ? orderShippingFee.value : 0;
});

const refundAmount = computed(() => {
  return roundMoneyAmount(selectedProductRefundAmount.value + returnShippingFee.value);
});

const selectedVoucherDiscount = computed(() => {
  return roundMoneyAmount(
    Math.max(0, selectedReturnSubtotal.value - selectedProductRefundAmount.value)
  );
});

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      resetForm();
      fetchSupportedBanks(false);
    }
  }
);

onBeforeUnmount(() => {
  revokePreviewUrls(imagePreviewUrls.value);
  revokePreviewUrls(videoPreviewUrls.value);
});

function resetForm() {
  step.value = 1;
  revokePreviewUrls(imagePreviewUrls.value);
  revokePreviewUrls(videoPreviewUrls.value);

  imageFiles.value = [];
  videoFiles.value = [];
  imagePreviewUrls.value = [];
  videoPreviewUrls.value = [];
  editingImageIndex.value = null;
  editingVideoIndex.value = null;
  reasonDropdownOpen.value = false;
  hoveredReasonValue.value = "";
  bankDropdownOpen.value = false;

  form.returnType = "";
  form.reason = "";
  form.description = "";
  form.email = props.defaultEmail || "";
  form.refundMethod = "BANK_TRANSFER";
  form.bankName = "";
  form.bankAccountNumber = "";
  form.bankAccountHolder = "";

  clearReturnItemSelections();

  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }

  if (videoInputRef.value) {
    videoInputRef.value.value = "";
  }

  if (replaceImageInputRef.value) {
    replaceImageInputRef.value.value = "";
  }

  if (replaceVideoInputRef.value) {
    replaceVideoInputRef.value.value = "";
  }
}

function chooseReturnType(value: ReturnType) {
  form.returnType = value;
  form.reason = "";
  hoveredReasonValue.value = "";
  reasonDropdownOpen.value = false;
  step.value = 2;
}

function hasReasonChildren(reason: ReasonOption) {
  return Array.isArray(reason.children) && reason.children.length > 0;
}

function toggleReasonDropdown() {
  reasonDropdownOpen.value = !reasonDropdownOpen.value;

  if (!reasonDropdownOpen.value) {
    hoveredReasonValue.value = "";
  }
}

function hoverReason(reason: ReasonOption) {
  if (hasReasonChildren(reason)) {
    hoveredReasonValue.value = reason.value;
    return;
  }

  hoveredReasonValue.value = "";
}

function handleReasonOptionClick(reason: ReasonOption) {
  if (hasReasonChildren(reason)) {
    hoveredReasonValue.value = reason.value;
    return;
  }

  chooseReason(reason.value);
}

function chooseReason(value: string) {
  form.reason = value;
  reasonDropdownOpen.value = false;
  hoveredReasonValue.value = "";
}

function isReasonSelected(reason: ReasonOption) {
  if (form.reason === reason.value) {
    return true;
  }

  return !!reason.children?.some((child) => child.value === form.reason);
}

function getMaxReturnQuantity(item: any) {
  return Math.max(0, Number(item?.quantity || 0));
}

function getReturnItemKey(item: any) {
  return Number(item?.orderItemId || 0);
}

function isReturnItemSelected(item: any) {
  const key = getReturnItemKey(item);
  return key > 0 && (returnItemQuantities[key] ?? 0) > 0;
}

function getReturnItemQuantity(item: any) {
  const key = getReturnItemKey(item);
  return key > 0 ? returnItemQuantities[key] ?? 0 : 0;
}

function toggleReturnItem(item: any) {
  const key = getReturnItemKey(item);
  const maxQuantity = getMaxReturnQuantity(item);

  if (key <= 0 || maxQuantity <= 0) {
    return;
  }

  if (isReturnItemSelected(item)) {
    delete returnItemQuantities[key];
    return;
  }

  returnItemQuantities[key] = 1;
}

function increaseReturnQuantity(item: any) {
  if (!isReturnItemSelected(item)) {
    return;
  }

  const key = getReturnItemKey(item);
  const currentQuantity = getReturnItemQuantity(item);
  const maxQuantity = getMaxReturnQuantity(item);

  returnItemQuantities[key] = Math.min(currentQuantity + 1, maxQuantity);
}

function decreaseReturnQuantity(item: any) {
  if (!isReturnItemSelected(item)) {
    return;
  }

  const key = getReturnItemKey(item);
  const currentQuantity = getReturnItemQuantity(item);

  if (currentQuantity <= 1) {
    returnItemQuantities[key] = 1;
    return;
  }

  returnItemQuantities[key] = currentQuantity - 1;
}

function changeReturnQuantity(item: any, event: Event) {
  if (!isReturnItemSelected(item)) {
    return;
  }

  const target = event.target as HTMLInputElement;
  const key = getReturnItemKey(item);
  const maxQuantity = getMaxReturnQuantity(item);
  const inputValue = Math.floor(Number(target.value || 1));
  const safeQuantity = Math.min(Math.max(inputValue, 1), maxQuantity);

  returnItemQuantities[key] = safeQuantity;
  target.value = String(safeQuantity);
}

function getOrderItemLineAmountBeforeOrderDiscount(item: any) {
  const finalPrice = Number(item?.finalPrice || 0);
  const quantity = Number(item?.quantity || 0);

  if (finalPrice <= 0 || quantity <= 0) {
    return 0;
  }

  return finalPrice * quantity;
}

function getOrderItemQuantity(item: any) {
  return Math.max(0, Math.floor(Number(item?.quantity || 0)));
}

function toMoneyNumber(value: unknown) {
  const numberValue = Number(value || 0);

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return 0;
  }

  return numberValue;
}

function getFirstPositiveMoneyByKeys(item: any, keys: string[]) {
  for (const key of keys) {
    const value = toMoneyNumber(item?.[key]);

    if (value > 0) {
      return value;
    }
  }

  return 0;
}

function getItemFinalPrice(item: any) {
  return getFirstPositiveMoneyByKeys(item, [
    "finalPrice",
    "unitFinalPrice",
    "priceAfterDiscount",
    "discountedPrice",
    "salePrice",
    "sellingPrice",
  ]);
}

function getItemUnitDiscount(item: any) {
  const unitDiscount = getFirstPositiveMoneyByKeys(item, [
    "discountAmount",
    "unitDiscountAmount",
    "itemDiscountAmount",
    "productDiscountAmount",
  ]);

  if (unitDiscount > 0) {
    return roundMoneyAmount(unitDiscount);
  }

  const lineDiscount = getFirstPositiveMoneyByKeys(item, [
    "lineDiscountAmount",
    "totalDiscountAmount",
    "totalItemDiscountAmount",
  ]);

  const quantity = getOrderItemQuantity(item);

  if (lineDiscount > 0 && quantity > 0) {
    return roundMoneyAmount(lineDiscount / quantity);
  }

  return 0;
}

function getItemOriginalPrice(item: any) {
  const finalPrice = getItemFinalPrice(item);

  const originalPrice = getFirstPositiveMoneyByKeys(item, [
    "originalPrice",
    "unitOriginalPrice",
    "priceBeforeDiscount",
    "beforeDiscountPrice",
    "listedPrice",
    "listPrice",
    "regularPrice",
    "basePrice",
    "variantOriginalPrice",
    "variantPrice",
    "productOriginalPrice",
    "productPrice",
  ]);

  if (originalPrice > finalPrice) {
    return originalPrice;
  }

  const unitDiscount = getItemUnitDiscount(item);

  if (finalPrice > 0 && unitDiscount > 0) {
    return roundMoneyAmount(finalPrice + unitDiscount);
  }

  return finalPrice;
}

function hasItemDiscount(item: any) {
  return getItemOriginalPrice(item) > getItemFinalPrice(item);
}

function getOrderItemFinalTotal(item: any) {
  return roundMoneyAmount(
    getItemFinalPrice(item) * getOrderItemQuantity(item)
  );
}

function getReturnItemBaseAmount(item: any) {
  if (!isReturnItemSelected(item)) {
    return 0;
  }

  const finalPrice = Number(item?.finalPrice || 0);
  const returnQuantity = getReturnItemQuantity(item);

  if (finalPrice <= 0 || returnQuantity <= 0) {
    return 0;
  }

  return finalPrice * returnQuantity;
}

function getReturnItemAllocatedDiscount(item: any) {
  const baseAmount = getReturnItemBaseAmount(item);

  if (
    baseAmount <= 0 ||
    orderDiscountAmount.value <= 0 ||
    orderDiscountBaseAmount.value <= 0
  ) {
    return 0;
  }

  return (
    (orderDiscountAmount.value * baseAmount) / orderDiscountBaseAmount.value
  );
}

function getReturnItemRefundAmount(item: any) {
  const baseAmount = getReturnItemBaseAmount(item);

  if (baseAmount <= 0) {
    return 0;
  }

  const allocatedDiscount = getReturnItemAllocatedDiscount(item);
  const refundValue = baseAmount - allocatedDiscount;

  return roundMoneyAmount(Math.max(0, refundValue));
}

function roundMoneyAmount(value: number) {
  return Math.round(Number(value || 0) * 100) / 100;
}

function clearReturnItemSelections() {
  Object.keys(returnItemQuantities).forEach((key) => {
    delete returnItemQuantities[Number(key)];
  });
}

function closeModal() {
  if (props.loading) return;
  emit("update:modelValue", false);
}

function normalizeSpaces(value: string) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ");
}

function normalizeBankFields() {
  form.bankName = normalizeSpaces(form.bankName);
  form.bankAccountNumber = String(form.bankAccountNumber || "").trim();
  form.bankAccountHolder = normalizeSpaces(form.bankAccountHolder);
}

function normalizeBankText(value: unknown) {
  return normalizeSpaces(String(value || ""));
}

function mapVietQrBank(rawBank: any): VietQrBankOption | null {
  const name = normalizeBankText(rawBank?.name);
  const shortName = normalizeBankText(rawBank?.shortName);
  const code = normalizeBankText(rawBank?.code);
  const bin = normalizeBankText(rawBank?.bin);
  const logo = normalizeBankText(rawBank?.logo);
  const value = shortName || name || code;

  if (!value) {
    return null;
  }

  return {
    id: rawBank?.id ?? bin ?? code ?? value,
    name,
    shortName,
    code,
    bin,
    logo,
    value,
    displayName: shortName || code || name,
    fullName: name && name !== shortName ? name : "",
  };
}

function isSameBankValue(bank: VietQrBankOption, value: string) {
  const cleanValue = normalizeBankText(value).toLowerCase();

  if (!cleanValue) {
    return false;
  }

  return [bank.value, bank.name, bank.shortName, bank.code]
    .map((item) => normalizeBankText(item).toLowerCase())
    .some((item) => item === cleanValue);
}

async function fetchSupportedBanks(force = false) {
  if (bankLoading.value) {
    return;
  }

  if (!force && supportedBanks.value.length > 0) {
    return;
  }

  bankLoading.value = true;
  bankLoadError.value = "";

  try {
    const response = await fetch(VIETQR_BANKS_API_URL);

    if (!response.ok) {
      throw new Error(`VietQR banks API error: ${response.status}`);
    }

    const body = await response.json();
    const rawBanks = Array.isArray(body?.data) ? body.data : [];

    const banks = rawBanks
      .map(mapVietQrBank)
      .filter((bank: VietQrBankOption | null): bank is VietQrBankOption => Boolean(bank))
      .sort((firstBank: VietQrBankOption, secondBank: VietQrBankOption) =>
        firstBank.displayName.localeCompare(secondBank.displayName, "vi"),
      );

    if (banks.length === 0) {
      throw new Error("VietQR banks API returned empty data");
    }

    supportedBanks.value = banks;
  } catch (error) {
    supportedBanks.value = [];
    bankLoadError.value = "Không tải được danh sách ngân hàng từ VietQR.";
  } finally {
    bankLoading.value = false;
  }
}

function toggleBankDropdown() {
  if (bankLoading.value) {
    return;
  }

  bankDropdownOpen.value = !bankDropdownOpen.value;

  if (bankDropdownOpen.value) {
    fetchSupportedBanks(false);
  }
}

function chooseBank(bank: VietQrBankOption) {
  form.bankName = bank.value;
  bankDropdownOpen.value = false;
  normalizeBankFields();
}

function isSupportedBankName(value: string) {
  return supportedBanks.value.some((bank) => isSameBankValue(bank, value));
}

function getFileExtension(fileName: string | undefined | null) {
  if (!fileName) {
    return "";
  }

  const cleanName = fileName.split(/[\\/]/).pop() || "";
  const dotIndex = cleanName.lastIndexOf(".");

  if (dotIndex < 0 || dotIndex === cleanName.length - 1) {
    return "";
  }

  return cleanName.slice(dotIndex + 1).toLowerCase();
}

function revokePreviewUrls(urls: string[]) {
  urls.forEach((url) => URL.revokeObjectURL(url));
}

function rebuildImagePreviewUrls() {
  revokePreviewUrls(imagePreviewUrls.value);
  imagePreviewUrls.value = imageFiles.value.map((file) => URL.createObjectURL(file));
}

function rebuildVideoPreviewUrls() {
  revokePreviewUrls(videoPreviewUrls.value);
  videoPreviewUrls.value = videoFiles.value.map((file) => URL.createObjectURL(file));
}

function getTotalImageSize(fileList: File[]) {
  return fileList.reduce((total, file) => total + file.size, 0);
}

function validateTotalImageSize(fileList: File[]) {
  if (getTotalImageSize(fileList) > MAX_TOTAL_IMAGE_SIZE) {
    return "Tổng dung lượng hình ảnh không được vượt quá 10MB.";
  }

  return "";
}

function validateImageFile(file: File) {
  if (!file.type.startsWith("image/")) {
    return `File "${file.name}" không phải hình ảnh.`;
  }

  if (!ALLOWED_IMAGE_EXTENSIONS.has(getFileExtension(file.name))) {
    return `Ảnh "${file.name}" chỉ hỗ trợ JPG, JPEG, PNG hoặc WEBP.`;
  }

  return "";
}

function validateVideoFile(file: File) {
  if (!file.type.startsWith("video/")) {
    return `File "${file.name}" không phải video.`;
  }

  if (!ALLOWED_VIDEO_EXTENSIONS.has(getFileExtension(file.name))) {
    return `Video "${file.name}" chỉ hỗ trợ MP4, MOV hoặc WEBM.`;
  }

  if (file.size > MAX_VIDEO_SIZE) {
    return `Video "${file.name}" vượt quá dung lượng 10MB.`;
  }

  return "";
}

function openImagePicker() {
  if (imageFiles.value.length >= MAX_IMAGE_COUNT) {
    showFileWarning(`Chỉ được tải tối đa ${MAX_IMAGE_COUNT} hình ảnh.`);
    return;
  }

  imageInputRef.value?.click();
}

function openVideoPicker() {
  if (videoFiles.value.length >= MAX_VIDEO_COUNT) {
    showFileWarning(`Chỉ được tải tối đa ${MAX_VIDEO_COUNT} video.`);
    return;
  }

  videoInputRef.value?.click();
}

function handleImageFilesChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const selectedFiles = target.files ? Array.from(target.files) : [];

  if (selectedFiles.length === 0) return;

  const remainingSlots = MAX_IMAGE_COUNT - imageFiles.value.length;

  if (remainingSlots <= 0) {
    showFileWarning(`Chỉ được tải tối đa ${MAX_IMAGE_COUNT} hình ảnh.`);
    target.value = "";
    return;
  }

  const validFiles: File[] = [];

  for (const file of selectedFiles) {
    const error = validateImageFile(file);

    if (error) {
      showFileWarning(error);
      target.value = "";
      return;
    }

    if (validFiles.length >= remainingSlots) {
      break;
    }

    validFiles.push(file);
  }

  const nextImageFiles = [...imageFiles.value, ...validFiles].slice(0, MAX_IMAGE_COUNT);
  const totalImageSizeError = validateTotalImageSize(nextImageFiles);

  if (totalImageSizeError) {
    showFileWarning(totalImageSizeError);
    target.value = "";
    return;
  }

  imageFiles.value = nextImageFiles;
  rebuildImagePreviewUrls();

  target.value = "";

  if (selectedFiles.length > remainingSlots) {
    showFileWarning(`Chỉ thêm được ${remainingSlots} hình ảnh còn trống.`);
  }
}

function handleVideoFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const selectedFiles = target.files ? Array.from(target.files) : [];
  const file = selectedFiles[0];

  if (!file) return;

  if (videoFiles.value.length >= MAX_VIDEO_COUNT) {
    showFileWarning(`Chỉ được tải tối đa ${MAX_VIDEO_COUNT} video.`);
    target.value = "";
    return;
  }

  const error = validateVideoFile(file);

  if (error) {
    showFileWarning(error);
    target.value = "";
    return;
  }

  videoFiles.value = [file].slice(0, MAX_VIDEO_COUNT);
  rebuildVideoPreviewUrls();

  target.value = "";
}

function replaceImageFile(index: number) {
  if (index < 0 || index >= imageFiles.value.length) {
    return;
  }

  editingImageIndex.value = index;
  replaceImageInputRef.value?.click();
}

function replaceVideoFile(index: number) {
  if (index < 0 || index >= videoFiles.value.length) {
    return;
  }

  editingVideoIndex.value = index;
  replaceVideoInputRef.value?.click();
}

function handleReplaceImageFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file || editingImageIndex.value === null) {
    target.value = "";
    editingImageIndex.value = null;
    return;
  }

  const error = validateImageFile(file);

  if (error) {
    showFileWarning(error);
    target.value = "";
    editingImageIndex.value = null;
    return;
  }

  const nextImageFiles = [...imageFiles.value];
  nextImageFiles.splice(editingImageIndex.value, 1, file);

  const limitedImageFiles = nextImageFiles.slice(0, MAX_IMAGE_COUNT);
  const totalImageSizeError = validateTotalImageSize(limitedImageFiles);

  if (totalImageSizeError) {
    showFileWarning(totalImageSizeError);
    target.value = "";
    editingImageIndex.value = null;
    return;
  }

  imageFiles.value = limitedImageFiles;
  rebuildImagePreviewUrls();

  target.value = "";
  editingImageIndex.value = null;
}

function handleReplaceVideoFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file || editingVideoIndex.value === null) {
    target.value = "";
    editingVideoIndex.value = null;
    return;
  }

  const error = validateVideoFile(file);

  if (error) {
    showFileWarning(error);
    target.value = "";
    editingVideoIndex.value = null;
    return;
  }

  videoFiles.value.splice(editingVideoIndex.value, 1, file);
  videoFiles.value = videoFiles.value.slice(0, MAX_VIDEO_COUNT);
  rebuildVideoPreviewUrls();

  target.value = "";
  editingVideoIndex.value = null;
}

function removeImageFile(index: number) {
  imageFiles.value.splice(index, 1);
  rebuildImagePreviewUrls();

  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }

  if (replaceImageInputRef.value) {
    replaceImageInputRef.value.value = "";
  }
}

function removeVideoFile(index: number) {
  videoFiles.value.splice(index, 1);
  rebuildVideoPreviewUrls();

  if (videoInputRef.value) {
    videoInputRef.value.value = "";
  }

  if (replaceVideoInputRef.value) {
    replaceVideoInputRef.value.value = "";
  }
}

function showEvidenceExample() {
  Swal.fire({
    icon: "info",
    title: "Ví dụ bằng chứng hợp lệ",
    text: evidenceGuideText.value,
    confirmButtonColor: "#bd9a5f",
  });
}

function showFileWarning(message: string) {
  Swal.fire({
    icon: "warning",
    title: "File không hợp lệ",
    text: message,
    confirmButtonColor: "#bd9a5f",
  });
}

function formatFileSize(size: number) {
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)}MB`;
  }

  return `${Math.max(1, Math.round(size / 1024))}KB`;
}

function validateForm() {
  if (!props.order) {
    return "Không tìm thấy đơn hàng cần hoàn.";
  }

  if (!form.returnType) {
    return "Vui lòng chọn tình huống bạn đang gặp.";
  }

  if (!form.reason || !selectedReason.value) {
    return "Vui lòng chọn lý do hoàn hàng hợp lệ.";
  }

  const cleanDescription = normalizeSpaces(form.description);

  if (form.description && cleanDescription.length > 2000) {
    return "Mô tả không được vượt quá 2000 ký tự.";
  }

  if (reasonNeedsDescription.value && cleanDescription.length < 10) {
    return "Vui lòng mô tả rõ hơn khi chọn lý do Khác, tối thiểu 10 ký tự.";
  }

  if (selectedReturnItems.value.length === 0) {
    return "Vui lòng chọn sản phẩm cần hoàn hàng.";
  }

  const hasInvalidQuantity = selectedReturnItems.value.some((item) => {
    return (
      !Number.isInteger(item.quantity) ||
      item.quantity <= 0 ||
      item.orderItemId <= 0
    );
  });

  if (hasInvalidQuantity) {
    return "Số lượng sản phẩm hoàn không hợp lệ.";
  }

  if (reasonNeedsEvidence.value && files.value.length === 0) {
    return "Vui lòng tải lên ảnh hoặc video bằng chứng cho lý do hoàn hàng này.";
  }

  const totalImageSizeError = validateTotalImageSize(imageFiles.value);
  if (totalImageSizeError) {
    return totalImageSizeError;
  }

  const oversizedVideo = videoFiles.value.find((file) => file.size > MAX_VIDEO_SIZE);
  if (oversizedVideo) {
    return `Video "${oversizedVideo.name}" vượt quá dung lượng 10MB.`;
  }

  if (form.refundMethod === "BANK_TRANSFER") {
    normalizeBankFields();

    const bankName = form.bankName;
    const bankAccountNumber = form.bankAccountNumber;
    const bankAccountHolder = form.bankAccountHolder;

    if (!bankName) {
      return "Vui lòng chọn ngân hàng.";
    }

    if (bankLoading.value) {
      return "Đang tải danh sách ngân hàng từ VietQR, vui lòng chờ trong giây lát.";
    }

    if (bankLoadError.value || supportedBanks.value.length === 0) {
      return "Không tải được danh sách ngân hàng từ VietQR. Vui lòng thử lại.";
    }

    if (!isSupportedBankName(bankName)) {
      return "Vui lòng chọn ngân hàng trong danh sách VietQR hỗ trợ.";
    }

    if (
      bankName.length < MIN_BANK_NAME_LENGTH ||
      bankName.length > MAX_BANK_NAME_LENGTH
    ) {
      return "Tên ngân hàng phải từ 2 đến 100 ký tự.";
    }

    if (!/[\p{L}]/u.test(bankName)) {
      return "Tên ngân hàng phải có ít nhất một chữ cái.";
    }

    if (/^[0-9]+$/.test(bankName)) {
      return "Tên ngân hàng không được chỉ gồm số.";
    }

    if (!BANK_NAME_PATTERN.test(bankName)) {
      return "Tên ngân hàng chứa ký tự không hợp lệ.";
    }

    if (!bankAccountNumber) {
      return "Vui lòng nhập số tài khoản.";
    }

    if (!/^[0-9]{6,30}$/.test(bankAccountNumber)) {
      return "Số tài khoản chỉ gồm số và từ 6 đến 30 ký tự.";
    }

    if (/^0+$/.test(bankAccountNumber)) {
      return "Số tài khoản không hợp lệ.";
    }

    if (!bankAccountHolder) {
      return "Vui lòng nhập tên chủ tài khoản.";
    }

    if (bankAccountHolder.split(/\s+/).length < 2) {
      return "Tên chủ tài khoản phải gồm ít nhất 2 từ.";
    }

    if (
      bankAccountHolder.length < MIN_BANK_ACCOUNT_HOLDER_LENGTH ||
      bankAccountHolder.length > MAX_BANK_ACCOUNT_HOLDER_LENGTH
    ) {
      return "Tên chủ tài khoản phải từ 2 đến 100 ký tự.";
    }

    if (!/[\p{L}]/u.test(bankAccountHolder)) {
      return "Tên chủ tài khoản phải có ít nhất một chữ cái.";
    }

    if (/[0-9]/.test(bankAccountHolder)) {
      return "Tên chủ tài khoản không được chứa số.";
    }

    if (!BANK_ACCOUNT_HOLDER_PATTERN.test(bankAccountHolder)) {
      return "Tên chủ tài khoản chứa ký tự không hợp lệ.";
    }
  }

  return "";
}

function submit() {
  const errorMessage = validateForm();

  if (errorMessage) {
    Swal.fire({
      icon: "warning",
      title: "Thiếu thông tin",
      text: errorMessage,
      confirmButtonColor: "#bd9a5f",
    });

    return;
  }

  if (!props.order || !form.returnType) return;

  const cleanDescription = normalizeSpaces(form.description);

  emit("submit", {
    orderId: props.order.orderId,
    returnType: form.returnType,
    reason: form.reason,
    description: cleanDescription,
    email: form.email || props.defaultEmail || "",
    refundMethod: form.refundMethod,
    bankName: form.refundMethod === "BANK_TRANSFER" ? form.bankName : null,
    bankAccountNumber:
      form.refundMethod === "BANK_TRANSFER" ? form.bankAccountNumber : null,
    bankAccountHolder:
      form.refundMethod === "BANK_TRANSFER" ? form.bankAccountHolder : null,
    returnItems: selectedReturnItems.value,
    files: files.value,
  });
}

function formatMoney(value: number | null | undefined) {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
}

function getItemImage(item: any) {
  return item?.image || item?.imageUrl || item?.thumbnailUrl || FALLBACK_IMAGE;
}

function handleImageError(event: Event) {
  const target = event.target as HTMLImageElement;
  target.src = FALLBACK_IMAGE;
}
</script>

<style scoped>
.return-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(6, 19, 43, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.return-modal {
  --return-navy: #06132b;
  --return-gold: #bd9a5f;
  --return-gold-dark: #9d7939;
  --return-gold-soft: #fffaf2;
  --return-border: #eadfca;
  --return-text: #0f172a;
  --return-muted: #64748b;

  position: relative;
  width: min(760px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.28);
  border-radius: 18px;
  box-shadow: 0 26px 76px rgba(6, 19, 43, 0.28);
  padding: 28px;
}

.return-close {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 20;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: #f8fafc;
  color: var(--return-muted);
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.18s ease;
}

.return-close:hover {
  background: var(--return-gold-soft);
  color: var(--return-gold-dark);
}

.return-title {
  color: var(--return-navy);
  font-size: 23px;
  font-weight: 800;
}

.return-subtitle {
  color: var(--return-muted);
  font-size: 13px;
  max-width: 560px;
}

.return-option-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 22px;
}

.return-type-card {
  width: 100%;
  border: 1px solid var(--return-border);
  background: #ffffff;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.return-type-card:hover,
.return-type-card.selected {
  border-color: var(--return-gold);
  background: var(--return-gold-soft);
  box-shadow: 0 12px 28px rgba(189, 154, 95, 0.12);
}

.return-type-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(189, 154, 95, 0.34);
  background: #fffdf8;
  color: var(--return-gold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.return-type-card h5 {
  margin: 0 0 8px;
  color: var(--return-navy);
  font-size: 16px;
  font-weight: 800;
}

.return-type-card p {
  margin: 0;
  color: var(--return-muted);
  font-size: 13px;
}

.return-form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding-right: 58px;
}

.return-form-header > div:first-child {
  flex: 1;
  min-width: 0;
}

.change-type-btn {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--return-gold-dark);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.3;
  white-space: nowrap;
  text-align: right;
  padding: 4px 0;
  margin-top: 4px;
}

.change-type-btn:hover {
  color: var(--return-navy);
}

.return-section {
  border-top: 8px solid #faf7f1;
  padding-top: 18px;
  margin-top: 18px;
}

.return-section h5 {
  color: var(--return-navy);
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 14px;
}

.return-product-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.return-product-item {
  display: grid;
  grid-template-columns: 58px 1fr auto;
  align-items: center;
  gap: 12px;
  border: 1px solid #efe7d8;
  border-radius: 14px;
  padding: 10px;
  background: #fffdf9;
}

.return-product-img {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid #eadfca;
}

.return-product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.return-product-info strong {
  color: var(--return-navy);
  font-size: 14px;
}

.return-product-info span {
  color: var(--return-muted);
  font-size: 12px;
}

.return-product-qty {
  color: var(--return-gold-dark);
  font-size: 13px;
  font-weight: 800;
}

.return-item-select-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.return-select-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  border: 1px solid #efe7d8;
  border-radius: 14px;
  padding: 12px;
  background: #ffffff;
  transition: all 0.18s ease;
}

.return-select-item.selected {
  border-color: var(--return-gold);
  background: var(--return-gold-soft);
  box-shadow: 0 10px 22px rgba(189, 154, 95, 0.1);
}

.return-select-main {
  display: grid !important;
  grid-template-columns: auto 66px minmax(0, 1fr);
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 0 !important;
  cursor: pointer;
  min-width: 0;
}

.return-select-checkbox {
  width: 16px;
  height: 16px;
  margin-top: 22px;
  accent-color: var(--return-gold-dark);
}

.return-select-img {
  width: 66px;
  height: 66px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eadfca;
  background: #ffffff;
  flex-shrink: 0;
}

.return-select-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.return-select-info strong {
  color: var(--return-navy);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.35;

  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;

  overflow: hidden;
}

.return-variant-line,
.return-sku-line {
  color: var(--return-muted);
  font-size: 12px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.return-price-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 2px;
}

.return-original-price {
  color: #94a3b8;
  font-size: 12px;
  text-decoration: line-through;
}

.return-final-price {
  color: var(--return-gold-dark);
  font-size: 13px;
  font-weight: 900;
}

.return-item-right {
  min-width: 132px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.return-order-qty {
  color: var(--return-gold-dark);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.return-selected-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 7px;
}

.return-quantity-box {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.return-quantity-box button {
  width: 28px;
  height: 28px;
  border: 1px solid #d8c39b;
  border-radius: 8px;
  background: #ffffff;
  color: var(--return-gold-dark);
  font-weight: 900;
}

.return-quantity-box input {
  width: 48px;
  height: 28px;
  border: 1px solid #e2d6bf;
  border-radius: 8px;
  text-align: center;
  color: var(--return-navy);
  font-weight: 800;
}

.return-quantity-box span {
  color: var(--return-muted);
  font-size: 12px;
}

.return-line-refund {
  min-width: 116px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: right;
}

.return-line-refund span {
  color: var(--return-muted);
  font-size: 11px;
  font-weight: 700;
}

.return-line-refund strong {
  color: var(--return-gold-dark);
  font-size: 13px;
  font-weight: 900;
}

.return-item-note {
  color: var(--return-muted);
  font-size: 12px;
  margin: 0 0 14px;
}

.form-row {
  margin-bottom: 14px;
}

.form-row label {
  display: block;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 7px;
}

.required {
  color: var(--return-gold-dark);
  margin-right: 3px;
}

.return-input,
.return-textarea {
  width: 100%;
  border: 1px solid #e2d6bf;
  border-radius: 10px;
  min-height: 42px;
  padding: 9px 12px;
  outline: none;
  color: var(--return-text);
  background: #ffffff;
}

.return-textarea {
  min-height: 120px;
  resize: vertical;
}

.return-input:focus,
.return-textarea:focus {
  border-color: var(--return-gold);
  box-shadow: 0 0 0 3px rgba(189, 154, 95, 0.16);
}

.reason-select {
  position: relative;
}

.reason-select-toggle {
  width: 100%;
  min-height: 44px;
  border: 1px solid #e2d6bf;
  border-radius: 10px;
  background: #ffffff;
  color: var(--return-text);
  padding: 9px 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.reason-value-text,
.reason-placeholder {
  flex: 1;
  min-width: 0;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
}

.reason-placeholder {
  color: #9ca3af;
  font-weight: 500;
}

.reason-value-text {
  color: var(--return-navy);
  font-weight: 700;
}

.reason-chevron {
  color: var(--return-gold-dark);
  font-size: 13px;
  flex-shrink: 0;
  transition: transform 0.18s ease;
}

.reason-chevron.rotated {
  transform: rotate(180deg);
}

.reason-select.open .reason-select-toggle {
  border-color: var(--return-gold);
  box-shadow: 0 0 0 3px rgba(189, 154, 95, 0.16);
}

.reason-dropdown {
  position: absolute;
  z-index: 60;
  left: 0;
  right: 0;
  top: calc(100% + 4px);
  min-height: 286px;
  max-height: 330px;
  overflow: visible;
  background: #ffffff;
  border: 1px solid var(--return-border);
  border-radius: 10px;
  box-shadow: 0 18px 40px rgba(6, 19, 43, 0.16);
  display: flex;
}

.reason-main-list {
  width: 100%;
  max-height: 330px;
  overflow-y: auto;
  padding: 6px 0;
  background: #ffffff;
  border-radius: 10px;
}

.reason-dropdown.has-submenu .reason-main-list {
  width: 50%;
  border-right: 1px solid #f1e5cc;
  border-radius: 10px 0 0 10px;
}

.reason-option {
  width: 100%;
  min-height: 44px;
  border: none;
  background: #ffffff;
  color: var(--return-text);
  padding: 10px 12px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 3px;
  cursor: pointer;
}

.reason-option:hover,
.reason-option.hovered,
.reason-option.selected {
  background: var(--return-gold-soft);
  color: var(--return-gold-dark);
}

.reason-row-head {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.reason-label {
  font-size: 14px;
  font-weight: 700;
}

.reason-child-arrow {
  color: var(--return-gold-dark);
  font-size: 13px;
  flex-shrink: 0;
}

.reason-option-description {
  color: #7c6a4b;
  font-size: 12px;
  line-height: 1.4;
}

.reason-submenu-panel {
  width: 50%;
  max-height: 330px;
  overflow-y: auto;
  padding: 6px 0;
  background: #ffffff;
  border-radius: 0 10px 10px 0;
}

.reason-sub-option {
  width: 100%;
  min-height: 44px;
  border: none;
  background: #ffffff;
  color: var(--return-text);
  padding: 10px 12px;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
}

.reason-sub-option:hover,
.reason-sub-option.selected {
  background: var(--return-gold-soft);
  color: var(--return-gold-dark);
  font-weight: 700;
}

.reason-description {
  margin-top: 6px;
  color: #7c6a4b;
  font-size: 12px;
  line-height: 1.45;
}

.char-count {
  color: #94a3b8;
  font-size: 12px;
  text-align: right;
  margin-top: 4px;
}

.file-note {
  color: var(--return-muted);
  font-size: 12px;
  margin-top: 8px;
}

.file-required-note {
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  margin-top: 6px;
}

.file-hidden-input {
  display: none;
}

.evidence-upload-grid {
  display: flex;
  align-items: stretch;
  gap: 12px;
  flex-wrap: wrap;
}

.evidence-upload-card {
  width: 116px;
  min-height: 104px;
  border: 1px dashed #d2c0a0;
  border-radius: 10px;
  background: #fffdf9;
  color: var(--return-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.evidence-upload-card i {
  color: var(--return-gold-dark);
  font-size: 22px;
}

.evidence-upload-card strong {
  color: var(--return-navy);
  font-size: 13px;
  font-weight: 800;
}

.evidence-upload-card span {
  color: var(--return-muted);
  font-size: 12px;
}

.evidence-upload-card small {
  color: #9ca3af;
  font-size: 11px;
}

.evidence-upload-card:hover:not(:disabled) {
  border-color: var(--return-gold);
  background: var(--return-gold-soft);
  box-shadow: 0 10px 22px rgba(189, 154, 95, 0.12);
}

.evidence-upload-card:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.evidence-file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.evidence-file-chip {
  display: inline-grid;
  grid-template-columns: auto minmax(80px, 1fr) auto auto;
  align-items: center;
  gap: 7px;
  max-width: 100%;
  border-radius: 999px;
  background: var(--return-gold-soft);
  color: var(--return-navy);
  border: 1px solid var(--return-border);
  padding: 7px 8px 7px 11px;
  font-size: 12px;
}

.evidence-file-chip.video {
  background: #fffdf9;
}

.evidence-file-chip > i {
  color: var(--return-gold-dark);
}

.evidence-file-chip span {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
}

.evidence-file-chip small {
  color: var(--return-muted);
  font-size: 11px;
}

.evidence-file-chip button {
  border: none;
  background: transparent;
  color: var(--return-gold-dark);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}


.evidence-title {
  display: flex !important;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.evidence-example-btn {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  padding: 0;
  cursor: pointer;
}

.evidence-example-btn:hover {
  text-decoration: underline;
}

.evidence-required-guide {
  color: #7c6a4b;
  font-size: 13px;
  line-height: 1.45;
  margin: -2px 0 10px 18px;
}

.evidence-preview-grid {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.evidence-preview-card {
  position: relative;
  width: 102px;
  height: 102px;
  border: 1px solid #d8c39b;
  border-radius: 8px;
  overflow: hidden;
  background: #fffdf9;
}

.evidence-preview-card.video::before {
  content: "\f4f4";
  font-family: "bootstrap-icons";
  position: absolute;
  top: 6px;
  left: 6px;
  z-index: 2;
  color: #ffffff;
  font-size: 16px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.55);
  pointer-events: none;
}

.evidence-preview-media {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  background: #111827;
}

.evidence-preview-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 3;
  display: flex;
  justify-content: space-between;
  background: rgba(6, 19, 43, 0.62);
}

.evidence-preview-actions button {
  width: 50%;
  height: 26px;
  border: none;
  background: transparent;
  color: #ffffff;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.evidence-preview-actions button:hover {
  background: rgba(255, 255, 255, 0.16);
}

.evidence-upload-card-square {
  width: 102px;
  min-height: 102px;
  border-radius: 8px;
}


.refund-method-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.refund-method-card {
  border: 1px solid #e2d6bf;
  border-radius: 14px;
  background: #ffffff;
  padding: 14px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.refund-method-card:hover,
.refund-method-card.selected {
  border-color: var(--return-gold);
  background: var(--return-gold-soft);
  box-shadow: 0 10px 24px rgba(189, 154, 95, 0.12);
}

.refund-method-card i {
  color: var(--return-gold);
  font-size: 22px;
}

.refund-method-card strong {
  display: block;
  color: var(--return-navy);
  font-size: 14px;
}

.refund-method-card span {
  display: block;
  color: var(--return-muted);
  font-size: 12px;
  margin-top: 4px;
}

.bank-info-box {
  margin-top: 14px;
  border: 1px solid #efe7d8;
  background: #fffdf9;
  border-radius: 14px;
  padding: 14px;
}

.bank-select {
  position: relative;
}

.bank-select-toggle {
  width: 100%;
  min-height: 48px;
  border: 1px solid #e2d6bf;
  border-radius: 10px;
  background: #ffffff;
  color: var(--return-text);
  padding: 8px 40px 8px 12px;
  outline: none;
  display: flex;
  align-items: center;
  text-align: left;
  position: relative;
}

.bank-select-toggle:disabled {
  background: #f8fafc;
  cursor: not-allowed;
}

.bank-select.open .bank-select-toggle,
.bank-select-toggle:focus {
  border-color: var(--return-gold);
  box-shadow: 0 0 0 3px rgba(189, 154, 95, 0.16);
}

.bank-select-value,
.bank-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bank-logo {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  object-fit: contain;
  background: #ffffff;
  border: 1px solid #f1e8d8;
  padding: 3px;
  flex-shrink: 0;
}

.bank-select-text,
.bank-option-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bank-select-text strong,
.bank-option-text strong {
  font-size: 13px;
  color: var(--return-navy);
  line-height: 1.25;
}

.bank-select-text small,
.bank-option-text small {
  color: var(--return-muted);
  font-size: 11px;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bank-placeholder {
  color: #64748b;
  font-size: 14px;
}

.bank-chevron {
  position: absolute;
  right: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: #8a6a36;
  transition: transform 0.18s ease;
}

.bank-chevron.rotated {
  transform: translateY(-50%) rotate(180deg);
}

.bank-dropdown {
  position: absolute;
  z-index: 20;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid #e2d6bf;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.16);
  padding: 6px;
}

.bank-option {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 8px;
  border-radius: 10px;
  text-align: left;
  cursor: pointer;
}

.bank-option:hover,
.bank-option.selected {
  background: #fff7e8;
}

.bank-option-error,
.bank-loading {
  width: 100%;
  min-height: 44px;
  color: #92400e;
  background: #fff7ed;
  border-radius: 10px;
  padding: 10px;
  font-size: 13px;
}

.bank-option-error {
  justify-content: space-between;
}

.bank-option-error strong {
  color: var(--return-gold-dark);
}

.store-refund-box {
  margin-top: 14px;
  border: 1px solid rgba(189, 154, 95, 0.36);
  background: var(--return-gold-soft);
  color: var(--return-navy);
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  gap: 10px;
  font-size: 13px;
}

.store-refund-box i {
  color: var(--return-gold-dark);
}

.return-refund-section {
  border-top: 8px solid #faf7f1;
  padding-top: 18px;
  margin-top: 18px;
}

.refund-card {
  border: 1px solid #eadfca;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #fffdf9 100%);
  padding: 18px;
}

.refund-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #efe7d8;
}

.refund-card-header h5 {
  color: var(--return-navy);
  font-size: 20px;
  font-weight: 900;
  margin: 0 0 4px;
}

.refund-card-header p {
  margin: 0;
  color: var(--return-muted);
  font-size: 12px;
}

.refund-method-pill {
  flex-shrink: 0;
  border: 1px solid rgba(189, 154, 95, 0.36);
  background: var(--return-gold-soft);
  color: var(--return-gold-dark);
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
}

.refund-total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 0;
}

.refund-total-row span {
  color: var(--return-text);
  font-size: 14px;
  font-weight: 700;
}

.refund-total-row strong {
  color: var(--return-navy);
  font-size: 22px;
  font-weight: 900;
}

.refund-summary-box {
  border: 1px solid #efe7d8;
  border-radius: 14px;
  background: #ffffff;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.refund-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  color: var(--return-muted);
  font-size: 13px;
}

.refund-line strong {
  color: var(--return-navy);
  font-size: 14px;
  font-weight: 800;
}

.refund-line.discount strong {
  color: #dc2626;
}

.refund-line.receive {
  padding-top: 12px;
  border-top: 1px dashed #eadfca;
  color: var(--return-navy);
  font-size: 16px;
  font-weight: 800;
}

.refund-line.receive strong {
  color: var(--return-gold-dark);
  font-size: 24px;
  font-weight: 900;
}

.refund-action-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.return-submit-btn {
  min-width: 150px;
  min-height: 46px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(
    135deg,
    var(--return-gold),
    var(--return-gold-dark)
  );
  color: #ffffff;
  font-weight: 900;
  letter-spacing: 0.2px;
  box-shadow: 0 10px 22px rgba(189, 154, 95, 0.24);
  transition: all 0.18s ease;
}

.return-submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(189, 154, 95, 0.3);
}

.return-submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}
:global(.swal2-container) {
  z-index: 5000 !important;
}

:global(.swal2-popup) {
  z-index: 5001 !important;
}
@media (max-width: 767.98px) {
  .return-modal {
    padding: 22px 16px;
  }

  .return-form-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding-right: 44px;
  }

  .change-type-btn {
    align-self: flex-start;
    margin-top: 0;
    font-size: 12px;
    text-align: left;
  }

  .refund-method-grid {
    grid-template-columns: 1fr;
  }

  .refund-bottom-box {
    align-items: stretch;
  }

  .refund-line,
  .refund-receive-line {
    width: 100%;
  }

  .return-submit-btn {
    width: 100%;
  }

  .reason-dropdown {
    position: static;
    margin-top: 4px;
    min-height: auto;
    max-height: 330px;
    overflow-y: auto;
    flex-direction: column;
  }

  .reason-dropdown.has-submenu .reason-main-list,
  .reason-main-list,
  .reason-submenu-panel {
    width: 100%;
    max-height: none;
    border-right: none;
    border-radius: 10px;
  }

  .evidence-upload-card {
    width: calc(50% - 6px);
  }

  .refund-card-header,
  .refund-total-row,
  .refund-line {
    flex-direction: column;
    align-items: flex-start;
  }

  .refund-method-pill {
    width: fit-content;
  }

  .return-select-item {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .return-select-main {
    grid-template-columns: auto 58px minmax(0, 1fr);
  }

  .return-select-img {
    width: 58px;
    height: 58px;
  }

  .return-item-right,
  .return-selected-actions {
    align-items: flex-start;
  }

  .return-quantity-box {
    justify-content: flex-start;
  }

  .return-line-refund {
    text-align: left;
  }
}
</style>
