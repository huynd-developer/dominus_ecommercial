import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { h } from "vue";
import { useAuthStore } from "@/modules/auth/stores/authStore";

import ShopLayout from "@/modules/shop/layout/ShopLayout.vue";
import AdminLayout from "@/modules/admin/layout/AdminLayout.vue";

import CartView from "@/modules/shop/feature/cart/views/CartView.vue";
import CheckoutView from "@/modules/shop/feature/checkout/views/CheckoutView.vue";
import PaymentReturnView from "@/modules/shop/feature/checkout/views/PaymentReturnView.vue";

import ProductView from "@/modules/admin/feature/product/views/ProductView.vue";

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
          import("@/modules/shop/feature/profile/views/CustomerProfileView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["USER"],
        },
      },
    ],
  },

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

  {
    path: "/payment/result",
    name: "PosPaymentResult",
    component: () => import("@/modules/pos/views/PaymentResult.vue"),
    meta: {
      requiresAuth: true,
      allowedRoles: ["OWNER", "MANAGER", "CASHIER"],
    },
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
      {
        path: "products",
        name: "AdminProducts",
        component: ProductView,
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
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
        component: () =>
          import(
            "@/modules/admin/feature/fragranceFamily/views/FragranceFamilyView.vue"
          ),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "capacities",
        name: "AdminCapacities",
        component: () =>
          import("@/modules/admin/feature/capacity/views/CapacityView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "concentrations",
        name: "AdminConcentrations",
        component: () =>
          import(
            "@/modules/admin/feature/concentration/views/ConcentrationView.vue"
          ),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "bottle-types",
        name: "AdminBottleTypes",
        component: () =>
          import("@/modules/admin/feature/bottleType/views/BottleTypeView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
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
        component: () =>
          import("@/modules/admin/feature/voucher/views/VoucherListView.vue"),
        meta: {
          requiresAuth: true,
          allowedRoles: ["OWNER", "MANAGER"],
        },
      },
      {
        path: "flash-sale",
        name: "AdminFlashSale",
        component: () =>
          import("@/modules/admin/feature/promotion/views/PromotionView.vue"),
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

  {
    path: "/:pathMatch(.*)*",
    redirect: "/",
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  const token = localStorage.getItem("token");
  const localRole = localStorage.getItem("role");

  const isAuthenticated = authStore.isAuthenticated || Boolean(token);

  const userRole = String(authStore.role || localRole || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim();

  if (
    isAuthenticated &&
    ["Login", "AdminLogin", "Register"].includes(String(to.name || ""))
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
        query: {
          redirect: to.fullPath,
        },
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