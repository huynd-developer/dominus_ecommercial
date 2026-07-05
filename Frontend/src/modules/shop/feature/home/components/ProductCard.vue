<template>
  <div class="product-card h-100 position-relative overflow-hidden" @click="goToDetail">
    <span v-if="product.discountPercent" class="discount-badge">
      -{{ product.discountPercent }}%
    </span>

    <div class="product-image-wrapper">
      <div class="product-bottle" :style="getBottleStyle(product.color)">
        <div class="product-bottle-cap"></div>
        <div class="product-bottle-neck"></div>
        <div class="product-bottle-body">
          <div class="product-bottle-label">
            <strong>{{ shortBrand }}</strong>
            <span>PERFUME</span>
          </div>
        </div>
      </div>
    </div>

    <div class="product-content">
      <p class="product-brand mb-1">
        {{ product.brand }}
      </p>

      <h3 class="product-name text-truncate mb-2">
        {{ product.name }}
      </h3>

      <div class="rating-row d-flex align-items-center gap-2 mb-2">
        <span class="stars">★★★★★</span>
        <span class="review-count">{{ product.rating }} | {{ product.reviewCount }} đánh giá</span>
      </div>

      <div class="price-row d-flex align-items-end gap-2 mb-3 flex-wrap">
        <span class="sale-price">
          {{ formatCurrency(product.salePrice) }}
        </span>

        <span v-if="product.discountPercent > 0" class="original-price text-decoration-line-through">
          {{ formatCurrency(product.originalPrice) }}
        </span>
      </div>

      <button type="button" class="btn add-cart-btn w-100" @click.stop="handleAddToCart">
        <i class="bi bi-bag-plus me-2"></i>
        Thêm vào giỏ
      </button>
    </div>

    <Teleport to="body">
      <Transition name="toast-slide">
        <div v-if="showToast" class="custom-cart-toast">
          <div class="toast-icon">
            <i class="bi bi-check2"></i>
          </div>
          <div class="toast-info">
            <h4>Thêm thành công</h4>
            <p>Đã thêm 1 sản phẩm vào giỏ.</p>
          </div>
          <RouterLink to="/cart" class="toast-view-cart">
            XEM GIỎ HÀNG <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

interface Product {
  id: number;
  name: string;
  brand: string;
  color?: string;
  salePrice: number;
  originalPrice: number;
  discountPercent: number;
  rating: number;
  reviewCount: number;
  imageUrl?: string; 
}

const props = defineProps<{
  product: Product;
}>();

const router = useRouter();
const showToast = ref(false);

const brandMap: Record<string, string> = {
  Chanel: 'CHANEL',
  Dior: 'DIOR',
  'Yves Saint Laurent': 'YSL',
  'Giorgio Armani': 'ARMANI',
  Givenchy: 'GIVENCHY',
  Creed: 'CREED',
  Byredo: 'BYREDO',
  'Tom Ford': 'TOM FORD',
  'Maison Francis Kurkdjian': 'MFK',
  'Le Labo': 'LE LABO',
  'Paco Rabanne': 'PACO'
};

const shortBrand = computed(() => {
  return brandMap[props.product.brand] || props.product.brand.slice(0, 8).toUpperCase();
});

const getBottleStyle = (color?: string): Record<string, string> => {
  return {
    '--bottle-color': color || '#0a192f'
  };
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('vi-VN').format(value) + ' đ';
};

// CHUYỂN HƯỚNG TĨNH ĐẾN TRANG PRODUCT
// SỬA LẠI ĐOẠN NÀY ĐỂ NÓ TRUYỀN ĐÚNG ID SẢN PHẨM SANG TRANG CHI TIẾT
const goToDetail = () => {
  router.push({ name: 'ProductDetail', params: { id: props.product.id } });
};

// THÊM VÀO GIỎ HÀNG THỰC TẾ (GỌI API BACKEND) & HIỂN THỊ TOAST
// THÊM VÀO GIỎ HÀNG THỰC TẾ (GỌI API BACKEND) & HIỂN THỊ TOAST
const handleAddToCart = async () => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    alert('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
    return;
  }

  try {
    // Gọi API POST lên đúng đường dẫn của bạn
    await axios.post(
      'http://localhost:8080/api/v1/customer/cart/add', 
      {
        // ĐỔI THÀNH productVariantId cho khớp hoàn toàn với RequestBody của Java
        productVariantId: props.product.id, 
        quantity: 1
      },
      {
        headers: { Authorization: `Bearer ${token}` }
      }
    );

    // Nếu Backend trả về thành công, hiển thị Toast thông báo đẹp mắt
    showToast.value = true;
    setTimeout(() => {
      showToast.value = false;
    }, 3000);

  } catch (error) {
    console.error('Lỗi khi gọi API thêm giỏ hàng:', error);
    alert('Không thể thêm vào giỏ hàng! Hãy bật F12 kiểm tra tab Network xem Backend báo lỗi gì nhé.');
  }
};
</script>

<style scoped>
.product-card {
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(26, 26, 26, 0.055);
  box-shadow: 0 8px 28px rgba(5, 16, 36, 0.045);
  transition: all 0.28s ease;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 22px 48px rgba(5, 16, 36, 0.105);
}

.discount-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 3;
  background: #b31320;
  color: #ffffff;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
}

.product-image-wrapper {
  height: 235px;
  padding: 26px 24px 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at center, rgba(189, 154, 95, 0.1), transparent 48%),
    #ffffff;
}

.product-bottle {
  width: 120px;
  height: 190px;
  display: flex;
  flex-direction: column;
  align-items: center;
  filter: drop-shadow(0 18px 24px rgba(5, 16, 36, 0.18));
  transition: transform 0.28s ease;
}

.product-card:hover .product-bottle {
  transform: scale(1.05);
}

.product-bottle-cap {
  width: 58px;
  height: 34px;
  border-radius: 15px 15px 6px 6px;
  background: linear-gradient(135deg, #f1d08a, #9b6f2e);
}

.product-bottle-neck {
  width: 32px;
  height: 22px;
  background: linear-gradient(135deg, #d2ad68, #8b642c);
}

.product-bottle-body {
  width: 120px;
  height: 134px;
  border-radius: 16px 16px 24px 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.32), transparent 32%),
    linear-gradient(145deg, var(--bottle-color), #080808 86%);
  border: 2px solid rgba(255, 255, 255, 0.24);
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-bottle-label {
  width: 82px;
  height: 70px;
  border: 1px solid var(--aura-gold);
  background: rgba(5, 16, 36, 0.88);
  color: var(--aura-gold);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.product-bottle-label strong {
  font-family: var(--aura-serif);
  font-size: 14px;
  letter-spacing: 1.5px;
  line-height: 1;
}

.product-bottle-label span {
  margin-top: 5px;
  font-size: 7px;
  letter-spacing: 2px;
  font-family: var(--aura-sans);
}

.product-content {
  padding: 18px 20px 20px;
}

.product-brand {
  color: #8c8c8c;
  font-family: var(--aura-sans);
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  font-weight: 700;
}

.product-name {
  font-family: var(--aura-serif);
  font-size: 17px;
  font-weight: 700;
  color: var(--aura-black);
  line-height: 1.28;
  letter-spacing: 0;
}

.stars {
  color: var(--aura-gold);
  font-size: 13px;
  letter-spacing: 1px;
}

.review-count {
  color: #777777;
  font-size: 12px;
  font-weight: 500;
}

.sale-price {
  color: #111111;
  font-size: 18px;
  font-weight: 800;
  font-family: var(--aura-sans);
}

.original-price {
  color: #a8a8a8;
  font-size: 13px;
  font-weight: 500;
}

.add-cart-btn {
  border: 1px solid var(--aura-gold);
  color: var(--aura-gold);
  background: #ffffff;
  font-size: 13px;
  font-weight: 700;
  border-radius: 6px;
  padding: 10px 12px;
  transition: all 0.22s ease;
}

.add-cart-btn:hover {
  background: var(--aura-gold);
  color: #ffffff;
}
</style>

<style>
.custom-cart-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background-color: #0b1120;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 99999;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
}

.toast-icon {
  width: 34px;
  height: 34px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.toast-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
}

.toast-info p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.toast-view-cart {
  margin-left: 12px;
  color: #d2ad68;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
  display: flex;
  align-items: center;
  transition: color 0.25s ease;
}

.toast-view-cart:hover {
  color: #f1d08a;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  transform: translateX(120%);
  opacity: 0;
}
</style>