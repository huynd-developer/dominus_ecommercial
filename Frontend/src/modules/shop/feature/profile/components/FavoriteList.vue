<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3 d-flex justify-content-between">
      <h5 class="mb-0 fw-bold">Sản phẩm yêu thích</h5>

      <button
        class="btn btn-outline-dark btn-sm"
        :disabled="store.favoriteLoading"
        @click="store.fetchFavorites()"
      >
        Làm mới
      </button>
    </div>

    <div class="card-body">
      <div v-if="store.favoriteLoading" class="text-center py-5">
        <div class="spinner-border"></div>
      </div>

      <div v-else-if="store.favorites.length === 0" class="empty-box">
        Bạn chưa có sản phẩm yêu thích nào
      </div>

      <div v-else class="row g-3">
        <div
          v-for="item in store.favorites"
          :key="item.favoriteId"
          class="col-12 col-md-6 col-xl-4"
        >
          <div class="favorite-card">
            <div class="d-flex gap-3">
              <img
                v-if="item.imageUrl"
                :src="item.imageUrl"
                class="product-img"
                alt="product"
              />

              <div v-else class="product-img placeholder-img">
                No Image
              </div>

              <div class="flex-grow-1">
                <div class="fw-bold text-truncate">
                  {{ item.productName }}
                </div>

                <div class="text-muted small">
                  {{ item.brandName }} · {{ item.sku }}
                </div>

                <div class="small mt-1">
                  {{ item.capacityValue || "-" }}ml ·
                  {{ item.bottleTypeName || "-" }}
                </div>

                <div class="fw-bold mt-2">
                  {{ formatMoney(item.price) }}
                </div>

                <div class="small text-muted">
                  Tồn kho: {{ item.stockQuantity }}
                </div>
              </div>
            </div>

            <button
              class="btn btn-outline-danger btn-sm w-100 mt-3"
              @click="store.deleteFavorite(item.favoriteId)"
            >
              Bỏ yêu thích
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useCustomerProfileStore } from "../stores/customerProfile.store";

const store = useCustomerProfileStore();

const formatMoney = (value: number) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};
</script>

<style scoped>
.favorite-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.product-img {
  width: 76px;
  height: 76px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 12px;
}

.empty-box {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 16px;
}
</style>