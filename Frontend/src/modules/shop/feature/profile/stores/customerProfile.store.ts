import { defineStore } from "pinia";
import Swal from "sweetalert2";
import { customerProfileService } from "../services/customerProfile.service";
import type {
  ChangePasswordRequest,
  CustomerOrderResponse,
  CustomerProfileResponse,
  FavoriteResponse,
  GenderValue,
  UpdateCustomerProfileRequest,
} from "../types/profile.type";

const NAME_REGEX = /^[\p{L}]+(?: [\p{L}]+)*$/u;
const PHONE_REGEX = /^0\d{9}$/;
const PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#])[A-Za-z\d@$!%*?&.#]{8,50}$/;

const MAX_AVATAR_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

const toast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 2500,
  timerProgressBar: true,
});

function showSuccess(message: string) {
  toast.fire({
    icon: "success",
    title: message,
  });
}

function showError(message: string) {
  toast.fire({
    icon: "error",
    title: message,
  });
}

function hasWhitespace(value: string): boolean {
  return /\s/.test(value);
}

function hasLeadingOrTrailingSpace(value: string): boolean {
  return value !== value.trim();
}

function hasDoubleSpace(value: string): boolean {
  return value.includes("  ");
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim().length === 0;
}

function parseDate(value: string): Date | null {
  if (!value) return null;

  const parts = value.split("-");
  const rawYear = parts[0];
  const rawMonth = parts[1];
  const rawDay = parts[2];

  if (!rawYear || !rawMonth || !rawDay || parts.length !== 3) {
    return null;
  }

  const year = Number(rawYear);
  const month = Number(rawMonth);
  const day = Number(rawDay);

  if (
    !Number.isInteger(year) ||
    !Number.isInteger(month) ||
    !Number.isInteger(day)
  ) {
    return null;
  }

  const date = new Date(year, month - 1, day);

  const isValid =
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day;

  return isValid ? date : null;
}

function getTodayDateOnly(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

function getErrorMessage(error: any): string {
  const data = error?.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  if (data?.errors) {
    const firstError = Object.values(data.errors)[0];

    if (typeof firstError === "string") {
      return firstError;
    }

    if (Array.isArray(firstError) && firstError.length > 0) {
      return String(firstError[0]);
    }
  }

  if (error?.message) {
    return error.message;
  }

  return "Có lỗi xảy ra. Vui lòng thử lại";
}

function dispatchProfileUpdated(profile: CustomerProfileResponse) {
  localStorage.setItem("name", profile.name || "");
  localStorage.setItem("customerAvatarUrl", profile.avatarUrl || "");

  window.dispatchEvent(
    new CustomEvent("customer-profile-updated", {
      detail: {
        name: profile.name,
        avatarUrl: profile.avatarUrl,
        customerRank: profile.customerRank,
        loyaltyPoints: profile.loyaltyPoints,
      },
    })
  );
}

export const useCustomerProfileStore = defineStore("customerProfile", {
  state: () => ({
    activeTab: "profile" as "profile" | "password" | "favorites" | "orders",

    profile: null as CustomerProfileResponse | null,
    favorites: [] as FavoriteResponse[],
    orders: [] as CustomerOrderResponse[],

    selectedAvatarFile: null as File | null,
    avatarPreviewUrl: "",

    profileForm: {
      name: "",
      email: "",
      phone: "",
      address: "",
      avatarUrl: "",
      dateOfBirth: "",
      gender: null as GenderValue,
    },

    passwordForm: {
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
    },

    loading: false,
    profileLoading: false,
    avatarLoading: false,
    passwordLoading: false,
    favoriteLoading: false,
    orderLoading: false,

    error: "",
    success: "",
  }),

  actions: {
    clearMessage() {
      this.error = "";
      this.success = "";
    },

    setError(message: string) {
      this.error = message;
      showError(message);
    },

    setSuccess(message: string) {
      this.success = message;
      showSuccess(message);
    },

    fillProfileForm(profile: CustomerProfileResponse) {
      this.profileForm.name = profile.name || "";
      this.profileForm.email = profile.email || "";
      this.profileForm.phone = profile.phone || "";
      this.profileForm.address = profile.address || "";
      this.profileForm.avatarUrl = profile.avatarUrl || "";
      this.profileForm.dateOfBirth = profile.dateOfBirth || "";
      this.profileForm.gender = profile.gender ?? null;
      this.avatarPreviewUrl = profile.avatarUrl || "";

      dispatchProfileUpdated(profile);
    },

    async loadInitialData() {
      this.loading = true;
      this.clearMessage();

      try {
        await Promise.all([
          this.fetchProfile(),
          this.fetchFavorites(),
          this.fetchOrders(),
        ]);
      } finally {
        this.loading = false;
      }
    },

    async fetchProfile() {
      this.profileLoading = true;
      this.clearMessage();

      try {
        const res = await customerProfileService.getProfile();
        this.profile = res.data;
        this.fillProfileForm(res.data);
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.profileLoading = false;
      }
    },

    validateProfileForm(): string {
      const form = this.profileForm;

      if (isBlank(form.name)) {
        return "Họ tên không được để trống";
      }

      if (hasLeadingOrTrailingSpace(form.name)) {
        return "Họ tên không được có khoảng trắng ở đầu hoặc cuối";
      }

      if (hasDoubleSpace(form.name)) {
        return "Họ tên không được chứa nhiều khoảng trắng liên tiếp";
      }

      if (form.name.length < 2 || form.name.length > 100) {
        return "Họ tên phải từ 2 đến 100 ký tự";
      }

      if (!NAME_REGEX.test(form.name)) {
        return "Họ tên chỉ được chứa chữ cái và một khoảng trắng giữa các từ";
      }

      if (isBlank(form.phone)) {
        return "Số điện thoại không được để trống";
      }

      if (hasWhitespace(form.phone)) {
        return "Số điện thoại không được chứa khoảng trắng";
      }

      if (!PHONE_REGEX.test(form.phone)) {
        return "Số điện thoại phải gồm 10 số và bắt đầu bằng 0";
      }

      if (isBlank(form.address)) {
        return "Địa chỉ không được để trống";
      }

      if (hasLeadingOrTrailingSpace(form.address)) {
        return "Địa chỉ không được có khoảng trắng ở đầu hoặc cuối";
      }

      if (hasDoubleSpace(form.address)) {
        return "Địa chỉ không được chứa nhiều khoảng trắng liên tiếp";
      }

      if (form.address.length < 5 || form.address.length > 500) {
        return "Địa chỉ phải từ 5 đến 500 ký tự";
      }

      if (
        form.gender !== null &&
        form.gender !== 0 &&
        form.gender !== 1 &&
        form.gender !== 2
      ) {
        return "Giới tính không hợp lệ";
      }

      if (form.dateOfBirth) {
        const date = parseDate(form.dateOfBirth);

        if (!date) {
          return "Ngày sinh không hợp lệ";
        }

        if (date > getTodayDateOnly()) {
          return "Ngày sinh không được lớn hơn ngày hiện tại";
        }
      }

      return "";
    },

    buildProfilePayload(): UpdateCustomerProfileRequest {
      return {
        name: this.profileForm.name,
        phone: this.profileForm.phone,
        address: this.profileForm.address,
        dateOfBirth: this.profileForm.dateOfBirth || null,
        gender: this.profileForm.gender,
      };
    },

    async updateProfile() {
      this.profileLoading = true;
      this.clearMessage();

      const validateMessage = this.validateProfileForm();

      if (validateMessage) {
        this.setError(validateMessage);
        this.profileLoading = false;
        return;
      }

      try {
        const res = await customerProfileService.updateProfile(
          this.buildProfilePayload()
        );

        this.profile = res.data;
        this.fillProfileForm(res.data);
        this.setSuccess("Cập nhật thông tin thành công");
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.profileLoading = false;
      }
    },

    selectAvatarFile(file: File | null, previewUrl?: string) {
      this.selectedAvatarFile = null;

      if (!file) {
        return;
      }

      const validateMessage = this.validateAvatarFile(file);

      if (validateMessage) {
        this.setError(validateMessage);
        return;
      }

      this.selectedAvatarFile = file;
      this.avatarPreviewUrl = previewUrl || URL.createObjectURL(file);
    },

    validateAvatarFile(file: File): string {
      if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
        return "Ảnh đại diện chỉ hỗ trợ JPG, PNG hoặc WEBP";
      }

      if (file.size > MAX_AVATAR_SIZE) {
        return "Ảnh đại diện tối đa 5MB";
      }

      return "";
    },

    async uploadAvatar() {
      this.clearMessage();

      if (!this.selectedAvatarFile) {
        this.setError("Vui lòng chỉnh và áp dụng ảnh đại diện trước khi lưu");
        return;
      }

      this.avatarLoading = true;

      try {
        const res = await customerProfileService.uploadAvatar(
          this.selectedAvatarFile
        );

        this.profile = res.data;
        this.fillProfileForm(res.data);

        this.selectedAvatarFile = null;

        this.setSuccess("Cập nhật ảnh đại diện thành công");
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.avatarLoading = false;
      }
    },

    validatePasswordForm(): string {
      const form = this.passwordForm;

      if (isBlank(form.oldPassword)) {
        return "Mật khẩu cũ không được để trống";
      }

      if (hasWhitespace(form.oldPassword)) {
        return "Mật khẩu cũ không được chứa khoảng trắng";
      }

      if (isBlank(form.newPassword)) {
        return "Mật khẩu mới không được để trống";
      }

      if (hasWhitespace(form.newPassword)) {
        return "Mật khẩu mới không được chứa khoảng trắng";
      }

      if (!PASSWORD_REGEX.test(form.newPassword)) {
        return "Mật khẩu mới phải có chữ hoa, chữ thường, số, ký tự đặc biệt và từ 8 đến 50 ký tự";
      }

      if (isBlank(form.confirmPassword)) {
        return "Xác nhận mật khẩu không được để trống";
      }

      if (hasWhitespace(form.confirmPassword)) {
        return "Xác nhận mật khẩu không được chứa khoảng trắng";
      }

      if (form.newPassword !== form.confirmPassword) {
        return "Xác nhận mật khẩu không khớp";
      }

      if (form.oldPassword === form.newPassword) {
        return "Mật khẩu mới không được trùng mật khẩu cũ";
      }

      return "";
    },

    async changePassword() {
      this.passwordLoading = true;
      this.clearMessage();

      const validateMessage = this.validatePasswordForm();

      if (validateMessage) {
        this.setError(validateMessage);
        this.passwordLoading = false;
        return;
      }

      const payload: ChangePasswordRequest = {
        oldPassword: this.passwordForm.oldPassword,
        newPassword: this.passwordForm.newPassword,
        confirmPassword: this.passwordForm.confirmPassword,
      };

      try {
        await customerProfileService.changePassword(payload);

        this.passwordForm.oldPassword = "";
        this.passwordForm.newPassword = "";
        this.passwordForm.confirmPassword = "";

        this.setSuccess("Đổi mật khẩu thành công");
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.passwordLoading = false;
      }
    },

    async fetchFavorites() {
      this.favoriteLoading = true;
      this.clearMessage();

      try {
        const res = await customerProfileService.getFavorites();
        this.favorites = res.data || [];
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.favoriteLoading = false;
      }
    },

    async deleteFavorite(favoriteId: number) {
      if (!Number.isInteger(favoriteId) || favoriteId <= 0) {
        this.setError("favoriteId không hợp lệ");
        return;
      }

      const result = await Swal.fire({
        title: "Bỏ yêu thích?",
        text: "Bạn có chắc chắn muốn xóa sản phẩm này khỏi danh sách yêu thích không?",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Đồng ý",
        cancelButtonText: "Hủy",
        confirmButtonColor: "#dc3545",
        cancelButtonColor: "#6c757d",
      });

      if (!result.isConfirmed) {
        return;
      }

      this.favoriteLoading = true;
      this.clearMessage();

      try {
        await customerProfileService.deleteFavorite(favoriteId);
        this.setSuccess("Đã xóa sản phẩm khỏi yêu thích");
        await this.fetchFavorites();
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.favoriteLoading = false;
      }
    },

    async fetchOrders() {
      this.orderLoading = true;
      this.clearMessage();

      try {
        const res = await customerProfileService.getOrders();
        this.orders = res.data || [];
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.orderLoading = false;
      }
    },

    async cancelOrder(order: CustomerOrderResponse) {
      if (!order || !Number.isInteger(order.orderId) || order.orderId <= 0) {
        this.setError("orderId không hợp lệ");
        return;
      }

      if (order.canCancel !== true) {
        this.setError(
          order.statusText
            ? `Không thể hủy đơn hàng ở trạng thái: ${order.statusText}`
            : "Chỉ được hủy đơn hàng khi đơn đang chờ xác nhận"
        );
        return;
      }

      const result = await Swal.fire({
        title: `Hủy đơn hàng #${order.orderId}?`,
        text: "Sau khi hủy, hệ thống sẽ hoàn lại tồn kho cho sản phẩm trong đơn.",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Hủy đơn",
        cancelButtonText: "Không",
        confirmButtonColor: "#dc3545",
        cancelButtonColor: "#6c757d",
      });

      if (!result.isConfirmed) {
        return;
      }

      this.orderLoading = true;
      this.clearMessage();

      try {
        await customerProfileService.cancelOrder(order.orderId);
        this.setSuccess("Hủy đơn hàng thành công");
        await this.fetchOrders();
      } catch (error: any) {
        this.setError(getErrorMessage(error));
      } finally {
        this.orderLoading = false;
      }
    },
  },
});
