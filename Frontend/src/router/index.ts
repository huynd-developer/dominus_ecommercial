import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { h } from "vue";

// Import các trang của m
import ProductDetailView from "@/modules/shop/feature/product/views/ProductDetailView.vue";
import CartView from "@/modules/shop/feature/cart/views/CartView.vue";
import CheckoutView from "@/modules/shop/feature/checkout/views/CheckoutView.vue";
import PaymentReturnView from "@/modules/shop/feature/checkout/views/PaymentReturnView.vue";

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
  {
    path: "/payment/result",
    name: "PosPaymentResult",
    component: () => import("@/modules/pos/views/PaymentResult.vue"),
    meta: {
      requiresAuth: true,
      allowedRoles: ["OWNER", "MANAGER", "CASHIER"],
    },
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
        component: () =>
          import("@/modules/admin/feature/category/views/CategoryView.vue"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "brands",
        name: "AdminBrands",
        component: () =>
          import("@/modules/admin/feature/brand/views/BrandView.vue"),
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
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER", "CASHIER"],
        },
      },
      {
        path: "employees",
        name: "AdminEmployees",
        component: () =>
          import("@/modules/admin/feature/employee/views/EmployeeView.vue"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER"] },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from) => {
  const authStore = useAuthStore();

  const token = localStorage.getItem("token");
  const localRole = localStorage.getItem("role");

  const isAuthenticated = authStore.isAuthenticated || !!token;

  const userRole = (authStore.role || localRole || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim();

  if (
    isAuthenticated &&
    ["Login", "AdminLogin", "Register"].includes(to.name as string)
  ) {
    if (userRole === "OWNER") {
      return { path: "/admin/dashboard", replace: true };
    }

    if (["MANAGER", "CASHIER"].includes(userRole)) {
      return { path: "/admin/pos", replace: true };
    }

    return { path: "/", replace: true };
  }

  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      if (to.path.startsWith("/admin")) {
        return { name: "AdminLogin", replace: true };
      }

      return { name: "Login", replace: true };
    }

    const allowedRoles = to.meta.allowedRoles as string[] | undefined;

    if (allowedRoles && !allowedRoles.includes(userRole)) {
      if (from.name === "AdminLogin" || from.name === "Login") {
        if (["CASHIER", "MANAGER"].includes(userRole)) {
          return { path: "/admin/pos", replace: true };
        }

        if (userRole === "OWNER") {
          return { path: "/admin/dashboard", replace: true };
        }

        return { path: "/", replace: true };
      }

      if (from.matched.length > 0) {
        return from.fullPath;
      }

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
