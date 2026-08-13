<script setup lang="ts">
import type {
  InventoryLotDetailResponse,
  InventoryLotLockHistoryResponse,
  InventoryLotSourceResponse,
} from "../types/inventory-lot.type";

const props = defineProps<{
  visible: boolean;
  detail: InventoryLotDetailResponse | null;
  source: InventoryLotSourceResponse | null;
  history: InventoryLotLockHistoryResponse[];
  loading?: boolean;
  loadingSource?: boolean;
  loadingHistory?: boolean;
  processing?: boolean;
  canManage?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "lock", id: number): void;
  (e: "unlock", id: number): void;
  (e: "open-receipt", receiptNo: string): void;
}>();

const formatDate = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`))
    : "—";

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const expiryText = (detail: InventoryLotDetailResponse) => {
  if (detail.isExpired) {
    return `Đã hết hạn ${Math.abs(detail.daysToExpiry)} ngày`;
  }

  if (detail.isNearExpiry) {
    return `Sắp hết hạn · còn ${detail.daysToExpiry} ngày`;
  }

  return `Còn ${detail.daysToExpiry} ngày`;
};

const expiryClass = (detail: InventoryLotDetailResponse) => {
  if (detail.isExpired) return "badge-expired";
  if (detail.isNearExpiry) return "badge-warning";
  return "badge-ok";
};

const actionClass = (actionType?: string) =>
  actionType === "LOCK" ? "history-lock" : "history-unlock";
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="detail-backdrop" @click.self="emit('close')">
      <div class="detail-dialog">
        <div class="detail-header">
          <div>
            <div class="title-line">
              <h3>{{ detail?.lotCode || "Chi tiết lô hàng" }}</h3>
              <span
                v-if="detail"
                class="status-badge"
                :class="detail.isLocked ? 'badge-locked' : 'badge-unlocked'"
              >
                {{ detail.isLocked ? "Đang khóa" : "Đang mở" }}
              </span>
              <span
                v-if="detail"
                class="status-badge"
                :class="expiryClass(detail)"
              >
                {{ expiryText(detail) }}
              </span>
            </div>
            <p v-if="detail">{{ detail.sku }} · {{ detail.productName }}</p>
          </div>

          <button type="button" class="close-btn" @click="emit('close')">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div v-if="loading" class="state">Đang tải chi tiết lô...</div>

        <div v-else-if="detail" class="detail-body">
          <section>
            <h4>Thông tin lô</h4>
            <div class="info-grid">
              <div>
                <span>Mã lô</span>
                <strong>{{ detail.lotCode }}</strong>
              </div>
              <div>
                <span>SKU</span>
                <strong>{{ detail.sku }}</strong>
              </div>
              <div>
                <span>Sản phẩm</span>
                <strong>{{ detail.productName }}</strong>
              </div>
              <div>
                <span>Ngày sản xuất</span>
                <strong>{{ formatDate(detail.manufacturedDate) }}</strong>
              </div>
              <div>
                <span>Ngày nhận</span>
                <strong>{{ formatDate(detail.receivedDate) }}</strong>
              </div>
              <div>
                <span>Hạn sử dụng</span>
                <strong>{{ formatDate(detail.expirationDate) }}</strong>
              </div>
              <div>
                <span>Trạng thái HSD</span>
                <strong>{{ expiryText(detail) }}</strong>
              </div>
            </div>
          </section>

          <section>
            <h4>Tồn kho</h4>
            <div class="stock-grid">
              <div>
                <span>Tồn ban đầu</span>
                <strong>{{ formatNumber(detail.initialQuantity) }}</strong>
              </div>
              <div>
                <span>Tồn thực tế</span>
                <strong>{{ formatNumber(detail.quantityOnHand) }}</strong>
              </div>
              <div>
                <span>Có thể bán</span>
                <strong>{{ formatNumber(detail.sellableQuantity) }}</strong>
              </div>
            </div>

            <div
              v-if="detail.isExpired"
              class="notice danger"
            >
              <i class="bi bi-exclamation-triangle"></i>
              Lô đã hết hạn nên số lượng có thể bán bằng 0.
            </div>

            <div
              v-else-if="detail.isLocked"
              class="notice warning"
            >
              <i class="bi bi-lock"></i>
              Lô đang bị khóa nên số lượng có thể bán bằng 0. Tồn vật lý vẫn giữ nguyên.
            </div>
          </section>

          <section>
            <h4>Trạng thái lô</h4>

            <div v-if="detail.isLocked" class="info-grid">
              <div>
                <span>Trạng thái</span>
                <strong>Đang khóa</strong>
              </div>
              <div>
                <span>Người khóa</span>
                <strong>{{ detail.lockedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian khóa</span>
                <strong>{{ formatDateTime(detail.lockedAt) }}</strong>
              </div>
              <div>
                <span>Lý do khóa hiện tại</span>
                <strong>{{ detail.lockReason || "—" }}</strong>
              </div>
            </div>

            <div v-else class="notice success">
              <i class="bi bi-check-circle"></i>
              Lô đang hoạt động bình thường và không bị khóa.
            </div>
          </section>

          <section>
            <div class="section-title-row">
              <h4>Nguồn nhập</h4>
              <button
                v-if="source?.receiptNo"
                type="button"
                class="link-btn"
                @click="emit('open-receipt', source.receiptNo)"
              >
                <i class="bi bi-box-arrow-up-right"></i>
                Xem phiếu nhập
              </button>
            </div>

            <div v-if="loadingSource" class="state small">Đang tải nguồn nhập...</div>

            <div v-else-if="source" class="info-grid">
              <div>
                <span>Mã phiếu</span>
                <strong>{{ source.receiptNo || "—" }}</strong>
              </div>
              <div>
                <span>Loại phiếu</span>
                <strong>{{ source.receiptTypeLabel || source.receiptType || "—" }}</strong>
              </div>
              <div>
                <span>Trạng thái phiếu</span>
                <strong>{{ source.receiptStatusLabel || source.receiptStatus || "—" }}</strong>
              </div>
            </div>

            <div v-else class="state small">Lô không có nguồn phiếu nhập.</div>
          </section>

          <section>
            <h4>Thông tin tạo lô</h4>
            <div class="info-grid">
              <div>
                <span>Người tạo</span>
                <strong>{{ detail.createdByName }}</strong>
              </div>
              <div>
                <span>Thời gian tạo</span>
                <strong>{{ formatDateTime(detail.createdAt) }}</strong>
              </div>
            </div>
          </section>

          <section>
            <h4>Lịch sử khóa / mở khóa</h4>

            <div v-if="loadingHistory" class="state small">
              Đang tải lịch sử...
            </div>

            <div v-else-if="history.length === 0" class="state small">
              Lô chưa có lịch sử khóa / mở khóa.
            </div>

            <div v-else class="timeline">
              <div
                v-for="entry in history"
                :key="entry.id"
                class="timeline-item"
              >
                <div class="dot" :class="actionClass(entry.actionType)"></div>
                <div class="timeline-content">
                  <div class="history-head">
                    <strong>{{ entry.actionTypeLabel }}</strong>
                    <span>{{ formatDateTime(entry.actionAt) }}</span>
                  </div>
                  <p>{{ entry.actionByName }}</p>
                  <p v-if="entry.reason" class="history-reason">
                    {{ entry.reason }}
                  </p>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div class="detail-footer">
          <div>
            <button
              v-if="detail && canManage && !detail.isLocked"
              type="button"
              class="danger-btn"
              :disabled="processing"
              @click="emit('lock', detail.id)"
            >
              <i class="bi bi-lock"></i>
              Khóa lô
            </button>

            <button
              v-if="detail && canManage && detail.isLocked"
              type="button"
              class="success-btn"
              :disabled="processing"
              @click="emit('unlock', detail.id)"
            >
              <i class="bi bi-unlock"></i>
              Mở khóa
            </button>
          </div>

          <button type="button" class="close-footer-btn" @click="emit('close')">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.detail-backdrop{position:fixed;inset:0;z-index:99999;overflow-y:auto;padding:24px;background:rgba(15,23,42,.5)}
.detail-dialog{width:min(1120px,100%);margin:20px auto;overflow:hidden;border-radius:16px;background:#fff;box-shadow:0 24px 60px rgba(15,23,42,.22)}
.detail-header,.detail-footer{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:18px 22px;border-bottom:1px solid #e5e7eb}
.detail-header h3{margin:0}
.detail-header p{margin:6px 0 0;color:#6b7280;font-size:13px}
.title-line{display:flex;align-items:center;flex-wrap:wrap;gap:8px}
.detail-footer{border-top:1px solid #e5e7eb;border-bottom:0}
.detail-footer>div{display:flex;gap:8px}
.close-btn{width:36px;height:36px;border:0;border-radius:8px;background:transparent;cursor:pointer}
.close-footer-btn,.danger-btn,.success-btn,.link-btn{border:0;border-radius:8px;cursor:pointer;font-weight:600}
.close-footer-btn{padding:9px 16px;background:#111827;color:#fff}
.danger-btn{padding:9px 14px;background:#b91c1c;color:#fff}
.success-btn{padding:9px 14px;background:#047857;color:#fff}
.link-btn{padding:7px 10px;background:#eff6ff;color:#1d4ed8}
.detail-footer button:disabled{opacity:.5;cursor:not-allowed}
.detail-body{padding:20px 22px}
section+section{margin-top:24px}
section h4{margin:0 0 12px}
.section-title-row{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:12px}
.section-title-row h4{margin:0}
.info-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}
.info-grid>div,.stock-grid>div{padding:12px;border:1px solid #e5e7eb;border-radius:10px;background:#fafafa}
.info-grid span,.stock-grid span{display:block;margin-bottom:4px;color:#6b7280;font-size:12px}
.stock-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}
.stock-grid strong{font-size:22px}
.notice{display:flex;align-items:center;gap:8px;margin-top:12px;padding:12px;border-radius:10px;font-size:13px}
.notice.danger{border:1px solid #fecaca;background:#fef2f2;color:#991b1b}
.notice.warning{border:1px solid #fed7aa;background:#fff7ed;color:#9a3412}
.notice.success{border:1px solid #a7f3d0;background:#ecfdf5;color:#047857}
.status-badge{display:inline-flex;padding:4px 9px;border-radius:999px;font-size:12px;font-weight:600}
.badge-locked{background:#fef2f2;color:#b91c1c}
.badge-unlocked{background:#ecfdf5;color:#047857}
.badge-expired{background:#fef2f2;color:#b91c1c}
.badge-warning{background:#fff7ed;color:#c2410c}
.badge-ok{background:#ecfdf5;color:#047857}
.timeline{display:flex;flex-direction:column;gap:14px}
.timeline-item{display:grid;grid-template-columns:16px 1fr;gap:10px}
.dot{width:10px;height:10px;margin-top:5px;border-radius:50%}
.history-lock{background:#dc2626}
.history-unlock{background:#059669}
.history-head{display:flex;align-items:center;justify-content:space-between;gap:12px}
.history-head span,.timeline-content p{color:#6b7280;font-size:13px}
.timeline-content p{margin:3px 0 0}
.history-reason{color:#374151!important}
.state{padding:24px;text-align:center;color:#6b7280}
.state.small{padding:14px}
@media(max-width:800px){
  .detail-backdrop{padding:10px}
  .info-grid{grid-template-columns:repeat(2,1fr)}
  .stock-grid{grid-template-columns:1fr}
  .section-title-row{align-items:flex-start;flex-direction:column}
}
</style>
