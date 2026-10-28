import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('bankops_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Generate or forward Correlation ID
  let correlationId = sessionStorage.getItem('bankops_correlation_id');
  if (!correlationId) {
    correlationId = 'UI-' + Math.random().toString(36).substring(2, 9).toUpperCase();
    sessionStorage.setItem('bankops_correlation_id', correlationId);
  }
  config.headers['X-Correlation-ID'] = correlationId;

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !window.location.pathname.includes('/login')) {
      localStorage.removeItem('bankops_token');
      localStorage.removeItem('bankops_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
