import axios, {
  type AxiosInstance,
  type AxiosError,
  type InternalAxiosRequestConfig,
} from "axios";

const baseURL = "http://localhost:8080";

const request: AxiosInstance = axios.create({
  baseURL,
  timeout: 60000,
  headers: { "Content-Type": "application/json" },
});

let isRefreshing = false;
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
  (error) => Promise.reject(error),
);

// 2. Interceptor nhận về của custom request
request.interceptors.response.use(
  // Tự động bóc tách 1 lớp 'data' của axios
  (response) => response.data, 
  async (error: AxiosError) => {
    const originalRequest: any = error.config;

    // Xử lý lỗi 403 (Cấm truy cập)
    if (error.response && error.response.status === 403) {
      const errorData: any = error.response.data;
      
      // Đã tắt cái alert quê mùa này đi
      // alert(errorData?.message || "Bạn không có quyền thực hiện hành động này!"); 
      
      // Ghi log ngầm ra console để dev dễ fix bug
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
    if (!refreshToken || refreshToken === "null" || refreshToken === "undefined") {
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
        originalRequest.headers.set("Authorization", `Bearer ${newAccessToken}`);
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
  },
);

export default request;