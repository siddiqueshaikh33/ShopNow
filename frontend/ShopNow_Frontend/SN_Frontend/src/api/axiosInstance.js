import axios from "axios";

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  withCredentials: true,
});

// Route /admin requests to the backend /admin endpoints rather than /api/admin
axiosInstance.interceptors.request.use((config) => {
  if (config.url && config.url.startsWith("/admin")) {
    config.url = `..${config.url}`;
  }
  return config;
});

// State for handling silent refresh and request queue
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

// Response Interceptor for handling 401 and silent token rotation
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Handle 429 Rate Limiting: extract Retry-After seconds
    if (error.response?.status === 429) {
      const retryHeader = error.response.headers?.["retry-after"];
      const parsedSeconds = parseInt(retryHeader, 10);
      error.retryAfter = !isNaN(parsedSeconds) && parsedSeconds > 0 ? parsedSeconds : 60;
    }

    // Only intercept 401 Unauthorized errors
    // Do NOT intercept if the failed request was already login or refresh
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes("/auth/refresh") &&
      !originalRequest.url?.includes("/auth/login")
    ) {
      if (isRefreshing) {
        // If another request is currently refreshing the token, wait in the queue
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => axiosInstance(originalRequest))
          .catch((err) => Promise.reject(err));
      }
      // We are the first request to fail with 401; trigger silent refresh
      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Ask backend for a fresh access token using the refreshToken cookie
        await axiosInstance.post("/auth/refresh");

        // Release all requests waiting in the queue
        processQueue(null);

        // Replay the original failed request with the new access token
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        // Refresh token expired (7 days) or revoked -> reject queue & force re-login
        processQueue(refreshError);

        if (
          !window.location.pathname.includes("/login") &&
          window.location.pathname !== "/"
        ) {
          window.location.href = "/login";
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }

    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
