<template>
  <div
      class="modal fade"
      ref="modalRef"
      tabindex="-1">

    <div class="modal-dialog modal-lg">

      <div class="modal-content">

        <!-- Header -->

        <div class="modal-header bg-primary text-white">

          <h5 class="modal-title">

            {{ form.id ? "Cập nhật biến thể" : "Thêm biến thể" }}

          </h5>

          <button
              class="btn-close btn-close-white"
              data-bs-dismiss="modal">
          </button>

        </div>

        <!-- Body -->

        <div class="modal-body">

          <div class="row">

            <!-- Capacity -->

            <div class="col-md-6 mb-3">

              <label class="form-label">

                Dung tích

              </label>

              <select
                  v-model="form.capacityId"
                  class="form-select">

                <option
                    :value="null">

                  Chọn dung tích

                </option>

                <option
                    v-for="item in capacities"
                    :key="item.id"
                    :value="item.id">

                  {{ item.value }} ml

                </option>

              </select>

            </div>

            <!-- Bottle -->

            <div class="col-md-6 mb-3">

              <label class="form-label">

                Loại chai

              </label>

              <select
                  v-model="form.bottleTypeId"
                  class="form-select">

                <option
                    :value="null">

                  Chọn loại chai

                </option>

                <option
                    v-for="item in bottleTypes"
                    :key="item.id"
                    :value="item.id">

                  {{ item.name }}

                </option>

              </select>

            </div>

            <!-- SKU -->

            <div class="col-md-6 mb-3">

              <label class="form-label">

                SKU

              </label>

              <input

                  class="form-control"

                  v-model="form.sku"

                  placeholder="VD: CHANEL100EDP">

            </div>

            <!-- Price -->

            <div class="col-md-6 mb-3">

              <label class="form-label">

                Giá bán

              </label>

              <input

                  type="number"

                  class="form-control"

                  v-model.number="form.price">

            </div>

            <!-- Stock -->

            <div class="col-md-6 mb-3">

              <label class="form-label">

                Tồn kho

              </label>

              <input

                  type="number"

                  class="form-control"

                  v-model.number="form.stockQuantity">

            </div>

            <!-- Status -->

            <div class="col-md-6 mb-3">

              <label class="form-label">

                Trạng thái

              </label>

              <select

                  class="form-select"

                  v-model="form.status">

                <option :value="1">

                  Hoạt động

                </option>

                <option :value="0">

                  Ngừng

                </option>

              </select>

            </div>

          </div>

        </div>

        <!-- Footer -->

        <div class="modal-footer">

          <button

              class="btn btn-secondary"

              data-bs-dismiss="modal">

            Hủy

          </button>

          <button

              class="btn btn-primary"

              @click="save">

            Lưu

          </button>

        </div>

      </div>

    </div>

  </div>
</template>

<script setup lang="ts">

import {
    ref,
    reactive,
    onMounted,
    watch,
    nextTick
} from "vue";

import { Modal } from "bootstrap";

import CapacityService from "../../capacity/services/capacityService";
import BottleTypeService from "../../bottleType/services/bottleTypeService";

import type { ProductVariant } from "../types/ProductVariant";

/* =========================
   EMIT
========================= */

const emit = defineEmits<{
    (e: "saved", value: ProductVariant): void;
}>();

/* =========================
   PROPS
========================= */

const props = defineProps<{

    variant?: ProductVariant | null;

}>();

/* =========================
   BOOTSTRAP MODAL
========================= */

const modalRef = ref<HTMLDivElement>();

let modal: Modal;

/* =========================
   DROPDOWN
========================= */

const capacities = ref<any[]>([]);

const bottleTypes = ref<any[]>([]);

/* =========================
   FORM
========================= */

const form = reactive<Partial<ProductVariant>>({

    id: undefined,

    productId: undefined,

    capacityId: undefined,

    bottleTypeId: undefined,

    sku: "",

    price: 0,

    stockQuantity: 0,

    status: 1

});

/* =========================
   LOAD DROPDOWN
========================= */

async function loadDropdown() {

    try {

        const capacityRes = await CapacityService.getAll();

        capacities.value = capacityRes.content ?? capacityRes;

        const bottleRes = await BottleTypeService.getAll();

        bottleTypes.value = bottleRes.content ?? bottleRes;

    } catch (error) {

        console.error(error);

    }

}

/* =========================
   INIT MODAL
========================= */

onMounted(async () => {

    await loadDropdown();

    await nextTick();

    if (modalRef.value) {

        modal = new Modal(modalRef.value);

    }

});

/* =========================
   OPEN
========================= */

function open(item?: ProductVariant) {

    resetForm();

    if (item) {

        Object.assign(form, item);

    }

    modal.show();

}

/* =========================
   CLOSE
========================= */

function close() {

    modal.hide();

}

/* =========================
   WATCH PROP
========================= */

watch(

    () => props.variant,

    (value) => {

        resetForm();

        if (value) {

            Object.assign(form, value);

        }

    },

    {

        immediate: true

    }

);

/* =========================
   RESET FORM
========================= */

function resetForm() {

    Object.assign(form, {

        id: undefined,

        productId: undefined,

        capacityId: undefined,

        bottleTypeId: undefined,

        sku: "",

        price: 0,

        stockQuantity: 0,

        status: 1

    });

}

/* =========================
   VALIDATE
========================= */

function validate(): boolean {

    if (!form.capacityId) {

        alert("Vui lòng chọn dung tích.");

        return false;

    }

    if (!form.bottleTypeId) {

        alert("Vui lòng chọn loại chai.");

        return false;

    }

    if (!form.sku || !form.sku.trim()) {

      alert("SKU không được để trống.");

      return false;

    }

    if (Number(form.price) <= 0) {

        alert("Giá bán phải lớn hơn 0.");

        return false;

    }

    if (Number(form.stockQuantity) < 0) {

        alert("Tồn kho không hợp lệ.");

        return false;

    }

    return true;

}

/* =========================
   SAVE
========================= */

function save() {

    if (!validate()) {

        return;

    }

    const capacity = capacities.value.find(

        x => x.id === form.capacityId

    );

    const bottle = bottleTypes.value.find(

        x => x.id === form.bottleTypeId

    );

    const variant: ProductVariant = {

        id: form.id,

        productId: form.productId,

        capacityId: Number(form.capacityId ?? 0),

        capacityValue: capacity?.value,

        bottleTypeId: Number(form.bottleTypeId ?? 0),

        bottleTypeName: bottle?.name,

        sku: (form.sku ?? "").trim(),

        price: Number(form.price),

        stockQuantity: Number(form.stockQuantity),

        status: Number(form.status)

    };

    emit("saved", variant);

    close();

}

/* =========================
   EXPOSE
========================= */

defineExpose({

    open,

    close

});

</script>