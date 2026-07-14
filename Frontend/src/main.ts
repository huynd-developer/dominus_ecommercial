import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router' // 1. ĐÃ BỎ DẤU // Ở ĐÂY
import { permission } from './common/directives/permission.ts'
// Import Bootstrap
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'
// 1. Khởi tạo instance của ứng dụng Vue và Pinia trước
const app = createApp(App)
const pinia = createPinia()

// 2. Kích hoạt các thư viện (Plugin) vào ứng dụng
app.use(pinia) 
app.use(router) // 2. ĐÃ BỎ DẤU // Ở ĐÂY

// 3. Cuối cùng mới gắn ứng dụng vào thẻ #app ngoài giao diện HTML
app.mount('#app')
app.directive(
  'permission',
  permission
)