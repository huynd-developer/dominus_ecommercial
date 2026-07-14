<template>
  <div
    class="cart-premium-panel rounded-3 d-flex flex-column h-100 p-3 select-none position-relative"
  >
    <div class="pos-ui-container d-flex flex-column h-100 min-h-0">
      <div
        class="cart-header d-flex align-items-center justify-content-between border-bottom border-dark-custom pb-2 mb-2 shrink-0"
      >
        <div class="d-flex align-items-center gap-2 min-w-0">
          <div class="icon-wrap rounded p-2 d-flex align-items-center justify-content-center">
            <i class="bi bi-cart3 text-gold fs-6"></i>
          </div>

          <div class="min-w-0">
            <h6 class="mb-0 text-light fw-bold text-truncate">
              Giỏ hàng
            </h6>
            <span class="text-muted-custom font-xs">
              {{ posStore.cart.length }} dòng sản phẩm
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
        <div
          v-if="posStore.activeHeldOrderId"
          class="active-held-box rounded-3 p-2 mb-2"
        >
          <div class="d-flex justify-content-between align-items-start gap-2">
            <div class="min-w-0">
              <div class="text-warning fw-bold font-xs text-truncate">
                <i class="bi bi-pause-circle me-1"></i>
                Đang mở phiếu treo #{{ posStore.activeHeldOrderId }}
              </div>

              <div class="text-muted-custom font-xs mt-1 text-truncate">
                Nhân viên giữ phiếu:
                <strong class="text-light">
                  {{ posStore.activeHeldOrderCashierName || "Theo dữ liệu hệ thống" }}
                </strong>
              </div>

              <div class="text-muted-custom font-xs mt-1">
                Không được sửa sản phẩm/khách/voucher. Chỉ thanh toán hoặc chuyển phiếu.
              </div>
            </div>

            <div class="d-flex flex-column gap-1 shrink-0">
              <button
                type="button"
                class="btn-held-outline"
                :disabled="posStore.isLoading"
                @click="handleTransferActiveHeldOrder"
              >
                Chuyển
              </button>

              <button
                type="button"
                class="btn-held-danger"
                :disabled="posStore.isLoading"
                @click="handleCancelActiveHeldOrder"
              >
                Hủy phiếu
              </button>
            </div>
          </div>
        </div>

        <div class="customer-section mb-2">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <span class="text-light fw-bold font-xs">
              Khách hàng <span class="text-danger">*</span>
            </span>

            <span
              v-if="posStore.customer?.customerId"
              class="customer-status-badge old-customer"
            >
              Khách cũ
            </span>

            <span
              v-else-if="posStore.customer"
              class="customer-status-badge new-customer"
            >
              Khách mới
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
                :disabled="lockedOrder"
                @keydown.enter="handleSearchCustomer"
              />
            </div>

            <button
              type="button"
              class="btn-mini shrink-0"
              :disabled="posStore.isLoading || lockedOrder"
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
                  :disabled="lockedOrder"
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
                  :disabled="lockedOrder"
                />
              </div>
            </div>

            <div class="d-flex justify-content-between align-items-center mt-1">
              <span class="text-muted-custom font-xs">
                Hạng:
                <strong class="text-gold">
                  {{ posStore.customer.customerRank || "BRONZE" }}
                </strong>
              </span>

              <button
                type="button"
                class="btn-clear-customer border-0 bg-transparent p-0 font-xs"
                :disabled="lockedOrder"
                @click="handleClearCustomer"
              >
                <i class="bi bi-x-circle"></i>
                Xóa khách
              </button>
            </div>
          </div>

          <div v-else class="customer-required-hint rounded-3 px-2 py-2 font-xs">
            <i class="bi bi-info-circle me-1"></i>
            Bắt buộc SĐT, họ tên, email. Không bán khách vãng lai.
          </div>
        </div>

        <div
          v-if="posStore.cart.length === 0"
          class="empty-cart d-flex flex-column align-items-center justify-content-center text-center rounded-3 mb-2"
        >
          <i class="bi bi-bag-x display-6 text-muted-custom opacity-25"></i>

          <p class="text-light opacity-75 mb-1 fw-bold font-sm">
            Giỏ hàng trống
          </p>

          <p class="text-muted-custom font-xs mb-0">
            Chọn sản phẩm hoặc quét SKU để thêm vào giỏ.
          </p>
        </div>

        <div v-else class="cart-table-wrapper rounded-3 mb-2">
          <table class="table table-cart-custom align-middle mb-0">
            <thead>
              <tr>
                <th>Sản phẩm trong giỏ</th>
                <th class="text-center">SL</th>
                <th class="text-end">Tiền</th>
                <th class="text-end"></th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="item in posStore.cart" :key="item.product.sku">
                <td class="cart-product-cell">
                  <div
                    class="cart-product-name text-truncate"
                    :title="item.product.name"
                  >
                    {{ item.product.name }}
                  </div>

                  <div
                    class="cart-variant text-truncate"
                    :title="getVariantText(item.product)"
                  >
                    <i class="bi bi-droplet-half me-1"></i>
                    {{ getVariantText(item.product) }}
                  </div>

                  <div class="cart-meta text-truncate" :title="item.product.sku">
                    SKU: {{ item.product.sku }}
                  </div>

                  <div class="cart-meta">
                    Đơn giá: {{ formatPrice(item.product.price) }} ₫
                  </div>
                </td>

                <td class="text-center qty-cell">
                  <div class="qty-box d-inline-flex align-items-center rounded-2">
                    <button
                      class="qty-btn"
                      type="button"
                      :disabled="lockedOrder"
                      @click.stop="posStore.updateQuantity(item.product.sku, item.quantity - 1)"
                    >
                      <i class="bi bi-dash"></i>
                    </button>

                    <span class="qty-number">
                      {{ item.quantity }}
                    </span>

                    <button
                      class="qty-btn"
                      type="button"
                      :disabled="lockedOrder"
                      @click.stop="posStore.updateQuantity(item.product.sku, item.quantity + 1)"
                    >
                      <i class="bi bi-plus"></i>
                    </button>
                  </div>
                </td>

                <td class="text-end money-cell">
                  <strong class="line-total">
                    {{ formatPrice(item.product.price * item.quantity) }} ₫
                  </strong>
                </td>

                <td class="text-end action-cell">
                  <button
                    class="btn-delete-item"
                    type="button"
                    :disabled="lockedOrder"
                    @click.stop="posStore.removeFromCart(item.product.sku)"
                  >
                    <i class="bi bi-trash3"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="checkout-section pt-2 border-top border-dark-custom mt-2">
          <div class="voucher-row d-flex align-items-center gap-2 mb-2">
            <div
              class="input-wrapper flex-grow-1 position-relative d-flex align-items-center min-w-0"
            >
              <i
                class="bi bi-ticket-perforated text-muted-custom position-absolute start-0 ms-2 font-xs"
              ></i>

              <input
                type="text"
                v-model.trim="posStore.voucherCode"
                placeholder="Mã giảm giá"
                class="form-input ps-4 text-uppercase"
                maxlength="50"
                :disabled="posStore.cart.length === 0 || lockedOrder || posStore.isLoading"
                @input="handleVoucherTyping"
                @keydown.enter="handleApplyVoucher"
              />
            </div>

            <button
              type="button"
              class="btn-apply-voucher shrink-0"
              :disabled="posStore.cart.length === 0 || lockedOrder || posStore.isLoading || !posStore.voucherCode"
              @click="handleApplyVoucher"
            >
              Áp
            </button>

            <button
              v-if="posStore.voucherCode || posStore.discountAmount > 0"
              type="button"
              class="btn-clear-voucher shrink-0"
              :disabled="lockedOrder || posStore.isLoading"
              title="Xóa mã giảm giá"
              @click="handleClearVoucher"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div
            v-if="posStore.discountAmount > 0"
            class="voucher-applied-box rounded-3 px-2 py-1 mb-2 d-flex justify-content-between align-items-center gap-2"
          >
            <span class="font-xs text-success text-truncate">
              <i class="bi bi-check-circle me-1"></i>
              Đã áp mã {{ posStore.voucherCode }}
            </span>

            <strong class="font-xs text-success shrink-0">
              -{{ formatPrice(posStore.discountAmount) }} ₫
            </strong>
          </div>

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
              <span>
                Giảm mã
                <strong>{{ posStore.voucherCode }}</strong>
              </span>

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

          <div class="held-actions mb-2">
            <div class="d-grid held-grid gap-2">
              <button
                type="button"
                class="btn-held"
                :disabled="posStore.cart.length === 0 || posStore.isLoading || posStore.hasPartialCashPayment || !!posStore.activeHeldOrderId"
                @click="handleHoldOrder"
              >
                <i class="bi bi-pause-circle"></i>
                Treo phiếu
              </button>

              <button
                type="button"
                class="btn-held-outline"
                :disabled="posStore.isLoading"
                @click="toggleHeldOrdersPanel"
              >
                <i class="bi bi-list-check"></i>
                Phiếu treo ({{ posStore.heldOrders.length }})
              </button>
            </div>

            <div
              v-if="posStore.showHeldOrdersPanel"
              class="held-list rounded-3 mt-2"
            >
              <div
                class="d-flex justify-content-between align-items-center px-2 py-2 border-bottom border-dark-custom"
              >
                <span class="text-light fw-bold font-xs">
                  Danh sách phiếu treo
                </span>

                <button
                  type="button"
                  class="btn-refresh-held"
                  @click="posStore.fetchHeldOrders()"
                >
                  <i class="bi bi-arrow-clockwise"></i>
                </button>
              </div>

              <div
                v-if="posStore.heldOrders.length === 0"
                class="text-muted-custom font-xs text-center py-3"
              >
                Không có phiếu treo.
              </div>

              <div
                v-for="held in posStore.heldOrders"
                :key="held.orderId"
                class="held-item px-2 py-2 border-bottom border-dark-custom"
              >
                <div class="d-flex justify-content-between gap-2">
                  <div class="min-w-0">
                    <div class="text-light fw-bold font-xs text-truncate">
                      #{{ held.orderId }} - {{ held.customerName || "Khách tại quầy" }}
                    </div>

                    <div class="text-muted-custom font-xs text-truncate">
                      {{ held.customerPhone || "Không có SĐT" }}
                    </div>

                    <div class="text-muted-custom font-xs text-truncate">
                      NV: {{ held.cashierName || "Không rõ" }}
                    </div>

                    <div class="text-gold font-xs fw-bold">
                      {{ formatPrice(held.finalAmount) }} ₫
                    </div>
                  </div>

                  <div class="d-flex flex-column gap-1 shrink-0">
                    <button
                      type="button"
                      class="btn-open-held"
                      :disabled="posStore.isLoading"
                      @click="handleOpenHeldOrder(held.orderId)"
                    >
                      Mở
                    </button>

                    <button
                      type="button"
                      class="btn-transfer-held"
                      :disabled="posStore.isLoading"
                      @click="handleTransferHeldOrder(held.orderId)"
                    >
                      Chuyển
                    </button>

                    <button
                      type="button"
                      class="btn-cancel-held"
                      :disabled="posStore.isLoading"
                      @click="handleCancelHeldOrder(held.orderId)"
                    >
                      Hủy
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="total-row d-flex justify-content-between align-items-center mb-2">
            <span class="text-light font-sm fw-bold">
              Còn phải thu
            </span>

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
              <i class="bi bi-qr-code-scan text-info"></i>
              {{ posStore.hasPartialCashPayment ? "VNPay phần còn lại" : "VNPay" }}
            </button>
          </div>

          <p
            v-if="posStore.errorMsg && !showTransferModal"
            class="text-danger font-xs mb-2 text-center fw-bold"
          >
            <i class="bi bi-exclamation-circle"></i>
            {{ posStore.errorMsg }}
          </p>

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
              {{ posStore.activeHeldOrderId ? "THANH TOÁN PHIẾU TREO" : "XÁC NHẬN THANH TOÁN" }}
            </span>
          </button>
        </div>
      </div>
    </div>

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
              :class="cashAfterThisPayment >= posStore.finalAmount ? 'text-success' : 'text-warning'"
            >
              {{
                cashAfterThisPayment >= posStore.finalAmount
                  ? "Đủ tiền mặt"
                  : "Còn thiếu " + formatPrice(posStore.finalAmount - cashAfterThisPayment) + " ₫"
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
            Nếu khách đưa thiếu, hệ thống sẽ giữ tiền mặt tạm và quay lại giỏ hàng để chọn tiếp phương thức.
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

            <span v-else>
              Hoàn tất thanh toán
            </span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="showTransferModal" class="modal-overlay">
      <div class="modal-content transfer-modal">
        <div class="modal-header">
          <h3>Chuyển phiếu treo #{{ transferOrderId }}</h3>

          <button
            class="close-btn"
            type="button"
            @click="closeTransferModal"
          >
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
              :class="{ active: selectedTransferEmployeeId === employee.employeeId }"
              @click="selectedTransferEmployeeId = employee.employeeId"
            >
              <div class="d-flex justify-content-between align-items-start gap-2">
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

          <p
            v-if="posStore.errorMsg"
            class="text-danger font-xs text-center fw-bold mt-2 mb-0"
          >
            <i class="bi bi-exclamation-circle"></i>
            {{ posStore.errorMsg }}
          </p>
        </div>

        <div class="modal-footer">
          <button
            class="btn-cancel"
            type="button"
            @click="closeTransferModal"
          >
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

            <span v-else>
              Xác nhận chuyển
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { usePosStore } from "@/modules/pos/stores/posStore";

const posStore = usePosStore();
const router = useRouter();

const customerPhoneInput = ref("");
const showCashModal = ref(false);
const displayCash = ref("");

const showTransferModal = ref(false);
const transferOrderId = ref<number | null>(null);
const transferKeyword = ref("");
const selectedTransferEmployeeId = ref<number | null>(null);

const lockedOrder = computed(() => {
  return posStore.isOrderLocked;
});

const filteredTransferTargets = computed(() => {
  const keyword = transferKeyword.value.trim().toLowerCase();

  if (!keyword) {
    return posStore.transferTargets;
  }

  return posStore.transferTargets.filter((employee) => {
    return (
      String(employee.employeeCode || "").toLowerCase().includes(keyword) ||
      String(employee.name || "").toLowerCase().includes(keyword) ||
      String(employee.email || "").toLowerCase().includes(keyword) ||
      String(employee.phone || "").toLowerCase().includes(keyword) ||
      String(employee.roleName || "").toLowerCase().includes(keyword) ||
      String(employee.jobTitle || "").toLowerCase().includes(keyword)
    );
  });
});

const formatPrice = (val?: number | null) => {
  return new Intl.NumberFormat("vi-VN").format(Number(val || 0));
};

const normalizePhone = (phone?: string | null) => {
  return (phone || "").replace(/[\s.-]/g, "").trim();
};

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
  return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email);
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

const ensureCustomerDraftFromPhoneInput = () => {
  const phone = normalizePhone(posStore.customer?.phone || customerPhoneInput.value);

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

const validateCustomerBeforeCheckout = () => {
  if (posStore.activeHeldOrderId) {
    return true;
  }

  ensureCustomerDraftFromPhoneInput();

  if (!posStore.customer) {
    posStore.errorMsg = "Vui lòng nhập số điện thoại khách hàng.";
    return false;
  }

  const phone = normalizePhone(posStore.customer.phone);
  const name = normalizeText(posStore.customer.name);
  const email = normalizeEmail(posStore.customer.email);

  if (!phone) {
    posStore.errorMsg = "Số điện thoại khách hàng không được để trống.";
    return false;
  }

  if (!isValidVietnamPhone(phone)) {
    posStore.errorMsg =
      "Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09.";
    return false;
  }

  if (!name) {
    posStore.errorMsg = "Họ tên khách hàng không được để trống.";
    return false;
  }

  if (name.length < 2 || name.length > 100) {
    posStore.errorMsg = "Họ tên khách hàng phải từ 2 đến 100 ký tự.";
    return false;
  }

  if (!email) {
    posStore.errorMsg = "Email khách hàng không được để trống.";
    return false;
  }

  if (!isValidEmail(email)) {
    posStore.errorMsg = "Email khách hàng không đúng định dạng.";
    return false;
  }

  posStore.customer.phone = phone;
  posStore.customer.name = name;
  posStore.customer.email = email;
  customerPhoneInput.value = phone;
  posStore.errorMsg = "";

  return true;
};

const handleSearchCustomer = async () => {
  if (lockedOrder.value) {
    posStore.errorMsg =
      "Đơn đang bị khóa do đã nhận tiền hoặc đang mở phiếu treo.";
    return;
  }

  const phone = normalizePhone(customerPhoneInput.value);

  if (!phone) {
    posStore.customer = null;
    posStore.errorMsg = "Vui lòng nhập số điện thoại khách hàng.";
    return;
  }

  if (!isValidVietnamPhone(phone)) {
    posStore.customer = null;
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

const handleClearCustomer = () => {
  if (lockedOrder.value) {
    posStore.errorMsg =
      "Đơn đang bị khóa do đã nhận tiền hoặc đang mở phiếu treo.";
    return;
  }

  posStore.customer = null;
  customerPhoneInput.value = "";
  posStore.errorMsg = "";
};

const handleVoucherTyping = () => {
  posStore.voucherCode = String(posStore.voucherCode || "").toUpperCase();
  posStore.handleVoucherTyping();
};

const handleApplyVoucher = async () => {
  await posStore.applyVoucher();
};

const handleClearVoucher = () => {
  posStore.clearVoucher();
};

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

const toggleHeldOrdersPanel = async () => {
  posStore.showHeldOrdersPanel = !posStore.showHeldOrdersPanel;

  if (posStore.showHeldOrdersPanel) {
    await posStore.fetchHeldOrders();
  }
};

const handleHoldOrder = async () => {
  if (!validateCustomerBeforeCheckout()) {
    return;
  }

  const result = await posStore.holdCurrentOrder();

  if (result) {
    customerPhoneInput.value = "";
    displayCash.value = "";
    showCashModal.value = false;
  }
};

const handleOpenHeldOrder = async (orderId: number) => {
  const result = await posStore.openHeldOrder(orderId);

  if (result && posStore.customer?.phone) {
    customerPhoneInput.value = normalizePhone(posStore.customer.phone);
  }
};

const openTransferModal = async (orderId: number) => {
  transferOrderId.value = orderId;
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
    posStore.errorMsg = "Mã phiếu treo không hợp lệ.";
    return;
  }

  if (!selectedTransferEmployeeId.value) {
    posStore.errorMsg = "Vui lòng chọn nhân viên nhận phiếu.";
    return;
  }

  const result = await posStore.transferHeldOrder(
    transferOrderId.value,
    selectedTransferEmployeeId.value
  );

  if (result) {
    closeTransferModal();
  }
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

const handleCheckoutAction = async () => {
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

  await posStore.processCheckout({
    paymentMethod: posStore.hasPartialCashPayment ? "MIXED" : "VNPAY",
    cashGiven: posStore.hasPartialCashPayment ? posStore.cashPaid : 0,
    transferAmount: posStore.hasPartialCashPayment
      ? posStore.remainingAmount
      : 0,
  });
};

const getCheckoutData = (checkoutResult: any) => {
  return checkoutResult?.data || checkoutResult?.response || checkoutResult || {};
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
          variantName: [item.capacityLabel, item.bottleTypeName]
            .filter(Boolean)
            .join(" - "),
          price: Number(item.unitPrice || 0),
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

  return {
    orderId: backendData.orderId || posStore.lastOrderId || "TM" + Date.now(),
    paymentMethod: backendData.paymentMethod || "CASH",

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

    cashGiven: Number(backendData.cashGiven ?? cashAfterThisPayment.value),
    transferAmount: Number(backendData.transferAmount ?? 0),
    changeAmount: Number(
      backendData.changeAmount ??
        Math.max(cashAfterThisPayment.value - posStore.finalAmount, 0)
    ),

    voucherCode: posStore.voucherCode || "",
    items,
  };
};

const processCashPayment = async () => {
  if (!validateCustomerBeforeCheckout()) {
    return;
  }

  if (rawCashGiven.value <= 0) {
    posStore.errorMsg = "Vui lòng nhập số tiền khách đưa.";
    return;
  }

  if (!posStore.activeHeldOrderId && cashAfterThisPayment.value < posStore.finalAmount) {
    const ok = posStore.registerPartialCashPayment(rawCashGiven.value);

    if (ok) {
      showCashModal.value = false;
      displayCash.value = "";
      posStore.paymentMethod = "VNPAY";
    }

    return;
  }

  if (posStore.activeHeldOrderId && rawCashGiven.value < posStore.finalAmount) {
    posStore.errorMsg =
      "Tiền khách đưa chưa đủ. Phiếu treo phải thanh toán đủ bằng CASH hoặc chọn VNPay/MIXED.";
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

.text-light-custom {
  color: #f1f5f9;
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
.btn-clear-customer:disabled,
.btn-mini:disabled,
.payment-method-btn:disabled,
.btn-held:disabled,
.btn-held-outline:disabled,
.btn-held-danger:disabled,
.btn-open-held:disabled,
.btn-transfer-held:disabled,
.btn-cancel-held:disabled,
.btn-apply-voucher:disabled,
.btn-clear-voucher:disabled {
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

.customer-status-badge {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 0.66rem;
  font-weight: 900;
}

.old-customer {
  color: #86efac;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.25);
}

.new-customer {
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.25);
}

.field-label {
  display: block;
  color: #94a3b8;
  font-size: 0.66rem;
  font-weight: 800;
  margin-bottom: 3px;
}

.btn-clear-customer {
  color: #94a3b8;
}

.btn-clear-customer:hover:not(:disabled) {
  color: #ef4444;
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

.cart-product-cell {
  width: auto;
  min-width: 0;
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

.btn-apply-voucher {
  border: 1px solid rgba(243, 198, 63, 0.42);
  background: rgba(243, 198, 63, 0.12);
  color: #f3c63f;
  border-radius: 8px;
  padding: 7px 11px;
  font-size: 0.75rem;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.btn-apply-voucher:hover:not(:disabled) {
  background: #f3c63f;
  color: #0b1120;
}

.btn-clear-voucher {
  width: 31px;
  height: 31px;
  border-radius: 8px;
  border: 1px solid rgba(239, 68, 68, 0.28);
  background: rgba(239, 68, 68, 0.08);
  color: #fca5a5;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.btn-clear-voucher:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.18);
  color: #fecaca;
}

.voucher-applied-box {
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.22);
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

.money-detail-row.text-muted-custom span,
.money-detail-row.text-muted-custom strong {
  color: #64748b !important;
}

.partial-payment-box {
  background-color: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.28);
}

.active-held-box {
  background: rgba(14, 165, 233, 0.08);
  border: 1px solid rgba(14, 165, 233, 0.28);
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
.btn-cancel-held {
  border: none;
  border-radius: 7px;
  padding: 5px 8px;
  font-size: 0.68rem;
  font-weight: 900;
  white-space: nowrap;
}

.btn-open-held {
  background: #f3c63f;
  color: #0b1120;
}

.btn-transfer-held {
  background: rgba(14, 165, 233, 0.16);
  color: #7dd3fc;
  border: 1px solid rgba(14, 165, 233, 0.35);
}

.btn-held-danger {
  border: 1px solid rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.08);
  color: #fca5a5;
  border-radius: 7px;
  padding: 5px 8px;
  font-size: 0.68rem;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.btn-held-danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.18);
  color: #fecaca;
}

.btn-cancel-held {
  background: rgba(239, 68, 68, 0.1);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.32);
}

.btn-cancel-held:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  color: #fecaca;
}

.payment-row-btns {
  grid-template-columns: 1fr 1fr;
}

.payment-method-btn {
  background-color: #131c31;
  border: 1px solid #222f4f;
  color: #94a3b8;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 9px;
  font-size: 0.78rem;
  font-weight: 900;
  transition: all 0.15s;
}

.payment-method-btn:hover:not(:disabled) {
  border-color: #384f80;
  color: #f1f5f9;
}

.payment-method-btn.active {
  border-color: #f3c63f;
  background-color: rgba(243, 198, 63, 0.05);
  color: #f3c63f;
}

.submit-pay-btn {
  background-image: linear-gradient(to right, #f3c63f, #e0aa14);
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(243, 198, 63, 0.15);
}

.submit-pay-btn:hover:not(:disabled) {
  filter: brightness(1.08);
}

.submit-pay-btn:disabled {
  background-image: none !important;
  background-color: #1e2538 !important;
  color: #475569 !important;
  box-shadow: none !important;
  cursor: not-allowed;
}

.cart-content-scroll::-webkit-scrollbar,
.cart-table-wrapper::-webkit-scrollbar,
.held-list::-webkit-scrollbar,
.transfer-list::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

.cart-content-scroll::-webkit-scrollbar-track,
.cart-table-wrapper::-webkit-scrollbar-track,
.held-list::-webkit-scrollbar-track,
.transfer-list::-webkit-scrollbar-track {
  background: transparent;
}

.cart-content-scroll::-webkit-scrollbar-thumb,
.cart-table-wrapper::-webkit-scrollbar-thumb,
.held-list::-webkit-scrollbar-thumb,
.transfer-list::-webkit-scrollbar-thumb {
  background: #1d2740;
  border-radius: 4px;
}

.cart-content-scroll::-webkit-scrollbar-thumb:hover,
.cart-table-wrapper::-webkit-scrollbar-thumb:hover,
.held-list::-webkit-scrollbar-thumb:hover,
.transfer-list::-webkit-scrollbar-thumb:hover {
  background: #384f80;
}

.spin {
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

.modal-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(11, 17, 32, 0.85);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
  border-radius: inherit;
}

.modal-content {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  width: 90%;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  color: #f8fafc;
  overflow: hidden;
}

.transfer-modal {
  max-width: 560px;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #334155;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #f1f5f9;
  font-weight: bold;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 20px;
  cursor: pointer;
}

.close-btn:hover {
  color: #ef4444;
}

.modal-body {
  padding: 20px;
}

.payment-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 14px;
}

.input-row {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.cash-input {
  width: 100%;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #475569;
  background: #0f172a;
  color: #fbbf24;
  font-size: 18px;
  font-weight: bold;
  outline: none;
}

.cash-input:focus {
  border-color: #3b82f6;
}

.btn-quick-cash {
  padding: 6px 12px;
  background: #0f172a;
  color: #94a3b8;
  border: 1px solid #334155;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.btn-quick-cash:hover {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.text-highlight {
  color: #fbbf24;
  font-size: 18px;
  font-weight: bold;
}

.text-success {
  color: #4ade80 !important;
  font-weight: bold;
}

.text-warning {
  color: #fbbf24 !important;
  font-weight: bold;
}

.text-danger {
  color: #ef4444 !important;
}

.modal-footer {
  padding: 16px 20px;
  background: #0f172a;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  padding: 8px 16px;
  background: transparent;
  color: #94a3b8;
  border: 1px solid #475569;
  border-radius: 6px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #334155;
}

.btn-confirm {
  padding: 8px 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
}

.btn-confirm:disabled {
  background: #475569;
  color: #94a3b8;
  cursor: not-allowed;
}

.btn-confirm:not(:disabled):hover {
  background: #2563eb;
}

.transfer-search-input {
  padding-left: 12px;
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
</style>