<script setup lang="ts">
import { useRoute } from "vue-router";
import { computed, onMounted, ref } from "vue";
import Swal from "sweetalert2";
import { useAuthStore } from "@/modules/auth/stores/authStore";
import { useGoodsReceiptStore } from "../stores/goods-receipt.store";
import GoodsReceiptFormModal from "../components/GoodsReceiptFormModal.vue";
import GoodsReceiptDetailModal from "../components/GoodsReceiptDetailModal.vue";
import type {
  GoodsReceiptDetailResponse,
  GoodsReceiptListResponse,
  GoodsReceiptSaveRequest,
  GoodsReceiptStatus,
} from "../types/goods-receipt.type";
const route = useRoute();
const store = useGoodsReceiptStore();
const authStore = useAuthStore();

const userRole = computed(() =>
  (authStore.role || localStorage.getItem("role") || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim()
);

const canApproveOrReject = computed(() =>
  ["OWNER", "MANAGER"].includes(userRole.value)
);

const formVisible = ref(false);
const detailVisible = ref(false);
const editingDetail = ref<GoodsReceiptDetailResponse | null>(null);

const statusOptions: Array<{ value: GoodsReceiptStatus | ""; label: string }> =
  [
    { value: "", label: "Tất cả trạng thái" },
    { value: "DRAFT", label: "Lưu tạm" },
    { value: "PENDING_APPROVAL", label: "Chờ duyệt" },
    { value: "APPROVED", label: "Đã duyệt" },
    { value: "REJECTED", label: "Từ chối" },
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

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const statusClass = (status: GoodsReceiptStatus) => {
  switch (status) {
    case "DRAFT":
      return "status-draft";
    case "PENDING_APPROVAL":
      return "status-pending";
    case "APPROVED":
      return "status-approved";
    case "REJECTED":
      return "status-rejected";
    case "CANCELLED":
      return "status-cancelled";
  }
};

const getErrorMessage = (error: any) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  store.error ||
  "Đã xảy ra lỗi.";

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
      title: "Không thể tải phiếu nhập",
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
  if (page < 0 || page >= totalPages.value || page === currentPage.value)
    return;
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

const openCreate = () => {
  editingDetail.value = null;
  formVisible.value = true;
};

const openEdit = async (item: GoodsReceiptListResponse) => {
  if (item.status !== "DRAFT") return;
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

const saveForm = async (payload: GoodsReceiptSaveRequest) => {
  try {
    if (editingDetail.value?.id) {
      await store.update(editingDetail.value.id, payload);
      await Swal.fire({
        icon: "success",
        title: "Cập nhật thành công",
        timer: 1400,
        showConfirmButton: false,
      });
    } else {
      const created = await store.create(payload);
      await Swal.fire({
        icon: "success",
        title: "Tạo phiếu thành công",
        text: `${created.receiptNo} đã được lưu tạm.`,
        timer: 1600,
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
  try {
    await Promise.all([store.fetchDetail(id), store.fetchHistory(id)]);
  } catch (error) {
    detailVisible.value = false;
    await Swal.fire({
      icon: "error",
      title: "Không thể tải chi tiết",
      text: getErrorMessage(error),
    });
  }
};

const submitReceipt = async (item: GoodsReceiptListResponse) => {
  const result = await Swal.fire({
    icon: "question",
    title: "Gửi phiếu đi duyệt?",
    text: `${item.receiptNo} sẽ chuyển sang Chờ duyệt và không thể sửa.`,
    showCancelButton: true,
    confirmButtonText: "Gửi duyệt",
    cancelButtonText: "Hủy",
  });
  if (!result.isConfirmed) return;

  try {
    await store.submit(item.id);
    await Swal.fire({
      icon: "success",
      title: "Đã gửi duyệt",
      timer: 1300,
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

const approveReceipt = async (item: GoodsReceiptListResponse) => {
  const result = await Swal.fire({
    icon: "warning",
    title: "Phê duyệt phiếu nhập?",
    html: `<div style="text-align:left"><p><strong>${item.receiptNo}</strong></p><p>Sau khi duyệt, hệ thống sẽ tạo lô và tăng tồn kho.</p></div>`,
    showCancelButton: true,
    confirmButtonText: "Phê duyệt",
    cancelButtonText: "Hủy",
  });
  if (!result.isConfirmed) return;

  try {
    await store.approve(item.id);
    await Swal.fire({
      icon: "success",
      title: "Phê duyệt thành công",
      text: "Lô kho và biến động nhập kho đã được tạo.",
      timer: 1700,
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

const rejectReceipt = async (item: GoodsReceiptListResponse) => {
  const result = await Swal.fire({
    icon: "warning",
    title: "Từ chối phiếu nhập",
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
          <option value="Sai thông tin sản phẩm">Sai thông tin sản phẩm</option>
          <option value="Sai số lượng">Sai số lượng</option>
          <option value="Sai mã lô / HSD">Sai mã lô / HSD</option>
          <option value="Sai đơn giá">Sai đơn giá</option>
          <option value="Thiếu thông tin">Thiếu thông tin</option>
          <option value="Chứng từ không hợp lệ">Chứng từ không hợp lệ</option>
          <option value="Hàng hóa không đạt yêu cầu">Hàng hóa không đạt yêu cầu</option>
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
    cancelButtonText: "Hủy",
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
      timer: 1300,
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

const cancelReceipt = async (item: GoodsReceiptListResponse) => {
  const result = await Swal.fire({
    icon: "question",
    title: "Hủy phiếu lưu tạm?",
    html: `
      <div style="text-align: left;">
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
          <option value="Sai sản phẩm / SKU">Sai sản phẩm / SKU</option>
          <option value="Sai số lượng">Sai số lượng</option>
          <option value="Sai mã lô / HSD">Sai mã lô / HSD</option>
          <option value="Sai đơn giá">Sai đơn giá</option>
          <option value="Nhà cung cấp hủy giao hàng">Nhà cung cấp hủy giao hàng</option>
          <option value="Hàng chưa thực tế nhập kho">Hàng chưa thực tế nhập kho</option>
          <option value="Khác">Khác</option>
        </select>

        <label
          for="cancel-reason-note"
          style="display:block; margin-bottom:6px; font-weight:600;"
        >
          Ghi chú thêm
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
          Nếu chọn "Khác", bắt buộc nhập nội dung cụ thể.
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
        Swal.showValidationMessage("Lý do hủy không được vượt quá 500 ký tự.");
        return false;
      }

      return reason;
    },
  });

  if (!result.isConfirmed) return;

  try {
    await store.cancel(item.id, {
      reason: String(result.value || "").trim(),
    });

    await Swal.fire({
      icon: "success",
      title: "Đã hủy phiếu",
      timer: 1300,
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

onMounted(async () => {
  await Promise.all([loadList(), store.fetchPendingCount()]);
});
const receiptNoFromLot = String(route.query.receiptNo || "").trim();
if (receiptNoFromLot) {
  store.keyword = receiptNoFromLot;
  store.page = 0;
}
</script>

<template>
  <div class="goods-receipt-page">
    <div class="page-header">
      <div>
        <div class="title-row">
          <h1>Phiếu nhập kho</h1>
          <span v-if="store.pendingCount > 0" class="pending-badge"
            >{{ store.pendingCount }} chờ duyệt</span
          >
        </div>
        <p>Tạo phiếu, lưu tạm, gửi duyệt và phê duyệt nhập kho theo lô.</p>
      </div>
      <button type="button" class="create-btn" @click="openCreate">
        <i class="bi bi-plus-lg"></i> Tạo phiếu nhập
      </button>
    </div>

    <div class="content-card">
      <div class="filters">
        <div class="search-box">
          <input
            v-model="store.keyword"
            maxlength="100"
            placeholder="Tìm mã phiếu hoặc người tạo..."
            @keyup.enter="search"
          />
          <button type="button" @click="search">Tìm kiếm</button>
        </div>
        <select v-model="store.status" @change="search">
          <option
            v-for="option in statusOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
        <input v-model="store.fromDate" type="date" title="Từ ngày" />
        <input v-model="store.toDate" type="date" title="Đến ngày" />
        <button type="button" class="filter-btn" @click="search">Lọc</button>
        <button type="button" class="reset-btn" @click="resetFilters">
          Xóa lọc
        </button>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã phiếu</th>
              <th>Trạng thái</th>
              <th>SKU</th>
              <th>Tổng SL</th>
              <th>Người tạo</th>
              <th>Ngày tạo</th>
              <th>Gửi duyệt</th>
              <th>Xử lý</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="store.loadingList">
              <td colspan="9" class="state-row">Đang tải dữ liệu...</td>
            </tr>
            <tr v-else-if="store.receipts.length === 0">
              <td colspan="9" class="state-row">
                Không có phiếu nhập phù hợp.
              </td>
            </tr>
            <tr v-for="item in store.receipts" v-else :key="item.id">
              <td>
                <button
                  type="button"
                  class="receipt-link"
                  @click="openDetail(item.id)"
                >
                  {{ item.receiptNo }}
                </button>
              </td>
              <td>
                <span class="status-badge" :class="statusClass(item.status)">{{
                  item.statusLabel
                }}</span>
              </td>
              <td>{{ item.totalSku }}</td>
              <td>{{ item.totalQuantity }}</td>
              <td>{{ item.createdByName }}</td>
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ formatDateTime(item.submittedAt) }}</td>
              <td>
                {{
                  item.approvedAt
                    ? `Duyệt ${formatDateTime(item.approvedAt)}`
                    : item.rejectedAt
                    ? `Từ chối ${formatDateTime(item.rejectedAt)}`
                    : item.cancelledAt
                    ? `Hủy ${formatDateTime(item.cancelledAt)}`
                    : "—"
                }}
              </td>
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
                    v-if="item.status === 'DRAFT'"
                    type="button"
                    title="Sửa phiếu"
                    @click="openEdit(item)"
                  >
                    <i class="bi bi-pencil"></i>
                  </button>
                  <button
                    v-if="item.status === 'DRAFT'"
                    type="button"
                    class="action-submit"
                    title="Gửi duyệt"
                    :disabled="store.processing"
                    @click="submitReceipt(item)"
                  >
                    <i class="bi bi-send"></i>
                  </button>
                  <button
                    v-if="item.status === 'DRAFT'"
                    type="button"
                    class="action-danger"
                    title="Hủy phiếu"
                    :disabled="store.processing"
                    @click="cancelReceipt(item)"
                  >
                    <i class="bi bi-x-circle"></i>
                  </button>
                  <button
                    v-if="
                      item.status === 'PENDING_APPROVAL' && canApproveOrReject
                    "
                    type="button"
                    class="action-approve"
                    title="Phê duyệt"
                    :disabled="store.processing"
                    @click="approveReceipt(item)"
                  >
                    <i class="bi bi-check-lg"></i>
                  </button>
                  <button
                    v-if="
                      item.status === 'PENDING_APPROVAL' && canApproveOrReject
                    "
                    type="button"
                    class="action-danger"
                    title="Từ chối"
                    :disabled="store.processing"
                    @click="rejectReceipt(item)"
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
          <span
            >Trang <strong>{{ totalPages > 0 ? currentPage + 1 : 0 }}</strong> /
            <strong>{{ totalPages }}</strong></span
          >
        </div>
        <div class="page-controls">
          <button
            type="button"
            :disabled="currentPage <= 0"
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
            :disabled="totalPages === 0 || currentPage + 1 >= totalPages"
            @click="nextPage"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <GoodsReceiptFormModal
      :visible="formVisible"
      :detail="editingDetail"
      :saving="store.saving"
      @close="closeForm"
      @save="saveForm"
    />
    <GoodsReceiptDetailModal
      :visible="detailVisible"
      :detail="store.detail"
      :history="store.history"
      :loading="store.loadingDetail"
      :loading-history="store.loadingHistory"
      @close="detailVisible = false"
    />
  </div>
</template>

<style scoped>
.goods-receipt-page {
  min-height: 100%;
  padding: 24px;
  background: #f7f7f8;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-header h1 {
  margin: 0 0 6px;
  color: #222;
  font-size: 26px;
}
.page-header p {
  margin: 0;
  color: #777;
}
.pending-badge {
  padding: 4px 9px;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}
.create-btn {
  padding: 11px 16px;
  border: 0;
  border-radius: 8px;
  background: #222;
  color: #fff;
  cursor: pointer;
}
.content-card {
  overflow: hidden;
  padding: 18px;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  background: #fff;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.search-box {
  display: flex;
  flex: 1;
  min-width: 320px;
}
.search-box input,
.filters select,
.filters > input {
  box-sizing: border-box;
  height: 40px;
  padding: 0 11px;
  border: 1px solid #ddd;
  background: #fff;
  outline: none;
}
.search-box input {
  flex: 1;
  min-width: 0;
  border-radius: 8px 0 0 8px;
}
.search-box button {
  padding: 0 15px;
  border: 0;
  border-radius: 0 8px 8px 0;
  background: #222;
  color: #fff;
  cursor: pointer;
}
.filters select,
.filters > input,
.filter-btn,
.reset-btn {
  border-radius: 8px;
}
.filter-btn,
.reset-btn {
  height: 40px;
  padding: 0 14px;
  cursor: pointer;
}
.filter-btn {
  border: 0;
  background: #222;
  color: #fff;
}
.reset-btn {
  border: 1px solid #ddd;
  background: #fff;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
}
table {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
}
th,
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
  vertical-align: middle;
  font-size: 13px;
}
th {
  background: #f9fafb;
  color: #4b5563;
}
.receipt-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #111827;
  font-weight: 700;
  cursor: pointer;
}
.receipt-link:hover {
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
  color: #374151;
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
.actions {
  display: flex;
  align-items: center;
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
  color: #2563eb;
}
.actions .action-approve {
  color: #047857;
}
.actions .action-danger {
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}
.pagination-info {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}
.page-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}
.page-controls button {
  min-width: 38px;
  height: 38px;
  padding: 0 11px;
  border: 1px solid #ddd;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
}
.page-controls .page-number.active {
  border-color: #222;
  background: #222;
  color: #fff;
  opacity: 1;
}
.page-controls button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.page-controls .page-number.active:disabled {
  opacity: 1;
}
@media (max-width: 800px) {
  .goods-receipt-page {
    padding: 14px;
  }
  .page-header {
    flex-direction: column;
  }
  .search-box {
    width: 100%;
    min-width: 100%;
  }
  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }
  .page-controls {
    max-width: 100%;
    overflow-x: auto;
  }
}
</style>
