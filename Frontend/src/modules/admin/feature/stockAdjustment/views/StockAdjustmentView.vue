<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Swal from "sweetalert2";
import { useAuthStore } from "@/modules/auth/stores/authStore";

import StockAdjustmentFormModal from "../components/StockAdjustmentFormModal.vue";
import StockAdjustmentDetailModal from "../components/StockAdjustmentDetailModal.vue";
import { useStockAdjustmentStore } from "../stores/stock-adjustment.store";

import type {
  StockAdjustmentDetailResponse,
  StockAdjustmentListResponse,
  StockAdjustmentSaveRequest,
  StockAdjustmentStatus,
} from "../types/stock-adjustment.type";

const store = useStockAdjustmentStore();
const authStore = useAuthStore();

const formVisible = ref(false);
const detailVisible = ref(false);
const editingDetail = ref<StockAdjustmentDetailResponse | null>(null);

const userRole = computed(() =>
  (authStore.role || localStorage.getItem("role") || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim()
);

const currentUserId = computed(() => {
  const auth = authStore as any;

  const authId =
    auth?.id ??
    auth?.userId ??
    auth?.employeeId ??
    auth?.user?.id ??
    auth?.user?.userId ??
    auth?.user?.employeeId ??
    auth?.currentUser?.id ??
    auth?.currentUser?.userId ??
    auth?.currentUser?.employeeId;

  const parsedAuthId = Number(authId);
  if (Number.isInteger(parsedAuthId) && parsedAuthId > 0) {
    return parsedAuthId;
  }

  try {
    const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");
    const localId = Number(
      currentUser?.id ??
        currentUser?.userId ??
        currentUser?.employeeId ??
        0
    );

    return Number.isInteger(localId) && localId > 0 ? localId : 0;
  } catch {
    return 0;
  }
});

const currentUserName = computed(() => {
  const auth = authStore as any;

  let localUser: any = {};
  try {
    localUser = JSON.parse(localStorage.getItem("currentUser") || "{}");
  } catch {
    localUser = {};
  }

  return String(
    auth?.name ??
      auth?.user?.name ??
      auth?.currentUser?.name ??
      localUser?.name ??
      localUser?.fullName ??
      localUser?.username ??
      ""
  ).trim();
});

const normalizeUserName = (value?: string | null) =>
  String(value ?? "")
    .trim()
    .toLocaleLowerCase("vi-VN");

const isOwner = computed(() => userRole.value === "OWNER");

const canCreate = computed(() =>
  ["OWNER", "MANAGER", "CASHIER"].includes(userRole.value)
);

const isOwnAdjustment = (item: StockAdjustmentListResponse) => {
  const createdById = Number(item.createdById);

  if (
    currentUserId.value > 0 &&
    Number.isInteger(createdById) &&
    createdById > 0
  ) {
    return createdById === currentUserId.value;
  }

  const loggedInName = normalizeUserName(currentUserName.value);
  const createdByName = normalizeUserName(item.createdByName);

  return loggedInName !== "" && loggedInName === createdByName;
};

const canEditOrSubmit = (item: StockAdjustmentListResponse) =>
  isOwner.value ||
  (["MANAGER", "CASHIER"].includes(userRole.value) &&
    isOwnAdjustment(item));

const canReview = (item: StockAdjustmentListResponse) =>
  isOwner.value ||
  (userRole.value === "MANAGER" && !isOwnAdjustment(item));

const statusOptions: Array<{
  value: StockAdjustmentStatus | "";
  label: string;
}> = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "DRAFT", label: "Lưu tạm" },
  { value: "PENDING_APPROVAL", label: "Chờ duyệt" },
  { value: "APPROVED", label: "Đã phê duyệt" },
  { value: "REJECTED", label: "Đã từ chối" },
  { value: "CANCELLED", label: "Đã hủy" },
];

const totalPages = computed(() => {
  const value = Number(store.totalPages);
  return Number.isFinite(value) && value > 0 ? value : 0;
});

const currentPage = computed(() => {
  const value = Number(store.page);
  return Number.isFinite(value) && value >= 0 ? value : 0;
});

const pageNumbers = computed(() => {
  const total = totalPages.value;
  if (total <= 0) return [];

  const maxVisible = 5;
  let start = Math.max(0, currentPage.value - Math.floor(maxVisible / 2));
  let end = Math.min(total - 1, start + maxVisible - 1);
  start = Math.max(0, end - maxVisible + 1);

  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});

const pageStart = computed(() =>
  store.totalElements <= 0 ? 0 : currentPage.value * store.size + 1
);

const pageEnd = computed(() =>
  store.totalElements <= 0
    ? 0
    : Math.min((currentPage.value + 1) * store.size, store.totalElements)
);

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const statusClass = (status: StockAdjustmentStatus) =>
  `status-${status.toLowerCase().replace("_approval", "")}`;

const getErrorMessage = (error: any) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  store.error ||
  "Đã xảy ra lỗi.";

const ensureCanCreate = async () => {
  if (canCreate.value) return true;

  await Swal.fire({
    icon: "error",
    title: "Không có quyền",
    text: "Bạn không có quyền tạo phiếu kiểm kê thực tế.",
  });

  return false;
};

const ensureCanEditOrSubmit = async (item: StockAdjustmentListResponse) => {
  if (canEditOrSubmit(item)) return true;

  await Swal.fire({
    icon: "error",
    title: "Không có quyền",
    text:
      userRole.value === "OWNER"
        ? "Không thể thao tác phiếu này."
        : "Bạn chỉ được sửa hoặc gửi duyệt phiếu Lưu tạm do chính mình tạo.",
  });

  return false;
};

const ensureCanReview = async (item: StockAdjustmentListResponse) => {
  if (canReview(item)) return true;

  await Swal.fire({
    icon: "error",
    title: "Không có quyền",
    text:
      userRole.value === "MANAGER"
        ? "Quản lý không được tự phê duyệt hoặc từ chối phiếu do chính mình tạo."
        : "Chỉ chủ hệ thống hoặc quản lý được phê duyệt / từ chối phiếu kiểm kê.",
  });

  return false;
};

const validateDateFilter = async () => {
  if (store.fromDate && store.toDate && store.fromDate > store.toDate) {
    await Swal.fire({
      icon: "warning",
      title: "Khoảng ngày không hợp lệ",
      text: "Từ ngày không được lớn hơn đến ngày.",
    });
    return false;
  }

  return true;
};

const loadList = async () => {
  if (!(await validateDateFilter())) return;

  try {
    await store.fetchList();
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể tải danh sách kiểm kê",
      text: getErrorMessage(error),
    });
  }
};

const search = async () => {
  store.page = 0;
  await loadList();
};

const resetFilters = async () => {
  store.resetFilters();
  await loadList();
};

const goToPage = async (page: number) => {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return;
  store.page = page;
  await loadList();
};

const previousPage = async () => {
  if (currentPage.value <= 0) return;
  store.page--;
  await loadList();
};

const nextPage = async () => {
  if (currentPage.value + 1 >= totalPages.value) return;
  store.page++;
  await loadList();
};

const openCreate = async () => {
  if (!(await ensureCanCreate())) return;

  editingDetail.value = null;
  formVisible.value = true;
};

const openEdit = async (item: StockAdjustmentListResponse) => {
  if (item.status !== "DRAFT") return;
  if (!(await ensureCanEditOrSubmit(item))) return;

  try {
    editingDetail.value = await store.fetchDetail(item.id);
    formVisible.value = true;
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể mở phiếu",
      text: getErrorMessage(error),
    });
  }
};

const closeForm = () => {
  if (store.saving) return;
  formVisible.value = false;
  editingDetail.value = null;
};

const saveForm = async (payload: StockAdjustmentSaveRequest) => {
  if (editingDetail.value?.id) {
    const permissionItem = editingDetail.value as StockAdjustmentListResponse;
    if (!(await ensureCanEditOrSubmit(permissionItem))) return;
  } else if (!(await ensureCanCreate())) {
    return;
  }

  try {
    if (editingDetail.value?.id) {
      await store.update(editingDetail.value.id, payload);

      await Swal.fire({
        icon: "success",
        title: "Cập nhật thành công",
        text: "Phiếu vẫn ở trạng thái Lưu tạm, tồn kho chưa thay đổi.",
        timer: 1600,
        showConfirmButton: false,
      });
    } else {
      const created = await store.create(payload);

      await Swal.fire({
        icon: "success",
        title: "Tạo phiếu thành công",
        text: `${created.adjustmentNo} đã được lưu tạm, tồn kho chưa thay đổi.`,
        timer: 1900,
        showConfirmButton: false,
      });
    }

    closeForm();
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể lưu phiếu",
      text: getErrorMessage(error),
    });
  }
};

const openDetail = async (id: number) => {
  detailVisible.value = true;
  store.clearDetail();

  try {
    await store.fetchDetail(id);
  } catch (error) {
    detailVisible.value = false;
    await Swal.fire({
      icon: "error",
      title: "Không thể tải chi tiết",
      text: getErrorMessage(error),
    });
  }
};

const closeDetail = () => {
  if (store.processing) return;
  detailVisible.value = false;
  store.clearDetail();
};

const submitAdjustment = async (item: StockAdjustmentListResponse) => {
  if (!(await ensureCanEditOrSubmit(item))) return;

  const result = await Swal.fire({
    icon: "question",
    title: "Gửi phiếu đi duyệt?",
    html: `<div style="text-align:left"><p><strong>${item.adjustmentNo}</strong></p><p>Sau khi gửi, phiếu chuyển sang Chờ duyệt và không thể sửa.</p><p><strong>Tồn kho chưa thay đổi ở bước này.</strong></p></div>`,
    showCancelButton: true,
    confirmButtonText: "Gửi duyệt",
    cancelButtonText: "Đóng",
  });

  if (!result.isConfirmed) return;

  try {
    await store.submit(item.id);

    await Swal.fire({
      icon: "success",
      title: "Đã gửi duyệt",
      text: "Phiếu đang chờ duyệt, tồn kho chưa thay đổi.",
      timer: 1700,
      showConfirmButton: false,
    });
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể gửi duyệt",
      text: getErrorMessage(error),
    });
  }
};

const cancelAdjustment = async (item: StockAdjustmentListResponse) => {
  if (item.status !== "DRAFT") return;
  if (!(await ensureCanEditOrSubmit(item))) return;

  const result = await Swal.fire({
    icon: "warning",
    title: "Hủy phiếu kiểm kê",
    html: `
      <div style="text-align: left;">
        <p style="margin:0 0 14px 0;">
          <strong>${item.adjustmentNo}</strong>
        </p>

        <label
          for="cancel-reason-select"
          style="display:block; margin-bottom:6px; font-weight:600;"
        >
          Lý do hủy <span style="color:#dc2626;">*</span>
        </label>

        <select
          id="cancel-reason-select"
          class="swal2-select"
          style="width:100%; margin:0 0 14px 0;"
        >
          <option value="">-- Chọn lý do hủy --</option>
          <option value="Tạo nhầm phiếu">Tạo nhầm phiếu</option>
          <option value="Sai lô kiểm kê">Sai lô kiểm kê</option>
          <option value="Sai số lượng kiểm kê">Sai số lượng kiểm kê</option>
          <option value="Không cần kiểm kê nữa">Không cần kiểm kê nữa</option>
          <option value="Sẽ tạo lại phiếu">Sẽ tạo lại phiếu</option>
          <option value="Khác">Khác</option>
        </select>

        <label
          for="cancel-reason-note"
          style="display:block; margin-bottom:6px; font-weight:600;"
        >
          Ghi chú chi tiết
        </label>

        <textarea
          id="cancel-reason-note"
          class="swal2-textarea"
          maxlength="450"
          placeholder="Nhập thêm chi tiết nếu cần..."
          style="width:100%; margin:0; box-sizing:border-box;"
        ></textarea>

        <div
          style="margin-top:8px; color:#6b7280; font-size:12px;"
        >
          Chọn lý do có sẵn thì không cần nhập ghi chú. Nếu chọn "Khác", bắt buộc nhập nội dung cụ thể.
        </div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Hủy phiếu",
    cancelButtonText: "Đóng",
    focusConfirm: false,
    preConfirm: () => {
      const reasonSelect = document.getElementById(
        "cancel-reason-select"
      ) as HTMLSelectElement | null;

      const noteInput = document.getElementById(
        "cancel-reason-note"
      ) as HTMLTextAreaElement | null;

      const selectedReason = String(reasonSelect?.value || "").trim();
      const note = String(noteInput?.value || "").trim();

      if (!selectedReason) {
        Swal.showValidationMessage("Vui lòng chọn lý do hủy.");
        return false;
      }

      if (selectedReason === "Khác" && !note) {
        Swal.showValidationMessage(
          'Vui lòng nhập lý do cụ thể khi chọn "Khác".'
        );
        return false;
      }

      const reason = note ? `${selectedReason} - ${note}` : selectedReason;

      if (reason.length > 500) {
        Swal.showValidationMessage(
          "Lý do hủy không được vượt quá 500 ký tự."
        );
        return false;
      }

      return reason;
    },
  });

  if (!result.isConfirmed) return;

  try {
    await store.cancel(item.id, {
      reason: String(result.value).trim(),
    });

    await Swal.fire({
      icon: "success",
      title: "Đã hủy phiếu",
      text: "Phiếu Lưu tạm đã được hủy. Tồn kho không thay đổi.",
      timer: 1700,
      showConfirmButton: false,
    });
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể hủy phiếu",
      text: getErrorMessage(error),
    });
  }
};

const approveAdjustment = async (item: StockAdjustmentListResponse) => {
  if (!(await ensureCanReview(item))) return;

  const result = await Swal.fire({
    icon: "warning",
    title: "Phê duyệt phiếu kiểm kê?",
    html: `<div style="text-align:left"><p><strong>${item.adjustmentNo}</strong></p><p>Lô chênh lệch: <strong>${formatNumber(item.mismatchLots)}</strong></p><p>Tổng tăng: <strong>+${formatNumber(item.totalIncrease)}</strong> · Tổng giảm: <strong>-${formatNumber(item.totalDecrease)}</strong></p><p>Sau khi duyệt, hệ thống sẽ ghi nhận điều chỉnh tồn tương ứng.</p></div>`,
    showCancelButton: true,
    confirmButtonText: "Phê duyệt",
    cancelButtonText: "Đóng",
  });

  if (!result.isConfirmed) return;

  try {
    await store.approve(item.id);

    await Swal.fire({
      icon: "success",
      title: "Phê duyệt thành công",
      text: "Điều chỉnh tồn đã được ghi nhận vào lịch sử kho.",
      timer: 1800,
      showConfirmButton: false,
    });
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể phê duyệt",
      text: getErrorMessage(error),
    });
  }
};

const rejectAdjustment = async (item: StockAdjustmentListResponse) => {
  if (!(await ensureCanReview(item))) return;

  const result = await Swal.fire({
    icon: "warning",
    title: "Từ chối phiếu kiểm kê",
    html: `
      <div style="text-align: left;">
        <label
          for="reject-reason-select"
          style="display:block; margin-bottom:6px; font-weight:600;"
        >
          Lý do từ chối <span style="color:#dc2626;">*</span>
        </label>

        <select
          id="reject-reason-select"
          class="swal2-select"
          style="width:100%; margin:0 0 14px 0;"
        >
          <option value="">-- Chọn lý do từ chối --</option>
          <option value="Sai số lượng kiểm kê">Sai số lượng kiểm kê</option>
          <option value="Sai lô hàng">Sai lô hàng</option>
          <option value="Chênh lệch chưa hợp lý">Chênh lệch chưa hợp lý</option>
          <option value="Lý do điều chỉnh không phù hợp">Lý do điều chỉnh không phù hợp</option>
          <option value="Cần kiểm đếm lại">Cần kiểm đếm lại</option>
          <option value="Thiếu thông tin">Thiếu thông tin</option>
          <option value="Khác">Khác</option>
        </select>

        <label
          for="reject-reason-note"
          style="display:block; margin-bottom:6px; font-weight:600;"
        >
          Ghi chú chi tiết
        </label>

        <textarea
          id="reject-reason-note"
          class="swal2-textarea"
          maxlength="450"
          placeholder="Nhập thêm chi tiết nếu cần..."
          style="width:100%; margin:0; box-sizing:border-box;"
        ></textarea>

        <div
          style="margin-top:8px; color:#6b7280; font-size:12px;"
        >
          Nếu chọn "Khác", bắt buộc nhập nội dung cụ thể.
        </div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Từ chối phiếu",
    cancelButtonText: "Đóng",
    focusConfirm: false,
    preConfirm: () => {
      const reasonSelect = document.getElementById(
        "reject-reason-select"
      ) as HTMLSelectElement | null;

      const noteInput = document.getElementById(
        "reject-reason-note"
      ) as HTMLTextAreaElement | null;

      const selectedReason = String(reasonSelect?.value || "").trim();
      const note = String(noteInput?.value || "").trim();

      if (!selectedReason) {
        Swal.showValidationMessage("Vui lòng chọn lý do từ chối.");
        return false;
      }

      if (selectedReason === "Khác" && !note) {
        Swal.showValidationMessage(
          'Vui lòng nhập lý do cụ thể khi chọn "Khác".'
        );
        return false;
      }

      const reason = note ? `${selectedReason} - ${note}` : selectedReason;

      if (reason.length > 500) {
        Swal.showValidationMessage(
          "Lý do từ chối không được vượt quá 500 ký tự."
        );
        return false;
      }

      return reason;
    },
  });

  if (!result.isConfirmed) return;

  try {
    await store.reject(item.id, {
      reason: String(result.value).trim(),
    });

    await Swal.fire({
      icon: "success",
      title: "Đã từ chối phiếu",
      text: "Tồn kho không thay đổi.",
      timer: 1500,
      showConfirmButton: false,
    });
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể từ chối",
      text: getErrorMessage(error),
    });
  }
};

onMounted(async () => {
  await Promise.allSettled([loadList(), store.fetchPendingCount()]);
});
</script>

<template>
  <div class="stock-adjustment-page">
    <div class="page-header">
      <div>
        <div class="title-row">
          <h1>Kiểm kê thực tế</h1>
          <span v-if="store.pendingCount > 0" class="pending-badge">
            {{ store.pendingCount }} chờ duyệt
          </span>
        </div>
        <p>
          Đối chiếu tồn hệ thống với số lượng kiểm đếm theo từng lô và chỉ điều
          chỉnh kho sau khi phiếu được phê duyệt.
        </p>
      </div>

      <button v-if="canCreate" class="create-btn" type="button" @click="openCreate">
        <i class="bi bi-plus-lg"></i>
        Tạo phiếu kiểm kê
      </button>
    </div>

    <div v-if="userRole === 'CASHIER'" class="permission-note">
      <i class="bi bi-info-circle"></i>
      Thu ngân được tạo, sửa và gửi duyệt phiếu Lưu tạm do chính mình tạo.
      Phê duyệt / từ chối do quản lý khác người tạo hoặc chủ hệ thống thực hiện.
    </div>

    <div v-else-if="userRole === 'MANAGER'" class="permission-note">
      <i class="bi bi-info-circle"></i>
      Quản lý được lập và gửi phiếu của mình; được xử lý phiếu do người khác
      tạo nhưng không được tự duyệt phiếu của chính mình.
    </div>

    <div class="filter-card">
      <div class="filters">
        <div class="field keyword-field">
          <label>Tìm kiếm</label>
          <input
            v-model="store.keyword"
            placeholder="Mã phiếu, người tạo, ghi chú..."
            @keyup.enter="search"
          />
        </div>

        <div class="field">
          <label>Trạng thái</label>
          <select v-model="store.status" @change="search">
            <option
              v-for="option in statusOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </div>

        <div class="field">
          <label>Từ ngày</label>
          <input v-model="store.fromDate" type="date" @change="search" />
        </div>

        <div class="field">
          <label>Đến ngày</label>
          <input v-model="store.toDate" type="date" @change="search" />
        </div>

        <div class="filter-actions">
          <button type="button" class="filter-btn" @click="search">
            <i class="bi bi-search"></i> Tìm
          </button>
          <button type="button" class="reset-btn" @click="resetFilters">
            Đặt lại
          </button>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="table-head">
        <div>
          <h3>Danh sách phiếu kiểm kê</h3>
          <p>
            {{ formatNumber(store.totalElements) }} phiếu
          </p>
        </div>

        <div class="size-control">
          <span>Hiển thị</span>
          <select
            v-model.number="store.size"
            @change="
              store.page = 0;
              loadList();
            "
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã phiếu</th>
              <th>Trạng thái</th>
              <th>Tổng lô</th>
              <th>Khớp</th>
              <th>Chênh lệch</th>
              <th>Tổng tăng</th>
              <th>Tổng giảm</th>
              <th>Người tạo</th>
              <th>Ngày tạo</th>
              <th>Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="store.loadingList">
              <td colspan="10" class="state-row">Đang tải dữ liệu...</td>
            </tr>

            <tr v-else-if="store.adjustments.length === 0">
              <td colspan="10" class="state-row">
                <i class="bi bi-inbox"></i>
                Không có phiếu phù hợp.
              </td>
            </tr>

            <tr
              v-for="item in store.adjustments"
              v-else
              :key="item.id"
            >
              <td>
                <button
                  class="code-link"
                  type="button"
                  @click="openDetail(item.id)"
                >
                  {{ item.adjustmentNo }}
                </button>
              </td>

              <td>
                <span class="status-badge" :class="statusClass(item.status)">
                  {{ item.statusLabel }}
                </span>
              </td>

              <td>{{ formatNumber(item.totalLots) }}</td>
              <td>{{ formatNumber(item.matchedLots) }}</td>
              <td>
                <strong :class="item.mismatchLots > 0 ? 'has-diff' : ''">
                  {{ formatNumber(item.mismatchLots) }}
                </strong>
              </td>
              <td class="text-up">+{{ formatNumber(item.totalIncrease) }}</td>
              <td class="text-down">-{{ formatNumber(item.totalDecrease) }}</td>
              <td>{{ item.createdByName || "—" }}</td>
              <td>{{ formatDateTime(item.createdAt) }}</td>

              <td>
                <div class="actions">
                  <button
                    type="button"
                    title="Xem chi tiết"
                    @click="openDetail(item.id)"
                  >
                    <i class="bi bi-eye"></i>
                  </button>

                  <button
                    v-if="canEditOrSubmit(item) && item.status === 'DRAFT'"
                    type="button"
                    title="Sửa phiếu"
                    @click="openEdit(item)"
                  >
                    <i class="bi bi-pencil"></i>
                  </button>

                  <button
                    v-if="canEditOrSubmit(item) && item.status === 'DRAFT'"
                    type="button"
                    class="action-submit"
                    title="Gửi duyệt"
                    :disabled="store.processing"
                    @click="submitAdjustment(item)"
                  >
                    <i class="bi bi-send"></i>
                  </button>

                  <button
                    v-if="canEditOrSubmit(item) && item.status === 'DRAFT'"
                    type="button"
                    class="action-cancel"
                    title="Hủy phiếu"
                    :disabled="store.processing"
                    @click="cancelAdjustment(item)"
                  >
                    <i class="bi bi-ban"></i>
                  </button>

                  <button
                    v-if="canReview(item) && item.status === 'PENDING_APPROVAL'"
                    type="button"
                    class="action-approve"
                    title="Phê duyệt"
                    :disabled="store.processing"
                    @click="approveAdjustment(item)"
                  >
                    <i class="bi bi-check-lg"></i>
                  </button>

                  <button
                    v-if="canReview(item) && item.status === 'PENDING_APPROVAL'"
                    type="button"
                    class="action-reject"
                    title="Từ chối"
                    :disabled="store.processing"
                    @click="rejectAdjustment(item)"
                  >
                    <i class="bi bi-x-lg"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <div class="pagination-info">
          Hiển thị <strong>{{ pageStart }}-{{ pageEnd }}</strong> /
          <strong>{{ store.totalElements }}</strong> bản ghi
        </div>

        <div class="page-controls">
          <button
            type="button"
            :disabled="currentPage <= 0 || store.loadingList"
            @click="previousPage"
          >
            Trước
          </button>

          <button
            v-for="page in pageNumbers"
            :key="page"
            type="button"
            class="page-number"
            :class="{ active: page === currentPage }"
            :disabled="page === currentPage"
            @click="goToPage(page)"
          >
            {{ page + 1 }}
          </button>

          <button
            type="button"
            :disabled="
              totalPages === 0 ||
              currentPage + 1 >= totalPages ||
              store.loadingList
            "
            @click="nextPage"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <StockAdjustmentFormModal
      :visible="formVisible"
      :detail="editingDetail"
      :saving="store.saving"
      @close="closeForm"
      @save="saveForm"
    />

    <StockAdjustmentDetailModal
      :visible="detailVisible"
      :detail="store.detail"
      :loading="store.loadingDetail"
      @close="closeDetail"
    />
  </div>
</template>

<style scoped>
.stock-adjustment-page {
  min-height: 100%;
  padding: 24px;
  background: #f7f7f8;
}

.page-header,
.title-row,
.table-head,
.size-control,
.pagination,
.page-controls,
.actions,
.filters,
.filter-actions {
  display: flex;
  align-items: center;
}

.page-header,
.table-head,
.pagination {
  justify-content: space-between;
}

.page-header {
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 16px;
}

.title-row {
  gap: 10px;
}

.page-header h1 {
  margin: 0;
  font-size: 26px;
}

.page-header p,
.table-head p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.pending-badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}

.create-btn,
.filter-btn {
  border: 0;
  background: #111827;
  color: #fff;
}

.create-btn,
.filter-btn,
.reset-btn {
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
}

.permission-note {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1e3a8a;
  font-size: 13px;
}

.filter-card,
.content-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.filter-card {
  margin-bottom: 16px;
  padding: 14px;
}

.filters {
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
}

.field {
  display: flex;
  min-width: 160px;
  flex-direction: column;
  gap: 5px;
}

.keyword-field {
  min-width: 260px;
  flex: 1;
}

.field label {
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.field input,
.field select,
.size-control select {
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
}

.filter-actions {
  gap: 8px;
}

.reset-btn {
  border: 1px solid #d1d5db;
  background: #fff;
}

.content-card {
  padding: 16px;
}

.table-head {
  margin-bottom: 12px;
}

.table-head h3 {
  margin: 0;
  font-size: 17px;
}

.size-control {
  gap: 7px;
  color: #6b7280;
  font-size: 13px;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

table {
  width: 100%;
  min-width: 1160px;
  border-collapse: collapse;
}

th,
td {
  padding: 11px 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
  vertical-align: middle;
  font-size: 13px;
}

th {
  background: #f9fafb;
  color: #4b5563;
}

.code-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #111827;
  font-weight: 700;
  cursor: pointer;
}

.code-link:hover {
  text-decoration: underline;
}

.status-badge {
  display: inline-flex;
  white-space: nowrap;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-draft {
  background: #f3f4f6;
  color: #4b5563;
}

.status-pending {
  background: #fff7ed;
  color: #c2410c;
}

.status-approved {
  background: #ecfdf5;
  color: #047857;
}

.status-rejected {
  background: #fef2f2;
  color: #b91c1c;
}

.status-cancelled {
  background: #f3f4f6;
  color: #6b7280;
}

.text-up {
  color: #047857;
}

.text-down,
.has-diff {
  color: #b91c1c;
}

.actions {
  gap: 5px;
}

.actions button {
  width: 34px;
  height: 34px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #374151;
  cursor: pointer;
}

.actions .action-submit {
  color: #1d4ed8;
}

.actions .action-approve {
  color: #047857;
}

.actions .action-cancel {
  color: #b45309;
}

.actions .action-reject {
  color: #b91c1c;
}

.actions button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.state-row {
  padding: 28px;
  color: #6b7280;
  text-align: center;
}

.pagination {
  gap: 16px;
  margin-top: 16px;
  color: #6b7280;
  font-size: 13px;
}

.page-controls {
  gap: 5px;
}

.page-controls button {
  min-width: 34px;
  height: 34px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
}

.page-controls button.active {
  border-color: #111827;
  background: #111827;
  color: #fff;
}

.page-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .filters {
    align-items: stretch;
  }

  .field,
  .keyword-field {
    min-width: 100%;
  }

  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}

:global(.swal2-container) {
  z-index: 1000000 !important;
}
</style>
