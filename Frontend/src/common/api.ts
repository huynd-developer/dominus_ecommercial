import axios from "axios";
import Swal from "sweetalert2";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 30000,
});

/*
 * Tránh nhiều request cùng nhận 401 và mở nhiều popup
 * "Phiên đăng nhập đã hết hạn" cùng lúc.
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

    /*
     * Chỉ coi là "phiên đăng nhập đã hết hạn" khi request thực tế
     * đã gửi Bearer token nhưng backend trả 401.
     *
     * Nếu request không có token thì không tự bật popup hết phiên;
     * các chức năng cần login sẽ tự yêu cầu đăng nhập theo flow hiện tại.
     */
    const requestHadToken = Boolean(
      originalRequest?.headers?.Authorization ||
      originalRequest?.headers?.authorization
    );

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