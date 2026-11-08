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

export default axiosInstance;
