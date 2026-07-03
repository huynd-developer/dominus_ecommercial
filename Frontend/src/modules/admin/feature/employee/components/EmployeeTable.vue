<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle">
      <thead class="table-light">
        <tr>
          <th>Mã NV</th>
          <th>Nhân viên</th>
          <th>Liên hệ</th>
          <th>Địa chỉ</th>
          <th>Vai trò</th>
          <th>Chức vụ</th>
          <th>Lương</th>
          <th>Ngày vào làm</th>
          <th>Trạng thái</th>
          <th class="text-end">Thao tác</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="loading">
          <td colspan="10" class="text-center py-4">
            Đang tải dữ liệu...
          </td>
        </tr>

        <tr v-else-if="employees.length === 0">
          <td colspan="10" class="text-center text-muted py-4">
            Không có nhân viên nào.
          </td>
        </tr>

        <tr v-for="employee in employees" :key="employee.userId">
          <td>
            <span class="fw-semibold">{{ employee.employeeCode }}</span>
          </td>

          <td>
            <div class="fw-semibold">{{ employee.name }}</div>
            <small class="text-muted">CCCD: {{ employee.citizenId }}</small>
          </td>

          <td>
            <div>{{ employee.email }}</div>
            <small class="text-muted">{{ employee.phone }}</small>
          </td>

          <td>
            <div
              v-if="employee.address"
              class="address-text"
              :title="employee.address"
            >
              {{ employee.address }}
            </div>

            <span v-else class="text-muted small">
              Chưa cập nhật
            </span>
          </td>

          <td>
            <span class="role-badge" :class="getRoleBadgeClass(employee.roleName)">
              {{ formatRole(employee.roleName) }}
            </span>
          </td>

          <td>{{ employee.jobTitle }}</td>

          <td>{{ formatCurrency(employee.salary) }}</td>

          <td>{{ formatDate(employee.hireDate) }}</td>

          <td>
            <div class="d-flex align-items-center gap-2">
              <div class="form-check form-switch m-0">
                <input
                  class="form-check-input cursor-pointer"
                  type="checkbox"
                  role="switch"
                  :checked="employee.status === 1"
                  :disabled="loading || isOwnerRole(employee.roleName)"
                  @click.prevent="handleToggleStatus(employee)"
                />
              </div>

              <span
                class="status-badge"
                :class="employee.status === 1 ? 'status-active' : 'status-locked'"
              >
                {{ employee.status === 1 ? "Hoạt động" : "Đã khóa" }}
              </span>
            </div>

            <small v-if="isOwnerRole(employee.roleName)" class="text-muted d-block mt-1">
              Tài khoản chủ hệ thống
            </small>
          </td>

          <td class="text-end">
            <div v-if="!isOwnerRole(employee.roleName)" class="btn-group">
              <button
                class="btn btn-sm btn-outline-primary"
                :disabled="loading"
                @click="$emit('edit', employee)"
              >
                Sửa
              </button>

              <button
                class="btn btn-sm btn-outline-danger"
                :disabled="loading"
                @click="$emit('delete', employee)"
              >
                Xóa
              </button>
            </div>

            <span v-else class="text-muted small">
              Không thao tác
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { EmployeeResponse } from "../types/employee.type";

defineProps<{
  employees: EmployeeResponse[];
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "edit", employee: EmployeeResponse): void;
  (e: "toggle-status", employee: EmployeeResponse): void;
  (e: "delete", employee: EmployeeResponse): void;
}>();

function normalizeRole(role: string) {
  return (role || "").toUpperCase().replace("ROLE_", "").trim();
}

function isOwnerRole(role: string) {
  return normalizeRole(role) === "OWNER";
}

function formatRole(role: string) {
  const cleanRole = normalizeRole(role);

  if (cleanRole === "OWNER") return "Chủ";
  if (cleanRole === "MANAGER") return "Quản lý";
  if (cleanRole === "CASHIER") return "Thu ngân";
  if (cleanRole === "USER") return "Khách hàng";

  return role;
}

function getRoleBadgeClass(role: string) {
  const cleanRole = normalizeRole(role);

  if (cleanRole === "OWNER") return "role-owner";
  if (cleanRole === "MANAGER") return "role-manager";
  if (cleanRole === "CASHIER") return "role-cashier";
  if (cleanRole === "USER") return "role-user";

  return "role-default";
}

function handleToggleStatus(employee: EmployeeResponse) {
  if (isOwnerRole(employee.roleName)) {
    return;
  }

  emit("toggle-status", employee);
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value || 0);
}

function formatDate(value: string) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("vi-VN");
}
</script>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}

.address-text {
  max-width: 230px;
  color: #334155;
  font-size: 0.85rem;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 82px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.role-owner {
  color: #7c2d12;
  background: #ffedd5;
  border: 1px solid #fdba74;
}

.role-manager {
  color: #1e3a8a;
  background: #dbeafe;
  border: 1px solid #93c5fd;
}

.role-cashier {
  color: #065f46;
  background: #d1fae5;
  border: 1px solid #6ee7b7;
}

.role-user {
  color: #581c87;
  background: #f3e8ff;
  border: 1px solid #d8b4fe;
}

.role-default {
  color: #374151;
  background: #f3f4f6;
  border: 1px solid #d1d5db;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 86px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.status-active {
  color: #ffffff;
  background: #16834a;
}

.status-locked {
  color: #ffffff;
  background: #6b7280;
}

.form-check-input:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
</style>