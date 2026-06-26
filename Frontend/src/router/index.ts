import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { h } from "vue";

// Layout quản trị của Admin
import AdminLayout from "@/modules/admin/layout/AdminLayout.vue";

import ShopLayout from "@/modules/shop/layout/ShopLayout.vue";
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
        name: "AdminProducts",
        component: mockPage("Quản lý Sản phẩm", "Trung"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      // 👇 Phân hệ Thuộc tính sản phẩm của Đức (Đã đồng bộ chi tiết) 👇
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
      // ----------------------------------------------------------------
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
// Logic Bảo Mật Định Tuyến Toàn Cục
// CHỖ ĐÃ SỬA: Nhận thêm tham số `from` để lấy thông tin trang trước đó của user
router.beforeEach((to, from) => {
  const authStore = useAuthStore();

  // Chuẩn hóa quyền: Chuyển sang chữ HOA và bóc tách tiền tố ROLE_
  const userRole = (authStore.role || "").toUpperCase().replace("ROLE_", "");

  // LUỒNG 1: Ngăn chặn tài khoản đã đăng nhập quay lại các trang đăng ký/đăng nhập
  if (
    authStore.isAuthenticated &&
    ["Login", "AdminLogin", "Register"].includes(to.name as string)
  ) {
    if (userRole === "OWNER") {
      return { path: "/admin/dashboard" };
    } else if (["MANAGER", "CASHIER"].includes(userRole)) {
      return { path: "/admin/pos" };
    }
    return { path: "/" };
  }

  // LUỒNG 2: Kiểm soát quyền truy cập chi tiết từng trang nội bộ
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      if (to.path.startsWith("/admin")) {
        return { name: "AdminLogin" };
      }
      return { name: "Login" };
    }

    const allowedRoles = to.meta.allowedRoles as string[];

    // CHỖ ĐÃ SỬA: Xử lý khi user cố tình gõ link không có quyền hoặc bị push nhầm sau khi Login
    if (allowedRoles && !allowedRoles.includes(userRole)) {
      // 1. Bẻ lái nếu đến từ trang Đăng nhập (tức là vừa login xong nhưng bị đẩy nhầm vào trang cấm như Dashboard)
      if (from.name === "AdminLogin" || from.name === "Login") {
        if (["CASHIER", "MANAGER"].includes(userRole)) {
          return { path: "/admin/pos" };
        }
        if (userRole === "OWNER") {
          return { path: "/admin/dashboard" };
        }
        return { path: "/" };
      }

      // 2. Nếu đang lướt trong app mà bấm nhầm link cấm -> Giữ nguyên ở trang hiện tại
      if (from.matched.length > 0) {
        // MẸO: Bạn nên gọi 1 hàm Toast Notification (thông báo góc màn hình) ở đây.
        // VD: toast.error("Bạn không có quyền truy cập trang này!");
        // Để user biết tại sao bấm nút mà không chuyển trang, tránh việc họ tưởng web lỗi.
        return from.fullPath;
      }

      // 3. Nếu gõ link trực tiếp từ thanh địa chỉ (chưa có lịch sử)
      if (["CASHIER", "MANAGER"].includes(userRole)) {
        return { path: "/admin/pos" };
      }
      if (userRole === "OWNER") {
        return { path: "/admin/dashboard" };
      }

      // Default fallback
      return { path: "/" };
    }
  }

  return true;
});

export default router;
