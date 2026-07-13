<template>
  <div v-if="show">
    <div class="modal d-block" tabindex="-1">
      <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header">
            <div>
              <h5 class="modal-title fw-bold mb-1">
                {{
                  isReadonly
                    ? "Xem chiến dịch Flash Sale"
                    : promotion
                      ? "Cập nhật chiến dịch"
                      : "Tạo chiến dịch Flash Sale"
                }}
              </h5>

              <small class="text-muted">
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

          <div class="modal-body">
            <div v-if="isReadonly" class="alert alert-secondary py-2">
              Chiến dịch này đã kết thúc. Theo nghiệp vụ thực tế, không sửa lại
              thời gian, sản phẩm hoặc phần trăm giảm giá để tránh sai lịch sử
              khuyến mãi và báo cáo.
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label fw-semibold">
                  Tên chiến dịch <span class="text-danger">*</span>
                </label>

                <input
                  v-model.trim="form.name"
                  type="text"
                  maxlength="255"
                  class="form-control"
                  placeholder="VD: Flash Sale Cuối Tuần"
                  :disabled="isReadonly"
                />

                <small class="text-muted">
                  Từ 3 đến 255 ký tự.
                </small>
              </div>

              <div class="col-md-3">
                <label class="form-label fw-semibold">
                  Bắt đầu <span class="text-danger">*</span>
                </label>

                <input
                  v-model="form.startDate"
                  type="datetime-local"
                  class="form-control"
                  :disabled="isReadonly"
                />
              </div>

              <div class="col-md-3">
                <label class="form-label fw-semibold">
                  Kết thúc <span class="text-danger">*</span>
                </label>

                <input
                  v-model="form.endDate"
                  type="datetime-local"
                  class="form-control"
                  :disabled="isReadonly"
                />
              </div>
            </div>

            <ProductVariantPicker
              v-model="selectedVariants"
              :start-date="form.startDate"
              :end-date="form.endDate"
              :ignore-promotion-id="promotion?.id ?? null"
              :readonly="isReadonly"
            />
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" @click="close">
              {{ isReadonly ? "Đóng" : "Hủy" }}
            </button>

            <button
              v-if="!isReadonly"
              type="button"
              class="btn btn-dark"
              :disabled="store.saving"
              @click="submit"
            >
              <span
                v-if="store.saving"
                class="spinner-border spinner-border-sm me-1"
              ></span>
              {{ promotion ? "Lưu thay đổi" : "Tạo chiến dịch" }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-backdrop fade show"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
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

const isReadonly = computed(() => {
  return props.promotion?.ended === true;
});

const toDateTimeLocal = (value?: string | null) => {
  if (!value) return "";
  return value.substring(0, 16);
};

const resetForm = () => {
  form.name = props.promotion?.name || "";
  form.startDate = toDateTimeLocal(props.promotion?.startDate);
  form.endDate = toDateTimeLocal(props.promotion?.endDate);

  selectedVariants.value =
    props.promotion?.variants?.map((item) => ({
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

watch(
  () => props.show,
  (value) => {
    if (value) {
      resetForm();
    }
  },
  { immediate: true }
);

watch(
  () => props.promotion,
  () => {
    if (props.show) {
      resetForm();
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
  return {
    name: form.name.trim(),
    startDate: form.startDate,
    endDate: form.endDate,
    variants: selectedVariants.value.map((item) => ({
      productVariantId: item.productVariantId,
      discountPercent: Number(item.discountPercent),
    })),
  };
};

const submit = async () => {
  try {
    const valid = await validateBeforeSubmit();

    if (!valid) return;

    const payload = buildPayload();

    if (props.promotion) {
      await store.updatePromotion(props.promotion.id, payload);
    } else {
      await store.createPromotion(payload);
    }

    emit("saved");
  } catch (error) {
    console.error("Promotion submit failed:", error);
  }
};
</script>