import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { h } from "vue"; 
import { usePosStore } from "@/modules/pos/stores/posStore";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/modules/auth/views/CustomerLoginView.vue"),
  },
  {
    path: "/admin/login",
    name: "AdminLogin",
    component: () => import("@/modules/auth/views/EmployeeLoginView.vue"),
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/modules/auth/views/RegisterView.vue"),
  },
  {
    path: "/admin/dashboard",
    name: "AdminDashboard",
    // Trang trắng tạm thời bằng hàm h của Vue
    component: {
      render: () => h('div', { style: 'padding: 100px; text-align: center; font-family: sans-serif; color: #333;' }, [
        h('h1', '🎉 Đăng nhập thành công!'),
        h('p', 'Đây là trang trắng tạm thời của ông giáo.')
      ])
    },
    meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER", "CASHIER"] },
  },
  {
    path: "/customer/profile", 
    name: "CustomerProfile",
    component: () => import("@/modules/auth/views/CustomerLoginView.vue"), 
    meta: { requiresAuth: true, allowedRoles: ["USER"] },
  },
  {
  path: "/admin/pos",
  name: "DominusPOS",
  component: () => import("@/modules/pos/views/posView.vue"), // Đã thay thế hàm h() bằng component POS thực tế
  meta: { 
    requiresAuth: true, 
    allowedRoles: ["MANAGER", "CASHIER"] // Chỉ cho phép Manager và Cashier truy cập
  }
}
  // 🎯 ĐÃ XÓA BỎ ROUTE /admin/dashboard BỊ TRÙNG LẶP Ở ĐÂY NHA ÔNG!
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  
  // Chuẩn hóa quyền thành chữ HOA để so sánh tuyệt đối an toàn
  const userRole = (authStore.role || "").toUpperCase();

  // LUỒNG 1: Đã đăng nhập thì cấm quay lại trang login/register
  if (
    authStore.isAuthenticated &&
    ["Login", "AdminLogin", "Register"].includes(to.name as string)
  ) {
    const adminRoles = ["OWNER", "MANAGER", "CASHIER"];
    if (adminRoles.includes(userRole)) {
      return { path: "/admin/dashboard" };
    }
    return { path: "/" };
  }

  // LUỒNG 2: Chặn quyền truy cập các trang bảo mật
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      if (to.path.startsWith("/admin")) {
        return { name: "AdminLogin" };
      }
      return { name: "Login" };
    }

    const allowedRoles = to.meta.allowedRoles as string[];
    
    // 👇 CHỖ NÀY: Dùng userRole đã ép chữ HOA để so sánh, bao mượt, bao khớp! 👇
    if (allowedRoles && !allowedRoles.includes(userRole)) {
      alert(
        "Cảnh báo bảo mật: Tài khoản của bạn không có quyền truy cập vào hệ thống Quản trị!"
      );
      return { path: "/admin/login" }; 
    }
  }

  return true;
});

export default router;