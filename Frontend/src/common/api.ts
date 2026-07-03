import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
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

      alert("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!");

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