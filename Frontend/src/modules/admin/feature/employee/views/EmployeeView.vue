<template>
  <div class="employee-page">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="mb-1 fw-bold">Quản lý nhân viên</h3>
        <p class="text-muted mb-0">
          Owner quản lý tài khoản nhân sự nội bộ: Quản lý và Thu ngân.
        </p>
      </div>

      <button class="btn btn-primary" @click="openCreateModal">
        <i class="bi bi-plus-lg me-1"></i>
        Thêm nhân viên
      </button>
    </div>

    <div class="card border-0 shadow-sm">
      <div class="card-body">
        <EmployeeFilter
          v-model:keyword="searchKeyword"
          v-model:status="searchStatus"
          @search="handleSearch"
          @reset="resetFilter"
        />

        <div v-if="employeeStore.error" class="alert alert-danger">
          {{ employeeStore.error }}
        </div>

        <EmployeeTable
          :employees="employeeStore.employees"
          :loading="employeeStore.loading"
          @edit="openUpdateModal"
          @toggle-status="handleToggleStatus"
          @delete="handleDelete"
        />

        <EmployeePagination
          :page="employeeStore.page ?? 0"
          :total-pages="employeeStore.totalPages ?? 1"
          :total-elements="employeeStore.totalElements ?? 0"
          @change-page="changePage"
        />
      </div>
    </div>

    <EmployeeFormModal
      :show="showModal"
      :employee="selectedEmployee"
      :loading="employeeStore.loading"
      @close="closeModal"
      @submit="submitForm"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import Swal from "sweetalert2";

import EmployeeFilter from "../components/EmployeeFilter.vue";
import EmployeeTable from "../components/EmployeeTable.vue";
import EmployeePagination from "../components/EmployeePagination.vue";
import EmployeeFormModal from "../components/EmployeeFormModal.vue";

import { useEmployeeStore } from "../stores/employee.store";
import type {
  EmployeeCreateRequest,
  EmployeeResponse,
  EmployeeStatus,
  EmployeeUpdateRequest,
} from "../types/employee.type";

const employeeStore = useEmployeeStore();

const showModal = ref(false);
const selectedEmployee = ref<EmployeeResponse | null>(null);

const searchKeyword = ref("");
const searchStatus = ref<EmployeeStatus | null>(null);

const Toast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 2200,
  timerProgressBar: true,
});

onMounted(async () => {
  try {
    await employeeStore.fetchEmployees();
  } catch {
    showError(employeeStore.error || "Không thể tải danh sách nhân viên.");
  }
});

function openCreateModal() {
  selectedEmployee.value = null;
  showModal.value = true;
}

function openUpdateModal(employee: EmployeeResponse) {
  selectedEmployee.value = employee;
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  selectedEmployee.value = null;
}

async function submitForm(data: {
  userId: number | null;
  payload: EmployeeCreateRequest | EmployeeUpdateRequest;
}) {
  try {
    if (data.userId) {
      await employeeStore.updateEmployee(
        data.userId,
        data.payload as EmployeeUpdateRequest
      );

      Toast.fire({
        icon: "success",
        title: "Cập nhật nhân viên thành công",
      });
    } else {
      await employeeStore.createEmployee(data.payload as EmployeeCreateRequest);

      Toast.fire({
        icon: "success",
        title: "Thêm nhân viên thành công",
      });
    }

    closeModal();
  } catch {
    showError(employeeStore.error || "Lưu nhân viên thất bại.");
  }
}

async function handleSearch() {
  try {
    employeeStore.setKeyword(searchKeyword.value);
    employeeStore.setStatus(searchStatus.value);
    await employeeStore.fetchEmployees();
  } catch {
    showError(employeeStore.error || "Tìm kiếm nhân viên thất bại.");
  }
}

async function resetFilter() {
  try {
    searchKeyword.value = "";
    searchStatus.value = null;

    employeeStore.setKeyword("");
    employeeStore.setStatus(null);
    employeeStore.setPage(0);

    await employeeStore.fetchEmployees();
  } catch {
    showError(employeeStore.error || "Không thể reset bộ lọc.");
  }
}

async function changePage(page: number) {
  try {
    employeeStore.setPage(page);
    await employeeStore.fetchEmployees();
  } catch {
    showError(employeeStore.error || "Không thể chuyển trang.");
  }
}

async function handleToggleStatus(employee: EmployeeResponse) {
  const isActive = employee.status === 1;

  const result = await Swal.fire({
    title: isActive ? "Khóa tài khoản nhân viên?" : "Mở khóa tài khoản nhân viên?",
    html: `
      <div class="text-start">
        <p class="mb-2">Nhân viên: <b>${employee.name}</b></p>
        <p class="mb-0">
          ${
            isActive
              ? "Sau khi khóa, nhân viên sẽ không thể đăng nhập vào hệ thống."
              : "Sau khi mở khóa, nhân viên có thể đăng nhập lại hệ thống."
          }
        </p>
      </div>
    `,
    icon: isActive ? "warning" : "question",
    showCancelButton: true,
    confirmButtonText: isActive ? "Khóa tài khoản" : "Mở khóa",
    cancelButtonText: "Hủy",
    confirmButtonColor: isActive ? "#f59e0b" : "#16a34a",
    cancelButtonColor: "#64748b",
  });

  if (!result.isConfirmed) return;

  try {
    if (isActive) {
      await employeeStore.lockEmployee(employee.userId);

      Toast.fire({
        icon: "success",
        title: "Đã khóa tài khoản nhân viên",
      });
    } else {
      await employeeStore.unlockEmployee(employee.userId);

      Toast.fire({
        icon: "success",
        title: "Đã mở khóa tài khoản nhân viên",
      });
    }
  } catch {
    showError(employeeStore.error || "Cập nhật trạng thái thất bại.");
  }
}

async function handleDelete(employee: EmployeeResponse) {
  const result = await Swal.fire({
    title: "Xóa mềm nhân viên?",
    html: `
      <div class="text-start">
        <p class="mb-2">Nhân viên: <b>${employee.name}</b></p>
        <p class="mb-0">
          Tài khoản sẽ bị khóa và ẩn khỏi danh sách. Dữ liệu nhân viên vẫn được giữ để bảo toàn lịch sử đơn hàng.
        </p>
      </div>
    `,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Xóa nhân viên",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#64748b",
  });

  if (!result.isConfirmed) return;

  try {
    await employeeStore.deleteEmployee(employee.userId);

    Toast.fire({
      icon: "success",
      title: "Xóa nhân viên thành công",
    });
  } catch {
    showError(employeeStore.error || "Xóa nhân viên thất bại.");
  }
}

function showError(message: string) {
  Swal.fire({
    icon: "error",
    title: "Có lỗi xảy ra",
    text: message,
    confirmButtonText: "Đóng",
    confirmButtonColor: "#111827",
  });
}
</script>

<style scoped>
.employee-page {
  padding: 24px;
}
</style>