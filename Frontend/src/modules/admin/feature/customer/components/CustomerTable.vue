<template>
  <div class="table-responsive bg-white rounded-4 shadow-sm border">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th>Mã KH</th>
          <th>Khách hàng</th>
          <th>Liên hệ</th>
          <th>Địa chỉ</th>
          <th>Hạng</th>
          <th>Điểm</th>
          <th>Ngày tạo</th>
          <th>Trạng thái</th>
          <th class="text-end">Thao tác</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="loading">
          <td colspan="9" class="text-center py-5 text-muted">
            <span class="spinner-border spinner-border-sm me-2"></span>
            Đang tải dữ liệu khách hàng...
          </td>
        </tr>

        <tr v-else-if="customers.length === 0">
          <td colspan="9" class="text-center py-5 text-muted">
            Không tìm thấy khách hàng nào
          </td>
        </tr>

        <tr v-for="customer in customers" :key="customer.userId" v-else>
          <td>
            <span class="fw-semibold">#{{ customer.userId }}</span>
          </td>

          <td>
            <div class="d-flex align-items-center gap-3">
              <img
                :src="customer.avatarUrl || defaultAvatar"
                class="customer-avatar"
                alt="avatar"
              />

              <div>
                <div class="fw-semibold">
                  {{ customer.name }}
                </div>
                <div class="small text-muted">
                  {{ customer.genderText || "Chưa cập nhật" }}
                </div>
              </div>
            </div>
          </td>

          <td>
            <div>{{ customer.phone || "Chưa cập nhật" }}</div>
            <div class="small text-muted">{{ customer.email }}</div>
          </td>

          <!-- ĐÃ SỬA GỌI HÀM FORMAT ADDRESS Ở ĐÂY -->
          <td class="text-truncate" style="max-width: 230px" :title="formatAddress(customer.address)">
            {{ formatAddress(customer.address) }}
          </td>

          <td>
            <span class="badge rounded-pill text-bg-warning">
              {{ customer.customerRank || "Bronze" }}
            </span>
          </td>

          <td>
            <span class="fw-semibold">
              {{ customer.loyaltyPoints ?? 0 }}
            </span>
          </td>

          <td>
            {{ formatDate(customer.createdAt) }}
          </td>

          <td>
            <span
              class="badge rounded-pill"
              :class="customer.status === 1 ? 'text-bg-success' : 'text-bg-secondary'"
            >
              {{ customer.statusText || getStatusText(customer.status) }}
            </span>
          </td>

          <td class="text-end">
            <button
              type="button"
              class="btn btn-sm btn-outline-primary me-2"
              @click="$emit('view-detail', customer.userId)"
            >
              Chi tiết
            </button>

            <button
              v-if="canUpdateStatus"
              type="button"
              class="btn btn-sm"
              :class="
                customer.status === 1
                  ? 'btn-outline-danger'
                  : 'btn-outline-success'
              "
              @click="
                $emit(
                  'toggle-status',
                  customer.userId,
                  customer.status === 1 ? 0 : 1
                )
              "
            >
              {{ customer.status === 1 ? "Khóa" : "Mở" }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { CustomerResponse } from "../types/customer.types";

defineProps<{
  customers: CustomerResponse[];
  loading: boolean;
  canUpdateStatus: boolean;
}>();

defineEmits<{
  (e: "view-detail", userId: number): void;
  (e: "toggle-status", userId: number, status: number): void;
}>();

const defaultAvatar =
  "https://ui-avatars.com/api/?name=Customer&background=f8f9fa&color=6c757d";

const formatDate = (value: string | null) => {
  if (!value) {
    return "Chưa cập nhật";
  }

  return new Date(value).toLocaleDateString("vi-VN");
};

const getStatusText = (status: number) => {
  return status === 1 ? "Đang hoạt động" : "Đã khóa";
};

// HÀM FORMAT ĐỊA CHỈ TỪ JSON SANG TEXT
const formatAddress = (addressStr: string | null) => {
  if (!addressStr || addressStr === "Chưa cập nhật") return "Chưa cập nhật";
  
  try {
    if (addressStr.trim().startsWith('[')) {
      const parsedAddresses = JSON.parse(addressStr);
      
      if (Array.isArray(parsedAddresses) && parsedAddresses.length > 0) {
        const defaultAddress = parsedAddresses[0];
        return defaultAddress.fullAddress || defaultAddress.specificAddress || 'Chưa cập nhật';
      }
    }
    return addressStr;
  } catch (error) {
    console.error("Lỗi parse địa chỉ từ JSON:", error);
    return addressStr;
  }
};
</script>

<style scoped>
.customer-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}
</style>