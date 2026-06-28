<template>
  <aside class="sidebar-glass position-fixed top-0 start-0 bottom-0 d-flex flex-column shadow-sm" style="z-index: 1050; width: 260px">
   <div class="py-1 px-2 d-flex align-items-center justify-content-center border-bottom border-slate-100 flex-shrink-0 overflow-hidden" style="height: 75px;">
      <img 
        src="@/assets/logo.png" 
        alt="Dominus Logo" 
        class="w-100 object-fit-contain transition-all" 
        style="height: 170px; margin: -15px 0;" 
      />
    </div>

    <div class="flex-grow-1 overflow-y-auto px-3 py-3 menu-scroll-container">
      <div class="d-flex flex-column gap-1">
        <template v-for="item in filteredMenuItems" :key="item.id">
          
          <router-link
            v-if="!item.children"
            :to="item.path"
            class="nav-pill-vertical d-flex align-items-center gap-3 transition-all fw-semibold text-decoration-none"
            :class="isActive(item.path) ? 'active bg-dark-peel text-white shadow-peel' : 'text-slate-600 hover-bg-slate-100'"
          >
            <i :class="item.icon" class="fs-5 nav-icon-width"></i>
            <span>{{ item.name }}</span>
          </router-link>

          <div v-else class="d-flex flex-column">
            <div
              @click="toggleMenu(item.id)"
              class="nav-pill-vertical d-flex align-items-center justify-content-between transition-all fw-semibold cursor-pointer text-slate-600 hover-bg-slate-100"
              :class="{ 'text-dark-peel fw-bold': isGroupActive(item) }"
            >
              <div class="d-flex align-items-center gap-3">
                <i class="fs-5 nav-icon-width" :class="[item.icon, { 'text-dark-peel': isGroupActive(item) }]"></i>
                <span>{{ item.name }}</span>
              </div>
              <i class="bi bi-chevron-down transition-transform arrow-icon" :class="{ 'rotate-180': openMenus[item.id] }"></i>
            </div>

            <div v-if="openMenus[item.id]" class="d-flex flex-column gap-1 mt-1 ps-4 submenu-wrapper animate__animated animate__fadeIn animate__faster">
              <router-link
                v-for="child in item.children"
                :key="child.id"
                :to="child.path"
                class="dropdown-item-custom d-flex align-items-center gap-2 p-2 rounded-3 fw-medium text-decoration-none transition-all"
                :class="isActive(child.path) ? 'active-child' : ''"
              >
                <i :class="child.icon" class="dropdown-icon"></i>
                <span>{{ child.name }}</span>
              </router-link>
            </div>
          </div>
          
        </template>
      </div>
    </div>

    <div class="p-3 border-top border-slate-100 bg-slate-50 mt-auto flex-shrink-0 position-relative">
      <div class="text-slate-500 fw-medium d-flex align-items-center gap-2 bg-white px-3 py-2 rounded-3 shadow-sm border border-slate-100 mb-3 time-box justify-content-center">
        <i class="bi bi-clock text-dark-peel"></i> {{ currentTime }}
      </div>

      <div
        @click.stop="toggleProfile"
        class="d-flex align-items-center gap-2 p-2 rounded-3 cursor-pointer user-profile-box transition-all border border-transparent"
        :class="showProfile ? 'bg-white shadow-sm border-slate-200' : ''"
      >
        <div class="bg-dark-peel text-white rounded-circle fw-bold d-flex align-items-center justify-content-center shadow-peel avatar-box flex-shrink-0">
          {{ userAvatar }}
        </div>
        <div class="overflow-hidden flex-grow-1 text-nowrap text-truncate pe-1">
          <h6 class="mb-0 fw-bold text-slate-800 text-truncate" style="font-size: 0.85rem">
            {{ userDisplayName }}
          </h6>
          <div class="text-slate-500 text-truncate" style="font-size: 0.7rem">
            {{ userRoleDisplay }}
          </div>
        </div>
        <i class="bi bi-three-dots-vertical text-slate-400"></i>
      </div>

      <div
        v-if="showProfile"
        @click.stop
        class="dropdown-menu-custom position-absolute start-0 end-0 mx-3 bg-white rounded-4 shadow-lg border border-slate-200 overflow-hidden animate__animated animate__fadeInUp animate__faster"
        style="z-index: 1100; bottom: 85px"
      >
        <div class="p-2">
          <button class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-slate-700 fw-medium mb-1">
            <i class="bi bi-person me-2"></i> Hồ sơ cá nhân
          </button>
          <button class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-slate-700 fw-medium mb-1">
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

        <div class="p-2 border-top border-slate-100 bg-slate-50">
          <button
            @click="handleLogout"
            class="dropdown-item-custom-btn w-100 text-start bg-transparent border-0 p-2 rounded-3 text-danger fw-bold"
          >
            <i class="bi bi-box-arrow-right me-2"></i> Đăng xuất
          </button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive } from "vue";
import { useRouter, useRoute } from "vue-router";
import request from "@/common/services/request";
import Swal from 'sweetalert2';
import { useAuthStore } from "@/modules/auth/stores/authStore";

const router = useRouter();
const route = useRoute();
const currentTime = ref("");
const authStore = useAuthStore();

const currentUser = ref(
  JSON.parse(localStorage.getItem("currentUser") || "{}")
);

// CHỖ ĐÃ SỬA: Đổi từ ref sang reactive để Vue bắt trọn Reactivity động 100%
const openMenus = reactive<Record<string, boolean>>({});

const toggleMenu = (id: string) => {
  openMenus[id] = !openMenus[id];
};

const userRole = computed(() => {
  const rawRole = authStore.role || "";
  return rawRole.toUpperCase().replace("ROLE_", "") || "USER";
});

const userDisplayName = computed(
  () => authStore.name || currentUser.value.fullName || currentUser.value.username || "Người Dùng"
);

const userAvatar = computed(() => {
  const name = authStore.name || currentUser.value.fullName || currentUser.value.username || "AD";
  return name.substring(0, 2).toUpperCase();
});

const userRoleDisplay = computed(() => {
  if (userRole.value === "OWNER") return "Chủ Hệ Thống";
  if (userRole.value === "MANAGER") return "Quản Lý";
  if (userRole.value === "CASHIER") return "Thu Ngân";
  return "Khách Hàng";
});

const isActive = (path: string) => {
  if (!path) return false;
  return route.path === path || route.path.startsWith(path + "/");
};

const isGroupActive = (group: any) => {
  return group.children?.some((child: any) => isActive(child.path));
};

const menuItems = [
  {
    id: "dashboard",
    path: "/admin/dashboard",
    name: "Thống kê báo cáo",
    icon: "bi-graph-up-arrow",
    roles: ["OWNER"],
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
    name: "Quản lý sản phẩm",
    icon: "bi-box-seam",
    roles: ["OWNER", "MANAGER"],
    children: [
      {
        id: "products",
        path: "/admin/products",
        name: "Sản phẩm & Biến thể",
        icon: "bi-box",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "categories",
        path: "/admin/categories",
        name: "Danh mục (Category)",
        icon: "bi-folder2",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "brands",
        path: "/admin/brands",
        name: "Thương hiệu (Brand)",
        icon: "bi-award",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "fragrance-families",
        path: "/admin/fragrance-families",
        name: "Nhóm hương",
        icon: "bi-droplet",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "capacities",
        path: "/admin/capacities",
        name: "Dung tích (Capacity)",
        icon: "bi-moisture",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "concentrations",
        path: "/admin/concentrations",
        name: "Nồng độ (Concentration)",
        icon: "bi-funnel",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "bottle-types",
        path: "/admin/bottle-types",
        name: "Loại vỏ chai",
        icon: "bi-cylinder",
        roles: ["OWNER", "MANAGER"],
      },
    ],
  },
  {
    id: "orders",
    path: "/admin/orders",
    name: "Quản lý đơn hàng",
    icon: "bi-receipt",
    roles: ["OWNER", "MANAGER", "CASHIER"],
  },
  {
    id: "promotions-group",
    name: "Chương trình khuyến mãi",
    icon: "bi-ticket-perforated",
    roles: ["OWNER", "MANAGER"],
    children: [
      {
        id: "vouchers",
        path: "/admin/vouchers",
        name: "Mã giảm giá (Voucher)",
        icon: "bi-tags",
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
    name: "Quản lý thành viên",
    icon: "bi-people",
    roles: ["OWNER", "MANAGER"],
    children: [
      {
        id: "customers",
        path: "/admin/customers",
        name: "Khách hàng (Customer)",
        icon: "bi-person-heart",
        roles: ["OWNER", "MANAGER"],
      },
      {
        id: "employees",
        path: "/admin/employees",
        name: "Nhân viên (Employee)",
        icon: "bi-person-badge",
        roles: ["OWNER"],
      },
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
      confirmButtonText: '<i class="bi bi-printer me-1"></i> Chốt ca & In phiếu',
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
          authStore.logout();
          router.push({ name: "AdminLogin" }); 
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
      authStore.logout();
      Swal.fire({
        icon: "success",
        title: "Đã đăng xuất",
        timer: 1500,
        showConfirmButton: false,
      });
      setTimeout(() => {
        // SỬA CHỖ NÀY: Dùng window.location.href để ép trình duyệt Hard Reload
        // Xóa sạch hoàn toàn BFCache và bộ nhớ RAM cũ của Cashier
        window.location.href = "/admin/login"; 
      }, 1500);
    }
  });
};

let timer: ReturnType<typeof setInterval>;
onMounted(() => {
  const updateClock = () => {
    const now = new Date();
    const p = (n: number) => n.toString().padStart(2, "0");
    currentTime.value = `${p(now.getHours())}:${p(now.getMinutes())}:${p(now.getSeconds())} - ${p(now.getDate())}/${p(now.getMonth() + 1)}/${now.getFullYear()}`;
  };
  updateClock();
  timer = setInterval(updateClock, 1000);
  window.addEventListener("click", closeAllMenus);

  // CHỖ ĐÃ SỬA: Đăng ký toàn bộ Key con vào Reactive để đảm bảo bắt trọn tương tác click
  menuItems.forEach((item) => {
    if (item.children) {
      openMenus[item.id] = isGroupActive(item) ? true : false;
    }
  });
});

onUnmounted(() => {
  clearInterval(timer);
  window.removeEventListener("click", closeAllMenus);
});
</script>

<style scoped>
.sidebar-glass {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-right: 1px solid #e2e8f0;
}
.menu-scroll-container::-webkit-scrollbar {
  width: 4px;
}
.menu-scroll-container::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
.nav-pill-vertical {
  color: #475569;
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 0.85rem;
  border: none;
  background: transparent;
}
.nav-pill-vertical.active {
  background-color: #111111 !important;
  color: #ffffff !important;
}
.hover-bg-slate-100:hover {
  background-color: #f1f5f9 !important;
  color: #111111 !important;
}
.nav-icon-width {
  width: 24px;
  text-align: center;
}
.arrow-icon {
  font-size: 0.75rem;
  transition: transform 0.2s ease;
}
.rotate-180 {
  transform: rotate(180deg);
}
.shadow-peel {
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.1);
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
  width: 38px;
  height: 38px;
}
.avatar-box {
  width: 36px;
  height: 36px;
  font-size: 0.85rem;
}
.time-box {
  font-size: 0.75rem;
}
.submenu-wrapper {
  transition: all 0.2s ease-in-out;
}
.dropdown-item-custom {
  font-size: 0.8rem;
  color: #64748b !important;
  border-radius: 8px;
}
.dropdown-icon {
  color: #94a3b8 !important;
  font-size: 0.85rem;
  width: 20px;
  text-align: center;
}
.dropdown-item-custom:hover {
  background-color: #f8fafc !important;
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
.user-profile-box {
  border: 1px solid transparent;
}
.user-profile-box:hover {
  background-color: #f1f5f9;
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