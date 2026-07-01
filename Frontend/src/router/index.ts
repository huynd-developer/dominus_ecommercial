import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { h } from "vue";

// Import các trang của m
import ProductDetailView from "@/modules/shop/feature/product/ProductDetailView.vue";
import CartView from "@/modules/shop/feature/cart/CartViews.vue";
import CheckoutView from "@/modules/shop/feature/checkout/CheckoutViews.vue";
import PaymentReturnView from "@/modules/shop/feature/checkout/PaymentReturnView.vue";

// Layout quản trị của Admin & Shop
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

  // Code của Team (Trang chủ)
  {
    path: "/",
    component: ShopLayout,
    children: [
      {
        path: "",
        name: "shop-home",
        component: () =>
          import("@/modules/shop/feature/home/views/HomeView.vue"),
      },
    ],
  },

  // Code của m (Chi tiết SP, Giỏ hàng, Thanh toán)
  {
    path: "/product",
    name: "ProductDetail",
    component: ProductDetailView,
  },
  {
    path: "/cart",
    name: "Cart",
    component: CartView,
    meta: { requiresAuth: true, allowedRoles: ["USER"] },
  },
  {
    path: "/checkout",
    name: "Checkout",
    component: CheckoutView,
    meta: { requiresAuth: true, allowedRoles: ["USER"] },
  },
  {
    path: "/payment-return",
    name: "PaymentReturn",
    component: PaymentReturnView,
  },

  // Các trang Auth
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
  // LUỒNG ADMIN QUẢN TRỊ
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
        component: () => import("@/modules/pos/views/PosView.vue"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER", "CASHIER"] },
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
        component: () => import("@/modules/admin/feature/category/views/CategoryView.vue"),
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

// Logic Bảo Mật Định Tuyến Toàn Cục (Đã gộp code của m và team)
router.beforeEach(async (to, from) => {
  const authStore = useAuthStore();

  // ĐẢM BẢO STATE ĐƯỢC ĐỒNG BỘ: Đọc trực tiếp từ LocalStorage nếu Pinia chưa kịp load lại
  if (!authStore.isAuthenticated && localStorage.getItem("token")) {
    // Giữ nguyên phần nhắc nhở logic đồng bộ của team
  }

  const userRole = (authStore.role || "").toUpperCase().trim();
  // ==========================================
  // LUỒNG 1: ĐÃ ĐĂNG NHẬP THÌ KHÔNG CHO RA LOGIN
  // ==========================================
  if (
    authStore.isAuthenticated &&
    ["Login", "AdminLogin", "Register"].includes(to.name as string)
  ) {
    if (userRole === "OWNER") {
      return { path: "/admin/dashboard", replace: true };
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
        return from.fullPath;
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

  return true;
});

export default router;
