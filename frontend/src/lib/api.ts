import axios from 'axios';

const envApiUrl = typeof import.meta !== 'undefined' && (import.meta as any).env ? (import.meta as any).env.VITE_API_URL : '';
const API_URL = envApiUrl || '';

export const api = axios.create({
  baseURL: API_URL,
  timeout: 120000,
});

api.interceptors.request.use((config) => {
  const customKey = localStorage.getItem('vrezerApiKey') || '';
  if (customKey && config.headers && !config.headers['X-GEMINI-API-KEY']) {
    config.headers['X-GEMINI-API-KEY'] = customKey;
  }
  return config;
});

export default api;
