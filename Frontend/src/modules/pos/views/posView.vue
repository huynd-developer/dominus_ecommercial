<template>
  <!-- Thêm d-flex flex-column bằng class Bootstrap để đảm bảo không lỗi CSS -->
  <div class="pos-layout-wrapper w-100 d-flex flex-column p-3 overflow-hidden">
    <!-- Header của POS -->
    <posHeader />

    <!-- Khu vực chia cột Landscape: Ép giãn hết cỡ xuống đáy -->
    <div class="row flex-grow-1 overflow-hidden m-0 pt-2 gy-0 gx-3 alignment-stretch">
      
      <!-- SIDE CENTER: Lưới sản phẩm -->
      <div class="col-lg-7 col-xl-8 col-xxl-8-5 h-100 d-flex flex-column ps-0">
        <productGrid />
      </div>
      
      <!-- SIDE RIGHT: Giỏ hàng tính tiền -->
      <div class="col-lg-5 col-xl-4 col-xxl-3-5 h-100 pe-0">
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

// Bộ nhớ tạm để khôi phục lại trạng thái cho các trang quản lý khác
const originalStyles = new Map()

onMounted(() => {
  const wrapper = document.querySelector('.pos-layout-wrapper')
  if (wrapper) {
    let parent = wrapper.parentElement
    
    // Thuật toán lội ngược dòng DOM: Ép toàn bộ các thẻ cha bên phải Sidebar xả sạch padding và kéo giãn 100vh
    while (parent && parent.tagName !== 'BODY') {
      originalStyles.set(parent, {
        backgroundColor: parent.style.backgroundColor,
        padding: parent.style.padding,
        margin: parent.style.margin,
        height: parent.style.height,
        minHeight: parent.style.minHeight,
        overflow: parent.style.overflow,
        display: parent.style.display,
        flexDirection: parent.style.flexDirection
      })

      // Cấu hình ép buộc bẻ gãy layout trắng
      parent.style.backgroundColor = '#090d1a'
      parent.style.padding = '0'
      parent.style.margin = '0'
      parent.style.height = '100vh'
      parent.style.minHeight = '100vh'
      parent.style.display = 'flex'
      parent.style.flexDirection = 'column'
      parent.style.overflow = 'hidden'
      
      parent = parent.parentElement
    }
  }
  // Thêm class môi trường tối toàn cục cho body
  document.body.classList.add('pos-global-dark-env')
})

onUnmounted(() => {
  // Trả lại nguyên trạng giao diện trắng cho Admin khi thoát trang POS
  document.body.classList.remove('pos-global-dark-env')
  originalStyles.forEach((style, element) => {
    if (element) {
      element.style.backgroundColor = style.backgroundColor
      element.style.padding = style.padding
      element.style.margin = style.margin
      element.style.height = style.height
      element.style.minHeight = style.minHeight
      element.style.overflow = style.overflow
      element.style.display = style.display
      element.style.flexDirection = style.flexDirection
    }
  })
})
</script>

<style scoped>
.pos-layout-wrapper {
  background-color: #090d1a !important;
  color: #f8fafc;
  height: 100vh !important;      /* Bắt buộc chiếm trọn chiều cao màn hình hiển thị */
  min-height: 100vh !important;
  width: 100% !important;
  display: flex !important;
  flex-direction: column !important; /* ĐÃ FIX TYPO: Sửa từ flex-column thành flex-direction */
  box-sizing: border-box;
}

.alignment-stretch {
  align-items: stretch !important;
}

@media (min-width: 1500px) {
  .col-xxl-8-5 { width: 73%; }
  .col-xxl-3-5 { width: 27%; }
}
</style>

<!-- KHU VỰC ĐÈ STYLE ĐỂ ĐẨY SÁT VÀO SIDEBAR TRẮNG BÊN TRÁI -->
<style>
.pos-global-dark-env {
  background-color: #090d1a !important;
}

/* Triệt hạ tận gốc các khoảng cách mặc định của ElMain hoặc khung bao AdminLayout */
.pos-global-dark-env .el-main,
.pos-global-dark-env .el-container,
.pos-global-dark-env .main-content,
.pos-global-dark-env .content-wrapper,
.pos-global-dark-env .wrapper,
.pos-global-dark-env #admin-content {
  padding: 0 !important;
  margin: 0 !important;
  background-color: #090d1a !important;
  height: 100vh !important;
  min-height: 100vh !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
}
</style>