<template>
  <div class="customer-profile-page">
    <div class="container py-4">
      <div class="profile-header mb-4">
        <div>
          <h2 class="fw-bold mb-1">Tài khoản của tôi</h2>
          <p class="text-muted mb-0">
            Quản lý thông tin cá nhân, yêu thích và lịch sử đơn hàng
          </p>
        </div>
      </div>

      <div v-if="store.profile" class="rank-card mb-4">
        <div>
          <div class="text-muted small">Hạng thành viên</div>
          <div class="fw-bold fs-4">
            {{ store.profile.customerRank || "Bronze" }}
          </div>
        </div>

        <div>
          <div class="text-muted small">Tổng điểm tích lũy</div>
          <div class="fw-bold fs-4">
            {{ store.profile.loyaltyPoints || 0 }}
          </div>
        </div>

        <div>
          <div class="text-muted small">Email</div>
          <div class="fw-semibold">
            {{ store.profile.email }}
          </div>
        </div>
      </div>

      <div v-if="store.error" class="message-box message-error">
        {{ store.error }}
      </div>

      <div v-if="store.success" class="message-box message-success">
        {{ store.success }}
      </div>

      <div class="profile-layout">
        <div class="profile-sidebar">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="['profile-tab', { active: store.activeTab === tab.key }]"
            @click="handleTabClick(tab.key)"
          >
            <i :class="tab.icon"></i>
            {{ tab.label }}
          </button>
        </div>

        <div class="profile-content">
          <div v-if="store.loading" class="loading-box">
            <div class="spinner-border"></div>
            <p class="mt-3 mb-0">Đang tải dữ liệu...</p>
          </div>

          <template v-else>
            <ProfileForm v-if="store.activeTab === 'profile'" />

            <ChangePasswordForm v-if="store.activeTab === 'password'" />

            <FavoriteList v-if="store.activeTab === 'favorites'" />

            <OrderHistory v-if="store.activeTab === 'orders'" />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import ChangePasswordForm from "../components/ChangePasswordForm.vue";
import FavoriteList from "../components/FavoriteList.vue";
import OrderHistory from "../components/OrderHistory.vue";
import ProfileForm from "../components/ProfileForm.vue";
import { useCustomerProfileStore } from "../stores/customerProfile.store";

const route = useRoute();
const router = useRouter();
const store = useCustomerProfileStore();

const tabs = [
  {
    key: "profile",
    label: "Thông tin cá nhân",
    icon: "bi bi-person",
  },
  {
    key: "password",
    label: "Đổi mật khẩu",
    icon: "bi bi-lock",
  },
  {
    key: "favorites",
    label: "Sản phẩm yêu thích",
    icon: "bi bi-heart",
  },
  {
    key: "orders",
    label: "Lịch sử đơn hàng",
    icon: "bi bi-receipt",
  },
] as const;

type ProfileTab = (typeof tabs)[number]["key"];

const isValidTab = (value: unknown): value is ProfileTab => {
  return ["profile", "password", "favorites", "orders"].includes(String(value));
};

const syncTabFromRoute = () => {
  const tab = route.query.tab;

  if (isValidTab(tab)) {
    store.activeTab = tab;
    return;
  }

  store.activeTab = "profile";
};

const handleTabClick = (tab: ProfileTab) => {
  store.activeTab = tab;

  router.replace({
    path: "/customer/profile",
    query: {
      tab,
    },
  });
};

onMounted(() => {
  syncTabFromRoute();
  store.loadInitialData();
});

watch(
  () => route.query.tab,
  () => {
    syncTabFromRoute();
  }
);
</script>

<style scoped>
.customer-profile-page {
  background: #f8fafc;
  min-height: 100vh;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rank-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  background: #ffffff;
  padding: 22px;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.profile-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
}

.profile-sidebar {
  background: #ffffff;
  border-radius: 18px;
  padding: 14px;
  height: fit-content;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.profile-tab {
  width: 100%;
  border: none;
  background: transparent;
  padding: 13px 14px;
  border-radius: 12px;
  text-align: left;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-tab:hover,
.profile-tab.active {
  background: #111827;
  color: #ffffff;
}

.profile-content {
  min-width: 0;
}

.loading-box {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

@media (max-width: 992px) {
  .rank-card {
    grid-template-columns: 1fr;
  }

  .profile-layout {
    grid-template-columns: 1fr;
  }
}
.message-box {
  border-radius: 14px;
  padding: 12px 16px;
  font-weight: 600;
  margin-bottom: 18px;
}

.message-error {
  background: #fff1f2;
  color: #b91c1c;
  border: 1px solid #fecdd3;
}

.message-success {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}
</style>
