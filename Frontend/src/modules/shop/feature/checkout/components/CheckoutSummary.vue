<template>
    <div class="checkout-right">
        <h3 class="summary-title">Đơn hàng của bạn ({{ totalItems }})</h3>

        <div class="mini-cart-items">
            <div class="mini-item" v-for="item in cartItems" :key="item.cartItemId">
                <div class="mini-qty">{{ item.quantity }}</div>
                <img src="https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=150"
                    class="mini-img" />
                <div class="mini-info">
                    <h4 class="item-name">{{ item.productName || item.sku }}</h4>
                    <p class="item-variant">Dung tích: <strong>{{ item.capacity }}</strong></p>
                    <p style="font-size: 14px; color: #718096; margin: 4px 0;">
                        Dung tích: <strong style="color: #b78d52;">{{ item.capacity }}</strong>
                    </p>
                    <div class="mini-price">{{ formatCurrency(item.price * item.quantity) }}</div>
                </div>
            </div>
        </div>

        <div class="summary-preview">
            <div class="summary-line"><span>Tạm tính</span><span>{{ formatCurrency(totalAmount) }}</span></div>
            <div class="summary-line">
                <span>Phí vận chuyển</span>
                <span :class="{ 'free-ship': shippingFee === 0 }">
                    {{ shippingFee === 0 ? 'Miễn phí' : formatCurrency(shippingFee) }}
                </span>
            </div>
            <div class="summary-line total-line"><span>Tổng thanh toán</span><span class="total-val">{{
                formatCurrency(finalTotal) }}</span></div>
        </div>

        <button class="btn-submit" @click="$emit('submit-order')" :disabled="isSubmitting">
            {{ isSubmitting ? 'ĐANG XỬ LÝ...' : 'XÁC NHẬN ĐẶT HÀNG' }}
        </button>
        <button class="btn-back" @click="$emit('back')">Quay lại giỏ hàng</button>
    </div>
</template>

<script setup lang="ts">
defineProps<{ cartItems: any[], totalItems: number, totalAmount: number, shippingFee: number, finalTotal: number, isSubmitting: boolean }>();
defineEmits(['submit-order', 'back']);
const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val).replace('VND', '₫');
</script>

<style scoped>
.checkout-right {
    flex: 1.2;
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
    border-bottom: 1px solid #eee;
    padding-bottom: 15px;
}

.mini-cart-items {
    max-height: 250px;
    overflow-y: auto;
    margin-bottom: 20px;
    padding-right: 5px;
}

.mini-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 15px;
    position: relative;
}

.mini-qty {
    position: absolute;
    top: -5px;
    left: -5px;
    background: #666;
    color: white;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    z-index: 2;
}

.mini-img {
    width: 50px;
    height: 50px;
    border-radius: 6px;
    border: 1px solid #eaeaea;
    object-fit: cover;
}

.mini-info {
    flex: 1;
}

.mini-name {
    font-size: 13px;
    font-weight: 600;
    color: #333;
    margin-bottom: 4px;
}

.mini-price {
    font-size: 13px;
    color: #666;
}

.summary-preview {
    margin-top: 20px;
    padding: 20px 0;
    border-top: 1px dashed #cbd5e0;
}

.summary-line {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 14px;
    color: #4a5568;
}

.free-ship {
    color: #38a169;
    font-weight: 500;
}

.total-line {
    border-top: 1px solid #e2e8f0;
    padding-top: 15px;
    margin-top: 5px;
    font-weight: bold;
    font-size: 16px;
    color: #06132b;
}

.total-val {
    font-size: 22px;
    color: #b78d52;
}

.btn-submit {
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
    margin-top: 10px;
}

.btn-submit:hover:not(:disabled) {
    background: #0a1f44;
}

.btn-submit:disabled {
    background: #a0aec0;
    cursor: not-allowed;
}

.btn-back {
    width: 100%;
    background: transparent;
    border: none;
    color: #666;
    font-size: 14px;
    text-decoration: underline;
    cursor: pointer;
    padding: 15px 0;
    margin-top: 5px;
}

.btn-back:hover {
    color: #06132b;
}
</style>