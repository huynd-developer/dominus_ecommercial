<template>
  <div class="product-review-wrapper py-2 view-transition text-white">
    <div class="card card-custom p-4 p-md-5 rounded-5 border-gold-opacity">
      <div class="row g-4">
        
        <div class="col-lg-4">
          <div class="review-left-banner p-4 p-md-5 rounded-4 h-100 d-flex flex-column justify-content-between text-white">
            <div>
              <span class="badge-tag text-uppercase mb-3 d-inline-block">SAU KHI MUA HÀNG</span>
              <h2 class="font-luxury display-5 text-gold mb-4 lh-sm">Để lại <br/>đánh giá</h2>
              <p class="text-white-50 text-xs lh-base fw-light" style="max-width: 260px;">
                Chia sẻ trải nghiệm về mùi hương, độ lưu hương, đóng gói và dịch vụ DOMINUS.
              </p>
            </div>
            <div class="font-luxury display-1 opacity-5 text-gold text-end user-select-none">D</div>
          </div>
        </div>

        <div class="col-lg-8 ps-lg-4">
          <div class="review-form-area">
            <span class="text-gold text-xs tracking-wider fw-semibold uppercase d-block mb-1">ĐƠN HÀNG #DMN-2048</span>
            <h3 class="font-luxury text-white mb-2 fs-2">Velvet Oud EDP</h3>
            <p class="text-white-50 text-xs mb-4 fw-light">Đã giao ngày 21/06/2026 - DOMINUS Signature</p>

            <div class="rating-selection-section mb-4">
              <label class="text-gold text-xs fw-semibold uppercase d-block mb-2">Bạn đánh giá bao nhiêu sao?</label>
              <div class="d-flex gap-2">
                <i 
                  v-for="star in 5" 
                  :key="star" 
                  @click="rating = star"
                  :class="['bi', star <= rating ? 'bi-star-fill text-gold' : 'bi-star text-white-50', 'fs-3 cursor-pointer star-transition']"
                ></i>
              </div>
            </div>

            <div class="tags-selection-section mb-4">
              <div class="d-flex flex-wrap gap-2">
                <button 
                  v-for="tag in availableTags" 
                  :key="tag"
                  @click="toggleTag(tag)"
                  :class="['btn btn-tag text-xs rounded-pill px-3 py-2', selectedTags.includes(tag) ? 'tag-active' : 'tag-inactive']"
                >
                  {{ tag }}
                </button>
              </div>
            </div>

            <div class="comment-input-section mb-4">
              <textarea 
                v-model="comment" 
                rows="5" 
                class="form-control comment-textarea" 
                placeholder="Viết cảm nhận của bạn sau khi sử dụng sản phẩm..."
              ></textarea>
            </div>

            <div class="d-flex flex-sm-row justify-content-between align-items-stretch align-items-sm-center gap-3">
              <button class="btn btn-upload text-xs rounded-pill px-4 py-2-5 d-flex align-items-center justify-content-center gap-2">
                <i class="bi bi-images text-gold"></i> Thêm ảnh/video
              </button>
              <button @click="submitReview" class="btn btn-gold text-xs rounded-pill px-5 py-2-5 fw-bold shadow">
                <i class="bi bi-send-fill me-1"></i> Gửi đánh giá
              </button>
            </div>

          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const rating = ref(5); // Mặc định chọn 5 sao như ảnh mẫu
const comment = ref('');
const selectedTags = ref(['Mùi sang']); // Mặc định chọn trước một tag theo ảnh

const availableTags = [
  'Lưu hương lâu',
  'Mùi sang',
  'Đóng gói đẹp',
  'Giao nhanh',
  'Sẽ mua lại'
];

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag);
  if (index > -1) {
    selectedTags.value.splice(index, 1);
  } else {
    selectedTags.value.push(tag);
  }
};

const submitReview = () => {
  const data = {
    rating: rating.value,
    tags: selectedTags.value,
    comment: comment.value
  };
  console.log('Dữ liệu đánh giá gửi đi:', data);
  alert('Cảm ơn bạn đã gửi đánh giá cho DOMINUS!');
};
</script>

<style scoped>
.card-custom {
  background-color: #131a2e !important;
}
.border-gold-opacity {
  border: 1px solid rgba(197, 147, 70, 0.15) !important;
}

/* Banner tối bên trái */
.review-left-banner {
  background: linear-gradient(180deg, #0b0f19 0%, #05070c 100%);
  border: 1px solid rgba(255, 255, 255, 0.03);
  min-height: 380px;
}
.badge-tag {
  border: 1px solid rgba(197, 147, 70, 0.4);
  padding: 6px 14px;
  border-radius: 50px;
  font-size: 0.65rem;
  letter-spacing: 1px;
  color: #94a3b8;
}

/* Hệ thống Tags */
.btn-tag {
  transition: all 0.2s ease;
  font-weight: 500;
}
.tag-inactive {
  background-color: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #e2e8f0;
}
.tag-inactive:hover {
  border-color: #c59346;
  color: #c59346;
}
.tag-active {
  background-color: #c59346 !important;
  color: #0b0f19 !important;
  border: 1px solid #c59346 !important;
}

/* Khung nhập cảm nhận Textarea */
.comment-textarea {
  background-color: #1e293b !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  color: #ffffff !important;
  border-radius: 12px !important;
  padding: 16px !important;
  font-size: 0.875rem;
  resize: none;
}
.comment-textarea:focus {
  border-color: #c59346 !important;
  box-shadow: 0 0 0 3px rgba(197, 147, 70, 0.15) !important;
  outline: none;
}

/* Nút bấm điều hướng và upload */
.btn-upload {
  background-color: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #ffffff;
  transition: all 0.2s ease;
}
.btn-upload:hover {
  border-color: #c59346;
  background-color: rgba(255, 255, 255, 0.02);
}

.btn-gold {
  background-color: #c59346;
  color: #0b0f19;
  border: none;
  transition: all 0.2s ease;
}
.btn-gold:hover {
  background-color: #dfaa53;
}

.text-gold { color: #c59346 !important; }
.cursor-pointer { cursor: pointer; }
.star-transition { transition: transform 0.15s ease, color 0.15s ease; }
.star-transition:hover { transform: scale(1.15); }
</style>