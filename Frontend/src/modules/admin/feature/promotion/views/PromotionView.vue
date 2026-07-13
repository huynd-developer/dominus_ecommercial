<template>
  <div class="container-fluid py-3">
    <div class="d-flex justify-content-between align-items-start mb-3">
      <div>
        <h4 class="fw-bold mb-1">Quản lý Flash Sale</h4>
        <p class="text-muted mb-0">
          CRUD chiến dịch khuyến mãi, chọn biến thể sản phẩm và cấu hình phần trăm giảm giá.
        </p>
      </div>

      <button class="btn btn-dark" @click="openCreateModal">
        + Tạo chiến dịch
      </button>
    </div>

    <div class="card border-0 shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2">
          <div class="col-md-6">
            <input
              v-model.trim="filters.keyword"
              type="text"
              class="form-control"
              placeholder="Tìm theo tên chiến dịch..."
              @keyup.enter="fetchPromotions(0)"
            />
          </div>

          <div class="col-md-3">
            <select
              v-model="filters.status"
              class="form-select"
              @change="fetchPromotions(0)"
            >
              <option :value="null">Tất cả trạng thái</option>
              <option :value="1">Đang bật</option>
              <option :value="0">Đã tắt / đã kết thúc</option>
            </select>
          </div>

          <div class="col-md-3 d-grid">
            <button
              class="btn btn-outline-dark"
              :disabled="store.loading"
              @click="fetchPromotions(0)"
            >
              <span
                v-if="store.loading"
                class="spinner-border spinner-border-sm me-1"
              ></span>
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="card border-0 shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width: 80px">ID</th>
              <th>Chiến dịch</th>
              <th>Bắt đầu</th>
              <th>Kết thúc</th>
              <th>Trạng thái</th>
              <th class="text-end">Số biến thể</th>
              <th class="text-end" style="width: 270px">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="store.loading">
              <td colspan="7" class="text-center py-4">
                <span class="spinner-border spinner-border-sm me-2"></span>
                Đang tải dữ liệu...
              </td>
            </tr>

            <tr v-else-if="store.promotions.length === 0">
              <td colspan="7" class="text-center py-4 text-muted">
                Chưa có chiến dịch khuyến mãi.
              </td>
            </tr>

            <template v-else>
              <tr v-for="promotion in store.promotions" :key="promotion.id">
                <td>#{{ promotion.id }}</td>

                <td>
                  <div class="fw-semibold">{{ promotion.name }}</div>
                  <small class="text-muted">
                    Đang chạy:
                    <span :class="promotion.activeNow ? 'text-success' : 'text-muted'">
                      {{ promotion.activeNow ? "Có" : "Không" }}
                    </span>
                  </small>
                </td>

                <td>{{ formatDateTime(promotion.startDate) }}</td>

                <td>{{ formatDateTime(promotion.endDate) }}</td>

                <td>
                  <span :class="statusBadgeClass(promotion)">
                    {{ promotion.statusText }}
                  </span>
                </td>

                <td class="text-end">
                  {{ promotion.variants?.length || 0 }}
                </td>

                <td class="text-end">
                  <div class="btn-group">
                    <button
                      v-if="promotion.ended"
                      class="btn btn-sm btn-outline-secondary"
                      @click="openEditModal(promotion.id)"
                    >
                      Xem
                    </button>

                    <button
                      v-else
                      class="btn btn-sm btn-outline-primary"
                      @click="openEditModal(promotion.id)"
                    >
                      Sửa
                    </button>

                    <button
                      v-if="promotion.status === 1 && !promotion.ended"
                      class="btn btn-sm btn-outline-warning"
                      @click="changeStatus(promotion.id, 0)"
                    >
                      Tắt
                    </button>

                    <button
                      v-else-if="promotion.status === 0 && !promotion.ended"
                      class="btn btn-sm btn-outline-success"
                      @click="changeStatus(promotion.id, 1)"
                    >
                      Bật
                    </button>

                    <button
                      class="btn btn-sm btn-outline-danger"
                      :disabled="promotion.activeNow"
                      @click="removePromotion(promotion)"
                    >
                      Xóa
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div
        v-if="store.totalPages > 1"
        class="card-footer bg-white d-flex justify-content-between align-items-center"
      >
        <small class="text-muted">
          Tổng {{ store.totalElements }} chiến dịch
        </small>

        <div class="d-flex align-items-center gap-2">
          <button
            class="btn btn-sm btn-outline-secondary"
            :disabled="store.pageNumber <= 0 || store.loading"
            @click="fetchPromotions(store.pageNumber - 1)"
          >
            Trước
          </button>

          <span class="small text-muted">
            Trang {{ store.pageNumber + 1 }} / {{ store.totalPages }}
          </span>

          <button
            class="btn btn-sm btn-outline-secondary"
            :disabled="store.pageNumber + 1 >= store.totalPages || store.loading"
            @click="fetchPromotions(store.pageNumber + 1)"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <PromotionFormModal
      :show="showModal"
      :promotion="editingPromotion"
      @close="closeModal"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
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

const fetchPromotions = async (page = 0) => {
  await store.fetchPromotions({
    keyword: filters.keyword,
    status: filters.status,
    page,
    size: store.pageSize,
  });
};

const openCreateModal = () => {
  editingPromotion.value = null;
  showModal.value = true;
};

const openEditModal = async (id: number) => {
  try {
    const detail = await store.fetchDetail(id);
    editingPromotion.value = detail;
    showModal.value = true;
  } catch (error) {
    console.error("Fetch promotion detail failed:", error);
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
    confirmButtonColor: "#bd9a5f",
  });

  if (!confirm.isConfirmed) return;

  try {
    await store.changeStatus(id, status);
    await fetchPromotions(store.pageNumber);
  } catch (error) {
    console.error("Change promotion status failed:", error);
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
    html: `Chiến dịch <b>${promotion.name}</b> sẽ bị xóa mềm.`,
    showCancelButton: true,
    confirmButtonText: "Xóa",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#dc3545",
  });

  if (!confirm.isConfirmed) return;

  try {
    await store.removePromotion(promotion.id);
    await fetchPromotions(store.pageNumber);
  } catch (error) {
    console.error("Remove promotion failed:", error);
  }
};

const statusBadgeClass = (promotion: PromotionResponse) => {
  if (promotion.ended) {
    return "badge bg-secondary";
  }

  if (promotion.activeNow) {
    return "badge bg-success";
  }

  if (promotion.status === 1) {
    return "badge bg-primary";
  }

  return "badge bg-secondary";
};

const formatDateTime = (value: string) => {
  if (!value) return "";

  return new Date(value).toLocaleString("vi-VN", {
    hour12: false,
  });
};

onMounted(() => {
  fetchPromotions(0);
});
</script>