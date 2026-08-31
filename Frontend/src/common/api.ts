import axios from "axios";
import Swal from "sweetalert2";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 30000,
});

/*
 * Tránh nhiều request cùng lỗi authentication
 * và mở nhiều popup cùng lúc.
 *
 * Dùng chung cho:
 * - 401: phiên đăng nhập hết hạn
 * - ACCOUNT_DISABLED: tài khoản bị khóa/ngừng hoạt động
 * - ACCOUNT_REMOVED: tài khoản đã bị xóa
 */
let handlingUnauthorized = false;

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    /**
     * Quan trọng:
     * - Request thường: JSON
     * - Upload file FormData: KHÔNG tự set Content-Type
     *   để browser tự sinh multipart/form-data; boundary=...
     */
    if (config.data instanceof FormData) {
      delete (config.headers as any)["Content-Type"];
    } else {
      config.headers["Content-Type"] = "application/json";
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,

  async (error) => {
    const originalRequest = error.config;

    const requestUrl = originalRequest?.url || "";

    const status = error.response?.status;

    const responseCode = error.response?.data?.code;

    const responseMessage = error.response?.data?.message;

    /*
     * Kiểm tra request thực tế có gửi Bearer token hay không.
     */
    const requestHadToken = Boolean(
      originalRequest?.headers?.Authorization ||
        originalRequest?.headers?.authorization
    );

    /*
     * =========================================================
     * 1. TÀI KHOẢN BỊ KHÓA / NGỪNG HOẠT ĐỘNG / SOFT DELETE
     * =========================================================
     *
     * Chỉ xử lý đúng code do JwtAuthenticationFilter trả về.
     *
     * KHÔNG bắt tất cả 403 vì 403 còn được dùng cho lỗi phân quyền.
     */
    if (
      status === 403 &&
      responseCode === "ACCOUNT_DISABLED" &&
      !requestUrl.includes("/auth/") &&
      requestHadToken &&
      !handlingUnauthorized
    ) {
      handlingUnauthorized = true;

      /*
       * Hiển thị thông báo TRƯỚC.
       *
       * Không xóa token ngay để tránh router/watcher nào đó
       * redirect làm popup biến mất trước khi user nhìn thấy.
       */
      await Swal.fire({
        icon: "warning",
        title: "Tài khoản không còn hoạt động",
        text:
          responseMessage ||
          "Tài khoản của bạn đã bị khóa hoặc không còn hoạt động. Vui lòng liên hệ quản trị viên.",
        confirmButtonText: "Đồng ý",
        confirmButtonColor: "#bd9a5f",
        allowOutsideClick: false,
        allowEscapeKey: false,
      });

      /*
       * Sau khi user đã thấy thông báo mới clear session.
       */
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("name");
      localStorage.removeItem("customerAvatarUrl");

      /*
       * Employee/Admin -> admin login
       * Customer       -> customer login
       */
      if (window.location.pathname.startsWith("/admin")) {
        window.location.href = "/admin/login";
      } else {
        window.location.href = "/login";
      }

      return Promise.reject(error);
    }

    /*
     * =========================================================
     * 2. TÀI KHOẢN ĐÃ BỊ XÓA KHỎI DATABASE
     * =========================================================
     */
    if (
      status === 403 &&
      responseCode === "ACCOUNT_REMOVED" &&
      !requestUrl.includes("/auth/") &&
      requestHadToken &&
      !handlingUnauthorized
    ) {
      handlingUnauthorized = true;

      await Swal.fire({
        icon: "warning",
        title: "Tài khoản không còn tồn tại",
        text:
          responseMessage ||
          "Tài khoản của bạn không còn tồn tại hoặc đã bị xóa.",
        confirmButtonText: "Đồng ý",
        confirmButtonColor: "#bd9a5f",
        allowOutsideClick: false,
        allowEscapeKey: false,
      });

      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("name");
      localStorage.removeItem("customerAvatarUrl");

      if (window.location.pathname.startsWith("/admin")) {
        window.location.href = "/admin/login";
      } else {
        window.location.href = "/login";
      }

      return Promise.reject(error);
    }

    /*
     * =========================================================
     * 3. PHIÊN ĐĂNG NHẬP HẾT HẠN
     * =========================================================
     *
     * GIỮ NGUYÊN LOGIC CŨ.
     *
     * Chỉ coi là "phiên đăng nhập đã hết hạn" khi request thực tế
     * đã gửi Bearer token nhưng backend trả 401.
     *
     * Nếu request không có token thì không tự bật popup hết phiên;
     * các chức năng cần login sẽ tự yêu cầu đăng nhập theo flow hiện tại.
     */
    if (
      status === 401 &&
      !requestUrl.includes("/auth/") &&
      requestHadToken &&
      !handlingUnauthorized
    ) {
      handlingUnauthorized = true;

      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("name");
      localStorage.removeItem("customerAvatarUrl");

      await Swal.fire({
        icon: "warning",
        title: "Phiên đăng nhập đã hết hạn",
        text: "Vui lòng đăng nhập lại để tiếp tục.",
        confirmButtonText: "Đăng nhập lại",
        confirmButtonColor: "#bd9a5f",
      });

      if (window.location.pathname.startsWith("/admin")) {
        window.location.href = "/admin/login";
      } else {
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

export default api;
