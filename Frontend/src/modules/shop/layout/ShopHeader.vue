<template>
  <div class="shop-header">
    <div class="container-fluid px-4 px-lg-5 h-100">
      <div class="row align-items-center h-100">
        <div class="col-4 col-lg-3">
          <RouterLink to="/" class="brand-logo-link text-decoration-none d-inline-flex align-items-center">
            <img v-if="!logoLoadFailed" :src="logoAura" alt="DOMINUS PERFUME" class="brand-logo-img"
              @error="logoLoadFailed = true" />

            <div v-else class="brand-logo-text">
              <div class="brand-name">DOMINUS</div>
              <div class="brand-subtitle">PERFUME</div>
            </div>
          </RouterLink>
        </div>

        <div class="col-12 col-lg-6 order-3 order-lg-2 mt-3 mt-lg-0">
          <div class="search-wrapper position-relative mx-auto">
            <input v-model="keyword" type="text" class="form-control rounded-pill search-input"
              placeholder="Tìm kiếm nước hoa..." @keyup.enter="handleSearch" />

            <button type="button" class="search-button" aria-label="Tìm kiếm" @click="handleSearch">
              <i class="bi bi-search"></i>
            </button>
          </div>
        </div>

        <div class="col-8 col-lg-3 order-2 order-lg-3">
          <div
            class="d-flex align-items-center justify-content-end gap-3 gap-lg-4"
          >
            <div class="account-dropdown-wrapper">
              <button type="button" class="header-action account-trigger d-flex align-items-center gap-2">
                <span class="action-icon-circle">
                  <i class="bi bi-person"></i>
                </span>

                <span class="d-none d-md-inline">
                  {{
                    isAuthenticated
                      ? `${name || "Khách"}`
                      : "Tài khoản"
                  }}
                </span>

                <i
                  class="bi bi-chevron-down account-chevron d-none d-md-inline"
                ></i>
              </button>

              <div class="account-dropdown">
                <div v-if="!isAuthenticated" class="guest-dropdown">
                  <p class="dropdown-title mb-3">Tài khoản khách hàng</p>

                  <RouterLink
                    to="/login"
                    class="btn dropdown-login-btn w-100 mb-2"
                  >
                    Đăng nhập
                  </RouterLink>

                  <RouterLink
                    to="/register"
                    class="btn dropdown-register-btn w-100"
                  >
                    Đăng ký
                  </RouterLink>
                </div>

                <div v-else class="logged-dropdown">
                  <div class="user-block d-flex align-items-center gap-3">
                    <div class="user-avatar">
                      {{ userInitial }}
                    </div>

                    <div class="user-info">
                      <div class="user-name">{{ name || "Khách hàng" }}</div>

                      <div class="rank-badge mt-1">
                        <i class="bi bi-gem me-1"></i>
                        {{ userRank }} Rank - {{ userPoints }} Pts
                      </div>
                    </div>
                  </div>

                  <div class="dropdown-divider"></div>

                  <RouterLink to="/profile" class="account-menu-item">
                    <i class="bi bi-person-circle"></i>
                    <span>Thông tin cá nhân</span>
                  </RouterLink>

                  <RouterLink to="/profile/favorites" class="account-menu-item">
                    <i class="bi bi-heart"></i>
                    <span>Sản phẩm yêu thích</span>
                  </RouterLink>

                  <RouterLink to="/profile/orders" class="account-menu-item">
                    <i class="bi bi-receipt"></i>
                    <span>Lịch sử đơn hàng</span>
                  </RouterLink>

                  <button
                    type="button"
                    class="account-menu-item logout-item"
                    @click="handleLogout"
                  >
                    <i class="bi bi-box-arrow-right"></i>
                    <span>Đăng xuất</span>
                  </button>
                </div>
              </div>
            </div>

            <RouterLink to="/cart"
              class="header-action cart-action d-flex align-items-center gap-2 text-decoration-none position-relative">
              <span class="action-icon-circle">
                <i class="bi bi-bag"></i>
              </span>

              <span class="d-none d-md-inline">Giỏ hàng</span>

              <span class="cart-badge">0</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import api from "@/common/api";
import logoAura from "@/assets/logo.png";
import Swal from "sweetalert2";
const router = useRouter();
const authStore = useAuthStore();

// Lấy state từ store (đảm bảo reactive)
const { isAuthenticated, name } = storeToRefs(authStore);

const logoLoadFailed = ref(false);
const keyword = ref("");

// Trạng thái Rank và Điểm lấy từ API
const userRank = ref("Bronze");
const userPoints = ref(0);

// Tính toán Avatar từ tên thật
const userInitial = computed(() => {
  if (!name.value) return "U";
  const nameParts = name.value.trim().split(" ");
  return nameParts[nameParts.length - 1]?.charAt(0).toUpperCase() || "U";
});

// Hàm lấy thông tin Rank và Điểm
const fetchCustomerProfile = async () => {
  // SỬA CHỖ NÀY: Chỉ gọi API nếu đã đăng nhập VÀ tài khoản KHÔNG PHẢI là Nhân viên
  if (isAuthenticated.value && !authStore.isAdminSection) {
    try {
      const res = await api.get("/customer/profile");
      userRank.value = res.data.customerRank || "Bronze";
      userPoints.value = res.data.loyaltyPoints || 0;
    } catch (error) {
      console.error("Lỗi lấy thông tin Rank/Điểm:", error);
    }
  }
};

// Gọi API khi component vừa load lên
onMounted(() => {
  fetchCustomerProfile();
});

// Chờ và gọi API lại nếu người dùng vừa mới đăng nhập thành công
watch(isAuthenticated, async (newVal) => {
  if (newVal) {
    // SỬA CHỖ NÀY: Xóa hoàn toàn khối setTimeout 200ms đi.
    // Giờ đây gọi thẳng luôn vì authStore đã lưu Token xong xuôi rồi.
    await fetchCustomerProfile();
  }
});

const handleSearch = () => {
  const trimmedKeyword = keyword.value.trim();

  if (!trimmedKeyword) {
    router.push("/products");
    return;
  }

  router.push({
    path: "/products",
    query: {
      keyword: trimmedKeyword,
    },
  });
};

const handleLogout = () => {
  Swal.fire({
    title: "Xác nhận đăng xuất?",
    text: "Bạn có chắc chắn muốn rời khỏi phiên làm việc này không?",
    icon: "warning",
    showCancelButton: true,
    confirmButtonColor: "#bd9a5f", // Giữ màu Vàng Gold cho nút Đồng ý để làm điểm nhấn thương hiệu
    cancelButtonColor: "#6c757d", // Nút Hủy chuyển sang màu xám nhẹ hợp với nền trắng
    confirmButtonText: "Đồng ý",
    cancelButtonText: "Hủy",
    background: "#ffffff", // 👇 Đổi thành Nền Màu Trắng
    color: "#000000", // 👇 Đổi thành Chữ Màu Đen
    iconColor: "#dc3545", // Đổi icon cảnh báo sang màu đỏ (hoặc #bd9a5f tùy bạn) để nổi bật trên nền trắng
    customClass: {
      popup: "border-gold-sweetalert", // Vẫn giữ viền gold mỏng nếu bạn muốn, hoặc bỏ dòng customClass này đi nếu muốn trắng tinh khôi
    },
  }).then((result) => {
    // Nếu người dùng nhấn nút "Đồng ý"
    if (result.isConfirmed) {
      // Thực hiện các bước xóa session
      authStore.logout();

      userRank.value = "Bronze";
      userPoints.value = 0;

      // Chuyển về trang chủ
      router.push("/");
    }
  });
};
</script>

<style scoped>
.shop-header {
  position: relative;
  z-index: 1050;
  min-height: 104px;
  overflow: visible;
  background: radial-gradient(
      circle at top left,
      rgba(189, 154, 95, 0.16),
      transparent 32%
    ),
    linear-gradient(135deg, #030d1a 0%, #07172f 54%, #051024 100%);
  border-bottom: 1px solid rgba(189, 154, 95, 0.22);
}

/* LOGO */
.brand-logo-link {
  min-height: 104px;
  width: 260px;
  padding-left: 14px;
  overflow: visible;
}

.brand-logo-img {
  width: 210px;
  height: auto;
  max-height: 96px;
  object-fit: contain;
  object-position: left center;
  display: block;
  transform: scale(1.35);
  transform-origin: left center;
  filter: drop-shadow(0 10px 26px rgba(189, 154, 95, 0.26));
}

.brand-logo-text {
  text-align: center;
  line-height: 1;
}

.brand-name {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 48px;
  letter-spacing: 5px;
  font-weight: 600;
  color: var(--aura-gold);
}

.brand-subtitle {
  margin-top: 7px;
  font-size: 12px;
  letter-spacing: 7px;
  color: var(--aura-gold);
  font-weight: 600;
}

/* SEARCH */
.search-wrapper {
  max-width: 700px;
}

.search-input {
  height: 50px;
  padding-left: 26px;
  padding-right: 58px;
  border: 1px solid rgba(189, 154, 95, 0.42);
  background: rgba(255, 255, 255, 0.075);
  color: #ffffff;
  font-size: 15px;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.72);
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.11);
  color: #ffffff;
  border-color: var(--aura-gold);
  box-shadow: 0 0 0 0.22rem rgba(189, 154, 95, 0.16);
}

.search-button {
  position: absolute;
  top: 50%;
  right: 7px;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  transform: translateY(-50%);
  background: transparent;
  color: var(--aura-gold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
}

.search-button:hover {
  background: rgba(189, 154, 95, 0.14);
}

.search-button i {
  font-size: 18px;
}

/* ACCOUNT / CART */
.header-action {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 600;
  font-size: 15px;
  transition: all 0.22s ease;
}

.header-action:hover {
  color: var(--aura-gold);
}

.account-trigger {
  border: none;
  outline: none;
  background: transparent;
  padding: 0;
}

.account-chevron {
  font-size: 12px;
  opacity: 0.8;
  transition: transform 0.22s ease;
}

.account-dropdown-wrapper:hover .account-chevron {
  transform: rotate(180deg);
}

.action-icon-circle {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(189, 154, 95, 0.58);
  border-radius: 50%;
  color: var(--aura-gold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.045);
  transition: all 0.22s ease;
}

.action-icon-circle i {
  font-size: 18px;
  line-height: 1;
}

.header-action:hover .action-icon-circle {
  background: var(--aura-gold);
  color: var(--aura-navy);
}

.cart-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: var(--aura-gold);
  color: var(--aura-navy);
  border: 2px solid var(--aura-navy);
  font-size: 10px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ACCOUNT DROPDOWN */
.account-dropdown-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 18px);
  right: 0;
  z-index: 3000;
  width: 320px;
  padding: 18px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.18);
  box-shadow: 0 24px 60px rgba(5, 16, 36, 0.22);
  opacity: 0;
  visibility: hidden;
  transform: translateY(10px);
  transition: all 0.22s ease;
  pointer-events: none;
}

/* --- GIỮ NGUYÊN ĐOẠN CŨ NÀY CỦA BẠN --- */
.account-dropdown::before {
  content: "";
  position: absolute;
  top: -8px;
  right: 46px;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-left: 1px solid rgba(189, 154, 95, 0.18);
  border-top: 1px solid rgba(189, 154, 95, 0.18);
  transform: rotate(45deg);
}

/* --- THÊM CHÍNH XÁC ĐOẠN MỚI NÀY VÀO NGAY DƯỚI --- */
.account-dropdown::after {
  content: "";
  position: absolute;
  top: -24px;
  /* Phủ ngược lên trên 24px để dính liền vào nút bấm */
  left: 0;
  width: 100%;
  height: 24px;
  /* Tạo một cầu nối vô hình cao 24px */
  background: transparent;
  /* Hoàn toàn trong suốt */
}

.account-dropdown-wrapper:hover .account-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  pointer-events: auto;
}

.dropdown-title {
  color: var(--aura-black);
  font-weight: 800;
  font-size: 15px;
}

.dropdown-login-btn {
  background: var(--aura-gold);
  color: #ffffff;
  font-weight: 800;
  border-radius: 8px;
  border: 1px solid var(--aura-gold);
}

.dropdown-login-btn:hover {
  background: var(--aura-gold-hover);
  color: #ffffff;
}

.dropdown-register-btn {
  background: #ffffff;
  color: var(--aura-gold);
  font-weight: 800;
  border-radius: 8px;
  border: 1px solid var(--aura-gold);
}

.dropdown-register-btn:hover {
  background: rgba(189, 154, 95, 0.08);
  color: var(--aura-gold-hover);
}

.user-block {
  padding-bottom: 4px;
}

.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #d1b06e, var(--aura-gold));
  color: #ffffff;
  font-weight: 900;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(189, 154, 95, 0.24);
}

.user-name {
  color: var(--aura-black);
  font-weight: 800;
  font-size: 15px;
}

.rank-badge {
  width: fit-content;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(189, 154, 95, 0.14);
  color: var(--aura-gold);
  border: 1px solid rgba(189, 154, 95, 0.26);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.dropdown-divider {
  margin: 16px 0 10px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
}

.account-menu-item {
  width: 100%;
  min-height: 42px;
  padding: 10px 8px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--aura-black);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.account-menu-item i {
  width: 20px;
  color: var(--aura-gold);
  font-size: 17px;
}

.account-menu-item:hover {
  background: rgba(189, 154, 95, 0.09);
  color: var(--aura-gold);
}

.logout-item {
  cursor: pointer;
  color: #b31320;
}

.logout-item i {
  color: #b31320;
}

.logout-item:hover {
  background: rgba(179, 19, 32, 0.08);
  color: #b31320;
}

/* TABLET */
@media (max-width: 991.98px) {
  .shop-header {
    min-height: auto;
    padding: 16px 0;
  }

  .brand-logo-link {
    min-height: auto;
    width: 190px;
    padding-left: 4px;
  }

  .brand-logo-img {
    width: 150px;
    max-height: 72px;
    transform: scale(1.18);
  }

  .brand-name {
    font-size: 32px;
  }

  .brand-subtitle {
    font-size: 10px;
    letter-spacing: 5px;
  }

  .account-dropdown {
    right: -70px;
  }

  .account-dropdown::before {
    right: 116px;
  }
}

/* MOBILE */
@media (max-width: 575.98px) {
  .brand-logo-link {
    width: 140px;
  }

  .brand-logo-img {
    width: 118px;
    max-height: 58px;
    transform: scale(1.12);
  }

  .action-icon-circle {
    width: 35px;
    height: 35px;
  }

  .header-action {
    font-size: 14px;
  }

  .account-dropdown {
    width: 290px;
    right: -88px;
  }

  .account-dropdown::before {
    right: 132px;
  }
}
</style>
