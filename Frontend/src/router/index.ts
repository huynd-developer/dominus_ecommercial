import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { h } from "vue";

// Layout quản trị của Admin
import AdminLayout from "@/modules/admin/layout/AdminLayout.vue";
import ShopLayout from "@/modules/shop/layout/ShopLayout.vue";
import ProductListView from "@/modules/admin/feature/product/views/ProductListView.vue";
import ProductCreateView from "@/modules/admin/feature/product/views/ProductCreateView.vue";
import ProductUpdateView from "@/modules/admin/feature/product/views/ProductUpdateView.vue";

// Hàm tạo trang tạm thời phục vụ giai đoạn phát triển
const mockPage = (title: string, assignee: string) => ({
  render: () =>
    h(
      "div",
      {
        style:
          "padding: 80px; text-align: center; font-family: sans-serif; color: #333;",
      },
      [
        h("h1", `🚧 Trang ${title}`),
        h(
          "p",
          `Giao diện đang được xây dựng bởi: ${assignee}. Sau khi code xong file Vue, hãy mở comment import trong router ra!`
        ),
      ]
    ),
});

const routes: Array<RouteRecordRaw> = [
  // ==========================================
  // LUỒNG AUTH & STOREFRONT (GIAO DIỆN KHÁCH)
  // ==========================================
  {
    path: "/",
    component: () => import("@/modules/shop/layout/ShopLayout.vue"),
    children: [
      {
        path: "",
        name: "shop-home",
        component: () =>
          import("@/modules/shop/feature/home/views/HomeView.vue"),
      },
    ],
  },
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
    path: "/customer/profile",
    name: "CustomerProfile",
    component: () => import("@/modules/auth/views/CustomerLoginView.vue"),
    meta: { requiresAuth: true, allowedRoles: ["USER"] },
  },

  // ==========================================
  // LUỒNG ADMIN QUẢN TRỊ (Khớp chính xác với TopHeader)
  // ==========================================
  {
    path: "/admin",
    component: AdminLayout,
    redirect: "/admin/dashboard", 
    meta: { requiresAuth: true },
    children: [
      {
        path: "dashboard",
        name: "AdminDashboard",
        component: mockPage("Tổng quan (Báo cáo)", "Huy"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER"] },
      },
      {
        path: "pos",
        name: "AdminPOS",
        component: mockPage("Bán hàng POS", "Đan & Trung"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER", "CASHIER"],
        },
      },
      {
  path: "products",
  meta: {
    requiresAuth: true,
    allowedRoles: ["OWNER", "MANAGER"],
  },
  children: [
    {
      path: "",
      name: "AdminProducts",
      component: ProductListView,
    },
    {
      path: "create",
      name: "AdminProductCreate",
      component: ProductCreateView,
    },
    {
      path: ":id",
      name: "AdminProductUpdate",
      component: ProductUpdateView,
    },
  ],
},
      {
        path: "categories",
        name: "AdminCategories",
        component: mockPage("Danh mục (Category)", "Đức"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "brands",
        name: "AdminBrands",
        component: mockPage("Thương hiệu (Brand)", "Đức"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "fragrance-families",
        name: "AdminFragranceFamilies",
        component: mockPage("Nhóm hương", "Đức"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "capacities",
        name: "AdminCapacities",
        component: mockPage("Dung tích (Capacity)", "Đức"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "concentrations",
        name: "AdminConcentrations",
        component: mockPage("Nồng độ (Concentration)", "Đức"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "bottle-types",
        name: "AdminBottleTypes",
        component: mockPage("Loại vỏ chai", "Đức"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "orders",
        name: "AdminOrders",
        component: mockPage("Quản lý Đơn hàng", "Trung"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER", "CASHIER"],
        },
      },
      {
        path: "vouchers",
        name: "AdminVouchers",
        component: mockPage("Hệ thống Voucher", "Hiếu"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "flash-sale",
        name: "AdminFlashSale",
        component: mockPage("Quản lý Flash Sale", "Huy"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "customers",
        name: "AdminCustomers",
        component: mockPage("Quản lý Khách hàng", "Huy"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "employees",
        name: "AdminEmployees",
        component: mockPage("Quản lý Nhân viên", "Huy"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER"] },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Logic Bảo Mật Định Tuyến Toàn Cục
// CHỖ THAY ĐỔI: Bỏ tham số `next` ở callback của beforeEach
router.beforeEach(async (to, from) => {
  const authStore = useAuthStore();

  // ĐẢM BẢO STATE ĐƯỢC ĐỒNG BỘ: Đọc trực tiếp từ LocalStorage nếu Pinia chưa kịp load lại
  if (!authStore.isAuthenticated && localStorage.getItem('token')) {
    // Giữ nguyên phần nhắc nhở logic đồng bộ của bạn
  }

  const userRole = (authStore.role || "").toUpperCase().replace("ROLE_", "");

  // ==========================================
  // LUỒNG 1: ĐÃ ĐĂNG NHẬP THÌ KHÔNG CHO RA LOGIN
  // ==========================================
  if (
    authStore.isAuthenticated &&
    ["Login", "AdminLogin", "Register"].includes(to.name as string)
  ) {
    if (userRole === "OWNER") {
      return { path: "/admin/dashboard", replace: true }; // Thay thế: next({...}) -> return {...}
    } else if (["MANAGER", "CASHIER"].includes(userRole)) {
      return { path: "/admin/pos", replace: true };
    }
    return { path: "/", replace: true };
  }

  // ==========================================
  // LUỒNG 2: KIỂM TRA QUYỀN TRUY CẬP NỘI BỘ
  // ==========================================
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      if (to.path.startsWith("/admin")) {
        return { name: "AdminLogin", replace: true };
      }
      return { name: "Login", replace: true };
    }

    const allowedRoles = to.meta.allowedRoles as string[];

    if (allowedRoles && !allowedRoles.includes(userRole)) {
      // 1. Nếu vừa đăng nhập xong bị đá sang link cấm không đúng quyền hạn
      if (from.name === "AdminLogin" || from.name === "Login") {
        if (["CASHIER", "MANAGER"].includes(userRole)) {
          return { path: "/admin/pos", replace: true };
        }
        if (userRole === "OWNER") {
          return { path: "/admin/dashboard", replace: true };
        }
        return { path: "/", replace: true };
      }

      // 2. Nếu đang thao tác nội bộ mà bấm nhầm link cấm
      if (from.matched.length > 0) {
        return from.fullPath; // Trả về chính vị trí hiện tại để hủy điều hướng
      }

      // 3. Nếu gõ link trực tiếp lên thanh URL nhưng sai Role
      if (["CASHIER", "MANAGER"].includes(userRole)) {
        return { path: "/admin/pos", replace: true };
      }
      if (userRole === "OWNER") {
        return { path: "/admin/dashboard", replace: true };
      }

      return { path: "/", replace: true };
    }
  }

  // Hoàn toàn hợp lệ -> cho phép đi tiếp (không cần gọi next() nữa)
  return true; 
});

export default router;