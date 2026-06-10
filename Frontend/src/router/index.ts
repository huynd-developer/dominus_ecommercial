import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // {
    //   path: '/',
    //   name: 'home',
    //   // Đảm bảo bạn đã có file HomeView.vue trong thư mục src/views/
    //   // Hoặc bạn có thể tạm thời comment dòng component bên dưới lại nếu chưa có file
    //   component: () => import('../views/HomeView.vue') 
    // }
  ]
})

export default router