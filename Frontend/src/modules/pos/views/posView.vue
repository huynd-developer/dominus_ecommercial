<template>
  <div class="pos-layout-wrapper w-100 h-100 d-flex flex-column p-3 overflow-hidden">
    <posHeader />

    <div class="row flex-grow-1 flex-nowrap overflow-hidden m-0 pt-2 gy-0 gx-3 alignment-stretch">
      
      <div class="col-left h-100 d-flex flex-column ps-0 overflow-hidden">
        <productGrid />
      </div>
      
      <div class="col-5 col-xl-4 col-xxl-3-5 h-100 pe-0 overflow-hidden">
        <cartSideBar />
      </div>
      
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import posHeader from '../components/posHeader.vue'
import productGrid from '../components/productGrid.vue'
import cartSideBar from '../components/cartSideBar.vue'

// Bộ nhớ tạm để khôi phục lại trạng thái ban đầu cho Admin Layout khi thoát POS
const originalStyles = new Map()

onMounted(() => {
  // Kích hoạt class môi trường tối toàn cục
  document.body.classList.add('pos-global-dark-env')

  const wrapper = document.querySelector('.pos-layout-wrapper')
  if (wrapper) {
    let parent = wrapper.parentElement
    
    // Lội ngược dòng các thẻ cha bọc ĐỘC LẬP bên phải (Không động chạm tới Sidebar trắng)
    while (parent && parent.tagName !== 'BODY') {
      // Nếu va phải các thẻ định danh của Menu/Sidebar trắng thì bỏ qua không can thiệp
      if (parent.classList.contains('sidebar') || parent.classList.contains('el-aside') || parent.tagName === 'ASIDE') {
        parent = parent.parentElement
        continue
      }

      originalStyles.set(parent, {
        backgroundColor: parent.style.backgroundColor,
        height: parent.style.height,
        overflow: parent.style.overflow
      })

      // CHỈ đổi màu nền sang đen tối thượng và ép chiều cao, TUYỆT ĐỐI KHÔNG chạm vào margin/padding layout ở đây
      parent.style.setProperty('background-color', '#090d1a', 'important')
      parent.style.setProperty('height', '100vh', 'important')
      parent.style.setProperty('overflow', 'hidden', 'important')
      
      parent = parent.parentElement
    }
  }
})

onUnmounted(() => {
  // Trả lại nguyên trạng hoàn hảo cho trang quản trị Admin khi rời trang POS
  document.body.classList.remove('pos-global-dark-env')
  originalStyles.forEach((style, element) => {
    if (element) {
      Object.keys(style).forEach((key) => {
        element.style[key] = style[key]
      })
    }
  })
})
</script>

<style scoped>
.pos-layout-wrapper {
  background-color: #090d1a !important;
  color: #f8fafc;
  height: 100% !important;      /* Điền đầy 100% chiều cao của khung chứa Admin bên phải */
  width: 100% !important;       /* Điền đầy 100% chiều rộng của khung chứa Admin bên phải */
  display: flex !important;
  flex-direction: column !important; 
  box-sizing: border-box;
}

.alignment-stretch {
  align-items: stretch !important;
}

/* KHỐI TRÁI SẢN PHẨM: Tự động lấp đầy phần không gian còn lại một cách linh hoạt */
.col-left {
  flex: 1 !important;
  min-width: 0 !important;
}

/* KHỐI PHẢI GIỎ HÀNG: Định hình chiều rộng cố định theo từng độ phân giải */
@media (min-width: 1200px) and (max-width: 1499px) {
  .col-5 { width: 35% !important; flex: 0 0 35% !important; }
}

@media (min-width: 1500px) {
  .col-xxl-3-5 { width: 26% !important; flex: 0 0 26% !important; }
}
</style>

<style>
.pos-global-dark-env {
  background-color: #090d1a !important;
}

/* Đối với Element Plus Layout chuẩn (<el-main>) */
.pos-global-dark-env .el-main {
  padding: 0 !important;
  background-color: #090d1a !important;
  height: 100vh !important;
  overflow: hidden !important;
}

/* Đối với các dạng Admin Layout tùy biến khác (.main-content, .content-wrapper) */
.pos-global-dark-env .main-content,
.pos-global-dark-env .content-wrapper,
.pos-global-dark-env .wrapper,
.pos-global-dark-env #admin-content {
  /* TUYỆT ĐỐI KHÔNG dùng margin: 0 !important để giữ lại khoảng trống né Sidebar của hệ thống gốc */
  background-color: #090d1a !important;
  height: 100vh !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
  
  /* Xả sạch padding nội khu để POS bám sát biên, nhưng bảo vệ padding-left nếu hệ thống dùng nó làm lề */
  padding-top: 0 !important;
  padding-right: 0 !important;
  padding-bottom: 0 !important;
}
</style>