<template>
  <div class="my-orders-wrapper py-2 view-transition text-white">
    <div class="card card-custom p-4 p-md-5 rounded-5 border-gold-opacity">
      
      <div class="d-flex flex-column flex-sm-row justify-content-between align-items-start align-items-sm-center gap-3 mb-5">
        <div>
          <span class="text-overline text-gold uppercase d-block mb-1 fw-semibold">TÀI KHOẢN DOMINUS</span>
          <h2 class="font-luxury display-6 text-gold mb-0">Đơn hàng của tôi</h2>
        </div>
        <button class="btn btn-gold text-xs rounded-pill px-4 py-2-5 fw-bold shadow-sm">
          Xem đơn vừa thanh toán
        </button>
      </div>

      <div class="d-flex flex-column gap-4">
        <div v-for="order in orders" :key="order.id" class="order-item-card p-4 rounded-4 d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-4">
          
          <div class="d-flex align-items-center gap-4">
            <div class="img-placeholder rounded-4 d-flex align-items-center justify-content-center flex-shrink-0">
              <div class="inner-oval"></div>
            </div>
            <div>
              <span class="text-gold text-xs tracking-wider fw-semibold d-block mb-1">#{{ order.code }}</span>
              <h4 class="font-luxury text-white mb-2 order-title">{{ order.productName }}</h4>
              <p class="text-white-50 text-xs mb-0 fw-light">{{ order.statusDescription }}</p>
            </div>
          </div>

          <div class="d-flex justify-content-md-end flex-shrink-0">
            <button 
              @click="handleAction(order)" 
              :class="['btn btn-action rounded-pill px-4 text-xs fw-semibold', order.canReview ? 'btn-outline-gold' : 'btn-outline-gray']"
            >
              {{ order.canReview ? 'Đánh giá' : 'Xem chi tiết' }}
            </button>
          </div>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Giả lập dữ liệu từ thiết kế của bạn
const orders = ref([
  {
    id: 1,
    code: 'DMN-2048',
    productName: 'Velvet Oud EDP',
    statusDescription: 'Thanh toán thành công - Chờ chuẩn bị hàng',
    canReview: false
  },
  {
    id: 2,
    code: 'DMN-1986',
    productName: 'Amber Solstice',
    statusDescription: 'Đã giao - Có thể đánh giá',
    canReview: true
  }
]);

const emit = defineEmits(['goToReview']);

const handleAction = (order) => {
  if (order.canReview) {
    emit('goToReview', order); // Chuyển sang tab đánh giá nếu nhấn nút Đánh giá
  } else {
    console.log('Xem chi tiết đơn hàng:', order.code);
  }
};
</script>

<style scoped>
.card-custom {
  background-color: #131a2e !important;
}
.border-gold-opacity {
  border: 1px solid rgba(197, 147, 70, 0.15) !important;
}

/* Khối bao bọc mỗi dòng đơn hàng */
.order-item-card {
  background-color: #0b0f19;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.25s ease;
}
.order-item-card:hover {
  border-color: rgba(197, 147, 70, 0.3);
  transform: translateY(-2px);
}

/* Khung ảnh tối giản hình bầu dục giống thiết kế */
.img-placeholder {
  width: 75px;
  height: 90px;
  background: linear-gradient(180deg, #1e293b 0%, #111827 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.inner-oval {
  width: 35px;
  height: 55px;
  border-radius: 50%;
  border: 2px solid rgba(197, 147, 70, 0.2);
  background: rgba(0, 0, 0, 0.2);
}

/* Typography */
.order-title {
  font-size: 1.5rem;
  font-weight: 400;
  letter-spacing: 0.5px;
}
.text-gold { color: #c59346 !important; }
.text-overline { font-size: 0.65rem; letter-spacing: 0.25em; }

/* Các loại nút bấm */
.btn-gold {
  background-color: #c59346;
  color: #0b0f19;
  border: none;
  transition: all 0.2s ease;
}
.btn-gold:hover {
  background-color: #dfaa53;
}

.btn-action {
  padding: 10px 24px;
  background: transparent;
  transition: all 0.2s ease;
}
.btn-outline-gold {
  color: #c59346;
  border: 1px solid rgba(197, 147, 70, 0.5);
}
.btn-outline-gold:hover {
  background-color: #c59346;
  color: #0b0f19;
}
.btn-outline-gray {
  color: #94a3b8;
  border: 1px solid rgba(255, 255, 255, 0.15);
}
.btn-outline-gray:hover {
  border-color: #ffffff;
  color: #ffffff;
}
</style>