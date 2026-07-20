<template>
  <div
    class="cart-premium-panel rounded-3 d-flex flex-column h-100 p-2 select-none position-relative"
  >
    <div class="pos-ui-container d-flex flex-column h-100 min-h-0">
      <!-- HEADER -->
      <div
        class="cart-header d-flex align-items-center justify-content-between border-bottom border-dark-custom pb-2 mb-2 shrink-0"
      >
        <div class="d-flex align-items-center gap-2 min-w-0">
          <div
            class="icon-wrap rounded p-2 d-flex align-items-center justify-content-center"
          >
            <i class="bi bi-cart3 text-gold fs-6"></i>
          </div>

          <div class="min-w-0">
            <h6 class="mb-0 text-light fw-bold text-truncate">Thanh toán</h6>

            <span class="text-muted-custom font-xs">
              {{ posStore.cart.length }} sản phẩm đã chọn
            </span>
          </div>
        </div>

        <button
          v-if="posStore.cart.length > 0"
          class="btn btn-sm btn-outline-danger font-xs"
          type="button"
          @click="handleCancelOrder"
        >
          <i class="bi bi-trash"></i>
          {{ posStore.activeHeldOrderId ? "Đóng" : "Hủy" }}
        </button>
      </div>

      <div class="cart-content-scroll flex-grow-1 min-h-0 pe-1">
        <div class="customer-section mb-2">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <span class="text-light fw-bold font-xs">
              Khách hàng <span class="text-danger">*</span>
            </span>
          </div>

          <div class="d-flex align-items-center gap-2 mb-2">
            <div
              class="input-wrapper flex-grow-1 position-relative d-flex align-items-center min-w-0"
            >
              <i
                class="bi bi-telephone text-muted-custom position-absolute start-0 ms-2 font-xs"
              ></i>

              <input
                type="text"
                v-model="customerPhoneInput"
                placeholder="SĐT khách hàng"
                class="form-input ps-4"
                maxlength="12"
                :disabled="customerLocked"
                @input="handleCustomerPhoneTyping"
                @keydown.enter="handleSearchCustomer"
              />
            </div>

            <button
              type="button"
              class="btn-mini shrink-0"
              :disabled="posStore.isLoading || customerLocked"
              @click="handleSearchCustomer"
            >
              <i v-if="posStore.isLoading" class="bi bi-arrow-repeat spin"></i>
              <span v-else>Tìm</span>
            </button>
          </div>

          <div v-if="posStore.customer" class="customer-box rounded-3 p-2">
            <div class="row g-2">
              <div class="col-6">
                <label class="field-label">
                  Họ tên <span class="text-danger">*</span>
                </label>

                <input
                  type="text"
                  v-model="posStore.customer.name"
                  placeholder="Nguyễn Văn An"
                  class="form-input"
                  maxlength="100"
                  :disabled="customerLocked"
                  @input="handleCustomerChanged"
                />
              </div>

              <div class="col-6">
                <label class="field-label">
                  Email <span class="text-danger">*</span>
                </label>

                <input
                  type="email"
                  v-model="posStore.customer.email"
                  placeholder="email@gmail.com"
                  class="form-input"
                  maxlength="255"
                  :disabled="customerLocked"
                  @input="handleCustomerChanged"
                />
              </div>
            </div>

            <div
              v-if="shouldShowCustomerSaveControls"
              class="d-flex justify-content-between align-items-center mt-2 gap-2"
            >
              <div class="font-xs">
                <span
                  v-if="posStore.isCustomerSavedForCurrentInfo"
                  class="text-success fw-bold"
                >
                  <i class="bi bi-check-circle me-1"></i>
                  Đã lưu khách hàng
                </span>
              </div>

              <button
                type="button"
                class="btn-save-customer"
                :disabled="
                  customerLocked ||
                  posStore.isLoading ||
                  posStore.isCustomerSavedForCurrentInfo
                "
                @click="handleSaveCustomer"
              >
                <i
                  v-if="posStore.isLoading"
                  class="bi bi-arrow-repeat spin me-1"
                ></i>
                <i v-else class="bi bi-save me-1"></i>
                Lưu khách
              </button>
            </div>
          </div>
        </div>
        <!-- CHECKOUT -->
        <div class="checkout-section pt-2 border-top border-dark-custom mt-2">
          <!-- VOUCHER -->
          <div class="voucher-wrapper position-relative mb-2">
            <div class="voucher-row d-flex align-items-center gap-2">
              <div
                class="input-wrapper flex-grow-1 position-relative d-flex align-items-center min-w-0"
              >
                <i
                  class="bi bi-ticket-perforated text-muted-custom position-absolute start-0 ms-2 font-xs"
                ></i>

                <input
                  type="text"
                  v-model.trim="posStore.voucherCode"
                  placeholder="Chọn mã giảm giá"
                  class="form-input ps-4 text-uppercase"
                  maxlength="50"
                  autocomplete="off"
                  readonly
                  :disabled="
                    posStore.cart.length === 0 ||
                    lockedOrder ||
                    posStore.isLoading
                  "
                  @focus="openVoucherDropdown"
                  @click="openVoucherDropdown"
                  @blur="scheduleCloseVoucherDropdown"
                />
              </div>

              <button
                type="button"
                class="btn-toggle-voucher shrink-0"
                :disabled="
                  posStore.cart.length === 0 ||
                  lockedOrder ||
                  posStore.isLoading
                "
                @mousedown.prevent
                @click="toggleVoucherDropdown"
              >
                <i
                  class="bi"
                  :class="
                    showVoucherDropdown ? 'bi-chevron-up' : 'bi-chevron-down'
                  "
                ></i>
              </button>

              <button
                v-if="posStore.voucherCode || posStore.discountAmount > 0"
                type="button"
                class="btn-clear-voucher shrink-0"
                :disabled="lockedOrder || posStore.isLoading"
                title="Xóa mã giảm giá"
                @mousedown.prevent
                @click="handleClearVoucher"
              >
                <i class="bi bi-x-lg"></i>
              </button>
            </div>

            <div
              v-if="
                showVoucherDropdown && posStore.cart.length > 0 && !lockedOrder
              "
              class="voucher-dropdown-panel"
            >
              <div
                class="voucher-dropdown-header d-flex align-items-center justify-content-between gap-2"
              >
                <div class="min-w-0">
                  <div class="voucher-dropdown-title">
                    <i class="bi bi-stars me-1"></i>
                    Voucher khả dụng
                  </div>

                  <div class="voucher-dropdown-subtitle text-truncate">
                    Voucher giảm nhiều nhất được xếp lên đầu.
                  </div>
                </div>

                <button
                  type="button"
                  class="btn-refresh-voucher shrink-0"
                  :disabled="posStore.voucherLoading || posStore.isLoading"
                  @mousedown.prevent
                  @click="handleRefreshVouchers"
                >
                  <i
                    class="bi bi-arrow-clockwise"
                    :class="{ spin: posStore.voucherLoading }"
                  ></i>
                </button>
              </div>

              <div
                v-if="posStore.voucherLoading"
                class="voucher-dropdown-empty py-3 text-center"
              >
                <i class="bi bi-arrow-repeat spin me-1"></i>
                Đang tải voucher...
              </div>

              <div
                v-else-if="availableVoucherList.length === 0"
                class="voucher-dropdown-empty py-3 text-center"
              >
                Chưa có voucher phù hợp với đơn hàng này.
              </div>

              <div v-else class="voucher-dropdown-list">
                <button
                  v-for="(voucher, index) in availableVoucherList"
                  :key="voucher.code"
                  type="button"
                  class="voucher-option"
                  :class="{
                    active: isVoucherSelected(voucher),
                    best: index === 0 && voucher.eligible,
                    disabled: !voucher.eligible,
                  }"
                  :disabled="posStore.isLoading || !voucher.eligible"
                  @mousedown.prevent="handleSelectVoucher(voucher)"
                >
                  <div
                    class="d-flex justify-content-between align-items-start gap-2"
                  >
                    <div class="min-w-0 text-start">
                      <div class="d-flex align-items-center gap-2 min-w-0">
                        <span class="voucher-code text-truncate">
                          {{ voucher.code }}
                        </span>

                        <span
                          v-if="index === 0 && voucher.eligible"
                          class="voucher-best-badge"
                        >
                          Tốt nhất
                        </span>
                      </div>

                      <div class="voucher-name text-truncate">
                        {{ getVoucherDisplayTitle(voucher) }}
                      </div>

                      <div class="voucher-condition text-truncate">
                        {{ formatVoucherCondition(voucher) }}
                      </div>
                    </div>

                    <div class="voucher-discount text-end shrink-0">
                      <div>-{{ formatPrice(voucher.discountAmount) }} ₫</div>
                      <small>Tiết kiệm</small>
                    </div>
                  </div>
                </button>
              </div>
            </div>
          </div>

          <!-- MONEY DETAIL -->
          <div class="money-detail-box rounded-3 p-2 mb-2">
            <div class="money-detail-title mb-2">
              <i class="bi bi-receipt-cutoff me-1"></i>
              Chi tiết thanh toán
            </div>

            <div class="money-detail-row">
              <span>Tiền hàng</span>
              <strong>{{ formatPrice(posStore.totalAmount) }} ₫</strong>
            </div>

            <div
              v-if="posStore.discountAmount > 0"
              class="money-detail-row text-success"
            >
              <span> Giảm mã </span>

              <strong>-{{ formatPrice(posStore.discountAmount) }} ₫</strong>
            </div>

            <div v-else class="money-detail-row text-muted-custom">
              <span>Giảm giá</span>
              <strong>0 ₫</strong>
            </div>

            <div class="money-detail-divider"></div>

            <div class="money-detail-row money-detail-final">
              <span>Thành tiền sau giảm</span>
              <strong>{{ formatPrice(posStore.finalAmount) }} ₫</strong>
            </div>

            <template v-if="posStore.cashPaid > 0">
              <div class="money-detail-row text-success">
                <span>Đã nhận tiền mặt</span>
                <strong>-{{ formatPrice(posStore.cashPaid) }} ₫</strong>
              </div>

              <div class="money-detail-divider"></div>

              <div class="money-detail-row money-detail-payable">
                <span>Còn phải thu</span>
                <strong>{{ formatPrice(posStore.remainingAmount) }} ₫</strong>
              </div>
            </template>

            <template v-else>
              <div class="money-detail-row money-detail-payable">
                <span>Khách cần thanh toán</span>
                <strong>{{ formatPrice(posStore.remainingAmount) }} ₫</strong>
              </div>
            </template>
          </div>

          <div
            v-if="posStore.hasPartialCashPayment"
            class="partial-payment-box rounded-3 p-2 mb-2"
          >
            <div class="d-flex justify-content-between font-xs mb-1">
              <span class="text-muted-custom">Đã nhận tiền mặt</span>
              <strong class="text-success">
                {{ formatPrice(posStore.cashPaid) }} ₫
              </strong>
            </div>

            <div class="d-flex justify-content-between font-xs">
              <span class="text-muted-custom">Còn thiếu</span>
              <strong class="text-warning">
                {{ formatPrice(posStore.remainingAmount) }} ₫
              </strong>
            </div>

            <div class="font-xs text-muted-custom mt-1">
              Đơn đã nhận tiền, không được sửa sản phẩm/khách/voucher.
            </div>
          </div>

          <!-- HELD ACTIONS -->
          <div class="held-actions mb-2">
            <button
              type="button"
              class="btn-held w-100"
              :disabled="
                posStore.cart.length === 0 ||
                posStore.isLoading ||
                posStore.hasPartialCashPayment
              "
              @click="handleHoldOrder"
            >
              <i
                class="bi"
                :class="
                  posStore.activeHeldOrderId
                    ? 'bi-check2-circle'
                    : 'bi-pause-circle'
                "
              ></i>

              {{ posStore.activeHeldOrderId ? "Cập nhật phiếu" : "Lưu tạm" }}
            </button>
          </div>

          <!-- PAYMENT -->
          <div
            class="total-row d-flex justify-content-between align-items-center mb-2"
          >
            <span class="text-light font-sm fw-bold"> Còn phải thu </span>

            <strong class="text-gold fs-5">
              {{ formatPrice(posStore.remainingAmount) }} ₫
            </strong>
          </div>

          <div class="payment-row-btns d-grid gap-2 mb-2">
            <button
              type="button"
              class="payment-method-btn"
              :class="{ active: posStore.paymentMethod === 'CASH' }"
              @click="posStore.paymentMethod = 'CASH'"
            >
              <i class="bi bi-cash-coin text-warning"></i>
              Tiền mặt
            </button>

            <button
              type="button"
              class="payment-method-btn"
              :class="{ active: posStore.paymentMethod === 'VNPAY' }"
              @click="posStore.paymentMethod = 'VNPAY'"
            >
              <i class="bi bi-credit-card-2-front text-info"></i>
              {{ posStore.hasPartialCashPayment ? "VNPay" : "VNPay" }}
            </button>

            <button
              type="button"
              class="payment-method-btn"
              :class="{ active: posStore.paymentMethod === 'VIETQR' }"
              @click="posStore.paymentMethod = 'VIETQR'"
            >
              <i class="bi bi-qr-code-scan text-success"></i>
              {{ posStore.hasPartialCashPayment ? "VietQR" : "VietQR" }}
            </button>
          </div>
          <button
            class="submit-pay-btn w-100 py-2 rounded-3 text-dark fw-black font-sm tracking-wider"
            type="button"
            :disabled="posStore.cart.length === 0 || posStore.isLoading"
            @click="handleCheckoutAction"
          >
            <span v-if="posStore.isLoading">
              <i class="bi bi-arrow-repeat spin"></i>
              ĐANG XỬ LÝ...
            </span>

            <span v-else>
              {{
                posStore.activeHeldOrderId
                  ? "THANH TOÁN PHIẾU TREO"
                  : "XÁC NHẬN THANH TOÁN"
              }}
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- CASH MODAL -->
    <div v-if="showCashModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Thanh toán tiền mặt</h3>

          <button
            class="close-btn"
            type="button"
            @click="showCashModal = false"
          >
            ✕
          </button>
        </div>

        <div class="modal-body">
          <div v-if="posStore.hasPartialCashPayment" class="payment-row">
            <span>Đã nhận trước đó:</span>
            <span class="text-success">
              {{ formatPrice(posStore.cashPaid) }} ₫
            </span>
          </div>

          <div class="payment-row">
            <span>Còn cần thanh toán:</span>
            <span class="text-highlight">
              {{ formatPrice(posStore.remainingAmount) }} ₫
            </span>
          </div>

          <div class="payment-row input-row">
            <span>Tiền khách đưa thêm:</span>

            <div class="position-relative w-100 d-flex align-items-center">
              <input
                type="text"
                v-model="displayCash"
                placeholder="Nhập số tiền..."
                class="cash-input pe-4 text-end"
                @input="handleInputCash"
                @keydown.enter="processCashPayment"
              />

              <span
                class="position-absolute text-muted-custom fw-bold"
                style="right: 12px"
              >
                ₫
              </span>
            </div>

            <div class="d-flex gap-2 mt-2 flex-wrap">
              <button
                type="button"
                class="btn-quick-cash"
                @click="setExactAmount"
              >
                Đưa đủ
              </button>

              <button
                type="button"
                class="btn-quick-cash"
                @click="addAmount(500000)"
              >
                +500.000
              </button>

              <button
                type="button"
                class="btn-quick-cash"
                @click="addAmount(100000)"
              >
                +100.000
              </button>
            </div>
          </div>

          <div class="payment-row border-top border-dark-custom pt-3 mt-1">
            <span>Sau lần đưa này:</span>

            <span
              :class="
                cashAfterThisPayment >= posStore.finalAmount
                  ? 'text-success'
                  : 'text-warning'
              "
            >
              {{
                cashAfterThisPayment >= posStore.finalAmount
                  ? "Đủ tiền mặt"
                  : "Còn thiếu " +
                    formatPrice(posStore.finalAmount - cashAfterThisPayment) +
                    " ₫"
              }}
            </span>
          </div>

          <div
            v-if="cashAfterThisPayment >= posStore.finalAmount"
            class="payment-row"
          >
            <span>Tiền thừa dự kiến:</span>

            <span class="text-success">
              {{ formatPrice(cashAfterThisPayment - posStore.finalAmount) }} ₫
            </span>
          </div>

          <p class="text-muted-custom font-xs mb-0">
            Nếu khách đưa thiếu, hệ thống sẽ giữ tiền mặt tạm và quay lại giỏ
            hàng để chọn tiếp phương thức.
          </p>
        </div>

        <div class="modal-footer">
          <button
            class="btn-cancel"
            type="button"
            @click="showCashModal = false"
          >
            Hủy bỏ
          </button>

          <button
            class="btn-confirm"
            type="button"
            :disabled="rawCashGiven <= 0 || posStore.isLoading"
            @click="processCashPayment"
          >
            <span v-if="posStore.isLoading">
              <i class="bi bi-arrow-repeat spin"></i>
              Đang xử lý...
            </span>

            <span v-else> Hoàn tất thanh toán </span>
          </button>
        </div>
      </div>
    </div>
    <!-- VIETQR MODAL -->
    <div v-if="showVietQrModal" class="modal-overlay">
      <div class="modal-content vietqr-modal">
        <div class="modal-header">
          <h3>Thanh toán VietQR</h3>

          <button class="close-btn" type="button" @click="closeVietQrModal">
            ✕
          </button>
        </div>

        <div class="modal-body">
          <div class="vietqr-summary rounded-3 p-2 mb-3">
            <div class="payment-row mb-1">
              <span>Mã hóa đơn:</span>
              <strong class="text-light">#{{ vietQrOrderId }}</strong>
            </div>

            <div class="payment-row mb-1">
              <span>Số tiền cần chuyển:</span>
              <strong class="text-highlight">
                {{ formatPrice(posStore.pendingVietQrAmount) }} ₫
              </strong>
            </div>

            <div class="payment-row mb-0">
              <span>Nội dung CK:</span>
              <strong class="text-gold">{{ posStore.vietQrContent }}</strong>
            </div>
          </div>

          <div class="vietqr-box">
            <img
              v-if="posStore.vietQrImageUrl"
              :src="posStore.vietQrImageUrl"
              alt="VietQR"
              class="vietqr-image"
            />

            <div v-else class="text-muted-custom font-xs py-4 text-center">
              Không có ảnh VietQR.
            </div>
          </div>

          <p class="text-muted-custom font-xs mt-3 mb-0 text-center">
            Khách quét QR bằng app ngân hàng. Sau khi thấy tiền vào tài khoản,
            nhân viên bấm xác nhận để hoàn tất hóa đơn.
          </p>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" type="button" @click="closeVietQrModal">
            Đổi phương thức
          </button>

          <button
            class="btn-confirm"
            type="button"
            :disabled="posStore.isLoading || !vietQrOrderId"
            @click="confirmVietQrPayment"
          >
            <span v-if="posStore.isLoading">
              <i class="bi bi-arrow-repeat spin"></i>
              Đang xác nhận...
            </span>

            <span v-else>
              <i class="bi bi-check-circle me-1"></i>
              Đã nhận chuyển khoản
            </span>
          </button>
        </div>
      </div>
    </div>
    <!-- TRANSFER MODAL -->
    <div v-if="showTransferModal" class="modal-overlay">
      <div class="modal-content transfer-modal">
        <div class="modal-header">
          <h3>Chuyển phiếu treo #{{ transferOrderId }}</h3>

          <button class="close-btn" type="button" @click="closeTransferModal">
            ✕
          </button>
        </div>

        <div class="modal-body">
          <input
            type="text"
            v-model="transferKeyword"
            class="form-input transfer-search-input mb-3"
            placeholder="Tìm theo mã NV, tên, email, SĐT, vai trò..."
          />

          <div
            v-if="posStore.isLoading"
            class="text-center text-muted-custom py-3 font-xs"
          >
            <i class="bi bi-arrow-repeat spin"></i>
            Đang tải danh sách nhân viên...
          </div>

          <div
            v-else-if="filteredTransferTargets.length === 0"
            class="text-center text-muted-custom py-3 font-xs"
          >
            Không có nhân viên phù hợp để chuyển phiếu.
          </div>

          <div v-else class="transfer-list">
            <button
              v-for="employee in filteredTransferTargets"
              :key="employee.employeeId"
              type="button"
              class="transfer-employee-item"
              :class="{
                active: selectedTransferEmployeeId === employee.employeeId,
              }"
              @click="selectedTransferEmployeeId = employee.employeeId"
            >
              <div
                class="d-flex justify-content-between align-items-start gap-2"
              >
                <div class="min-w-0 text-start">
                  <div class="employee-name text-truncate">
                    {{ employee.name || "Chưa có tên" }}
                  </div>

                  <div class="employee-meta text-truncate">
                    Mã NV: {{ employee.employeeCode || "N/A" }}
                  </div>

                  <div class="employee-meta text-truncate">
                    Email: {{ employee.email || "N/A" }}
                  </div>

                  <div class="employee-meta text-truncate">
                    SĐT: {{ employee.phone || "N/A" }}
                  </div>
                </div>

                <div class="text-end shrink-0">
                  <span class="role-badge">
                    {{ employee.roleName || "STAFF" }}
                  </span>

                  <div class="employee-job mt-1">
                    {{ employee.jobTitle || "Nhân viên" }}
                  </div>
                </div>
              </div>
            </button>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" type="button" @click="closeTransferModal">
            Hủy
          </button>

          <button
            class="btn-confirm"
            type="button"
            :disabled="!selectedTransferEmployeeId || posStore.isLoading"
            @click="confirmTransferOrder"
          >
            <span v-if="posStore.isLoading">
              <i class="bi bi-arrow-repeat spin"></i>
              Đang chuyển...
            </span>

            <span v-else> Xác nhận chuyển </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import Swal, { type SweetAlertIcon } from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { useRouter } from "vue-router";
import { usePosStore } from "@/modules/pos/stores/posStore";

const posStore = usePosStore();
const router = useRouter();
const handleTransferHeldOrderEvent = async (event: Event) => {
  const customEvent = event as CustomEvent;
  const orderId = Number(customEvent.detail?.orderId || 0);

  if (!orderId) {
    setPosError("Mã phiếu treo không hợp lệ.");
    return;
  }

  await openTransferModal(orderId);
};

const handleCancelHeldOrderEvent = async (event: Event) => {
  const customEvent = event as CustomEvent;
  const orderId = Number(customEvent.detail?.orderId || 0);

  if (!orderId) {
    setPosError("Mã phiếu treo không hợp lệ.");
    return;
  }

  await handleCancelHeldOrder(orderId);
};

onMounted(() => {
  posStore.restorePendingCheckoutDraft();

  if (posStore.customer?.phone) {
    customerPhoneInput.value = posStore.customer.phone;
  }

  window.addEventListener(
    "pos-transfer-held-order",
    handleTransferHeldOrderEvent
  );

  window.addEventListener("pos-cancel-held-order", handleCancelHeldOrderEvent);
});

onUnmounted(() => {
  window.removeEventListener(
    "pos-transfer-held-order",
    handleTransferHeldOrderEvent
  );

  window.removeEventListener(
    "pos-cancel-held-order",
    handleCancelHeldOrderEvent
  );
});

const customerPhoneInput = ref("");
const showCashModal = ref(false);
const displayCash = ref("");
const showVietQrModal = ref(false);
const vietQrOrderId = ref<number | string | null>(null);
const pendingVietQrInvoiceSnapshot = ref<any | null>(null);
const showTransferModal = ref(false);
const transferOrderId = ref<number | null>(null);
const transferKeyword = ref("");
const selectedTransferEmployeeId = ref<number | null>(null);
const customerNameError = ref("");
const customerEmailError = ref("");

const showVoucherDropdown = ref(false);
let voucherCloseTimer: ReturnType<typeof setTimeout> | null = null;

const lockedOrder = computed(() => {
  return posStore.isOrderLocked;
});

const customerLocked = computed(() => {
  return posStore.isCustomerLocked;
});
const heldOrderCannotCheckoutMessage =
  "Phiếu này đang thuộc nhân viên khác. Vui lòng chuyển phiếu trước khi thanh toán.";

const heldOrderCannotCheckout = computed(() => {
  return Boolean(
    posStore.activeHeldOrderId && posStore.activeHeldOrderCanCheckout === false
  );
});

const availableVoucherList = computed(() => {
  return posStore.availableVouchers || [];
});

const posToast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 2600,
  timerProgressBar: true,
  background: "#0f172a",
  color: "#f8fafc",
});

const getSwalIcon = (message: string): SweetAlertIcon => {
  const lowerMessage = message.toLowerCase();

  if (
    lowerMessage.includes("thành công") ||
    lowerMessage.includes("đã lưu") ||
    lowerMessage.includes("đã hủy") ||
    lowerMessage.includes("đã chuyển") ||
    lowerMessage.includes("đã treo") ||
    lowerMessage.includes("đã cập nhật")
  ) {
    return "success";
  }

  if (
    lowerMessage.includes("còn thiếu") ||
    lowerMessage.includes("chưa đủ") ||
    lowerMessage.includes("đã nhận tiền mặt")
  ) {
    return "warning";
  }

  if (
    lowerMessage.includes("không") ||
    lowerMessage.includes("thất bại") ||
    lowerMessage.includes("lỗi") ||
    lowerMessage.includes("vui lòng") ||
    lowerMessage.includes("không hợp lệ") ||
    lowerMessage.includes("trống") ||
    lowerMessage.includes("đã thuộc") ||
    lowerMessage.includes("hết hàng") ||
    lowerMessage.includes("hết hạn")
  ) {
    return "error";
  }

  return "info";
};

let lastLocalToastMessage = "";
let lastLocalToastAt = 0;

const showPosToast = (message: string) => {
  const cleanMessage = String(message || "").trim();

  if (!cleanMessage) {
    return;
  }

  posToast.fire({
    icon: getSwalIcon(cleanMessage),
    title: cleanMessage,
  });
};

const setPosError = (message: string) => {
  const cleanMessage = String(message || "").trim();

  if (!cleanMessage) {
    return;
  }

  lastLocalToastMessage = cleanMessage;
  lastLocalToastAt = Date.now();

  posStore.errorMsg = cleanMessage;
  showPosToast(cleanMessage);
};

watch(
  () => posStore.errorMsg,
  (message) => {
    const cleanMessage = String(message || "").trim();

    if (!cleanMessage) {
      return;
    }

    const isLocalDuplicate =
      cleanMessage === lastLocalToastMessage &&
      Date.now() - lastLocalToastAt < 300;

    if (isLocalDuplicate) {
      return;
    }

    showPosToast(cleanMessage);
  }
);

const shouldShowCustomerSaveControls = computed(() => {
  if (!posStore.customer) {
    return false;
  }

  if (posStore.activeHeldOrderId) {
    return false;
  }

  return (
    !posStore.customer.customerId || !posStore.isCustomerSavedForCurrentInfo
  );
});

const filteredTransferTargets = computed(() => {
  const keyword = transferKeyword.value.trim().toLowerCase();

  if (!keyword) {
    return posStore.transferTargets;
  }

  return posStore.transferTargets.filter((employee) => {
    return (
      String(employee.employeeCode || "")
        .toLowerCase()
        .includes(keyword) ||
      String(employee.name || "")
        .toLowerCase()
        .includes(keyword) ||
      String(employee.email || "")
        .toLowerCase()
        .includes(keyword) ||
      String(employee.phone || "")
        .toLowerCase()
        .includes(keyword) ||
      String(employee.roleName || "")
        .toLowerCase()
        .includes(keyword) ||
      String(employee.jobTitle || "")
        .toLowerCase()
        .includes(keyword)
    );
  });
});

const formatPrice = (val?: number | null) => {
  return new Intl.NumberFormat("vi-VN").format(Number(val || 0));
};

const normalizePhone = (phone?: string | null) => {
  return (phone || "").replace(/\D/g, "").trim();
};

watch(
  () => [posStore.activeHeldOrderId, posStore.customer?.phone],
  () => {
    /*
     * Khi mở phiếu treo từ header,
     * cartSideBar phải tự đồng bộ lại SĐT khách hàng vào input.
     */
    if (posStore.activeHeldOrderId && posStore.customer?.phone) {
      customerPhoneInput.value = normalizePhone(posStore.customer.phone);
    }
  }
);

const normalizeText = (value?: string | null) => {
  return (value || "").trim().replace(/\s+/g, " ");
};

const normalizeEmail = (value?: string | null) => {
  return (value || "").trim().toLowerCase();
};

const isValidVietnamPhone = (phone: string) => {
  return /^(03|05|07|08|09)\d{8}$/.test(phone);
};

const isValidEmail = (email: string) => {
  const value = normalizeEmail(email);

  if (!value) return false;
  if (value.length > 255) return false;

  const parts = value.split("@");

  if (parts.length !== 2) return false;

  const [localPart, domain] = parts;

  if (!localPart || !domain) return false;
  if (localPart.startsWith(".") || localPart.endsWith(".")) return false;
  if (domain.startsWith(".") || domain.endsWith(".")) return false;
  if (value.includes("..")) return false;
  if (!/^[A-Za-z0-9._%+-]+$/.test(localPart)) return false;

  const domainParts = domain.split(".");

  if (domainParts.length < 2) return false;

  for (const label of domainParts) {
    if (!label) return false;
    if (!/^[A-Za-z0-9-]+$/.test(label)) return false;
    if (label.startsWith("-") || label.endsWith("-")) return false;
  }

  const tld = domainParts[domainParts.length - 1];

  if (!tld) return false;
  if (!/^[A-Za-z]{2,}$/.test(tld)) return false;

  return true;
};

const isValidCustomerName = (name: string) => {
  const value = normalizeText(name).normalize("NFC");

  if (!value) return false;
  if (value.length < 2 || value.length > 100) return false;

  return /^\p{L}+(?:\s+\p{L}+)*$/u.test(value);
};

const getVariantText = (product: any) => {
  return product?.subName && String(product.subName).trim()
    ? product.subName
    : "Biến thể mặc định";
};

const rawCashGiven = computed(() => {
  const raw = displayCash.value.replace(/\D/g, "");
  return raw ? Number(raw) : 0;
});

const cashAfterThisPayment = computed(() => {
  return posStore.cashPaid + rawCashGiven.value;
});

const getVoucherDisplayTitle = (voucher: any) => {
  const name = String(voucher?.name || "").trim();

  if (name && name !== voucher?.code) {
    return name;
  }

  const type = String(voucher?.discountType || "").toUpperCase();

  if ((type === "PERCENT" || type === "PERCENTAGE") && voucher?.discountValue) {
    return `Giảm ${voucher.discountValue}%`;
  }

  if (voucher?.discountPercent) {
    return `Giảm ${voucher.discountPercent}%`;
  }

  if (voucher?.discountValue) {
    return `Giảm ${formatPrice(voucher.discountValue)} ₫`;
  }

  return "Voucher giảm giá";
};

const formatVoucherCondition = (voucher: any) => {
  if (!voucher?.eligible && voucher?.ineligibleReason) {
    return voucher.ineligibleReason;
  }

  const conditions: string[] = [];

  if (voucher?.minOrderAmount && Number(voucher.minOrderAmount) > 0) {
    conditions.push(`Đơn từ ${formatPrice(voucher.minOrderAmount)} ₫`);
  }

  if (voucher?.maxDiscountAmount && Number(voucher.maxDiscountAmount) > 0) {
    conditions.push(`Tối đa ${formatPrice(voucher.maxDiscountAmount)} ₫`);
  }

  if (voucher?.remainingQuantity && Number(voucher.remainingQuantity) > 0) {
    conditions.push(`Còn ${voucher.remainingQuantity} lượt`);
  }

  return conditions.length > 0
    ? conditions.join(" • ")
    : "Có thể áp dụng cho đơn này";
};

const isVoucherSelected = (voucher: any) => {
  return (
    String(posStore.voucherCode || "").toUpperCase() ===
    String(voucher?.code || "").toUpperCase()
  );
};

const openVoucherDropdown = async () => {
  if (posStore.cart.length === 0 || lockedOrder.value || posStore.isLoading) {
    return;
  }

  if (voucherCloseTimer) {
    clearTimeout(voucherCloseTimer);
    voucherCloseTimer = null;
  }

  showVoucherDropdown.value = true;

  if (!posStore.availableVouchers || posStore.availableVouchers.length === 0) {
    await posStore.fetchAvailableVouchers();
  }
};

const scheduleCloseVoucherDropdown = () => {
  if (voucherCloseTimer) {
    clearTimeout(voucherCloseTimer);
  }

  voucherCloseTimer = setTimeout(() => {
    showVoucherDropdown.value = false;
  }, 180);
};

const toggleVoucherDropdown = async () => {
  if (posStore.cart.length === 0 || lockedOrder.value || posStore.isLoading) {
    return;
  }

  if (showVoucherDropdown.value) {
    showVoucherDropdown.value = false;
    return;
  }

  await openVoucherDropdown();
};

const handleRefreshVouchers = async () => {
  showVoucherDropdown.value = true;
  await posStore.fetchAvailableVouchers();
};

const handleSelectVoucher = async (voucher: any) => {
  if (!voucher?.code || !voucher?.eligible) {
    return;
  }

  posStore.voucherCode = String(voucher.code || "")
    .trim()
    .toUpperCase();

  const result = await posStore.applyVoucher();

  if (result) {
    showVoucherDropdown.value = false;
    await posStore.fetchAvailableVouchers();
  }
};

const ensureCustomerDraftFromPhoneInput = () => {
  const phone = normalizePhone(
    posStore.customer?.phone || customerPhoneInput.value
  );

  if (!phone) {
    return;
  }

  if (!posStore.customer) {
    posStore.customer = {
      name: "",
      phone,
      email: "",
      loyaltyPoints: 0,
    };
  } else {
    posStore.customer.phone = phone;
  }
};

const validateCustomerNameField = (showRequired = false) => {
  const name = normalizeText(posStore.customer?.name);

  if (!name) {
    customerNameError.value = showRequired
      ? "Họ tên khách hàng không được để trống."
      : "";
    return !showRequired;
  }

  if (name.length < 2 || name.length > 100) {
    customerNameError.value = "Họ tên khách hàng phải từ 2 đến 100 ký tự.";
    return false;
  }

  if (!isValidCustomerName(name)) {
    customerNameError.value =
      "Họ tên chỉ được chứa chữ cái và khoảng trắng, không được chứa số hoặc ký tự đặc biệt.";
    return false;
  }

  customerNameError.value = "";
  return true;
};

const validateCustomerEmailField = (showRequired = false) => {
  const email = normalizeEmail(posStore.customer?.email);

  if (!email) {
    customerEmailError.value = showRequired
      ? "Email khách hàng không được để trống."
      : "";
    return !showRequired;
  }

  if (!isValidEmail(email)) {
    customerEmailError.value = "Email khách hàng không đúng định dạng.";
    return false;
  }

  customerEmailError.value = "";
  return true;
};

const validateCustomerBeforeCheckout = () => {
  if (posStore.activeHeldOrderId) {
    return true;
  }

  ensureCustomerDraftFromPhoneInput();

  if (!posStore.customer) {
    setPosError("Vui lòng nhập thông tin khách hàng trước khi thanh toán.");
    return false;
  }

  const phone = normalizePhone(posStore.customer.phone);
  const name = normalizeText(posStore.customer.name);
  const email = normalizeEmail(posStore.customer.email);

  if (!phone) {
    setPosError("Vui lòng nhập số điện thoại khách hàng trước khi thanh toán.");
    return false;
  }

  if (!isValidVietnamPhone(phone)) {
    setPosError(
      "Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09."
    );
    return false;
  }

  if (!validateCustomerNameField(true)) {
    setPosError(customerNameError.value);
    return false;
  }

  if (!validateCustomerEmailField(true)) {
    setPosError(customerEmailError.value);
    return false;
  }

  posStore.customer.phone = phone;
  posStore.customer.name = name;
  posStore.customer.email = email;
  customerPhoneInput.value = phone;
  posStore.errorMsg = "";

  return true;
};

const handleCustomerChanged = () => {
  if (customerLocked.value) {
    posStore.errorMsg =
      "Không được sửa thông tin khách hàng của phiếu treo. Muốn đổi khách thì hủy phiếu cũ và tạo phiếu mới.";
    return;
  }

  posStore.markCustomerUnsaved();
};

const handleCustomerNameInput = () => {
  handleCustomerChanged();
  validateCustomerNameField(false);
};

const handleCustomerEmailInput = () => {
  handleCustomerChanged();
  validateCustomerEmailField(false);
};

const handleCustomerPhoneTyping = () => {
  if (customerLocked.value) {
    posStore.errorMsg =
      "Không được sửa số điện thoại khách hàng của phiếu treo.";
    return;
  }

  const phone = customerPhoneInput.value.replace(/\D/g, "").slice(0, 10);
  customerPhoneInput.value = phone;

  if (posStore.customer) {
    posStore.customer.phone = phone;
  }

  posStore.markCustomerUnsaved();
  posStore.errorMsg = "";
};

const handleSaveCustomer = async () => {
  if (customerLocked.value) {
    setPosError(
      "Không được lưu/sửa khách hàng khi đang mở phiếu treo hoặc đơn đã nhận tiền."
    );
    return;
  }

  ensureCustomerDraftFromPhoneInput();

  if (!validateCustomerBeforeCheckout()) {
    return;
  }

  const result = await posStore.saveCustomerForPos();

  if (!result) {
    setPosError(
      posStore.errorMsg ||
        "Lưu khách hàng thất bại. Vui lòng kiểm tra lại thông tin."
    );
    return;
  }

  if (posStore.customer?.phone) {
    customerPhoneInput.value = normalizePhone(posStore.customer.phone);
  }

  showPosToast("Đã lưu thông tin khách hàng.");
};

const handleSearchCustomer = async () => {
  if (customerLocked.value) {
    posStore.errorMsg =
      "Không được đổi khách hàng của phiếu treo. Muốn đổi khách thì hủy phiếu cũ và tạo phiếu mới.";
    return;
  }

  const phone = normalizePhone(customerPhoneInput.value);

  if (!phone) {
    posStore.customer = null;
    posStore.customerSavedKey = "";
    posStore.errorMsg = "Vui lòng nhập số điện thoại khách hàng.";
    return;
  }

  if (!isValidVietnamPhone(phone)) {
    posStore.customer = null;
    posStore.customerSavedKey = "";
    posStore.errorMsg =
      "Số điện thoại không hợp lệ. Vui lòng nhập 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.";
    return;
  }

  customerPhoneInput.value = phone;
  await posStore.searchCustomer(phone);

  if (posStore.customer?.phone) {
    customerPhoneInput.value = normalizePhone(posStore.customer.phone);
  }
};

const handleClearVoucher = async () => {
  posStore.clearVoucher();
  showVoucherDropdown.value = true;
  await posStore.fetchAvailableVouchers();
};

watch(
  () => [posStore.cart.length, posStore.totalAmount, posStore.cashPaid],
  async () => {
    if (
      posStore.cart.length === 0 ||
      lockedOrder.value ||
      posStore.hasPartialCashPayment
    ) {
      posStore.availableVouchers = [];
      showVoucherDropdown.value = false;
      return;
    }

    await posStore.fetchAvailableVouchers();
  },
  { immediate: true }
);

const handleCancelOrder = () => {
  if (posStore.activeHeldOrderId) {
    customerPhoneInput.value = "";
    displayCash.value = "";
    showCashModal.value = false;
    closeTransferModal();
    posStore.closeHeldOrderLocal();
    return;
  }

  if (posStore.hasPartialCashPayment) {
    posStore.errorMsg =
      "Đơn đã nhận tiền mặt một phần, không được hủy trực tiếp. Cần xử lý hoàn tiền hoặc quản lý xác nhận hủy.";
    return;
  }

  customerPhoneInput.value = "";
  displayCash.value = "";
  showCashModal.value = false;
  closeTransferModal();
  posStore.startNewOrder();
};

const handleInputCash = (e: Event) => {
  const target = e.target as HTMLInputElement;
  const val = target.value.replace(/\D/g, "");

  if (!val) {
    displayCash.value = "";
    return;
  }

  displayCash.value = new Intl.NumberFormat("vi-VN").format(Number(val));
};

const setExactAmount = () => {
  displayCash.value = new Intl.NumberFormat("vi-VN").format(
    posStore.remainingAmount
  );
};

const addAmount = (amount: number) => {
  const current = rawCashGiven.value;
  displayCash.value = new Intl.NumberFormat("vi-VN").format(current + amount);
};

const handleHoldOrder = async () => {
  if (!validateCustomerBeforeCheckout()) {
    return;
  }

  const wasUpdatingHeldOrder = Boolean(posStore.activeHeldOrderId);

  /*
   * Nếu chưa mở phiếu treo thì check trùng SĐT trước khi tạo phiếu mới.
   * Nếu đang mở phiếu treo thì cho cập nhật chính phiếu đó.
   */
  if (!posStore.activeHeldOrderId) {
    const currentPhone = normalizePhone(
      posStore.customer?.phone || customerPhoneInput.value
    );

    await posStore.fetchHeldOrders();

    const duplicatedHeldOrder = posStore.heldOrders.find((held: any) => {
      return normalizePhone(held.customerPhone) === currentPhone;
    });

    if (duplicatedHeldOrder) {
      const message = `Khách hàng này đang có phiếu treo #${duplicatedHeldOrder.orderId} chưa thanh toán. Vui lòng mở phiếu treo đó ở khu Đơn hàng đang xử lý để cập nhật sản phẩm.`;

      setPosError(message);

      /*
       * Header đang là nơi hiển thị phiếu treo,
       * nên chỉ cần reload danh sách, không mở panel bên cart nữa.
       */
      await posStore.fetchHeldOrders();

      return;
    }
  }

  const result = await posStore.holdCurrentOrder();

  if (!result) {
    const message =
      posStore.errorMsg ||
      "Treo phiếu thất bại. Vui lòng kiểm tra lại thông tin.";

    setPosError(message);

    if (message.includes("đang có phiếu treo")) {
      await posStore.fetchHeldOrders();
    }

    return;
  }

  customerPhoneInput.value = "";
  displayCash.value = "";
  showCashModal.value = false;
  closeTransferModal();

  /*
   * Sau khi lưu/cập nhật, reload header phiếu treo.
   */
  await posStore.fetchHeldOrders();

  showPosToast(
    wasUpdatingHeldOrder
      ? "Đã cập nhật phiếu treo thành công."
      : "Đã lưu tạm phiếu thành công. Phiếu đã hiển thị ở phía trên."
  );
};

const handleOpenHeldOrder = async (orderId: number) => {
  const result = await posStore.openHeldOrder(orderId);

  if (result && posStore.customer?.phone) {
    customerPhoneInput.value = normalizePhone(posStore.customer.phone);
  }

  if (result && posStore.voucherCode) {
    await posStore.fetchAvailableVouchers();
  }
};

const openTransferModal = async (orderId: number) => {
  if (!orderId) {
    setPosError("Mã phiếu treo không hợp lệ.");
    return;
  }

  if (posStore.cashPaid > 0) {
    setPosError(
      "Đơn đã nhận tiền mặt một phần, không được chuyển phiếu cho nhân viên khác."
    );
    return;
  }

  transferOrderId.value = Number(orderId);
  transferKeyword.value = "";
  selectedTransferEmployeeId.value = null;
  showTransferModal.value = true;

  await posStore.fetchTransferTargets();
};

const closeTransferModal = () => {
  showTransferModal.value = false;
  transferOrderId.value = null;
  transferKeyword.value = "";
  selectedTransferEmployeeId.value = null;
};

const confirmTransferOrder = async () => {
  if (!transferOrderId.value) {
    setPosError("Mã phiếu treo không hợp lệ.");
    return;
  }

  if (!selectedTransferEmployeeId.value) {
    setPosError("Vui lòng chọn nhân viên nhận phiếu.");
    return;
  }

  const currentTransferOrderId = Number(transferOrderId.value);
  const isTransferringActiveHeldOrder =
    Number(posStore.activeHeldOrderId || 0) === currentTransferOrderId;

  const result = await posStore.transferHeldOrder(
    currentTransferOrderId,
    selectedTransferEmployeeId.value
  );

  if (!result) {
    setPosError(
      posStore.errorMsg ||
        "Chuyển phiếu thất bại. Vui lòng kiểm tra nhân viên nhận phiếu."
    );
    return;
  }

  closeTransferModal();

  /*
   * Nếu đang mở chính phiếu vừa chuyển,
   * phải dọn form hiện tại để cashier không thao tác tiếp trên phiếu đã chuyển.
   */
  if (isTransferringActiveHeldOrder) {
    customerPhoneInput.value = "";
    displayCash.value = "";
    showCashModal.value = false;
    showVietQrModal.value = false;
    vietQrOrderId.value = null;
    pendingVietQrInvoiceSnapshot.value = null;
  }

  posStore.showHeldOrdersPanel = true;
  await posStore.fetchHeldOrders();

  showPosToast(posStore.errorMsg || "Đã chuyển phiếu thành công.");
};

const handleTransferHeldOrder = async (orderId: number) => {
  await openTransferModal(orderId);
};

const handleTransferActiveHeldOrder = async () => {
  if (!posStore.activeHeldOrderId) {
    return;
  }

  await openTransferModal(posStore.activeHeldOrderId);
};

const handleCancelHeldOrder = async (orderId: number) => {
  if (!orderId) {
    posStore.errorMsg = "Mã phiếu treo không hợp lệ.";
    return;
  }

  const isCancellingActiveHeldOrder = posStore.activeHeldOrderId === orderId;
  const result = await posStore.cancelHeldOrder(orderId);

  if (result && isCancellingActiveHeldOrder) {
    customerPhoneInput.value = "";
    displayCash.value = "";
    showCashModal.value = false;
    closeTransferModal();
  }
};

const handleCancelActiveHeldOrder = async () => {
  if (!posStore.activeHeldOrderId) {
    posStore.errorMsg = "Không có phiếu treo đang mở.";
    return;
  }

  await handleCancelHeldOrder(posStore.activeHeldOrderId);
};
const openVietQrModalFromCheckout = (checkoutResult: any) => {
  const backendData = getCheckoutData(checkoutResult);

  const orderId =
    backendData.orderId ||
    backendData.id ||
    backendData.orderCode ||
    posStore.pendingVietQrOrderId ||
    posStore.lastOrderId;

  if (!orderId || !backendData.vietQrImageUrl) {
    setPosError("Không nhận được mã VietQR từ máy chủ.");
    return false;
  }

  vietQrOrderId.value = orderId;
  pendingVietQrInvoiceSnapshot.value = buildInvoiceSnapshot({
    ...backendData,
    paymentMethod: backendData.paymentMethod || posStore.paymentMethod,
    transferProvider:
      backendData.transferProvider ||
      (backendData.paymentMethod === "MIXED" ? "VIETQR" : "VIETQR"),
  });
  showVietQrModal.value = true;

  return true;
};

const closeVietQrModal = () => {
  showVietQrModal.value = false;
  vietQrOrderId.value = null;
};

const confirmVietQrPayment = async () => {
  if (!vietQrOrderId.value) {
    setPosError("Không xác định được hóa đơn VietQR cần xác nhận.");
    return;
  }

  const result = await posStore.confirmVietQrPayment(vietQrOrderId.value);

  if (!result) {
    return;
  }

  const backendData = getCheckoutData(result);
  const pendingSnapshot = pendingVietQrInvoiceSnapshot.value || {};

  /*
   * BE không có bảng PaymentTransaction nên response confirm VietQR
   * không luôn nhớ được cashGiven/transferAmount của MIXED.
   * FE giữ snapshot lúc tạo QR để hóa đơn vẫn đúng CASH + VIETQR.
   */
  const invoiceSnapshot = buildInvoiceSnapshot({
    ...pendingSnapshot,
    ...backendData,
    paymentMethod:
      pendingSnapshot.paymentMethod || backendData.paymentMethod || "VIETQR",
    transferProvider:
      pendingSnapshot.transferProvider ||
      backendData.transferProvider ||
      "VIETQR",
    cashGiven:
      pendingSnapshot.cashGiven !== undefined
        ? pendingSnapshot.cashGiven
        : backendData.cashGiven,
    transferAmount:
      pendingSnapshot.transferAmount !== undefined
        ? pendingSnapshot.transferAmount
        : backendData.transferAmount,
    paidAmount: backendData.paidAmount ?? pendingSnapshot.amount,
    remainingAmount: backendData.remainingAmount ?? 0,
  });

  sessionStorage.setItem("pos_latest_invoice", JSON.stringify(invoiceSnapshot));

  showVietQrModal.value = false;
  vietQrOrderId.value = null;
  pendingVietQrInvoiceSnapshot.value = null;
  showCashModal.value = false;
  displayCash.value = "";
  customerPhoneInput.value = "";
  closeTransferModal();

  router.push({
    name: "PosPaymentResult",
    query: {
      orderId: invoiceSnapshot.orderId,
      paymentMethod: invoiceSnapshot.paymentMethod || "VIETQR",
      transferProvider: invoiceSnapshot.transferProvider || "VIETQR",
    },
  });
};

const handleCheckoutAction = async () => {
  if (heldOrderCannotCheckout.value) {
    setPosError(heldOrderCannotCheckoutMessage);
    return;
  }

  if (!validateCustomerBeforeCheckout()) {
    return;
  }

  if (posStore.paymentMethod === "CASH") {
    displayCash.value = new Intl.NumberFormat("vi-VN").format(
      posStore.remainingAmount
    );
    showCashModal.value = true;
    return;
  }

  const selectedPaymentMethod = posStore.hasPartialCashPayment
    ? "MIXED"
    : posStore.paymentMethod;

  const transferProvider =
    selectedPaymentMethod === "MIXED"
      ? posStore.paymentMethod === "VNPAY"
        ? "VNPAY"
        : "VIETQR"
      : posStore.paymentMethod === "VNPAY"
      ? "VNPAY"
      : "VIETQR";

  const checkoutResult = await posStore.processCheckout({
    paymentMethod: selectedPaymentMethod as any,
    transferProvider: transferProvider as any,
    cashGiven: selectedPaymentMethod === "MIXED" ? posStore.cashPaid : 0,
    transferAmount:
      selectedPaymentMethod === "MIXED" || selectedPaymentMethod === "VIETQR"
        ? posStore.remainingAmount
        : 0,
  });

  if (!checkoutResult) {
    return;
  }

  const backendData = getCheckoutData(checkoutResult);

  if (backendData.vietQrImageUrl) {
    openVietQrModalFromCheckout(checkoutResult);
  }
};

const getCheckoutData = (checkoutResult: any) => {
  return (
    checkoutResult?.data || checkoutResult?.response || checkoutResult || {}
  );
};

const isCheckoutSuccess = (checkoutResult: any) => {
  return checkoutResult?.success === true || !!checkoutResult?.data?.orderId;
};

const buildInvoiceSnapshot = (backendData: any = {}) => {
  const itemsFromBackend = Array.isArray(backendData.items)
    ? backendData.items
    : [];

  const items =
    itemsFromBackend.length > 0
      ? itemsFromBackend.map((item: any) => ({
          sku: item.sku,
          productName: item.productName,
          variantName:
            item.variantName ||
            [item.capacityLabel, item.bottleTypeName]
              .filter(Boolean)
              .join(" - "),
          price: Number(item.unitPrice || item.price || 0),
          quantity: Number(item.quantity || 1),
          lineTotal: Number(item.lineTotal || 0),
        }))
      : posStore.cart.map((item) => ({
          sku: item.product.sku,
          productName: item.product.name,
          variantName: item.product.subName || "",
          price: item.product.price,
          quantity: item.quantity,
          lineTotal: item.product.price * item.quantity,
        }));

  const paymentMethod =
    backendData.paymentMethod || posStore.paymentMethod || "CASH";

  const transferProvider =
    backendData.transferProvider ||
    (paymentMethod === "VNPAY"
      ? "VNPAY"
      : paymentMethod === "VIETQR"
      ? "VIETQR"
      : "");

  return {
    orderId: backendData.orderId || posStore.lastOrderId || "TM" + Date.now(),
    paymentMethod,
    transferProvider,

    customerName: backendData.customerName || posStore.customer?.name || "",
    customerPhone: backendData.customerPhone || posStore.customer?.phone || "",
    customerEmail: backendData.customerEmail || posStore.customer?.email || "",

    orderTime: backendData.createdAt || new Date().toISOString(),

    totalAmount: Number(backendData.totalAmount ?? posStore.totalAmount),
    amount: Number(backendData.finalAmount ?? posStore.finalAmount),
    discountAmount: Number(
      backendData.discountAmount ??
        Math.max((posStore.totalAmount || 0) - (posStore.finalAmount || 0), 0)
    ),

    paidAmount: Number(backendData.paidAmount ?? posStore.finalAmount),
    remainingAmount: Number(backendData.remainingAmount ?? 0),

    cashGiven: Number(backendData.cashGiven ?? cashAfterThisPayment.value ?? 0),
    transferAmount: Number(backendData.transferAmount ?? 0),
    changeAmount: Number(
      backendData.changeAmount ??
        Math.max(cashAfterThisPayment.value - posStore.finalAmount, 0)
    ),

    voucherCode: backendData.voucherCode || posStore.voucherCode || "",
    vietQrContent: backendData.vietQrContent || posStore.vietQrContent || "",
    items,
  };
};

const processCashPayment = async () => {
  if (heldOrderCannotCheckout.value) {
    setPosError(heldOrderCannotCheckoutMessage);
    return;
  }

  if (!validateCustomerBeforeCheckout()) {
    return;
  }

  if (rawCashGiven.value <= 0) {
    posStore.errorMsg = "Vui lòng nhập số tiền khách đưa.";
    return;
  }

  if (cashAfterThisPayment.value < posStore.finalAmount) {
    const ok = posStore.registerPartialCashPayment(rawCashGiven.value);

    if (ok) {
      showCashModal.value = false;
      displayCash.value = "";
      posStore.paymentMethod = "VIETQR";
    }

    return;
  }

  const checkoutResult = await posStore.processCheckout({
    paymentMethod: "CASH",
    cashGiven: posStore.activeHeldOrderId
      ? rawCashGiven.value
      : cashAfterThisPayment.value,
  });

  if (!isCheckoutSuccess(checkoutResult)) {
    return;
  }

  const backendData = getCheckoutData(checkoutResult);
  const invoiceSnapshot = buildInvoiceSnapshot(backendData);

  sessionStorage.setItem("pos_latest_invoice", JSON.stringify(invoiceSnapshot));

  showCashModal.value = false;
  displayCash.value = "";
  customerPhoneInput.value = "";
  closeTransferModal();

  posStore.startNewOrder();

  router.push({
    name: "PosPaymentResult",
    query: {
      orderId: invoiceSnapshot.orderId,
      paymentMethod: "CASH",
    },
  });
};
</script>

<style scoped>
.cart-premium-panel {
  background-color: #0b1120;
  border: 1px solid #1d2740;
  height: 100%;
}

.pos-ui-container {
  overflow: hidden;
}

.cart-content-scroll {
  overflow-y: auto;
  overflow-x: hidden;
}

.min-h-0 {
  min-height: 0;
}

.min-w-0 {
  min-width: 0;
}

.shrink-0 {
  flex-shrink: 0;
}

.text-gold {
  color: #f3c63f !important;
}

.text-muted-custom {
  color: #64748b;
}

.border-dark-custom {
  border-color: #1d2740 !important;
}

.icon-wrap {
  background-color: rgba(243, 198, 63, 0.06);
  border: 1px solid rgba(243, 198, 63, 0.15);
}

.font-sm {
  font-size: 0.875rem;
}

.font-xs {
  font-size: 0.75rem;
}

.fw-black {
  font-weight: 900;
}

.form-input {
  width: 100%;
  background-color: #131c31;
  border: 1px solid #222f4f;
  color: #f8fafc;
  border-radius: 8px;
  font-size: 0.78rem;
  padding: 7px 9px;
  outline: none;
}

.form-input:focus {
  border-color: #384f80;
  background-color: #17223b;
}

.form-input::placeholder {
  color: #536686;
}

.form-input:disabled,
.qty-btn:disabled,
.btn-delete-item:disabled,
.btn-save-customer:disabled,
.btn-mini:disabled,
.payment-method-btn:disabled,
.btn-held:disabled,
.btn-held-outline:disabled,
.btn-held-danger:disabled,
.btn-open-held:disabled,
.btn-transfer-held:disabled,
.btn-cancel-held:disabled,
.btn-clear-voucher:disabled,
.btn-toggle-voucher:disabled,
.btn-refresh-voucher:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-mini {
  background-color: #1e293b;
  border: 1px solid #334155;
  color: #f1f5f9;
  padding: 7px 12px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 800;
  cursor: pointer;
}

.btn-mini:hover:not(:disabled) {
  background-color: #f3c63f;
  border-color: #f3c63f;
  color: #0b1120;
}

.customer-box {
  background-color: #10182a;
  border: 1px solid #222f4f;
}

.customer-required-hint {
  background-color: rgba(243, 198, 63, 0.06);
  border: 1px dashed rgba(243, 198, 63, 0.3);
  color: #d6c68a;
}

.field-label {
  display: block;
  color: #94a3b8;
  font-size: 0.66rem;
  font-weight: 800;
  margin-bottom: 3px;
}

.btn-save-customer {
  border: 1px solid rgba(34, 197, 94, 0.35);
  background: rgba(34, 197, 94, 0.1);
  color: #86efac;
  border-radius: 8px;
  padding: 5px 9px;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.btn-save-customer:hover:not(:disabled) {
  background: #22c55e;
  border-color: #22c55e;
  color: #052e16;
}

.empty-cart {
  min-height: 130px;
  border: 1px dashed #263654;
  background: rgba(15, 23, 42, 0.45);
}

.cart-table-wrapper {
  border: 1px solid #1d2740;
  background: #0b1120;
  min-height: 155px;
  max-height: 230px;
  overflow-y: auto;
  overflow-x: hidden;
}

.table-cart-custom {
  width: 100%;
  color: #dbeafe;
  background: #0b1120;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 0.75rem;
  table-layout: fixed;
}

.table-cart-custom thead th {
  position: sticky;
  top: 0;
  z-index: 4;
  background: #111a2f;
  color: #94a3b8;
  border-bottom: 1px solid #263654;
  padding: 7px 8px;
  font-size: 0.68rem;
  text-transform: uppercase;
  white-space: nowrap;
}

.table-cart-custom tbody td {
  background: #0b1120;
  border-bottom: 1px solid #18243c;
  padding: 7px 8px;
  vertical-align: middle;
}

.table-cart-custom tbody tr:hover td {
  background: #121c31;
}

.qty-cell {
  width: 76px;
}

.money-cell {
  width: 95px;
}

.action-cell {
  width: 34px;
}

.cart-product-name {
  color: #f8fafc;
  font-weight: 900;
  max-width: 100%;
  line-height: 1.2;
}

.cart-variant {
  color: #f3c63f;
  font-weight: 800;
  font-size: 0.72rem;
  max-width: 100%;
  line-height: 1.2;
}

.cart-meta {
  color: #64748b;
  font-size: 0.66rem;
  max-width: 100%;
  line-height: 1.25;
}

.qty-box {
  background-color: #070c18;
  border: 1px solid #222f4f;
  overflow: hidden;
}

.qty-btn {
  width: 22px;
  height: 23px;
  background: transparent;
  border: none;
  color: #94a3b8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.qty-btn:hover:not(:disabled) {
  background: #222f4f;
  color: #fff;
}

.qty-number {
  min-width: 22px;
  text-align: center;
  color: #f8fafc;
  font-weight: 900;
  font-size: 0.74rem;
}

.line-total {
  color: #f8fafc;
  white-space: nowrap;
  font-size: 0.72rem;
}

.btn-delete-item {
  border: 1px solid rgba(239, 68, 68, 0.18);
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;
  border-radius: 7px;
  width: 27px;
  height: 27px;
}

.btn-delete-item:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.16);
}

.voucher-wrapper {
  position: relative;
  z-index: 60;
}

.btn-toggle-voucher,
.btn-clear-voucher {
  width: 31px;
  height: 31px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: #111827;
  color: #cbd5e1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.btn-toggle-voucher:hover:not(:disabled) {
  border-color: #f3c63f;
  color: #f3c63f;
}

.btn-clear-voucher {
  border-color: rgba(239, 68, 68, 0.28);
  background: rgba(239, 68, 68, 0.08);
  color: #fca5a5;
}

.btn-clear-voucher:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.18);
  color: #fecaca;
}

.voucher-dropdown-panel {
  position: absolute;
  top: calc(100% + 7px);
  left: 0;
  right: 0;
  z-index: 999;
  background: #0f172a;
  border: 1px solid #263654;
  border-radius: 12px;
  padding: 9px;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.42);
}

.voucher-dropdown-header {
  border-bottom: 1px solid #263654;
  padding-bottom: 7px;
  margin-bottom: 7px;
}

.voucher-dropdown-title {
  color: #f8fafc;
  font-size: 0.76rem;
  font-weight: 900;
}

.voucher-dropdown-subtitle {
  color: #64748b;
  font-size: 0.66rem;
}

.btn-refresh-voucher {
  border: 1px solid #334155;
  background: #111827;
  color: #cbd5e1;
  border-radius: 8px;
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.btn-refresh-voucher:hover:not(:disabled) {
  border-color: #f3c63f;
  color: #f3c63f;
}

.voucher-dropdown-empty {
  color: #94a3b8;
  font-size: 0.75rem;
}

.voucher-dropdown-list {
  max-height: 230px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding-right: 2px;
}

.voucher-option {
  width: 100%;
  border: 1px solid #263654;
  background: #0b1120;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 8px 9px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.voucher-option:hover:not(:disabled) {
  border-color: rgba(243, 198, 63, 0.55);
  background: #111c32;
}

.voucher-option.best {
  border-color: rgba(243, 198, 63, 0.55);
  background: rgba(243, 198, 63, 0.08);
}

.voucher-option.active {
  border-color: #22c55e;
  background: rgba(34, 197, 94, 0.08);
}

.voucher-option.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.voucher-code {
  color: #f3c63f;
  font-size: 0.76rem;
  font-weight: 900;
  max-width: 130px;
}

.voucher-best-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 2px 6px;
  background: rgba(243, 198, 63, 0.16);
  border: 1px solid rgba(243, 198, 63, 0.32);
  color: #f3c63f;
  font-size: 0.62rem;
  font-weight: 900;
  white-space: nowrap;
}

.voucher-name {
  color: #cbd5e1;
  font-size: 0.7rem;
  font-weight: 700;
  margin-top: 2px;
}

.voucher-condition {
  color: #64748b;
  font-size: 0.65rem;
  margin-top: 2px;
}

.voucher-discount {
  color: #4ade80;
  font-size: 0.74rem;
  font-weight: 900;
  white-space: nowrap;
}

.voucher-discount small {
  display: block;
  color: #64748b;
  font-size: 0.62rem;
  font-weight: 700;
}

.money-detail-box {
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid #263654;
}

.money-detail-title {
  color: #f3c63f;
  font-size: 0.74rem;
  font-weight: 900;
}

.money-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #94a3b8;
  font-size: 0.74rem;
  margin-bottom: 6px;
}

.money-detail-row strong {
  color: #f8fafc;
  white-space: nowrap;
}

.money-detail-divider {
  height: 1px;
  background: #263654;
  margin: 7px 0;
}

.money-detail-final span,
.money-detail-final strong {
  color: #f8fafc;
  font-weight: 900;
}

.money-detail-payable span,
.money-detail-payable strong {
  color: #f3c63f;
  font-weight: 900;
  font-size: 0.82rem;
}

.money-detail-row.text-success span,
.money-detail-row.text-success strong {
  color: #4ade80 !important;
}

.partial-payment-box {
  background-color: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.28);
}

.active-held-box {
  background: rgba(243, 198, 63, 0.08);
  border: 1px solid rgba(243, 198, 63, 0.28);
}

.held-grid {
  grid-template-columns: 1fr 1fr;
}

.btn-held,
.btn-held-outline {
  border-radius: 9px;
  padding: 8px 10px;
  font-size: 0.76rem;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-held {
  border: 1px solid rgba(243, 198, 63, 0.42);
  background: rgba(243, 198, 63, 0.12);
  color: #f3c63f;
}

.btn-held:hover:not(:disabled) {
  background: #f3c63f;
  color: #0b1120;
}

.btn-held-outline {
  border: 1px solid #334155;
  background: #131c31;
  color: #cbd5e1;
}

.btn-held-outline:hover:not(:disabled) {
  border-color: #f3c63f;
  color: #f3c63f;
}

.held-list {
  background: #0f172a;
  border: 1px solid #263654;
  max-height: 165px;
  overflow-y: auto;
}

.held-item:hover {
  background: #111c32;
}

.btn-refresh-held {
  border: 1px solid #334155;
  background: #111827;
  color: #cbd5e1;
  border-radius: 7px;
  width: 28px;
  height: 28px;
}

.btn-open-held,
.btn-transfer-held,
.btn-cancel-held,
.btn-held-danger {
  border-radius: 7px;
  padding: 5px 8px;
  font-size: 0.68rem;
  font-weight: 900;
  cursor: pointer;
}

.btn-open-held {
  background: rgba(34, 197, 94, 0.12);
  color: #86efac;
  border: 1px solid rgba(34, 197, 94, 0.28);
}

.btn-transfer-held {
  background: rgba(59, 130, 246, 0.12);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.28);
}

.btn-cancel-held,
.btn-held-danger {
  background: rgba(239, 68, 68, 0.1);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.28);
}

.total-row {
  border-top: 1px solid #263654;
  padding-top: 8px;
}

.payment-row-btns {
  grid-template-columns: repeat(3, 1fr);
}

.payment-method-btn {
  background-color: #131c31;
  border: 1px solid #222f4f;
  color: #94a3b8;
  border-radius: 9px;
  padding: 8px 10px;
  font-size: 0.76rem;
  font-weight: 900;
  cursor: pointer;
}

.payment-method-btn.active {
  border-color: #f3c63f;
  background-color: rgba(243, 198, 63, 0.12);
  color: #f3c63f;
}

.submit-pay-btn {
  background: linear-gradient(135deg, #f3c63f, #facc15);
  border: none;
  letter-spacing: 0.03em;
}

.submit-pay-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(2, 6, 23, 0.72);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.modal-content {
  width: min(450px, 100%);
  background: #0b1120;
  border: 1px solid #263654;
  border-radius: 16px;
  box-shadow: 0 22px 70px rgba(0, 0, 0, 0.45);
  overflow: hidden;
}

.transfer-modal {
  width: min(640px, 100%);
}

.modal-header {
  padding: 16px 18px;
  border-bottom: 1px solid #263654;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  color: #f8fafc;
  font-size: 1rem;
  font-weight: 900;
  margin: 0;
}

.close-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 1.1rem;
}

.modal-body {
  padding: 18px;
}

.modal-footer {
  padding: 14px 18px;
  border-top: 1px solid #263654;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.payment-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #cbd5e1;
  font-size: 0.86rem;
  margin-bottom: 12px;
}

.input-row {
  flex-direction: column;
}

.text-highlight {
  color: #f3c63f;
  font-weight: 900;
}

.cash-input {
  width: 100%;
  background-color: #131c31;
  border: 1px solid #334155;
  color: #f8fafc;
  border-radius: 10px;
  padding: 10px 36px 10px 12px;
  outline: none;
  font-weight: 900;
}

.cash-input:focus {
  border-color: #f3c63f;
}

.btn-quick-cash {
  border: 1px solid #334155;
  background: #111827;
  color: #cbd5e1;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.75rem;
  font-weight: 800;
}

.btn-quick-cash:hover {
  background: #f3c63f;
  color: #0b1120;
  border-color: #f3c63f;
}

.btn-cancel,
.btn-confirm {
  border-radius: 10px;
  padding: 9px 14px;
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
}

.btn-cancel {
  border: 1px solid #334155;
  background: #111827;
  color: #cbd5e1;
}

.btn-confirm {
  border: 1px solid #f3c63f;
  background: #f3c63f;
  color: #0b1120;
}

.btn-confirm:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.transfer-search-input {
  height: 40px;
}

.transfer-list {
  max-height: 330px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transfer-employee-item {
  width: 100%;
  border: 1px solid #334155;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.transfer-employee-item:hover {
  border-color: #f3c63f;
  background: #111c32;
}

.transfer-employee-item.active {
  border-color: #f3c63f;
  background: rgba(243, 198, 63, 0.1);
}

.employee-name {
  color: #f8fafc;
  font-weight: 900;
  font-size: 0.9rem;
  max-width: 310px;
}

.employee-meta {
  color: #94a3b8;
  font-size: 0.74rem;
  margin-top: 2px;
  max-width: 310px;
}

.role-badge {
  display: inline-flex;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.28);
  color: #93c5fd;
  font-size: 0.68rem;
  font-weight: 900;
}

.employee-job {
  color: #f3c63f;
  font-size: 0.7rem;
  font-weight: 800;
}

.spin {
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.tracking-wider {
  letter-spacing: 0.04em;
}
.cart-content-scroll,
.cart-table-wrapper,
.voucher-dropdown-list,
.held-list,
.transfer-list {
  scrollbar-width: thin;
  scrollbar-color: rgba(243, 198, 63, 0.72) rgba(15, 23, 42, 0.35);
  scrollbar-gutter: stable;
}

.cart-content-scroll::-webkit-scrollbar,
.cart-table-wrapper::-webkit-scrollbar,
.voucher-dropdown-list::-webkit-scrollbar,
.held-list::-webkit-scrollbar,
.transfer-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.cart-content-scroll::-webkit-scrollbar-track,
.cart-table-wrapper::-webkit-scrollbar-track,
.voucher-dropdown-list::-webkit-scrollbar-track,
.held-list::-webkit-scrollbar-track,
.transfer-list::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.45);
  border-radius: 999px;
  margin: 4px 0;
}

.cart-content-scroll::-webkit-scrollbar-thumb,
.cart-table-wrapper::-webkit-scrollbar-thumb,
.voucher-dropdown-list::-webkit-scrollbar-thumb,
.held-list::-webkit-scrollbar-thumb,
.transfer-list::-webkit-scrollbar-thumb {
  background: linear-gradient(
    180deg,
    rgba(243, 198, 63, 0.95),
    rgba(147, 197, 253, 0.62)
  );
  border-radius: 999px;
  border: 2px solid rgba(11, 17, 32, 0.95);
}

.cart-content-scroll::-webkit-scrollbar-thumb:hover,
.cart-table-wrapper::-webkit-scrollbar-thumb:hover,
.voucher-dropdown-list::-webkit-scrollbar-thumb:hover,
.held-list::-webkit-scrollbar-thumb:hover,
.transfer-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(
    180deg,
    rgba(243, 198, 63, 1),
    rgba(147, 197, 253, 0.9)
  );
}

.cart-content-scroll::-webkit-scrollbar-corner,
.cart-table-wrapper::-webkit-scrollbar-corner,
.voucher-dropdown-list::-webkit-scrollbar-corner,
.held-list::-webkit-scrollbar-corner,
.transfer-list::-webkit-scrollbar-corner {
  background: transparent;
}

.vietqr-modal {
  width: min(430px, 100%);
}

.vietqr-summary {
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid #263654;
}

.vietqr-box {
  background: #ffffff;
  border-radius: 14px;
  padding: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 260px;
}

.vietqr-image {
  width: 100%;
  max-width: 260px;
  display: block;
  object-fit: contain;
}
.held-note {
  color: #64748b;
  font-size: 0.68rem;
  font-weight: 700;
  line-height: 1.25;
}
.cart-premium-panel {
  padding: 10px !important;
}

.cart-header {
  padding-bottom: 6px !important;
  margin-bottom: 6px !important;
}

.cart-header .icon-wrap {
  width: 36px;
  height: 36px;
  padding: 0 !important;
}

.cart-header h6 {
  font-size: 0.92rem;
  line-height: 1.15;
}

.cart-header .font-xs {
  font-size: 0.68rem;
}

.cart-content-scroll {
  overflow-y: auto;
  padding-right: 4px !important;
}

.customer-section {
  margin-bottom: 6px !important;
}

.customer-section .mb-1 {
  margin-bottom: 4px !important;
}

.customer-section .mb-2 {
  margin-bottom: 6px !important;
}

.customer-box {
  padding: 8px !important;
}

.customer-box .row {
  --bs-gutter-x: 8px;
  --bs-gutter-y: 6px;
}

.field-label {
  font-size: 0.68rem;
  margin-bottom: 3px;
  line-height: 1.1;
}

.form-input {
  min-height: 36px;
  height: 36px;
  font-size: 0.78rem;
  padding-top: 6px;
  padding-bottom: 6px;
}

.btn-mini {
  min-height: 36px;
  height: 36px;
  padding: 0 14px;
}

.checkout-section {
  padding-top: 6px !important;
  margin-top: 6px !important;
}

.voucher-wrapper {
  margin-bottom: 6px !important;
}

.btn-toggle-voucher,
.btn-clear-voucher {
  width: 36px;
  height: 36px;
}

.money-detail-box {
  padding: 8px !important;
  margin-bottom: 6px !important;
}

.money-detail-title {
  margin-bottom: 6px !important;
  font-size: 0.78rem;
  line-height: 1.1;
}

.money-detail-row {
  font-size: 0.74rem;
  line-height: 1.15;
  margin-bottom: 5px;
}

.money-detail-final,
.money-detail-payable {
  font-size: 0.78rem;
}

.money-detail-divider {
  margin: 6px 0 !important;
}

.partial-payment-box {
  padding: 7px !important;
  margin-bottom: 6px !important;
}

.held-actions {
  margin-bottom: 6px !important;
}

.btn-held {
  min-height: 38px;
  height: 38px;
  padding: 0 10px;
  font-size: 0.78rem;
}

.held-note {
  font-size: 0.64rem;
  line-height: 1.15;
  margin-top: 4px !important;
}

.total-row {
  margin-bottom: 6px !important;
}

.total-row span {
  font-size: 0.82rem;
}

.total-row strong {
  font-size: 1.05rem !important;
}

.payment-row-btns {
  gap: 6px !important;
  margin-bottom: 6px !important;
}

.payment-method-btn {
  min-height: 38px;
  height: 38px;
  padding: 0 8px;
  font-size: 0.76rem;
}

.submit-pay-btn {
  min-height: 42px;
  height: 42px;
  padding: 0 !important;
  font-size: 0.82rem !important;
}

@media (max-height: 760px) {
  .cart-premium-panel {
    padding: 8px !important;
  }

  .cart-header .icon-wrap {
    width: 32px;
    height: 32px;
  }

  .customer-box {
    padding: 6px !important;
  }

  .form-input,
  .btn-mini,
  .btn-toggle-voucher,
  .btn-clear-voucher {
    min-height: 34px;
    height: 34px;
  }

  .money-detail-box {
    padding: 7px !important;
  }

  .money-detail-row {
    margin-bottom: 4px;
  }

  .btn-held,
  .payment-method-btn {
    height: 36px;
    min-height: 36px;
  }

  .submit-pay-btn {
    height: 40px;
    min-height: 40px;
  }
}
.held-checkout-warning {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.28);
  border-radius: 10px;
  padding: 7px 9px;
  font-size: 0.68rem;
  font-weight: 900;
  line-height: 1.25;
}
</style>
