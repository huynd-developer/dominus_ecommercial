<template>
  <header class="top-nav-glass sticky-top shadow-sm" style="z-index: 1050">
    <div
      class="container-xl d-flex align-items-center justify-content-between h-100 px-3"
    >
      <div class="d-flex align-items-center gap-2 flex-shrink-0">
        <div
          class="bg-dark-peel text-white rounded-3 d-flex align-items-center justify-content-center shadow-peel logo-box"
        >
          <i class="bi bi-layers-fill fs-6"></i>
        </div>
        <h5
          class="fw-black mb-0 text-slate-800 tracking-tight d-none d-md-block"
          style="letter-spacing: 0.5px"
        >
          LATRA
        </h5>
      </div>

      <div
        class="menu-scroll-container d-flex align-items-center gap-1 bg-slate-100 p-1 rounded-pill border border-slate-200 shadow-sm mx-3"
      >
        <template v-for="item in filteredMenuItems" :key="item.id">
          <router-link
            v-if="!item.children"
            :to="item.path"
            class="btn nav-pill transition-all fw-semibold text-decoration-none text-nowrap"
            :class="
              isActive(item.path)
                ? 'active bg-dark-peel text-white shadow-peel'
                : 'text-slate-500 hover-text-dark'
            "
          >
            <i :class="item.icon" class="me-1"></i> {{ item.name }}
          </router-link>

          <div
            v-else
            class="dropdown-hover-container position-relative"
            @mouseenter="activeHoverMenu = item.id"
            @mouseleave="activeHoverMenu = null"
          >
            <div
              class="btn nav-pill transition-all fw-semibold text-decoration-none text-nowrap d-flex align-items-center gap-1 cursor-pointer"
              :class="
                isGroupActive(item)
                  ? 'active bg-dark-peel text-white shadow-peel'
                  : 'text-slate-500 hover-text-dark'
              "
            >
              <i :class="item.icon"></i> {{ item.name }}
              <i class="bi bi-chevron-down ms-1" style="font-size: 0.7rem"></i>
            </div>

            <div
              class="dropdown-hover-menu position-absolute top-100 bg-white rounded-4 shadow-lg border border-slate-100 overflow-hidden pt-1 pb-1 px-1"
              :class="[
                item.id === 'users-group' || item.id === 'promotions-group'
                  ? 'end-0'
                  : 'start-0',
                { 'is-open': activeHoverMenu === item.id },
              ]"
              style="z-index: 1100"
            >
              <router-link
                v-for="child in item.children"
                :key="child.id"
                :to="child.path"
                @click="activeHoverMenu = null"
                class="dropdown-item-custom d-flex align-items-center gap-2 p-2 rounded-3 fw-medium text-decoration-none transition-all mb-1 text-nowrap"
                :class="{ 'active-child': isActive(child.path) }"
              >
                <i :class="child.icon" class="dropdown-icon"></i>
                {{ child.name }}
              </router-link>
            </div>
          </div>
        </template>
      </div>

      <div class="d-flex align-items-center gap-2 flex-shrink-0">
        <div
          class="text-slate-500 fw-medium d-none d-lg-flex align-items-center gap-2 bg-white px-3 py-2 rounded-pill shadow-sm border border-slate-100 time-box"
        >
          <i class="bi bi-clock text-dark-peel"></i> {{ currentTime }}
        </div>

        <div class="position-relative">
          <div
            @click.stop="toggleProfile"
            class="bg-dark-peel text-white rounded-circle fw-bold d-flex align-items-center justify-content-center shadow-peel cursor-pointer hover-lift avatar-box"
          >
            {{ userAvatar }}
          </div>

          <div
            v-if="showProfile"
            @click.stop
            class="dropdown-menu-custom position-absolute end-0 mt-2 bg-white rounded-4 shadow-lg border border-slate-100 overflow-hidden animate__animated animate__fadeIn animate__faster"
            style="z-index: 1100; width: 260px"
          >
            <div
              class="p-3 border-bottom border-slate-100 bg-slate-50 d-flex gap-3 align-items-center"
            >
              <div
                class="bg-dark-peel text-white rounded-circle fw-bold d-flex align-items-center justify-content-center flex-shrink-0 avatar-large"
              >
                {{ userAvatar }}
              </div>
              <div>
                <h6
                  class="mb-0 fw-bold text-slate-800"
                  style="font-size: 0.9rem"
                >
                  {{ userDisplayName }}
                </h6>
                <div class="text-slate-500" style="font-size: 0.75rem">
                  {{ userRoleDisplay }}
                </div>
              </div>
            </div>

            <div class="p-2">
              <button
                class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-slate-700 fw-medium mb-1"
              >
                <i class="bi bi-person me-2"></i> Hồ sơ cá nhân
              </button>
              <button
                class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-slate-700 fw-medium mb-1"
              >
                <i class="bi bi-shield-lock me-2"></i> Đổi mật khẩu
              </button>
              <hr class="my-1 border-slate-200" />
              <button
                @click="handleShiftHandover"
                class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-slate-700 fw-bold mb-1"
                style="color: #0284c7 !important"
              >
                <i class="bi bi-clock-history me-2"></i> Bàn giao ca làm việc
              </button>
            </div>

            <div class="p-2 border-top border-slate-100">
              <button
                @click="handleLogout"
                class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-danger fw-bold"
              >
                <i class="bi bi-box-arrow-right me-2"></i> Đăng xuất
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import request from "@/common/services/request";
import Swal from 'sweetalert2';

const router = useRouter();
const route = useRoute();
const currentTime = ref("");

const currentUser = ref(
  JSON.parse(localStorage.getItem("currentUser") || "{}")
);

// Chuẩn hóa quyền từ LocalStorage
const userRole = computed(() => {
  const rawRole = currentUser.value.role || currentUser.value.roles?.[0] || "";
  return rawRole.toUpperCase().replace("ROLE_", "") || "USER";
});

const userAvatar = computed(() => {
  const name = currentUser.value.fullName || currentUser.value.username || "AD";
  return name.substring(0, 2).toUpperCase();
});
const userDisplayName = computed(
  () => currentUser.value.fullName || currentUser.value.username || "Người Dùng"
);

const userRoleDisplay = computed(() => {
  if (userRole.value === "OWNER") return "Chủ Hệ Thống (Owner)";
  if (userRole.value === "MANAGER") return "Quản Lý Cửa Hàng (Manager)";
  if (userRole.value === "CASHIER") return "Thu Ngân (Cashier)";
  return "Khách Hàng";
});

const activeHoverMenu = ref<string | null>(null);

const isActive = (path: string) => {
  if (!path) return false;
  return route.path.startsWith(path);
};

const isGroupActive = (group: any) => {
  return group.children?.some((child: any) => isActive(child.path));
};

// 🛠️ CẤU TRÚC DANH MỤC QUẢN LÝ MỚI - ĐỒNG BỘ 100% THEO FILE CHIA TASK
const menuItems = [
  {
    id: "dashboard",
    path: "/admin/dashboard",
    name: "Tổng quan",
    icon: "bi-grid-1x2",
    roles: ["OWNER"], // Chỉ Owner được vào
  },
  {
    id: "pos",
    path: "/admin/pos",
    name: "Bán hàng (POS)",
    icon: "bi-upc-scan",
    roles: ["OWNER", "MANAGER", "CASHIER"],
  },
  {
    id: "catalog",
    name: "Sản phẩm",
    icon: "bi-box-seam",
    roles: ["OWNER", "MANAGER"],
    children: [
      {
        id: "products",
        path: "/admin/products",
        name: "Danh sách SP",
        icon: "bi-box",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "attributes",
        path: "/admin/attributes",
        name: "Thuộc tính SP",
        icon: "bi-tags",
        roles: ["OWNER", "MANAGER"],
      },
    ],
  },
  {
    id: "orders",
    path: "/admin/orders",
    name: "Đơn hàng",
    icon: "bi-receipt",
    roles: ["OWNER", "MANAGER", "CASHIER"],
  },
  {
    id: "promotions-group",
    name: "Khuyến mãi",
    icon: "bi-ticket-perforated",
    roles: ["OWNER", "MANAGER"],
    children: [
      {
        id: "vouchers",
        path: "/admin/vouchers",
        name: "Mã giảm giá",
        icon: "bi-percent",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "flash-sale",
        path: "/admin/flash-sale",
        name: "Flash Sale",
        icon: "bi-lightning-charge",
        roles: ["OWNER", "MANAGER"],
      },
    ],
  },
  {
    id: "users-group",
    name: "Người dùng",
    icon: "bi-people",
    roles: ["OWNER", "MANAGER"], // Cả 2 được xem danh mục cha để vào xem Khách hàng
    children: [
      {
        id: "customers",
        path: "/admin/customers",
        name: "Khách hàng",
        icon: "bi-person-heart",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "employees",
        path: "/admin/employees",
        name: "Nhân viên",
        icon: "bi-person-badge",
        roles: ["OWNER"],
      }, // Chỉ Owner thấy Nhân viên
    ],
  },
];

const filteredMenuItems = computed(() => {
  return menuItems.reduce((acc: any[], item) => {
    if (item.roles.includes(userRole.value)) {
      if (item.children) {
        const filteredChildren = item.children.filter((child) =>
          child.roles.includes(userRole.value)
        );
        if (filteredChildren.length > 0) {
          acc.push({ ...item, children: filteredChildren });
        }
      } else {
        acc.push(item);
      }
    }
    return acc;
  }, []);
});

const showProfile = ref(false);
const toggleProfile = () => {
  showProfile.value = !showProfile.value;
};
const closeAllMenus = () => {
  showProfile.value = false;
  activeHoverMenu.value = null;
};

const handleShiftHandover = async () => {
  showProfile.value = false;
  try {
    const userId = currentUser.value.id || currentUser.value.userId || 0;
    if (!userId) {
      return Swal.fire({
        icon: "error",
        title: "Lỗi",
        text: "Không xác định được danh tính nhân viên!",
      });
    }
    const res = await request.get(`/shifts/summary?userId=${userId}`);
    const data = res.data;
    const formattedRevenue = new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(data.totalRevenue || 0);

    Swal.fire({
      title: "Tổng kết ca làm việc",
      html: `Doanh thu trong ca: <b>${formattedRevenue}</b><br/>Xác nhận bàn giao?`,
      icon: "info",
      showCancelButton: true,
      confirmButtonColor: "#111111",
      cancelButtonColor: "#94a3b8",
      confirmButtonText:
        '<i class="bi bi-printer me-1"></i> Chốt ca & In phiếu',
      cancelButtonText: "Đóng",
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          icon: "success",
          title: "Chốt ca thành công!",
          text: "Phiếu bàn giao đang được in...",
          timer: 2000,
          showConfirmButton: false,
        });
        setTimeout(() => {
          localStorage.removeItem("accessToken");
          localStorage.removeItem("currentUser");
          router.push("/login");
        }, 2000);
      }
    });
  } catch (error) {
    Swal.fire({
      icon: "error",
      title: "Lỗi",
      text: "Không thể kết nối đến máy chủ hoặc phiên làm việc đã hết hạn.",
    });
  }
};

const handleLogout = () => {
  showProfile.value = false;
  Swal.fire({
    title: "Đăng xuất",
    text: "Bạn có chắc chắn muốn đăng xuất?",
    icon: "question",
    showCancelButton: true,
    confirmButtonColor: "#ef4444",
    cancelButtonColor: "#1e293b",
    confirmButtonText: "Đăng xuất",
    cancelButtonText: "Hủy",
  }).then((result) => {
    if (result.isConfirmed) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("currentUser");
      Swal.fire({
        icon: "success",
        title: "Đã đăng xuất",
        timer: 1500,
        showConfirmButton: false,
      });
      router.push("/login");
    }
  });
};

let timer: ReturnType<typeof setInterval>;
onMounted(() => {
  const updateClock = () => {
    const now = new Date();
    const p = (n: number) => n.toString().padStart(2, "0");
    currentTime.value = `${p(now.getHours())}:${p(now.getMinutes())}:${p(
      now.getSeconds()
    )} - ${p(now.getDate())}/${p(now.getMonth() + 1)}/${now.getFullYear()}`;
  };
  updateClock();
  timer = setInterval(updateClock, 1000);
  window.addEventListener("click", closeAllMenus);
});

onUnmounted(() => {
  clearInterval(timer);
  window.removeEventListener("click", closeAllMenus);
});
</script>

<style scoped>
.top-nav-glass {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  height: 64px;
}
.menu-scroll-container {
  overflow: visible !important;
}
.nav-pill {
  color: #64748b;
  border-radius: 50px;
  padding: 5px 14px;
  font-size: 0.8rem;
  border: none;
  background: transparent;
}
.nav-pill.active {
  background-color: #111111 !important;
  color: #ffffff !important;
}
.hover-text-dark:hover {
  color: #1e293b !important;
}
.shadow-peel {
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.1);
}
.hover-lift {
  transition: transform 0.2s, box-shadow 0.2s;
}
.hover-lift:hover {
  transform: translateY(-2px);
}
.bg-dark-peel {
  background-color: #111111 !important;
}
.text-dark-peel {
  color: #111111 !important;
}
.cursor-pointer {
  cursor: pointer;
}
.logo-box {
  width: 32px;
  height: 32px;
}
.avatar-box {
  width: 36px;
  height: 36px;
  font-size: 0.85rem;
}
.avatar-large {
  width: 45px;
  height: 45px;
}
.time-box {
  font-size: 0.8rem;
}
.dropdown-hover-container {
  display: flex;
  align-items: center;
  height: 100%;
}
.dropdown-hover-menu {
  opacity: 0;
  visibility: hidden;
  transform: translateY(10px);
  min-width: 170px;
  transition: opacity 0.15s ease-in-out, transform 0.15s ease-in-out,
    visibility 0.15s;
  margin-top: 8px;
}
.dropdown-hover-menu::before {
  content: "";
  position: absolute;
  top: -25px;
  left: 0;
  right: 0;
  height: 25px;
  background: transparent;
}
.dropdown-hover-menu.is-open {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}
.dropdown-item-custom {
  font-size: 0.85rem;
  color: #475569 !important;
  transition: all 0.15s ease-in-out;
}
.dropdown-icon {
  color: #94a3b8 !important;
  font-size: 0.95rem;
  width: 20px;
}
.dropdown-item-custom:hover {
  background-color: #f1f5f9 !important;
  color: #111111 !important;
}
.dropdown-item-custom:hover .dropdown-icon {
  color: #111111 !important;
}
.active-child {
  background-color: #f1f5f9 !important;
  color: #111111 !important;
  font-weight: 700 !important;
}
.active-child .dropdown-icon {
  color: #111111 !important;
}
.dropdown-menu-custom {
  transform-origin: top right;
}
.dropdown-item-custom-btn {
  transition: background-color 0.2s;
  font-size: 0.85rem;
}
.dropdown-item-custom-btn:hover {
  background-color: #f1f5f9 !important;
}
.animate__faster {
  animation-duration: 0.2s;
}
</style>
