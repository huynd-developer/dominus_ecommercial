import axios from "axios";
import Swal from "sweetalert2";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 30000,
});

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

    if (
      error.response &&
      error.response.status === 401 &&
      !requestUrl.includes("/auth/")
    ) {
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