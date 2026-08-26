<template>
  <Teleport to="body">
    <div v-if="show" class="custom-modal-overlay" @click.self="close">
      <div class="custom-modal-content custom-modal-xl">
        <div class="modal-header">
          <div>
            <h4 class="fw-bold mb-0">
              {{
                isReadonly
                  ? "Xem chiến dịch Flash Sale"
                  : effectivePromotion
                    ? "Cập nhật chiến dịch"
                    : "Tạo chiến dịch Flash Sale"
              }}
            </h4>
            <small class="text-muted mt-1 d-block">
              <template v-if="isReadonly">
                Chiến dịch đã kết thúc nên chỉ được xem, không được chỉnh sửa.
              </template>
              <template v-else>
                Chọn biến thể sản phẩm và cấu hình phần trăm giảm giá.
              </template>
            </small>
          </div>
          <button type="button" class="btn-close" @click="close"></button>
        </div>

        <div class="modal-body p-4">
          <div v-if="isReadonly" class="alert alert-secondary py-2 mb-4">
            Chiến dịch này đã kết thúc. Theo nghiệp vụ thực tế, không sửa lại
            thời gian, sản phẩm hoặc phần trăm giảm giá để tránh sai lịch sử
            khuyến mãi và báo cáo.
          </div>

          <div class="row g-4 mb-4">
            <div class="col-md-6">
              <label class="form-label fw-bold">
                Tên chiến dịch <span class="text-danger">*</span>
              </label>
              <input
                v-model.trim="form.name"
                type="text"
                maxlength="255"
                class="form-control form-control-lg"
                placeholder="VD: Flash Sale Cuối Tuần"
                :disabled="isReadonly"
              />
              <small class="text-muted mt-1 d-block">Từ 3 đến 255 ký tự.</small>
            </div>

            <div class="col-md-3">
              <label class="form-label fw-bold">
                Bắt đầu <span class="text-danger">*</span>
              </label>
              <input
                v-model="form.startDate"
                type="datetime-local"
                class="form-control form-control-lg"
                :disabled="isReadonly"
              />
            </div>

            <div class="col-md-3">
              <label class="form-label fw-bold">
                Kết thúc <span class="text-danger">*</span>
              </label>
              <input
                v-model="form.endDate"
                type="datetime-local"
                class="form-control form-control-lg"
                :disabled="isReadonly"
              />
            </div>
          </div>

          <div v-if="isVerifying" class="text-center py-4 text-muted">
            <span class="spinner-border spinner-border-sm me-2"></span> Đang tải và xác minh dữ liệu sản phẩm...
          </div>
          
          <ProductVariantPicker
            v-else
            v-model="selectedVariants"
            :start-date="form.startDate"
            :end-date="form.endDate"
            :ignore-promotion-id="effectivePromotion?.id ?? null"
            :readonly="isReadonly"
          />
        </div>

        <div class="modal-footer mt-2 pt-4 border-top p-4">
          <button type="button" class="btn btn-light btn-lg px-4" @click="close">
            {{ isReadonly ? "Đóng" : "Hủy" }}
          </button>

          <button
            v-if="!isReadonly"
            type="button"
            class="btn btn-dark btn-lg px-5"
            :disabled="store.saving || isVerifying"
            @click="submit"
          >
            <span v-if="store.saving" class="spinner-border spinner-border-sm me-2"></span>
            {{ effectivePromotion ? "Lưu thay đổi" : "Tạo chiến dịch" }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import api from "@/common/api";
import ProductVariantPicker from "./ProductVariantPicker.vue";
import { usePromotionStore } from "../stores/promotion.store";
import type {
  PromotionRequest,
  PromotionResponse,
  PromotionVariantFormItem,
} from "../types/promotion.types";

const props = defineProps<{
  show: boolean;
  promotion: PromotionResponse | null;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "saved"): void;
}>();

const store = usePromotionStore();

const form = reactive({
  name: "",
  startDate: "",
  endDate: "",
});

const selectedVariants = ref<PromotionVariantFormItem[]>([]);
const isVerifying = ref(false);

const latestPromotion = ref<PromotionResponse | null>(null);
const effectivePromotion = computed(
  () => latestPromotion.value ?? props.promotion
);

const isReadonly = computed(() => {
  return effectivePromotion.value?.ended === true;
});

const isConflictError = (error: any) => Number(error?.response?.status) === 409;

const toDateTimeLocal = (value?: string | null) => {
  if (!value) return "";
  return value.substring(0, 16);
};

const mapSourceVariants = (source: PromotionResponse | null) => {
  form.name = source?.name || "";
  form.startDate = toDateTimeLocal(source?.startDate);
  form.endDate = toDateTimeLocal(source?.endDate);

  return source?.variants?.map((item) => ({
    productVariantId: item.productVariantId,
    discountPercent: item.discountPercent,
    sku: item.sku,
    productName: item.productName,
    capacity: item.capacity,
    bottleType: item.bottleType,
    originalPrice: item.originalPrice,
    price: item.originalPrice,
    salePrice: item.salePrice,
    stockQuantity: item.stockQuantity,
  })) || [];
};

const verifySelectedVariants = async (variants: any[]) => {
  if (!variants || variants.length === 0) {
    selectedVariants.value = [];
    return;
  }
  isVerifying.value = true;
  const valid = [];
  
  for (const item of variants) {
    try {
      const res = await api.get(`/v1/products`, { params: { keyword: item.sku || item.productName || "", size: 50, t: Date.now() } });
      const list = res.data?.data?.content || res.data?.data || res.data?.content || res.data || [];
      
      const exists = list.some((p: any) => {
        if (p.isDeleted || p.deleted || Number(p.status) === 0) return false;
        if (p.variants) {
          return p.variants.some((pv: any) => {
            const vId = Number(pv.productVariantId || pv.variantId || pv.id);
            const targetId = Number((item as any).productVariantId || (item as any).variantId || (item as any).id);
            return vId === targetId && !pv.isDeleted && !pv.deleted && Number(pv.status) !== 0;
          });
        }
        return false;
      });
      
      if (exists) valid.push(item);
    } catch {
      valid.push(item); // Fallback an toàn nếu lỗi mạng
    }
  }
  
  selectedVariants.value = valid;
  isVerifying.value = false;
};

watch(
  () => props.show,
  async (show) => {
    if (show) {
      latestPromotion.value = null;
      const mapped = mapSourceVariants(props.promotion);
      await verifySelectedVariants(mapped);
    } else {
      selectedVariants.value = [];
    }
  }
);

watch(
  () => props.promotion,
  async (promo) => {
    if (props.show) {
      latestPromotion.value = null;
      const mapped = mapSourceVariants(promo);
      await verifySelectedVariants(mapped);
    }
  }
);

const close = () => {
  emit("close");
};

const validateBeforeSubmit = async () => {
  if (isReadonly.value) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể sửa",
      text: "Chiến dịch đã kết thúc nên không được chỉnh sửa.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  const cleanName = form.name.trim();

  if (!cleanName || cleanName.length < 3) {
    await Swal.fire({
      icon: "warning",
      title: "Tên chiến dịch chưa hợp lệ",
      text: "Tên chiến dịch phải từ 3 ký tự trở lên.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  if (cleanName.length > 255) {
    await Swal.fire({
      icon: "warning",
      title: "Tên chiến dịch quá dài",
      text: "Tên chiến dịch không được vượt quá 255 ký tự.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  if (!form.startDate || !form.endDate) {
    await Swal.fire({
      icon: "warning",
      title: "Thiếu thời gian",
      text: "Vui lòng chọn thời gian bắt đầu và kết thúc.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  if (new Date(form.endDate).getTime() <= new Date(form.startDate).getTime()) {
    await Swal.fire({
      icon: "warning",
      title: "Thời gian chưa hợp lệ",
      text: "Ngày kết thúc phải lớn hơn ngày bắt đầu.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  if (new Date(form.endDate).getTime() <= Date.now()) {
    await Swal.fire({
      icon: "warning",
      title: "Thời gian chưa hợp lệ",
      text: "Ngày kết thúc phải lớn hơn thời gian hiện tại.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  if (selectedVariants.value.length === 0) {
    await Swal.fire({
      icon: "warning",
      title: "Chưa chọn sản phẩm",
      text: "Vui lòng chọn ít nhất 1 biến thể sản phẩm.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  if (selectedVariants.value.length > 100) {
    await Swal.fire({
      icon: "warning",
      title: "Chọn quá nhiều biến thể",
      text: "Một chiến dịch chỉ nên áp dụng tối đa 100 biến thể.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  const duplicateIds = selectedVariants.value
    .map((item) => item.productVariantId)
    .filter((id, index, arr) => arr.indexOf(id) !== index);

  if (duplicateIds.length > 0) {
    await Swal.fire({
      icon: "warning",
      title: "Trùng biến thể",
      text: "Một biến thể chỉ được chọn một lần trong cùng chiến dịch.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  const invalidDiscount = selectedVariants.value.find(
    (item) =>
      item.discountPercent == null ||
      Number.isNaN(Number(item.discountPercent)) ||
      Number(item.discountPercent) <= 0 ||
      Number(item.discountPercent) > 99.99
  );

  if (invalidDiscount) {
    await Swal.fire({
      icon: "warning",
      title: "Phần trăm giảm giá chưa hợp lệ",
      text: "Mỗi biến thể phải có % giảm lớn hơn 0 và nhỏ hơn hoặc bằng 99.99.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  const unavailableSelected = selectedVariants.value.find(
    (item) => item.availableForPromotion === false
  );

  if (unavailableSelected) {
    await Swal.fire({
      icon: "warning",
      title: "Có biến thể không đủ điều kiện",
      text:
        unavailableSelected.unavailableReason ||
        "Vui lòng bỏ các biến thể không đủ điều kiện khuyến mãi.",
      confirmButtonColor: "#bd9a5f",
    });
    return false;
  }

  return true;
};

const buildPayload = (): PromotionRequest => {
  const payload: PromotionRequest = {
    name: form.name.trim(),
    startDate: form.startDate,
    endDate: form.endDate,
    variants: selectedVariants.value.map((item) => ({
      productVariantId: item.productVariantId,
      discountPercent: Number(item.discountPercent),
    })),
  };

  /* CREATE không gửi revision; UPDATE luôn gửi revision của snapshot đang sửa. */
  if (effectivePromotion.value?.revision) {
    payload.expectedRevision = effectivePromotion.value.revision;
  }

  return payload;
};

const reloadLatestAfterConflict = async (promotionId: number) => {
  try {
    const latest = await store.fetchDetail(promotionId);
    latestPromotion.value = latest;
    const mapped = mapSourceVariants(latest);
    await verifySelectedVariants(mapped);

    await Swal.fire({
      icon: "warning",
      title: "Chiến dịch đã thay đổi",
      text: "Dữ liệu mới nhất đã được tải vào form. Kiểm tra lại rồi lưu lần nữa nếu vẫn muốn cập nhật.",
      confirmButtonColor: "#bd9a5f",
    });
  } catch (refreshError) {
    console.error("Reload latest promotion after conflict failed:", refreshError);

    await Swal.fire({
      icon: "error",
      title: "Chiến dịch đã thay đổi",
      text: "Không tải được dữ liệu mới nhất. Hãy đóng form và mở lại chiến dịch.",
      confirmButtonColor: "#bd9a5f",
    });
  }
};

const submit = async () => {
  try {
    const valid = await validateBeforeSubmit();
    if (!valid) return;

    const payload = buildPayload();
    const editing = effectivePromotion.value;

    if (editing) {
      await store.updatePromotion(editing.id, payload);
    } else {
      await store.createPromotion(payload);
    }

    emit("saved");
  } catch (error: any) {
    if (isConflictError(error) && effectivePromotion.value?.id) {
      await reloadLatestAfterConflict(effectivePromotion.value.id);
      return;
    }

    console.error("Promotion submit failed:", error);
  }
};
</script>

<style scoped>
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1050;
  backdrop-filter: blur(4px);
}

.custom-modal-content {
  background-color: white;
  width: 90%;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.custom-modal-xl {
  max-width: 1000px;
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-body {
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>