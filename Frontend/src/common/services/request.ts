import axios, {
  type AxiosInstance,
  type AxiosError,
  type InternalAxiosRequestConfig,
} from "axios";
import Swal from "sweetalert2";

const baseURL = "http://localhost:8080";

const request: AxiosInstance = axios.create({
  baseURL,
  timeout: 60000,
  headers: { "Content-Type": "application/json" },
});

let isRefreshing = false;
let handlingForcedLogout = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error);
    else prom.resolve(token);
  });
  failedQueue = [];
};

// 1. Interceptor gửi đi của custom request (Giữ nguyên logic JWT)
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem("token");
    if (!config.headers) {
      config.headers = new axios.AxiosHeaders();
    }
    if (token && token !== "null" && token !== "undefined") {
      config.headers.set("Authorization", `Bearer ${token}`);
    } else {
      config.headers.delete("Authorization");
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 2. Interceptor nhận về của custom request
request.interceptors.response.use(
  // Tự động bóc tách 1 lớp 'data' của axios
  (response) => response.data,
  async (error: AxiosError) => {
    const originalRequest: any = error.config;
    const requestUrl = originalRequest?.url || "";

    const authorizationHeader =
      typeof originalRequest?.headers?.get === "function"
        ? originalRequest.headers.get("Authorization")
        : originalRequest?.headers?.Authorization ||
          originalRequest?.headers?.authorization;

    const requestHadToken = Boolean(authorizationHeader);

    // Xử lý lỗi 403 (Cấm truy cập)
    if (error.response && error.response.status === 403) {
      const errorData: any = error.response.data;
      const code = errorData?.code;

      /*
       * Chỉ logout khi BE xác nhận tài khoản đã bị vô hiệu hóa/xóa.
       *
       * KHÔNG logout với 403 phân quyền bình thường.
       */
      if (
        (code === "ACCOUNT_DISABLED" || code === "ACCOUNT_REMOVED") &&
        requestHadToken &&
        !requestUrl.includes("/auth/") &&
        !handlingForcedLogout
      ) {
        handlingForcedLogout = true;

        const removed = code === "ACCOUNT_REMOVED";

        await Swal.fire({
          icon: "warning",
          title: removed
            ? "Tài khoản không còn tồn tại"
            : "Tài khoản không còn hoạt động",
          text:
            errorData?.message ||
            (removed
              ? "Tài khoản của bạn không còn tồn tại hoặc đã bị xóa."
              : "Tài khoản của bạn đã bị khóa hoặc không còn hoạt động. Vui lòng liên hệ quản trị viên."),
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
       * 403 phân quyền bình thường:
       * giữ nguyên hành vi cũ, không logout.
       */
      console.error("Lỗi 403 Forbidden:", errorData?.message);

      return Promise.reject(error);
    }

    if (!originalRequest) return Promise.reject(error);

    // Bỏ qua logic refresh token cho các API Auth
    if (
      originalRequest.url?.includes("/auth/login") ||
      originalRequest.url?.includes("/auth/register") ||
      originalRequest.url?.includes("/auth/refresh")
    ) {
      return Promise.reject(error);
    }

    // Nếu không phải lỗi 401 hoặc đã retry rồi thì ném lỗi luôn
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    const refreshToken = localStorage.getItem("refreshToken");
    if (
      !refreshToken ||
      refreshToken === "null" ||
      refreshToken === "undefined"
    ) {
      return Promise.reject(error);
    }

    // Đưa các request tiếp theo vào hàng đợi nếu đang refresh token
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((token) => {
          if (!originalRequest.headers) {
            originalRequest.headers = new axios.AxiosHeaders();
          }
          originalRequest.headers.set("Authorization", "Bearer " + token);
          return request(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const response: any = await axios.post(`${baseURL}/auth/refresh`, {
        refreshToken,
      });

      const resData = response.data;
      if (resData && (resData.status === 200 || resData.code === 200)) {
        const newAccessToken = resData.data.accessToken;
        localStorage.setItem("accessToken", newAccessToken);
        processQueue(null, newAccessToken);

        if (!originalRequest.headers) {
          originalRequest.headers = new axios.AxiosHeaders();
        }
        originalRequest.headers.set(
          "Authorization",
          `Bearer ${newAccessToken}`
        );
        return request(originalRequest);
      }
      throw new Error("Refresh token invalid");
    } catch (refreshError) {
      processQueue(refreshError, null);
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      localStorage.removeItem("name");

      if (window.location.pathname.startsWith("/admin")) {
        window.location.href = "/login";
      }
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default request;
