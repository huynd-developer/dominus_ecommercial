import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { h } from "vue";

// Import các trang của m
import CartView from "@/modules/shop/feature/cart/views/CartView.vue";
import CheckoutView from "@/modules/shop/feature/checkout/views/CheckoutView.vue";
import PaymentReturnView from "@/modules/shop/feature/checkout/views/PaymentReturnView.vue";

// Admin layout & pages
import AdminLayout from "@/modules/admin/layout/AdminLayout.vue";
import ShopLayout from "@/modules/shop/layout/ShopLayout.vue";

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
  // STOREFRONT - USER
  // ==========================================
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
      {
        path: "customer/profile",
        name: "CustomerProfile",
        component: () =>
          import(
            "@/modules/shop/feature/profile/views/CustomerProfileView.vue"
          ),
        meta: {
          requiresAuth: true,
          allowedRoles: ["USER"],
        },
      },
    ],
  },

  // Code của m (Chi tiết SP, Giỏ hàng, Thanh toán)
  {
    path: "/products",
    name: "ProductList",
    component: () =>
      import("@/modules/shop/feature/product/views/ProductDetailView.vue"),
  },
  {
    path: "/product/:id",
    name: "SingleProduct",
    component: () =>
      import("@/modules/shop/feature/product/views/SingleProductView.vue"),
  },
  {
    path: "/product",
    redirect: "/products",
  },
  {
    path: "/cart",
    name: "Cart",
    component: CartView,
    meta: {
      requiresAuth: true,
      allowedRoles: ["USER"],
    },
  },
  {
    path: "/checkout",
    name: "Checkout",
    component: CheckoutView,
    meta: {
      requiresAuth: true,
      allowedRoles: ["USER"],
    },
  },
  {
    path: "/payment-return",
    name: "PaymentReturn",
    component: PaymentReturnView,
  },

  // ==========================================
  // POS PAYMENT RETURN
  // ==========================================
  {
    path: "/payment/result",
    name: "PosPaymentResult",
    component: () => import("@/modules/pos/views/PaymentResult.vue"),
    meta: {
      requiresAuth: true,
      allowedRoles: ["OWNER", "MANAGER", "CASHIER"],
    },
  },

  // ==========================================
  // AUTH
  // ==========================================
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

  // ==========================================
  // ADMIN
  // ==========================================
  {
    path: "/admin",
    component: AdminLayout,
    redirect: "/admin/dashboard",
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: "dashboard",
        name: "AdminDashboard",
        component: () =>
          import("@/modules/admin/feature/report/views/OwnerReportView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER"],
        },
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
      //       {
      //   path: "products",
      //   meta: {
      //     requiresAuth: true,
      //     allowedRoles: ["OWNER", "MANAGER"],
      //   },
      //   children: [
      //     {
      //       path: "",
      //       name: "AdminProducts",
      //       component: ProductListView,
      //     },
      //     {
      //       path: "create",
      //       name: "AdminProductCreate",
      //       component: ProductCreateView,
      //     },
      //     {
      //       path: ":id",
      //       name: "AdminProductUpdate",
      //       component: ProductUpdateView,
      //     },
      //   ],
      // },
      {
        path: "categories",
        name: "AdminCategories",
        component: () =>
          import("@/modules/admin/feature/category/views/CategoryView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "brands",
        name: "AdminBrands",
        component: () =>
          import("@/modules/admin/feature/brand/views/BrandView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "fragrance-families",
        name: "AdminFragranceFamilies",
        // Trỏ đúng đường dẫn tới file View của bạn
        component: () =>
          import(
            "@/modules/admin/feature/fragranceFamily/views/FragranceFamilyView.vue"
          ),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "capacities",
        name: "AdminCapacities",
        component: () =>
          import("@/modules/admin/feature/capacity/views/CapacityView.vue"),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "concentrations",
        name: "AdminConcentrations",
        component: () =>
          import(
            "@/modules/admin/feature/concentration/views/ConcentrationView.vue"
          ),
        meta: { requiresAuth: true, allowedRoles: ["OWNER", "MANAGER"] },
      },
      {
        path: "bottle-types",
        name: "AdminBottleTypes",
        // Import trực tiếp đến file Vue giao diện mà bạn vừa tạo
        component: () =>
          import("@/modules/admin/feature/bottleType/views/BottleTypeView.vue"),
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
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "flash-sale",
        name: "AdminFlashSale",
        component: mockPage("Quản lý Flash Sale", "Huy"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "customers",
        name: "AdminCustomers",
        component: () =>
          import("@/modules/admin/feature/customer/views/CustomerView.vue"),
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
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER"],
        },
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
      return {
        path: "/admin/dashboard",
        replace: true,
      };
    }

    if (["MANAGER", "CASHIER"].includes(userRole)) {
      return {
        path: "/admin/pos",
        replace: true,
      };
    }

    return {
      path: "/",
      replace: true,
    };
  }

  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      if (to.path.startsWith("/admin")) {
        return {
          name: "AdminLogin",
          replace: true,
        };
      }

      return {
        name: "Login",
        replace: true,
      };
    }

    const allowedRoles = to.meta.allowedRoles as string[] | undefined;

    if (allowedRoles && !allowedRoles.includes(userRole)) {
      if (["CASHIER", "MANAGER"].includes(userRole)) {
        return {
          path: "/admin/pos",
          replace: true,
        };
      }

      if (userRole === "OWNER") {
        return {
          path: "/admin/dashboard",
          replace: true,
        };
      }

      if (userRole === "USER") {
        return {
          path: "/",
          replace: true,
        };
      }

      return {
        name: "Login",
        replace: true,
      };
    }
  }

  return true;
});

export default router;
