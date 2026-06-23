import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login', // Chỉ khai báo 1 lần duy nhất cho Khách hàng
    name: 'CustomerLogin',
    component: () => import('@/modules/auth/views/CustomerLoginView.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/admin/login', 
    name: 'EmployeeLogin',
    component: () => import('@/modules/auth/views/EmployeeLoginView.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/register', // Chỉ khai báo 1 lần duy nhất
    name: 'Register',
    component: () => import('@/modules/auth/views/RegisterView.vue'),
    meta: { requiresGuest: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// Middleware chặn quyền tối ưu - Bỏ `next()` để sửa cảnh báo màu vàng
router.beforeEach((to, from) => {
  const token = localStorage.getItem('accessToken');
  const currentUserRaw = localStorage.getItem('currentUser');
  let userRole = 'USER';
  
  if (currentUserRaw) {
    try {
      const userObj = JSON.parse(currentUserRaw);
      userRole = userObj.role || 'USER';
    } catch (e) {
      userRole = 'USER';
    }
  }

  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const requiresGuest = to.matched.some(record => record.meta.requiresGuest);

  if (requiresAuth) {
    if (!token) {
      alert('Vui lòng đăng nhập để truy cập chức năng này!');
      return '/login'; // Chuẩn mới: return string thay vì next()
    }

    const allowedRoles = to.meta.roles as string[];
    if (allowedRoles && !allowedRoles.includes(userRole)) {
      alert('Bạn không có quyền truy cập vào khu vực này!');
      return from.path && from.path !== '/login' ? from.path : '/';
    }
  }

  if (requiresGuest && token) {
    return userRole === 'USER' ? '/' : '/admin/dashboard';
  }

  return true; // Cho phép qua guard thay vì gọi next()
});

export default router;