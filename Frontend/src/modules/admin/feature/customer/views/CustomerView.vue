<template>
  <div class="customer-page">
    <div class="d-flex justify-content-between align-items-start mb-4">
      <div>
        <h3 class="fw-bold mb-1">Quản lý khách hàng</h3>
        <p class="text-muted mb-0">
          Xem danh sách, tìm kiếm, theo dõi hạng thành viên và điểm tích lũy.
        </p>
      </div>

      <div class="text-end">
        <div class="small text-muted">Tổng khách hàng</div>
        <div class="fs-4 fw-bold">{{ customerStore.totalElements }}</div>
      </div>
    </div>

    <div class="bg-white rounded-4 shadow-sm border p-3 mb-3">
      <div class="row g-3 align-items-end">
        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Tìm kiếm</label>
          <input
            v-model="customerStore.keyword"
            type="text"
            class="form-control"
            placeholder="Nhập tên, email, số điện thoại hoặc hạng khách hàng..."
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="col-12 col-md-3">
          <label class="form-label fw-semibold">Trạng thái</label>
          <select v-model="customerStore.status" class="form-select">
            <option :value="null">Tất cả</option>
            <option :value="1">Đang hoạt động</option>
            <option :value="0">Đã khóa</option>
          </select>
        </div>

        <div class="col-12 col-md-3 d-flex gap-2">
          <button
            type="button"
            class="btn btn-primary flex-fill"
            :disabled="customerStore.isLoading"
            @click="handleSearch"
          >
            Tìm kiếm
          </button>

          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="customerStore.isLoading"
            @click="handleReset"
          >
            Reset
          </button>
        </div>
      </div>
    </div>

    <CustomerTable
      :customers="customerStore.customers"
      :loading="customerStore.isLoading"
      :can-update-status="canUpdateStatus"
      @view-detail="handleViewDetail"
      @toggle-status="openConfirmStatusModal"
    />

    <div
      v-if="customerStore.totalElements > 0"
      class="pagination-wrapper bg-white rounded-4 shadow-sm border mt-3 p-3"
    >
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
        <div class="text-muted small">
          Hiển thị
          <strong>{{ pageStart }}</strong>
          -
          <strong>{{ pageEnd }}</strong>
          trên tổng
          <strong>{{ customerStore.totalElements }}</strong>
          khách hàng
        </div>

        <div class="d-flex align-items-center gap-2">
          <span class="text-muted small">Số dòng:</span>

          <select
            v-model.number="customerStore.size"
            class="form-select form-select-sm page-size-select"
            :disabled="customerStore.isLoading"
            @change="handleChangeSize"
          >
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </div>
      </div>

      <div
        v-if="customerStore.totalPages > 1"
        class="d-flex flex-wrap justify-content-end align-items-center gap-2 mt-3"
      >
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          :disabled="customerStore.page === 0 || customerStore.isLoading"
          @click="handleChangePage(0)"
        >
          Đầu
        </button>

        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          :disabled="customerStore.page === 0 || customerStore.isLoading"
          @click="handleChangePage(customerStore.page - 1)"
        >
          Trước
        </button>

        <button
          v-for="pageItem in visiblePages"
          :key="pageItem"
          type="button"
          class="btn btn-sm"
          :class="
            pageItem === customerStore.page
              ? 'btn-primary'
              : 'btn-outline-secondary'
          "
          :disabled="customerStore.isLoading"
          @click="handleChangePage(pageItem)"
        >
          {{ pageItem + 1 }}
        </button>

        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          :disabled="
            customerStore.page >= customerStore.totalPages - 1 ||
            customerStore.isLoading
          "
          @click="handleChangePage(customerStore.page + 1)"
        >
          Sau
        </button>

        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          :disabled="
            customerStore.page >= customerStore.totalPages - 1 ||
            customerStore.isLoading
          "
          @click="handleChangePage(customerStore.totalPages - 1)"
        >
          Cuối
        </button>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal d-block" tabindex="-1">
      <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content rounded-4">
          <div class="modal-header">
            <h5 class="modal-title fw-bold">Chi tiết khách hàng</h5>
            <button
              type="button"
              class="btn-close"
              @click="closeDetailModal"
            ></button>
          </div>

          <div class="modal-body">
            <div
              v-if="customerStore.isDetailLoading"
              class="text-center py-5 text-muted"
            >
              <span class="spinner-border spinner-border-sm me-2"></span>
              Đang tải chi tiết khách hàng...
            </div>

            <template v-else-if="selectedCustomer">
              <div class="row g-4">
                <div class="col-md-4 text-center">
                  <img
                    :src="selectedCustomer.avatarUrl || defaultAvatar"
                    class="detail-avatar mb-3"
                    alt="avatar"
                  />

                  <h5 class="fw-bold mb-1">
                    {{ selectedCustomer.name }}
                  </h5>

                  <div class="text-muted">
                    #{{ selectedCustomer.userId }}
                  </div>

                  <div class="mt-3">
                    <span
                      class="badge rounded-pill"
                      :class="
                        selectedCustomer.status === 1
                          ? 'text-bg-success'
                          : 'text-bg-secondary'
                      "
                    >
                      {{
                        selectedCustomer.statusText ||
                        getStatusText(selectedCustomer.status)
                      }}
                    </span>
                  </div>
                </div>

                <div class="col-md-8">
                  <div class="info-grid">
                    <div class="info-item">
                      <div class="info-label">Email</div>
                      <div class="info-value">
                        {{ selectedCustomer.email }}
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Số điện thoại</div>
                      <div class="info-value">
                        {{ selectedCustomer.phone || "Chưa cập nhật" }}
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Địa chỉ</div>
                      <div class="info-value">
                        {{ selectedCustomer.address || "Chưa cập nhật" }}
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Giới tính</div>
                      <div class="info-value">
                        {{ selectedCustomer.genderText || "Chưa cập nhật" }}
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Ngày sinh</div>
                      <div class="info-value">
                        {{ formatDate(selectedCustomer.dateOfBirth) }}
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Hạng khách hàng</div>
                      <div class="info-value">
                        <span class="badge rounded-pill text-bg-warning">
                          {{ selectedCustomer.customerRank || "Bronze" }}
                        </span>
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Điểm tích lũy</div>
                      <div class="info-value fw-bold">
                        {{ selectedCustomer.loyaltyPoints ?? 0 }}
                      </div>
                    </div>

                    <div class="info-item">
                      <div class="info-label">Ngày tạo</div>
                      <div class="info-value">
                        {{ formatDate(selectedCustomer.createdAt) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <div v-else class="text-center py-5 text-muted">
              Không tìm thấy thông tin khách hàng.
            </div>
          </div>

          <div class="modal-footer">
            <button
              v-if="canUpdateSelectedCustomerStatus"
              type="button"
              class="btn"
              :class="selectedCustomerStatusButtonClass"
              :disabled="customerStore.isLoading"
              @click="openConfirmSelectedCustomerStatusModal"
            >
              {{ selectedCustomerStatusButtonText }}
            </button>

            <button
              type="button"
              class="btn btn-secondary"
              @click="closeDetailModal"
            >
              Đóng
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-backdrop show"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import CustomerTable from "../components/CustomerTable.vue";
import { useCustomerStore } from "../stores/customer.store";

const customerStore = useCustomerStore();

const showDetailModal = ref(false);

const defaultAvatar =
  "https://ui-avatars.com/api/?name=Customer&background=f8f9fa&color=6c757d";

const selectedCustomer = computed(() => customerStore.selectedCustomer);

const Toast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 2300,
  timerProgressBar: true,
});

const showSuccess = (message: string) => {
  Toast.fire({
    icon: "success",
    title: message,
  });
};

const showError = (message: string) => {
  Toast.fire({
    icon: "error",
    title: message,
  });
};

const currentRole = computed(() => {
  return (localStorage.getItem("role") || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim();
});

const canUpdateStatus = computed(() => {
  return ["OWNER", "MANAGER"].includes(currentRole.value);
});

const canUpdateSelectedCustomerStatus = computed(() => {
  return canUpdateStatus.value && selectedCustomer.value !== null;
});

const selectedCustomerNextStatus = computed(() => {
  if (!selectedCustomer.value) {
    return 1;
  }

  return selectedCustomer.value.status === 1 ? 0 : 1;
});

const selectedCustomerStatusButtonText = computed(() => {
  if (!selectedCustomer.value) {
    return "Khóa tài khoản";
  }

  return selectedCustomer.value.status === 1
    ? "Khóa tài khoản"
    : "Mở tài khoản";
});

const selectedCustomerStatusButtonClass = computed(() => {
  if (!selectedCustomer.value) {
    return "btn-outline-danger";
  }

  return selectedCustomer.value.status === 1
    ? "btn-outline-danger"
    : "btn-outline-success";
});

const visiblePages = computed(() => {
  const pages: number[] = [];

  const total = customerStore.totalPages;
  const current = customerStore.page;

  const start = Math.max(0, current - 2);
  const end = Math.min(total - 1, current + 2);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return pages;
});

const pageStart = computed(() => {
  if (customerStore.totalElements === 0) {
    return 0;
  }

  return customerStore.page * customerStore.size + 1;
});

const pageEnd = computed(() => {
  const end = (customerStore.page + 1) * customerStore.size;
  return Math.min(end, customerStore.totalElements);
});

onMounted(async () => {
  await customerStore.fetchCustomers();

  if (customerStore.errorMessage) {
    showError(customerStore.errorMessage);
  }
});

const handleSearch = async () => {
  await customerStore.searchCustomers();

  if (customerStore.errorMessage) {
    showError(customerStore.errorMessage);
  }
};

const handleReset = async () => {
  await customerStore.resetFilter();

  if (customerStore.errorMessage) {
    showError(customerStore.errorMessage);
  }
};

const handleChangePage = async (page: number) => {
  if (
    page < 0 ||
    page >= customerStore.totalPages ||
    page === customerStore.page
  ) {
    return;
  }

  await customerStore.changePage(page);

  if (customerStore.errorMessage) {
    showError(customerStore.errorMessage);
  }
};

const handleChangeSize = async () => {
  await customerStore.changeSize(customerStore.size);

  if (customerStore.errorMessage) {
    showError(customerStore.errorMessage);
  }
};

const handleViewDetail = async (userId: number) => {
  showDetailModal.value = true;

  await customerStore.fetchCustomerById(userId);

  if (customerStore.errorMessage) {
    showError(customerStore.errorMessage);
  }
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  customerStore.clearSelectedCustomer();
};

const openConfirmStatusModal = async (userId: number, status: number) => {
  if (!canUpdateStatus.value) {
    showError("Bạn không có quyền khóa hoặc mở tài khoản khách hàng");
    return;
  }

  const actionText = status === 0 ? "khóa" : "mở khóa";

  const result = await Swal.fire({
    title: "Xác nhận thao tác",
    text: `Bạn có chắc chắn muốn ${actionText} tài khoản khách hàng này không?`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Xác nhận",
    cancelButtonText: "Hủy",
    reverseButtons: true,
    confirmButtonColor: status === 0 ? "#dc3545" : "#198754",
    cancelButtonColor: "#6c757d",
  });

  if (!result.isConfirmed) {
    return;
  }

  try {
    await customerStore.updateStatus(userId, status);

    if (status === 0) {
      showSuccess("Khóa tài khoản khách hàng thành công");
    } else {
      showSuccess("Mở khóa tài khoản khách hàng thành công");
    }
  } catch {
    showError(
      customerStore.errorMessage || "Thao tác thất bại, vui lòng thử lại"
    );
  }
};

const openConfirmSelectedCustomerStatusModal = async () => {
  if (!selectedCustomer.value) {
    showError("Không tìm thấy thông tin khách hàng cần thao tác");
    return;
  }

  await openConfirmStatusModal(
    selectedCustomer.value.userId,
    selectedCustomerNextStatus.value
  );
};

const formatDate = (value: string | null) => {
  if (!value) {
    return "Chưa cập nhật";
  }

  return new Date(value).toLocaleDateString("vi-VN");
};

const getStatusText = (status: number) => {
  return status === 1 ? "Đang hoạt động" : "Đã khóa";
};
</script>

<style scoped>
.customer-page {
  padding: 24px;
}

.pagination-wrapper {
  font-size: 14px;
}

.page-size-select {
  width: 76px;
}

.detail-avatar {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.info-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8f9fa;
}

.info-label {
  font-size: 13px;
  color: #6c757d;
  margin-bottom: 4px;
}

.info-value {
  font-weight: 500;
  word-break: break-word;
}

.modal {
  background: rgba(0, 0, 0, 0.05);
}

@media (max-width: 768px) {
  .customer-page {
    padding: 16px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>