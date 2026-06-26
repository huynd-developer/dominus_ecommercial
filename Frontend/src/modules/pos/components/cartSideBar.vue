<template>
  <div class="cart-premium-panel rounded-3 d-flex flex-column h-100 p-3">
    <!-- Tiêu đề -->
    <div class="d-flex align-items-center justify-content-between border-bottom border-dark-custom pb-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <div class="icon-wrap rounded p-2">
          <i class="bi bi-cart3 text-warning"></i>
        </div>
        <div>
          <h5 class="mb-0 text-light fw-bold font-sm">Giỏ hàng</h5>
          <span class="text-muted-custom font-xs">{{ posStore.cart.length }} dòng mặt hàng</span>
        </div>
      </div>
    </div>

    <!-- DANH SÁCH MÓN ĂN / NƯỚC HOA TRONG GIỎ -->
    <div v-if="posStore.cart.length === 0" class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-center py-5">
      <div class="opacity-15 mb-2">
        <i class="bi bi-bag-dash display-4 text-light"></i>
      </div>
      <p class="text-light opacity-60 mb-0 font-xs">Giỏ hàng hiện tại đang trống</p>
    </div>

    <el-scrollbar v-else class="flex-grow-1 mb-2 pe-1">
      <div v-for="item in posStore.cart" :key="item.id" class="cart-item-row d-flex align-items-center justify-content-between py-2 text-light mb-2">
        <div class="overflow-hidden flex-grow-1 pe-2">
          <div class="fw-bold text-truncate font-sm text-light">{{ item.name }}</div>
          <div class="text-warning font-xs mt-1">{{ formatPrice(item.price) }} đ</div>
        </div>

        <!-- CỤM TĂNG GIẢM SỐ LƯỢNG CHỨC NĂNG ĐẦY ĐỦ -->
        <div class="d-flex align-items-center gap-2">
          <div class="quantity-controller-box d-flex align-items-center rounded">
            <button class="qty-btn" @click.stop="posStore.decreaseQuantity(item.id)">
              <i class="bi bi-dash"></i>
            </button>
            <span class="qty-number px-2 font-xs fw-bold">{{ item.quantity }}</span>
            <button class="qty-btn" @click.stop="posStore.addToCart(item)">
              <i class="bi bi-plus"></i>
            </button>
          </div>
          
          <!-- Nút xoá hẳn -->
          <button class="btn-delete-item p-1 ms-1 rounded" @click.stop="posStore.removeItem(item.id)">
            <i class="bi bi-trash3 text-danger font-xs"></i>
          </button>
        </div>
      </div>
    </el-scrollbar>

    <!-- FOOTER THANH TOÁN CỐ ĐỊNH -->
    <div class="checkout-section pt-3 border-top border-dark-custom">
      <div class="d-flex justify-content-between mb-2 text-muted-custom font-xs">
        <span>Tạm tính tổng đơn</span>
        <span class="text-light fw-medium">{{ formatPrice(posStore.cartTotal) }} đ</span>
      </div>
      <div class="d-flex justify-content-between align-items-center mb-3">
        <span class="text-light font-sm fw-bold">Khách cần thanh toán</span>
        <h4 class="text-warning mb-0 fw-bold fs-4">{{ formatPrice(posStore.cartTotal) }} đ</h4>
      </div>

      <!-- Hình thức thanh toán nhanh -->
      <div class="row g-2 mb-3">
        <div class="col-6">
          <button class="payment-method-btn w-100 py-2 rounded d-flex align-items-center justify-content-center gap-1">
            <i class="bi bi-cash-coin text-warning"></i> Tiền mặt
          </button>
        </div>
        <div class="col-6">
          <button class="payment-method-btn w-100 py-2 rounded d-flex align-items-center justify-content-center gap-1">
            <i class="bi bi-qr-code-scan text-info"></i> Chuyển khoản
          </button>
        </div>
      </div>

      <button class="submit-pay-btn w-100 py-2-5 rounded text-dark fw-bold uppercase" :disabled="posStore.cart.length === 0">
        XÁC NHẬN THANH TOÁN
      </button>
    </div>
  </div>
</template>

<script setup>
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val)
</script>

<style scoped>
.cart-premium-panel { background-color: #111726; border: 1px solid #1e293b; height: 100%; }
.icon-wrap { background-color: rgba(250, 204, 21, 0.08); }
.text-muted-custom { color: #64748b; }
.border-dark-custom { border-color: #1e293b !important; }
.font-sm { font-size: 0.85rem; }
.font-xs { font-size: 0.75rem; }

.cart-item-row { border-bottom: 1px solid #1e293b; }

/* CỤM ĐIỀU KHIỂN TĂNG GIẢM */
.quantity-controller-box { background-color: #090d1a; border: 1px solid #1e293b; overflow: hidden; }
.qty-btn { background: transparent; border: none; color: #94a3b8; padding: 4px 8px; cursor: pointer; transition: all 0.2s; }
.qty-btn:hover { background-color: #1e293b; color: #fff; }
.qty-number { color: #f8fafc; min-width: 24px; text-align: center; }

.btn-delete-item { background: transparent; border: none; cursor: pointer; transition: background 0.2s; }
.btn-delete-item:hover { background-color: rgba(239, 68, 68, 0.1); }

.payment-method-btn { background-color: #161f33; border: 1px solid #22314d; color: #cbd5e1; font-size: 0.8rem; cursor: pointer; transition: all 0.2s; }
.payment-method-btn:hover { border-color: #facc15; background-color: #1e293b; color: #fff; }

.submit-pay-btn { background-color: #facc15; border: none; font-size: 0.9rem; cursor: pointer; transition: opacity 0.2s; padding: 12px; }
.submit-pay-btn:hover:not(:disabled) { opacity: 0.9; }
.submit-pay-btn:disabled { background-color: #1e2430; color: #475569; cursor: not-allowed; }
</style>