<script setup lang="ts">
import type {
  OpeningBalanceApprovalHistoryResponse,
  OpeningBalanceDetailResponse,
} from "../types/opening-balance.type";

const props = defineProps<{
  visible: boolean;
  detail: OpeningBalanceDetailResponse | null;
  history: OpeningBalanceApprovalHistoryResponse[];
  loading?: boolean;
  loadingHistory?: boolean;
}>();

const emit = defineEmits<{ (e: "close"): void }>();

const formatDateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";

const formatDate = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`))
    : "—";

const formatNumber = (value?: number | null) =>
  new Intl.NumberFormat("vi-VN").format(Number(value ?? 0));

const statusLabel = (status?: string | null, backendLabel?: string | null) => {
  const normalized = String(status || backendLabel || "")
    .trim()
    .toUpperCase()
    .replace(/\s+/g, "_");

  switch (normalized) {
    case "DRAFT":
      return "Lưu tạm";
    case "PENDING_APPROVAL":
    case "PENDING":
      return "Chờ duyệt";
    case "APPROVED":
      return "Đã phê duyệt";
    case "REJECTED":
      return "Đã từ chối";
    case "CANCELLED":
    case "CANCELED":
      return "Đã hủy";
    default:
      return backendLabel || status || "—";
  }
};

const translateHistoryStatus = (value?: string | null) => {
  if (!value) return "Khởi tạo";

  const normalized = String(value)
    .trim()
    .toUpperCase()
    .replace(/\s+/g, "_");

  switch (normalized) {
    case "DRAFT":
      return "Lưu tạm";
    case "PENDING_APPROVAL":
    case "PENDING":
      return "Chờ duyệt";
    case "APPROVED":
      return "Đã phê duyệt";
    case "REJECTED":
      return "Đã từ chối";
    case "CANCELLED":
    case "CANCELED":
      return "Đã hủy";
    default:
      return value;
  }
};

const statusClass = (status?: string) =>
  `status-${String(status || "")
    .toLowerCase()
    .replace("_approval", "")}`;
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="detail-backdrop" @click.self="emit('close')">
      <div class="detail-dialog">
        <div class="detail-header">
          <div>
            <h3>{{ detail?.receiptNo || "Chi tiết phiếu tồn đầu kỳ" }}</h3>
            <div class="header-meta">
              <span class="type-badge">Tồn đầu kỳ</span>
              <span
                v-if="detail"
                class="status-badge"
                :class="statusClass(detail.status)"
              >
                {{ statusLabel(detail.status, detail.statusLabel) }}
              </span>
            </div>
          </div>
          <button type="button" class="close-btn" @click="emit('close')">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div v-if="loading" class="state">Đang tải chi tiết...</div>

        <div v-else-if="detail" class="detail-body">
          <section>
            <h4>Thông tin chung</h4>
            <div class="info-grid">
              <div>
                <span>Loại nghiệp vụ</span>
                <strong>Tồn đầu kỳ</strong>
              </div>
              <div>
                <span>Trạng thái</span>
                <strong>{{ statusLabel(detail.status, detail.statusLabel) }}</strong>
              </div>
              <div>
                <span>Người tạo</span>
                <strong>{{ detail.createdByName || "—" }}</strong>
              </div>
              <div>
                <span>Ngày tạo</span>
                <strong>{{ formatDateTime(detail.createdAt) }}</strong>
              </div>
              <div>
                <span>Tổng SKU</span>
                <strong>{{ formatNumber(detail.totalSku) }}</strong>
              </div>
              <div>
                <span>Tổng số lượng thực tế</span>
                <strong>{{ formatNumber(detail.totalQuantity) }}</strong>
              </div>
              <div>
                <span>Người gửi duyệt</span>
                <strong>{{ detail.submittedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian gửi duyệt</span>
                <strong>{{ formatDateTime(detail.submittedAt) }}</strong>
              </div>
              <div>
                <span>Người phê duyệt</span>
                <strong>{{ detail.approvedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian phê duyệt</span>
                <strong>{{ formatDateTime(detail.approvedAt) }}</strong>
              </div>
              <div>
                <span>Người từ chối</span>
                <strong>{{ detail.rejectedByName || "—" }}</strong>
              </div>
              <div>
                <span>Thời gian từ chối</span>
                <strong>{{ formatDateTime(detail.rejectedAt) }}</strong>
              </div>
            </div>

            <div v-if="detail.note" class="note-box">
              <span>Ghi chú</span>
              <p>{{ detail.note }}</p>
            </div>

            <div v-if="detail.rejectionReason" class="reason-box danger">
              <strong>Lý do từ chối</strong>
              <p>{{ detail.rejectionReason }}</p>
            </div>

            <div v-if="detail.cancellationReason" class="reason-box">
              <strong>Lý do hủy</strong>
              <p>{{ detail.cancellationReason }}</p>
            </div>

            <div v-if="detail.status === 'APPROVED'" class="approved-note">
              <i class="bi bi-check-circle"></i>
              Phiếu đã được phê duyệt. Số lượng tồn đầu kỳ đã được ghi nhận
              vào kho theo từng lô.
            </div>
          </section>

          <section>
            <h4>Số lượng thực tế theo lô</h4>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Sản phẩm</th>
                    <th>Mã lô</th>
                    <th>Số lượng thực tế</th>
                    <th>Ngày sản xuất</th>
                    <th>Ngày nhận / ghi nhận</th>
                    <th>Hạn sử dụng</th>
                    <th>Ghi chú</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in detail.items" :key="item.id">
                    <td><strong>{{ item.sku }}</strong></td>
                    <td>{{ item.productName }}</td>
                    <td>{{ item.lotCode }}</td>
                    <td>{{ formatNumber(item.quantity) }}</td>
                    <td>{{ formatDate(item.manufacturedDate) }}</td>
                    <td>{{ formatDate(item.receivedDate) }}</td>
                    <td>{{ formatDate(item.expirationDate) }}</td>
                    <td>{{ item.note || "—" }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section>
            <h4>Lịch sử duyệt</h4>
            <div v-if="loadingHistory" class="state">Đang tải lịch sử...</div>
            <div v-else-if="history.length === 0" class="state">
              Chưa có lịch sử duyệt.
            </div>
            <div v-else class="timeline">
              <div
                v-for="entry in history"
                :key="entry.id"
                class="timeline-item"
              >
                <div class="dot"></div>
                <div>
                  <strong>
                    {{ translateHistoryStatus(entry.fromStatusLabel) }} →
                    {{ translateHistoryStatus(entry.toStatusLabel) }}
                  </strong>
                  <p>
                    {{ entry.actionByName }} · {{ formatDateTime(entry.actionAt) }}
                  </p>
                  <p v-if="entry.reason" class="timeline-reason">
                    {{ entry.reason }}
                  </p>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div class="detail-footer">
          <button type="button" @click="emit('close')">Đóng</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 99999;
  overflow-y: auto;
  padding: 24px;
  background: rgba(15, 23, 42, 0.5);
}
.detail-dialog {
  width: min(1120px, 100%);
  margin: 20px auto;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}
.detail-header,
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 22px;
  border-bottom: 1px solid #e5e7eb;
}
.detail-footer {
  justify-content: flex-end;
  border-top: 1px solid #e5e7eb;
  border-bottom: 0;
}
.detail-header h3 {
  margin: 0 0 7px;
}
.header-meta {
  display: flex;
  gap: 7px;
}
.close-btn,
.detail-footer button {
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}
.close-btn {
  width: 36px;
  height: 36px;
  background: transparent;
}
.detail-footer button {
  padding: 9px 16px;
  background: #111827;
  color: #fff;
}
.detail-body {
  padding: 20px 22px;
}
section + section {
  margin-top: 24px;
}
section h4 {
  margin: 0 0 12px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.info-grid > div,
.note-box,
.reason-box,
.approved-note {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}
.info-grid span,
.note-box span {
  display: block;
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
}
.note-box,
.reason-box,
.approved-note {
  margin-top: 12px;
}
.note-box p,
.reason-box p {
  margin: 4px 0 0;
}
.reason-box.danger {
  border-color: #fecaca;
  background: #fef2f2;
}
.approved-note {
  border-color: #a7f3d0;
  background: #ecfdf5;
  color: #047857;
}
.approved-note i {
  margin-right: 6px;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}
table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
}
th,
td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
  font-size: 13px;
}
th {
  background: #f9fafb;
  color: #4b5563;
}
.timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.timeline-item {
  display: grid;
  grid-template-columns: 16px 1fr;
  gap: 10px;
}
.dot {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: #111827;
}
.timeline-item p {
  margin: 3px 0 0;
  color: #6b7280;
  font-size: 13px;
}
.timeline-reason {
  color: #374151 !important;
}
.type-badge,
.status-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.type-badge {
  background: #eff6ff;
  color: #1d4ed8;
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
.state {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}
@media (max-width: 800px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
