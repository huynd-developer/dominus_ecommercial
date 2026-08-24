<template>
  <div class="p-4 min-vh-100" style="background-color: #f8f9fa;">
    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="fw-bold mb-0 d-flex align-items-center gap-2">
        <i class="bi bi-lightning-charge"></i> Quản lý Flash Sale
      </h4>
      <button @click="openCreateModal" class="btn btn-primary rounded-pill px-4 py-2 shadow-sm d-flex align-items-center gap-2">
        <i class="bi bi-plus-circle"></i> Tạo chiến dịch
      </button>
    </div>

    <!-- Bọc toàn bộ vào Card trắng bo góc như trang Sản phẩm -->
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        
        <!-- Filter & Search Bar kiểu mới -->
        <div class="d-flex justify-content-between align-items-center mb-4 gap-3 flex-wrap">
          <div class="position-relative flex-grow-1" style="max-width: 400px;">
            <i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-muted"></i>
            <input
              v-model.trim="filters.keyword"
              type="text"
              class="form-control rounded-pill ps-5 bg-light border-0"
              placeholder="Tìm theo tên chiến dịch..."
              @keyup.enter="fetchPromotions(0)"
            />
          </div>
          <div class="d-flex align-items-center gap-2">
            <select
              v-model="filters.status"
              class="form-select rounded-pill bg-light border-0 px-4" style="width: 200px;"
              @change="fetchPromotions(0)"
            >
              <option :value="null">Tất cả trạng thái</option>
              <option :value="1">Đang hoạt động</option>
              <option :value="0">Đã kết thúc/Tắt</option>
            </select>
            <button
              class="btn btn-light rounded-circle shadow-sm text-muted"
              :disabled="store.loading"
              @click="fetchPromotions(0)"
              title="Làm mới"
            >
              <i class="bi bi-arrow-clockwise" :class="{'fa-spin': store.loading}"></i>
            </button>
          </div>
        </div>

        <!-- Table Danh Sách -->
        <div v-if="store.loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <div class="table-responsive" v-else>
          <table class="table align-middle table-borderless table-hover custom-table">
            <thead class="text-muted border-bottom">
              <tr>
                <th class="ps-3 fw-medium" style="width: 80px">ID</th>
                <th class="fw-medium">Chiến dịch</th>
                <th class="fw-medium">Bắt đầu</th>
                <th class="fw-medium">Kết thúc</th>
                <th class="fw-medium text-center">Trạng thái</th>
                <th class="fw-medium text-center">Số sản phẩm gốc</th>
                <th class="fw-medium text-end pe-3">Thao tác</th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="groupedPromotions.length === 0">
                <td colspan="7" class="text-center py-5 text-muted">
                  Không tìm thấy chiến dịch khuyến mãi nào.
                </td>
              </tr>

              <template v-else>
                <tr v-for="promotion in groupedPromotions" :key="promotion.id" class="border-bottom">
                  <td class="ps-3 fw-bold text-dark">#{{ promotion.id }}</td>

                  <td>
                    <div class="fw-bold text-dark">{{ promotion.name }}</div>
                    <small class="text-muted">
                      Đang chạy:
                      <span :class="promotion.activeNow ? 'text-success fw-bold' : 'text-muted'">
                        {{ promotion.activeNow ? "Có" : "Không" }}
                      </span>
                    </small>
                  </td>

                  <td class="small text-muted">{{ formatDateTime(promotion.startDate) }}</td>

                  <td class="small text-muted">
                     <span :class="{'text-danger fw-bold': isExpiredStatus(promotion.endDate)}">
                        {{ formatDateTime(promotion.endDate) }}
                     </span>
                  </td>

                  <td class="text-center">
                    <span :class="statusBadgeClass(promotion)" class="badge rounded-pill px-3 py-2">
                      {{ promotion.statusText }}
                    </span>
                  </td>

                  <!-- Hiển thị số lượng sản phẩm gốc sau khi đã gom nhóm các biến thể trùng -->
                  <td class="text-center">
                    <span class="badge bg-light text-dark border">{{ promotion.variants?.length || 0 }}</span>
                  </td>

                  <td class="text-end pe-3">
                    <div class="d-flex gap-1 justify-content-end">
                      <button
                        v-if="promotion.ended"
                        class="btn btn-sm btn-light text-secondary rounded-circle action-btn"
                        @click="openEditModal(promotion.id)" title="Xem chi tiết"
                      >
                        <i class="bi bi-eye"></i>
                      </button>

                      <button
                        v-else
                        class="btn btn-sm btn-light text-primary rounded-circle action-btn"
                        @click="openEditModal(promotion.id)" title="Sửa"
                      >
                        <i class="bi bi-pencil-square"></i>
                      </button>

                      <button
                        v-if="promotion.status === 1 && !promotion.ended"
                        class="btn btn-sm btn-light text-warning rounded-circle action-btn"
                        @click="changeStatus(promotion.id, 0)" title="Tạm dừng"
                      >
                        <i class="bi bi-pause-circle"></i>
                      </button>

                      <button
                        v-else-if="promotion.status === 0 && !promotion.ended"
                        class="btn btn-sm btn-light text-success rounded-circle action-btn"
                        @click="changeStatus(promotion.id, 1)" title="Kích hoạt"
                      >
                        <i class="bi bi-play-circle"></i>
                      </button>

                      <button
                        class="btn btn-sm btn-light text-danger rounded-circle action-btn"
                        :disabled="promotion.activeNow"
                        @click="removePromotion(promotion)" title="Xóa"
                      >
                        <i class="bi bi-trash3"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- Pagination kiểu mới -->
        <div class="d-flex justify-content-between align-items-center mt-4 text-muted small" v-if="store.totalPages > 0">
          <div>
            Hiển thị trang {{ store.pageNumber + 1 }} / {{ store.totalPages }} (Tổng: {{ store.totalElements }})
          </div>
          <div class="d-flex gap-2 align-items-center">
            <button class="btn btn-sm btn-light rounded-circle" :disabled="store.pageNumber <= 0 || store.loading" @click="fetchPromotions(store.pageNumber - 1)">
              <i class="bi bi-arrow-left"></i>
            </button>
            <span class="mx-2">{{ store.pageNumber + 1 }} / {{ store.totalPages }}</span>
            <button class="btn btn-sm btn-light rounded-circle" :disabled="store.pageNumber + 1 >= store.totalPages || store.loading" @click="fetchPromotions(store.pageNumber + 1)">
              <i class="bi bi-arrow-right"></i>
            </button>
          </div>
        </div>

      </div>
    </div>

    <!-- MODAL -->
    <PromotionFormModal
      :show="showModal"
      :promotion="editingPromotion"
      @close="closeModal"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import PromotionFormModal from "../components/PromotionFormModal.vue";
import { usePromotionStore } from "../stores/promotion.store";
import type {
  PromotionResponse,
  PromotionStatus,
} from "../types/promotion.types";

const store = usePromotionStore();

const filters = reactive<{
  keyword: string;
  status: number | null;
}>({
  keyword: "",
  status: null,
});

const showModal = ref(false);
const editingPromotion = ref<PromotionResponse | null>(null);

const isConflictError = (error: any) => Number(error?.response?.status) === 409;

const fetchPromotions = async (page = 0) => {
  await store.fetchPromotions({
    keyword: filters.keyword,
    status: filters.status,
    page,
    size: store.pageSize,
  });
};

// Computed xử lý gom nhóm các biến thể cùng 1 sản phẩm lại thành 1 dòng duy nhất nếu backend trả về phân tách
const groupedPromotions = computed(() => {
  if (!store.promotions || store.promotions.length === 0) return [];

  const map = new Map<number, any>();

  store.promotions.forEach((item: any) => {
    // Dùng ID của chiến dịch hoặc ID sản phẩm để làm chuẩn gộp nhóm
    const key = item.id;

    if (!map.has(key)) {
      map.set(key, {
        ...item,
        variants: item.variants ? [...item.variants] : [],
      });
    } else {
      const existing = map.get(key);
      if (item.variants && Array.isArray(item.variants)) {
        // Gộp các biến thể tránh trùng lặp dựa vào id biến thể
        item.variants.forEach((v: any) => {
          const vId = v.productVariantId || v.id;
          const exists = existing.variants.some(
            (ev: any) => (ev.productVariantId || ev.id) === vId
          );
          if (!exists) {
            existing.variants.push(v);
          }
        });
      }
    }
  });

  return Array.from(map.values());
});

const openCreateModal = () => {
  editingPromotion.value = null;
  showModal.value = true;
};

const openEditModal = async (id: number) => {
  try {
    /* Luôn GET detail mới nhất trước khi edit để lấy đúng revision. */
    const detail = await store.fetchDetail(id);
    editingPromotion.value = detail;
    showModal.value = true;
  } catch (error) {
    console.error("Fetch promotion detail failed:", error);

    await Swal.fire({
      icon: "error",
      title: "Không tải được chi tiết chiến dịch",
      text: "Vui lòng thử lại sau.",
      confirmButtonColor: "#bd9a5f",
    });
  }
};

const closeModal = () => {
  showModal.value = false;
  editingPromotion.value = null;
};

const handleSaved = async () => {
  closeModal();
  await fetchPromotions(store.pageNumber);
};

const getMutationSnapshot = async (
  promotion: PromotionResponse | undefined,
  id: number
): Promise<PromotionResponse> => {
  if (promotion?.revision) {
    return promotion;
  }

  /*
   * BE giữ expectedRevision nullable để compatibility caller cũ,
   * nhưng Admin FE mới không được mutation thiếu revision.
   */
  return store.fetchDetail(id);
};

const handleMutationConflict = async () => {
  await fetchPromotions(store.pageNumber);

  await Swal.fire({
    icon: "warning",
    title: "Chiến dịch đã thay đổi",
    text: "Dữ liệu mới nhất đã được tải lại. Thao tác cũ không được tự động thực hiện lại.",
    confirmButtonColor: "#bd9a5f",
  });
};

const changeStatus = async (id: number, status: PromotionStatus) => {
  const promotion = store.promotions.find((item) => item.id === id);

  if (promotion?.ended) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể đổi trạng thái",
      text: "Chiến dịch đã kết thúc nên không thể bật hoặc tắt lại.",
      confirmButtonColor: "#bd9a5f",
    });

    return;
  }

  const confirm = await Swal.fire({
    icon: "question",
    title: status === 1 ? "Bật chiến dịch?" : "Tắt chiến dịch?",
    text:
      status === 1
        ? "Chiến dịch sẽ được hiển thị nếu đang trong thời gian chạy."
        : "Sản phẩm thuộc chiến dịch sẽ không còn hiện ở Flash Sale.",
    showCancelButton: true,
    confirmButtonText: status === 1 ? "Bật" : "Tắt",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#0d6efd",
  });

  if (!confirm.isConfirmed) return;

  try {
    const snapshot = await getMutationSnapshot(promotion, id);
    await store.changeStatus(id, status, snapshot.revision);
    await fetchPromotions(store.pageNumber);
  } catch (error: any) {
    console.error("Change promotion status failed:", error);

    if (isConflictError(error)) {
      await handleMutationConflict();
    }
  }
};

const removePromotion = async (promotion: PromotionResponse) => {
  if (promotion.activeNow) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể xóa",
      text: "Chiến dịch đang diễn ra. Hãy tắt chiến dịch trước khi xóa.",
      confirmButtonColor: "#bd9a5f",
    });

    return;
  }

  const confirm = await Swal.fire({
    icon: "warning",
    title: "Xóa chiến dịch?",
    html: `Chiến dịch <b>${promotion.name}</b> sẽ bị xóa.`,
    showCancelButton: true,
    confirmButtonText: "Xóa",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#dc3545",
  });

  if (!confirm.isConfirmed) return;

  try {
    const snapshot = await getMutationSnapshot(promotion, promotion.id);
    await store.removePromotion(promotion.id, snapshot.revision);
    await fetchPromotions(store.pageNumber);
  } catch (error: any) {
    console.error("Remove promotion failed:", error);

    if (isConflictError(error)) {
      await handleMutationConflict();
    }
  }
};

const statusBadgeClass = (promotion: PromotionResponse) => {
  if (promotion.ended) {
    return "bg-secondary-subtle text-secondary";
  }
  if (promotion.activeNow) {
    return "bg-success-subtle text-success";
  }
  if (promotion.status === 1) {
    return "bg-primary-subtle text-primary";
  }
  return "bg-secondary-subtle text-secondary";
};

const formatDateTime = (value: string) => {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN", {
    hour12: false,
  });
};

const isExpiredStatus = (endDate: string) => {
  if (!endDate) return false;
  return new Date(endDate).getTime() < new Date().getTime();
};

/*
 * Không-F5: khi quay lại tab/window chỉ refresh LIST.
 * Không tự reload detail đang edit để tránh ghi đè draft của người dùng;
 * nếu detail đã stale thì PUT sẽ bị BE trả 409 và modal tự tải snapshot mới nhất.
 */
let lastAutoRefreshAt = 0;
const handleWindowFocus = () => {
  const now = Date.now();
  if (now - lastAutoRefreshAt < 300) return;
  lastAutoRefreshAt = now;

  void fetchPromotions(store.pageNumber);
};

const handleVisibilityChange = () => {
  if (document.visibilityState === "visible") {
    handleWindowFocus();
  }
};

onMounted(() => {
  fetchPromotions(0);
  window.addEventListener("focus", handleWindowFocus);
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onUnmounted(() => {
  window.removeEventListener("focus", handleWindowFocus);
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});
</script>

<style scoped>
.custom-table th {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 1rem 0.5rem;
}
.custom-table td {
  padding: 1rem 0.5rem;
  vertical-align: middle;
}
.action-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.action-btn:hover {
  transform: translateY(-2px);
  background-color: #e9ecef !important;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.bg-success-subtle {
  background-color: #d1e7dd !important;
}
.text-success {
  color: #198754 !important;
}
.bg-secondary-subtle {
  background-color: #e2e3e5 !important;
}
.text-secondary {
  color: #6c757d !important;
}
.bg-primary-subtle {
  background-color: #cfe2ff !important;
}
.text-primary {
  color: #0d6efd !important;
}
</style>