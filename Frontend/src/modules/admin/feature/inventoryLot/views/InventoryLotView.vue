<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";

import InventoryLotDetailModal from "../components/InventoryLotDetailModal.vue";
import { useInventoryLotStore } from "../stores/inventory-lot.store";
import type {
  InventoryLotExpiryFilter,
  InventoryLotListResponse,
  InventoryLotLockFilter,
  InventoryLotStockFilter,
} from "../types/inventory-lot.type";

const store = useInventoryLotStore();
const router = useRouter();
const detailVisible = ref(false);

const role = computed(() =>
  String(localStorage.getItem("role") || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim()
);

const canManage = computed(() => ["OWNER", "MANAGER"].includes(role.value));

const lockOptions: Array<{
  value: InventoryLotLockFilter;
  label: string;
}> = [
  { value: "", label: "Tất cả trạng thái lô" },
  { value: "LOCKED", label: "Đang khóa" },
  { value: "UNLOCKED", label: "Không khóa" },
];

const expiryOptions: Array<{
  value: InventoryLotExpiryFilter;
  label: string;
}> = [
  { value: "", label: "Tất cả HSD" },
  { value: "NEAR_EXPIRY", label: "Sắp hết hạn" },
  { value: "EXPIRED", label: "Đã hết hạn" },
  { value: "NOT_EXPIRED", label: "Chưa hết hạn" },
];

const stockOptions: Array<{
  value: InventoryLotStockFilter;
  label: string;
}> = [
  { value: "", label: "Tất cả tồn kho" },
  { value: "IN_STOCK", label: "Còn hàng" },
  { value: "OUT_OF_STOCK", label: "Hết hàng" },
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

const formatDate = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`))
    : "—";

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const getErrorMessage = (error: any) =>
  error?.response?.data?.message ||
  error?.response?.data?.detail ||
  store.error ||
  "Đã xảy ra lỗi.";

const setProductVariantId = (event: Event) => {
  const value = (event.target as HTMLInputElement).value.trim();
  store.productVariantId = value === "" ? null : Number(value);
};

const expiryLabel = (item: InventoryLotListResponse) => {
  if (item.isExpired) return "Đã hết hạn";
  if (item.isNearExpiry) return `Sắp hết hạn · ${item.daysToExpiry} ngày`;
  return `Còn ${item.daysToExpiry} ngày`;
};

const expiryClass = (item: InventoryLotListResponse) => {
  if (item.isExpired) return "status-expired";
  if (item.isNearExpiry) return "status-near-expiry";
  return "status-valid";
};

const validateFilters = async () => {
  if (
    store.productVariantId !== null &&
    (!Number.isInteger(Number(store.productVariantId)) ||
      Number(store.productVariantId) <= 0)
  ) {
    await Swal.fire({
      icon: "warning",
      title: "ProductVariantId không hợp lệ",
      text: "ProductVariantId phải là số nguyên lớn hơn 0.",
    });
    return false;
  }

  if (
    store.expirationFrom &&
    store.expirationTo &&
    store.expirationFrom > store.expirationTo
  ) {
    await Swal.fire({
      icon: "warning",
      title: "Khoảng HSD không hợp lệ",
      text: "Từ ngày HSD không được lớn hơn đến ngày HSD.",
    });
    return false;
  }

  return true;
};

const loadList = async () => {
  if (!(await validateFilters())) return;

  try {
    await store.fetchList();
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể tải danh sách lô",
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
  if (page < 0 || page >= totalPages.value || page === currentPage.value) {
    return;
  }

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

const openDetail = async (id: number) => {
  detailVisible.value = true;

  try {
    await store.fetchDetailContext(id);
  } catch (error) {
    detailVisible.value = false;

    await Swal.fire({
      icon: "error",
      title: "Không thể tải chi tiết lô",
      text: getErrorMessage(error),
    });
  }
};

const closeDetail = () => {
  if (store.processing) return;
  detailVisible.value = false;
  store.clearDetailContext();
};

const lockLot = async (id: number) => {
  if (!canManage.value) return;

  const result = await Swal.fire({
    icon: "warning",
    title: "Khóa lô hàng?",
    width: 640,
    html: `
      <div style="text-align:left;">
        <label
          for="lock-reason-select"
          style="
            display:block;
            margin-bottom:8px;
            font-size:16px;
            font-weight:700;
            color:#4b5563;
          "
        >
          Lý do khóa <span style="color:#dc2626;">*</span>
        </label>

        <select
          id="lock-reason-select"
          class="swal2-select"
          style="
            width:100%;
            box-sizing:border-box;
            margin:0 0 16px 0;
            height:50px;
            border:2px solid #374151;
            border-radius:10px;
            font-size:16px;
          "
        >
          <option value="">-- Chọn lý do khóa lô --</option>
          <option value="Hàng có dấu hiệu hư hỏng">
            Hàng có dấu hiệu hư hỏng
          </option>
          <option value="Bao bì bị hư hỏng">
            Bao bì bị hư hỏng
          </option>
          <option value="Nghi ngờ chất lượng sản phẩm">
            Nghi ngờ chất lượng sản phẩm
          </option>
          <option value="Chờ kiểm tra chất lượng">
            Chờ kiểm tra chất lượng
          </option>
          <option value="Sai thông tin lô / HSD">
            Sai thông tin lô / HSD
          </option>
          <option value="Tạm ngưng bán để kiểm tra">
            Tạm ngưng bán để kiểm tra
          </option>
          <option value="__OTHER__">Khác</option>
        </select>

        <label
          for="lock-reason-description"
          style="
            display:block;
            margin-bottom:8px;
            font-size:16px;
            font-weight:700;
            color:#4b5563;
          "
        >
          Mô tả chi tiết
          <span style="font-weight:600;color:#6b7280;">
            (không bắt buộc, trừ khi chọn Khác)
          </span>
        </label>

        <textarea
          id="lock-reason-description"
          class="swal2-textarea"
          maxlength="500"
          placeholder="Có thể nhập thêm ghi chú khóa lô để lưu lịch sử xử lý..."
          style="
            width:100%;
            min-height:130px;
            box-sizing:border-box;
            margin:0;
            padding:14px;
            border:1px solid #cbd5e1;
            border-radius:10px;
            resize:vertical;
            font-size:15px;
          "
        ></textarea>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận khóa",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#6b7280",
    focusConfirm: false,

    preConfirm: () => {
      const select = document.getElementById(
        "lock-reason-select"
      ) as HTMLSelectElement | null;

      const descriptionInput = document.getElementById(
        "lock-reason-description"
      ) as HTMLTextAreaElement | null;

      const selectedReason = String(select?.value || "").trim();
      const description = String(descriptionInput?.value || "").trim();

      if (!selectedReason) {
        Swal.showValidationMessage("Bắt buộc chọn lý do khóa lô.");
        return false;
      }

      if (selectedReason === "__OTHER__" && !description) {
        Swal.showValidationMessage(
          "Bắt buộc nhập mô tả chi tiết khi chọn lý do Khác."
        );
        return false;
      }

      const reason =
        selectedReason === "__OTHER__"
          ? description
          : description
          ? `${selectedReason}: ${description}`
          : selectedReason;

      if (reason.length > 500) {
        Swal.showValidationMessage(
          "Tổng nội dung lý do khóa lô không được vượt quá 500 ký tự."
        );
        return false;
      }

      return reason;
    },
  });

  if (!result.isConfirmed) return;

  try {
    await store.lock(id, {
      reason: String(result.value).trim(),
    });

    await Swal.fire({
      icon: "success",
      title: "Đã khóa lô",
      text: "Lô không còn được phép bán. Tồn vật lý không thay đổi.",
      timer: 1600,
      showConfirmButton: false,
    });
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể khóa lô",
      text: getErrorMessage(error),
    });
  }
};

const unlockLot = async (id: number) => {
  if (!canManage.value) return;

  const result = await Swal.fire({
    icon: "question",
    title: "Mở khóa lô hàng?",
    width: 640,
    html: `
      <div style="text-align:left;">
        <label
          for="unlock-reason-select"
          style="
            display:block;
            margin-bottom:8px;
            font-size:16px;
            font-weight:700;
            color:#4b5563;
          "
        >
          Lý do mở khóa <span style="color:#dc2626;">*</span>
        </label>

        <select
          id="unlock-reason-select"
          class="swal2-select"
          style="
            width:100%;
            box-sizing:border-box;
            margin:0 0 16px 0;
            height:50px;
            border:2px solid #374151;
            border-radius:10px;
            font-size:16px;
          "
        >
          <option value="">-- Chọn lý do mở khóa lô --</option>
          <option value="Đã kiểm tra lại chất lượng">
            Đã kiểm tra lại chất lượng
          </option>
          <option value="Sản phẩm đạt yêu cầu">
            Sản phẩm đạt yêu cầu
          </option>
          <option value="Đã xử lý vấn đề bao bì">
            Đã xử lý vấn đề bao bì
          </option>
          <option value="Đã xác minh lại thông tin lô / HSD">
            Đã xác minh lại thông tin lô / HSD
          </option>
          <option value="Cho phép bán lại">
            Cho phép bán lại
          </option>
          <option value="__OTHER__">Khác</option>
        </select>

        <label
          for="unlock-reason-description"
          style="
            display:block;
            margin-bottom:8px;
            font-size:16px;
            font-weight:700;
            color:#4b5563;
          "
        >
          Mô tả chi tiết
          <span style="font-weight:600;color:#6b7280;">
            (không bắt buộc, trừ khi chọn Khác)
          </span>
        </label>

        <textarea
          id="unlock-reason-description"
          class="swal2-textarea"
          maxlength="500"
          placeholder="Có thể nhập thêm ghi chú mở khóa lô để lưu lịch sử xử lý..."
          style="
            width:100%;
            min-height:130px;
            box-sizing:border-box;
            margin:0;
            padding:14px;
            border:1px solid #cbd5e1;
            border-radius:10px;
            resize:vertical;
            font-size:15px;
          "
        ></textarea>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Xác nhận mở khóa",
    cancelButtonText: "Quay lại",
    confirmButtonColor: "#059669",
    cancelButtonColor: "#6b7280",
    focusConfirm: false,

    preConfirm: () => {
      const select = document.getElementById(
        "unlock-reason-select"
      ) as HTMLSelectElement | null;

      const descriptionInput = document.getElementById(
        "unlock-reason-description"
      ) as HTMLTextAreaElement | null;

      const selectedReason = String(select?.value || "").trim();
      const description = String(descriptionInput?.value || "").trim();

      if (!selectedReason) {
        Swal.showValidationMessage("Bắt buộc chọn lý do mở khóa lô.");
        return false;
      }

      if (selectedReason === "__OTHER__" && !description) {
        Swal.showValidationMessage(
          "Bắt buộc nhập mô tả chi tiết khi chọn lý do Khác."
        );
        return false;
      }

      const reason =
        selectedReason === "__OTHER__"
          ? description
          : description
          ? `${selectedReason}: ${description}`
          : selectedReason;

      if (reason.length > 500) {
        Swal.showValidationMessage(
          "Tổng nội dung lý do mở khóa lô không được vượt quá 500 ký tự."
        );
        return false;
      }

      return reason;
    },
  });

  if (!result.isConfirmed) return;

  try {
    await store.unlock(id, {
      reason: String(result.value).trim(),
    });

    await Swal.fire({
      icon: "success",
      title: "Đã mở khóa lô",
      text: "Lô được phép bán lại nếu chưa hết hạn và vẫn còn tồn.",
      timer: 1600,
      showConfirmButton: false,
    });
  } catch (error) {
    await Swal.fire({
      icon: "error",
      title: "Không thể mở khóa lô",
      text: getErrorMessage(error),
    });
  }
};

const openReceiptList = async (receiptNo: string) => {
  // Module 2 hiện có route danh sách phiếu nhập.
  // Truyền receiptNo qua query để không làm sai route hiện có.
  await router.push({
    name: "AdminGoodsReceipts",
    query: {
      receiptNo,
    },
  });
};

onMounted(loadList);
</script>

<template>
  <div class="inventory-lot-page">
    <div class="page-header">
      <div>
        <h1>Lô hàng</h1>
        <p>Theo dõi tồn theo lô, hạn sử dụng, nguồn nhập và trạng thái khóa.</p>
      </div>
    </div>

    <div class="content-card">
      <div class="filters">
        <div class="search-box">
          <input
            v-model="store.keyword"
            maxlength="100"
            placeholder="Tìm SKU, sản phẩm, mã lô hoặc mã phiếu..."
            @keyup.enter="search"
          />
          <button type="button" @click="search">Tìm kiếm</button>
        </div>

        <select v-model="store.lockFilter" @change="search">
          <option
            v-for="option in lockOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>

        <select v-model="store.expiryFilter" @change="search">
          <option
            v-for="option in expiryOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>

        <select v-model="store.stockFilter" @change="search">
          <option
            v-for="option in stockOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>

        <input v-model="store.expirationFrom" type="date" title="HSD từ ngày" />

        <input v-model="store.expirationTo" type="date" title="HSD đến ngày" />

        <button type="button" class="filter-btn" @click="search">Lọc</button>

        <button type="button" class="reset-btn" @click="resetFilters">
          Đặt lại
        </button>
      </div>

      <div class="summary-line">
        <span>
          Tổng cộng <strong>{{ store.totalElements }}</strong> lô
        </span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã lô</th>
              <th>SKU</th>
              <th>Sản phẩm</th>
              <th>Ngày nhập</th>
              <th>Hạn sử dụng</th>
              <th>Tồn thực tế</th>
              <th>Có thể bán</th>
              <th>Trạng thái HSD</th>
              <th>Trạng thái lô</th>
              <th>Nguồn nhập</th>
              <th>Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="store.loadingList">
              <td colspan="11" class="state-row">Đang tải danh sách lô...</td>
            </tr>

            <tr v-else-if="store.lots.length === 0">
              <td colspan="11" class="state-row">Không có lô hàng phù hợp.</td>
            </tr>

            <template v-else>
              <tr v-for="item in store.lots" :key="item.id">
                <td>
                  <button
                    type="button"
                    class="lot-link"
                    @click="openDetail(item.id)"
                  >
                    {{ item.lotCode }}
                  </button>
                </td>

                <td>
                  <strong>{{ item.sku }}</strong>
                </td>

                <td>{{ item.productName }}</td>

                <td>{{ formatDate(item.receivedDate) }}</td>

                <td>{{ formatDate(item.expirationDate) }}</td>

                <td>
                  <strong>{{ formatNumber(item.quantityOnHand) }}</strong>
                </td>

                <td>
                  <strong
                    :class="{
                      'sellable-zero': item.sellableQuantity === 0,
                      'sellable-positive': item.sellableQuantity > 0,
                    }"
                  >
                    {{ formatNumber(item.sellableQuantity) }}
                  </strong>
                </td>

                <td>
                  <span class="status-badge" :class="expiryClass(item)">
                    {{ expiryLabel(item) }}
                  </span>
                </td>

                <td>
                  <span
                    class="status-badge"
                    :class="item.isLocked ? 'status-locked' : 'status-unlocked'"
                  >
                    {{ item.isLocked ? "Đang khóa" : "Không khóa" }}
                  </span>
                </td>

                <td>
                  <span v-if="item.receiptNo" class="receipt-no">
                    {{ item.receiptNo }}
                  </span>
                  <span v-else>—</span>
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
                      v-if="canManage && !item.isLocked"
                      type="button"
                      class="action-danger"
                      title="Khóa lô"
                      :disabled="store.processing"
                      @click="lockLot(item.id)"
                    >
                      <i class="bi bi-lock"></i>
                    </button>

                    <button
                      v-if="canManage && item.isLocked"
                      type="button"
                      class="action-success"
                      title="Mở khóa lô"
                      :disabled="store.processing"
                      @click="unlockLot(item.id)"
                    >
                      <i class="bi bi-unlock"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <div class="pagination-info">
          Hiển thị
          <strong>{{ pageStart }}-{{ pageEnd }}</strong>
          /
          <strong>{{ store.totalElements }}</strong>
          bản ghi

          <span>
            Trang
            <strong>{{ totalPages > 0 ? currentPage + 1 : 0 }}</strong>
            /
            <strong>{{ totalPages }}</strong>
          </span>
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

    <InventoryLotDetailModal
      :visible="detailVisible"
      :detail="store.detail"
      :source="store.source"
      :history="store.history"
      :loading="store.loadingDetail"
      :loading-source="store.loadingSource"
      :loading-history="store.loadingHistory"
      :processing="store.processing"
      :can-manage="canManage"
      @close="closeDetail"
      @lock="lockLot"
      @unlock="unlockLot"
      @open-receipt="openReceiptList"
    />
  </div>
</template>

<style scoped>
.inventory-lot-page {
  min-height: 100%;
  padding: 24px;
  background: #f7f7f8;
}
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
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
.role-note {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 9px;
  background: #fff;
  color: #4b5563;
  font-size: 13px;
}
.content-card {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}
.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-bottom: 14px;
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
.variant-filter {
  width: 150px;
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
.summary-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 12px;
  color: #6b7280;
  font-size: 13px;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
}
table {
  width: 100%;
  min-width: 1320px;
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
.lot-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #111827;
  font-weight: 700;
  cursor: pointer;
}
.lot-link:hover {
  text-decoration: underline;
}
.receipt-no {
  color: #374151;
  font-weight: 600;
}
.status-badge {
  display: inline-flex;
  white-space: nowrap;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.status-expired {
  background: #fef2f2;
  color: #b91c1c;
}
.status-near-expiry {
  background: #fff7ed;
  color: #c2410c;
}
.status-valid {
  background: #ecfdf5;
  color: #047857;
}
.status-locked {
  background: #fef2f2;
  color: #b91c1c;
}
.status-unlocked {
  background: #f3f4f6;
  color: #4b5563;
}
.sellable-zero {
  color: #b91c1c;
}
.sellable-positive {
  color: #047857;
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
.actions .action-danger {
  color: #b91c1c;
}
.actions .action-success {
  color: #047857;
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
  .inventory-lot-page {
    padding: 14px;
  }
  .page-header {
    flex-direction: column;
  }
  .search-box {
    width: 100%;
    min-width: 100%;
  }
  .summary-line {
    align-items: flex-start;
    flex-direction: column;
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
